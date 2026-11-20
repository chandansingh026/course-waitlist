import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.concurrent.*;

public class LiveApiEvidence {
    static final ObjectMapper json = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
    static final Path root = Path.of("api-execution-evidence");
    static final Path logs = root.resolve("06-all-request-response-logs");
    static final Path journal = logs.resolve("complete-execution.log");
    static final String origin = "http://127.0.0.1:8080";
    static final String base = origin + "/api/v1/offerings/CS101-FALL";
    static final String[] ids = {"learner-1001", "learner-1002", "learner-1003", "learner-1042"};
    static final List<Map<String,Object>> allRequests = new ArrayList<>(), allResponses = new ArrayList<>();
    static final List<CaseData> cases = new ArrayList<>();
    static int sequence;
    static CaseData current;
    static String phase;

    public static class CaseData {
        public String id, name, folder, status = "PASS", reason = "";
        public List<String> failures = new ArrayList<>();
        public List<Map<String,Object>> requests = new ArrayList<>(), responses = new ArrayList<>(), snapshots = new ArrayList<>();
        CaseData(String id, String name, String folder) { this.id=id; this.name=name; this.folder=folder; }
    }
    static class Raw {
        String method, url, requestBody, requestTime, responseTime, responseBody, error;
        int status;
        Map<String,String> requestHeaders = new LinkedHashMap<>();
        Map<String,List<String>> responseHeaders = new LinkedHashMap<>();
    }
    static class Result {
        Raw raw; JsonNode body;
        Result(Raw raw, JsonNode body) { this.raw=raw; this.body=body; }
    }
    interface Scenario { void run() throws Exception; }

    static String now() { return OffsetDateTime.now().toString(); }
    static Map<String,Object> fields(Object... values) {
        Map<String,Object> m = new LinkedHashMap<>();
        for (int i=0; i<values.length; i+=2) m.put((String)values[i], values[i+1]);
        return m;
    }
    static void writeJson(Path path, Object value) throws IOException {
        Files.createDirectories(path.getParent());
        json.writeValue(path.toFile(), value);
    }
    static void note(String text) throws IOException {
        String line=now()+" "+text;
        Files.writeString(journal,line+System.lineSeparator(),StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,StandardOpenOption.APPEND);
        System.out.println(line);
    }
    static void check(boolean ok, String message) throws IOException {
        if (!ok) { current.failures.add(message); note("FAIL "+current.id+" "+message); }
    }
    static String line(InputStream in) throws IOException {
        ByteArrayOutputStream out=new ByteArrayOutputStream();
        int prev=-1, next;
        while ((next=in.read())!=-1) {
            if (prev=='\r' && next=='\n') {
                byte[] b=out.toByteArray();
                return new String(b,0,b.length-1,StandardCharsets.ISO_8859_1);
            }
            out.write(next); prev=next;
        }
        throw new EOFException("Incomplete HTTP header");
    }
    static byte[] readBody(InputStream in, Map<String,List<String>> headers) throws IOException {
        String transfer = header(headers,"Transfer-Encoding");
        if (transfer.toLowerCase(Locale.ROOT).contains("chunked")) {
            ByteArrayOutputStream out=new ByteArrayOutputStream();
            while (true) {
                String sizeLine=line(in).split(";",2)[0].trim();
                int size=Integer.parseInt(sizeLine,16);
                if (size==0) { while (!line(in).isEmpty()) {} break; }
                byte[] chunk=in.readNBytes(size);
                if (chunk.length!=size) throw new EOFException("Incomplete chunk");
                out.write(chunk);
                if (!line(in).isEmpty()) throw new IOException("Malformed chunk terminator");
            }
            return out.toByteArray();
        }
        String length=header(headers,"Content-Length");
        if (!length.isEmpty()) {
            int n=Integer.parseInt(length); byte[] bytes=in.readNBytes(n);
            if (bytes.length!=n) throw new EOFException("Incomplete response body");
            return bytes;
        }
        return in.readAllBytes();
    }
    static String header(Map<String,List<String>> headers,String name) {
        return headers.entrySet().stream().filter(e -> e.getKey().equalsIgnoreCase(name))
                .map(e -> String.join(", ",e.getValue())).findFirst().orElse("");
    }
    static Raw sendRaw(String method,String url,String body) {
        Raw r=new Raw(); r.method=method; r.url=url; r.requestBody=body;
        URI uri=URI.create(url);
        r.requestHeaders.put("Host",uri.getAuthority());
        r.requestHeaders.put("Accept","application/json, application/problem+json");
        r.requestHeaders.put("User-Agent","course-waitlist-live-evidence/1.0");
        r.requestHeaders.put("Connection","close");
        byte[] payload=body==null ? new byte[0] : body.getBytes(StandardCharsets.UTF_8);
        if (body!=null) {
            r.requestHeaders.put("Content-Type","application/json; charset=utf-8");
            r.requestHeaders.put("Content-Length",Integer.toString(payload.length));
        }
        r.requestTime=now();
        try (Socket socket=new Socket()) {
            socket.connect(new InetSocketAddress(uri.getHost(),uri.getPort()),10000);
            socket.setSoTimeout(15000);
            OutputStream out=socket.getOutputStream();
            StringBuilder request=new StringBuilder(method+" "+uri.getRawPath()+" HTTP/1.1\r\n");
            r.requestHeaders.forEach((k,v)->request.append(k).append(": ").append(v).append("\r\n"));
            out.write((request+"\r\n").getBytes(StandardCharsets.ISO_8859_1)); out.write(payload); out.flush();
            InputStream in=new BufferedInputStream(socket.getInputStream());
            String statusLine=line(in);
            r.status=Integer.parseInt(statusLine.split(" ",3)[1]);
            String h;
            while (!(h=line(in)).isEmpty()) {
                int colon=h.indexOf(':');
                if (colon<1) throw new IOException("Malformed response header: "+h);
                r.responseHeaders.computeIfAbsent(h.substring(0,colon),k->new ArrayList<>()).add(h.substring(colon+1).trim());
            }
            r.responseBody=new String(readBody(in,r.responseHeaders),StandardCharsets.UTF_8);
        } catch (Exception e) { r.error=e.toString(); }
        r.responseTime=now();
        return r;
    }
    static Result record(Raw raw,String step,int[] statuses,Map<String,Object> expected) throws IOException {
        JsonNode body=null;
        if (raw.responseBody!=null && !raw.responseBody.isEmpty()) {
            try { body=json.readTree(raw.responseBody); } catch (Exception ignored) {}
        }
        List<String> errors=new ArrayList<>();
        if (raw.error!=null) errors.add("Transport: "+raw.error);
        if (Arrays.stream(statuses).noneMatch(s->s==raw.status))
            errors.add("Expected "+Arrays.toString(statuses)+"; got "+raw.status);
        for (var entry:expected.entrySet()) {
            JsonNode n=body==null ? null : body.get(entry.getKey());
            Object actual=n==null || n.isNull() ? null : json.convertValue(n,Object.class);
            if (n==null || !Objects.equals(actual,entry.getValue()))
                errors.add("Expected "+entry.getKey()+"="+entry.getValue()+"; got "+(n==null?"<missing>":actual));
        }
        if (raw.status>=400 && raw.status<500) {
            if (!header(raw.responseHeaders,"Content-Type").startsWith("application/problem+json"))
                errors.add("Expected application/problem+json");
            for (String key:List.of("type","title","status","detail","instance"))
                if (body==null || !body.has(key)) errors.add("Problem response missing "+key);
            if (body!=null && body.has("status") && body.get("status").asInt()!=raw.status)
                errors.add("Problem status differs from HTTP status");
        }
        int n=++sequence;
        Map<String,Object> request=fields("sequence",n,"testCaseId",current.id,"scenario",current.name,
                "phase",phase,"step",step,"method",raw.method,"url",raw.url,"headers",raw.requestHeaders,
                "body",raw.requestBody,"timestamp",raw.requestTime,"expected",fields("httpStatus",statuses,"responseFields",expected));
        Map<String,Object> response=fields("sequence",n,"testCaseId",current.id,"scenario",current.name,
                "phase",phase,"step",step,"httpStatus",raw.status==0?null:raw.status,"headers",raw.responseHeaders,
                "body",raw.responseBody,"timestamp",raw.responseTime,"transportError",raw.error,
                "actual",errors.isEmpty()?"HTTP status and specified fields matched":String.join("; ",errors),
                "result",errors.isEmpty()?"PASS":"FAIL");
        allRequests.add(request); allResponses.add(response); current.requests.add(request); current.responses.add(response);
        for (String error:errors) current.failures.add(step+": "+error);
        Path exchange=root.resolve(current.folder.replace('/','\\')).resolve("exchanges").resolve(String.format("%04d.json",n));
        writeJson(exchange,fields("request",request,"response",response));
        note(current.id+" "+phase+" "+step+" "+raw.method+" "+raw.url+" -> "+raw.status+" "+response.get("result"));
        return new Result(raw,body);
    }
    static Result send(String step,String method,String url,String body,int[] statuses,Map<String,Object> expected) throws IOException {
        return record(sendRaw(method,url,body),step,statuses,expected);
    }
    static Result post(String id,int status,Map<String,Object> expected) throws IOException {
        Result r=send("register "+id,"POST",base+"/registrations",json.writeValueAsString(fields("learnerId",id)),new int[]{status},expected);
        if (phase.equals("scenario")) snapshot("after register "+id);
        return r;
    }
    static Result drop(String id,int status,Map<String,Object> expected) throws IOException {
        Result r=send("drop "+id,"DELETE",base+"/enrollments/"+id,null,new int[]{status},expected);
        if (phase.equals("scenario")) snapshot("after drop "+id);
        return r;
    }
    static Map<String,Object> snapshot(String label) throws IOException {
        List<Map<String,Object>> entries=new ArrayList<>(), waiting=new ArrayList<>();
        int enrolled=0;
        for (String id:ids) {
            Result r=send("snapshot "+label+" "+id,"GET",base+"/registrations/"+id,null,new int[]{200,404},fields());
            String status=r.raw.status==200 && r.body!=null ? r.body.path("status").asText() : "NOT_FOUND";
            Integer pos=r.body!=null && r.body.hasNonNull("waitlistPosition") ? r.body.get("waitlistPosition").asInt() : null;
            if (r.raw.status==200) check(r.body!=null && id.equals(r.body.path("learnerId").asText()),label+" learner ID mismatch");
            Map<String,Object> entry=fields("learnerId",id,"status",status,"waitlistPosition",pos);
            entries.add(entry);
            if (status.equals("ENROLLED")) enrolled++;
            if (status.equals("WAITLISTED")) waiting.add(entry);
        }
        waiting.sort(Comparator.comparingInt(e -> (Integer)e.get("waitlistPosition")));
        check(enrolled<=2,label+" exceeded configured capacity");
        for (int i=0;i<waiting.size();i++) check(Objects.equals(waiting.get(i).get("waitlistPosition"),i+1),label+" queue position mismatch");
        List<String> order=waiting.stream().map(e->(String)e.get("learnerId")).toList();
        Map<String,Object> snap=fields("label",label,"timestamp",now(),"configuredCapacity",2,
                "enrolledCount",enrolled,"remainingCapacity",2-enrolled,"waitlistOrder",order,"learners",entries,
                "limitation","HTTP-observable fixture registrations; not a direct internal-store or database query.");
        current.snapshots.add(snap); return snap;
    }
    static void reset() throws IOException {
        phase="cleanup";
        for (int i=0;i<5;i++) {
            Map<String,Object> state=snapshot("cleanup-"+i);
            @SuppressWarnings("unchecked") List<Map<String,Object>> rows=(List<Map<String,Object>>)state.get("learners");
            Optional<Map<String,Object>> enrolled=rows.stream().filter(r->r.get("status").equals("ENROLLED")).findFirst();
            if (enrolled.isEmpty()) {
                check(((List<?>)state.get("waitlistOrder")).isEmpty(),"Cleanup left a waiting learner");
                return;
            }
            drop((String)enrolled.get().get("learnerId"),200,fields());
        }
        check(false,"Cleanup did not clear registrations");
    }
    static List<Result> concurrent(String method,List<String> urls,List<String> bodies,int[] statuses,Map<String,Object> expected) throws Exception {
        int n=urls.size(); CyclicBarrier barrier=new CyclicBarrier(n);
        ExecutorService pool=Executors.newFixedThreadPool(n);
        try {
            List<Future<Raw>> tasks=new ArrayList<>();
            for (int i=0;i<n;i++) {
                final int index=i;
                tasks.add(pool.submit(()->{ barrier.await(15,TimeUnit.SECONDS); return sendRaw(method,urls.get(index),bodies.get(index)); }));
            }
            List<Result> results=new ArrayList<>();
            for (int i=0;i<n;i++) results.add(record(tasks.get(i).get(30,TimeUnit.SECONDS),"concurrent request "+(i+1),statuses,expected));
            boolean overlaps=false;
            for (int i=0;i<n;i++) for (int j=i+1;j<n;j++) {
                Raw a=results.get(i).raw,b=results.get(j).raw;
                if (OffsetDateTime.parse(a.requestTime).isBefore(OffsetDateTime.parse(b.responseTime))
                        && OffsetDateTime.parse(b.requestTime).isBefore(OffsetDateTime.parse(a.responseTime))) overlaps=true;
            }
            check(overlaps,"Concurrent request intervals did not overlap");
            snapshot("after concurrent requests");
            return results;
        } finally { pool.shutdownNow(); pool.awaitTermination(15,TimeUnit.SECONDS); }
    }
    static void scenario(String id,String name,String folder,Scenario operation) throws IOException {
        current=new CaseData(id,name,folder); note("BEGIN "+id+" "+name);
        try { reset(); phase="before"; snapshot("before"); phase="scenario"; operation.run(); phase="after"; snapshot("after"); }
        catch (Exception e) { current.failures.add(e.toString()); note("ERROR "+id+" "+e); }
        if (!current.failures.isEmpty()) current.status="FAIL";
        saveCase(); note("END "+id+" "+current.status);
    }
    static void saveCase() throws IOException {
        Path dir=root.resolve(current.folder.replace('/','\\'));
        writeJson(dir.resolve("requests.json"),current.requests);
        writeJson(dir.resolve("responses.json"),current.responses);
        writeJson(dir.resolve("scenario-responses.json"),current.responses.stream().filter(r->r.get("phase").equals("scenario")).toList());
        writeJson(dir.resolve("execution.json"),current);
        cases.add(current);
    }
    static List<String> repeat(String s,int n) { return Collections.nCopies(n,s); }
    static List<String> empty(int n) { return new ArrayList<>(Collections.nCopies(n,null)); }
    static long count(List<Result> results,String key,String value) {
        return results.stream().filter(r->r.body!=null && r.body.path(key).asText().equals(value)).count();
    }
    public static void main(String[] args) throws Exception {
        Files.createDirectories(logs);
        scenario("TC01","Successful enrollment","01-enrollment/TC01-successful-enrollment",()->post(ids[0],201,fields("status","ENROLLED","created",true)));
        scenario("TC02","Course full waitlist","01-enrollment/TC02-course-full-waitlist",()->{
            post(ids[0],201,fields("status","ENROLLED")); post(ids[1],201,fields("status","ENROLLED")); post(ids[2],201,fields("status","WAITLISTED","waitlistPosition",1)); });
        scenario("TC03","Duplicate enrollment and waitlisting","01-enrollment/TC03-duplicate-enrollment",()->{
            post(ids[0],201,fields("created",true)); post(ids[0],200,fields("status","ENROLLED","created",false));
            post(ids[1],201,fields("status","ENROLLED")); post(ids[2],201,fields("status","WAITLISTED","waitlistPosition",1));
            post(ids[2],200,fields("status","WAITLISTED","waitlistPosition",1,"created",false)); });
        scenario("TC04","Ineligible learner","01-enrollment/TC04-ineligible-learner",()->{
            post("not-eligible",422,fields("status",422,"type","urn:course-waitlist:ineligible"));
            send("lookup rejected learner","GET",base+"/registrations/not-eligible",null,new int[]{404},fields("status",404)); });
        scenario("TC05","Unknown offering","01-enrollment/TC05-invalid-course",()->
            send("unknown offering","POST",origin+"/api/v1/offerings/unknown/registrations","{\"learnerId\":\"learner-1001\"}",new int[]{404},fields("status",404)));
        scenario("TC06","Invalid learner ID","01-enrollment/TC06-invalid-learner",()->
            send("blank learner","POST",base+"/registrations","{\"learnerId\":\" \"}",new int[]{400},fields("status",400)));
        scenario("TC07","Successful drop","02-course-dropping/TC07-successful-drop",()->{
            post(ids[0],201,fields("status","ENROLLED")); drop(ids[0],200,fields("droppedLearnerId",ids[0],"remainingCapacity",2,"promotedLearnerId",null)); });
        scenario("TC08","Drop when not enrolled","02-course-dropping/TC08-invalid-drop",()->drop(ids[0],409,fields("status",409)));
        scenario("TC09","Repeated drop","02-course-dropping/TC09-repeated-drop",()->{
            post(ids[0],201,fields("status","ENROLLED")); drop(ids[0],200,fields("droppedLearnerId",ids[0])); drop(ids[0],409,fields("status",409)); });
        scenario("TC10","Drop with empty waitlist","02-course-dropping/TC10-drop-with-empty-waitlist",()->{
            post(ids[0],201,fields("status","ENROLLED")); post(ids[1],201,fields("status","ENROLLED"));
            drop(ids[0],200,fields("promotedLearnerId",null,"skippedIneligibleCount",0,"remainingCapacity",1)); });
        scenario("TC11","FIFO promotion","03-waitlist-promotion/TC11-fifo-promotion",()->{
            four(); snapshot("queue-before-drop"); drop(ids[0],200,fields("promotedLearnerId",ids[2],"remainingCapacity",0));
            Map<String,Object> s=snapshot("first-promotion"); check(s.get("waitlistOrder").equals(List.of(ids[3])),"Remaining FIFO queue mismatch"); });
        current=new CaseData("TC12","First waiter becomes ineligible","03-waitlist-promotion/TC12-ineligible-first-learner");
        current.status="NOT EXECUTED"; current.reason="The running application uses an immutable eligibility fixture and exposes no API to change eligibility after waitlisting. No mock or business-logic change was used.";
        saveCase(); note("END TC12 NOT EXECUTED: "+current.reason);
        scenario("TC13","Multiple waitlisted learners","03-waitlist-promotion/TC13-multiple-waitlisted-learners",()->{
            four(); Map<String,Object> s=snapshot("multiple-waiters"); check(s.get("waitlistOrder").equals(List.of(ids[2],ids[3])),"Multiple-waiter order mismatch"); });
        scenario("TC14","Multiple sequential promotions","03-waitlist-promotion/TC14-promotion-after-drop",()->{
            four(); drop(ids[0],200,fields("promotedLearnerId",ids[2])); drop(ids[1],200,fields("promotedLearnerId",ids[3])); });
        scenario("TC15","Simultaneous enrollment for final seat","04-concurrency/TC15-simultaneous-enrollment",()->{
            post(ids[0],201,fields("status","ENROLLED"));
            List<String> bodies=List.of("{\"learnerId\":\"learner-1002\"}","{\"learnerId\":\"learner-1003\"}","{\"learnerId\":\"learner-1042\"}");
            List<Result> r=concurrent("POST",repeat(base+"/registrations",3),bodies,new int[]{201},fields("created",true));
            check(count(r,"status","ENROLLED")==1,"Expected exactly one last-seat enrollment");
            check(count(r,"status","WAITLISTED")==2,"Expected exactly two waitlisted learners"); });
        scenario("TC16","Duplicate concurrent enrollment","04-concurrency/TC16-duplicate-concurrent-enrollment",()->{
            post(ids[0],201,fields("status","ENROLLED"));
            List<Result> r=concurrent("POST",repeat(base+"/registrations",8),repeat("{\"learnerId\":\"learner-1002\"}",8),new int[]{200,201},fields("status","ENROLLED"));
            check(r.stream().filter(x->x.raw.status==201 && x.body!=null && x.body.path("created").asBoolean()).count()==1,"Expected one created registration");
            check(r.stream().filter(x->x.raw.status==200 && x.body!=null && !x.body.path("created").asBoolean()).count()==7,"Expected seven idempotent responses"); });
        scenario("TC17","Simultaneous drops","04-concurrency/TC17-simultaneous-drops",()->{
            post(ids[0],201,fields("status","ENROLLED")); post(ids[1],201,fields("status","ENROLLED"));
            List<Result> r=concurrent("DELETE",List.of(base+"/enrollments/"+ids[0],base+"/enrollments/"+ids[1]),empty(2),new int[]{200},fields("promotedLearnerId",null));
            check(count(r,"droppedLearnerId",ids[0])==1 && count(r,"droppedLearnerId",ids[1])==1,"Expected both drops exactly once"); });
        scenario("TC18","Concurrent promotion opportunities","04-concurrency/TC18-concurrent-promotion",()->{
            four(); snapshot("queue-before-concurrent-drops");
            List<Result> r=concurrent("DELETE",List.of(base+"/enrollments/"+ids[0],base+"/enrollments/"+ids[1]),empty(2),new int[]{200},fields("remainingCapacity",0));
            check(count(r,"promotedLearnerId",ids[2])==1 && count(r,"promotedLearnerId",ids[3])==1,"Expected both waiters promoted exactly once"); });
        scenario("TC19","Missing mandatory fields","05-validation-and-errors/TC19-missing-fields",()->
            send("missing learner ID","POST",base+"/registrations","{}",new int[]{400},fields("status",400)));
        scenario("TC20","Malformed request","05-validation-and-errors/TC20-invalid-request",()->{
            send("malformed JSON","POST",base+"/registrations","{",new int[]{400},fields("status",400));
            send("numeric learner ID","POST",base+"/registrations","{\"learnerId\":123}",new int[]{400},fields("status",400)); });
        scenario("TC21","Invalid identifiers","05-validation-and-errors/TC21-invalid-identifiers",()->{
            send("surrounding whitespace","POST",base+"/registrations","{\"learnerId\":\" learner-1001\"}",new int[]{400},fields("status",400));
            send("identifier longer than 100","POST",base+"/registrations",json.writeValueAsString(fields("learnerId","x".repeat(101))),new int[]{400},fields("status",400)); });
        scenario("TC22","Unsupported method","05-validation-and-errors/TC22-unsupported-method",()->{
            Result r=send("delete lookup endpoint","DELETE",base+"/registrations/"+ids[0],null,new int[]{405},fields("status",405));
            check(header(r.raw.responseHeaders,"Allow").contains("GET"),"405 response did not advertise GET"); });
        writeJson(logs.resolve("all-api-requests.json"),allRequests);
        writeJson(logs.resolve("all-api-responses.json"),allResponses);
        writeJson(logs.resolve("scenario-summary.json"),cases.stream().map(c->fields("id",c.id,"name",c.name,"status",c.status,"reason",c.reason,"failures",c.failures,"requestCount",c.requests.size(),"folder",c.folder)).toList());
        long pass=cases.stream().filter(c->c.status.equals("PASS")).count();
        long fail=cases.stream().filter(c->c.status.equals("FAIL")).count();
        long notExecuted=cases.stream().filter(c->c.status.equals("NOT EXECUTED")).count();
        note("TOTAL scenarios="+cases.size()+" pass="+pass+" fail="+fail+" not-executed="+notExecuted+" requests="+allRequests.size());
        if (fail>0) System.exit(1);
    }
    static void four() throws IOException {
        post(ids[0],201,fields("status","ENROLLED")); post(ids[1],201,fields("status","ENROLLED"));
        post(ids[2],201,fields("status","WAITLISTED","waitlistPosition",1)); post(ids[3],201,fields("status","WAITLISTED","waitlistPosition",2));
    }
}

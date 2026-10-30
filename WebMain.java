package college.web;

import java.io.IOException;
import java.net.BindException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import college.mediator.CollegeNotificationMediator;
import college.model.Administrator;
import college.model.CollegeModel;
import college.model.Course;
import college.model.Faculty;
import college.model.Student;
import college.model.TimetableConfig;
import college.prototype.PrototypeRegistry;
import college.strategy.GradeCalculator;

/** HTTP adapter that exposes the existing College Management domain model to the website. */
public final class WebMain {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static JsonObject state = seedState();
    private static int deliveredNotificationCount = 0;
    private static final Path WEB_ROOT = Path.of("web").toAbsolutePath().normalize();
    private static final Path DATA_FILE = Path.of("data", "state.json").toAbsolutePath().normalize();

    public static void main(String[] args) throws Exception {
        if (Files.isRegularFile(DATA_FILE)) {
            try { state = JsonParser.parseString(Files.readString(DATA_FILE, StandardCharsets.UTF_8)).getAsJsonObject(); }
            catch (Exception e) { System.err.println("Saved data could not be read; using sample data: " + e.getMessage()); }
        }
        normalizeLegacyGradingPolicies();
        deliveredNotificationCount = array(state, "notifications").size();
        String configuredPort = System.getenv("PORT");
        int port = configuredPort == null || configuredPort.isBlank() ? 8080 : Integer.parseInt(configuredPort);
        if (port < 0 || port > 65535) {
            throw new IllegalArgumentException("PORT must be between 0 and 65535.");
        }
        HttpServer server;
        try {
            server = HttpServer.create(new InetSocketAddress("127.0.0.1", port), 0);
        } catch (BindException e) {
            if (port == 0) throw e;
            server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            System.err.println("Port " + port + " is already in use; using available port "
                    + server.getAddress().getPort() + " instead.");
        }
        int actualPort = server.getAddress().getPort();
        server.createContext("/api/health", ex -> respond(ex, 200, json(Map.of("status", "ok", "service", "College Management System API"))));
        server.createContext("/api/state", WebMain::handleState);
        server.createContext("/", WebMain::serveStatic);
        server.setExecutor(java.util.concurrent.Executors.newCachedThreadPool());
        server.start();
        System.out.println("College Management System website: http://localhost:" + actualPort);
        System.out.println("API health check: http://localhost:" + actualPort + "/api/health");
    }

    private static void normalizeLegacyGradingPolicies() throws IOException {
        boolean changed = false;
        for (JsonElement element : array(state, "courses")) {
            if (!element.isJsonObject()) continue;
            JsonObject course = element.getAsJsonObject();
            if ("relative".equalsIgnoreCase(str(course, "policy"))) {
                course.addProperty("policy", "absolute");
                changed = true;
            }
        }
        if (changed && Files.isRegularFile(DATA_FILE)) {
            Files.writeString(DATA_FILE, GSON.toJson(state), StandardCharsets.UTF_8);
        }
    }

    private static synchronized void handleState(HttpExchange ex) throws IOException {
        if ("GET".equalsIgnoreCase(ex.getRequestMethod())) {
            respond(ex, 200, state.toString());
            return;
        }
        if (!"POST".equalsIgnoreCase(ex.getRequestMethod())) {
            respond(ex, 405, json(Map.of("error", "Method not allowed")));
            return;
        }
        try {
            String body = new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            JsonObject candidate = JsonParser.parseString(body).getAsJsonObject();
            validateAndApplyThroughDomain(candidate);
            Files.createDirectories(DATA_FILE.getParent());
            Files.writeString(DATA_FILE, GSON.toJson(candidate), StandardCharsets.UTF_8);
            state = candidate;
            respond(ex, 200, json(Map.of("ok", true, "message", "Saved through Java backend")));
        } catch (Exception err) {
            respond(ex, 400, json(Map.of("error", err.getMessage() == null ? "Invalid request" : err.getMessage())));
        }
    }

    /** Rebuilds and validates the domain state using the actual Java Builder, Prototype, Strategy, MVC model and Mediator. */
    private static void validateAndApplyThroughDomain(JsonObject s) {
        CollegeModel model = new CollegeModel();
        CollegeNotificationMediator mediator = new CollegeNotificationMediator();
        Administrator admin = new Administrator("ADM01", "Registrar Office");
        model.setAdmin(admin);
        mediator.register(admin);
        Map<String, Faculty> facultyMap = new LinkedHashMap<>();
        for (JsonElement el : array(s, "faculty")) {
            JsonObject f = el.getAsJsonObject();
            Faculty faculty = new Faculty(str(f, "id"), str(f, "name"), str(f, "dept"));
            model.addFaculty(faculty); mediator.register(faculty); facultyMap.put(faculty.getId(), faculty);
        }
        PrototypeRegistry<Course> templates = new PrototypeRegistry<>();
        templates.register("THEORY-4CR", new Course.Builder("TEMPLATE", "Theory Course", 4).maxSeats(60).gradingPolicy("absolute").topic("Unit 1").build());
        templates.register("LAB-2CR", new Course.Builder("TEMPLATE", "Lab Course", 2).maxSeats(30).gradingPolicy("passfail").topic("Practical").build());
        for (JsonElement el : array(s, "students")) {
            JsonObject x = el.getAsJsonObject();
            Student.Builder b = new Student.Builder(str(x,"roll"), str(x,"name"), str(x,"dept"), integer(x,"semester",1));
            b.email(str(x,"email")).phone(str(x,"phone")).hostelRequired("Yes".equalsIgnoreCase(str(x,"hostel"))).scholarshipPercent(number(x,"scholarship",0));
            Student student = b.build(); model.addStudent(student); mediator.register(student);
        }
        for (JsonElement el : array(s, "courses")) {
            JsonObject x = el.getAsJsonObject();
            String code = str(x,"code"), title = str(x,"title"), policy = str(x,"policy").toLowerCase();
            GradeCalculator.strategyFor(policy); // Strategy policy validation
            int credits = integer(x,"credits",1), seats = integer(x,"seats",60);
            String facultyId = str(x,"faculty"), term = str(x,"term");
            Course course;
            String templateKey = credits <= 2 || "passfail".equals(policy) ? "LAB-2CR" : "THEORY-4CR";
            try { course = templates.create(templateKey); course.setCode(code); course.setTitle(title); course.setFacultyId(facultyId); course.setTerm(term); }
            catch (RuntimeException ignored) { course = new Course.Builder(code,title,credits).faculty(facultyId).maxSeats(seats).term(term).gradingPolicy(policy).build(); }
            if (course.getCredits() != credits || course.getMaxSeats() != seats || !course.getGradingPolicy().equals(policy))
                course = new Course.Builder(code,title,credits).faculty(facultyId).maxSeats(seats).term(term).gradingPolicy(policy).build();
            model.addCourse(course);
        }
        JsonObject enrollments = obj(s, "enrollments");
        for (Map.Entry<String, JsonElement> entry : enrollments.entrySet()) {
            if (!entry.getValue().isJsonArray()) continue;
            for (JsonElement code : entry.getValue().getAsJsonArray()) model.enroll(entry.getKey(), code.getAsString());
        }
        JsonObject marks = obj(s, "marks");
        for (Map.Entry<String, JsonElement> e : marks.entrySet()) {
            String[] key = e.getKey().split("-", 2);
            if (key.length == 2) model.recordMarks(key[0], key[1], e.getValue().getAsDouble());
        }
        JsonObject attendance = obj(s, "attendance");
        for (Map.Entry<String, JsonElement> e : attendance.entrySet()) {
            String[] key = e.getKey().split("-", 2); if (key.length != 2 || !e.getValue().isJsonObject()) continue;
            JsonObject a = e.getValue().getAsJsonObject(); int present = integer(a,"present",0), total = integer(a,"total",0);
            for (int i=0;i<total;i++) model.markAttendance(key[0],key[1],i<present);
        }
        JsonObject fees = obj(s, "fees");
        for (JsonElement el : array(s,"students")) {
            JsonObject student = el.getAsJsonObject(); String roll = str(student,"roll");
            JsonObject fee = fees.has(roll) && fees.get(roll).isJsonObject() ? fees.getAsJsonObject(roll) : new JsonObject();
            double paid = number(fee,"paid",0);
            if (paid > 0) model.getFees(roll).pay(paid);
        }
        PrototypeRegistry<TimetableConfig> timetableTemplates = new PrototypeRegistry<>();
        timetableTemplates.register("REGULAR-WEEK", new TimetableConfig("Template", "Template").assign("Mon 09:00", "Lecture").assign("Wed 14:00", "Lab"));
        for (JsonElement el : array(s,"timetables")) {
            JsonObject t = el.getAsJsonObject(); TimetableConfig config;
            try { config = timetableTemplates.create("REGULAR-WEEK"); config.setName(str(t,"name")); config.setTerm(str(t,"term")); }
            catch (RuntimeException ignored) { config = new TimetableConfig(str(t,"name"),str(t,"term")); }
            if (t.has("slots") && t.get("slots").isJsonArray()) {
                for (JsonElement slotElement : t.getAsJsonArray("slots")) {
                    if (slotElement.isJsonArray() && slotElement.getAsJsonArray().size() >= 2) {
                        JsonArray slot = slotElement.getAsJsonArray();
                        config.assign(slot.get(0).getAsString(), slot.get(1).getAsString());
                    } else if (slotElement.isJsonObject()) {
                        JsonObject slot = slotElement.getAsJsonObject();
                        String day = str(slot, "day");
                        String start = str(slot, "startTime");
                        String end = str(slot, "endTime");
                        String code = str(slot, "courseCode");
                        if (!day.isBlank() && !start.isBlank() && !code.isBlank()) {
                            String time = day + " " + start + (end.isBlank() ? "" : "-" + end)
                                    + " · Room " + str(slot, "room")
                                    + " · Section " + str(slot, "section");
                            String details = code + " — " + str(slot, "courseTitle")
                                    + " | " + str(slot, "faculty")
                                    + " | Room " + str(slot, "room")
                                    + " | Section " + str(slot, "section");
                            config.assign(time, details);
                        }
                    }
                }
            }
            model.addTimetable(config);
        }
        JsonArray notifications = array(s,"notifications");
        if (notifications.size() > deliveredNotificationCount) {
            JsonObject n = notifications.get(notifications.size()-1).getAsJsonObject();
            String recipient = str(n,"to"), message = str(n,"message");
            if (!message.isBlank()) {
                if (recipient.toLowerCase().contains("faculty")) admin.sendToRole("Faculty", message);
                else if (recipient.toLowerCase().contains("administration")) admin.sendTo(admin, message);
                else admin.broadcast(message);
            }
            deliveredNotificationCount = notifications.size();
        }
    }

    private static void serveStatic(HttpExchange ex) throws IOException {
        String raw = ex.getRequestURI().getPath();
        if (raw.startsWith("/api/")) { respond(ex,404,json(Map.of("error","Not found"))); return; }
        if (raw.equals("/")) raw = "/index.html";
        Path file = WEB_ROOT.resolve(raw.substring(1)).normalize();
        if (!file.startsWith(WEB_ROOT) || !Files.isRegularFile(file)) { respond(ex,404,"Not found"); return; }
        String type = file.toString().endsWith(".css") ? "text/css; charset=utf-8" : file.toString().endsWith(".js") ? "application/javascript; charset=utf-8" : "text/html; charset=utf-8";
        ex.getResponseHeaders().set("Content-Type", type);
        byte[] bytes = Files.readAllBytes(file); ex.sendResponseHeaders(200, bytes.length); ex.getResponseBody().write(bytes); ex.close();
    }

    private static void respond(HttpExchange ex, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        ex.getResponseHeaders().set("Cache-Control", "no-store");
        ex.sendResponseHeaders(status, bytes.length); ex.getResponseBody().write(bytes); ex.close();
    }
    private static String json(Object o) { return GSON.toJson(o); }
    private static JsonArray array(JsonObject o, String key) { return o.has(key) && o.get(key).isJsonArray() ? o.getAsJsonArray(key) : new JsonArray(); }
    private static JsonObject obj(JsonObject o, String key) { return o.has(key) && o.get(key).isJsonObject() ? o.getAsJsonObject(key) : new JsonObject(); }
    private static String str(JsonObject o, String key) { return o.has(key) && !o.get(key).isJsonNull() ? o.get(key).getAsString() : ""; }
    private static int integer(JsonObject o, String key, int d) { try { return o.has(key) ? o.get(key).getAsInt() : d; } catch (Exception e) { return d; } }
    private static double number(JsonObject o, String key, double d) { try { return o.has(key) ? o.get(key).getAsDouble() : d; } catch (Exception e) { return d; } }

    private static JsonObject seedState() {
        return JsonParser.parseString("""
        {"students":[{"roll":"S101","name":"Aarav Deshmukh","dept":"CSE","semester":5,"email":"aarav@college.edu","phone":"9876543210","hostel":"No","scholarship":0},{"roll":"S102","name":"Priya Sharma","dept":"CSE","semester":5,"email":"priya@college.edu","phone":"9876543211","hostel":"Yes","scholarship":20},{"roll":"S103","name":"Rohan Patil","dept":"IT","semester":3,"email":"rohan@college.edu","phone":"9876543212","hostel":"No","scholarship":0},{"roll":"S104","name":"Sneha Kulkarni","dept":"ENTC","semester":5,"email":"sneha@college.edu","phone":"9876543213","hostel":"Yes","scholarship":10}],"courses":[{"code":"CS301","title":"Design Patterns","credits":4,"faculty":"F101","term":"Odd 2026","policy":"absolute","seats":60},{"code":"MA201","title":"Discrete Mathematics","credits":4,"faculty":"F102","term":"Odd 2026","policy":"relative","seats":60},{"code":"CS3L1","title":"Design Patterns Lab","credits":2,"faculty":"F101","term":"Odd 2026","policy":"passfail","seats":30}],"faculty":[{"id":"F101","name":"Dr. Mehta","dept":"CSE"},{"id":"F102","name":"Prof. Iyer","dept":"Mathematics"}],"enrollments":{"S101":["CS301","CS3L1"],"S102":["CS301","MA201"],"S103":["MA201"],"S104":["CS301","CS3L1"]},"attendance":{},"marks":{"S101-CS301":88,"S102-CS301":76,"S104-CS301":92,"S102-MA201":81,"S103-MA201":67},"fees":{"S101":{"paid":30000},"S102":{"paid":40000},"S103":{"paid":15000},"S104":{"paid":25000}},"timetables":[{"name":"Regular Week","term":"Odd 2026","slots":[["Mon 09:00","Design Patterns"],["Mon 10:00","Discrete Mathematics"],["Wed 14:00","Design Patterns Lab"],["Fri 11:00","Tutorial"]]}],"notifications":[{"to":"All students","message":"Welcome to the new academic term.","date":""}]}
        """).getAsJsonObject();
    }
}

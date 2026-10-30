package college.controller;

import college.mediator.CollegeNotificationMediator;
import college.model.Administrator;
import college.model.CollegeModel;
import college.model.Course;
import college.model.Faculty;
import college.model.FeeAccount;
import college.model.GradeRow;
import college.model.Student;
import college.model.StudentProfileDirector;
import college.model.TimetableConfig;
import college.prototype.PrototypeRegistry;
import college.strategy.AbsoluteGradingStrategy;
import college.strategy.GradeCalculator;
import college.strategy.GradingStrategy;
import college.view.ConsoleView;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * MVC - the CONTROLLER. Reads input from the View, calls the Model, then asks
 * the View to display the result. It also wires the other patterns together:
 *   Builder (student/course creation), Prototype (templates), Strategy
 *   (grade calculation) and Mediator (notifications).
 */
public class CollegeController {
    private final CollegeModel model;
    private final ConsoleView view;
    private final CollegeNotificationMediator mediator = new CollegeNotificationMediator();
    private final PrototypeRegistry<Course> courseTemplates = new PrototypeRegistry<>();
    private final PrototypeRegistry<TimetableConfig> timetableTemplates = new PrototypeRegistry<>();
    private final GradeCalculator calculator = new GradeCalculator(new AbsoluteGradingStrategy());
    private Administrator admin;

    public CollegeController(CollegeModel model, ConsoleView view) {
        this.model = model;
        this.view = view;
        seedSampleData();
    }

    // =====================================================================
    //  Interactive menu
    // =====================================================================
    public void run() {
        view.header("COLLEGE MANAGEMENT SYSTEM");
        view.println("Sample data loaded. Students: S101-S104, courses: CS301, MA201, CS3L1.");
        boolean running = true;
        while (running) {
            view.showMenu();
            try {
                String choice = view.ask("Choose");
                switch (choice) {
                    case "1" -> registerStudent();
                    case "2" -> createCustomCourse();
                    case "3" -> cloneCourseFromTemplate();
                    case "4" -> cloneTimetable();
                    case "5" -> enrollStudent();
                    case "6" -> markAttendance();
                    case "7" -> recordMarks();
                    case "8" -> gradeReport();
                    case "9" -> payFees();
                    case "10" -> sendNotification();
                    case "11" -> viewInbox();
                    case "12" -> listRecords();
                    case "0" -> running = false;
                    default -> view.println("Invalid choice, try again.");
                }
            } catch (NoSuchElementException e) {
                running = false; // input stream closed
            } catch (RuntimeException e) {
                view.println("Error: " + e.getMessage());
            }
        }
        view.println("Goodbye!");
    }

    private void registerStudent() {
        view.header("REGISTER STUDENT");
        Student.Builder b = new Student.Builder(
                view.ask("Roll no"), view.ask("Name"), view.ask("Department"), view.askInt("Semester (1-8)"));
        String email = view.ask("Email (Enter to skip)");
        if (!email.isEmpty()) b.email(email);
        String phone = view.ask("Phone (Enter to skip)");
        if (!phone.isEmpty()) b.phone(phone);
        b.hostelRequired(view.askYesNo("Hostel required"));
        b.scholarshipPercent(view.askDouble("Scholarship % (0 for none)"));
        registerStudent(b.build());
        view.println("Student registered.");
    }

    private void createCustomCourse() {
        view.header("CREATE CUSTOM COURSE");
        Course.Builder b = new Course.Builder(view.ask("Course code").toUpperCase(), view.ask("Title"), view.askInt("Credits"));
        b.faculty(view.ask("Faculty id (e.g. F101)"));
        b.maxSeats(view.askInt("Max seats"));
        b.term(view.ask("Term (e.g. Odd 2026)"));
        String policy = view.ask("Grading policy (standard/passfail)").toLowerCase();
        GradeCalculator.strategyFor(policy); // validates the policy name
        b.gradingPolicy("passfail".equals(policy) ? "passfail" : "absolute");
        model.addCourse(b.build());
        view.println("Course created.");
    }

    private void cloneCourseFromTemplate() {
        view.header("CLONE COURSE TEMPLATE");
        view.println("Templates: " + courseTemplates.keys());
        Course c = courseTemplates.create(view.ask("Template"));
        c.setCode(view.ask("New course code").toUpperCase());
        c.setTitle(view.ask("New title"));
        c.setFacultyId(view.ask("Faculty id"));
        c.setTerm(view.ask("Term"));
        model.addCourse(c);
        view.println("Created: " + c);
    }

    private void cloneTimetable() {
        view.header("CLONE TIMETABLE");
        view.println("Templates: " + timetableTemplates.keys());
        TimetableConfig t = timetableTemplates.create(view.ask("Template"));
        t.setName(view.ask("New timetable name"));
        t.setTerm(view.ask("Term"));
        model.addTimetable(t);
        view.showTimetable(t);
    }

    private void enrollStudent() {
        model.enroll(view.ask("Roll no"), view.ask("Course code").toUpperCase());
        view.println("Enrolled.");
    }

    private void markAttendance() {
        String roll = view.ask("Roll no");
        String code = view.ask("Course code").toUpperCase();
        model.markAttendance(roll, code, view.askYesNo("Present"));
        view.println(String.format("Attendance now %.1f%%", model.attendancePercent(roll, code)));
    }

    private void recordMarks() {
        model.recordMarks(view.ask("Roll no"), view.ask("Course code").toUpperCase(), view.askDouble("Marks (%)"));
        view.println("Marks saved.");
    }

    private void gradeReport() {
        String code = view.ask("Course code").toUpperCase();
        Course course = model.requireCourse(code);
        String policy = view.ask("Policy standard/passfail (Enter = "
                + ("passfail".equals(course.getGradingPolicy()) ? "passfail" : "standard") + ")");
        if (policy.isEmpty()) policy = course.getGradingPolicy();
        showGradeReport(code, GradeCalculator.strategyFor(policy));
    }

    private void payFees() {
        String roll = view.ask("Roll no");
        FeeAccount fee = model.getFees(roll);
        view.println(String.format("Total Rs %.0f, paid Rs %.0f, due Rs %.0f", fee.getTotalFee(), fee.getPaid(), fee.due()));
        fee.pay(view.askDouble("Amount to pay"));
        view.println(String.format("Payment recorded. Remaining due: Rs %.0f", fee.due()));
    }

    private void sendNotification() {
        view.header("SEND NOTIFICATION");
        view.println("1. Admin: broadcast to everyone");
        view.println("2. Admin: fee reminders to students with dues");
        view.println("3. Faculty: message all students of a course");
        view.println("4. Student: message a faculty member");
        view.println("5. Student: try to broadcast (to show the mediator blocking it)");
        switch (view.ask("Choose")) {
            case "1" -> view.println("Delivered to " + admin.broadcast(view.ask("Message")) + " participants.");
            case "2" -> view.println("Reminders sent: " + sendFeeReminders());
            case "3" -> {
                Faculty f = model.requireFaculty(view.ask("Faculty id"));
                String code = view.ask("Course code").toUpperCase();
                model.requireCourse(code);
                view.println("Delivered to " + f.sendToGroup(model.enrolledStudents(code), view.ask("Message")) + " students.");
            }
            case "4" -> {
                Student s = model.requireStudent(view.ask("Student roll no"));
                Faculty f = model.requireFaculty(view.ask("Faculty id"));
                view.println("Delivered: " + s.sendTo(f, view.ask("Message")));
            }
            case "5" -> view.println("Delivered: " + model.requireStudent(view.ask("Student roll no")).broadcast("Hello everyone!")
                    + " (0 = blocked by mediator)");
            default -> view.println("Invalid choice.");
        }
    }

    private void viewInbox() {
        var p = model.findParticipant(view.ask("Participant id (roll no / faculty id / ADM01)"));
        view.showInbox(p.getName() + " (" + p.getRole() + ")", p.getInbox());
    }

    private void listRecords() {
        view.showStudents(model.getStudents());
        view.showCourses(model.getCourses(), model);
        model.getTimetables().forEach(view::showTimetable);
    }

    // =====================================================================
    //  Shared helpers
    // =====================================================================
    private void registerStudent(Student s) {
        model.addStudent(s);
        mediator.register(s); // every student becomes a Mediator colleague
    }

    private List<GradeRow> buildGradeRows(String courseCode, GradingStrategy strategy) {
        calculator.setStrategy(strategy); // STRATEGY: swap the algorithm at run time
        double avg = model.classAverage(courseCode);
        List<GradeRow> rows = new ArrayList<>();
        for (Student s : model.enrolledStudents(courseCode)) {
            Double pct = model.getMarks(s.getId(), courseCode);
            if (pct == null) continue;
            String grade = calculator.grade(pct, avg);
            rows.add(new GradeRow(s.getId(), s.getName(), pct, grade, calculator.points(grade)));
        }
        return rows;
    }

    private void showGradeReport(String courseCode, GradingStrategy strategy) {
        List<GradeRow> rows = buildGradeRows(courseCode, strategy);
        view.showGradeReport(courseCode, strategy.getName(), model.classAverage(courseCode), rows);
    }

    private int sendFeeReminders() {
        int sent = 0;
        for (Student s : model.studentsWithDues()) {
            sent += admin.sendTo(s, String.format("Fee reminder: Rs %.0f is due.", model.getFees(s.getId()).due()));
        }
        return sent;
    }

    private void seedSampleData() {
        admin = new Administrator("ADM01", "Registrar Office");
        model.setAdmin(admin);
        mediator.register(admin);

        for (Faculty f : List.of(new Faculty("F101", "Dr. Mehta", "CSE"), new Faculty("F102", "Prof. Iyer", "Mathematics"))) {
            model.addFaculty(f);
            mediator.register(f);
        }

        // PROTOTYPE: register templates once, clone them whenever a new course/timetable is needed
        courseTemplates.register("THEORY-4CR", new Course.Builder("TEMPLATE", "Theory Course", 4)
                .maxSeats(60).gradingPolicy("absolute")
                .topic("Unit 1").topic("Unit 2").topic("Unit 3")
                .weight("Internal", 30).weight("End Semester", 70).build());
        courseTemplates.register("LAB-2CR", new Course.Builder("TEMPLATE", "Lab Course", 2)
                .maxSeats(30).gradingPolicy("passfail")
                .topic("Experiments").weight("Practical", 60).weight("Viva", 40).build());
        timetableTemplates.register("REGULAR-WEEK", new TimetableConfig("Regular Week", "Template")
                .assign("Mon 09:00", "Lecture 1").assign("Mon 10:00", "Lecture 2")
                .assign("Wed 14:00", "Lab Session").assign("Fri 11:00", "Tutorial"));

        addClonedCourse("THEORY-4CR", "CS301", "Design Patterns", "F101");
        addClonedCourse("THEORY-4CR", "MA201", "Discrete Mathematics", "F102");
        addClonedCourse("LAB-2CR", "CS3L1", "Design Patterns Lab", "F101");

        // BUILDER (via Director) for sample students
        registerStudent(StudentProfileDirector.regular("S101", "Aarav Deshmukh", "CSE", 5));
        registerStudent(StudentProfileDirector.regular("S102", "Priya Joshi", "CSE", 5));
        registerStudent(StudentProfileDirector.hostelScholar("S103", "Rohan Patil", "CSE", 5, 25));
        registerStudent(new Student.Builder("S104", "Sneha Kulkarni", "CSE", 5)
                .email("sneha@college.edu").phone("9876543210").guardianName("Mr. Kulkarni").build());

        for (String roll : List.of("S101", "S102", "S103", "S104")) model.enroll(roll, "CS301");
        for (String roll : List.of("S101", "S103")) model.enroll(roll, "MA201");
        for (String roll : List.of("S101", "S102", "S103", "S104")) model.enroll(roll, "CS3L1");

        double[] cs301Marks = {92, 78, 61, 45};
        String[] rolls = {"S101", "S102", "S103", "S104"};
        for (int i = 0; i < rolls.length; i++) {
            model.recordMarks(rolls[i], "CS301", cs301Marks[i]);
            model.recordMarks(rolls[i], "CS3L1", 55 + i * 10);
        }
        for (int i = 0; i < 5; i++) model.markAttendance("S101", "CS301", true);
        for (int i = 0; i < 5; i++) model.markAttendance("S104", "CS301", i < 3);

        model.getFees("S101").pay(60000); // S101 fully paid, others still owe fees

        TimetableConfig tt = timetableTemplates.create("REGULAR-WEEK");
        tt.setName("CSE-5A");
        tt.setTerm("Odd 2026");
        model.addTimetable(tt);
    }

    private void addClonedCourse(String template, String code, String title, String facultyId) {
        Course c = courseTemplates.create(template);
        c.setCode(code);
        c.setTitle(title);
        c.setFacultyId(facultyId);
        c.setTerm("Odd 2026");
        model.addCourse(c);
    }

    // =====================================================================
    //  Non-interactive demo (java -cp out college.Main --demo)
    // =====================================================================
    public void runDemo() {
        view.header("DEMO 1 - BUILDER: customized registration profiles");
        Student custom = new Student.Builder("S105", "Isha Rao", "IT", 3)
                .email("isha@college.edu").hostelRequired(true).scholarshipPercent(50).build();
        registerStudent(custom);
        view.println("Built with Builder  : " + custom);
        Student viaDirector = StudentProfileDirector.regular("S106", "Kabir Shah", "IT", 3);
        registerStudent(viaDirector);
        view.println("Built with Director : " + viaDirector);
        try {
            new Student.Builder("S107", "Bad Email", "IT", 3).email("not-an-email").build();
        } catch (IllegalArgumentException e) {
            view.println("Validation in build(): " + e.getMessage());
        }

        view.header("DEMO 2 - PROTOTYPE: clone course and timetable templates");
        Course original = courseTemplates.create("THEORY-4CR");
        Course copy = original.cloneObject();
        copy.setCode("CS305");
        copy.setTitle("Software Architecture");
        copy.addTopic("Extra topic only in the clone");
        view.println("Original topics: " + original.getTopics().size() + " | clone topics: " + copy.getTopics().size()
                + "  -> deep copy, original untouched");
        view.println("Cloned course  : " + copy);
        TimetableConfig tt = timetableTemplates.create("REGULAR-WEEK");
        tt.setName("IT-3B");
        tt.setTerm("Odd 2026");
        tt.assign("Thu 15:00", "Extra Doubt Session");
        view.showTimetable(tt);
        view.println("Template still has " + timetableTemplates.create("REGULAR-WEEK").getSlots().size()
                + " slots (the clone has " + tt.getSlots().size() + ")");

        view.header("DEMO 3 - STRATEGY: same marks, two grading policies for CS301");
        for (String policy : List.of("standard", "passfail")) {
            showGradeReport("CS301", GradeCalculator.strategyFor(policy));
        }

        view.header("DEMO 4 - MEDIATOR: coordinated notifications");
        view.println("Admin broadcast delivered to: " + admin.broadcast("Mid-semester exams start on Monday.") + " participants");
        view.println("Fee reminders sent: " + sendFeeReminders());
        view.println("Dr. Mehta -> CS301 students: "
                + model.requireFaculty("F101").sendToGroup(model.enrolledStudents("CS301"), "Assignment 2 is published.") + " delivered");
        view.println("S102 -> Dr. Mehta: " + model.requireStudent("S102").sendTo(model.requireFaculty("F101"), "Sir, can I get an extension?"));
        view.println("S102 broadcast attempt: " + model.requireStudent("S102").broadcast("Hi all") + " delivered");
        view.println("S102 -> S103 attempt: " + model.requireStudent("S102").sendTo(model.requireStudent("S103"), "Hey") + " delivered");
        view.showInbox("Aarav Deshmukh", model.requireStudent("S101").getInbox());
        view.showLines("MEDIATOR AUDIT LOG", mediator.getAuditLog());

        view.header("DEMO 5 - MVC: records listed through Model -> Controller -> View");
        listRecords();
    }
}

package college.model;

import college.prototype.Prototype;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Course is BOTH:
 *  - BUILDER:   created through Course.Builder (custom course profiles), and
 *  - PROTOTYPE: implements Prototype<Course> so a template can be cloned
 *               (deep copy of the topic list and assessment weights).
 */
public class Course implements Prototype<Course> {
    private String code;
    private String title;
    private final int credits;
    private String facultyId;
    private final int maxSeats;
    private String term;
    private final String gradingPolicy;
    private final List<String> topics;
    private final Map<String, Integer> assessmentWeights;

    private Course(Builder b) {
        this.code = b.code;
        this.title = b.title;
        this.credits = b.credits;
        this.facultyId = b.facultyId;
        this.maxSeats = b.maxSeats;
        this.term = b.term;
        this.gradingPolicy = b.gradingPolicy;
        this.topics = new ArrayList<>(b.topics);
        this.assessmentWeights = new LinkedHashMap<>(b.assessmentWeights);
    }

    /** Copy constructor used by cloneObject(): copies mutable collections too (deep copy). */
    private Course(Course o) {
        this.code = o.code;
        this.title = o.title;
        this.credits = o.credits;
        this.facultyId = o.facultyId;
        this.maxSeats = o.maxSeats;
        this.term = o.term;
        this.gradingPolicy = o.gradingPolicy;
        this.topics = new ArrayList<>(o.topics);
        this.assessmentWeights = new LinkedHashMap<>(o.assessmentWeights);
    }

    @Override
    public Course cloneObject() {
        return new Course(this);
    }

    public String getCode() { return code; }
    public String getTitle() { return title; }
    public int getCredits() { return credits; }
    public String getFacultyId() { return facultyId; }
    public int getMaxSeats() { return maxSeats; }
    public String getTerm() { return term; }
    public String getGradingPolicy() { return gradingPolicy; }
    public List<String> getTopics() { return Collections.unmodifiableList(topics); }
    public Map<String, Integer> getAssessmentWeights() { return Collections.unmodifiableMap(assessmentWeights); }

    public void setCode(String code) { this.code = code; }
    public void setTitle(String title) { this.title = title; }
    public void setFacultyId(String facultyId) { this.facultyId = facultyId; }
    public void setTerm(String term) { this.term = term; }
    public void addTopic(String topic) { topics.add(topic); }

    @Override
    public String toString() {
        return String.format("%s | %s | %d credits | faculty=%s | seats=%d | term=%s | grading=%s",
                code, title, credits, facultyId.isEmpty() ? "-" : facultyId, maxSeats, term, gradingPolicy);
    }

    // ------------------------------------------------------------------ Builder
    public static class Builder {
        private final String code;
        private final String title;
        private final int credits;
        private String facultyId = "";
        private int maxSeats = 60;
        private String term = "Unassigned";
        private String gradingPolicy = "absolute";
        private final List<String> topics = new ArrayList<>();
        private final Map<String, Integer> assessmentWeights = new LinkedHashMap<>();

        public Builder(String code, String title, int credits) {
            this.code = code;
            this.title = title;
            this.credits = credits;
        }

        public Builder faculty(String facultyId) { this.facultyId = facultyId; return this; }
        public Builder maxSeats(int maxSeats) { this.maxSeats = maxSeats; return this; }
        public Builder term(String term) { this.term = term; return this; }
        public Builder gradingPolicy(String policy) { this.gradingPolicy = policy; return this; }
        public Builder topic(String topic) { this.topics.add(topic); return this; }
        public Builder weight(String component, int percent) { this.assessmentWeights.put(component, percent); return this; }

        public Course build() {
            if (code == null || code.isBlank()) throw new IllegalArgumentException("Course code is required");
            if (title == null || title.isBlank()) throw new IllegalArgumentException("Course title is required");
            if (credits < 1 || credits > 6) throw new IllegalArgumentException("Credits must be between 1 and 6");
            if (maxSeats < 1) throw new IllegalArgumentException("Seats must be positive");
            if (!assessmentWeights.isEmpty()) {
                int total = assessmentWeights.values().stream().mapToInt(Integer::intValue).sum();
                if (total != 100) throw new IllegalArgumentException("Assessment weights must add up to 100 (got " + total + ")");
            }
            return new Course(this);
        }
    }
}

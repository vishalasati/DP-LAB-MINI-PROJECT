package college.model;

import college.prototype.Prototype;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * PROTOTYPE pattern - a timetable configuration that can be cloned and then
 * tweaked (e.g. reuse last semester's weekly layout for a new section).
 */
public class TimetableConfig implements Prototype<TimetableConfig> {
    private String name;
    private String term;
    private final Map<String, String> slots = new LinkedHashMap<>();

    public TimetableConfig(String name, String term) {
        this.name = name;
        this.term = term;
    }

    private TimetableConfig(TimetableConfig other) {
        this.name = other.name;
        this.term = other.term;
        this.slots.putAll(other.slots); // deep copy of the slot map
    }

    public TimetableConfig assign(String slot, String entry) {
        slots.put(slot, entry);
        return this;
    }

    @Override
    public TimetableConfig cloneObject() {
        return new TimetableConfig(this);
    }

    public String getName() { return name; }
    public String getTerm() { return term; }
    public void setName(String name) { this.name = name; }
    public void setTerm(String term) { this.term = term; }
    public Map<String, String> getSlots() { return Collections.unmodifiableMap(slots); }
}

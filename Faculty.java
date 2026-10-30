package college.model;

import college.mediator.Participant;

public class Faculty extends Participant {
    private final String department;

    public Faculty(String id, String name, String department) {
        super(id, name, Participant.FACULTY);
        this.department = department;
    }

    public String getDepartment() { return department; }

    @Override
    public String toString() {
        return getId() + " | " + getName() + " | " + department;
    }
}

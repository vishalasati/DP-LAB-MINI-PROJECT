package college.mediator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * MEDIATOR pattern - the Colleague base class.
 * Students, Faculty and Administrators never hold references to each other;
 * they only talk to the mediator.
 */
public abstract class Participant {
    public static final String STUDENT = "Student";
    public static final String FACULTY = "Faculty";
    public static final String ADMIN = "Administrator";

    private final String id;
    private final String name;
    private final String role;
    private NotificationMediator mediator;
    private final List<String> inbox = new ArrayList<>();

    protected Participant(String id, String name, String role) {
        this.id = id;
        this.name = name;
        this.role = role;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getRole() { return role; }

    public void setMediator(NotificationMediator mediator) {
        this.mediator = mediator;
    }

    private NotificationMediator mediator() {
        if (mediator == null) throw new IllegalStateException(name + " is not registered with the mediator");
        return mediator;
    }

    public int sendTo(Participant recipient, String message) {
        return mediator().sendTo(this, recipient, message);
    }

    public int sendToRole(String targetRole, String message) {
        return mediator().sendToRole(this, targetRole, message);
    }

    public int sendToGroup(Collection<? extends Participant> group, String message) {
        return mediator().sendToGroup(this, group, message);
    }

    public int broadcast(String message) {
        return mediator().broadcast(this, message);
    }

    /** Called by the mediator only. */
    public void receive(Participant from, String message) {
        inbox.add("[" + from.getRole() + ": " + from.getName() + "] " + message);
    }

    public List<String> getInbox() {
        return Collections.unmodifiableList(inbox);
    }
}

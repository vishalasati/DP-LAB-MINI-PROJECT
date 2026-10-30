package college.mediator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * MEDIATOR pattern - the Concrete Mediator.
 * Holds all routing rules in ONE place:
 *   - Students may write to faculty/administration, but NOT to other students
 *     and NOT broadcast.
 *   - Faculty and administrators may write to anyone.
 * Every attempt (delivered or blocked) is recorded in an audit log.
 */
public class CollegeNotificationMediator implements NotificationMediator {
    private final Map<String, Participant> participants = new LinkedHashMap<>();
    private final List<String> auditLog = new ArrayList<>();

    @Override
    public void register(Participant p) {
        participants.put(p.getId(), p);
        p.setMediator(this);
    }

    private boolean allowed(Participant sender, String targetRole) {
        if (Participant.STUDENT.equals(sender.getRole())) {
            return !Participant.STUDENT.equals(targetRole);
        }
        return true;
    }

    private int deliver(Participant from, Participant to, String message) {
        to.receive(from, message);
        auditLog.add("DELIVERED " + from.getName() + " -> " + to.getName() + ": " + message);
        return 1;
    }

    private int block(Participant from, String what) {
        auditLog.add("BLOCKED   " + from.getName() + " (" + from.getRole() + ") not allowed to " + what);
        return 0;
    }

    @Override
    public int sendTo(Participant sender, Participant recipient, String message) {
        if (!allowed(sender, recipient.getRole())) return block(sender, "message " + recipient.getRole() + "s");
        return deliver(sender, recipient, message);
    }

    @Override
    public int sendToRole(Participant sender, String role, String message) {
        if (!allowed(sender, role)) return block(sender, "message all " + role + "s");
        int count = 0;
        for (Participant p : participants.values()) {
            if (p != sender && p.getRole().equals(role)) count += deliver(sender, p, message);
        }
        return count;
    }

    @Override
    public int sendToGroup(Participant sender, Collection<? extends Participant> group, String message) {
        int count = 0;
        for (Participant p : group) {
            if (p != sender) count += sendTo(sender, p, message);
        }
        return count;
    }

    @Override
    public int broadcast(Participant sender, String message) {
        if (Participant.STUDENT.equals(sender.getRole())) return block(sender, "broadcast");
        int count = 0;
        for (Participant p : participants.values()) {
            if (p != sender) count += deliver(sender, p, message);
        }
        return count;
    }

    public List<String> getAuditLog() {
        return Collections.unmodifiableList(auditLog);
    }
}

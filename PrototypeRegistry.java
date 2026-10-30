package college.prototype;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * PROTOTYPE pattern - registry of ready-made templates.
 * Clients ask for a template by key and always receive a fresh clone,
 * so the stored template is never modified.
 */
public class PrototypeRegistry<T extends Prototype<T>> {
    private final Map<String, T> templates = new LinkedHashMap<>();

    public void register(String key, T prototype) {
        templates.put(key, prototype);
    }

    public T create(String key) {
        T prototype = templates.get(key);
        if (prototype == null) {
            throw new IllegalArgumentException("No template named '" + key + "'. Available: " + templates.keySet());
        }
        return prototype.cloneObject();
    }

    public Set<String> keys() {
        return templates.keySet();
    }
}

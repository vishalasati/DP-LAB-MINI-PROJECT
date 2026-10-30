package college.prototype;

/**
 * PROTOTYPE pattern - the contract every cloneable template implements.
 * Using our own interface (instead of java.lang.Cloneable) lets us guarantee
 * a DEEP copy and a type-safe return value.
 */
public interface Prototype<T> {
    T cloneObject();
}

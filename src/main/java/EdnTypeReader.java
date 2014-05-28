import clojure.lang.AFn;

public abstract class EdnTypeReader extends AFn {
    /**
     * Read a tagged Edn object as its intended type.
     */
    abstract Object read(Object o);

    /**
     * For use by the EdnReader only. Do not call directly.
     */
    @Override
    public final Object invoke(Object arg2) {
        return read(arg2);
    }
}
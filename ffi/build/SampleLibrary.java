import clojure.java.api.Clojure;
import clojure.lang.IFn;
import org.graalvm.nativeimage.IsolateThread;
import org.graalvm.nativeimage.c.function.CEntryPoint;

public final class SampleLibrary {
    private static final IFn ADD = Clojure.var("experiment.ffi", "add");

    @CEntryPoint(name = "sample_add")
    public static long add(IsolateThread thread, long a, long b) {
        return ((Number) ADD.invoke(a, b)).longValue();
    }
}

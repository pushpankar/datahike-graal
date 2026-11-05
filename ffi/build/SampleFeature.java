import clojure.java.api.Clojure;
import org.graalvm.nativeimage.hosted.Feature;

public final class SampleFeature implements Feature {
    @Override
    public void afterRegistration(AfterRegistrationAccess access) {
        Clojure.var("clojure.core", "require").invoke(Clojure.read("experiment.ffi"));
    }
}

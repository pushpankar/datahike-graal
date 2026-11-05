import clojure.java.api.Clojure;
import org.graalvm.nativeimage.hosted.Feature;

public final class DatomicImage implements Feature {
    @Override
    public void afterRegistration(AfterRegistrationAccess access) {
        Clojure.var("clojure.core", "require").invoke(Clojure.read("experiment.main"));
        Clojure.var("experiment.image", "prepare!").invoke();
    }
}

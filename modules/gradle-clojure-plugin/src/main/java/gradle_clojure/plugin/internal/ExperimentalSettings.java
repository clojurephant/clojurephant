package gradle_clojure.plugin.internal;

public final class ExperimentalSettings {
  private ExperimentalSettings() {
    // do not instantiate
  }

  public static boolean isUseWorkers() {
    return Boolean.getBoolean("gradle-clojure.experimental.use-workers");
  }
}

package gradle_clojure.plugin.common.internal;

public final class ExperimentalSettings {
  private ExperimentalSettings() {
    // do not instantiate
  }

  public static boolean isUseWorkers() {
    return Boolean.getBoolean("gradle-clojure.experimental.use-workers");
  }
}

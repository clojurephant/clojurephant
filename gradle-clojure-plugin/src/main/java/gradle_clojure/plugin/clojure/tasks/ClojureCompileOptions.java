package gradle_clojure.plugin.clojure.tasks;


import java.util.Collections;
import java.util.List;

import org.gradle.api.tasks.Input;

public final class ClojureCompileOptions {
  private boolean disableLocalsClearing = false;
  private List<String> elideMeta = Collections.emptyList();
  private boolean directLinking = false;

  @Input
  public boolean isDisableLocalsClearing() {
    return disableLocalsClearing;
  }

  public void setDisableLocalsClearing(boolean disableLocalsClearing) {
    this.disableLocalsClearing = disableLocalsClearing;
  }

  @Input
  public List<String> getElideMeta() {
    return elideMeta;
  }

  public void setElideMeta(List<String> elideMeta) {
    this.elideMeta = elideMeta;
  }

  @Input
  public boolean isDirectLinking() {
    return directLinking;
  }

  public void setDirectLinking(boolean directLinking) {
    this.directLinking = directLinking;
  }
}

package gradle_clojure.plugin.clojure.tasks;


import java.util.Collections;
import java.util.List;

import groovy.lang.Closure;
import org.gradle.api.Action;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Nested;
import org.gradle.api.tasks.compile.ForkOptions;

public final class ClojureCompileOptions {
  private final ForkOptions forkOptions = new ForkOptions();

  private ReflectionWarnings reflectionWarnings = new ReflectionWarnings(false, false, false);

  private boolean disableLocalsClearing = false;
  private List<String> elideMeta = Collections.emptyList();
  private boolean directLinking = false;

  @Nested
  public ForkOptions getForkOptions() {
    return forkOptions;
  }

  public ClojureCompileOptions forkOptions(Action<? super ForkOptions> configureAction) {
    configureAction.execute(forkOptions);
    return this;
  }

  /*
   * We only have this variant (instead of just Action) since Gradle doesn't currently (as of 4.7)
   * instrument Action methods on nested config objects
   */
  public ClojureCompileOptions forkOptions(Closure<?> configureAction) {
    configureAction.setResolveStrategy(Closure.DELEGATE_FIRST);
    configureAction.setDelegate(forkOptions);
    configureAction.call(forkOptions);
    return this;
  }

  @Nested
  public ReflectionWarnings getReflectionWarnings() {
    return reflectionWarnings;
  }

  public void setReflectionWarnings(ReflectionWarnings reflectionWarnings) {
    this.reflectionWarnings = reflectionWarnings;
  }

  public ClojureCompileOptions reflectionWarnings(Action<? super ReflectionWarnings> configureAction) {
    configureAction.execute(reflectionWarnings);
    return this;
  }

  /*
   * We only have this variant (instead of just Action) since Gradle doesn't currently (as of 4.7)
   * instrument Action methods on nested config objects
   */
  public ClojureCompileOptions reflectionWarnings(Closure<?> configureAction) {
    configureAction.setResolveStrategy(Closure.DELEGATE_FIRST);
    configureAction.setDelegate(reflectionWarnings);
    configureAction.call(reflectionWarnings);
    return this;
  }

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

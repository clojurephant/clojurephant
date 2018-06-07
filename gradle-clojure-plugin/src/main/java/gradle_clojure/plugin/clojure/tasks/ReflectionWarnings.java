package gradle_clojure.plugin.clojure.tasks;



import org.gradle.api.tasks.Input;

public final class ReflectionWarnings {
  private boolean enabled;
  private boolean projectOnly;
  private boolean asErrors;

  public ReflectionWarnings(boolean enabled, boolean projectOnly, boolean asErrors) {
    this.enabled = enabled;
    this.projectOnly = projectOnly;
    this.asErrors = asErrors;
  }

  @Input
  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  @Input
  public boolean isProjectOnly() {
    return projectOnly;
  }

  public void setProjectOnly(boolean projectOnly) {
    this.projectOnly = projectOnly;
  }

  @Input
  public boolean isAsErrors() {
    return asErrors;
  }

  public void setAsErrors(boolean asErrors) {
    this.asErrors = asErrors;
  }
}

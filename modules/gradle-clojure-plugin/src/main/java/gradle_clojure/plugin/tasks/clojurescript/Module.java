package gradle_clojure.plugin.tasks.clojurescript;

import java.io.File;
import java.io.Serializable;
import java.util.Set;

import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.OutputFile;

public class Module implements Serializable {
  private File outputTo;
  private Set<String> entries;
  private Set<String> dependsOn;

  @OutputFile
  public File getOutputTo() {
    return outputTo;
  }

  public void setOutputTo(File outputTo) {
    this.outputTo = outputTo;
  }

  @Input
  @Optional
  public Set<String> getEntries() {
    return entries;
  }

  public void setEntries(Set<String> entries) {
    this.entries = entries;
  }

  @Input
  @Optional
  public Set<String> getDependsOn() {
    return dependsOn;
  }

  public void setDependsOn(Set<String> dependsOn) {
    this.dependsOn = dependsOn;
  }
}

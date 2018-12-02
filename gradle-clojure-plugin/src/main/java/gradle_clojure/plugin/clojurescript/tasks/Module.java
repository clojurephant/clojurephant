package gradle_clojure.plugin.clojurescript.tasks;


import java.io.File;
import java.util.Set;

import org.gradle.api.Project;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.OutputFile;

public class Module {
  private final DirectoryProperty destinationDir;
  private final RegularFileProperty outputTo;
  private Set<String> entries;
  private Set<String> dependsOn;

  public Module(Project project, DirectoryProperty destinationDir) {
    this.destinationDir = destinationDir;
    this.outputTo = project.getObjects().fileProperty();
  }

  @OutputFile
  public RegularFileProperty getOutputTo() {
    return outputTo;
  }

  public void setOutputTo(String outputTo) {
    this.outputTo.set(destinationDir.file(outputTo));
  }

  public void setOutputTo(File outputTo) {
    this.outputTo.set(outputTo);
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

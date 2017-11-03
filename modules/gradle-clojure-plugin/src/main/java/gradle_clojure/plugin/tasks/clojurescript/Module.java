/*
 * Copyright 2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package gradle_clojure.plugin.tasks.clojurescript;

import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.OutputFile;

import java.io.File;
import java.io.Serializable;
import java.util.Set;

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

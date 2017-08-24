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
package gradle_clojure.plugin.tasks;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

import org.gradle.api.Action;

public final class ClojureCompileOptions implements Serializable {
  private final ClojureForkOptions forkOptions = new ClojureForkOptions();

  private boolean aotCompile = false;
  private Boolean copySourceSetToOutput = null;
  private ReflectionWarnings reflectionWarnings = new ReflectionWarnings(false, false, false);

  private boolean disableLocalsClearing = false;
  private List<String> elideMeta = Collections.emptyList();
  private boolean directLinking = false;

  public ClojureForkOptions getForkOptions() {
    return forkOptions;
  }

  public ClojureCompileOptions forkOptions(Action<? super ClojureForkOptions> configureAction) {
    configureAction.execute(forkOptions);
    return this;
  }

  public boolean isAotCompile() {
    return aotCompile;
  }

  public void setAotCompile(boolean aotCompile) {
    this.aotCompile = aotCompile;
  }

  public boolean isCopySourceSetToOutput() {
    return copySourceSetToOutput == null ? !aotCompile : copySourceSetToOutput;
  }

  public void setCopySourceSetToOutput(boolean copySourceSetToOutput) {
    this.copySourceSetToOutput = copySourceSetToOutput;
  }

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

  public boolean isDisableLocalsClearing() {
    return disableLocalsClearing;
  }

  public void setDisableLocalsClearing(boolean disableLocalsClearing) {
    this.disableLocalsClearing = disableLocalsClearing;
  }

  public List<String> getElideMeta() {
    return elideMeta;
  }

  public void setElideMeta(List<String> elideMeta) {
    this.elideMeta = elideMeta;
  }

  public boolean isDirectLinking() {
    return directLinking;
  }

  public void setDirectLinking(boolean directLinking) {
    this.directLinking = directLinking;
  }
}

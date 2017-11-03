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
package gradle_clojure.plugin.internal;

import gradle_clojure.plugin.tasks.clojurescript.ClojurescriptSourceSet;
import org.gradle.api.Action;
import org.gradle.api.file.SourceDirectorySet;
import org.gradle.api.internal.file.SourceDirectorySetFactory;

public class DefaultClojurescriptSourceSet implements ClojurescriptSourceSet {
  private final SourceDirectorySet clojurescript;

  public DefaultClojurescriptSourceSet(String name, SourceDirectorySetFactory sourceDirectorySetFactory) {
    this.clojurescript = sourceDirectorySetFactory.create(name);
    this.clojurescript.getFilter().include("**/*.cljs", "**/*.cljc", "**/*.clj");
  }

  @Override
  public SourceDirectorySet getClojurescript() {
    return clojurescript;
  }

  @Override
  public ClojurescriptSourceSet clojurescript(Action<? super SourceDirectorySet> configureAction) {
    configureAction.execute(clojurescript);
    return this;
  }
}

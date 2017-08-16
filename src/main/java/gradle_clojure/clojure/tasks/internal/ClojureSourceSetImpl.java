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
package gradle_clojure.clojure.tasks.internal;

import org.gradle.api.Action;
import org.gradle.api.file.SourceDirectorySet;
import org.gradle.api.internal.file.DefaultSourceDirectorySet;
import org.gradle.api.internal.file.FileResolver;
import org.gradle.api.internal.file.collections.DefaultDirectoryFileTreeFactory;

import gradle_clojure.clojure.tasks.ClojureSourceSet;

public class ClojureSourceSetImpl implements ClojureSourceSet {
  private final String displayName;
  private final FileResolver resolver;
  private final SourceDirectorySet clojure;

  public ClojureSourceSetImpl(String displayName, FileResolver resolver) {
    this.displayName = displayName;
    this.resolver = resolver;
    this.clojure = new DefaultSourceDirectorySet(displayName, resolver, new DefaultDirectoryFileTreeFactory());
    this.clojure.getFilter().include("**/*.clj", "**/*.cljc");
  }

  @Override
  public SourceDirectorySet getClojure() {
    return clojure;
  }

  @Override
  public ClojureSourceSet clojure(Action<? super SourceDirectorySet> configureAction) {
    configureAction.execute(clojure);
    return this;
  }
}

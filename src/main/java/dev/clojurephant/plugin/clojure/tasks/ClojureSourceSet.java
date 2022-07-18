package dev.clojurephant.plugin.clojure.tasks;

import groovy.lang.Closure;
import org.gradle.api.Action;
import org.gradle.api.file.SourceDirectorySet;

public interface ClojureSourceSet {
  SourceDirectorySet getClojure();

  ClojureSourceSet clojure(Action<? super SourceDirectorySet> configureAction);

  /*
   * We only have this variant (instead of just Action) since Gradle doesn't currently (as of 4.7)
   * instrument Action methods on nested config objects
   */
  ClojureSourceSet clojure(Closure<?> configureAction);
}

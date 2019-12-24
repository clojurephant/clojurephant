package dev.clojurephant.plugin.clojure.internal;

import dev.clojurephant.plugin.clojure.tasks.ClojureSourceSet;
import groovy.lang.Closure;
import org.gradle.api.Action;
import org.gradle.api.file.SourceDirectorySet;
import org.gradle.api.model.ObjectFactory;

public class DefaultClojureSourceSet implements ClojureSourceSet {
  private final SourceDirectorySet clojure;

  public DefaultClojureSourceSet(String name, ObjectFactory objects) {
    this.clojure = objects.sourceDirectorySet(name, name);
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

  /*
   * We only have this variant (instead of just Action) since Gradle doesn't currently (as of 4.7)
   * instrument Action methods on nested config objects
   */
  @Override
  public ClojureSourceSet clojure(Closure<?> configureAction) {
    configureAction.setResolveStrategy(Closure.DELEGATE_FIRST);
    configureAction.setDelegate(clojure);
    configureAction.call(clojure);
    return this;
  }
}

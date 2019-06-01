package dev.clojurephant.plugin.clojurescript.internal;

import dev.clojurephant.plugin.clojurescript.tasks.ClojureScriptSourceSet;
import org.gradle.api.Action;
import org.gradle.api.file.SourceDirectorySet;
import org.gradle.api.model.ObjectFactory;

public class DefaultClojureScriptSourceSet implements ClojureScriptSourceSet {
  private final SourceDirectorySet clojurescript;

  public DefaultClojureScriptSourceSet(String name, ObjectFactory objects) {
    this.clojurescript = objects.sourceDirectorySet(name, name);
    this.clojurescript.getFilter().include("**/*.cljs", "**/*.cljc", "**/*.clj");
  }

  @Override
  public SourceDirectorySet getClojureScript() {
    return clojurescript;
  }

  @Override
  public ClojureScriptSourceSet clojurescript(Action<? super SourceDirectorySet> configureAction) {
    configureAction.execute(clojurescript);
    return this;
  }
}

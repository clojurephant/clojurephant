package gradle_clojure.plugin.clojurescript.internal;

import gradle_clojure.plugin.clojurescript.tasks.ClojureScriptSourceSet;
import org.gradle.api.Action;
import org.gradle.api.file.SourceDirectorySet;
import org.gradle.api.internal.file.SourceDirectorySetFactory;

public class DefaultClojureScriptSourceSet implements ClojureScriptSourceSet {
  private final SourceDirectorySet clojurescript;

  public DefaultClojureScriptSourceSet(String name, SourceDirectorySetFactory sourceDirectorySetFactory) {
    this.clojurescript = sourceDirectorySetFactory.create(name);
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

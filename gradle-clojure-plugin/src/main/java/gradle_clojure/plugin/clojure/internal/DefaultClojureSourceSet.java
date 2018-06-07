package gradle_clojure.plugin.clojure.internal;

import gradle_clojure.plugin.clojure.tasks.ClojureSourceSet;
import org.gradle.api.Action;
import org.gradle.api.file.SourceDirectorySet;
import org.gradle.api.internal.file.SourceDirectorySetFactory;

public class DefaultClojureSourceSet implements ClojureSourceSet {
  private final SourceDirectorySet clojure;

  public DefaultClojureSourceSet(String name, SourceDirectorySetFactory sourceDirectorySetFactory) {
    this.clojure = sourceDirectorySetFactory.create(name);
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

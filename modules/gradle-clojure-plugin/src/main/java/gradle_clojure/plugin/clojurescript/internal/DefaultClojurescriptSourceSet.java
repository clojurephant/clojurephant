package gradle_clojure.plugin.clojurescript.internal;

import gradle_clojure.plugin.clojurescript.tasks.ClojurescriptSourceSet;
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

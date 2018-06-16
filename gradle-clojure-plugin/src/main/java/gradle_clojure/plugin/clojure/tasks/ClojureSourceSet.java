package gradle_clojure.plugin.clojure.tasks;

import org.gradle.api.Action;
import org.gradle.api.file.SourceDirectorySet;

public interface ClojureSourceSet {
  SourceDirectorySet getClojure();

  ClojureSourceSet clojure(Action<? super SourceDirectorySet> configureAction);
}

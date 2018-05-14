package gradle_clojure.plugin.tasks.clojurescript;

import org.gradle.api.Action;
import org.gradle.api.file.SourceDirectorySet;

public interface ClojurescriptSourceSet {
  SourceDirectorySet getClojurescript();

  ClojurescriptSourceSet clojurescript(Action<? super SourceDirectorySet> configureAction);
}

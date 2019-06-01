package dev.clojurephant.plugin.clojurescript.tasks;

import org.gradle.api.Action;
import org.gradle.api.file.SourceDirectorySet;

public interface ClojureScriptSourceSet {
  SourceDirectorySet getClojureScript();

  ClojureScriptSourceSet clojurescript(Action<? super SourceDirectorySet> configureAction);
}

package dev.clojurephant.plugin.clojurescript;

import org.gradle.api.NamedDomainObjectContainer;
import org.gradle.api.file.DirectoryProperty;

public interface ClojureScriptExtension {
  DirectoryProperty getRootOutputDir();

  NamedDomainObjectContainer<ClojureScriptBuild> getBuilds();
}

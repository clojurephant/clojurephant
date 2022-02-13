package dev.clojurephant.plugin.clojure;

import org.gradle.api.NamedDomainObjectContainer;
import org.gradle.api.file.DirectoryProperty;

public interface ClojureExtension {
  DirectoryProperty getRootOutputDir();

  NamedDomainObjectContainer<ClojureBuild> getBuilds();
}

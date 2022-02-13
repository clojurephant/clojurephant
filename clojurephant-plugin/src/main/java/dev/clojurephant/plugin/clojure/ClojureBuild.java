package dev.clojurephant.plugin.clojure;

import java.util.Set;

import dev.clojurephant.plugin.clojure.tasks.ClojureCompileOptions;
import dev.clojurephant.plugin.clojure.tasks.ClojureSourceSet;
import dev.clojurephant.plugin.common.internal.Namespaces;
import org.apache.commons.text.WordUtils;
import org.gradle.api.Named;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.FileCollection;
import org.gradle.api.internal.plugins.DslObject;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;
import org.gradle.api.provider.SetProperty;
import org.gradle.api.tasks.Nested;
import org.gradle.api.tasks.SourceSet;

public abstract class ClojureBuild implements Named {
  public abstract DirectoryProperty getOutputDir();

  public abstract Property<SourceSet> getSourceSet();

  Provider<FileCollection> getSourceRoots() {
    return getSourceSet().map(sourceSet -> {
      ClojureSourceSet clojure = (ClojureSourceSet) new DslObject(sourceSet).getConvention().getPlugins().get("clojure");
      return clojure.getClojure().getSourceDirectories();
    });
  }

  Provider<Set<String>> getAllNamespaces() {
    return getSourceRoots().map(roots -> Namespaces.findNamespaces(roots, Namespaces.CLOJURE_EXTENSIONS));
  }

  public abstract SetProperty<String> getCheckNamespaces();

  public void checkAll() {
    getCheckNamespaces().set(getAllNamespaces());
  }

  public abstract Property<String> getReflection();

  boolean isCompilerConfigured() {
    return getAotNamespaces().map(set -> !set.isEmpty()).getOrElse(false);
  }

  public abstract SetProperty<String> getAotNamespaces();

  public void aotAll() {
    getAotNamespaces().set(getAllNamespaces());
  }

  @Nested
  public abstract ClojureCompileOptions getCompiler();

  String getTaskName(String task) {
    if ("main".equals(getName())) {
      return String.format("%sClojure", task);
    } else {
      return String.format("%s%sClojure", task, WordUtils.capitalize(getName()));
    }
  }
}

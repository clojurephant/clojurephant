package dev.clojurephant.plugin.clojure;

import java.util.Set;

import dev.clojurephant.plugin.clojure.tasks.ClojureCompileOptions;
import dev.clojurephant.plugin.clojure.tasks.ClojureSourceSet;
import dev.clojurephant.plugin.common.internal.Namespaces;
import org.apache.commons.text.WordUtils;
import org.gradle.api.Action;
import org.gradle.api.Named;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.FileCollection;
import org.gradle.api.file.FileTree;
import org.gradle.api.internal.plugins.DslObject;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;
import org.gradle.api.provider.SetProperty;
import org.gradle.api.tasks.Nested;
import org.gradle.api.tasks.SourceSet;
import org.gradle.internal.classpath.Instrumented;

public abstract class ClojureBuild implements Named {
  public abstract DirectoryProperty getOutputDir();

  public abstract ConfigurableFileCollection getClasspath();

  public abstract ConfigurableFileCollection getSourceRoots();

  public FileTree getSourceTree() {
    return getSourceRoots().getAsFileTree();
  }

  Provider<Set<String>> getAllNamespaces() {
    // Using internal API due to https://github.com/gradle/gradle/issues/20265
    Instrumented.fileCollectionObserved(getSourceTree(), "Clojurephant");
    return Namespaces.findNamespaces(getSourceTree());
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

  public void compiler(Action<? super ClojureCompileOptions> configureAction) {
    configureAction.execute(getCompiler());
  }

  String getTaskName(String task) {
    if ("main".equals(getName())) {
      return String.format("%sClojure", task);
    } else {
      return String.format("%s%sClojure", task, WordUtils.capitalize(getName()));
    }
  }
}

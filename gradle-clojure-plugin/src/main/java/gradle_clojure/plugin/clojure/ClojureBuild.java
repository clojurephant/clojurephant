package gradle_clojure.plugin.clojure;

import java.util.Set;

import gradle_clojure.plugin.clojure.tasks.ClojureCompileOptions;
import gradle_clojure.plugin.clojure.tasks.ClojureReflection;
import gradle_clojure.plugin.clojure.tasks.ClojureSourceSet;
import gradle_clojure.plugin.common.internal.Namespaces;
import groovy.lang.Closure;
import org.apache.commons.text.WordUtils;
import org.gradle.api.Action;
import org.gradle.api.Named;
import org.gradle.api.Project;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.FileCollection;
import org.gradle.api.internal.plugins.DslObject;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;
import org.gradle.api.provider.SetProperty;
import org.gradle.api.tasks.SourceSet;

public class ClojureBuild implements Named {
  private final String name;
  private final DirectoryProperty outputDir;
  private final Property<SourceSet> sourceSet;
  private final SetProperty<String> checkNamespaces;
  private Property<ClojureReflection> reflection;
  private final SetProperty<String> aotNamespaces;
  private final ClojureCompileOptions compiler;

  public ClojureBuild(Project project, String name) {
    this.name = name;
    this.outputDir = project.getObjects().directoryProperty();
    this.sourceSet = project.getObjects().property(SourceSet.class);
    this.checkNamespaces = project.getObjects().setProperty(String.class);
    this.reflection = project.getObjects().property(ClojureReflection.class);
    this.aotNamespaces = project.getObjects().setProperty(String.class);
    this.compiler = new ClojureCompileOptions();

    this.reflection.set(ClojureReflection.silent);
  }

  @Override
  public String getName() {
    return name;
  }

  public DirectoryProperty getOutputDir() {
    return outputDir;
  }

  public Property<SourceSet> getSourceSet() {
    return sourceSet;
  }

  Provider<FileCollection> getSourceRoots() {
    return getSourceSet().map(sourceSet -> {
      ClojureSourceSet clojure = (ClojureSourceSet) new DslObject(sourceSet).getConvention().getPlugins().get("clojure");
      return clojure.getClojure().getSourceDirectories();
    });
  }

  Provider<Set<String>> getAllNamespaces() {
    return getSourceRoots().map(roots -> {
      return Namespaces.findNamespaces(roots, Namespaces.CLOJURE_EXTENSIONS);
    });
  }

  public SetProperty<String> getCheckNamespaces() {
    return checkNamespaces;
  }

  public void checkAll() {
    checkNamespaces.set(getAllNamespaces());
  }

  public Property<ClojureReflection> getReflection() {
    return reflection;
  }

  public void setReflection(String reflection) {
    this.reflection.set(ClojureReflection.valueOf(reflection));
  }

  public boolean isCompilerConfigured() {
    return getAotNamespaces().map(set -> !set.isEmpty()).getOrElse(false);
  }

  public SetProperty<String> getAotNamespaces() {
    return aotNamespaces;
  }

  public void aotAll() {
    aotNamespaces.set(getAllNamespaces());
  }

  public ClojureCompileOptions getCompiler() {
    return compiler;
  }

  public void compiler(Action<? super ClojureCompileOptions> configureAction) {
    configureAction.execute(compiler);
  }

  /*
   * We only have this variant (instead of just Action) since Gradle doesn't currently (as of 4.7)
   * instrument Action methods on nested config objects
   */
  public void compiler(Closure<?> configureAction) {
    configureAction.setResolveStrategy(Closure.DELEGATE_FIRST);
    configureAction.setDelegate(compiler);
    configureAction.call(compiler);
  }

  String getTaskName(String task) {
    if ("main".equals(name)) {
      return String.format("%sClojure", task);
    } else {
      return String.format("%s%sClojure", task, WordUtils.capitalize(name));
    }
  }
}

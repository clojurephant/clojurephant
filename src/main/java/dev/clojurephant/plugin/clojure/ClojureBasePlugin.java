package dev.clojurephant.plugin.clojure;

import java.io.File;
import java.util.Collections;

import javax.inject.Inject;

import dev.clojurephant.plugin.clojure.tasks.ClojureCheck;
import dev.clojurephant.plugin.clojure.tasks.ClojureCompile;
import dev.clojurephant.plugin.common.internal.ClojureCommonBasePlugin;
import dev.clojurephant.plugin.common.internal.Namespaces;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.file.FileCollection;
import org.gradle.api.file.SourceDirectorySet;
import org.gradle.api.internal.tasks.DefaultSourceSetOutput;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.api.tasks.TaskContainer;
import org.gradle.api.tasks.TaskProvider;

public class ClojureBasePlugin implements Plugin<Project> {
  public static final String SOURCE_DIRECTORY_SET_NAME = "clojure";

  private final ObjectFactory objects;

  @Inject
  public ClojureBasePlugin(ObjectFactory objects) {
    this.objects = objects;
  }

  @Override
  public void apply(Project project) {
    project.getPluginManager().apply(ClojureCommonBasePlugin.class);
    ClojureExtension extension = project.getExtensions().create("clojure", ClojureExtension.class);
    SourceSetContainer sourceSets = project.getExtensions().getByType(SourceSetContainer.class);
    configureSourceSetDefaults(project, sourceSets, extension);
    configureBuildDefaults(project, sourceSets, extension);
    configureCheckDefaults(project.getTasks());
  }

  private void configureSourceSetDefaults(Project project, SourceSetContainer sourceSets, ClojureExtension extension) {
    sourceSets.all(sourceSet -> {
      // every source set gets clojure source, following same convention as Java/Groovy
      SourceDirectorySet clojureSource = objects.sourceDirectorySet(sourceSet.getName(), sourceSet.getName());
      clojureSource.srcDir(String.format("src/%s/clojure", sourceSet.getName()));
      clojureSource.getFilter().include(Namespaces.CLOJURE_PATTERNS);

      // make the sources available on the source set
      sourceSet.getExtensions().add(SOURCE_DIRECTORY_SET_NAME, clojureSource);

      // in case the clojure source overlaps with the resources source
      sourceSet.getResources().getFilter().exclude(element -> clojureSource.contains(element.getFile()));

      // ensure that clojure is considered part of full source of source set
      sourceSet.getAllSource().source(clojureSource);

      // every source set gets a default clojure build
      ClojureBuild build = extension.getBuilds().create(sourceSet.getName());
      build.getSourceRoots().from(clojureSource.getSourceDirectories());
      clojureSource.getDestinationDirectory().set(build.getOutputDir());

      build.getClasspath()
          .from(project.provider(() -> sourceSet.getCompileClasspath()))
          // depend on compiled Java by default
          .from(project.files(sourceSet.getJava().getClassesDirectory()))
          // depend on processed resources by default
          .from(project.getTasks().named(sourceSet.getProcessResourcesTaskName()));

      Provider<FileCollection> output = project.provider(() -> {
        if (build.isCompilerConfigured()) {
          return project.files(build.getOutputDir());
        } else {
          return build.getSourceRoots();
        }
      });

      ((DefaultSourceSetOutput) sourceSet.getOutput()).getClassesDirs().from(output);

      project.getTasks().named(sourceSet.getClassesTaskName(), task -> {
        task.dependsOn(build.getTaskName("compile"));
        task.dependsOn(build.getTaskName("check"));
      });
    });
  }

  private void configureBuildDefaults(Project project, SourceSetContainer sourceSets, ClojureExtension extension) {
    extension.getRootOutputDir().set(project.getLayout().getBuildDirectory().dir("clojure"));

    extension.getBuilds().configureEach(build -> {
      build.getOutputDir().convention(extension.getRootOutputDir().dir(build.getName()));

      build.getReflection().convention(ClojureCheck.REFLECTION_SILENT);
      build.getCompiler().getDirectLinking().convention(false);
      build.getCompiler().getDisableLocalsClearing().convention(false);
      build.getCompiler().getElideMeta().convention(Collections.emptyList());

      String checkTaskName = build.getTaskName("check");
      project.getTasks().register(checkTaskName, ClojureCheck.class, task -> {
        task.setDescription(String.format("Checks the Clojure source for the %s build.", build.getName()));
        task.setSource(build.getSourceTree());
        task.getClasspath().from(build.getSourceRoots());
        task.getClasspath().from(build.getClasspath());
        task.getReflection().set(build.getReflection());
        task.getNamespaces().set(build.getCheckNamespaces());
      });

      String compileTaskName = build.getTaskName("compile");
      TaskProvider<ClojureCompile> compileTask = project.getTasks().register(compileTaskName, ClojureCompile.class, task -> {
        task.setDescription(String.format("Compiles the Clojure source for the %s build.", build.getName()));
        task.getDestinationDir().set(build.getOutputDir());
        task.setSource(build.getSourceTree());
        task.getClasspath().from(build.getSourceRoots());
        task.getClasspath().from(build.getClasspath());
        task.getOptions().set(build.getCompiler());
        task.getNamespaces().set(build.getAotNamespaces());
      });

      // wire SourceDirectorySet properties per https://github.com/gradle/gradle/issues/11333
      SourceSet sourceSet = sourceSets.findByName(build.getName());
      if (sourceSet != null) {
        SourceDirectorySet source = (SourceDirectorySet) sourceSet.getExtensions().getByName(SOURCE_DIRECTORY_SET_NAME);
        source.compiledBy(compileTask, ClojureCompile::getDestinationDir);
      }
    });
  }

  private void configureCheckDefaults(TaskContainer tasks) {
    tasks.withType(ClojureCheck.class, task -> {
      task.getInternalOutputFile().set(new File(task.getTemporaryDir(), "internal.txt"));
    });
  }
}

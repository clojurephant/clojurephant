package dev.clojurephant.plugin.clojurescript;

import javax.inject.Inject;

import dev.clojurephant.plugin.clojurescript.tasks.ClojureScriptCompile;
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
import org.gradle.api.tasks.TaskProvider;

public class ClojureScriptBasePlugin implements Plugin<Project> {
  public static final String SOURCE_DIRECTORY_SET_NAME = "clojurescript";

  private final ObjectFactory objects;

  @Inject
  public ClojureScriptBasePlugin(ObjectFactory objects) {
    this.objects = objects;
  }

  @Override
  public void apply(Project project) {
    project.getPluginManager().apply(ClojureCommonBasePlugin.class);
    ClojureScriptExtension extension = project.getExtensions().create("clojurescript", ClojureScriptExtension.class);
    SourceSetContainer sourceSets = project.getExtensions().getByType(SourceSetContainer.class);
    configureSourceSetDefaults(project, sourceSets, extension);
    configureBuildDefaults(project, sourceSets, extension);
  }

  private void configureSourceSetDefaults(Project project, SourceSetContainer sourceSets, ClojureScriptExtension extension) {
    sourceSets.all((SourceSet sourceSet) -> {
      // every source set gets clojurescript source, following same convention as Java/Groovy
      SourceDirectorySet clojureScriptSource = objects.sourceDirectorySet(sourceSet.getName(), sourceSet.getName());
      clojureScriptSource.srcDir(String.format("src/%s/clojurescript", sourceSet.getName()));
      clojureScriptSource.getFilter().include(Namespaces.CLOJURESCRIPT_PATTERNS);

      // make the sources available on the source set
      sourceSet.getExtensions().add(SOURCE_DIRECTORY_SET_NAME, clojureScriptSource);

      // in case the clojurescript source overlaps with the resources source
      sourceSet.getResources().getFilter().exclude(element -> clojureScriptSource.contains(element.getFile()));

      // ensure that clojurescript is considered part of full source of source set
      sourceSet.getAllJava().source(clojureScriptSource);
      sourceSet.getAllSource().source(clojureScriptSource);

      // every source set gets a default clojurescript build
      ClojureScriptBuild build = extension.getBuilds().create(sourceSet.getName());
      build.getSourceRoots().from(clojureScriptSource.getSourceDirectories());
      clojureScriptSource.getDestinationDirectory().set(build.getOutputDir());

      build.getClasspath()
          .from(project.provider(() -> sourceSet.getCompileClasspath()))
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
      });
    });
  }

  private void configureBuildDefaults(Project project, SourceSetContainer sourceSets, ClojureScriptExtension extension) {
    extension.getRootOutputDir().set(project.getLayout().getBuildDirectory().dir("clojurescript"));

    extension.getBuilds().configureEach(build -> {
      build.getOutputDir().set(extension.getRootOutputDir().dir(build.getName()));

      build.getCompiler().getBaseOutputDirectory().set(build.getOutputDir());
      build.getCompiler().getModules().configureEach(module -> {
        module.getBaseOutputDirectory().set(build.getCompiler().getBaseOutputDirectory());
      });

      build.getFigwheel().getTargetDir().set(build.getOutputDir());
      build.getFigwheel().getWatchDirs().from(build.getSourceRoots());
      build.getFigwheel().getRebelReadline().set(false);
      build.getFigwheel().getHelpfulClasspaths().set(false);

      String compileTaskName = build.getTaskName("compile");
      TaskProvider<ClojureScriptCompile> compileTask = project.getTasks().register(compileTaskName, ClojureScriptCompile.class, task -> {
        task.setDescription(String.format("Compiles the ClojureScript source for the %s build.", build.getName()));
        task.getDestinationDir().set(build.getOutputDir());
        task.setSource(build.getSourceTree());
        task.getClasspath().from(build.getSourceRoots());
        task.getClasspath().from(build.getClasspath());
        task.getOptions().set(build.getCompiler());
      });

      // wire SourceDirectorySet properties per https://github.com/gradle/gradle/issues/11333
      SourceSet sourceSet = sourceSets.findByName(build.getName());
      if (sourceSet != null) {
        SourceDirectorySet source = (SourceDirectorySet) sourceSet.getExtensions().getByName(SOURCE_DIRECTORY_SET_NAME);
        source.compiledBy(compileTask, ClojureScriptCompile::getDestinationDir);
      }
    });
  }
}

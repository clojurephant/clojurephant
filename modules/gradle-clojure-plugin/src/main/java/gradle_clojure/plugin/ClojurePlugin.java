package gradle_clojure.plugin;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.function.Function;
import java.util.stream.Stream;

import gradle_clojure.plugin.tasks.ClojureCompile;
import gradle_clojure.plugin.tasks.ClojureNRepl;
import gradle_clojure.plugin.tasks.ClojureSourceSet;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.file.SourceDirectorySet;
import org.gradle.api.internal.plugins.DslObject;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.plugins.JavaPluginConvention;
import org.gradle.api.tasks.SourceSet;

public class ClojurePlugin implements Plugin<Project> {
  private static final String DEV_SOURCE_SET_NAME = "dev";

  @Override
  public void apply(Project project) {
    project.getPlugins().apply(ClojureBasePlugin.class);
    project.getPlugins().apply(JavaPlugin.class);

    JavaPluginConvention javaConvention = project.getConvention().getPlugin(JavaPluginConvention.class);
    configureTest(project, javaConvention);
    configureDev(project, javaConvention);
  }

  private void configureTest(Project project, JavaPluginConvention javaConvention) {
    SourceSet sourceSet = javaConvention.getSourceSets().getByName(SourceSet.TEST_SOURCE_SET_NAME);
    ClojureCompile compile = (ClojureCompile) project.getTasks().getByName(sourceSet.getCompileTaskName("clojure"));

    compile.getOptions().setAotCompile(true);

    Callable<?> namespaces = () -> {
      List<String> nses = new ArrayList<>();
      nses.add("gradle-clojure.tools.logger");
      nses.add("gradle-clojure.tools.clojure-test-junit4");
      nses.addAll(compile.findNamespaces());
      return nses;
    };

    compile.getConventionMapping().map("namespaces", namespaces);
  }

  private void configureDev(Project project, JavaPluginConvention javaConvention) {
    SourceSet main = javaConvention.getSourceSets().getByName(SourceSet.MAIN_SOURCE_SET_NAME);
    SourceSet test = javaConvention.getSourceSets().getByName(SourceSet.TEST_SOURCE_SET_NAME);
    SourceSet dev = javaConvention.getSourceSets().create(DEV_SOURCE_SET_NAME);

    SourceDirectorySet mainClojure = new DslObject(main).getConvention().getPlugin(ClojureSourceSet.class).getClojure();
    SourceDirectorySet testClojure = new DslObject(test).getConvention().getPlugin(ClojureSourceSet.class).getClojure();
    SourceDirectorySet devClojure = new DslObject(dev).getConvention().getPlugin(ClojureSourceSet.class).getClojure();

    dev.setCompileClasspath(project.files(
        test.getOutput(),
        main.getOutput(),
        project.getConfigurations().getByName(dev.getCompileClasspathConfigurationName())));
    dev.setRuntimeClasspath(project.files(
        devClojure.getSourceDirectories(),
        dev.getOutput().minus(project.files(devClojure.getOutputDir())),
        testClojure.getSourceDirectories(),
        test.getOutput().minus(project.files(testClojure.getOutputDir())),
        mainClojure.getSourceDirectories(),
        main.getOutput().minus(project.files(mainClojure.getOutputDir())),
        project.getConfigurations().getByName(dev.getRuntimeClasspathConfigurationName())));

    Stream.<Function<SourceSet, String>>of(
        SourceSet::getCompileConfigurationName,
        SourceSet::getImplementationConfigurationName,
        SourceSet::getRuntimeConfigurationName,
        SourceSet::getRuntimeOnlyConfigurationName).forEach(getter -> {
          Configuration devConf = project.getConfigurations().getByName(getter.apply(dev));
          Configuration testConf = project.getConfigurations().getByName(getter.apply(test));
          devConf.extendsFrom(testConf);
        });

    project.getTasks().create("clojureRepl", ClojureNRepl.class, task -> {
      task.setGroup("run");
      task.setDescription("Starts an nREPL server.");
      task.setClasspath(dev.getRuntimeClasspath());
    });
  }
}

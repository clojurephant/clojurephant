package gradle_clojure.plugin.clojure;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

import gradle_clojure.plugin.clojure.tasks.ClojureCompile;
import gradle_clojure.plugin.clojure.tasks.ClojureNRepl;
import gradle_clojure.plugin.clojure.tasks.ClojureSourceSet;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.internal.plugins.DslObject;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.plugins.JavaPluginConvention;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.testing.Test;
import org.gradle.jvm.tasks.Jar;

public class ClojurePlugin implements Plugin<Project> {
  private static final String DEV_SOURCE_SET_NAME = "dev";

  @Override
  public void apply(Project project) {
    project.getPlugins().apply(ClojureBasePlugin.class);
    project.getPlugins().apply(JavaPlugin.class);

    JavaPluginConvention javaConvention = project.getConvention().getPlugin(JavaPluginConvention.class);
    configureTest(project, javaConvention);
    configureAotJar(project, javaConvention);
    configureDev(project, javaConvention);

    project.getPlugins().withId("com.github.johnrengelman.shadow", plugin -> {
      Jar jar = (Jar) project.getTasks().getByName("shadowJar");
      jar.from(project.getTasks().getByName("compileClojure"));
    });
  }

  private void configureTest(Project project, JavaPluginConvention javaConvention) {
    SourceSet sourceSet = javaConvention.getSourceSets().getByName(SourceSet.TEST_SOURCE_SET_NAME);
    ClojureCompile compile = (ClojureCompile) project.getTasks().getByName(sourceSet.getCompileTaskName("clojure"));

    sourceSet.setRuntimeClasspath(project.files(
        compile,
        sourceSet.getRuntimeClasspath()));

    project.getTasks().withType(Test.class, task -> {
      task.setTestClassesDirs(project.files(
          compile,
          sourceSet.getOutput().getClassesDirs()));
    });

    compile.getNamespaces().add("gradle-clojure.tools.logger");
    compile.getNamespaces().add("gradle-clojure.tools.clojure-test-junit4");
  }

  private void configureAotJar(Project project, JavaPluginConvention javaConvention) {
    SourceSet sourceSet = javaConvention.getSourceSets().getByName(SourceSet.MAIN_SOURCE_SET_NAME);
    ClojureSourceSet clojure = (ClojureSourceSet) new DslObject(sourceSet).getConvention().getPlugins().get("clojure");
    Jar jar = project.getTasks().create("aotJar", Jar.class);
    jar.from(sourceSet.getOutput().minus(project.files(clojure.getClojure().getSrcDirs())));
    jar.from(project.getTasks().getByName(sourceSet.getCompileTaskName("clojure")));
    jar.dependsOn(String.format("%sAot", sourceSet.getClassesTaskName()));
  }

  private void configureDev(Project project, JavaPluginConvention javaConvention) {
    SourceSet main = javaConvention.getSourceSets().getByName(SourceSet.MAIN_SOURCE_SET_NAME);
    SourceSet test = javaConvention.getSourceSets().getByName(SourceSet.TEST_SOURCE_SET_NAME);
    SourceSet dev = javaConvention.getSourceSets().create(DEV_SOURCE_SET_NAME);

    Configuration nrepl = project.getConfigurations().getByName(ClojureBasePlugin.NREPL_CONFIGURATION_NAME);
    project.getConfigurations().getByName(dev.getCompileClasspathConfigurationName()).extendsFrom(nrepl);
    project.getConfigurations().getByName(dev.getRuntimeClasspathConfigurationName()).extendsFrom(nrepl);

    dev.setCompileClasspath(project.files(
        test.getOutput(),
        main.getOutput(),
        project.getConfigurations().getByName(dev.getCompileClasspathConfigurationName())));
    dev.setRuntimeClasspath(project.files(
        dev.getAllSource(),
        dev.getOutput(),
        test.getAllSource(),
        test.getOutput(),
        main.getAllSource(),
        main.getOutput(),
        project.getConfigurations().getByName(dev.getRuntimeClasspathConfigurationName())));

    Consumer<Function<SourceSet, String>> devExtendsTest = getConfName -> {
      Configuration devConf = project.getConfigurations().getByName(getConfName.apply(dev));
      Configuration testConf = project.getConfigurations().getByName(getConfName.apply(test));
      devConf.extendsFrom(testConf);
    };

    devExtendsTest.accept(SourceSet::getCompileConfigurationName);
    devExtendsTest.accept(SourceSet::getImplementationConfigurationName);
    devExtendsTest.accept(SourceSet::getRuntimeConfigurationName);
    devExtendsTest.accept(SourceSet::getRuntimeOnlyConfigurationName);

    project.getTasks().create("clojureRepl", ClojureNRepl.class, task -> {
      task.setGroup("run");
      task.setDescription("Starts an nREPL server.");
      task.setClasspath(dev.getRuntimeClasspath());
      Stream.of(main, test, dev).forEach(sourceSet -> {
        task.dependsOn(project.getTasks().getByName(sourceSet.getCompileJavaTaskName()));
        task.dependsOn(project.getTasks().getByName(sourceSet.getProcessResourcesTaskName()));
      });
    });
  }
}

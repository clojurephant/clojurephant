package dev.clojurephant.plugin.clojurescript;

import dev.clojurephant.plugin.clojurescript.tasks.ClojureScriptCompileOptions;
import dev.clojurephant.plugin.clojurescript.tasks.FigwheelOptions;
import org.apache.commons.text.WordUtils;
import org.gradle.api.Action;
import org.gradle.api.Named;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.FileTree;
import org.gradle.api.tasks.Nested;

public abstract class ClojureScriptBuild implements Named {
  public abstract DirectoryProperty getOutputDir();

  public abstract ConfigurableFileCollection getClasspath();

  public abstract ConfigurableFileCollection getSourceRoots();

  public FileTree getSourceTree() {
    return getSourceRoots().getAsFileTree();
  }

  boolean isCompilerConfigured() {
    return getCompiler().getOutputTo().isPresent() || getCompiler().getModules().stream()
        .anyMatch(module -> module.getOutputTo().isPresent());
  }

  @Nested
  public abstract ClojureScriptCompileOptions getCompiler();

  public void compiler(Action<? super ClojureScriptCompileOptions> configureAction) {
    configureAction.execute(getCompiler());
  }

  @Nested
  public abstract FigwheelOptions getFigwheel();

  public void figwheel(Action<? super FigwheelOptions> configureAction) {
    configureAction.execute(getFigwheel());
  }

  String getTaskName(String task) {
    if ("main".equals(getName())) {
      return String.format("%sClojureScript", task);
    } else {
      return String.format("%s%sClojureScript", task, WordUtils.capitalize(getName()));
    }
  }
}

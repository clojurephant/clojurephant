package gradle_clojure.plugin.common.internal;

import static us.bpsm.edn.Keyword.newKeyword;

import java.io.File;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import gradle_clojure.plugin.clojure.tasks.ClojureCompileOptions;
import gradle_clojure.plugin.clojurescript.ClojureScriptBuild;
import gradle_clojure.plugin.clojurescript.tasks.ClojureScriptCompileOptions;
import gradle_clojure.plugin.clojurescript.tasks.FigwheelOptions;
import gradle_clojure.plugin.clojurescript.tasks.ForeignLib;
import gradle_clojure.plugin.clojurescript.tasks.Module;
import org.gradle.api.NamedDomainObjectCollection;
import org.gradle.api.file.Directory;
import org.gradle.api.file.FileCollection;
import org.gradle.api.provider.Provider;
import us.bpsm.edn.Keyword;
import us.bpsm.edn.Symbol;
import us.bpsm.edn.parser.Parseable;
import us.bpsm.edn.parser.Parser;
import us.bpsm.edn.parser.Parsers;
import us.bpsm.edn.printer.Printer;
import us.bpsm.edn.printer.Printers;
import us.bpsm.edn.protocols.Protocol;

public class Edn {
  private static final Printer.Fn<Enum<?>> ENUM_PRINTER = (self, printer) -> printer.printValue(newKeyword(self.name()));

  private static final Printer.Fn<File> FILE_PRINTER = (self, printer) -> printer.printValue(self.getAbsolutePath());

  private static final Printer.Fn<FileCollection> FILE_COLLECTION_PRINTER = (self, printer) -> {
    if (self.isEmpty()) {
      printer.printValue(null);
    } else {
      printer.printValue(self.getFiles().stream().collect(Collectors.toList()));
    }
  };

  private static final Printer.Fn<NamedDomainObjectCollection<?>> NAMED_DOMAIN_PRINTER = (self, printer) -> {
    printer.printValue(self.isEmpty() ? null : self.getAsMap());
  };

  private static final Printer.Fn<Provider<?>> PROVIDER_PRINTER = (self, printer) -> printer.printValue(self.getOrNull());

  private static final Printer.Fn<ClojureCompileOptions> CLOJURE_COMPILE_OPTIONS_PRINTER = (self, printer) -> {
    Map<Object, Object> root = new LinkedHashMap<>();
    root.put(newKeyword("disable-locals-clearing"), self.isDisableLocalsClearing());
    root.put(newKeyword("direct-linking"), self.isDirectLinking());
    root.put(newKeyword("elide-metadata"), self.getElideMeta().stream().map(Keyword::newKeyword).collect(Collectors.toList()));
    printer.printValue(root);
  };

  private static final Printer.Fn<ClojureScriptBuild> CLOJURESCRIPT_BUILD_PRINTER = (self, printer) -> {
    Map<Object, Object> root = new LinkedHashMap<>();
    root.put(newKeyword("compiler"), self.getCompiler());
    root.put(newKeyword("figwheel"), self.getFigwheel());
    printer.printValue(root);
  };

  private static final Printer.Fn<FigwheelOptions> FIGWHEEL_OPTIONS_PRINTER = (self, printer) -> {
    Map<Object, Object> root = new LinkedHashMap<>();
    root.put(newKeyword("watch-dirs"), self.getWatchDirs());
    root.put(newKeyword("css-dirs"), self.getCssDirs());
    root.put(newKeyword("ring-handler"), Edn.toSymbol(self.getRingHandler()));
    root.put(newKeyword("ring-server-options"), keywordize(self.getRingServerOptions()));
    root.put(newKeyword("rebel-readline"), self.getRebelReadline());
    root.put(newKeyword("pprint-config"), self.getPprintConfig());
    root.put(newKeyword("open-file-command"), self.getOpenFileCommand());
    root.put(newKeyword("figwheel-core"), self.getFigwheelCore());
    root.put(newKeyword("hot-reload-cljs"), self.getHotReloadCljs());
    root.put(newKeyword("connect-url"), self.getConnectUrl());
    root.put(newKeyword("open-url"), self.getOpenUrl());
    root.put(newKeyword("reload-clj-files"), self.getReloadCljFiles());
    root.put(newKeyword("log-file"), self.getLogFile().getOrNull());
    root.put(newKeyword("log-level"), Edn.toKeyword(self.getLogLevel()));
    root.put(newKeyword("client-log-level"), Edn.toKeyword(self.getClientLogLevel()));
    root.put(newKeyword("log-syntax-error-style"), Edn.toKeyword(self.getLogSyntaxErrorStyle()));
    root.put(newKeyword("load-warninged-code"), self.getLoadWarningedCode());
    root.put(newKeyword("ansi-color-output"), self.getAnsiColorOutput());
    root.put(newKeyword("validate-config"), self.getValidateConfig());
    root.put(newKeyword("target-dir"), self.getTargetDir().map(Directory::getAsFile).getOrNull());
    root.put(newKeyword("launch-node"), self.getLaunchNode());
    root.put(newKeyword("inspect-node"), self.getInspectNode());
    root.put(newKeyword("node-command"), self.getNodeCommand());
    root.put(newKeyword("cljs-devtools"), self.getCljsDevtools());

    Edn.removeEmptyAndNulls(root);
    printer.printValue(root);
  };

  private static final Printer.Fn<ClojureScriptCompileOptions> CLOJURESCRIPT_COMPILE_OPTIONS_PRINTER = (self, printer) -> {
    Map<Object, Object> map = new LinkedHashMap<>();
    map.put(newKeyword("output-to"), self.getOutputTo().getAsFile().getOrNull());
    map.put(newKeyword("output-dir"), self.getOutputDir().getAsFile().getOrNull());
    map.put(newKeyword("optimizations"), self.getOptimizations());
    map.put(newKeyword("main"), self.getMain());
    map.put(newKeyword("asset-path"), self.getAssetPath());
    map.put(newKeyword("source-map"), self.getSourceMap());
    map.put(newKeyword("verbose"), self.getVerbose());
    map.put(newKeyword("pretty-print"), self.getPrettyPrint());
    map.put(newKeyword("target"), self.getTarget());
    map.put(newKeyword("foreign-libs"), self.getForeignLibs());
    map.put(newKeyword("externs"), self.getExterns());
    map.put(newKeyword("modules"), parseModules(self.getModules()));
    map.put(newKeyword("preloads"), parsePreloads(self.getPreloads()));
    map.put(newKeyword("npm-deps"), self.getNpmDeps());
    map.put(newKeyword("install-deps"), self.getInstallDeps());
    map.put(newKeyword("checked-arrays"), self.getCheckedArrays());

    Edn.removeEmptyAndNulls(map);
    printer.printValue(map);
  };

  private static Map<Keyword, Module> parseModules(Map<String, Module> modules) {
    return modules.entrySet().stream()
        .collect(Collectors.toMap(
            e -> newKeyword(e.getKey()),
            Map.Entry::getValue));
  }

  private static List<Symbol> parsePreloads(Collection<String> preloads) {
    if (preloads == null) {
      return null;
    }

    return preloads.stream()
        .map(Symbol::newSymbol)
        .collect(Collectors.toList());
  }

  private static final Printer.Fn<ForeignLib> FOREIGN_LIB_PRINTER = (self, printer) -> {
    Map<Object, Object> map = new LinkedHashMap<>();
    map.put(newKeyword("file"), self.getFile());
    map.put(newKeyword("file-min"), self.getFileMin());
    map.put(newKeyword("provides"), self.getProvides());
    map.put(newKeyword("requires"), self.getRequires());
    map.put(newKeyword("module-type"), self.getModuleType());
    map.put(newKeyword("preprocess"), parsePreprocess(self.getPreprocess()));
    map.put(newKeyword("global-exports"), parseGlobalExports(self.getGlobalExports()));

    Edn.removeEmptyAndNulls(map);
    printer.printValue(map);
  };

  private static Object parsePreprocess(String value) {
    if (value == null) {
      return null;
    }

    Parseable parseable = Parsers.newParseable(value);
    Parser parser = Parsers.newParser(Parsers.defaultConfiguration());
    return parser.nextValue(parseable);
  }

  private static Map<Symbol, Symbol> parseGlobalExports(Map<String, String> globalExports) {
    return globalExports.entrySet().stream()
        .collect(Collectors.toMap(
            e -> Symbol.newSymbol(e.getKey()),
            e -> Symbol.newSymbol(e.getValue())));
  }

  private static final Printer.Fn<Module> MODULE_PRINTER = (module, printer) -> {
    Map<Object, Object> map = new LinkedHashMap<>();
    map.put(newKeyword("output-to"), module.getOutputTo().getAsFile().getOrNull());
    map.put(newKeyword("entries"), module.getEntries());
    map.put(newKeyword("dependsOn"), module.getDependsOn());
    Edn.removeEmptyAndNulls(map);
    printer.printValue(map);
  };

  private static final Protocol<Printer.Fn<?>> PROTOCOL = Printers.prettyProtocolBuilder()
      // Core Printers
      .put(Enum.class, ENUM_PRINTER)
      .put(File.class, FILE_PRINTER)
      .put(FileCollection.class, FILE_COLLECTION_PRINTER)
      .put(NamedDomainObjectCollection.class, NAMED_DOMAIN_PRINTER)
      .put(Provider.class, PROVIDER_PRINTER)
      // Clojure
      .put(ClojureCompileOptions.class, CLOJURE_COMPILE_OPTIONS_PRINTER)
      // ClojureScript
      .put(ClojureScriptBuild.class, CLOJURESCRIPT_BUILD_PRINTER)
      .put(FigwheelOptions.class, FIGWHEEL_OPTIONS_PRINTER)
      .put(ClojureScriptCompileOptions.class, CLOJURESCRIPT_COMPILE_OPTIONS_PRINTER)
      .put(ForeignLib.class, FOREIGN_LIB_PRINTER)
      .put(Module.class, MODULE_PRINTER)
      .build();

  public static String print(Object value) {
    return Printers.printString(PROTOCOL, value);
  }

  private static Symbol toSymbol(String name) {
    return Optional.ofNullable(name)
        .map(Symbol::newSymbol)
        .orElse(null);
  }

  private static Keyword toKeyword(String name) {
    return Optional.ofNullable(name)
        .map(Keyword::newKeyword)
        .orElse(null);
  }

  public static <V> Map<Keyword, V> keywordize(Map<String, V> map) {
    return map.entrySet().stream()
        .collect(Collectors.toMap(e -> newKeyword(e.getKey()), e -> e.getValue()));
  }

  private static <K, V> void removeEmptyAndNulls(Map<K, V> map) {
    map.values().removeIf(Objects::isNull);
    map.values().removeIf(obj -> (obj instanceof Collection) && ((Collection<?>) obj).isEmpty());
    map.values().removeIf(obj -> (obj instanceof Map) && ((Map<?, ?>) obj).isEmpty());
  }
}

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
import gradle_clojure.plugin.clojurescript.tasks.ClojureScriptCompileOptions;
import gradle_clojure.plugin.clojurescript.tasks.ForeignLib;
import gradle_clojure.plugin.clojurescript.tasks.Module;
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

  private static final Printer.Fn<ClojureCompileOptions> CLOJURE_COMPILE_OPTIONS_PRINTER = (self, printer) -> {
    Map<Object, Object> root = new LinkedHashMap<>();
    root.put(newKeyword("disable-locals-clearing"), self.isDisableLocalsClearing());
    root.put(newKeyword("direct-linking"), self.isDirectLinking());
    root.put(newKeyword("elide-metadata"), self.getElideMeta().stream().map(Keyword::newKeyword).collect(Collectors.toList()));
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
    map.put(newKeyword("foreign-libs"), Edn.emptyToNull(self.getForeignLibs()));
    map.put(newKeyword("externs"), Edn.emptyToNull(self.getExterns()));
    map.put(newKeyword("modules"), parseModules(self.getModules()));
    map.put(newKeyword("preloads"), parsePreloads(self.getPreloads()));
    map.put(newKeyword("npm-deps"), Edn.emptyToNull(self.getNpmDeps()));
    map.put(newKeyword("install-deps"), self.getInstallDeps());
    map.put(newKeyword("checked-arrays"), self.getCheckedArrays());

    map.values().removeIf(Objects::isNull);
    printer.printValue(map);
  };

  private static Map<Keyword, Module> parseModules(Map<String, Module> modules) {
    Map<Keyword, Module> result = modules.entrySet().stream()
        .collect(Collectors.toMap(
            e -> newKeyword(e.getKey()),
            Map.Entry::getValue));

    return Edn.emptyToNull(result);
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

    map.values().removeIf(Objects::isNull);
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
    map.values().removeIf(Objects::isNull);
    printer.printValue(map);
  };

  private static final Protocol<Printer.Fn<?>> PROTOCOL = Printers.prettyProtocolBuilder()
      // Core Printers
      .put(Enum.class, ENUM_PRINTER)
      .put(File.class, FILE_PRINTER)
      // Clojure
      .put(ClojureCompileOptions.class, CLOJURE_COMPILE_OPTIONS_PRINTER)
      // ClojureScript
      .put(ClojureScriptCompileOptions.class, CLOJURESCRIPT_COMPILE_OPTIONS_PRINTER)
      .put(ForeignLib.class, FOREIGN_LIB_PRINTER)
      .put(Module.class, MODULE_PRINTER)
      .build();

  public static String print(Object value) {
    return Printers.printString(PROTOCOL, value);
  }

  private static <T extends Collection<?>> T emptyToNull(T collection) {
    return Optional.ofNullable(collection)
        .filter(c -> !c.isEmpty())
        .orElse(null);
  }

  private static <T extends Map<?, ?>> T emptyToNull(T collection) {
    return Optional.ofNullable(collection)
        .filter(c -> !c.isEmpty())
        .orElse(null);
  }
}

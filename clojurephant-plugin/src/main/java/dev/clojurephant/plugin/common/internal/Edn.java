package dev.clojurephant.plugin.common.internal;

import static us.bpsm.edn.Keyword.newKeyword;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import dev.clojurephant.plugin.clojure.tasks.ClojureCompileOptions;
import dev.clojurephant.plugin.clojurescript.ClojureScriptBuild;
import dev.clojurephant.plugin.clojurescript.tasks.ClojureScriptCompileOptions;
import dev.clojurephant.plugin.clojurescript.tasks.ForeignLib;
import dev.clojurephant.plugin.clojurescript.tasks.Module;
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

  private static final Printer.Fn<Directory> DIRECTORY_PRINTER = (self, printer) -> printer.printValue(self.getAsFile());

  private static final Printer.Fn<FileCollection> FILE_COLLECTION_PRINTER = (self, printer) -> {
    List<File> list = self.getFiles().stream()
        .filter(File::exists)
        .collect(Collectors.toList());
    printer.printValue(list);
  };

  private static final Printer.Fn<NamedDomainObjectCollection<?>> NAMED_DOMAIN_PRINTER = (self, printer) -> {
    printer.printValue(self.isEmpty() ? null : self.getAsMap());
  };

  private static final Printer.Fn<Provider<?>> PROVIDER_PRINTER = (self, printer) -> printer.printValue(self.getOrNull());

  private static final Printer.Fn<ClojureCompileOptions> CLOJURE_COMPILE_OPTIONS_PRINTER = (self, printer) -> {
    Map<Object, Object> root = new LinkedHashMap<>();
    root.put(newKeyword("disable-locals-clearing"), self.getDisableLocalsClearing().get());
    root.put(newKeyword("direct-linking"), self.getDirectLinking().get());
    root.put(newKeyword("elide-metadata"), self.getElideMeta().get().stream()
        .map(Keyword::newKeyword)
        .collect(Collectors.toList()));
    printer.printValue(root);
  };

  private static final Printer.Fn<ClojureScriptBuild> CLOJURESCRIPT_BUILD_PRINTER = (self, printer) -> {
    Map<Object, Object> root = new LinkedHashMap<>();
    root.put(newKeyword("output-dir"), self.getOutputDir().map(Directory::getAsFile).getOrNull());
    root.put(newKeyword("compiler"), self.getCompiler());
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

    map.put(newKeyword("devcards"), self.getDevcards());

    Edn.removeEmptyAndNulls(map);
    printer.printValue(map);
  };

  private static Map<Keyword, Module> parseModules(Map<String, Module> modules) {
    return modules.entrySet().stream()
        .collect(Collectors.toMap(
            e -> newKeyword(e.getKey()),
            Map.Entry::getValue));
  }

  private static List<?> parsePreloads(Collection<String> preloads) {
    if (preloads == null) {
      return null;
    }

    List<Symbol> syms = preloads.stream()
        .map(Symbol::newSymbol)
        .collect(Collectors.toList());
    return list(Symbol.newSymbol("quote"), syms);
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
      .put(Directory.class, DIRECTORY_PRINTER)
      .put(FileCollection.class, FILE_COLLECTION_PRINTER)
      .put(NamedDomainObjectCollection.class, NAMED_DOMAIN_PRINTER)
      .put(Provider.class, PROVIDER_PRINTER)
      // Clojure
      .put(ClojureCompileOptions.class, CLOJURE_COMPILE_OPTIONS_PRINTER)
      // ClojureScript
      .put(ClojureScriptBuild.class, CLOJURESCRIPT_BUILD_PRINTER)
      .put(ClojureScriptCompileOptions.class, CLOJURESCRIPT_COMPILE_OPTIONS_PRINTER)
      .put(ForeignLib.class, FOREIGN_LIB_PRINTER)
      .put(Module.class, MODULE_PRINTER)
      .build();

  public static String print(Object value) {
    return Printers.printString(PROTOCOL, value);
  }

  public static <V> Map<Keyword, V> keywordize(Map<String, V> map) {
    return map.entrySet().stream()
        .collect(Collectors.toMap(e -> newKeyword(e.getKey()), e -> e.getValue()));
  }

  private static <K, V> void removeEmptyAndNulls(Map<K, V> map) {
    map.values().removeIf(Objects::isNull);
    map.values().removeIf(obj -> (obj instanceof Collection) && ((Collection<?>) obj).isEmpty());
    map.values().removeIf(obj -> (obj instanceof Map) && ((Map<?, ?>) obj).isEmpty());
    map.values().removeIf(obj -> (obj instanceof FileCollection) && ((FileCollection) obj).isEmpty());
  }

  public static List<Object> list(Object... elements) {
    return Arrays.stream(elements).collect(Collectors.toCollection(LinkedList::new));
  }

  public static List<Object> vector(Object... elements) {
    return Arrays.stream(elements).collect(Collectors.toCollection(ArrayList::new));
  }
}

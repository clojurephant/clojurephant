/*
 * Copyright 2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package gradle_clojure.plugin.internal;

import gradle_clojure.plugin.tasks.clojurescript.ClojurescriptCompileOptions;
import gradle_clojure.plugin.tasks.clojurescript.ForeignLib;
import gradle_clojure.plugin.tasks.clojurescript.Module;
import us.bpsm.edn.Keyword;
import us.bpsm.edn.Symbol;
import us.bpsm.edn.parser.Parseable;
import us.bpsm.edn.parser.Parser;
import us.bpsm.edn.parser.Parsers;
import us.bpsm.edn.printer.Printer;
import us.bpsm.edn.printer.Printers;
import us.bpsm.edn.protocols.Protocol;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

import static us.bpsm.edn.Keyword.newKeyword;

public class CljsEdnUtils {
  private static <T extends Collection<?>> T emptyToNull(T collection) {
    return collection == null || collection.isEmpty()
        ? null
        : collection;
  }

  private static <T extends Map<?, ?>> T emptyToNull(T collection) {
    return collection == null || collection.isEmpty()
        ? null
        : collection;
  }

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

  private static Map<Keyword, Module> parseModules(Map<String, Module> modules) {
    Map<Keyword, Module> result = modules.entrySet().stream()
        .collect(Collectors.toMap(
            e -> newKeyword(e.getKey()),
            Map.Entry::getValue));

    return emptyToNull(result);
  }

  private static List<Symbol> parsePreloads(Collection<String> preloads) {
    if (preloads == null) {
      return null;
    }

    return preloads.stream()
        .map(Symbol::newSymbol)
        .collect(Collectors.toList());
  }

  private static final Printer.Fn<ClojurescriptCompileOptions> CLOJURESCRIPT_COMPILE_OPTIONS_PROTOCOL = (self, printer) -> {
    Map<Object, Object> map = new LinkedHashMap<>();
    map.put(newKeyword("output-to"), self.getOutputTo());
    map.put(newKeyword("output-dir"), self.getOutputDir());
    map.put(newKeyword("optimizations"), self.getOptimizations());
    map.put(newKeyword("main"), self.getMain());
    map.put(newKeyword("asset-path"), self.getAssetPath());
    map.put(newKeyword("source-map"), self.getSourceMap());
    map.put(newKeyword("verbose"), self.getVerbose());
    map.put(newKeyword("pretty-print"), self.getPrettyPrint());
    map.put(newKeyword("target"), self.getTarget());
    map.put(newKeyword("foreign-libs"), emptyToNull(self.getForeignLibs()));
    map.put(newKeyword("externs"), emptyToNull(self.getExterns()));
    map.put(newKeyword("modules"), parseModules(self.getModules()));
    map.put(newKeyword("preloads"), parsePreloads(self.getPreloads()));
    map.put(newKeyword("npm-deps"), emptyToNull(self.getNpmDeps()));
    map.put(newKeyword("install-deps"), self.getInstallDeps());
    map.put(newKeyword("checked-arrays"), self.getCheckedArrays());

    map.values().removeIf(Objects::isNull);
    printer.printValue(map);
  };

  private static final Printer.Fn<Enum<?>> ENUM_PROTOCOL = (self, printer) -> printer.printValue(newKeyword(self.name()));
  private static final Printer.Fn<File> FILE_PROTOCOL = (self, printer) -> printer.printValue(self.getAbsolutePath());

  private static final Printer.Fn<ForeignLib> FOREIGN_LIB_PROTOCOL = (self, printer) -> {
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

  private static final Printer.Fn<Module> MODULE_PROTOCOL = (module, printer) -> {
    Map<Object, Object> map = new LinkedHashMap<>();
    map.put(newKeyword("output-to"), module.getOutputTo());
    map.put(newKeyword("entries"), module.getEntries());
    map.put(newKeyword("dependsOn"), module.getDependsOn());
    map.values().removeIf(Objects::isNull);
    printer.printValue(map);
  };


  private static final Protocol<Printer.Fn<?>> CLOJURESCRIPT_COMPILER_PROTOCOL = Printers.prettyProtocolBuilder()
      .put(ClojurescriptCompileOptions.class, CLOJURESCRIPT_COMPILE_OPTIONS_PROTOCOL)
      .put(Enum.class, ENUM_PROTOCOL)
      .put(File.class, FILE_PROTOCOL)
      .put(ForeignLib.class, FOREIGN_LIB_PROTOCOL)
      .put(Module.class, MODULE_PROTOCOL)
      .build();

  public static String compilerOptionsToEdn(Set<String> sourceRoots, ClojurescriptCompileOptions compilerOptions) {
    Map<Object, Object> options = new LinkedHashMap<>();
    options.put(newKeyword("source-dirs"), sourceRoots);
    options.put(newKeyword("compiler-options"), compilerOptions);

    return Printers.printString(CLOJURESCRIPT_COMPILER_PROTOCOL, options);
  }
}

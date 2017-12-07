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

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.projectodd.shimdandy.ClojureRuntimeShim;

public class ClojureWorker implements Runnable {
  private final String namespace;
  private final String function;
  private final Object[] args;

  @Inject
  public ClojureWorker(String namespace, String function, Object[] args) {
    this.namespace = namespace;
    this.function = function;
    this.args = args;
  }

  @Override
  public void run() {
    try (ClojureRuntime runtime = ClojureRuntime.get()) {
      ClojureRuntimeShim shim = runtime.getShim();

      // (require namespace)
      shim.require(namespace);

      // (apply namespace/function args)
      Object sym = shim.invoke("clojure.core/symbol", namespace, function);
      Object var = shim.invoke("clojure.core/find-var", sym);
      shim.invoke("clojure.core/apply", var, args);
    }
  }

  private static class ClojureRuntime implements AutoCloseable {
    private final URLClassLoader loader;
    private final ClojureRuntimeShim shim;

    private ClojureRuntime(URLClassLoader loader, ClojureRuntimeShim shim) {
      this.loader = loader;
      this.shim = shim;
    }

    public ClojureRuntimeShim getShim() {
      return shim;
    }

    @Override
    public void close() {
      try {
        shim.close();
        loader.close();
      } catch (IOException e) {
        // don't care
      }
    }

    public static ClojureRuntime get() {
      String[] classpathElements = System.getProperty("shim.classpath").split(File.pathSeparator);
      URL[] classpathUrls = Arrays.stream(classpathElements)
          .map(Paths::get)
          .map(Path::toUri)
          .map(safe(URI::toURL))
          .toArray(size -> new URL[size]);

      URLClassLoader loader = new ClojureWorkerClassLoader(classpathUrls, ClojureWorker.class.getClassLoader());
      ClojureRuntimeShim shim = ClojureRuntimeShim.newRuntime(loader, "gradle-clojure");
      return new ClojureRuntime(loader, shim);
    }

    private static <T, R> Function<T, R> safe(FunctionThrows<T, R> fun) {
      return fun;
    }

    private static interface FunctionThrows<T, R> extends Function<T, R> {
      @Override
      default R apply(T arg) {
        try {
          return applyThrows(arg);
        } catch (Exception e) {
          sneakyThrows(e);
          return null;
        }
      }

      R applyThrows(T arg) throws Exception;
    }

    @SuppressWarnings("unchecked")
    private static <T extends Throwable> void sneakyThrows(Throwable t) throws T {
      throw (T) t;
    }
  }
}

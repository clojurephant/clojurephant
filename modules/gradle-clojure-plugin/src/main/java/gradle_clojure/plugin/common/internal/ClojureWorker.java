package gradle_clojure.plugin.common.internal;

import java.io.File;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.inject.Inject;

import org.projectodd.shimdandy.ClojureRuntimeShim;

public class ClojureWorker implements Runnable {
  private static final Set<String> CLASSLOADER_WHITELIST = Stream.of(
      // shimdandy api is loaded outside the shim
      "org.projectodd.shimdandy.",
      // base java classes
      "java.",
      "javax.",
      "jdk.",
      "sun.",
      "com.sun.",
      "org.ietf.",
      "org.omg.",
      "org.w3c.",
      "org.xml.").collect(Collectors.toSet());

  private static final UUID workerId = UUID.randomUUID();
  private static final AtomicInteger workerUseCounter = new AtomicInteger(0);

  private final String namespace;
  private final Object[] args;
  private final Set<File> classpath;

  @Inject
  public ClojureWorker(String namespace, Object[] args, Set<File> classpath) {
    this.namespace = namespace;
    this.args = args;
    this.classpath = classpath;
  }

  @Override
  public void run() {
    // Log some diagnostic information about the worker
    String logLevel = System.getProperty("gradle-clojure.tools.logger.level");
    if ("debug".equals(logLevel) || "info".equals(logLevel)) {
      System.out.println(String.format("INFO Worker process  %s has been used %d times.", workerId, workerUseCounter.incrementAndGet()));
    }

    // open a new runtime and execute the requested function
    try (ClojureRuntime runtime = ClojureRuntime.get(classpath)) {
      ClojureRuntimeShim shim = runtime.getShim();

      // (require namespace)
      shim.require(namespace);

      // (apply namespace/function args)
      Object sym = shim.invoke("clojure.core/symbol", namespace, "-main");
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
      } catch (Throwable t) {
        // don't care
      }
      try {
        loader.close();
      } catch (Throwable t) {
        // double don't care
      }
    }

    public static ClojureRuntime get(Set<File> classpath) {
      URL[] classpathUrls = classpath.stream()
          .map(File::toURI)
          .map(safe(URI::toURL))
          .toArray(size -> new URL[size]);

      URLClassLoader parent = new WhitelistClassLoader(CLASSLOADER_WHITELIST, ClojureWorker.class.getClassLoader());
      URLClassLoader loader = new URLClassLoader(classpathUrls, parent);
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

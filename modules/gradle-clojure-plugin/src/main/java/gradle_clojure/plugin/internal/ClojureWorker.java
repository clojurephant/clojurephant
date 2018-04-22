package gradle_clojure.plugin.internal;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import javax.inject.Inject;

import org.projectodd.shimdandy.ClojureRuntimeShim;

public class ClojureWorker implements Runnable {
  private static final UUID workerId = UUID.randomUUID();
  private static final AtomicInteger workerUseCounter = new AtomicInteger(0);

  private final UUID workerInstanceId = UUID.randomUUID();
  private final AtomicInteger workerInstanceUseCounter = new AtomicInteger(0);

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
    // Log some diagnostic information about the worker
    String logLevel = System.getProperty("gradle-clojure.tools.logger.level");
    if ("debug".equals(logLevel) || "info".equals(logLevel)) {
      System.out.println(String.format("INFO Worker process  %s has been used %d times.", workerId, workerUseCounter.incrementAndGet()));
      System.out.println(String.format("INFO Worker instance %s has been used %d times.", workerInstanceId, workerInstanceUseCounter.incrementAndGet()));
    }

    // open a new runtime and execute the requested function
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

package dev.clojurephant.plugin.common.internal;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UncheckedIOException;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.channels.AsynchronousCloseException;
import java.nio.channels.Channels;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import us.bpsm.edn.Keyword;
import us.bpsm.edn.parser.Parser;
import us.bpsm.edn.parser.Parsers;

public class PreplClient implements AutoCloseable {
  private static final Keyword TAG = Keyword.newKeyword("tag");

  private static final Keyword RET = Keyword.newKeyword("ret");
  private static final Keyword OUT = Keyword.newKeyword("out");
  private static final Keyword ERR = Keyword.newKeyword("err");
  private static final Keyword TAP = Keyword.newKeyword("tap");

  private static final Keyword VAL = Keyword.newKeyword("val");
  private static final Keyword NS = Keyword.newKeyword("ns");
  private static final Keyword MS = Keyword.newKeyword("ms");
  private static final Keyword FORM = Keyword.newKeyword("form");
  private static final Keyword EXCEPTION = Keyword.newKeyword("exception");

  private final BufferedReader reader;
  private final PrintWriter writer;
  private final AtomicBoolean serverStopped;

  private final Parser parser;

  private boolean closed = false;

  private final BlockingQueue<Map<Object, Object>> results = new LinkedBlockingQueue<>();
  private final BlockingQueue<Map<Object, Object>> output = new LinkedBlockingQueue<>();
  private final BlockingQueue<Map<Object, Object>> taps = new LinkedBlockingQueue<>();
  private final Thread inputThread;

  private PreplClient(BufferedReader reader, PrintWriter writer, AtomicBoolean serverStopped) {
    this.reader = reader;
    this.writer = writer;
    this.serverStopped = serverStopped;

    this.parser = Parsers.newParser(Parsers.defaultConfiguration());

    this.inputThread = new Thread(this::inputLoop);
  }

  void start() {
    boolean started = false;
    this.inputThread.start();
    try {
      // we're waiting for this promise to exist, so give it a few trys
      for (int i = 0; i < 30; i++) {
        if (serverStopped.get()) {
          break;
        }
        if (i != 0) {
          Thread.sleep(1000);
        }
        try {
          evalEdn("(do (require 'dev.clojurephant.prepl) (deliver dev.clojurephant.prepl/connected true))");
          started = true;
          break;
        } catch (ClojureException | ClassCastException e) {
          // retry
        }
      }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }

    if (!started) {
      throw new IllegalStateException(String.format("Timed out trying to initiate communication with prepl."));
    }
  }

  public void close() {
    closed = true;
    inputThread.interrupt();
    writer.close();
  }

  public Object evalData(Object form) throws InterruptedException {
    String ednForm = Edn.print(form);
    return evalEdn(ednForm);
  }

  public Object evalEdn(String form) throws InterruptedException {
    writer.println(form);
    Map<Object, Object> result = results.take();

    if (result.get(EXCEPTION) == null) {
      return result.get(VAL);
    } else {
      writer.println("(-> *e Throwable->map clojure.main/ex-triage clojure.main/ex-str)");
      String triageResultEdn = (String) results.take().get(VAL);
      String triageResult = (String) parser.nextValue(Parsers.newParseable(triageResultEdn));
      throw new ClojureException(triageResult);
    }
  }

  public List<String> pollOutput() {
    List<String> result = new ArrayList<>();
    while (!output.isEmpty()) {
      result.add((String) output.poll().get(VAL));
    }
    return result;
  }

  public List<String> pollTaps() {
    List<String> result = new ArrayList<>();
    while (!taps.isEmpty()) {
      result.add((String) taps.poll().get(VAL));
    }
    return result;
  }

  @SuppressWarnings("unchecked")
  public void inputLoop() {
    try (BufferedReader rdr = reader) {
      while (!closed) {
        String line = reader.readLine();
        Object obj = parser.nextValue(Parsers.newParseable(line));

        if (obj instanceof Map) {
          Map<Object, Object> data = (Map<Object, Object>) obj;
          Object tag = data.get(TAG);

          if (RET.equals(tag)) {
            this.results.put(data);
          } else if (TAP.equals(tag)) {
            this.taps.put(data);
          } else if (OUT.equals(tag)) {
            this.output.put(data);
          } else if (ERR.equals(tag)) {
            this.output.put(data);
          }
        }
      }
    } catch (AsynchronousCloseException e) {
      // ignore
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }

  public static PreplClient socketConnect(InetAddress address, int port, AtomicBoolean serverStopped) {
    SocketChannel socket = null;
    // we're waiting for the server to start, so give it a few trys
    try {
      for (int i = 0; i < 30; i++) {
        if (serverStopped.get()) {
          break;
        }

        if (i != 0) {
          Thread.sleep(1000);
        }
        try {
          InetSocketAddress addr = new InetSocketAddress(address, port);
          socket = SocketChannel.open(addr);
          break;
        } catch (ConnectException e) {
          // retry
        } catch (IOException e) {
          throw new UncheckedIOException(e);
        }
      }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }

    if (socket == null) {
      throw new IllegalStateException(String.format("Unable to connect to prepl at %s:%s", address, port));
    }

    BufferedReader reader = new BufferedReader(Channels.newReader(socket, StandardCharsets.UTF_8.name()));
    PrintWriter writer = new PrintWriter(Channels.newWriter(socket, StandardCharsets.UTF_8.name()), true);

    PreplClient client = new PreplClient(reader, writer, serverStopped);
    client.start();
    return client;
  }
}

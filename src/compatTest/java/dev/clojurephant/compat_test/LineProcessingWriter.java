package dev.clojurephant.compat_test;

import java.io.FilterWriter;
import java.io.IOException;
import java.io.Writer;

import clojure.lang.IFn;

public class LineProcessingWriter extends FilterWriter {
  private final IFn processor;
  private StringBuilder builder;
  private int position;

  public LineProcessingWriter(Writer out, IFn processor) {
    super(out);
    this.processor = processor;
    this.builder = new StringBuilder(1024);
    this.position = 0;
  }

  @Override
  public void write(int c) throws IOException {
    super.write(c);
    builder.append(c);
    process(false);
  }

  @Override
  public void write(char[] cbuf, int off, int len) throws IOException {
    super.write(cbuf, off, len);
    builder.append(cbuf, off, len);
    process(false);
  }

  @Override
  public void write(String str, int off, int len) throws IOException {
    super.write(str, off, len);
    builder.append(str, off, len);
    process(false);
  }

  @Override
  public void close() throws IOException {
    super.close();
    process(true);
  }

  private void process(boolean endOfInput) throws IOException {
    int lineEnding = builder.indexOf("\n", position);
    while (lineEnding >= 0) {
      processor.invoke(builder.substring(0, lineEnding));
      builder.delete(0, lineEnding + System.lineSeparator().length());
      this.position = builder.length();
      lineEnding = builder.indexOf(System.lineSeparator(), position);
      this.flush();
    }

    if (endOfInput && builder.length() > 0) {
      processor.invoke(builder.toString());
      builder.setLength(0);
      this.flush();
    }

    this.position = builder.length();
  }
}

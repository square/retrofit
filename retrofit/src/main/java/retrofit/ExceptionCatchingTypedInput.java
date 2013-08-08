package retrofit;

import java.io.IOException;
import java.io.InputStream;
import retrofit.mime.TypedInput;

class ExceptionCatchingTypedInput implements TypedInput {
  private final TypedInput delegate;
  private final ExceptionCatchingInputStream delegateStream;

  ExceptionCatchingTypedInput(TypedInput delegate) throws IOException {
    this.delegate = delegate;
    this.delegateStream = new ExceptionCatchingInputStream(delegate.in());
  }

  @Override public String mimeType() {
    return delegate.mimeType();
  }

  @Override public long length() {
    return delegate.length();
  }

  @Override public InputStream in() throws IOException {
    return delegateStream;
  }

  IOException getThrownException() {
    return delegateStream.thrownException;
  }

  boolean threwException() {
    return delegateStream.thrownException != null;
  }

  private static class ExceptionCatchingInputStream extends InputStream {
    private final InputStream delegate;
    private IOException thrownException;

    ExceptionCatchingInputStream(InputStream delegate) {
      this.delegate = delegate;
    }

    @Override public int read() throws IOException {
      try {
        return delegate.read();
      } catch (IOException e) {
        thrownException = e;
        throw e;
      }
    }

    @Override public int read(byte[] buffer) throws IOException {
      try {
        return delegate.read(buffer);
      } catch (IOException e) {
        thrownException = e;
        throw e;
      }
    }

    @Override public int read(byte[] buffer, int offset, int length) throws IOException {
      try {
        return delegate.read(buffer, offset, length);
      } catch (IOException e) {
        thrownException = e;
        throw e;
      }
    }

    @Override public long skip(long byteCount) throws IOException {
      try {
        return delegate.skip(byteCount);
      } catch (IOException e) {
        thrownException = e;
        throw e;
      }
    }

    @Override public int available() throws IOException {
      try {
        return delegate.available();
      } catch (IOException e) {
        thrownException = e;
        throw e;
      }
    }

    @Override public void close() throws IOException {
      try {
        delegate.close();
      } catch (IOException e) {
        thrownException = e;
        throw e;
      }
    }

    @Override public synchronized void mark(int readLimit) {
      delegate.mark(readLimit);
    }

    @Override public synchronized void reset() throws IOException {
      try {
        delegate.reset();
      } catch (IOException e) {
        thrownException = e;
        throw e;
      }
    }

    @Override public boolean markSupported() {
      return delegate.markSupported();
    }
  }
}

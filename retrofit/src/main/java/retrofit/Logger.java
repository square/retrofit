package retrofit;

/** Simple logging abstraction for debug messages. */
public interface Logger {
  /** Log a debug message to the appropriate console. */
  void log(String message);

  LogLevel level();

  /** A {@link Logger} implementation which does not log anything. */
  Logger NONE = new Logger() {
    @Override public void log(String message) {
    }

    @Override public LogLevel level() {
      return LogLevel.NONE;
    }
  };
}

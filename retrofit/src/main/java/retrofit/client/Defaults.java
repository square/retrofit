package retrofit.client;

final class Defaults {
  static final int CONNECT_TIMEOUT_MILLIS = 15 * 1000; // 15s
  static final int READ_TIMEOUT_MILLIS = 20 * 1000; // 20s

  private Defaults() {
    // No instances.
  }
}

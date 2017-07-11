package retrofit2.converter.thrifty;

import com.microsoft.thrifty.protocol.BinaryProtocol;
import com.microsoft.thrifty.protocol.CompactProtocol;
import com.microsoft.thrifty.protocol.Protocol;
import com.microsoft.thrifty.transport.Transport;

/**
 * See https://thrift.apache.org/docs/concepts#protocol
 * <p>
 * Thrifty currently only supports binary, compact and
 * simple json (which isn't very useful since it can only be written, not read).
 */
public enum ProtocolType {
  BINARY("binary"),
  COMPACT("compact");

  private final String type;

  ProtocolType(String type) {
    this.type = type;
  }

  public static Protocol createProtocol(ProtocolType type, Transport transport) {
    switch (type) {
      case BINARY:
        return new BinaryProtocol(transport);
      case COMPACT:
        return new CompactProtocol(transport);
      default:
        throw new RuntimeException("Unsupported protocol type: " + type);
    }
  }
}

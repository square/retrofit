package retrofit.rpc;

import java.util.Map;

/**
 * Created by dp on 22/04/14.
 */
public class RpcRequest {
  private String method;
  private Map<String, Object> params;

  public RpcRequest(String method, Map<String, Object> params) {
    this.method = method;
    this.params = params;
  }
}

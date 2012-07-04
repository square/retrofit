package retrofit.http;

/**
 * Get thrown when an http response code is not OK (in the range of 200-299)
 *
 * @author Udi Cohen (udinic@gmail.com)
 */
public class ResponseNotOKException extends Exception {

    int statusCode;

    public ResponseNotOKException(int statusCode, String message) {
        super(message);
        this.statusCode = statusCode;
    }
    
    public ResponseNotOKException(int statusCode) {
        super();
        this.statusCode = statusCode;
    }
    
    

    public int getStatusCode() {
        return statusCode;
    }

    @Override
    public String toString() {
        return "Http status code ["+statusCode+"]. " + super.toString();
    }
}

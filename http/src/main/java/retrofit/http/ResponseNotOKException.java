package retrofit.http;

/**
 * Created by IntelliJ IDEA.
 * User: udic
 * Date: 02/02/12
 * Time: 21:07
 * To change this template use File | Settings | File Templates.
 */
public class ResponseNotOKException extends Exception {

    int statusCode;

    public ResponseNotOKException(int statusCode, String message) {
        super(message);
        this.statusCode = statusCode;
    }
    
    public ResponseNotOKException(int statusCode) {
        super();    //To change body of overridden methods use File | Settings | File Templates.
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

package retrofit.converter.server;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "error")
public class Error {

	private String message;

	private String exception;

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public String getException() {
		return exception;
	}

	public void setException(String exception) {
		this.exception = exception;
	}

	public static Response formatError(Exception e) {
		final Error error = new Error();
		if (e.getClass() != null) {
			error.setException(e.getClass().getName());
		}
		error.setMessage(e.getMessage());
		final Response response = Response.status(Status.INTERNAL_SERVER_ERROR)
				.entity(error).build();
		return response;
	}
}

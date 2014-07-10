package retrofit.converter.client.model;

import java.util.List;

import javax.mail.internet.MimeMultipart;

import retrofit.http.GET;
import retrofit.http.POST;
import retrofit.http.Query;

public interface Documents {

	@POST("/documents")
	public MimeMultipart getDocumentsAndContent(@Query("query") String query,
			@Query("maxhits") int maxhits);

	@GET("/documents")
	public List<Document> getDocuments(@Query("query") String query,
			@Query("maxhits") int maxhits);
}

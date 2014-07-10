package retrofit.converter;

import java.io.UnsupportedEncodingException;
import java.util.List;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMultipart;

import org.junit.Before;
import org.junit.Test;

import retrofit.RestAdapter;
import retrofit.converter.client.model.Document;
import retrofit.converter.client.model.Documents;

public class RetrofitTest extends AbstractWebServiceTest {

	private String DOCID = "<1234>";

	private Documents documents;

	@Before
	public void createAdapter() {
		RestAdapter restAdapter = new RestAdapter.Builder()
				.setEndpoint(ENDPOINT_ADDRESS)
				.setLogLevel(RestAdapter.LogLevel.FULL)
				.setConverter(new MultipartConverter()).build();

		documents = restAdapter.create(Documents.class);
	}

	@Test
	public void testMultipart() throws MessagingException,
			UnsupportedEncodingException {
		MimeMultipart multipart = documents.getDocumentsAndContent("my query",
				2);
		assertEquals(3, multipart.getCount());
		assertEquals("application/json", multipart.getBodyPart(0)
				.getContentType());
		assertEquals("application/pdf", multipart.getBodyPart(1)
				.getContentType());
		String[] values = multipart.getBodyPart(1).getHeader("Content-ID");
		assertEquals(DOCID, values[0]);
		assertEquals("application/pdf", multipart.getBodyPart(2)
				.getContentType());
	}

	@Test
	public void testRegular() {
		List<Document> metadatas = documents.getDocuments("my query", 2);
		assertEquals(2, metadatas.size());
	}
}

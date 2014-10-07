package retrofit.converter.server;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;

import org.apache.cxf.jaxrs.ext.multipart.Attachment;
import org.apache.cxf.jaxrs.ext.multipart.MultipartBody;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path("documents")
public class Documents {
	private final Logger logger = LoggerFactory.getLogger(getClass());

	public static final String DOCID = "1234";

	@POST
	@Consumes(MediaType.MEDIA_TYPE_WILDCARD)
	@Produces("multipart/mixed")
	public MultipartBody getDocumentsAndContent(
			@QueryParam("query") String query,
			@QueryParam("maxhits") int maxhits) {
		try {
			logger.info("query: {}", query);
			logger.info("maxhits: {}", maxhits);

			List<Attachment> atts = new LinkedList<Attachment>();
			atts.add(new Attachment("metadata", MediaType.APPLICATION_JSON,
					fakeResults(maxhits)));
			final List<Document> docs = fakeResults(maxhits);
			for (final Document doc : docs) {
				final InputStream samplePdf = getClass().getResourceAsStream(
						"/pdf-sample.pdf");
				if (samplePdf == null) {
					throw new IOException(
							"unable to find /pdf-sample.pdf in classpath");
				}
				atts.add(new Attachment(doc.getDocId(), doc.getMimeType(),
						samplePdf));
			}
			return new MultipartBody(atts, true);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			throw new WebApplicationException(e, Error.formatError(e));
		}
	}

	@GET
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public List<Document> getDocuments(@QueryParam("query") String query,
			@QueryParam("maxhits") int maxhits) {
		try {
			logger.info("query: {}", query);
			logger.info("maxhits: {}", maxhits);
			return fakeResults(maxhits);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			throw new WebApplicationException(e, Error.formatError(e));
		}
	}

	private List<Document> fakeResults(int docsCount) {
		List<Document> documents = new ArrayList<Document>();
		Document document = new Document();
		document.setDocId(DOCID);
		document.setMimeType("application/pdf");
		document.setFolderName("Customer Information");
		for (int i = 0; i < docsCount; i++) {
			documents.add(document);
		}
		return documents;
	}
}

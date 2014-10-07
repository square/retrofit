package retrofit.converter;

import org.apache.cxf.endpoint.Server;
import org.apache.cxf.jaxrs.JAXRSServerFactoryBean;
import org.apache.cxf.jaxrs.client.WebClient;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public abstract class AbstractWebServiceTest extends Assert {

	public final static String ENDPOINT_ADDRESS = "http://localhost:9080/ws";
	private static Server server;
	protected WebClient client;
	private static ClassPathXmlApplicationContext ctx;

	@BeforeClass
	public static void initialize() throws Exception {
		ctx = new ClassPathXmlApplicationContext(new String[] { "beans.xml" });
		startServer(ctx);
	}

	@AfterClass
	public static void destroy() throws Exception {
		if (server != null) {
			server.stop();
			server.destroy();
		}
		ctx.close();
	}

	private static void startServer(ClassPathXmlApplicationContext ctx)
			throws Exception {
		JAXRSServerFactoryBean sf = (JAXRSServerFactoryBean) ctx
				.getBean("services");
		sf.setAddress(ENDPOINT_ADDRESS);
		server = sf.create();
	}
}
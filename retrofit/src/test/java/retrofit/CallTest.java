package retrofit;

import java.util.List;

import org.junit.Before;
import org.junit.Test;

import retrofit.client.Response;
import retrofit.http.GET;
import retrofit.http.Path;

/**
 * Exemple of the Call object behavior. Not a working test.
 */
public class CallTest {

	interface Api {
		@GET("/{user}/tweets")
		Call<List<String>> listTweets(@Path("user") String username);
	}

	private RestAdapter restAdapter;

	@Before public void setUp() {
		restAdapter = new RestAdapter.Builder()
				.setServer("https://twitter.com").build();
	}

	@Test public void test() {
		Api api = restAdapter.create(Api.class);

		Call<List<String>> c = api.listTweets("JakeWharton");

		try {
			// Synchronously
			List<String> tweets = c.execute();

			// Asynchronously;
			c.execute(new Callback<List<String>>() {

				@Override public void success(List<String> t, Response response) {
					// TODO Auto-generated method stub

				}

				@Override public void failure(RetrofitError error) {
					// TODO Auto-generated method stub

				}
			});

		} catch (Throwable e) {
			e.printStackTrace();
		}
	}

}

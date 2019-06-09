package za.co.riggaroo.retrofittestexample;

import android.test.InstrumentationTestCase;
import android.test.suitebuilder.annotation.SmallTest;


import junit.framework.Assert;

import java.lang.annotation.Annotation;
import okhttp3.OkHttpClient;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Converter;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;
import retrofit2.mock.BehaviorDelegate;
import retrofit2.mock.MockRetrofit;
import retrofit2.mock.NetworkBehavior;
import za.co.riggaroo.retrofittestexample.pojo.QuoteOfTheDayErrorResponse;
import za.co.riggaroo.retrofittestexample.pojo.QuoteOfTheDayResponse;

/**
 * @author rebeccafranks
 * @since 15/10/23.
 */
public class QuoteOfTheDayMockAdapterTest extends InstrumentationTestCase {
    private MockRetrofit mockRetrofit;
    private Retrofit retrofit;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        retrofit = new Retrofit.Builder().baseUrl("http://test.com")
                .client(new OkHttpClient())
                .addConverterFactory(JacksonConverterFactory.create())
                .build();

        NetworkBehavior behavior = NetworkBehavior.create();

        mockRetrofit = new MockRetrofit.Builder(retrofit)
                .networkBehavior(behavior)
                .build();
    }


    @SmallTest
    public void testRandomQuoteRetrieval() throws Exception {
        BehaviorDelegate<QuoteOfTheDayRestService> delegate = mockRetrofit.create(QuoteOfTheDayRestService.class);
        QuoteOfTheDayRestService mockQodService = new MockQuoteOfTheDayService(delegate);


        //Actual Test
        Call<QuoteOfTheDayResponse> quote = mockQodService.getQuoteOfTheDay();
        Response<QuoteOfTheDayResponse> quoteOfTheDayResponse = quote.execute();

        //Asserting response
        Assert.assertTrue(quoteOfTheDayResponse.isSuccessful());
        Assert.assertEquals("Always code as if the guy who ends up maintaining your code will be a violent psychopath who knows where you live.", quoteOfTheDayResponse.body().getContents().getQuotes().get(0).getQuote());

    }

    @SmallTest
    public void testFailedQuoteRetrieval() throws Exception {
        BehaviorDelegate<QuoteOfTheDayRestService> delegate = mockRetrofit.create(QuoteOfTheDayRestService.class);
        MockFailedQODService mockQodService = new MockFailedQODService(delegate);

        //Actual Test
        Call<QuoteOfTheDayResponse> quote = mockQodService.getQuoteOfTheDay();
        Response<QuoteOfTheDayResponse> quoteOfTheDayResponse = quote.execute();
        Assert.assertFalse(quoteOfTheDayResponse.isSuccessful());

        Converter<ResponseBody, QuoteOfTheDayErrorResponse> errorConverter = retrofit.responseBodyConverter(QuoteOfTheDayErrorResponse.class, new Annotation[0]);
        QuoteOfTheDayErrorResponse error = errorConverter.convert(quoteOfTheDayResponse.errorBody());

        //Asserting response
        Assert.assertEquals(404, quoteOfTheDayResponse.code());
        Assert.assertEquals("Quote Not Found", error.getError().getMessage());

    }
}
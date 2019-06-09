package za.co.riggaroo.retrofittestexample;

import android.util.Log;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;

import java.io.IOException;

import okhttp3.MediaType;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.mock.Calls;
import za.co.riggaroo.retrofittestexample.pojo.Error;
import za.co.riggaroo.retrofittestexample.pojo.QuoteOfTheDayErrorResponse;
import za.co.riggaroo.retrofittestexample.pojo.QuoteOfTheDayResponse;

import retrofit2.mock.BehaviorDelegate;
/**
 * @author rebeccafranks
 * @since 15/10/25.
 */
public class MockFailedQODService implements QuoteOfTheDayRestService {
    private static final String TAG = "MockFailedQOD";
    private final BehaviorDelegate<QuoteOfTheDayRestService> delegate;

    public MockFailedQODService(BehaviorDelegate<QuoteOfTheDayRestService> restServiceBehaviorDelegate) {
        this.delegate = restServiceBehaviorDelegate;

    }

    @Override
    public Call<QuoteOfTheDayResponse> getQuoteOfTheDay() {
        za.co.riggaroo.retrofittestexample.pojo.Error error = new Error();
        error.setCode(404);
        error.setMessage("Quote Not Found");
        QuoteOfTheDayErrorResponse quoteOfTheDayErrorResponse = new QuoteOfTheDayErrorResponse();
        quoteOfTheDayErrorResponse.setError(error);

        ObjectWriter ow = new ObjectMapper().writer().withDefaultPrettyPrinter();
        String json = "";
        try {
            json = ow.writeValueAsString(quoteOfTheDayErrorResponse);
            Response response = Response.error(404, ResponseBody.create(MediaType.parse("application/json") ,json));
            return delegate.returning(Calls.response(response)).getQuoteOfTheDay();
           // return delegate.returningResponse(response).getQuoteOfTheDay();
        } catch (JsonProcessingException e) {
            Log.e(TAG, "JSON Processing exception:",e);
            return Calls.failure(e);
        }

    }
}

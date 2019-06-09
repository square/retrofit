package retrofittest-kp.retrofittestexample;

import retrofit2.Call;
import retrofit2.Response;
import retrofit2.http.GET;
import za.co.riggaroo.retrofittestexample.pojo.QuoteOfTheDayResponse;

/**
 * @author rebeccafranks
 * @since 15/10/23.
 */
public interface QuoteOfTheDayRestService {

    @GET("/qod.json")
    Call<QuoteOfTheDayResponse> getQuoteOfTheDay();

}

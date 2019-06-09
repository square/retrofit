package za.co.riggaroo.retrofittestexample;

import java.util.ArrayList;

import retrofit2.Call;
import retrofit2.Response;
import retrofit2.mock.BehaviorDelegate;
import za.co.riggaroo.retrofittestexample.pojo.Contents;
import za.co.riggaroo.retrofittestexample.pojo.Quote;
import za.co.riggaroo.retrofittestexample.pojo.QuoteOfTheDayResponse;

/**
 * @author rebeccafranks
 * @since 15/10/24.
 */
public class MockQuoteOfTheDayService implements QuoteOfTheDayRestService {

    private final BehaviorDelegate<QuoteOfTheDayRestService> delegate;

    public MockQuoteOfTheDayService(BehaviorDelegate<QuoteOfTheDayRestService> service) {
        this.delegate = service;
    }

    @Override
    public Call<QuoteOfTheDayResponse> getQuoteOfTheDay() {
        QuoteOfTheDayResponse quoteOfTheDayResponse = new QuoteOfTheDayResponse();
        Contents contents = new Contents();
        Quote quote = new Quote();
        quote.setQuote("Always code as if the guy who ends up maintaining your code will be a violent psychopath who knows where you live.");
        ArrayList<Quote> quotes = new ArrayList<>();
        quotes.add(quote);
        contents.setQuotes(quotes);
        quoteOfTheDayResponse.setContents(contents);
        return delegate.returningResponse(quoteOfTheDayResponse).getQuoteOfTheDay();
    }
}

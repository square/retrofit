package retrofit2.converter.fruit;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.http.GET;

import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Created by ghui on 16/10/2017.
 */
public class FruitConverterFactoryTest {
    interface Service{
        @GET("/")
        Call<FruitInfo> get();
    }

    @Rule
    public MockWebServer server = new MockWebServer();

    private Service service;

    @Before public void setup() {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(server.url("/"))
                .addConverterFactory(FruitConverterFactory.create())
                .build();
        service = retrofit.create(Service.class);
    }

    @Test public void testConverter() throws IOException {
        server.enqueue(new MockResponse()
        .setBody("<html>\n" +
                "\n" +
                "<head><title>Ghui's favorite Fruits</title></head>\n" +
                "\n" +
                "<body>\n" +
                "    <div id='favorite'>Apple is my favorite fruit.</div>\n" +
                "    <img src='http://dwz.cn/5USjpv' class=\"apple\" />\n" +
                "    <br/>\n" +
                "    <a href='https://ghui.me' class=\"author\"> ghui</a>\n" +
                "    <div id='fruits'>\n" +
                "        <div class=\"fruit\" id=\"1\">\n" +
                "            <strong class=\"name\">apple</strong>\n" +
                "            <strong class=\"color\">red</strong>\n" +
                "        </div>\n" +
                "        <div class=\"fruit\" id=\"2\">\n" +
                "            <strong class=\"name\">orange</strong>\n" +
                "            <strong class=\"color\">green</strong>\n" +
                "        </div>\n" +
                "        <div class=\"fruit\" id=\"3\">\n" +
                "            <strong class=\"name\">banana</strong>\n" +
                "            <strong class=\"color\">yellow</strong>\n" +
                "        </div>\n" +
                "    </div>\n" +
                "</body>\n" +
                "\n" +
                "</html>"));
        Call<FruitInfo>  call = service.get();
        Response<FruitInfo> responseInfo = call.execute();
        FruitInfo fruitInfo = responseInfo.body();

        assertThat(fruitInfo.getTitle()).isEqualTo("Ghui's favorite Fruits");
        assertThat(fruitInfo.getAuthor()).isEqualTo("ghui");
        assertThat(fruitInfo.getAuthorBlog()).contains("ghui.me");
        assertThat(fruitInfo.getFavoriteImg()).isEqualTo("http://dwz.cn/5USjpv");
        List<FruitInfo.Item> itemList = fruitInfo.getOtherFruits();
        assertThat(itemList.size()).isEqualTo(3);
        assertThat(itemList.get(1).getColor()).isEqualTo("green");
        assertThat(itemList.get(1).getName()).isEqualTo("orange");
        assertThat(itemList.get(2).getId()).isEqualTo(3);

    }

}

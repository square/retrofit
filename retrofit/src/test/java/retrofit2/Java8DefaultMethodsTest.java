package retrofit2;

// TODO this test doesn't play nice in the IDE because it relies on Java 8 language features.
public final class Java8DefaultMethodsTest {
  //@Rule public final MockWebServer server = new MockWebServer();
  //
  //interface Example {
  //  @GET("/") Call<String> user(@Query("name") String name);
  //
  //  default Call<String> user() {
  //    return user("hey");
  //  }
  //}
  //
  //@Test public void test() throws IOException {
  //  server.enqueue(new MockResponse().setBody("Hi"));
  //  server.enqueue(new MockResponse().setBody("Hi"));
  //
  //  Retrofit retrofit = new Retrofit.Builder()
  //      .baseUrl(server.url("/"))
  //      .addConverterFactory(new ToStringConverterFactory())
  //      .build();
  //  Example example = retrofit.create(Example.class);
  //
  //  Response<String> response = example.user().execute();
  //  assertThat(response.body()).isEqualTo("Hi");
  //  Response<String> response = example.user("hi").execute();
  //  assertThat(response.body()).isEqualTo("Hi");
  //}
}

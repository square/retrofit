package retrofit.test_app;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;
import retrofit.io.Files;

import java.io.File;
import java.io.IOException;

public class Main extends Activity {

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    TextView result = new TextView(this);

    try {
      File dir = getFilesDir();
      Files.sync(dir);
    } catch (IOException e) {
      throw new AssertionError(e);
    }

    try {
      File file = new File(getFilesDir(), "not-a-dir");
      file.createNewFile();
      Files.sync(file);
      throw new AssertionError("Expected an IOException.");
    } catch (IOException e) { /* expected */ }

    result.setText("OK");
    setContentView(result);
  }
}

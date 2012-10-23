package retrofit.test_app;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;
import retrofit.io.Dirs;

import java.io.File;
import java.io.IOException;

public class Main extends Activity {

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    TextView result = new TextView(this);

    try {
      File dir = getFilesDir();
      Dirs.sync(dir);
    } catch (IOException e) {
      throw new AssertionError(e);
    }

    try {
      File file = new File(getFilesDir(), "not-a-dir");
      file.createNewFile();
      Dirs.sync(file);
      throw new AssertionError("Expected an IOException.");
    } catch (IOException e) { /* expected */ }

    result.setText("OK");
    setContentView(result);
  }
}

package retrofit;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.Executor;

public class TestExecutor implements Executor {
  final Deque<Runnable> runnables = new ArrayDeque<Runnable>();

  @Override public void execute(Runnable runnable) {
    runnables.addLast(runnable);
  }

  public void runNextRunnable() {
    runnables.removeFirst().run();
  }
}

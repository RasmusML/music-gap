package dk.rmls.musicgap;

public class ThreadUtil {

  static public void sleep(int ms) {
    try {
      Thread.sleep(ms);
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
  }
}

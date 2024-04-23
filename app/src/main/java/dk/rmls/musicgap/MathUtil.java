package dk.rmls.musicgap;

public class MathUtil {

  static public int getRandomInt(int fromInclusive, int toInclusive) {
    double rand = (toInclusive - fromInclusive + 1) * Math.random() + fromInclusive;
    return (int) rand;
  }

  static public int getRandomInt(int toInclusive) {
    return getRandomInt(0, toInclusive);
  }
}

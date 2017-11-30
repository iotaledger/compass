package coo.util;

public class Math {
    public static int log2(int what) {
      long input = Integer.toUnsignedLong(what);
      int count = 0;

      while ((input >>= 1) > 0) {
        count++;
      }

      return count;
    }
}

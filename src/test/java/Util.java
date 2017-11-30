import java.util.Random;

/**
 * @author Andreas C. Osowski
 */
public class Util {

    public static final String ALPHABET = "9ABCDEFGHIJKLMNOPQRSTUVWXYZ";

    public static String nextSeed() {
        return nextTrytes(81);
    }

    public static String nextTrytes(int count) {
        Random random = new Random();
        char[] buf = new char[count];

        for (int idx = 0; idx < buf.length; ++idx)
            buf[idx] = ALPHABET.charAt(random.nextInt(ALPHABET.length()));

        return new String(buf);
    }
}

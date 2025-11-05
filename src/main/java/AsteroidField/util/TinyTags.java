package AsteroidField.util;

public final class TinyTags {
    private static int seq; // ensures uniqueness across rapid calls

    private TinyTags() {}

    /** Example output: "8F-2C-91". Change BYTES=6 in toHex to get "AA-BB-CC-DD-EE-FF". */
    public static String sessionTag() {
        long t = System.currentTimeMillis();
        long n = System.nanoTime();
        
        long mix = t ^ (t >>> 32) ^ n ^ (n >>> 32) ^ (seq++);

        // small, fast avalanche to spread bits (no extra deps)
        int x = avalanche32((int)(mix ^ (mix >>> 32)));

        return toHex3(x);
    }

    private static int avalanche32(int x) {
        x ^= x >>> 16; x *= 0x7feb352d;
        x ^= x >>> 15; x *= 0x846ca68b;
        x ^= x >>> 16;
        return x;
    }

    private static String toHex3(int v) {
        char[] out = new char[8]; // "AA-BB-CC"
        hexByte(out, 0, (v >>> 16) & 0xFF); out[2] = '-';
        hexByte(out, 3, (v >>> 8)  & 0xFF); out[5] = '-';
        hexByte(out, 6,  v         & 0xFF);
        long pid = ProcessHandle.current().pid();
        
        return String.valueOf(pid).concat("# ").concat(new String(out));
    }

    private static final char[] HEX = "0123456789ABCDEF".toCharArray();
    private static void hexByte(char[] dst, int pos, int b) {
        dst[pos]   = HEX[(b >>> 4) & 0xF];
        dst[pos+1] = HEX[b & 0xF];
    }
}

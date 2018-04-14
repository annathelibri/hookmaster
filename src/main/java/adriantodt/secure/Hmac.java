package adriantodt.secure;

import kotlin.text.Charsets;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

public class Hmac {
    private static final String hexLookup = "0123456789ABCDEF";

    public static String digest(String msg, String keyString) {
        return digest(msg, keyString, "HmacMD5");
    }

    public static String digest(String msg, String keyString, String alg) {
        SecretKeySpec key = new SecretKeySpec(keyString.getBytes(Charsets.UTF_8), alg);
        try {
            Mac mac = Mac.getInstance(alg);
            mac.init(key);
            return hex(mac.doFinal(msg.getBytes(Charsets.UTF_8)));
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new IllegalArgumentException(e);
        }
    }

    public static void main(String[] a) {
        System.out.println(Hmac.digest("{This is a test message}", "1vSN2qi8D47VrTw11bG5kKLtQLGyoZtToPgy2i-fRAo6ZnU0g4CYAtFdfC3GN8xr"));
    }

    private static String hex(byte[] raw) {
        if (raw == null) return null;

        StringBuilder hex = new StringBuilder(raw.length * 2);

        for (byte b : raw)
            hex
                .append(hexLookup.charAt((b & 0xF0) >> 4))
                .append(hexLookup.charAt((b & 0x0F)));

        return hex.toString();
    }
}

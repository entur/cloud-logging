package no.entur.logging.cloud.rr.grpc.filter;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * Utilities methods for logging.
 *
 * @author August Detlefsen [augustd@codemagi.com]
 */
public class FilterUtils {

	/**
	 * Converts an input String to a SHA hash. The actual hash strength is
	 * hidden by the method name to allow for future-proofing this API, but the
	 * current default is SHA-256.
	 *
	 * @param input
	 *			The string to hash
	 * @return SHA hash of the input String, hex encoded.
	 */
	public static String toSHA(String input) {
		return toSHA(input.getBytes(StandardCharsets.UTF_8));
	}

	/**
	 * Converts an input byte array to a SHA hash. The actual hash strength is
	 * hidden by the method name to allow for future-proofing this API, but the
	 * current default is SHA-256.
	 *
	 * @param input
	 *			Byte array to hash
	 * @return SHA hash of the input String, hex encoded.
	 */
	public static String toSHA(byte[] input) {
		try {
			MessageDigest md = MessageDigest.getInstance("SHA-256");
			byte[] digest = md.digest(input);
			return HexFormat.of().formatHex(digest);
		} catch (NoSuchAlgorithmException nsae) {
			// this code should never be reached!
		}
		return null;
	}

	/**
	 * Determines if a string is null or empty
	 *
	 * @param value
	 *			string to test
	 * @return <code>true</code> if the string is empty or null;
	 *		 <code>false</code> otherwise
	 */
	public static boolean isEmpty(String value) {
		return (value == null || value.trim().length() == 0);
	}

}

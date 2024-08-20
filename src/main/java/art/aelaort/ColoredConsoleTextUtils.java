package art.aelaort;

public class ColoredConsoleTextUtils {
	public static final String ANSI_RESET = "\u001B[0m";
	public static final String ANSI_RED = "\u001B[31m";
	public static final String ANSI_GREEN = "\u001B[32m";
	public static final String ANSI_RESET_REGEXP = "\\u001B\\[0m";
	public static final String ANSI_RED_REGEXP = "\\u001B\\[31m";
	public static final String ANSI_GREEN_REGEXP = "\\u001B\\[32m";

	public static String wrapGreen(String text) {
		return ANSI_GREEN + text + ANSI_RESET;
	}

	public static String wrapRed(String text) {
		return ANSI_RED + text + ANSI_RESET;
	}
}

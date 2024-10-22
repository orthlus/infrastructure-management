package art.aelaort.utils;

import dnl.utils.text.table.TextTable;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

public class TablePrintingUtils {
	public static String getTableString(TextTable textTable) {
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		PrintStream printStream = new PrintStream(outputStream);
		textTable.printTable(printStream, 0);
		return outputStream.toString();
	}

	public static String nullable(Object obj) {
		return obj == null ? " - " : String.valueOf(obj);
	}

	public static void appendSpaceToRight(Object[][] result) {
		for (Object[] row : result) {
			for (int j = 0; j < row.length; j++) {
				row[j] = row[j] + " ";
			}
		}
	}
}
package art.aelaort;

import com.github.difflib.text.DiffRow;
import com.github.difflib.text.DiffRowGenerator;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@Component
public class FileDiffService {
	private static final String ANSI_RESET = "\u001B[0m";
	private static final String ANSI_RED = "\u001B[31m";
	private static final String ANSI_GREEN = "\u001B[32m";
	private static final String ANSI_RESET_REGEXP = "\\u001B\\[0m";
	private static final String ANSI_RED_REGEXP = "\\u001B\\[31m";
	private static final String ANSI_GREEN_REGEXP = "\\u001B\\[32m";

	public String getColoredFilesDiff(Path oldFile, Path newFile) {
		try {
			List<DiffRow> rows = getGenerator().generateDiffRows(
					Files.readAllLines(oldFile),
					Files.readAllLines(newFile)
			);

			return getColoredTable(rows);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private DiffRowGenerator getGenerator() {
		return DiffRowGenerator.create()
				.showInlineDiffs(true)
				.inlineDiffByWord(true)
				.newTag(f -> f ? ANSI_GREEN : ANSI_RESET)
				.oldTag(f -> f ? ANSI_RED : ANSI_RESET)
				.build();
	}

	private String getColoredTable(List<DiffRow> rows) {
		int maxOldL = 0;
		int maxNewL = 0;
		for (DiffRow row : rows) {
			maxOldL = Math.max(maxOldL, cleanedRow(row.getOldLine()).length());
			maxNewL = Math.max(maxNewL, cleanedRow(row.getNewLine()).length());
		}

		String repeatOld = " ".repeat(maxOldL / 2 - 2);
		String repeatNew = " ".repeat(maxNewL / 2 - 2);
		String dashes = "-".repeat(maxOldL + maxNewL + 3);
		String formatted = "|%s old%s|%s new%s|\n%s\n".formatted(repeatOld, repeatOld, repeatNew, repeatNew, dashes);
		StringBuilder sb = new StringBuilder(formatted);
		for (DiffRow row : rows) {
			sb.append("|")
					.append(row.getOldLine())
					.append(" ".repeat(maxOldL - cleanedRow(row.getOldLine()).length()))
					.append("|")
					.append(row.getNewLine())
					.append(" ".repeat(maxNewL - cleanedRow(row.getNewLine()).length()))
					.append("|\n");
		}
		sb.deleteCharAt(sb.length() - 1);

		return sb.toString();
	}

	private String cleanedRow(String row) {
		return row
				.replaceAll(ANSI_RESET_REGEXP, "")
				.replaceAll(ANSI_RED_REGEXP, "")
				.replaceAll(ANSI_GREEN_REGEXP, "");
	}
}

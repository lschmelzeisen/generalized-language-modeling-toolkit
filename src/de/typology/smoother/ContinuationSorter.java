package de.typology.smoother;

import java.io.File;

import de.typology.splitter.Sorter;
import de.typology.utils.SystemHelper;

public class ContinuationSorter extends Sorter {

	/**
	 * This class provides a method for sorting a given file by the second,
	 * third, fourth...(, first) word in order to calculate the novel
	 * continuation probability used in Kneser-Ney interpolation.
	 * 
	 * @param args
	 * 
	 * @author Martin Koerner
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

	public void sortSecondCloumnDirectory(String inputPath,
			String inputExtension, String outputExtension) {
		File[] files = new File(inputPath).listFiles();
		for (File file : files) {
			this.sortSecondCloumnFile(file.getAbsolutePath(), inputExtension,
					outputExtension);
		}
	}

	public void sortSecondCloumnFile(String inputPath, String inputExtension,
			String outputExtension) {
		if (inputPath.endsWith(inputExtension)) {
			// set output file name
			File inputFile = new File(inputPath);
			String outputPath = inputPath.replace(inputExtension,
					outputExtension);

			// build sort command
			int columnNumber = this.getColumnNumber(inputPath);
			String sortCommand = "sort --buffer-size=1G ";

			// 0edges are only sorted by count
			if (!inputPath.contains(".0")) {
				// don't sort for last column (columnnumber - 1) just yet
				for (int column = 2; column < columnNumber; column++) {
					sortCommand += "--key=" + column + "," + column + " ";
				}
			}
			// sort for count (nr --> numerics, reverse)
			sortCommand += "--key=" + columnNumber + "," + columnNumber + "nr ";
			// sort for first line
			sortCommand += "--key=1,1 ";
			sortCommand += "--output=" + outputPath + " " + inputPath;

			// execute command
			SystemHelper.runUnixCommand(sortCommand);

			inputFile.delete();
		}
		// note: when sorting an empty file, new file contains "null1"
	}

}
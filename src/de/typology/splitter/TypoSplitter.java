package de.typology.splitter;

import java.io.BufferedWriter;
import java.io.IOException;

import de.typology.utils.Config;
import de.typology.utils.IOHelper;

public class TypoSplitter extends Splitter {
	private String extension;

	public TypoSplitter(String directory, String indexName, String statsName,
			String inputName) {
		super(directory, indexName, statsName, inputName, "typos");
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String outputDirectory = Config.get().outputDirectory
				+ Config.get().inputDataSet;
		TypoSplitter ts = new TypoSplitter(outputDirectory, "index.txt",
				"stats.txt", "normalized.txt");
		ts.split(5);
	}

	@Override
	public void split(int maxSequenceLength) {
		int edgeType;
		BufferedWriter writer;
		for (int sequenceLength = 1; sequenceLength <= maxSequenceLength; sequenceLength++) {
			edgeType = sequenceLength - 1;
			this.extension = edgeType + "es";
			IOHelper.strongLog("splitting into " + this.extension);
			this.initialize(this.extension, sequenceLength);
			while (this.getNextSequence(sequenceLength)) {
				writer = this.getWriter(this.sequence[0]);
				try {
					writer.write(this.sequence[0] + "\t");
					writer.write(this.sequence[this.sequence.length - 1] + "\t");
					writer.write(this.sequenceCount + "\n");
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			this.reset();
			this.sortAndAggregate(this.outputDirectory.getAbsolutePath() + "/"
					+ this.extension);
		}
	}
}
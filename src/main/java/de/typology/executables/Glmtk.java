package de.typology.executables;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.cli.Option;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.typology.smoothing.ContinuationMaximumLikelihoodEstimator;
import de.typology.smoothing.Corpus;
import de.typology.smoothing.DeleteCalculator;
import de.typology.smoothing.Estimator;
import de.typology.smoothing.FalseMaximumLikelihoodEstimator;
import de.typology.smoothing.MaximumLikelihoodEstimator;
import de.typology.smoothing.PropabilityCalculator;
import de.typology.smoothing.SkipCalculator;
import de.typology.utils.StringUtils;

public class Glmtk extends Executable {

    private static final int LOG_BASE = 10;

    private static final List<String> SMOOTHERS = Arrays.asList("mle", "dmle",
            "fmle", "cmle", "dcmle");

    private static final String OPTION_SMOOTHER = "smoother";

    private static final String OPTION_TESTING = "testing";

    private static final String OPTION_CROSSPRODUCT = "cross-product";

    private static Logger logger = LoggerFactory.getLogger(Glmtk.class);

    private static List<Option> options;
    static {
        //@formatter:off
        Option help         = new Option("h", OPTION_HELP,         false, "Print this message.");
        Option version      = new Option("v", OPTION_VERSION,      false, "Print the version information and exit.");
        Option smoother     = new Option("s", OPTION_SMOOTHER,     true,  StringUtils.join(SMOOTHERS, ", ") + ".");
               smoother.setArgName("SMOOTHER");
        Option testing      = new Option("t", OPTION_TESTING,      true,  "Testing sequences files. (exclusive with -c).");
               testing.setArgName("TESTING");
        Option crossproduct = new Option("c", OPTION_CROSSPRODUCT, true,  "Use cross product of words up to N. (exclusive with -t).");
               crossproduct.setArgName("N");
        //@formatter:on
        options = Arrays.asList(help, version, smoother, testing, crossproduct);
    }

    private Path corpusDir = null;

    private String smoother = "mle";

    private Path testing = null;

    private Integer crossProductSize = null;

    private Corpus corpus = null;

    private PropabilityCalculator calculator = null;

    public static void main(String[] args) {
        new Glmtk().run(args);
    }

    @Override
    protected List<Option> getOptions() {
        return options;
    }

    @Override
    protected String getUsage() {
        return "glmtk [OPTION]... [CORPUS]";
    }

    @Override
    protected void parseArguments(String[] args) {
        super.parseArguments(args);

        if (line.getArgs() == null || line.getArgs().length == 0) {
            corpusDir = Paths.get(".");
        } else {
            corpusDir = Paths.get(line.getArgs()[0]);
        }
        if (!Files.exists(corpusDir)) {
            System.err.println("Corpus \"" + corpusDir + "\" does not exist.");
            throw new Termination();
        }
        if (!Files.isDirectory(corpusDir)) {
            System.err.println("Corpus \"" + corpusDir
                    + "\" is not a directory.");
            throw new Termination();
        }
        if (!Files.isReadable(corpusDir)) {
            System.err.println("Corpus \"" + corpusDir + "\" is not readable.");
            throw new Termination();
        }

        if (line.hasOption(OPTION_SMOOTHER)) {
            smoother = line.getOptionValue(OPTION_SMOOTHER);
        }
        if (!SMOOTHERS.contains(smoother)) {
            System.err.println("Invalid Smoother \"" + smoother
                    + "\". Valid smoothers are: "
                    + StringUtils.join(SMOOTHERS, ", ") + ".");
            throw new Termination();
        }

        if (line.hasOption(OPTION_TESTING)) {
            testing = Paths.get(line.getOptionValue(OPTION_TESTING));
            if (!Files.exists(testing)) {
                System.err.println("Testing file \"" + testing
                        + "\" does not exist.");
                throw new Termination();
            }
            if (Files.isDirectory(testing)) {
                System.err.println("Testing file \"" + testing
                        + "\" is a directory.");
                throw new Termination();
            }
            if (!Files.isReadable(testing)) {
                System.err.println("Testing file \"" + testing
                        + "\" is not readable.");
                throw new Termination();
            }
        } else if (line.hasOption(OPTION_CROSSPRODUCT)) {
            crossProductSize =
                    Integer.parseInt(line.getOptionValue(OPTION_CROSSPRODUCT));
        } else {
            System.err
                    .println("No test sequences specified. Use either \"-t\" or \"-c\".");
            throw new Termination();
        }
    }

    @Override
    protected void exec() throws IOException {
        Path absoluteDir = corpusDir.resolve("absolute");
        Path continuationDir = corpusDir.resolve("continuation");

        corpus = new Corpus(absoluteDir, continuationDir, "\t");

        Estimator estimator = null;
        switch (smoother) {
            case "mle":
                estimator = new MaximumLikelihoodEstimator(corpus);
                calculator = new SkipCalculator(estimator);
                break;
            case "dmle":
                estimator = new MaximumLikelihoodEstimator(corpus);
                calculator = new DeleteCalculator(estimator);
                break;
            case "fmle":
                estimator = new FalseMaximumLikelihoodEstimator(corpus);
                calculator = new DeleteCalculator(estimator);
                break;
            case "cmle":
                estimator = new ContinuationMaximumLikelihoodEstimator(corpus);
                calculator = new SkipCalculator(estimator);
                break;
            case "dcmle":
                estimator = new ContinuationMaximumLikelihoodEstimator(corpus);
                calculator = new DeleteCalculator(estimator);
                break;

            default:
                // We check for valid smoothers options before, so this should
                // not happen
                throw new IllegalStateException("Missing case statement.");
        }

        if (testing == null) {
            if (crossProductSize == null) {
                throw new IllegalStateException("No testing sequences.");
            } else {
                testing = Files.createTempFile("", "");
                try (BufferedWriter writer =
                        Files.newBufferedWriter(testing,
                                Charset.defaultCharset())) {
                    writeCrossProduct(writer, corpus, crossProductSize);
                }
            }
        }

        try (BufferedReader reader =
                Files.newBufferedReader(testing, Charset.defaultCharset())) {
            int cntZero = 0;
            int cntNonZero = 0;
            double sumPropabilities = 0;
            double entropy = 0;
            double logBase = Math.log(LOG_BASE);

            String sequence;
            while ((sequence = reader.readLine()) != null) {
                double propability = calculator.propability(sequence);

                if (propability == 0) {
                    ++cntZero;
                } else {
                    ++cntNonZero;
                    sumPropabilities += propability;
                    entropy -= Math.log(propability) / logBase;
                    logger.info("Propability(" + sequence + ") = "
                            + propability);
                }
            }

            entropy /= cntNonZero;

            logger.info(StringUtils.repeat("-", 80));

            logger.info("Count Zero-Propablity Sequences = " + cntZero
                    + getPercentLog((double) cntZero / (cntZero + cntNonZero)));
            logger.info("Count Non-Zero-Propability Sequences = "
                    + cntNonZero
                    + getPercentLog((double) cntNonZero
                            / (cntZero + cntNonZero)));
            logger.info("Sum of Propabilities = " + sumPropabilities);
            logger.info("Entropy = " + entropy);

            logger.info(StringUtils.repeat("-", 80));
        }
    }

    private static void writeCrossProduct(
            BufferedWriter writer,
            Corpus corpus,
            int length) throws IOException {
        List<String> words = new ArrayList<String>(corpus.getWords());

        for (int i = 0; i != (int) Math.pow(words.size(), length); ++i) {
            writer.write(getNthCrossProductSequence(words, i, length));
            writer.write("\n");
        }
    }

    private static String getNthCrossProductSequence(
            List<String> words,
            int n,
            int length) {
        List<String> sequence = new LinkedList<String>();
        for (int k = 0; k != length; ++k) {
            sequence.add(words.get(n % words.size()));
            n /= words.size();
        }
        Collections.reverse(sequence);
        return StringUtils.join(sequence, " ");
    }

    private static String getPercentLog(double percent) {
        return " (" + String.format("%.2f", percent * 100) + "%)";
    }

}
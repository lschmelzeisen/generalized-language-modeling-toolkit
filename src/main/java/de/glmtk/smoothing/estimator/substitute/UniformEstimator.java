package de.glmtk.smoothing.estimator.substitute;

import de.glmtk.smoothing.NGram;

public class UniformEstimator extends SubstituteEstimator {

    @Override
    protected double
    calcProbability(NGram sequence, NGram history, int recDepth) {
        long vocabSize = countCache.getVocabSize();
        logDebug(recDepth, "vocabSize = {}", vocabSize);
        return 1.0 / vocabSize;
    }
}

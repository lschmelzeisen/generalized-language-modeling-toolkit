/*
 * Generalized Language Modeling Toolkit (GLMTK)
 *
 * Copyright (C) 2015 Lukas Schmelzeisen
 *
 * GLMTK is free software: you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * GLMTK is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * GLMTK. If not, see <http://www.gnu.org/licenses/>.
 *
 * See the AUTHORS file for contributors.
 */

package de.glmtk.querying.estimator.weightedsum;

import static de.glmtk.common.NGram.WSKP_NGRAM;

import de.glmtk.common.NGram;
import de.glmtk.util.BinomDiamond;
import de.glmtk.util.BinomDiamondNode;


public class WeightedSumAverageEstimator extends AbstractWeightedSumEstimator {
    private static class Node extends BinomDiamondNode<Node> {}

    @Override
    public WeightedSumFunction calcWeightedSumFunction(NGram history) {
        if (history.isEmpty()) {
            WeightedSumFunction weightedSumFunction = new WeightedSumFunction();
            weightedSumFunction.add(1.0, history);
            return weightedSumFunction;
        }

        int order = history.size();
        BinomDiamond<Node> diamond = new BinomDiamond<>(order, Node.class);

        int numWeights = 2 * diamond.size();

        WeightedSumFunction weightedSumFunction =
            new WeightedSumFunction(numWeights);

        for (Node node : diamond.inOrder()) {
            NGram hist = history.applyIntPattern(~node.getIndex(), order);
            weightedSumFunction.add(1.0 / numWeights, hist);
            weightedSumFunction.add(1.0 / numWeights,
                WSKP_NGRAM.concat(hist.convertSkpToWskp()));
        }

        return weightedSumFunction;
    }
}

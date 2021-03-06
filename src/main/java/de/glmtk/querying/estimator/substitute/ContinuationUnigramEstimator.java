/*
 * Generalized Language Modeling Toolkit (GLMTK)
 *
 * Copyright (C) 2014-2015 Lukas Schmelzeisen, Rene Pickhardt
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

package de.glmtk.querying.estimator.substitute;

import static de.glmtk.common.NGram.WSKP_NGRAM;

import de.glmtk.common.NGram;


public class ContinuationUnigramEstimator extends SubstituteEstimator {
    private static final NGram WSKP_WSKP_NGRAM = WSKP_NGRAM.concat(WSKP_NGRAM);

    @Override
    protected double calcProbability(NGram sequence,
                                     NGram history,
                                     int recDepth) {
        return (double) cache
            .getCount(WSKP_NGRAM.concat(sequence.get(0).convertSkpToWskp()))
            / cache.getCount(WSKP_WSKP_NGRAM);
    }
}

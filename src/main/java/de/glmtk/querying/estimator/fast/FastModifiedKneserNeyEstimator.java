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

package de.glmtk.querying.estimator.fast;

import static de.glmtk.common.NGram.SKP_NGRAM;
import static de.glmtk.common.NGram.WSKP_NGRAM;
import de.glmtk.common.NGram;
import de.glmtk.counts.Counts;
import de.glmtk.counts.Discount;

public class FastModifiedKneserNeyEstimator extends FastModifiedKneserNeyAbsEstimator {
    @Override
    protected double calcProbability(NGram sequence,
                                     NGram history,
                                     int recDepth) {
        double denominator = cache.getAbsolute(history.concat(SKP_NGRAM));

        if (history.isEmptyOrOnlySkips()) {
            if (denominator == 0.0)
                return (double) cache.getAbsolute(sequence.get(0))
                        / cache.getNumWords();

            double numerator = cache.getAbsolute(history.concat(sequence));
            return numerator / denominator;
        }

        double discount;
        double gamma = 0.0;
        {
            Discount d = getDiscounts(history.getPattern(), recDepth);
            discount = d.getForCount(cache.getAbsolute(history));

            if (denominator != 0) {
                Counts c = cache.getContinuation(history.concat(NGram.WSKP_NGRAM));
                gamma = (d.getOne() * c.getOneCount() + d.getTwo()
                        * c.getTwoCount() + d.getThree()
                        * c.getThreePlusCount())
                        / denominator;
            }
        }

        double alpha;
        if (denominator == 0.0)
            alpha = (double) cache.getAbsolute(sequence.get(0))
                    / cache.getNumWords();
        else {
            double numerator = cache.getAbsolute(history.concat(sequence));
            numerator = Math.max(numerator - discount, 0.0);

            alpha = numerator / denominator;
        }

        NGram backoffHistory = history.backoffUntilSeen(backoffMode, cache);
        double beta = probabilityLower(sequence, backoffHistory, recDepth);

        return alpha + gamma * beta;
    }

    public final double probabilityLower(NGram sequence,
                                         NGram history,
                                         int recDepth) {
        logTrace(recDepth, "%s#probabilityLower(%s,%s)",
                getClass().getSimpleName(), sequence, history);
        ++recDepth;

        double result = calcProbabilityLower(sequence, history, recDepth);
        logTrace(recDepth, "result = %f", result);

        return result;
    }

    protected double calcProbabilityLower(NGram sequence,
                                          NGram history,
                                          int recDepth) {
        double denominator = cache.getContinuation(
                WSKP_NGRAM.concat(history.convertSkpToWskp().concat(WSKP_NGRAM))).getOnePlusCount();

        if (history.isEmptyOrOnlySkips()) {
            if (denominator == 0.0)
                return (double) cache.getAbsolute(sequence.get(0))
                        / cache.getNumWords();

            double numerator = cache.getContinuation(
                    WSKP_NGRAM.concat(history.concat(sequence)).convertSkpToWskp()).getOnePlusCount();
            return numerator / denominator;
        }

        double discount;
        double gamma = 0.0;
        {
            Discount d = getDiscounts(history.getPattern(), recDepth);
            discount = d.getForCount(cache.getAbsolute(history));

            if (denominator != 0) {
                Counts c = cache.getContinuation(history.concat(NGram.WSKP_NGRAM));
                gamma = (d.getOne() * c.getOneCount() + d.getTwo()
                        * c.getTwoCount() + d.getThree()
                        * c.getThreePlusCount())
                        / denominator;
            }
        }

        double alpha;
        if (denominator == 0.0)
            alpha = (double) cache.getAbsolute(sequence.get(0))
                    / cache.getNumWords();
        else {
            double numerator = cache.getContinuation(
                    WSKP_NGRAM.concat(history.concat(sequence).convertSkpToWskp())).getOnePlusCount();
            numerator = Math.max(numerator - discount, 0.0);
            alpha = numerator / denominator;
        }

        NGram backoffHistory = history.backoffUntilSeen(backoffMode, cache);
        double beta = probabilityLower(sequence, backoffHistory, recDepth);

        return alpha + gamma * beta;
    }
}
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

package de.glmtk.querying.estimator.discount;

import de.glmtk.common.NGram;
import de.glmtk.querying.estimator.fraction.FractionEstimator;


public class AbsoluteDiscountEstimator extends DiscountEstimator {
    private double discount;

    public AbsoluteDiscountEstimator(FractionEstimator fractionEstimator,
                                     double discount) {
        super(fractionEstimator);
        this.discount = discount;
    }

    @Override
    protected double calcDiscount(NGram sequence,
                                  NGram history,
                                  int recDepth) {
        return discount;
    }
}

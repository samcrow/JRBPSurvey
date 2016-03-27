/*
 * Copyright 2016 Sam Crow
 *
 * This file is part of JRBP Survey.
 *
 * JRBP Survey is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * JRBP Survey is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with JRBP Survey.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.samcrow.ridgesurvey;

import org.mapsforge.core.model.LatLong;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * A traveling salesman problem implementation that uses a heuristic
 *
 * At each step, this implementation finds the site nearest to the most recently visited site
 * and proceeds there.
 */
public class Nearest implements TravelingSalesman {
    @Override
    public OrderedRoute solve(Route input, Site start) {
        Objects.requireAllNonNull(input, start);

        final List<Site> sequence = new ArrayList<>();
        final Set<Site> remaining = input.getSites();

        if (BuildConfig.DEBUG) {
            // Check start is in input
            if (!input.getSites().contains(start)) {
                throw new IllegalArgumentException("start is not in input");
            }
        }

        // Add the initial site
        sequence.add(start);
        remaining.remove(start);

        while (!remaining.isEmpty()) {
            // Find the closest with a latitude/longitude approximation
            final LatLong previous = sequence.get(sequence.size() - 1).getPosition();
            double minDistance = Double.MAX_VALUE;
            Site next = null;
            for (Site site : remaining) {
                final double distance = site.getPosition().distance(previous);
                if (distance < minDistance) {
                    minDistance = distance;
                    next = site;
                }
            }
            if (next == null) {
                throw new IllegalStateException("Remaining sites have no distance less than Double.MAX_VALUE");
            }
            sequence.add(next);
            remaining.remove(next);
        }

        return new OrderedRoute(sequence);
    }
}

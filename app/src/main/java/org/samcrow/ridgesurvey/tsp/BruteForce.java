/*
 * Copyright 2017 Sam Crow
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

package org.samcrow.ridgesurvey.tsp;

import org.samcrow.ridgesurvey.OrderedRoute;
import org.samcrow.ridgesurvey.Route;
import org.samcrow.ridgesurvey.Site;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * A brute-force traveling salesman solution
 */
public class BruteForce implements TravelingSalesman {
    @Override
    public OrderedRoute solve(Route input, Site start) {
        final Set<Site> remaining = input.getSites();
        remaining.remove(start);
        final List<Site> sites = solveRecursive(Collections.singletonList(start), remaining);
        return new OrderedRoute(sites);
    }

    private List<Site> solveRecursive(List<Site> partial, Set<Site> remaining) {
        if (remaining.isEmpty()) {
            return partial;
        }

        double minCost = Double.MAX_VALUE;
        List<Site> best = null;
        // Recurse for each possible site to add
        for (Site site : remaining) {
            final Set<Site> nextRemaining = new LinkedHashSet<>(remaining);
            nextRemaining.remove(site);
            final List<Site> next = new ArrayList<>(partial);
            next.add(site);

            final List<Site> subSolution = solveRecursive(next, nextRemaining);
            if (cost(subSolution) < minCost) {
                best = subSolution;
            }
        }

        return best;
    }

    /**
     * Calculates the cost of visiting a list of sites in sequence
     * @param sites
     * @return
     */
    private double cost(List<Site> sites) {
        double cost = 0;
        for (int i = 1; i < sites.size(); i++) {
            final Site prev = sites.get(i - 1);
            final Site current = sites.get(i);
            cost += prev.getPosition().distance(current.getPosition());
        }
        return cost;
    }
}

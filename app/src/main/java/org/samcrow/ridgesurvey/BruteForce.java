package org.samcrow.ridgesurvey;

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

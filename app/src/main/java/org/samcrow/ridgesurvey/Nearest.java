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

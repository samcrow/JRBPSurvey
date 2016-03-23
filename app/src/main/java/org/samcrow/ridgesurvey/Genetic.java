package org.samcrow.ridgesurvey;


import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * A genetic-algorithm-based traveling salesman implementation
 */
public class Genetic implements TravelingSalesman {

    /**
     * The mutation rate
     */
    private final double mMutationRate;

    /**
     * The population size
     */
    private final int mPopulationSize;

    /**
     * The number of iterations
     */
    private final int mIterations;

    public Genetic(double mutationRate, int populationSize, int iterations) {
        mMutationRate = mutationRate;
        mPopulationSize = populationSize;
        mIterations = iterations;
    }

    @Override
    public OrderedRoute solve(Route input, Site start) {
        // Generate some random possibilities
        final List<Site> siteList = new ArrayList<>(input.getSites());
        Possibility[] population = new Possibility[mPopulationSize];
        for (int i = 0; i < population.length; i++) {
            Collections.shuffle(siteList);
            population[i] = new Possibility(siteList.toArray(new Site[siteList.size()]));
        }
        final double startFitness = fittest(population).getFitness();
        // Evolve
        Log.d("Genetic", "Starting evolution");
        double lastFitness = startFitness;
        for (int i = 0; true; i++) {
            final boolean elite = false;
            population = evolve(population, elite, mMutationRate);
            final double newFitness = fittest(population).getFitness();
            final double deltaFitness = newFitness - lastFitness;

            if (i > mIterations && deltaFitness < 10 && newFitness >= 1.3 * startFitness) {
                break;
            }

            lastFitness = newFitness;
        }
        // Select best
        final Possibility best = fittest(population);

        Log.d("Genetic", "Evolution done. Fitness increased from " + startFitness + " to " + best.getFitness());

        return new OrderedRoute(Arrays.asList(best.getSites()));
    }

    private static Possibility[] evolve(Possibility[] population, boolean elitism, double mutationRate) {
        final Possibility[] next = new Possibility[population.length];

        int eliteOffset = 0;
        if (elitism) {
            next[0] = fittest(population);
            eliteOffset = 1;
        }

        // Reproduce
        for (int i = eliteOffset; i < next.length; i++) {
            final Possibility parent1 = select(population);
            final Possibility parent2 = select(population);
            final int start = (int)(Math.random() * parent1.size());
            final int end = (int)(Math.random() * parent1.size());
            final Possibility child = cross(parent1, parent2, start, end);
            next[i] = child;
        }

        // Mutate
        for (int i = eliteOffset; i < next.length; i++) {
            next[i].mutate(mutationRate);
        }

        return next;
    }

    private static Possibility cross(Possibility parent1, Possibility parent2, int start, int end) {
        if (parent1.size() != parent2.size()) {
            throw new IllegalArgumentException("Parent sizes not equal");
        }
        final Site[] combined = new Site[parent1.size()];
        for (int i = 0; i < parent1.size(); i++) {
            if (start < end && i > start && i < end) {
                combined[i] = parent1.getSites()[i];
            } else if (start > end && !(i < start && i > end)) {
                combined[i] = parent1.getSites()[i];
            }
        }
        for (int i = 0; i < parent2.size(); i++) {
            if (!contains(combined, parent2.getSites()[i])) {
                for (int j = 0; j < combined.length; j++) {
                    if (combined[j] == null) {
                        combined[j] = parent2.getSites()[i];
                        break;
                    }
                }
            }
        }

        return new Possibility(combined);
    }

    private static Possibility select(Possibility[] options) {
        final int tournamentSize = 4;
        final Possibility[] selected = new Possibility[tournamentSize];
        for (int i = 0; i < selected.length; i++) {
            final int randomIndex = (int) (Math.random() * options.length);
            selected[i] = options[randomIndex];
        }
        return fittest(selected);
    }

    private static Possibility fittest(Possibility[] population) {
        double maxFitness = Double.NEGATIVE_INFINITY;
        Possibility best = null;
        for (Possibility possible : population) {
            if (possible.getFitness() > maxFitness) {
                best = possible;
            }
        }
        return best;
    }

    private static boolean contains(Site[] sites, Site target) {
        Objects.requireAllNonNull(sites, target);
        for (Site site : sites) {
            if (site != null && site.equals(target)) {
                return true;
            }
        }
        return false;
    }

    private static class Possibility {
        /**
         * The sites to visit, in order. Is not null, and does not contain any null elements.
         */
        private final Site[] mSites;

        public Possibility(Site[] sites) {
            Objects.requireNonNull(sites);
            Objects.requireAllNonNull((Object[]) sites);
            mSites = sites;
        }

        /**
         * Swaps the sites at indices p0 and p1
         *
         * Has no effect if either index is out of range.
         *
         * @param p0 the index of one site
         * @param p1 the index of another site
         */
        private void swap(int p0, int p1) {
            if (p0 >= 0 && p0 < mSites.length && p1 >= 0 && p1 < mSites.length) {
                final Site temp = mSites[p0];
                mSites[p0] = mSites[p1];
                mSites[p1] = temp;
            }
        }

        public void mutate(double mutationRate) {
            for (int i = 0; i < size(); i++) {
                if (Math.random() < mutationRate) {
                    final int otherIndex = (int) (Math.random() * size());
                    swap(i, otherIndex);
                }
            }
        }

        public boolean contains(Site target) {
            Objects.requireNonNull(target);
            for (Site site : mSites) {
                if (site.equals(target)) {
                    return true;
                }
            }
            return false;
        }

        public Site[] getSites() {
            return mSites;
        }

        public int size() {
            return mSites.length;
        }

        /**
         * Returns the fitness of this chromosome
         * @return the fitness
         */
        public double getFitness() {
            if (mSites.length < 2) {
                return 0;
            }
            double distance = 0;
            for (int i = 1; i < mSites.length; i++) {
                final Site prev = mSites[i - 1];
                final Site current = mSites[i];
                distance += prev.getPosition().distance(current.getPosition());
            }
            return 1.0 / distance;
        }
    }
}

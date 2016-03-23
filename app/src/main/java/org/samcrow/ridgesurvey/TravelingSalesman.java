package org.samcrow.ridgesurvey;

/**
 * Interface for algorithms that can solve the traveling salesman problem
 */
public interface TravelingSalesman {
    /**
     * Solves the traveling salesman problem for a route and returns an ordered route solution
     * @param input a route containing the sites to solve. Must not be null.
     * @param start  the site to start at. input must contain this site. Must not be null.
     * @return an ordered route visiting all the sites in the input
     */
    OrderedRoute solve(Route input, Site start);
}

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

package org.samcrow.ridgesurvey.tsp;

import org.samcrow.ridgesurvey.OrderedRoute;
import org.samcrow.ridgesurvey.Route;
import org.samcrow.ridgesurvey.Site;

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

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

package org.samcrow.ridgesurvey;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

/**
 * A named list of species
 */
public class SpeciesGroup {
    /**
     * The name of this group
     */
    @NonNull
    private final String mName;
    /**
     * The species in this group
     */
    @NonNull
    private final List<Species> mSpecies;

    /**
     * Creates a new species group
     * @param name the group name. Must not be null.
     * @param species The species in the group. Must not be null, and must not contain any null
     *                elements.
     */
    public SpeciesGroup(@NonNull String name,
                        @NonNull List<Species> species) {
        Objects.requireNonNull(name);
        Objects.requireNonNull(species);
        for (Species thisSpecies : species) {
            Objects.requireNonNull(thisSpecies, "Null element in species list");
        }
        mName = name;
        mSpecies = new ArrayList<>(species);
    }

    /**
     * Returns the name of this group
     * @return the name
     */
    @NonNull
    public String getName() {
        return mName;
    }

    /**
     * Returns the species in this group
     * @return the list of species
     */
    @NonNull
    public List<Species> getSpecies() {
        return new ArrayList<>(mSpecies);
    }
}

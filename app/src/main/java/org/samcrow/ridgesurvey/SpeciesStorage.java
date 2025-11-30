/*
 * Copyright (c) 2025 Sam Crow
 *
 * This file is part of JRBPSurvey.
 *
 * JRBPSurvey is free software: you can redistribute it and/or modify it under the terms of the
 * GNU General Public License as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 *
 * JRBPSurvey is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
 * PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with Foobar.
 * If not, see <https://www.gnu.org/licenses/>.
 */

package org.samcrow.ridgesurvey;

import android.content.Context;
import android.util.Log;

import androidx.annotation.DrawableRes;

import org.apache.commons.io.IOUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Provides utilities for reading {@link Species} and {@link SpeciesGroup} objects
 */
public class SpeciesStorage {

    private static final String TAG = SpeciesStorage.class.getSimpleName();

    /**
     * Reads zero or more species groups, containing zero or more species, from a stream
     * <p/>
     * The stream must provide JSON-formatted data.
     * <p/>
     * The root of the data must be a JSON array of groups.
     * <p/>
     * Each group must be a JSON object. It must have a "name" key corresponding to a string value
     * containing the name of the species group. It must have a "species" key corresponding to a
     * JSON array of species.
     * <p/>
     * A species must be a JSON object. It must have a "name" key corresponding to a string value
     * containing the name of the species. It may have an "image" key corresponding to a string
     * value. It may have a "description" key corresponding to a string that describes the species.
     * It must have a "column" key corresponding to a string that contains the column name used
     * to submit data on the species.
     * <p/>
     * If the "image" value is present, it must be the name of a drawable resource in the package.
     *
     * @param context a context. Must not be null.
     * @param source  the stream to read from. Must not be null. The stream will not be closed.
     * @return the species groups that were read
     * @throws java.io.IOException if a read or parse error occurs
     */
    public static List<SpeciesGroup> loadSpeciesGroups(Context context, InputStream source)
            throws IOException {
        Objects.requireNonNull(context);
        Objects.requireNonNull(source);

        final InputStreamReader reader = new InputStreamReader(source);
        final String jsonText = IOUtils.toString(reader);
        try {
            final JSONArray groups = new JSONArray(jsonText);
            final List<SpeciesGroup> groupList = new ArrayList<>(groups.length());

            for (int i = 0; i < groups.length(); i++) {
                try {
                    final JSONObject group = groups.getJSONObject(i);
                    final String groupName = group.getString("name");

                    final JSONArray species = group.getJSONArray("species");
                    final List<Species> speciesList = new ArrayList<>(species.length());
                    for (int j = 0; j < species.length(); j++) {
                        try {
                            final JSONObject speciesJsonObject = species.getJSONObject(j);
                            final String speciesName = speciesJsonObject.getString("name");
                            final String column = speciesJsonObject.getString("column");
                            final String description = speciesJsonObject.optString("description",
                                    null);
                            final String resourceName = speciesJsonObject.optString("image", null);
                            @DrawableRes
                            int speciesImage = 0;
                            if (resourceName != null && !resourceName.equals(JSONObject.NULL)) {
                                speciesImage = context.getResources()
                                        .getIdentifier(resourceName,
                                                "drawable",
                                                context.getPackageName());
                                if (speciesImage == 0) {
                                    Log.w(TAG,
                                            "Failed to find resource " + resourceName +
                                                    " for species " + speciesName);
                                }
                            }
                            speciesList.add(
                                    new Species(speciesName, column, description, speciesImage));
                        } catch (JSONException e) {
                            // Proceed to the next species
                        }
                    }
                    groupList.add(new SpeciesGroup(groupName, speciesList));
                } catch (JSONException e) {
                    Log.w(TAG, "Incorrect species group data", e);
                    // Proceed to the next group
                }
            }
            return groupList;
        } catch (JSONException e) {
            // Parse problem
            throw new IOException("JSON parse error", e);
        }
    }

    private SpeciesStorage() {
    }
}

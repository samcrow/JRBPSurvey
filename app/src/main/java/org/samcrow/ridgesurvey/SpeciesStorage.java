package org.samcrow.ridgesurvey;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.util.Log;

import org.apache.commons.io.IOUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * Provides utilities for reading {@link Species} and {@link SpeciesGroup} objects
 */
public class SpeciesStorage {

    private static final String TAG = SpeciesStorage.class.getSimpleName();

    /**
     * Reads zero or more species groups, containing zero or more species, from a stream
     *
     * The stream must provide JSON-formatted data.
     *
     * The root of the data must be a JSON array of groups.
     *
     * Each group must be a JSON object. It must have a "name" key corresponding to a string value
     * containing the name of the species group. It must have a "species" key corresponding to a
     * JSON array of species.
     *
     * A species may be either a string or a JSON object.
     *
     * If a species is a string, the string will be interpreted as the species name. The species
     * will have no image.
     *
     * If the species is a JSON object, it must have a "name" key corresponding to a string value
     * containing the name of the species. It may have an "image" key corresponding to a string
     * or null value.
     *
     * If the "image" value is not null, it must be a string in the form package:type/name.
     * package must be the name of a package that contains resources. type must be "drawable"
     * or "mipmap". name must be the name of a drawable or mipmap resource in the package.
     *
     * @param context a context. Must not be null.
     * @param source the stream to read from. Must not be null. The stream will not be closed.
     * @return the species groups that were read
     * @throws java.io.IOException if a read or parse error occurs
     */
    public static List<SpeciesGroup> loadSpeciesGroups(Context context, InputStream source) throws IOException {
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
                            // The entry in the species array may be either a name string or an object
                            // containing a name and a resource identifier
                            final Object speciesObject = species.get(j);
                            String speciesName;
                            Drawable speciesImage = null;
                            if (speciesObject instanceof String) {
                                speciesName = (String) speciesObject;
                            } else if (speciesObject instanceof JSONObject) {
                                final JSONObject speciesJsonObject = (JSONObject) speciesObject;
                                speciesName = speciesJsonObject.getString("name");
                                final String resourceName = speciesJsonObject.optString("image",
                                        null);
                                if (resourceName != null && !resourceName.equals(JSONObject.NULL)) {
                                    speciesImage = getDrawableResource(context.getResources(),
                                            resourceName);
                                }
                            } else {
                                throw new JSONException(
                                        "Species entry is neither string nor object");
                            }
                            speciesList.add(new Species(speciesName, speciesImage));
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

    /**
     * Finds and returns a drawable resource
     * @param name a resource name, in the form package:type/name. The type must be drawable
     *             or mipmap.
     * @return the drawable resource, or null if none could be found
     */
    private static Drawable getDrawableResource(Resources res, String name) {
        final int resourceId = res.getIdentifier(name, null, null);
        if (resourceId != 0) {
            return res.getDrawable(resourceId);
        } else {
            Log.w(TAG, "Resource " + name + " not found");
            return null;
        }
    }

    private SpeciesStorage() {}
}

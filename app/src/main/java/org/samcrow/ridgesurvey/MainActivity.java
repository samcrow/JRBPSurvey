package org.samcrow.ridgesurvey;

import android.app.AlertDialog.Builder;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.util.Log;

import org.mapsforge.core.model.LatLong;
import org.mapsforge.core.model.MapPosition;
import org.mapsforge.map.android.graphics.AndroidGraphicFactory;
import org.mapsforge.map.android.util.AndroidPreferences;
import org.mapsforge.map.android.util.AndroidUtil;
import org.mapsforge.map.android.view.MapView;
import org.mapsforge.map.layer.cache.TileCache;
import org.mapsforge.map.layer.renderer.TileRendererLayer;
import org.mapsforge.map.model.MapViewPosition;
import org.mapsforge.map.model.Model;
import org.mapsforge.map.model.common.PreferencesFacade;
import org.mapsforge.map.reader.MapFile;
import org.mapsforge.map.rendertheme.InternalRenderTheme;

import java.io.IOException;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    /**
     * A tag that identifies this class, used for logging and preferences
     */
    private static final String TAG = MainActivity.class.getSimpleName();

    /**
     * The initial position of the map
     */
    private static final MapPosition START_POSITION = new MapPosition(
            new LatLong(37.4037, -122.2269), (byte) 13);

    /**
     * The map view
     */
    private MapView mMap;

    /**
     * The preferences facade that the map view uses to save preferences
     */
    private PreferencesFacade mPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setTitle(getString(R.string.map));

        // Set up map graphics
        if (AndroidGraphicFactory.INSTANCE == null) {
            AndroidGraphicFactory.createInstance(getApplication());
        }

        setContentView(R.layout.activity_main);

        mPreferences = new AndroidPreferences(getSharedPreferences(TAG, MODE_PRIVATE));
        try {
            setUpMap();
        } catch (IOException e) {
            new Builder(this)
                    .setTitle(R.string.failed_to_load_map)
                    .setMessage(e.getLocalizedMessage())
                    .show();
            Log.e(TAG, "Failed to set up map", e);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        mMap.getModel().save(mPreferences);
    }

    @Override
    protected void onStop() {
        super.onStop();

    }

    /**
     * Sets up the map view in {@link #mMap}
     */
    private void setUpMap() throws IOException {
        mMap = (MapView) findViewById(R.id.map);
        // Disable built-in zoom controls, unless running in an emulator
        mMap.setBuiltInZoomControls(Build.HARDWARE.equals("goldfish"));

        final Model model = mMap.getModel();
        model.init(mPreferences);

        final DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);

        final TileCache cache = AndroidUtil.createTileCache(this, TAG,
                model.displayModel.getTileSize(),
                metrics.widthPixels, metrics.heightPixels,
                model.frameBufferModel.getOverdrawFactor(), true);

        TileRendererLayer tileRendererLayer = AndroidUtil.createTileRendererLayer(
                cache,
                initializePosition(model.mapViewPosition),
                new MapFile(Storage.getResourceAsFile(this, R.raw.jasper_ridge_map)),
                InternalRenderTheme.OSMARENDER,
                false,
                true);

        mMap.getLayerManager().getLayers().add(tileRendererLayer);



        // Try to load sites
        try {
            final List<Site> sites = SiteStorage.readFromStream(getResources().openRawResource(R.raw.sites));
            final Drawable marker = getResources().getDrawable(R.drawable.ic_gps_fixed_black_18dp);
            for (Site site : sites) {
                mMap.getLayerManager().getLayers().add(new SiteLayer(site, marker));
            }
        } catch (IOException e) {
            new Builder(this)
                    .setTitle(R.string.failed_to_load_sites)
                    .setMessage(e.getLocalizedMessage())
                    .show();
            Log.e(TAG, "Failed to load sites", e);
        }
    }

    /**
     * Accepts a MapViewPosition. If the position is set to a latitude/longitude of (0, 0),
     * sets it to {@link #START_POSITION}
     * @param mvp The position to modify. Must not be null.
     * @return mvp, potentially modified
     */
    private MapViewPosition initializePosition(MapViewPosition mvp) {
        LatLong center = mvp.getCenter();

        if (center.equals(new LatLong(0, 0))) {
            mvp.setMapPosition(START_POSITION);
        }
        return mvp;
    }
}

package org.samcrow.ridgesurvey;

import android.app.AlertDialog.Builder;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.Picture;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.PictureDrawable;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.View;

import com.caverock.androidsvg.SVG;
import com.caverock.androidsvg.SVGParseException;

import org.mapsforge.core.model.LatLong;
import org.mapsforge.core.model.MapPosition;
import org.mapsforge.map.android.graphics.AndroidGraphicFactory;
import org.mapsforge.map.android.util.AndroidPreferences;
import org.mapsforge.map.android.util.AndroidUtil;
import org.mapsforge.map.android.view.MapView;
import org.mapsforge.map.layer.Layer;
import org.mapsforge.map.layer.cache.TileCache;
import org.mapsforge.map.layer.renderer.TileRendererLayer;
import org.mapsforge.map.model.MapViewPosition;
import org.mapsforge.map.model.Model;
import org.mapsforge.map.model.common.PreferencesFacade;
import org.mapsforge.map.reader.MapFile;
import org.mapsforge.map.rendertheme.InternalRenderTheme;
import org.samcrow.ridgesurvey.HeadingCalculator.HeadingListener;

import java.io.IOException;
import java.util.ArrayList;
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

    /**
     * The heading calculator that gathers heading information
     */
    private HeadingCalculator mHeadingCalculator;

    /**
     * The location finder that gets heading information
     */
    private LocationFinder mLocationFinder;

    /**
     * The selection manager
     */
    private SelectionManager mSelectionManager;

    /**
     * The upload status tracker
     */
    private UploadStatusTracker mUploadStatusTracker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle(getString(R.string.map));

        // Set up map graphics
        if (AndroidGraphicFactory.INSTANCE == null || new View(this).isInEditMode()) {
            AndroidGraphicFactory.createInstance(getApplication());
        }

        setContentView(R.layout.activity_main);

        mSelectionManager = new SelectionManager();

        mLocationFinder = new LocationFinder(this);

        // Set up upload status tracker
        mUploadStatusTracker = new UploadStatusTracker(this);
        mUploadStatusTracker.addListener((UploadStatusListener) findViewById(R.id.upload_status_bar));

        final LocalBroadcastManager manager = LocalBroadcastManager.getInstance(this);
        final IntentFilter filter = new IntentFilter();
        filter.addAction(UploadStatusTracker.ACTION_OBSERVATION_MADE);
        filter.addAction(UploadStatusTracker.ACTION_UPLOAD_STARTED);
        filter.addAction(UploadStatusTracker.ACTION_UPLOAD_SUCCESS);
        filter.addAction(UploadStatusTracker.ACTION_UPLOAD_FAILED);
        manager.registerReceiver(mUploadStatusTracker, filter);

        final Compass compass = (Compass) findViewById(R.id.compass);
        mHeadingCalculator = new HeadingCalculator(this);
        if (mHeadingCalculator.isAvailable()) {
            Log.d(TAG, "Heading available");
            mHeadingCalculator.setHeadingListener(new HeadingListener() {
                @Override
                public void headingUpdated(double heading) {
                    compass.setHeading(heading);
                }
            });
        } else {
            Log.d(TAG, "Heading not available");
            compass.setVisibility(View.INVISIBLE);
        }


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

        startUpload();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mMap.getModel().save(mPreferences);
        mLocationFinder.pause();
        mHeadingCalculator.pause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mLocationFinder.resume();
        mHeadingCalculator.resume();
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


        final List<Layer> routeLayers = new ArrayList<>();
        // Try to load sites
        try {
            final List<Route> routes = SiteStorage.readRoutes(
                    getResources().openRawResource(R.raw.sites));
            final float saturation = 1.0f;
            final float value = 1.0f;
            int i = 0;
            for (Route route : routes) {
                if (!route.getSites().isEmpty()) {
                    final float hue = 360.0f * (i / (float) routes.size());

                    // Solve the traveling salesman problem, starting at the southwesternmost point
                    double minLatitude = Double.MAX_VALUE;
                    double minLongitude = Double.MAX_VALUE;
                    Site start = null;
                    for (Site site : route.getSites()) {
                        final double latitude = site.getPosition().latitude;
                        final double longitude = site.getPosition().longitude;
                        if (latitude < minLatitude && longitude < minLongitude) {
                            minLatitude = latitude;
                            minLongitude = longitude;
                            start = site;
                        }
                    }

                    final OrderedRoute solution = new Nearest().solve(route, start);
                    final int color = Color.HSVToColor(new float[]{hue, saturation, value});
                    final Layer routeLayer = new RouteLayer(route, solution, color, mSelectionManager);
                    routeLayers.add(routeLayer);
                }
                i++;
            }
        } catch (IOException e) {
            new Builder(this)
                    .setTitle(R.string.failed_to_load_sites)
                    .setMessage(e.getLocalizedMessage())
                    .show();
            Log.e(TAG, "Failed to load sites", e);
        }

        // Location layers
        final RouteLineLayer routeLineLayer = new RouteLineLayer();
        mSelectionManager.addSelectionListener(routeLineLayer);
        mLocationFinder.addListener(routeLineLayer);
        final MyLocationLayer locationLayer = new MyLocationLayer(getMyLocationDrawable());
        mLocationFinder.addListener(locationLayer);

        // Add layers to the map
        mMap.getLayerManager().getLayers().add(tileRendererLayer);
        mMap.getLayerManager().getLayers().add(routeLineLayer);
        mMap.getLayerManager().getLayers().addAll(routeLayers);
        mMap.getLayerManager().getLayers().add(locationLayer);
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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.home_menu, menu);

        final MenuItem editItem = menu.findItem(R.id.edit_item);
        editItem.setOnMenuItemClickListener(new OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                final Site selectedSite = mSelectionManager.getSelectedSite();
                final Route selectedSiteRoute = mSelectionManager.getSelectedSiteRoute();
                if (selectedSite != null && selectedSiteRoute != null) {
                    final Intent intent = new Intent(MainActivity.this, DataEntryActivity.class);
                    intent.putExtra(DataEntryActivity.ARG_SITE, selectedSite);
                    intent.putExtra(DataEntryActivity.ARG_ROUTE, selectedSiteRoute.getName());
                    startActivity(intent);
                    // Deselect the site so that the user does not accidentally enter an observation
                    // for it after moving to another site
                    mSelectionManager.setSelectedSite(null, null);
                    // Update the status bar
                    LocalBroadcastManager.getInstance(MainActivity.this)
                            .sendBroadcast(new Intent(UploadStatusTracker.ACTION_OBSERVATION_MADE));
                } else {
                    new Builder(MainActivity.this)
                            .setTitle(R.string.no_site_selected)
                            .setMessage(R.string.select_a_site)
                            .show();
                }
                return true;
            }
        });

        final MenuItem uploadItem = menu.findItem(R.id.upload_item);
        uploadItem.setOnMenuItemClickListener(new OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                // Start the upload service
                startUpload();
                return true;
            }
        });
        final UploadMenuItemController controller = new UploadMenuItemController(this, uploadItem);
        mUploadStatusTracker.addListener(controller);

        return true;
    }

    /**
     * Returns a drawable that represents an icon used to display the user's location
     * @return an icon drawable
     */
    private Drawable getMyLocationDrawable() {
        try {
            final SVG svg = SVG.getFromResource(getResources(), R.raw.my_location_icon);
            // Resize based on screen density
            final float density = getResources().getDisplayMetrics().density;
            svg.setDocumentWidth(svg.getDocumentWidth() * density);
            svg.setDocumentHeight(svg.getDocumentHeight() * density);
            final Picture picture = svg.renderToPicture();
            return new PictureDrawable(picture);
        } catch (SVGParseException e) {
            throw new RuntimeException("Could not load my location image", e);
        }
    }

    /**
     * Starts a service to upload observations
     */
    private void startUpload() {
        startService(new Intent(MainActivity.this, UploadService.class));
    }
}

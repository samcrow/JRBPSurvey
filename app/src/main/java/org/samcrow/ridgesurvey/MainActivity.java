package org.samcrow.ridgesurvey;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageManager;
import android.graphics.Picture;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.PictureDrawable;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.View;
import android.widget.Toast;

import com.caverock.androidsvg.SVG;
import com.caverock.androidsvg.SVGParseException;
import com.google.android.material.snackbar.BaseTransientBottomBar;
import com.google.android.material.snackbar.Snackbar;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.json.JSONException;
import org.mapsforge.core.model.LatLong;
import org.mapsforge.core.model.MapPosition;
import org.mapsforge.map.android.graphics.AndroidGraphicFactory;
import org.mapsforge.map.android.util.AndroidPreferences;
import org.mapsforge.map.android.util.AndroidUtil;
import org.mapsforge.map.android.view.MapView;
import org.mapsforge.map.layer.Layer;
import org.mapsforge.map.layer.LayerManager;
import org.mapsforge.map.layer.cache.InMemoryTileCache;
import org.mapsforge.map.layer.cache.TileCache;
import org.mapsforge.map.layer.cache.TwoLevelTileCache;
import org.mapsforge.map.layer.renderer.TileRendererLayer;
import org.mapsforge.map.layer.tilestore.TileStoreLayer;
import org.mapsforge.map.model.Model;
import org.mapsforge.map.model.common.PreferencesFacade;
import org.mapsforge.map.reader.MapFile;
import org.mapsforge.map.rendertheme.StreamRenderTheme;
import org.mapsforge.map.rendertheme.XmlRenderTheme;
import org.samcrow.ridgesurvey.color.Palette;
import org.samcrow.ridgesurvey.data.Database;
import org.samcrow.ridgesurvey.data.IdentifiedObservation;
import org.samcrow.ridgesurvey.data.NetworkBroadcastReceiver;
import org.samcrow.ridgesurvey.data.ObservationDatabase;
import org.samcrow.ridgesurvey.data.RouteState;
import org.samcrow.ridgesurvey.data.SimpleTimedEvent;
import org.samcrow.ridgesurvey.data.SimpleTimedEventDao;
import org.samcrow.ridgesurvey.data.UploadMenuItemController;
import org.samcrow.ridgesurvey.data.UploadService;
import org.samcrow.ridgesurvey.data.UploadStatusListener;
import org.samcrow.ridgesurvey.data.UploadStatusTracker;
import org.samcrow.ridgesurvey.map.MyLocationLayer;
import org.samcrow.ridgesurvey.map.RouteLayer;
import org.samcrow.ridgesurvey.map.RouteLineLayer;
import org.samcrow.ridgesurvey.map.TileFolder;
import org.samcrow.ridgesurvey.map.TileFolderLoad;
import org.samcrow.ridgesurvey.map.TileFolderLoad.DoneHandler;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import androidx.annotation.NonNull;
import androidx.annotation.RawRes;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.room.Room;

public class MainActivity extends AppCompatActivity {

    /**
     * Key corresponding to a RouteState extra that must be provided when launching this activity
     */
    public static final String EXTRA_ROUTE_STATE = MainActivity.class.getName() + ".EXTRA_ROUTE_STATE";
    /**
     * A tag that identifies this class, used for logging and preferences
     */
    private static final String TAG = MainActivity.class.getSimpleName();
    /**
     * A permission request code used when requesting location permission
     */
    private static final int LOCATION_PERMISSION_CODE = 136;
    /**
     * A code used when starting a DataEntryActivity to get a result from it
     */
    private static final int REQUEST_CODE_ENTRY = 13393;
    /**
     * A code used when starting an ObservationListActivity (really used to get a callback when
     * the observation edit activity closes)
     */
    private static final int REQUEST_CODE_OBSERVATION_LIST = 13621;
    /**
     * The initial position of the map
     */
    private static final MapPosition START_POSITION = new MapPosition(
            new LatLong(37.4037, -122.2269), (byte) 15);
    /**
     * The permission that allows the application to access the user's location
     */
    private static final String LOCATION_PERMISSION = "android.permission.ACCESS_FINE_LOCATION";
    private static final String SELECTED_SITE_KEY = MainActivity.class.getName() + ".SELECTED_SITE_KEY";
    /**
     * The executor that runs asynchronous layer site updates
     */
    private final ExecutorService mLayerUpdateExecutor = Executors.newSingleThreadExecutor();
    /**
     * The map view
     */
    private MapView mMap;
    /**
     * The preferences interface
     */
    private SharedPreferences mPreferences;
    /**
     * The preferences facade (wrapping {@link #mPreferences} that the map view uses to save preferences
     */
    private PreferencesFacade mPreferencesFacade;
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
    /**
     * The active route / other information
     */
    private RouteState mRouteState;

    private Database mDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle(getString(R.string.map));

        mRouteState = getIntent().getParcelableExtra(EXTRA_ROUTE_STATE);
        if (mRouteState == null) {
            throw new RuntimeException("Route state extra required");
        }
        // Check that the route state isn't too old
        if (mRouteState.isExpired()) {
            new AlertDialog.Builder(this).setTitle("Verify route")
                    .setMessage("Please go back and start a new route")
                    .setCancelable(false)
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            // This should go back to the welcome activity
                            finish();
                        }
                    }).show();
            // Don't load the rest of the activity
            return;
        }

        if (mRouteState.isTestMode()) {
            setTitle("Map - Test Mode");
        } else {
            setTitle(String.format("%s - %s", getString(R.string.map), mRouteState.getRouteName()));
        }

        // Check location permission
        final int permission = ActivityCompat.checkSelfPermission(this, LOCATION_PERMISSION);
        if (permission == PackageManager.PERMISSION_DENIED) {
            ActivityCompat.requestPermissions(this, new String[]{LOCATION_PERMISSION},
                    LOCATION_PERMISSION_CODE);
        }

        // Set up map graphics
        AndroidGraphicFactory.createInstance(getApplication());

        setContentView(R.layout.activity_main);

        final ActionBar bar = getSupportActionBar();
        if (bar != null) {
            bar.setDisplayHomeAsUpEnabled(true);
            if (mRouteState.isTestMode()) {
                final int color = getResources().getColor(R.color.testModeToolbar);
                bar.setBackgroundDrawable(new ColorDrawable(color));
                final View timerFragment = findViewById(R.id.timer_fragment);
                timerFragment.setBackgroundColor(color);
            }
        }

        mSelectionManager = new SelectionManager();

        mLocationFinder = new LocationFinder(this);

        // Set up upload status tracker
        mUploadStatusTracker = new UploadStatusTracker(this);
        mUploadStatusTracker.addListener(
                (UploadStatusListener) findViewById(R.id.upload_status_bar));

        final LocalBroadcastManager manager = LocalBroadcastManager.getInstance(this);
        final IntentFilter filter = new IntentFilter();
        filter.addAction(UploadStatusTracker.ACTION_OBSERVATION_MADE);
        filter.addAction(UploadStatusTracker.ACTION_UPLOAD_STARTED);
        filter.addAction(UploadStatusTracker.ACTION_UPLOAD_SUCCESS);
        filter.addAction(UploadStatusTracker.ACTION_UPLOAD_FAILED);
        manager.registerReceiver(mUploadStatusTracker, filter);

        // Check for upload/delete every minute
        final IntentFilter tickFilter = new IntentFilter();
        tickFilter.addAction(Intent.ACTION_TIME_TICK);
        registerReceiver(new NetworkBroadcastReceiver(), tickFilter);

        mPreferences = getSharedPreferences(TAG, MODE_PRIVATE);
        mPreferencesFacade = new AndroidPreferences(mPreferences);
        try {
            setUpMap();
        } catch (IOException e) {
            new AlertDialog.Builder(this)
                    .setTitle(R.string.failed_to_load_map)
                    .setMessage(e.getLocalizedMessage())
                    .show();
            Log.e(TAG, "Failed to set up map", e);
        }

        mDatabase = Room.databaseBuilder(getApplicationContext(), Database.class, "events")
                .allowMainThreadQueries()
                .build();

        startUpload();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mMap != null && mPreferencesFacade != null) {
            mMap.getModel().save(mPreferencesFacade);
            mPreferencesFacade.save();
        }

        if (mSelectionManager != null && mPreferences != null) {
            // Save selected colony
            final Site selectedSite = mSelectionManager.getSelectedSite();
            if (selectedSite != null) {
                Log.d(TAG, "Saving selected site " + selectedSite.getId());
                final Editor prefsEditor = mPreferences.edit();
                prefsEditor.putInt(SELECTED_SITE_KEY, selectedSite.getId());
                prefsEditor.apply();
            }
        }
        if (mLocationFinder != null) {
            mLocationFinder.pause();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mLocationFinder != null) {
            mLocationFinder.resume();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_CODE) {
            for (int i = 0; i < permissions.length; i++) {
                final String permission = permissions[i];
                final int result = grantResults[i];
                // If location access was granted, start location finding
                if (permission.equals(
                        LOCATION_PERMISSION) && result == PackageManager.PERMISSION_GRANTED) {
                    mLocationFinder.resume();
                }
            }
        }
    }

    /**
     * Adds orthophoto imagery from the provided tile folder to the map
     *
     * @param tileFolder the folder to get tiles from
     */
    private void addOrthophotos(@NonNull TileFolder tileFolder) {
        final TileCache orthoCache = new TwoLevelTileCache(new InMemoryTileCache(512), tileFolder);
        final Model model = mMap.getModel();
        final TileStoreLayer orthoLayer = new TileStoreLayer(orthoCache, model.mapViewPosition,
                AndroidGraphicFactory.INSTANCE, false);

        final LayerManager layerManager = mMap.getLayerManager();
        layerManager.getLayers().add(0, orthoLayer);
    }

    private void addVectorMaps() {
        try {
            addVectorMapLayer(R.raw.creek, R.raw.creek_render_theme);
        } catch (IOException e) {
            Log.w(TAG, "Failed to load creek map file", e);
        }
        try {
            addVectorMapLayer(R.raw.roads_trails, R.raw.roads_trails_render_theme);
        } catch (IOException e) {
            Log.w(TAG, "Failed to load roads/trails map file", e);
        }
    }

    private void addVectorMapLayer(@RawRes int map_file, @RawRes int theme_file) throws IOException {
        MapFile mapFile = new MapFile(
                Storage.getResourceAsFile(this, map_file));

        // Display the map over the aerial imagery
        final InMemoryTileCache memoryTileCache = new InMemoryTileCache(AndroidUtil.getMinimumCacheSize(this,
                mMap.getModel().displayModel.getTileSize(),
                mMap.getModel().frameBufferModel.getOverdrawFactor(), 0.9f));

        // Custom render theme from XML
        final XmlRenderTheme customTheme = new StreamRenderTheme("",
                getResources().openRawResource(theme_file));

        final TileRendererLayer mapTiles = AndroidUtil.createTileRendererLayer(
                memoryTileCache,
                mMap.getModel().mapViewPosition,
                mapFile,
                customTheme,
                true,
                false,
                false
        );
        mMap.getLayerManager().getLayers().add(1, mapTiles);
    }

    /**
     * Sets up the map view in {@link #mMap}
     */
    private void setUpMap() throws IOException {
        mMap = findViewById(R.id.map);
        // Set fixed tile size to make orthopthoto tiles display correctly
        mMap.getModel().displayModel.setFixedTileSize(256);

        // Start loading orthophoto images in the background
        final TileFolderLoad loadTask = new TileFolderLoad(this, R.raw.tiles, "ortho_tiles_v1",
                "jpeg");
        loadTask.setDoneHandler(new DoneHandler() {
            @Override
            public void done(TileFolder result) {
                addOrthophotos(result);
                // Finish map setup
                addVectorMaps();
                mMap.setCenter(START_POSITION.latLong);
                mMap.setZoomLevel(START_POSITION.zoomLevel);
            }
        });
        loadTask.execute();


        // Disable built-in zoom controls, unless running in an emulator or if the device
        // does not support basic multi-touch
        mMap.setBuiltInZoomControls(Build.HARDWARE.equals("goldfish") || Build.HARDWARE.equals("ranchu")
                || !(getPackageManager().hasSystemFeature(
                PackageManager.FEATURE_TOUCHSCREEN_MULTITOUCH)));

        {
            // Load map with roads and trails
            MapFile mapFile = new MapFile(
                    Storage.getResourceAsFile(this, R.raw.roads_trails));
            try {

                // Limit view to the bounds of the map file
                mMap.getModel().mapViewPosition.setMapLimit(mapFile.boundingBox());
                mMap.getModel().mapViewPosition.setZoomLevelMin(START_POSITION.zoomLevel);

            } finally {
                mapFile.close();
            }
        }

        // Get colors
        final Iterator<Integer> colors = Palette.getColorsRepeating();

        final List<Layer> routeLayers = new ArrayList<>();
        // Try to load sites
        List<Route> routes = null;
        try {
            routes = SiteStorage.readRoutes(
                    getResources().openRawResource(R.raw.sites));
            final ObservationDatabase db = new ObservationDatabase(this);
            for (Route route : routes) {
                final int color = colors.next();
                final Layer routeLayer = new RouteLayer(db, route, color,
                        mSelectionManager);
                routeLayers.add(routeLayer);

            }
        } catch (IOException | JSONException e) {
            new AlertDialog.Builder(this)
                    .setTitle(R.string.failed_to_load_sites)
                    .setMessage(e.getLocalizedMessage())
                    .show();
            Log.e(TAG, "Failed to load sites", e);
        }

        // Location layers
        final RouteLineLayer routeLineLayer = new RouteLineLayer(this);
        mSelectionManager.addSelectionListener(routeLineLayer);
        mLocationFinder.addListener(routeLineLayer);
        final MyLocationLayer locationLayer = new MyLocationLayer(getMyLocationDrawable());
        mLocationFinder.addListener(locationLayer);

        // If a selected site was saved, restore it
        if (routes != null && mPreferences.contains(SELECTED_SITE_KEY)) {
            final int selectedSiteId = mPreferences.getInt(SELECTED_SITE_KEY, 0);
            for (Route route : routes) {
                for (Site site : route.getSites()) {
                    if (site.getId() == selectedSiteId) {
                        Log.d(TAG, "Restoring selected site " + selectedSiteId);
                        mSelectionManager.setSelectedSite(site, route);
                    }
                }
            }
        }

        // Add layers to the map
        mMap.getLayerManager().getLayers().add(routeLineLayer);
        mMap.getLayerManager().getLayers().addAll(routeLayers);
        mMap.getLayerManager().getLayers().add(locationLayer);
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
                    // Look up observations for this site
                    final ObservationDatabase database = new ObservationDatabase(MainActivity.this);
                    final IdentifiedObservation lastObservation = database.getObservationForSite(selectedSite.getId());
                    // If this site has been visited, edit the most recent observation
                    if (lastObservation != null) {
                        startObservationEditActivity(lastObservation);
                    } else {
                        // Otherwise create a new observation
                        startDataEntryActivity(selectedSite, selectedSiteRoute);
                    }
                } else {
                    new AlertDialog.Builder(MainActivity.this)
                            .setTitle(R.string.no_site_selected)
                            .setMessage(R.string.select_a_site)
                            .setNeutralButton(android.R.string.ok, null)
                            .show();
                }
                return true;
            }
        });

        final MenuItem viewObservationsItem = menu.findItem(R.id.view_observations_item);
        viewObservationsItem.setOnMenuItemClickListener(new OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                final Intent intent = new Intent(MainActivity.this, ObservationListActivity.class);
                startActivityForResult(intent, REQUEST_CODE_OBSERVATION_LIST);
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
        if (mUploadStatusTracker != null) {
            mUploadStatusTracker.addListener(controller);
        }

        final MenuItem placeSensorItem = menu.findItem(R.id.home_item_place_sensor);
        initSensorMenuItem(placeSensorItem, "Sensor placement time", "Sensor placed");
        final MenuItem pickUpSensorItem = menu.findItem(R.id.home_item_pick_up_sensor);
        initSensorMenuItem(pickUpSensorItem, "Sensor pickup time", "Sensor picked up");

        final MenuItem viewEventsItem = menu.findItem(R.id.view_events_item);
        viewEventsItem.setOnMenuItemClickListener(new OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                final TimedEventFragment fragment = TimedEventFragment.newInstance();
                fragment.show(getSupportFragmentManager(), "timed events");
                return true;
            }
        });

        return true;
    }

    private void initSensorMenuItem(@NonNull MenuItem item, @NonNull String title, @NonNull String eventName) {
        item.setOnMenuItemClickListener(new OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                final TimePickerDialogFragment fragment = new TimePickerDialogFragment(title);
                fragment.setOnTimePickedListener(new TimePickerDialogFragment.TimePickedListener() {
                    @Override
                    public void onTimePicked(@NonNull TimePickerDialogFragment fragment, @NonNull DateTime selectedDateTime) {
                        final String activeRoute = mRouteState.getRouteName();

                        final SimpleTimedEvent event = new SimpleTimedEvent(selectedDateTime, eventName, activeRoute);
                        final SimpleTimedEventDao dao = mDatabase.simpleTimedEventDao();
                        dao.insert(event);
                        // Upload the new event if possible
                        startUpload();

                        final String timeString = DateTimeFormat.shortTime().print(selectedDateTime);
                        final Snackbar bar = Snackbar.make(
                                mMap,
                                String.format("Recorded \"%s\" at %s",eventName, timeString),
                                BaseTransientBottomBar.LENGTH_LONG
                        );
                        bar.show();
                    }
                });

                fragment.show(getSupportFragmentManager(), title);
                return true;
            }
        });
    }

    private void startObservationEditActivity(IdentifiedObservation observation) {
        final Intent intent = new Intent(this, ObservationEditActivity.class);
        intent.putExtra(ObservationEditActivity.EXTRA_OBSERVATION, observation);
        startActivityForResult(intent, REQUEST_CODE_ENTRY);
    }

    /**
     * Starts an activity to record a new observation for the selected site
     */
    private void startDataEntryActivity(Site selectedSite, Route selectedSiteRoute) {
        final Intent intent = new Intent(this, DataEntryActivity.class);
        intent.putExtra(DataEntryActivity.ARG_SITE, selectedSite);
        intent.putExtra(DataEntryActivity.ARG_ROUTE, selectedSiteRoute.getName());
        intent.putExtra(DataEntryActivity.ARG_ROUTE_STATE, mRouteState);
        startActivityForResult(intent, REQUEST_CODE_ENTRY);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_ENTRY && resultCode == RESULT_OK) {
            // The data entry activity just returned and an observation was recorded

            // Deselect the site so that the user does not accidentally enter an observation
            // for it after moving to another site
            mSelectionManager.setSelectedSite(null, null);

            mLayerUpdateExecutor.submit(new Runnable() {
                @Override
                public void run() {
                    // Tell all route layers to update the visited sites
                    for (Layer layer : mMap.getLayerManager().getLayers()) {
                        if (layer instanceof RouteLayer) {
                            ((RouteLayer) layer).updateVisitedSites();
                        }
                    }
                    mMap.getLayerManager().redrawLayers();
                }
            });
        }
        if (requestCode == REQUEST_CODE_OBSERVATION_LIST) {
            // Observation list has closed, clear selection
            mSelectionManager.setSelectedSite(null, null);
        }
    }

    @Override
    public void onDestroy() {
        if (mMap != null) {
            mMap.destroyAll();
            AndroidGraphicFactory.clearResourceMemoryCache();
        }
        super.onDestroy();
    }

    /**
     * Returns a drawable that represents an icon used to display the user's location
     *
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
        startService(new Intent(getApplicationContext(), UploadService.class));
    }

    // Close this activity when the user presses the back button
    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}

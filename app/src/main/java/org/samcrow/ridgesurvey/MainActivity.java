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

import static org.samcrow.ridgesurvey.map.RouteGraphicsKt.createRouteLayers;
import static org.samcrow.ridgesurvey.map.RouteGraphicsKt.createRouteLines;
import static org.samcrow.ridgesurvey.map.RouteGraphicsKt.readRoutes;

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
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.room.Room;

import com.google.android.material.snackbar.BaseTransientBottomBar;
import com.google.android.material.snackbar.Snackbar;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.maplibre.android.MapLibre;
import org.maplibre.android.camera.CameraPosition;
import org.maplibre.android.geometry.LatLngBounds;
import org.maplibre.android.maps.MapView;
import org.maplibre.android.maps.Style;
import org.maplibre.android.style.layers.Layer;
import org.maplibre.android.style.sources.GeoJsonSource;
import org.maplibre.android.style.sources.RasterSource;
import org.maplibre.geojson.FeatureCollection;
import org.samcrow.ridgesurvey.data.Database;
import org.samcrow.ridgesurvey.data.IdentifiedObservation;
import org.samcrow.ridgesurvey.data.NetworkBroadcastReceiver;
import org.samcrow.ridgesurvey.data.ObservationDatabase;
import org.samcrow.ridgesurvey.data.RouteState;
import org.samcrow.ridgesurvey.data.SimpleTimedEvent;
import org.samcrow.ridgesurvey.data.SimpleTimedEventDao;
import org.samcrow.ridgesurvey.data.UploadMenuItemController;
import org.samcrow.ridgesurvey.data.UploadService;
import org.samcrow.ridgesurvey.data.UploadStatusTracker;
import org.samcrow.ridgesurvey.map.RouteLayer;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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
    public static final LatLngBounds START_POSITION = new LatLngBounds(37.4175457, -122.1919819, 37.3909509, -122.2600484);
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
     * An immutable list of all routes, with their sites
     * <p>
     * This is always non-null after {@link #onCreate(Bundle)}.
     */
    private List<Route> mRoutes;
    /**
     * The preferences interface
     */
    private SharedPreferences mPreferences;
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

    // For testing
    private TileServer mTileServer;

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
        MapLibre.getInstance(this);

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

        mRoutes = readRoutes(this);
        mSelectionManager = new SelectionManager(mRoutes);

        // Set up upload status tracker
        mUploadStatusTracker = new UploadStatusTracker(this);
        mUploadStatusTracker.addListener(
                findViewById(R.id.upload_status_bar));

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

        mTileServer = new TileServer(this, "tiles-smco-2022");
    }

    @Override
    protected void onStart() {
        super.onStart();
        mMap.onStart();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mMap.onPause();

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
    }

    @Override
    protected void onResume() {
        super.onResume();
        mMap.onResume();
    }

    @Override
    protected void onStop() {
        super.onStop();
        mMap.onStop();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mMap.onLowMemory();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        mMap.onSaveInstanceState(outState);
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
                    // TODO: Request location
                }
            }
        }
    }

    /**
     * Sets up the map view in {@link #mMap}
     */
    private void setUpMap() throws IOException {
        mMap = findViewById(R.id.map);
        mMap.getMapAsync(map -> {
            final RouteLayer routeLayer = new RouteLayer(new ObservationDatabase(this), mRoutes, mSelectionManager);
            mSelectionManager.addSelectionListener(routeLayer);
            final String tileJsonUrl;
            try {
                tileJsonUrl = mTileServer.getTileJsonUrl().get();
            } catch (ExecutionException | InterruptedException e) {
                throw new RuntimeException(e);
            }
            final RasterSource imagery = new RasterSource("smco_2022_tiles", tileJsonUrl);

            final Style.Builder style = new Style.Builder()
                    .fromUri("asset://map_style.json")
                    .withSources(imagery, routeLayer.getSource());
            for (Layer layer : createRouteLayers(this)) {
                style.withLayer(layer);
            }
            map.setStyle(style);

            final CameraPosition initialCamera = map.getCameraForLatLngBounds(START_POSITION);
            assert initialCamera != null;
            map.setCameraPosition(initialCamera);
            map.getUiSettings().setRotateGesturesEnabled(false);
            map.getUiSettings().setAttributionEnabled(false);
            map.getUiSettings().setLogoEnabled(false);
            map.addOnMapClickListener(mSelectionManager);
        });

        // Location layer


//
//        // If a selected site was saved, restore it
//        if (routes != null && mPreferences.contains(SELECTED_SITE_KEY)) {
//            final int selectedSiteId = mPreferences.getInt(SELECTED_SITE_KEY, 0);
//            for (Route route : routes) {
//                for (Site site : route.getSites()) {
//                    if (site.getId() == selectedSiteId) {
//                        Log.d(TAG, "Restoring selected site " + selectedSiteId);
//                        mSelectionManager.setSelectedSite(site, route);
//                    }
//                }
//            }
//        }
//
//        // Add layers to the map
//        mMap.getLayerManager().getLayers().add(routeLineLayer);
//        mMap.getLayerManager().getLayers().addAll(routeLayers);
//        mMap.getLayerManager().getLayers().add(locationLayer);
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
                                String.format("Recorded \"%s\" at %s", eventName, timeString),
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
//                    for (Layer layer : mMap.getLayerManager().getLayers()) {
//                        if (layer instanceof RouteLayer) {
//                            ((RouteLayer) layer).updateVisitedSites();
//                        }
//                    }
//                    mMap.getLayerManager().redrawLayers();
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
        super.onDestroy();
        mMap.onDestroy();
        mTileServer.close();
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

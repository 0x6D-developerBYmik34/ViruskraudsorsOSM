package com.example.mik.viruskraudsorsosm;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.StrictMode;
import android.preference.PreferenceManager;
import android.util.Log;
import android.util.LogPrinter;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import org.osmdroid.api.IGeoPoint;
import org.osmdroid.api.IMapController;
import org.osmdroid.bonuspack.routing.OSRMRoadManager;
import org.osmdroid.bonuspack.routing.Road;
import org.osmdroid.bonuspack.routing.RoadManager;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.ItemizedOverlayWithFocus;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Overlay;
import org.osmdroid.views.overlay.OverlayItem;
import org.osmdroid.views.overlay.Polyline;
import org.osmdroid.views.overlay.gestures.RotationGestureOverlay;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MainActivity extends AppCompatActivity{
    private final int REQUEST_PERMISSIONS_REQUEST_CODE = 1;
    private MapView map = null;
    private MyLocationNewOverlay mLocationOverlay;
    private RotationGestureOverlay mRotationGestureOverlay;
    private final GeoPoint kvantoriumLocate = new GeoPoint(48.7032d, 44.50477d);
    private boolean routeState = false;
    private List<Marker> numsRouteMarks = new ArrayList<>();
    private Overlay roadOver;
    private final Overlay routeOverlay = new Overlay(){
        @Override
        public boolean onTouchEvent(MotionEvent event, MapView mapView) {
            return handlerMapClick(event, mapView);
//            return super.onTouchEvent(event, mapView);
        }
    };

    @SuppressLint("UseCompatLoadingForDrawables")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //handle permissions first, before map is created. not depicted here

        //load/initialize the osmdroid configuration, this can be done
        Context ctx = getApplicationContext();
        Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx));
        //setting this before the layout is inflated is a good idea
        //it 'should' ensure that the map has a writable location for the map cache, even without permissions
        //if no tiles are displayed, you can try overriding the cache path using Configuration.getInstance().setCachePath
        //see also StorageUtils
        //note, the load method also sets the HTTP User Agent to your application's package name, abusing osm's
        //tile servers will get you banned based on this string

        //inflate and create the map
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        setContentView(R.layout.main);

        map = (MapView) findViewById(R.id.map);
        map.setTileSource(TileSourceFactory.MAPNIK);

        requestPermissionsIfNecessary(new String[] {
                // if you need to show the current location, uncomment the line below
                Manifest.permission.ACCESS_FINE_LOCATION,
                // WRITE_EXTERNAL_STORAGE is required in order to show the map
                Manifest.permission.WRITE_EXTERNAL_STORAGE
        });

        this.mLocationOverlay = new MyLocationNewOverlay(new GpsMyLocationProvider(ctx), map);
        this.mLocationOverlay.enableMyLocation();
        map.getOverlays().add(mLocationOverlay);

        this.mRotationGestureOverlay = new RotationGestureOverlay(map);
        mRotationGestureOverlay.setEnabled(true);
        map.setMultiTouchControls(true);
        map.getOverlays().add(this.mRotationGestureOverlay);

        Marker startMarker = new Marker(map);
        startMarker.setPosition(this.kvantoriumLocate); // Lat/Lon decimal degrees
        startMarker.setTextLabelBackgroundColor(
                Color.TRANSPARENT
        );
        startMarker.setTextLabelForegroundColor(
                Color.RED
        );
        startMarker.setTextLabelFontSize(40);
        startMarker.setTextIcon("Кванториум");
        startMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        startMarker.setTitle("Кванториум ДЖД");
        map.getOverlays().add(startMarker);

        IMapController mapController = map.getController();
        mapController.setZoom(17.5);
        mapController.setCenter(kvantoriumLocate);
        mapController.stopPanning();

    }

    private boolean handlerMapClick(MotionEvent event, MapView mapView) {
        if(routeState){
            if (numsRouteMarks.size()==2) return false;
            if (event.getActionMasked()!=MotionEvent.ACTION_DOWN) return false;

            Drawable ico = ContextCompat.getDrawable(getApplicationContext(),R.drawable.osm_ic_center_map);
            IGeoPoint tchPoint = map.getProjection().fromPixels((int) event.getX(), (int) event.getY());
            Marker m = new Marker(map);
            m.setPosition((GeoPoint) tchPoint);
            m.setIcon(ico);
            mapView.getOverlays().add(m);
            numsRouteMarks.add(m);

            if(numsRouteMarks.size()==2){
                RoadManager roadManager = new OSRMRoadManager(this);
                ArrayList<GeoPoint> waypoints = new ArrayList<>();
                for(Marker i:numsRouteMarks) waypoints.add(i.getPosition());
                Road road = roadManager.getRoad(waypoints);
                roadOver = RoadManager.buildRoadOverlay(road);
                map.getOverlays().add(roadOver);
                map.invalidate();
            }
        }
        return false;
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    public void onResume() {
        super.onResume();
        this.mLocationOverlay.enableMyLocation();
        //this will refresh the osmdroid configuration on resuming.
        //if you make changes to the configuration, use
        //SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        //Configuration.getInstance().load(this, PreferenceManager.getDefaultSharedPreferences(this));
        map.onResume(); //needed for compass, my location overlays, v6.0.0 and up

    }

    @Override
    public void onPause() {
        super.onPause();
        this.mLocationOverlay.disableMyLocation();
        //this will refresh the osmdroid configuration on resuming.
        //if you make changes to the configuration, use
        //SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        //Configuration.getInstance().save(this, prefs);
        map.onPause();  //needed for compass, my location overlays, v6.0.0 and up
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        ArrayList<String> permissionsToRequest;
        permissionsToRequest = new ArrayList<>(Arrays.asList(permissions).subList(0, grantResults.length));
        if (permissionsToRequest.size() > 0) {
            ActivityCompat.requestPermissions(
                    this,
                    permissionsToRequest.toArray(new String[0]),
                    REQUEST_PERMISSIONS_REQUEST_CODE);
        }
    }

    private void requestPermissionsIfNecessary(String[] permissions) {
        ArrayList<String> permissionsToRequest = new ArrayList<>();
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(this, permission)
                    != PackageManager.PERMISSION_GRANTED) {
                // Permission is not granted
                permissionsToRequest.add(permission);
            }
        }
        if (permissionsToRequest.size() > 0) {
            ActivityCompat.requestPermissions(
                    this,
                    permissionsToRequest.toArray(new String[0]),
                    REQUEST_PERMISSIONS_REQUEST_CODE);
        }
    }

    public void buildRoute(View view) {
        Button btn = (Button) view;
        if(!routeState){
            routeState = true;
            btn.setText(R.string.Cancel);
            map.getOverlays().add(routeOverlay);

        }else{
            routeState = false;
            btn.setText(R.string.getRouteBtnName);
            map.getOverlays().remove(routeOverlay);
            for(Marker i:numsRouteMarks){
                map.getOverlays().remove(i);
            }
            map.getOverlays().remove(roadOver);
            numsRouteMarks.clear();
            map.invalidate();
        }
    }

    public void showDangerousZone(View view) {

    }
}
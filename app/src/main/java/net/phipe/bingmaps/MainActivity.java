package net.phipe.bingmaps;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;

import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.microsoft.maps.GPSMapLocationProvider;
import com.microsoft.maps.Geopath;
import com.microsoft.maps.Geoposition;
import com.microsoft.maps.MapElement;
import com.microsoft.maps.MapFlyout;
import com.microsoft.maps.MapPolygon;
import com.microsoft.maps.MapPolyline;
import com.microsoft.maps.MapRenderMode;
import com.microsoft.maps.MapServices;
import com.microsoft.maps.MapTappedEventArgs;
import com.microsoft.maps.MapUserLocation;
import com.microsoft.maps.MapUserLocationTrackingState;
import com.microsoft.maps.MapView;
import com.microsoft.maps.Geopoint;
import com.microsoft.maps.MapAnimationKind;
import com.microsoft.maps.MapScene;
import com.microsoft.maps.MapElementLayer;
import com.microsoft.maps.MapIcon;
import com.microsoft.maps.MapImage;
import com.microsoft.maps.MapStyleSheets;
import com.microsoft.maps.MapProjection;
import com.microsoft.maps.MapRouteLine;
import com.microsoft.maps.OnMapTappedListener;
import com.microsoft.maps.routing.MapRoute;
import com.microsoft.maps.routing.MapRouteDrivingOptions;
import com.microsoft.maps.routing.MapRouteFinder;
import com.microsoft.maps.routing.MapRouteFinderResult;
import com.microsoft.maps.routing.MapRouteFinderStatus;
import com.microsoft.maps.routing.MapRouteOptimization;
import com.microsoft.maps.routing.MapRouteRestrictions;
import com.microsoft.maps.routing.OnMapRouteFinderResultListener;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private MapView mMapView;
    private static final Geopoint CASA = new Geopoint(20.132003, -101.181823);
    private Geopoint startPin = null;
    private MapElementLayer mPinLayer;
    private boolean locationPin = false;
    private int indexLocationPin = 0;
    private Button btn_delete;
    private RequestQueue queue;
    private JsonObjectRequest requestMapRequest;
    private ArrayList<Geoposition> listRoute;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        queue = Volley.newRequestQueue(this);

        if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                        != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION}, 1);
        }

        mMapView = new MapView(this, MapRenderMode.VECTOR);  // or use MapRenderMode.RASTER for 2D map
        mMapView.setCredentialsKey(getString(R.string.key_api));
        ((FrameLayout) findViewById(R.id.map_view)).addView(mMapView);
        mMapView.onCreate(savedInstanceState);
        mMapView.setScene(
                MapScene.createFromLocationAndZoomLevel(CASA, 13),
                MapAnimationKind.NONE);
        mPinLayer = new MapElementLayer();
        mMapView.getLayers().add(mPinLayer);


        mMapView.setMapStyleSheet(MapStyleSheets.roadLight());
        mMapView.setMapProjection(MapProjection.GLOBE);
        mMapView.setMapProjection(MapProjection.WEB_MERCATOR);

        MapUserLocation userLocation = mMapView.getUserLocation();
        MapUserLocationTrackingState userLocationTrackingState = userLocation.startTracking(new GPSMapLocationProvider.Builder(getApplicationContext()).build());
        if (userLocationTrackingState == MapUserLocationTrackingState.PERMISSION_DENIED) {
            // request for user location permissions and then call startTracking again
        } else if (userLocationTrackingState == MapUserLocationTrackingState.READY) {
            // handle the case where location tracking was successfully started
        } else if (userLocationTrackingState == MapUserLocationTrackingState.DISABLED) {
            // handle the case where all location providers were disabled
        }

        /*mMapView.addOnMapTappedListener(new OnMapTappedListener() {
            @Override
            public boolean onMapTapped(MapTappedEventArgs mapTappedEventArgs) {
                Geopoint position = mapTappedEventArgs.location;
                startPin = position;

                if (!locationPin) {
                    String pin = "Inicio";
                    MapIcon pintest = new MapIcon();
                    pintest.setLocation(position);
                    pintest.setTitle(pin);
                    indexLocationPin = mPinLayer.getElements().size() - 1;
                    mPinLayer.getElements().add(indexLocationPin, pintest);
                    obtenerRouteFromMapRequest();
                    //drawLineOnMap(mMapView);
                    locationPin = true;
                }
                return false;
            }
        });*/

        drawPins();

        /*btn_delete = findViewById(R.id.btn_delete);
        btn_delete.setOnClickListener(view -> {
            if (locationPin) {
                locationPin = false;
                mPinLayer.getElements().remove(indexLocationPin);
                startPin = null;

                mMapView.getLayers().remove(mMapView.getLayers().size()-1);
            }
        });*/

        btn_delete = findViewById(R.id.btn_route);
        btn_delete.setOnClickListener(view -> {
            if (userLocation.getLastLocationData() != null) {
                startPin = new Geopoint(userLocation.getLastLocationData().getLatitude(), userLocation.getLastLocationData().getLongitude());
                obtenerRouteFromMapRequest();
            }
        });
    }

    void drawLineOnMap() {
        MapPolyline mapPolyline = new MapPolyline();
        mapPolyline.setPath(new Geopath(listRoute));
        mapPolyline.setStrokeColor(Color.rgb(0, 164, 239));
        mapPolyline.setStrokeWidth(5);
        mapPolyline.setStrokeDashed(false);

        // Add Polyline to a layer on the map control.
        MapElementLayer linesLayer = new MapElementLayer();
        linesLayer.setZIndex(1.0f);
        linesLayer.getElements().add(mapPolyline);
        mMapView.getLayers().add(linesLayer);
        Log.d("MapLayer", ""+mMapView.getLayers().size());
    }

    @Override
    protected void onStart() {
        super.onStart();
        mMapView.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mMapView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mMapView.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
        mMapView.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mMapView.onDestroy();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState, @NonNull PersistableBundle outPersistentState) {
        super.onSaveInstanceState(outState, outPersistentState);
        mMapView.onSaveInstanceState(outState);
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mMapView.onLowMemory();
    }

    private void obtenerRouteFromMapRequest() {
        String URL = String.format("https://dev.virtualearth.net/REST/v1/Routes/Driving?wayPoint.1=%f,%f&wayPoint.2=%f,%f&optimize=time&distanceUnit=km&key=%s",
                startPin.getPosition().getLatitude(), startPin.getPosition().getLongitude(), CASA.getPosition().getLatitude(), CASA.getPosition().getLongitude(),
                getString(R.string.key_api));
        Log.d("URL", URL);
        queue = Volley.newRequestQueue(this);
        requestMapRequest = new JsonObjectRequest(URL, null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        Log.d("GIVO", "se ejecuto");
                        try {
                            JSONArray indicaiones = response.getJSONArray("resourceSets").getJSONObject(0).
                                    getJSONArray("resources").getJSONObject(0).getJSONArray("routeLegs")
                                    .getJSONObject(0).getJSONArray("itineraryItems");

                            listRoute = new ArrayList<>();

                            for (int i = 0; i < indicaiones.length(); i++) {
                                JSONObject indi = indicaiones.getJSONObject(i);
                                JSONArray strlatlog = (JSONArray) indi.getJSONObject("maneuverPoint").get("coordinates");
                                double lat = strlatlog.getDouble(0);
                                double lon = strlatlog.getDouble(1);
                                listRoute.add(new Geoposition(lat, lon));
                                Log.d("JSON", "lat: "+lat+" lon: "+lon);
                            }

                            drawLineOnMap();
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.d("GIVO", "se ejecuto CON ERROR");
                        Log.d("GIVO", error.toString());
                    }
                }
        );
        queue.add(requestMapRequest);
    }

    public void drawPins() {
        String pin = "Casa";
        MapIcon pin1 = new MapIcon();
        pin1.setLocation(CASA);
        pin1.setTitle(pin);
        mPinLayer.getElements().add(pin1);

        MapFlyout flyout = new MapFlyout();
        flyout.setTitle("Casa");
        flyout.setDescription("Hogar dulce hogar");
        pin1.setFlyout(flyout);
    }
}
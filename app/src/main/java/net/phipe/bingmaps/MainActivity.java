package net.phipe.bingmaps;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.res.ResourcesCompat;

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
    private static final Geopoint ITSUR = new Geopoint(20.140062, -101.150552);
    private Geopoint startPin = null;
    private MapElementLayer mPinLayer;
    private boolean locationPin = false;
    private int indexLocationPin = 0;
    private Button btn_delete;
    private RequestQueue queue;
    private JsonObjectRequest requestMapRequest;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        queue = Volley.newRequestQueue(this);

        mMapView = new MapView(this, MapRenderMode.VECTOR);  // or use MapRenderMode.RASTER for 2D map
        mMapView.setCredentialsKey(getString(R.string.key_api));
        ((FrameLayout) findViewById(R.id.map_view)).addView(mMapView);
        mMapView.onCreate(savedInstanceState);
        mMapView.setScene(
                MapScene.createFromLocationAndZoomLevel(ITSUR, 15),
                MapAnimationKind.NONE);
        mPinLayer = new MapElementLayer();
        mMapView.getLayers().add(mPinLayer);


        mMapView.setMapStyleSheet(MapStyleSheets.roadLight());
        mMapView.setMapProjection(MapProjection.GLOBE);
        mMapView.setMapProjection(MapProjection.WEB_MERCATOR);

        mMapView.addOnMapTappedListener(new OnMapTappedListener() {
            @Override
            public boolean onMapTapped(MapTappedEventArgs mapTappedEventArgs) {
                Geopoint position = mapTappedEventArgs.location;
                startPin = position;

                if (!locationPin) {
                    String pin = "Destino";
                    MapIcon pintest = new MapIcon();
                    pintest.setLocation(position);
                    pintest.setTitle(pin);
                    indexLocationPin = mPinLayer.getElements().size() - 1;
                    Log.d("GPS", "index: " + Integer.toString(indexLocationPin));
                    mPinLayer.getElements().add(indexLocationPin, pintest);
                    obtenerRouteFromMapRequest();
                    locationPin = true;
                }
                return false;
            }
        });

        MapUserLocation userLocation = mMapView.getUserLocation();

        MapUserLocationTrackingState userLocationTrackingState = userLocation.startTracking(new GPSMapLocationProvider.Builder(getApplicationContext()).build());
        if (userLocationTrackingState == MapUserLocationTrackingState.PERMISSION_DENIED) {
            // request for user location permissions and then call startTracking again
        } else if (userLocationTrackingState == MapUserLocationTrackingState.READY) {
            // handle the case where location tracking was successfully started
        } else if (userLocationTrackingState == MapUserLocationTrackingState.DISABLED) {
            // handle the case where all location providers were disabled
        }

        //drawLineOnMap(mMapView);
        drawPins();

        btn_delete = findViewById(R.id.btn_delete);
        btn_delete.setOnClickListener(view -> {
            if (locationPin) {
                locationPin = false;
                mPinLayer.getElements().remove(indexLocationPin);
                startPin = null;
            }
        });
    }

    void drawLineOnMap(MapView mapView) {
        Geoposition center = mapView.getCenter().getPosition();

        ArrayList<Geoposition> geopoints = fillData();

        MapPolyline mapPolyline = new MapPolyline();
        mapPolyline.setPath(new Geopath(geopoints));
        mapPolyline.setStrokeColor(Color.rgb(0, 164, 239));
        mapPolyline.setStrokeWidth(5);
        mapPolyline.setStrokeDashed(false);

        // Add Polyline to a layer on the map control.
        MapElementLayer linesLayer = new MapElementLayer();
        linesLayer.setZIndex(1.0f);
        linesLayer.getElements().add(mapPolyline);
        mapView.getLayers().add(linesLayer);
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
        //obtenerRouteFromMapRequest();
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

    public ArrayList<Geoposition> fillData() {
        ArrayList<Geoposition> geopoints = new ArrayList<Geoposition>();
        geopoints.add(new Geoposition(20.131912,
                -101.181968));
        geopoints.add(new Geoposition(20.132098,
                -101.182085));
        geopoints.add(new Geoposition(20.135686,
                -101.180554));
        geopoints.add(new Geoposition(20.135493,
                -101.178985));
        geopoints.add(new Geoposition(20.136591,
                -101.178816));
        geopoints.add(new Geoposition(20.136665,
                -101.176773));
        geopoints.add(new Geoposition(20.1396,
                -101.176733));
        geopoints.add(new Geoposition(20.139332,
                -101.171185));
        geopoints.add(new Geoposition(20.141595,
                -101.171048));
        geopoints.add(new Geoposition(20.142849,
                -101.157803));
        geopoints.add(new Geoposition(20.14293,
                -101.156401));
        geopoints.add(new Geoposition(20.1435,
                -101.149911));
        geopoints.add(new Geoposition(20.140523,
                -101.150548));
        geopoints.add(new Geoposition(20.140523,
                -101.150548));
        return geopoints;
    }

    private void obtenerRouteFromMapRequest() {
        String URL = String.format("https://dev.virtualearth.net/REST/v1/Routes/Driving?wayPoint.1=%f,%f&wayPoint.2=%f,%f&optimize=time&distanceUnit=km&key=%s",
                startPin.getPosition().getLatitude(), startPin.getPosition().getLongitude(), ITSUR.getPosition().getLatitude(), ITSUR.getPosition().getLongitude(),
                getString(R.string.key_api));
        Log.d("URL", URL);
        queue = Volley.newRequestQueue(this);
        requestMapRequest = new JsonObjectRequest(URL, null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        Log.d("GIVO", "se ejecuto");
                        try {
                            JSONArray indicaiones = response.getJSONObject("resourceSets").getJSONArray("resources").getJSONObject(6).getJSONArray("itineraryItems");
                            //JSONArray indicaiones = response.getJSONObject("resourceSets").getJSONArray("resources");

                            for (int i = 0; i < indicaiones.length(); i++) {
                                JSONObject indi = indicaiones.getJSONObject(i);
                                String strlatlog = indi.getJSONObject("maneuverPoint").get("coordinates").toString();
                                Log.d("JSON", strlatlog);
                            }

                            /*for (int i = 0; i < indicaiones.length(); i++) {
                                JSONObject indi = indicaiones.getJSONObject(i);
                                String strlatlog = indi.getJSONObject("maneuverPoint").get("coordinates"). + "," + indi.getJSONObject("startPoint").get("lng").toString();
                                Log.d("GIVO", "se ejecuto: " + strlatlog);
                            }*/
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
        Geopoint l1 = new Geopoint(20.139561, -101.150646);
        String pin = "ITSUR";
        MapIcon pin1 = new MapIcon();
        pin1.setLocation(l1);
        pin1.setTitle(pin);
        mPinLayer.getElements().add(pin1);

        MapFlyout flyout = new MapFlyout();
        flyout.setTitle("ITSUR");
        flyout.setDescription("Instituto Tecnológico Superior del Sur de Guanajuato");
        pin1.setFlyout(flyout);
    }

    public void mapRoute() {
        MapServices.setCredentialsKey(getString(R.string.key_api));
        MapServices.setContext(getApplicationContext());
        MapServices.setLanguage("Spanish");

        MapRouteDrivingOptions options = new MapRouteDrivingOptions()
                .setRouteOptimization(MapRouteOptimization.TIME_WITH_TRAFFIC)
                .setRouteRestrictions(MapRouteRestrictions.TOLLROADS)
                .setMaxAlternateRouteCount(2);

        if (startPin != null) {
            try {
                MapRouteFinder.getDrivingRoute(startPin, ITSUR, options,
                        new OnMapRouteFinderResultListener() {
                            @Override
                            public void onMapRouteFinderResult(MapRouteFinderResult result) {
                                if (result.getStatus() == MapRouteFinderStatus.SUCCESS) {
                                    MapRoute route = result.getRoute();
                                    List<MapRoute> alternateRoutes = result.getAlternateRoutes();
                                }
                            }
                        }
                );
            } catch (Exception e) {
                Log.d("GPS", e.toString());
            }
        }
    }
}
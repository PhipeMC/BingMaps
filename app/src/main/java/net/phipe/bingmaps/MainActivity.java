package net.phipe.bingmaps;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.widget.FrameLayout;
import com.microsoft.maps.MapRenderMode;
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

public class MainActivity extends AppCompatActivity {

    private MapView mMapView;
    private static final Geopoint ITSUR = new Geopoint(20.140062, -101.150552);
    private MapElementLayer mPinLayer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mMapView = new MapView(this, MapRenderMode.VECTOR);  // or use MapRenderMode.RASTER for 2D map
        mMapView.setCredentialsKey("AoTiyZa6RZpBERvYHs-2CzOFrvBOjHpnD5COkE9m4jWdW2Cjg2nwRY6MWiP0hkuq");
        ((FrameLayout)findViewById(R.id.map_view)).addView(mMapView);
        mMapView.onCreate(savedInstanceState);
        mMapView.setScene(
                MapScene.createFromLocationAndZoomLevel(ITSUR, 16),
                MapAnimationKind.NONE);
        mPinLayer = new MapElementLayer();
        mMapView.getLayers().add(mPinLayer);

        Geopoint location = new Geopoint(20.140062, -101.150552);  // your pin lat-long coordinates
        String title = "Pin";       // title to be shown next to the pin
        //Bitmap pinBitmap = ...   // your pin graphic (optional)

        MapIcon pushpin = new MapIcon();
        pushpin.setLocation(location);
        pushpin.setTitle(title);
        //pushpin.setImage(new MapImage(pinBitmap));

        mPinLayer.getElements().add(pushpin);
        mMapView.setMapStyleSheet(MapStyleSheets.roadDark());
        mMapView.setMapProjection(MapProjection.GLOBE);
        mMapView.setMapProjection(MapProjection.WEB_MERCATOR);
    }

    @Override
    protected void onStart() {
        super.onStart();
        mMapView.onStart();
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
}
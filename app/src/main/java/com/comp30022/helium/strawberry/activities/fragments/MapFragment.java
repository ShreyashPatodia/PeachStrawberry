package com.comp30022.helium.strawberry.activities.fragments;

import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;

import com.comp30022.helium.strawberry.StrawberryApplication;
import com.comp30022.helium.strawberry.components.server.PeachServerInterface;
import com.comp30022.helium.strawberry.entities.User;

import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.comp30022.helium.strawberry.R;
import com.comp30022.helium.strawberry.components.location.LocationEvent;
import com.comp30022.helium.strawberry.components.location.LocationServiceFragment;
import com.comp30022.helium.strawberry.components.map.StrawberryMap;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;

import java.util.ArrayList;
import java.util.List;

/**
 * A simple {@link Fragment} subclass.
 */
public class MapFragment extends LocationServiceFragment implements OnMapReadyCallback, View.OnClickListener {
    private static final String TAG = "StrawberryMapFragment";
    private StrawberryMap map;
    private SupportMapFragment mMapView;
    private Button drive;
    private Button walk;
    private Button bicycle;
    private Button transit;
    private Button lastChanged;
    private TextView arrival_time;
    private TextView arrival_distance;
    private ArrayList<Location> locations;

    public MapFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_map, container, false);

        mMapView = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map);
        mMapView.onCreate(savedInstanceState);

        arrival_time = (TextView) view.findViewById(R.id.arrival_time);
        arrival_distance = (TextView) view.findViewById(R.id.arrival_distance);

        drive = (Button) view.findViewById(R.id.drive);
        drive.setOnClickListener(this);

        walk = (Button) view.findViewById(R.id.walk);
        walk.setOnClickListener(this);

        bicycle = (Button) view.findViewById(R.id.bicycle);
        bicycle.setOnClickListener(this);

        transit = (Button) view.findViewById(R.id.transit);
        transit.setOnClickListener(this);

        //TODO load this instead of hard-coded
        lastChanged = transit;

        lastChanged.setBackgroundResource(R.drawable.map_mode_selected);
        lastChanged.setTextColor(Color.WHITE);

        try {
            MapsInitializer.initialize(getActivity().getApplicationContext());
        } catch (Exception e) {
            e.printStackTrace();
        }

        mMapView.getMapAsync(this);
        return view;
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        map = new StrawberryMap(googleMap, this);
        locations = new ArrayList<>();

        for (User friend : StrawberryApplication.getCachedFriends()) {
            Location friendLoc = locationService.getUserLocation(friend);
            locations.add(friendLoc);

            map.updateMarker(friend.getId(), friend.getUsername(), friendLoc);
            Log.d(TAG, "Added " + friend + " as marker at " + friendLoc);
        }

        Location currentLocation = locationService.getDeviceLocation();
        map.updateMarker(PeachServerInterface.currentUser().getId(), "You are here", currentLocation);

        // save starting point of locations
        locations.add(currentLocation);

        refreshPath();
    }

    private void refreshPath() {
        String selectedId = StrawberryApplication.getString(StrawberryApplication.SELECTED_USER_TAG);

        if (selectedId != null) {
            Log.d(TAG, "Tracking " + selectedId);
            map.deleteAllPaths();

            if(!map.updatePath(PeachServerInterface.currentUser().getId(), selectedId)) {
                map.moveCamera(locations, 200);
                Log.e(TAG, "Failed to refresh path");
            }

            Log.i(TAG, "Successfully to refresh path");
        }
    }


    @Override
    public void update(LocationEvent updatedLocation) {
        if (map == null) {
            Log.e(TAG, "Map has not been initialized yet, ditching new location update");
            return;
        }

        User user = updatedLocation.getKey();
        Location currentLocation = updatedLocation.getValue();
        map.updateMarker(user.getId(), user.getUsername(), currentLocation);

        String selectedId = StrawberryApplication.getString(StrawberryApplication.SELECTED_USER_TAG);

        if(selectedId.equals(user.getId()) || selectedId.equals(PeachServerInterface.currentUser().getId()))
            refreshPath();
    }

    @Override
    protected void onResumeAction() {
        mMapView.onResume();
    }

    @Override
    protected void onPauseAction() {
        mMapView.onPause();
    }

    // Change the travel mode.
    @Override
    public void onClick(View view) {
        lastChanged.setBackgroundResource(R.drawable.map_mode_default);
        lastChanged.setTextColor(Color.BLACK);

        switch (view.getId()) {
            // Change the background of the clicked button, and change back the previously changed one
            case R.id.drive:
                lastChanged = drive;
                map.setMode("driving");
                break;

            case R.id.walk:
                lastChanged = walk;
                map.setMode("walking");
                break;

            case R.id.bicycle:
                lastChanged = bicycle;
                map.setMode("bicycling");
                break;

            case R.id.transit:
                lastChanged = transit;
                map.setMode("transit");
                break;

            default:
                Log.e("onClick", "Cannot find any button");
                return;
        }

        lastChanged.setBackgroundResource(R.drawable.map_mode_selected);
        lastChanged.setTextColor(Color.WHITE);

        map.updatePath(PeachServerInterface.currentUser().getId(), StrawberryApplication.getString(StrawberryApplication.SELECTED_USER_TAG));
    }

    // Update the arrival time and distance which will be shown in the textview.
    public void changeText(String name, String value) {
        switch (name) {
            case "distance":
                arrival_distance.setText("The estimated distance is " + value);
                break;

            case "duration":
                arrival_time.setText("The estimated arrival time is " + value);
                break;

            default:
                Log.d("changeText", "The text is changed successfully");
        }
    }
}

package com.comp30022.helium.strawberry.services;

import android.location.Location;

import com.comp30022.helium.strawberry.entities.Friend;
import com.comp30022.helium.strawberry.patterns.Publisher;
import com.comp30022.helium.strawberry.patterns.Subscriber;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import java.util.ArrayList;
import java.util.List;

/**
 * Make sure to call onResume and onPause
 */
public class LocationService implements Publisher<Location>, LocationListener {
    private GoogleApiClient mGoogleApiClient;
    private Location mLastLocation;
    private LocationRequest mLocationRequest;
    private static LocationService instance;
    private static boolean setupCalled = false;
    private static final int INTERVAL_SECS = 10;
    private static final int FASTEST_INTERVAL_SECS = 1;

    private List<Subscriber<Location>> subscribers; // all subscribers here

    public static LocationService getInstance() throws NotInstantiatedException {

        if (instance == null || !setupCalled)
            throw new NotInstantiatedException();
        return instance;
    }

    public void setup(GoogleApiClient mGoogleApiClient) {
        if (setupCalled) {
            return;
        }
        setupCalled = true;
        /* handle initialization here */
        this.mGoogleApiClient = mGoogleApiClient;
        mLocationRequest = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(INTERVAL_SECS * 1000)        // 10 seconds, in milliseconds
                .setFastestInterval(FASTEST_INTERVAL_SECS * 1000); // 1 second, in milliseconds
        this.mGoogleApiClient.connect();
        instance = this;
        subscribers = new ArrayList<>();
    }

    public Location getDeviceLocation() {
        // this method should return this device's current location
        return mLastLocation;
    }

    public LocationRequest getRequest() {
        return mLocationRequest;
    }


    public void onResume() {
        mGoogleApiClient.connect();
    }

    public void onPause() {
        if (mGoogleApiClient.isConnected()) {
            LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
            mGoogleApiClient.disconnect();
        }
    }

    public void setNewLocation(Location location) {
        mLastLocation = location;
    }

    ;

    @Override
    public void onLocationChanged(Location location) {
        setNewLocation(location);
        // Remember to UPDATE database.
        notifyAllSubscribers(location);
    }


    public Location getUserLocation(Friend user) {
        // this method should translate Friend (java Type) into information
        // that the Query language can use
        // to uniquely find the user in the database, then we can return
        // the last known location of this user
        // from the database. (REST calls)
        return null;
    }

    public void registerSubscriber(Subscriber<Location> sub) {
        // this should add the subscriber into its list
        subscribers.add(sub);
    }

    public void deregisterSubscriber(Subscriber<Location> sub) {
        // this should deregister the subscriber from the list
        subscribers.remove(sub);
    }

    private void notifyAllSubscribers(Location location) {
        // this method will call the update() method on all subscribers that are
        // registered.
        for (Subscriber<Location> sub : subscribers) {
            sub.update(location);
        }
    }
}
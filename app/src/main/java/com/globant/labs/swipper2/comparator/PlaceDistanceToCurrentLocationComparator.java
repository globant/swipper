package com.globant.labs.swipper2.comparator;

import com.globant.labs.swipper2.models.Place;
import com.globant.labs.swipper2.utils.GeoUtils;
import com.google.android.gms.maps.model.LatLng;

import java.util.Comparator;

public class PlaceDistanceToCurrentLocationComparator implements Comparator<Place> {

    private LatLng mCurrentLocation;

    @Override
    public int compare(Place placeOne, Place placeTwo) {
        // we assume mCurrentLocation isn't null because we'll get a list of places around
        // oneself only after knowing where we are
        return ((int) (getDistanceTo(placeOne) - getDistanceTo(placeTwo)));
    }

    private double getDistanceTo(Place p) {
        return GeoUtils.getDistance(mCurrentLocation, p.getLocation());
    }

    private void updateCurrentLocation(LatLng currentLocation) {
        mCurrentLocation = currentLocation;
    }
}

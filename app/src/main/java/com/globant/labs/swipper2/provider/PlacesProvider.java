package com.globant.labs.swipper2.provider;

import android.content.Context;

import com.globant.labs.swipper2.SwipperApp;
import com.globant.labs.swipper2.comparator.PlaceDistanceToCurrentLocationComparator;
import com.globant.labs.swipper2.models.Place;
import com.globant.labs.swipper2.repositories.PlaceRepository;
import com.globant.labs.swipper2.utils.GeoUtils;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.strongloop.android.loopback.RestAdapter;
import com.strongloop.android.loopback.callbacks.ListCallback;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class PlacesProvider {

    protected final static int MAX_RETRIES = 2;

    protected PlacesCallback mPlacesWithinBoundsCallback;
    protected PlaceRepository mRepository;
    protected List<Place> mFilteredPlaces;
    protected Set<String> mFilters;
    protected LatLng mCurrentLocation;
    protected LatLngBounds mCurrentBounds;
    protected Comparator<Place> mPlacesComparator;
    protected int mRetriesLeft;
    private PlacesCallback mMorePlacesCallback;
    private Collection<PlaceFiltersUpdatesCallback> mHandlers = new
            ArrayList<PlaceFiltersUpdatesCallback>(2);
    private LoadMoreHandler mLoadMoreHandler;
    private LoadWithinBoundsHandler mLoadWithinBoundsHandler;

    public PlacesProvider(Context context) {
        resetRetries();

        RestAdapter restAdapter = ((SwipperApp) context.getApplicationContext()).getRestAdapter();
        mRepository = restAdapter.createRepository(PlaceRepository.class);
        mFilteredPlaces = new ArrayList<Place>();
        mFilters = new HashSet<String>();

        mLoadWithinBoundsHandler = new LoadWithinBoundsHandler();
        mLoadMoreHandler = new LoadMoreHandler();
        mHandlers.add(mLoadWithinBoundsHandler);
        mHandlers.add(mLoadMoreHandler);

        mPlacesComparator = new PlaceDistanceToCurrentLocationComparator();
    }

    protected void resetRetries() {
        mRetriesLeft = MAX_RETRIES;
    }

    public double getDistanceTo(Place p) {
        return GeoUtils.getDistance(mCurrentLocation, p.getLocation());
    }

    public void setPlacesWithinBoundsCallback(PlacesCallback callback) {
        mPlacesWithinBoundsCallback = callback;
    }

    public void setLoadMorePlacesCallback(PlacesCallback callback) {
        mMorePlacesCallback = callback;
    }

    public boolean updateBounds(LatLngBounds bounds) {
        if (mCurrentBounds == null || !mCurrentBounds.contains(bounds.northeast) ||
                !mCurrentBounds.contains(bounds.southwest)) {

            mCurrentBounds = bounds;
            loadPlacesWithinBounds();

            return false;
        } else {
            return true;
        }
    }

    protected void loadPlacesWithinBounds() {
        if (mCurrentBounds != null) {
            LatLng northWest = new LatLng(mCurrentBounds.northeast.latitude + 0.036,
                    mCurrentBounds.southwest.longitude - 0.036);

            LatLng southEast = new LatLng(mCurrentBounds.southwest.latitude - 0.036,
                    mCurrentBounds.northeast.longitude + 0.036);

            mRepository.nearBy(northWest, southEast, mLoadWithinBoundsHandler);
        }
    }

    public void setFilters(List<String> filters) {
        mFilters.clear();
        for (String filter : filters) {
            mFilters.add(filter);
        }
        refreshFilteredPlaces();
        dispatchPlacesUpdated();
    }

    private void refreshFilteredPlaces() {
        for (PlaceFiltersUpdatesCallback mCallback : mHandlers) {
            mCallback.refreshFilteredPlaces();
        }
    }

    protected void dispatchPlacesUpdated() {
        if (mPlacesWithinBoundsCallback != null) {
            mPlacesWithinBoundsCallback.placesUpdated(getFilteredPlaces());
        }
    }

    public List<Place> getFilteredPlaces() {
        return mFilteredPlaces;
    }

    public void addFilter(String filter) {
        mFilters.add(filter);
        refreshFilteredPlaces();
        dispatchPlacesUpdated();
    }

    public void removeFilter(String filter) {
        mFilters.remove(filter);
        refreshFilteredPlaces();
        dispatchPlacesUpdated();
    }

    public void clearFilters() {
        mFilters.clear();
        refreshFilteredPlaces();
        dispatchPlacesUpdated();
    }

    public int getFilteredPlacesCount() {
        return mFilteredPlaces.size();
    }

    public Place getFilteredPlace(int position) {
        return mFilteredPlaces.get(position);
    }

    public void setCurrentLocation(LatLng currentLocation) {
        mCurrentLocation = currentLocation;
    }

    public interface PlacesCallback {
        public void placesUpdated(List<Place> places);

        public void placesRetry(Throwable t);

        public void placesError(Throwable t);
    }

    private interface PlaceFiltersUpdatesCallback {
        void refreshFilteredPlaces();
    }

    private class LoadWithinBoundsHandler implements ListCallback<Place>,
            PlaceFiltersUpdatesCallback {

        private Multimap<String, Place> mPlacesWithinBounds;

        private LoadWithinBoundsHandler() {
            mPlacesWithinBounds = ArrayListMultimap.create();
        }

        @Override
        public void onSuccess(List<Place> places) {
            resetRetries();
            mPlacesWithinBounds.clear();

            for (Place p : places) {
                mPlacesWithinBounds.put(p.getCategory(), p);
            }

            refreshFilteredPlaces();
            dispatchPlacesUpdated();
        }

        public void refreshFilteredPlaces() {
            mFilteredPlaces.clear();

            if (!mFilters.isEmpty()) {
                for (String filter : mFilters) {
                    mFilteredPlaces.addAll(mPlacesWithinBounds.get(filter));
                }
            } else {
                for (String key : mPlacesWithinBounds.keySet()) {
                    mFilteredPlaces.addAll(mPlacesWithinBounds.get(key));
                }
            }

            Collections.sort(mFilteredPlaces, mPlacesComparator);
        }

        @Override
        public void onError(Throwable t) {
            if (mRetriesLeft > 0) {
                if (mPlacesWithinBoundsCallback != null) {
                    mPlacesWithinBoundsCallback.placesRetry(t);
                }
                mRetriesLeft--;
                loadPlacesWithinBounds();
            } else {
                if (mPlacesWithinBoundsCallback != null) {
                    mPlacesWithinBoundsCallback.placesError(t);
                }
            }
        }


    }

    private class LoadMoreHandler implements ListCallback<Place>, PlaceFiltersUpdatesCallback {

        private void resetRetries() {
            retriesLeft = LOAD_MORE_MAX_RETRIES;
        }

        private void decreaseRetries() {
            retriesLeft--;
        }

        private void loadMorePlaces(int page) {
            if (mCurrentLocation != null) {
                mRepository.storedBy(mCurrentLocation, page, mLoadMoreHandler);
            }
        }

        private static final int LOAD_MORE_MAX_RETRIES = PlacesProvider.MAX_RETRIES;


        private int retriesLeft = MAX_RETRIES;

        @Override
        public void onSuccess(List<Place> objects) {

        }

        @Override
        public void onError(Throwable t) {

        }

        @Override
        public void refreshFilteredPlaces() {

        }


    }
}

package com.globant.labs.swipper2.provider;

import java.util.Comparator;

import android.content.Context;

import com.globant.labs.swipper2.models.Place;

public class ListPlacesProvider extends AbstractPlacesProvider {

	protected int mCurrentPage;
	protected Comparator<Place> mPlacesComparator;
	
	public ListPlacesProvider(Context context) {
		super(context);
		
		mCurrentPage = 0;
		
		mPlacesComparator = new Comparator<Place>() {
			
			@Override
			public int compare(Place p0, Place p1) {
				double d0 = getDistanceTo(p0);
				double d1 = getDistanceTo(p1);
				
				if(d0 < d1) return -1;
				if(d0 > d1) return 1;
				return 0;
			}
			
		};
	}
	
	protected void loadPlaces() {
		if(mCurrentLocation != null) {
			mRepository.storedBy(mCurrentLocation, mCurrentPage, this);
		}
	}
	
	public void loadMore() {
		mCurrentPage++;
		loadPlaces();
	}
		
}

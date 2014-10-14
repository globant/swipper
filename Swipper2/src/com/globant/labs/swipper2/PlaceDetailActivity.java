package com.globant.labs.swipper2;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.globant.labs.swipper2.drawer.CategoryMapper;
import com.globant.labs.swipper2.models.Photo;
import com.globant.labs.swipper2.models.PlaceDetails;
import com.globant.labs.swipper2.repositories.PlaceDetailsRepository;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;
import com.strongloop.android.loopback.RestAdapter;
import com.strongloop.android.loopback.callbacks.ObjectCallback;

public class PlaceDetailActivity extends ActionBarActivity implements ObjectCallback<PlaceDetails>{

	public static final String PHOTOS_API_KEY = "AIzaSyAyeLAbHzmMtrjOO_yVwGYs4Xg7iYbpVdM";
	
	public static final String PLACE_ID_EXTRA = "place-id-extra";
	public static final String PLACE_NAME_EXTRA = "place-name-extra";
	public static final String PLACE_CATEGORY_EXTRA = "place-category-extra";
	public static final String PLACE_DISTANCE_EXTRA = "place-distance-extra";
	
	protected int mCategoryStringId;
	protected int mCategoryMarkerId;
	
	protected RelativeLayout mProgressBarLayout;
	protected LinearLayout mDescriptionLayout;
	protected LinearLayout mScheduleLayout;
	protected LinearLayout mPhotosSection;
	protected LinearLayout mPhotosLayout;
	protected ListView mReviewsList;
	protected TextView mAddressTextView;
	protected TextView mCityTextView;
	protected TextView mDistanceTextView;
	protected TextView mPhoneTextView;
	protected TextView mScheduleTextView;
	protected ImageButton mNavigateButton;
	protected ImageButton mShareButton;
	protected ImageButton mReportButton;
	
	protected PlaceDetails mPlace;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.place_details);
		
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		
		Bundle extras = getIntent().getExtras();
		String placeId = extras.getString(PLACE_ID_EXTRA);
		String placeName = extras.getString(PLACE_NAME_EXTRA);
		String placeCategory = extras.getString(PLACE_CATEGORY_EXTRA);
		double placeDistance = extras.getDouble(PLACE_DISTANCE_EXTRA);
		
		Log.i("SWIPPER", "PlaceId: "+placeId);
		
		mCategoryMarkerId = CategoryMapper.getCategoryMarker(placeCategory);
		mCategoryStringId = CategoryMapper.getCategoryText(placeCategory);
		
		setTitle(placeName);
		
		mAddressTextView = (TextView) findViewById(R.id.addressText);
		mCityTextView = (TextView) findViewById(R.id.cityText);
		mDistanceTextView = (TextView) findViewById(R.id.distanceText);
		mPhoneTextView = (TextView) findViewById(R.id.phoneText);
		mScheduleTextView = (TextView) findViewById(R.id.scheduleText);
		
		mProgressBarLayout = (RelativeLayout) findViewById(R.id.progressBarLayout);
		mDescriptionLayout = (LinearLayout) findViewById(R.id.descriptionLayout);
		mScheduleLayout = (LinearLayout) findViewById(R.id.scheduleLayout);
		mPhotosSection = (LinearLayout) findViewById(R.id.photosSection);
		mPhotosLayout = (LinearLayout) findViewById(R.id.photosLayout);
		
		mReviewsList = (ListView) findViewById(R.id.reviewsList);
		
		mNavigateButton = (ImageButton) findViewById(R.id.navigateButton);
		mShareButton = (ImageButton) findViewById(R.id.shareButton);
		mReportButton = (ImageButton) findViewById(R.id.reportButton);
		
		mNavigateButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				navigateAction();
			}
		});
		
		mShareButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				shareAction();
			}
		});
		
		mReportButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				reportAction();
			}
		});
		 
		RestAdapter restAdapter = ((SwipperApp) getApplication()).getRestAdapter();
		PlaceDetailsRepository placeDetailsRepo = restAdapter.createRepository(PlaceDetailsRepository.class);
		placeDetailsRepo.details(placeId, this);

		DecimalFormat df = new DecimalFormat("0.00");
		mDistanceTextView.setText(df.format(placeDistance)+" km");
		
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuItem itemIcon = menu.add(mCategoryStringId);
		MenuItemCompat.setShowAsAction(itemIcon, MenuItemCompat.SHOW_AS_ACTION_ALWAYS);
		itemIcon.setIcon(mCategoryMarkerId);
		return super.onCreateOptionsMenu(menu);
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		changeSizeTitle();
	}
	
	private void changeSizeTitle() {
	    Typeface typeFace = Typeface.createFromAsset(getAssets(), "fonts/roboto_light.ttf");
	    int actionBarTitleId;
	    
	    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
	    	actionBarTitleId = getResources().getIdentifier("action_bar_title", "id", "android");
	    } else {
	    	actionBarTitleId = R.id.action_bar_title;
	    }
	    
	    TextView titleTextView = (TextView) findViewById(actionBarTitleId);
	    titleTextView.setTypeface(typeFace);
	}
	
	@Override
	public void onSuccess(PlaceDetails placeDetails) {
		
		mPlace = placeDetails;
		
		mAddressTextView.setText(placeDetails.getAddress());
		mCityTextView.setText(placeDetails.getCity()
				+ ", "
				+ placeDetails.getState()
				+ ", "
				+ placeDetails.getCountry());
		
		mPhoneTextView.setText(placeDetails.getPhone());
				
		List<Photo> photos = placeDetails.getPhotos();
		if(photos != null && photos.size() > 0) {
			
			Resources r = getResources();
			int rightMargin = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 4, r.getDisplayMetrics());
			int size = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 100, r.getDisplayMetrics());
			
			LinearLayout.LayoutParams imageLayoutParams = new LinearLayout.LayoutParams(
					size, 
					size);
			
			imageLayoutParams.setMargins(0, 0, rightMargin, 0);
			
			for(Photo photo: photos) {
				final ProgressBar progressBar = new ProgressBar(this, null, android.R.attr.progressBarStyleLarge);
				progressBar.setLayoutParams(imageLayoutParams);
				mPhotosLayout.addView(progressBar);
				
				final ImageView imageView = new ImageView(this);
				imageView.setLayoutParams(imageLayoutParams);
				imageView.setVisibility(View.GONE);
				mPhotosLayout.addView(imageView);
				
				Picasso.with(this)
				  .load(getPhotoURL(photo.getPhoto_reference()))
				  .resize(size, size)
				  .centerCrop()
				  .into(imageView, new Callback() {
	
					@Override
					public void onError() {
						progressBar.setVisibility(View.GONE);
					}
	
					@Override
					public void onSuccess() {
						progressBar.setVisibility(View.GONE);
						imageView.setVisibility(View.VISIBLE);
					}
					  
				  });			
			}
		}else{
			mPhotosSection.setVisibility(View.GONE);
		}
		
		if(placeDetails.getReviews() != null && placeDetails.getReviews().size() > 0) {
			ReviewsAdapter reviewsAdapter = new ReviewsAdapter(this);
			reviewsAdapter.setReviews(placeDetails.getReviews());
			mReviewsList.setAdapter(reviewsAdapter);
		}else{
			TextView emptyText = (TextView)findViewById(android.R.id.empty);
			mReviewsList.setEmptyView(emptyText);		
		}
		
		if(placeDetails.getSchedules() != null) {
			Calendar calendar = Calendar.getInstance();
			int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK) - 1; 
			mScheduleTextView.setText(placeDetails.getSchedules().get(dayOfWeek));
		}else{
			mScheduleLayout.setVisibility(View.GONE);
		}
		
		//mScheduleTextView.setText("10:30 am - 20:30 pm");
		
		mProgressBarLayout.setVisibility(View.GONE);

	}
	
	protected String getPhotoURL(String photoReference) {
		return "https://maps.googleapis.com/maps/api/place/photo" +
				"?maxwidth=300" +
				"&photoreference=" +
				photoReference +
				"&key=" +
				PHOTOS_API_KEY;
	}
	
	@Override
	public void onError(Throwable t) {
		String errorMessage = getResources().getString(R.string.error_place_details_not_available);
		Toast toast = Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT);
		toast.show();
		finish();
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		return super.onOptionsItemSelected(item);
	}
	
	public void navigateAction() {
		String url = "http://maps.google.com/maps?"
						+ "daddr="
						+ mPlace.getLocation().latitude
						+ ","
						+ mPlace.getLocation().longitude;
		
		Intent intent = new Intent(android.content.Intent.ACTION_VIEW,  Uri.parse(url));
		startActivity(intent);
	}
	
	protected void shareAction() {
		StringBuilder stringBuilder = new StringBuilder()
			.append(mPlace.getName())
			.append("\n").append(mPlace.getAddress())
			.append("\n").append(mPlace.getCity())
			.append(", ").append(mPlace.getState())
			.append(", ").append(mPlace.getCountry())
			.append("\n").append(mPlace.getPhone());
		
		if(mPlace.getUrl() != null) {
			stringBuilder.append("\n").append(mPlace.getUrl());
		}
					
		Intent sendIntent = new Intent(Intent.ACTION_SEND);
		sendIntent.setType("text/plain");
		sendIntent.putExtra(Intent.EXTRA_TEXT, stringBuilder.toString());
				
		final PackageManager packageManager = getPackageManager();
		
		List<Intent> targetedShareIntents = new ArrayList<Intent>();
		List<ResolveInfo> resInfo = packageManager.queryIntentActivities(sendIntent, 0);

		Collections.sort(resInfo, new Comparator<ResolveInfo>() {
	        @Override
	        public int compare(ResolveInfo first, ResolveInfo second) {
	            String firstName = first.loadLabel(packageManager).toString();
	            String secondName = second.loadLabel(packageManager).toString();
	            return firstName.compareToIgnoreCase(secondName);
	        }
	    });
		
		for (ResolveInfo resolveInfo : resInfo) {
			String packageName = resolveInfo.activityInfo.packageName;
			String className = resolveInfo.activityInfo.name;
			
			if (mPlace.getUrl() != null || !packageName.equals("com.facebook.katana")) {
				Intent targetedShareIntent = new Intent(Intent.ACTION_SEND);
				targetedShareIntent.setType("text/plain");
				targetedShareIntent.putExtra(Intent.EXTRA_TEXT, stringBuilder.toString());
				targetedShareIntent.setPackage(packageName);
				targetedShareIntent.setClassName(packageName, className);
						
				targetedShareIntents.add(targetedShareIntent);
			}
		}
		
		Intent chooserIntent = Intent.createChooser(targetedShareIntents.remove(targetedShareIntents.size() - 1), 
				getResources().getString(R.string.action_share));
		
		chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, targetedShareIntents.toArray(new Parcelable[]{}));
		
		startActivity(chooserIntent);
	}
	
	protected void reportAction() {
		StringBuilder stringBuilder = new StringBuilder()
			.append(mPlace.getName())
			.append("\n").append(mPlace.getAddress())
			.append("\n").append(mPlace.getCity())
			.append(", ").append(mPlace.getState())
			.append(", ").append(mPlace.getCountry())
			.append("\n").append(mPlace.getPhone());
	
		if(mPlace.getUrl() != null) {
			stringBuilder.append("\n").append(mPlace.getUrl());
		}
		
		stringBuilder.append("\n\n").append("What's the problem?\n...");
		
		Intent emailIntent = new Intent(Intent.ACTION_SENDTO, Uri.fromParts(
	            "mailto","candy.tellez@globant.com", null));
		emailIntent.putExtra(Intent.EXTRA_SUBJECT, "SWIPPER REPORT");
		emailIntent.putExtra(Intent.EXTRA_TEXT, stringBuilder.toString());
		startActivity(Intent.createChooser(emailIntent, getResources().getText(R.string.send_report)));
	}
}

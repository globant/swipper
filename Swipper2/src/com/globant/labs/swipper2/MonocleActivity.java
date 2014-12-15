package com.globant.labs.swipper2;

import java.text.DecimalFormat;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import android.app.Activity;
import android.content.Context;
import android.hardware.Camera;
import android.hardware.Camera.AutoFocusCallback;
import android.hardware.SensorManager;
import android.location.Location;
import android.os.Bundle;
import android.view.ViewGroup;
import android.view.animation.RotateAnimation;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.globant.labs.swipper2.models.Place;
import com.globant.labs.swipper2.provider.PlacesProvider;
import com.globant.labs.swipper2.provider.PlacesProvider.PlacesCallback;
import com.globant.labs.swipper2.utils.GeoUtils;
import com.globant.labs.swipper2.utils.OrientationSensorAccelerometerMagnetic;
import com.globant.labs.swipper2.utils.OrientationSensorGravityMagnetic;
import com.globant.labs.swipper2.utils.OrientationSensorRotationVector;
import com.globant.labs.swipper2.utils.OrientationSensorSamsungDevice;
import com.globant.labs.swipper2.utils.OrientationSensorRotationVector.OnAzimuthChangeListener;
import com.globant.labs.swipper2.widget.CameraPreview;
import com.globant.labs.swipper2.widget.RadarView;
import com.globant.labs.swipper2.widget.RealityView;
import com.globant.labs.swipper2.widget.SwipperTextView;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;

public class MonocleActivity extends Activity
		implements
			AutoFocusCallback,
			ConnectionCallbacks,
			OnConnectionFailedListener,
			LocationListener {

	public static final double DEFAULT_RADIUS = 1000;
	public static final double BASE_COEFICIENT = 1;
	private static final double NORTH_EAST_BEARING = 45;
	private static final double SOUTH_WEST_BEARING = 225;
	private static final int AUTO_FOCUS_INTERVAL_MILLIS = 10000;
	private static final int UPDATE_INTERVAL_MILLIS = 5000;
	private static final int SENSOR_DELAY_RADAR = SensorManager.SENSOR_DELAY_UI;
	private static final int RADAR_PLACES_LAYOUT_DELAY_MILLIS = 100;
	private static final int RADAR_NORTH_LAYOUT_DELAY_MILLIS = 100;
	private static final int REALITY_LAYOUT_DELAY_MILLIS = 75;
	private static final int AZIMUTH_TEXT_VIEW_UPDATE_DELAY_MILLIS = 100;
	private static final int LOADING_STEPS = 3;
	private static final String CAMERA_PREVIEW_TAG = "CameraPreview";
	private static final DecimalFormat DF = new DecimalFormat("0.0000");

	private double mSpeed;
	// private double mAzimuthDegrees;

	private Camera mCamera;
	private CameraPreview mPreview;
	private AutoFocusCallback mAutoFocusCallback;
	private ScheduledExecutorService mAutoFocusService;
	private boolean mStopped;
	private boolean mFocusing;
	private boolean mFocusAuto = false;

	// private SwipperTextView mBrand;
	private RelativeLayout mPreviewFrame;
	// private ImageView mRadarBackground;
	private RadarView mRadarPlaces;
	private RealityView mReality;
	private ProgressBar mLoadingProgressBar;
	private TextView mAzimuthTextView;
	private SwipperTextView mRadarNorthTextView;

	private GoogleApiClient mGoogleApiClient;
	private LocationRequest mLocationRequest;
	private Location mCurrentLocation;
	private Location mPreviousLocation;

	private MonoclePlacesProvider mPlacesProvider;
	private OrientationProvider mOrientationProvider;
	private LocationListener mOrientationSensorLocationListener;
	private SensorManager mSensorManager;

	// private float mRadarBackgroundCenter;
	private float mRadarNorthCenter;
	// private RotateAnimation mRadarBackgroundRotateAnimation;
	private RotateAnimation mRadarNorthRotateAnimation;

	private int mLoadingSteps;

	// private double mAzimuthRaw;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_monocle);

		// Set up the google api client
		mGoogleApiClient = new GoogleApiClient.Builder(this)
				.addApi(LocationServices.API).addConnectionCallbacks(this)
				.addOnConnectionFailedListener(this).build();

		// get some view handles
		mReality = (RealityView) findViewById(R.id.reality_monocle);
		mRadarPlaces = (RadarView) findViewById(R.id.radar_monocle_places);
		// mRadarBackground = (ImageView)
		// findViewById(R.id.radar_monocle_background);
		mLoadingProgressBar = (ProgressBar) findViewById(R.id.loading_monocle);
		mAzimuthTextView = (TextView) findViewById(R.id.azimuth_monocle);
		mRadarNorthTextView = (SwipperTextView) findViewById(R.id.north_monocle);

		// Create a new global location parameters object
		mLocationRequest = LocationRequest.create();

		// Set the update interval
		mLocationRequest.setInterval(UPDATE_INTERVAL_MILLIS);

		// Use high accuracy
		mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

		// Set up places provider (use our own tweaked version)
		mPlacesProvider = new MonoclePlacesProvider(this);
		mPlacesProvider.refreshFilteredPlaces();
		mPlacesProvider.setPlacesCallback(new PlacesCallback() {

			@Override
			public void placesUpdated(List<Place> places) {
				mRadarPlaces.onPlacesUpdate(places);
				mReality.onPlacesUpdate(places);
				if (mLoadingSteps <= LOADING_STEPS)
					onLoadingStep();
			}

			@Override
			public void placesRetry(Throwable t) {
			}

			@Override
			public void placesError(Throwable t) {
			}
		});

		// Set up sensor manager
		// setAzimuthDegrees(0);
		mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
		// mOrientationProvider = new OrientationSensor(mSensorManager);
		mOrientationProvider = new OrientationSensorAccelerometerMagnetic(
				mSensorManager);
		mOrientationSensorLocationListener = mOrientationProvider;

		// Set max progress
		mLoadingProgressBar.setMax(LOADING_STEPS);
	}

	@Override
	protected void onStart() {
		mGoogleApiClient.connect();
		super.onStart();
	}

	@Override
	protected void onResume() {
		mLoadingSteps = 0;
		setUpCamera();
		mOrientationProvider.Register(null, SENSOR_DELAY_RADAR);
		// mRadarBackgroundCenter =
		// getResources().getDimension(R.dimen.radar_monocle_size) / 2;
		mRadarNorthCenter = getResources().getDimension(
				R.dimen.radar_north_monocle_size) / 2;
		setUpLayoutRefreshers();
		super.onResume();
	}

	@Override
	protected void onPause() {
		mOrientationProvider.Unregister();
		stopAutoFocus();
		releaseCamera(); // release the camera immediately on pause event
		super.onPause();
	}

	@Override
	protected void onStop() {
		mGoogleApiClient.disconnect();
		super.onStop();
	}

	private void onLoadingStep() {
		mLoadingSteps++;
		mLoadingProgressBar.setProgress(mLoadingSteps);
		if (mLoadingSteps == LOADING_STEPS) {
			hideLoadingView();
		}
	}

	private void hideLoadingView() {
		ViewGroup content = (ViewGroup) ((ViewGroup) findViewById(android.R.id.content))
				.getChildAt(0);
		content.removeView(content.findViewById(R.id.black_background_monocle));
		content.removeView(content.findViewById(R.id.loading_monocle));
	}

	private void setUpLayoutRefreshers() {
		Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(
				new Runnable() {
					public void run() {
						runOnUiThread(new Runnable() {

							@Override
							public void run() {
								mRadarPlaces.requestLayout();
							}
						});
					}
				}, 0, RADAR_PLACES_LAYOUT_DELAY_MILLIS, TimeUnit.MILLISECONDS);

		Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(
				new Runnable() {
					public void run() {
						runOnUiThread(new Runnable() {

							@Override
							public void run() {
								rotateRadarNorth((float) getAzimuthDegrees());
							}
						});
					}
				}, 0, RADAR_NORTH_LAYOUT_DELAY_MILLIS, TimeUnit.MILLISECONDS);

		Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(
				new Runnable() {
					public void run() {
						runOnUiThread(new Runnable() {

							@Override
							public void run() {
								mReality.requestLayout();
							}
						});
					}
				}, 0, REALITY_LAYOUT_DELAY_MILLIS, TimeUnit.MILLISECONDS);

		Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(
				new Runnable() {
					public void run() {
						runOnUiThread(new Runnable() {

							@Override
							public void run() {
								runOnUiThread(new Runnable() {

									@Override
									public void run() {
										mAzimuthTextView.setText(DF
												.format(mOrientationProvider
														.getAzimuthRadians()));
									}
								});
							}
						});
					}
				}, 0, AZIMUTH_TEXT_VIEW_UPDATE_DELAY_MILLIS,
				TimeUnit.MILLISECONDS);
	}

	private void rotateRadarNorth(float angle) {
		mRadarNorthRotateAnimation = new RotateAnimation(-angle, -angle,
				mRadarNorthCenter, mRadarNorthCenter);
		mRadarNorthRotateAnimation.setFillAfter(true);
		mRadarNorthTextView.startAnimation(mRadarNorthRotateAnimation);
	}

	private void setUpCamera() {
		mPreviewFrame = (RelativeLayout) findViewById(R.id.camera_preview);
		mPreviewFrame.removeView(mPreviewFrame
				.findViewWithTag(CAMERA_PREVIEW_TAG));

		// Create an instance of Camera
		mCamera = getCameraInstance();

		// Create our Preview view and set it as the content of our activity.
		mPreview = new CameraPreview(this, mCamera);
		mPreview.setTag(CAMERA_PREVIEW_TAG);
		mPreviewFrame.addView(mPreview, 0);

		checkAndEnableAutoFocus();

		onLoadingStep();
	}

	private void checkAndEnableAutoFocus() {
		// get Camera parameters
		Camera.Parameters params = mCamera.getParameters();

		List<String> focusModes = params.getSupportedFocusModes();
		if (focusModes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO)) {
			// Continuous focus mode is supported
			// set the focus mode
			params.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
			// set Camera parameters
			mCamera.setParameters(params);
		} else if (focusModes.contains(Camera.Parameters.FOCUS_MODE_AUTO)) {
			// Autofocus mode is supported
			// set the focus mode
			params.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
			// set Camera parameters
			mCamera.setParameters(params);
			startAutoFocus();
			mFocusAuto = true;
		}
	}

	private void releaseCamera() {
		if (mCamera != null) {
			mCamera.stopPreview();
			mCamera.setPreviewCallback(null);
			mPreview.getHolder().removeCallback(mPreview);
			mCamera.release(); // release the camera for other applications
			mCamera = null;
		}
	}

	/** A safe way to get an instance of the Camera object. */
	public static Camera getCameraInstance() {
		Camera c = null;
		try {
			c = Camera.open(); // attempt to get a Camera instance
		} catch (Exception e) {
			// Camera is not available (in use or does not exist)
		}
		return c; // returns null if camera is unavailable
	}

	private void startAutoFocus() {

		mStopped = false;

		mAutoFocusCallback = this;

		mAutoFocusService = Executors.newSingleThreadScheduledExecutor();

		mAutoFocusService.scheduleAtFixedRate(new Runnable() {
			public void run() {
				try {
					if (!mStopped && !mFocusing) {
						mFocusing = true;
						mCamera.autoFocus(mAutoFocusCallback);
					}
				} catch (Exception e) {
					mFocusing = false;
				}
			}
		}, 0, AUTO_FOCUS_INTERVAL_MILLIS, TimeUnit.MILLISECONDS);
	}

	private void stopAutoFocus() {
		mStopped = true;
		shutdownAutoFocusService();
		// Doesn't hurt to call this even if not focusing
		try {
			mCamera.cancelAutoFocus();
		} catch (RuntimeException re) {
			// Have heard RuntimeException reported in Android 4.0.x+; continue?
		}
	}

	private synchronized void shutdownAutoFocusService() {
		if (mFocusAuto) {
			mAutoFocusService.shutdown();
		}
		mFocusing = false;
	}

	@Override
	public void onAutoFocus(boolean success, Camera camera) {
		mFocusing = false;
	}

	@Override
	public void onConnected(Bundle connectionHint) {
		setCurrentLocation(LocationServices.FusedLocationApi
				.getLastLocation(mGoogleApiClient));
		mRadarPlaces.onLocationChanged(getCurrentLocation());
		mReality.onLocationChanged(getCurrentLocation());
		mOrientationSensorLocationListener
				.onLocationChanged(getCurrentLocation());
		LocationServices.FusedLocationApi.requestLocationUpdates(
				mGoogleApiClient, mLocationRequest, this);
		onLoadingStep();
	}

	@Override
	public void onConnectionSuspended(int cause) {
	}

	@Override
	public void onConnectionFailed(ConnectionResult result) {
	}

	@Override
	public void onLocationChanged(Location location) {
		setCurrentLocation(location);
		setSpeed();
		mRadarPlaces.onLocationChanged(location);
		mReality.onLocationChanged(location);
		mOrientationSensorLocationListener
				.onLocationChanged(getCurrentLocation());
		mPlacesProvider.setCurrentLocation(new LatLng(location.getLatitude(),
				location.getLongitude()));
		if (mPlacesProvider.updateLocation(getBounds(BASE_COEFICIENT))) {
			mRadarPlaces.onPlacesUpdate(mPlacesProvider.getFilteredPlaces());
			mReality.onPlacesUpdate(mPlacesProvider.getFilteredPlaces());
		}
	}

	private double getSpeed() {
		return mSpeed;
	}

	private void setSpeed() {
		if (getCurrentLocation().hasSpeed()) {
			mSpeed = getCurrentLocation().getSpeed();
		} else {
			if (getPreviousLocation() != null) {
				double distance = getCurrentLocation().distanceTo(
						mPreviousLocation);
				double timeDiff = getCurrentLocation().getTime()
						- getPreviousLocation().getTime();
				mSpeed = distance / timeDiff;
			} else {
				mSpeed = 0;
			}
		}
	}

	private void setCurrentLocation(Location location) {
		setPreviousLocation(mCurrentLocation);
		this.mCurrentLocation = location;
	}

	public Location getCurrentLocation() {
		return mCurrentLocation;
	}

	public LatLng getCurrentLatLng() {
		return new LatLng(mCurrentLocation.getLatitude(),
				mCurrentLocation.getLongitude());
	}

	private Location getPreviousLocation() {
		return mPreviousLocation;
	}

	public void setPreviousLocation(Location mPreviousLocation) {
		this.mPreviousLocation = mPreviousLocation;
	}

	public LatLngBounds getBounds(double baseCoeficient) {
		// Set the bounds of the radar dinamically, according to speed
		// The idea is that between each update, the points do not get
		// completely replaced, but just displaced halfway, instead.
		double radius = getRadius(baseCoeficient);
		LatLng currentLatLng = new LatLng(getCurrentLocation().getLatitude(),
				getCurrentLocation().getLongitude());
		LatLng northEastLatLng = GeoUtils.displaceLatLng(currentLatLng, radius,
				NORTH_EAST_BEARING);
		LatLng southWestLatLng = GeoUtils.displaceLatLng(currentLatLng, radius,
				SOUTH_WEST_BEARING);
		return LatLngBounds.builder().include(northEastLatLng)
				.include(southWestLatLng).build();
	}

	public double getRadius(double baseCoeficient) {
		double speedMultiplier = baseCoeficient + (getSpeed() / DEFAULT_RADIUS);
		return DEFAULT_RADIUS * speedMultiplier;
	}

	private class MonoclePlacesProvider extends PlacesProvider {

		public MonoclePlacesProvider(Context context) {
			super(context);
		}

		@Override
		public void onError(Throwable t) {
			// this way we have an even harder worker provider. it doesn't
			// accept a no as an answer, it keeps working until it achieves it's
			// goal, or it dies
			loadPlaces();
		}
	}

	public double getAzimuthDegrees() {
		return mOrientationProvider.getAzimuthDegrees();
	}

	public interface OrientationProvider extends LocationListener {
		boolean Register(OnAzimuthChangeListener[] listeners, int sensorSpeed);
		void Unregister();
		double getAzimuthDegrees();
		double getAzimuthRadians();
	}
}

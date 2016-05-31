package com.asamm.locus.addon.smartwatch2.gui;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;

import com.asamm.locus.addon.smartwatch2.R;
import com.sonyericsson.extras.liveware.aef.control.Control;
import com.sonyericsson.extras.liveware.extension.util.ExtensionUtils;
import com.sonyericsson.extras.liveware.extension.util.control.ControlExtension;
import com.sonyericsson.extras.liveware.extension.util.control.ControlListItem;
import com.sonyericsson.extras.liveware.extension.util.control.ControlObjectClickEvent;
import com.sonyericsson.extras.liveware.extension.util.control.ControlTouchEvent;
import com.sonyericsson.extras.liveware.extension.util.control.ControlView;
import com.sonyericsson.extras.liveware.extension.util.control.ControlViewGroup;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import locus.api.android.ActionTools;
import locus.api.android.ActionTools.BitmapLoadResult;
import locus.api.android.features.periodicUpdates.UpdateContainer;
import locus.api.android.utils.LocusInfo;
import locus.api.android.utils.LocusUtils;
import locus.api.android.utils.LocusUtils.LocusVersion;
import locus.api.android.utils.UtilsFormat;
import locus.api.android.utils.exceptions.RequiredVersionMissingException;
import locus.api.objects.extra.Location;
import locus.api.utils.Logger;

/**
 * The sample control for SmartWatch handles the control on the accessory. This
 * class exists in one instance for every supported host application that we
 * have registered to
 */
public class ControlSmartWatch2 extends ControlExtension {

    // tag for logger
	private static final String TAG = "ControlSmartWatch2";

    // minimal version of Locus Map
    private static final int MIN_LOCUS_MAP_VERSION = 410;

    // registered instance
    private static ControlSmartWatch2 mInstance;

    /**
     * Return current active instance.
     * @return instance of control class
     */
    public static ControlSmartWatch2 getInstance() {
        return mInstance;
    }

    // keys for preferences
    private static final String KEY_I_LAST_SCREEN_MODE = "KEY_I_LAST_SCREEN_MODE";
    private static final String KEY_I_LAST_SCREEN_GUIDANCE_MODE = "KEY_I_LAST_SCREEN_GUIDANCE_MODE";

    // menu handler
    private static final int MENU_ITEM_0 = 0;
    private static final int MENU_ITEM_1 = 1;

    // paint for map resize
    private Paint mMapPaint;
    // scale for map preview
    private static final float MAP_SCALE = 2.5f;

    // format for time formatting
    public static final SimpleDateFormat TIME_FORMAT =
			new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
    static {
        TIME_FORMAT.setTimeZone(TimeZone.getTimeZone("UTC+0"));
    }

    public enum ScreenMode {
    	GUIDING, TRACK_RECORD
    }
    
	public enum ScreenGuidingImageMode {
		COMPASS, MAP
	}
	
	public enum StartTestResult {
		NO_PROBLEM,
		LOCUS_INVALID,
	}

    // current context
    private Context mContext;
    // inflater for UI
    private LayoutInflater mInflater;
    // reference to shared preferences
    private SharedPreferences mPrefs;

	// Last set layout
    private int mLastSetLayout;
    // Last received update from Locus
    private UpdateContainer mLastUpdate;
    // list of defined profiles
    private List<ActionTools.TrackRecordProfileSimple> mTrackRecProfiles;

    // current screen mode
    private ScreenMode mScreenMode;
    // current image mode
    private ScreenGuidingImageMode mGuidingMode;
    
    // width of image view
    private int mImageWidth;
    // height of image view
    private int mImageHeight;
    
    // Compass layout
    private CompassGenerator mCompassGenerator;
    // time of last map refresh
    private long mLastMapRefresh = 0L;

    // handler for controllers
    private ControlViewGroup mCvStartScreen;
    private ControlViewGroup mCvGuiding;
    private ControlViewGroup mCvTrackRecRunning;
    
    // result of start tests
    private StartTestResult mStartResult;
    // current locus info object
    private LocusInfo mLocusInfo;
    private LocusVersion mLocusVersion;

    // list of icons
    private Bundle[] mMenuItemsIcons = new Bundle[2];

    // flag if system is running
    private boolean mRunning;
    
    /**
     * Create sample control.
     *
     * @param hostAppPackageName Package name of host application.
     * @param context The context.
     * @param handler The handler to use
     */
    public ControlSmartWatch2(String hostAppPackageName, Context context, Handler handler) {
        super(context, hostAppPackageName);
        Logger.logD(TAG, "ControlSmartWatch2(" + hostAppPackageName + ", " + context + ", " + handler + ")");
        this.mContext = context;
        this.mInflater = (LayoutInflater) mContext.getSystemService
                (Context.LAYOUT_INFLATER_SERVICE);
        this.mPrefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        this.mRunning = true;
        if (handler == null) {
            throw new IllegalArgumentException("handler == null");
        }
        
        // basic initialization
        mCvStartScreen = createControlStartScreen();
        mCvGuiding = createControlGuiding();
        mCvTrackRecRunning = createControlTrackRecordRunning();
        initializeMenus();
        
        // prepare views for generating
        int screenModeValue = mPrefs.getInt(
                KEY_I_LAST_SCREEN_MODE, ScreenMode.GUIDING.ordinal());
        mScreenMode = ScreenMode.values()[screenModeValue];
        int screenGuidingModeValue = mPrefs.getInt(
                KEY_I_LAST_SCREEN_GUIDANCE_MODE, ScreenGuidingImageMode.MAP.ordinal());
        mGuidingMode = ScreenGuidingImageMode.values()[screenGuidingModeValue];
        
        // prepare UI
        Resources res = mContext.getResources();
        mImageWidth = (int) res.getDimension(R.dimen.view_image_width);
        mImageHeight = (int) res.getDimension(R.dimen.view_image_height);
        mCompassGenerator = new CompassGenerator(
        		mImageWidth, mImageHeight);

        // perform check and start automatic checker
        performLocusCheck();

        // class for periodic checks.
        Runnable mChecker = new Runnable() {

            @Override
            public void run() {
                try {
                    // repeat actions till system is running
                    while (mRunning) {
                        Thread.sleep(mLocusVersion == null ? 1000 : 5000);

                        // perform update
                        performLocusCheck();
                    }
                } catch (Exception e) {
                    Logger.logE(TAG, "mChecker, run()", e);
                }
            }
        };
        new Thread(mChecker).start();
    }

    /**
     * Perform one time, single check and update of basic layout.
     */
    private void performLocusCheck() {
        try {
            // read Locus info
            mLocusVersion = LocusUtils.getActiveVersion(mContext, MIN_LOCUS_MAP_VERSION);

            // check if object exists
            if (mLocusVersion == null) {
                mStartResult = StartTestResult.LOCUS_INVALID;
                return;
            }

            // handle info
            mLocusInfo = ActionTools.getLocusInfo(mContext, mLocusVersion);

            // load also track record profiles
            mTrackRecProfiles = ActionTools.getTrackRecordingProfiles(
                    mContext, mLocusVersion);

            // set state of Locus
            mStartResult = StartTestResult.NO_PROBLEM;
        } catch (RequiredVersionMissingException e) {
            Logger.logE(TAG, "ControlSmartWatch2()", e);

            // clear data
            mLocusVersion = null;
            mStartResult = StartTestResult.LOCUS_INVALID;
        }

        // refresh layout
        refreshData();
    }
    
    // BASIC INITIALIZATION

    /**
     * Prepare main menu.
     */
    private void initializeMenus() {
        mMenuItemsIcons[0] = new Bundle();
        mMenuItemsIcons[0].putInt(
        		Control.Intents.EXTRA_MENU_ITEM_ID, MENU_ITEM_0);
        mMenuItemsIcons[0].putString(
        		Control.Intents.EXTRA_MENU_ITEM_ICON,
                ExtensionUtils.getUriString(mContext, R.drawable.ic_guide_default));
        mMenuItemsIcons[1] = new Bundle();
        mMenuItemsIcons[1].putInt(
        		Control.Intents.EXTRA_MENU_ITEM_ID, MENU_ITEM_1);
        mMenuItemsIcons[1].putString(
        		Control.Intents.EXTRA_MENU_ITEM_ICON,
                ExtensionUtils.getUriString(mContext, R.drawable.ic_track_record_default));
    }

    /**
     * Get supported control width.
     *
     * @param context The context.
     * @return the width.
     */
    public static int getSupportedControlWidth(Context context) {
        return context.getResources().getDimensionPixelSize(R.dimen.smart_watch_2_control_width);
    }

    /**
     * Get supported control height.
     *
     * @param context The context.
     * @return the height.
     */
    public static int getSupportedControlHeight(Context context) {
        return context.getResources().getDimensionPixelSize(R.dimen.smart_watch_2_control_height);
    }

    // SYSTEM EVENTS
    
    @Override
    public void onStart() {
        super.onStart();
        Logger.logD(TAG, "onStart()");

        // store current instance
        mInstance = this;

        // enable receiver
        PeriodicUpdatesReceiver.enableReceiver(mContext);
    }

    @Override
    public void onResume() {
        super.onResume();
        Logger.logD(TAG, "onResume()");

        // refresh content
        refreshData();
        
        // turn screen on permanently
        setScreenState(Control.Intents.SCREEN_STATE_ON);
    }
    
    @Override
    public void onPause() {
        super.onPause();
        Logger.logD(TAG, "onPause()");
    }
    
    @Override
    public void onStop() {
        super.onStop();
        Logger.logD(TAG, "onStop()");

        // disable receiver
        PeriodicUpdatesReceiver.disableReceiver(mContext);

        // clear instance
        mInstance = null;

        // disable screen state
    	setScreenState(Control.Intents.SCREEN_STATE_AUTO);

        // store current values
        SharedPreferences.Editor editor = mPrefs.edit();
        editor.putInt(KEY_I_LAST_SCREEN_MODE, mScreenMode.ordinal());
        editor.putInt(KEY_I_LAST_SCREEN_GUIDANCE_MODE, mGuidingMode.ordinal());
        editor.commit();
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
    	Logger.logD(TAG, "onDestroy()");
        mRunning = false;
    }
    
    // SYSTEM EVENTS
    
    @Override
    public void onTouch(final ControlTouchEvent event) {
        Logger.logD(TAG, "onTouch() " + event.getAction());
    }

    @Override
    public void onObjectClick(final ControlObjectClickEvent event) {
        Logger.logD(TAG, "onObjectClick() " + event.getClickType());
        int layoutRef = event.getLayoutReference();
        if (layoutRef != -1) {
            if (mLastSetLayout == R.layout.screen_empty_with_button) {
                mCvStartScreen.onClick(layoutRef);
            } else if (mLastSetLayout == R.layout.screen_guidance) {
        		mCvGuiding.onClick(layoutRef);
        	} else if (mLastSetLayout == R.layout.screen_track_record_running) {
        		mCvTrackRecRunning.onClick(layoutRef);
        	}
        }
    }

    @Override
    public void onKey(final int action, final int keyCode, final long timeStamp) {
        Logger.logD(TAG, "onKey()");
        if (action == Control.Intents.KEY_ACTION_RELEASE
                && keyCode == Control.KeyCodes.KEYCODE_OPTIONS) {
            showMenu(mMenuItemsIcons);
        } else if (action == Control.Intents.KEY_ACTION_RELEASE
                && keyCode == Control.KeyCodes.KEYCODE_BACK) {
        	Logger.logD(TAG, "onKey() - back button intercepted.");
        }
    }

    @Override
    public void onMenuItemSelected(final int menuItem) {
        Logger.logD(TAG, "onMenuItemSelected(" + menuItem + ")");
        switch (menuItem) {
		case MENU_ITEM_0:
			if (mScreenMode != ScreenMode.GUIDING) {
				mScreenMode = ScreenMode.GUIDING;
				refreshData();
			}
			break;
		case MENU_ITEM_1:
			if (mScreenMode != ScreenMode.TRACK_RECORD) {
				mScreenMode = ScreenMode.TRACK_RECORD;
				refreshData();
			}
			break;
		}
    }
    
    // PERIODIC UPDATES

    /**
     * Update content with fresh updates.
     * @param update update container
     */
	protected void onUpdate(UpdateContainer update) {
		mLastUpdate = update;
		refreshData();
	}

    /**
     * Notify about incorrect data.
     */
    protected void onIncorrectData() {
        Logger.logW(TAG, "onIncorrectData()");
		mLastUpdate = null;
		refreshData();
	}
    
    /**************************************************/
    // UI HANDLING
    /**************************************************/

    /**
     * Refresh content of screen based on received parameters.
     */
    private void refreshData() {
    	// check data
    	if (mStartResult != StartTestResult.NO_PROBLEM ||
                mLocusInfo == null ||
                !mLocusInfo.isRunning() ||
                !mLocusInfo.isPeriodicUpdatesEnabled() ||
                mLastUpdate == null) {
            refreshEmptyLayout();
    	} else if (mScreenMode == ScreenMode.GUIDING) {
            refreshGuidanceLayout();
    	} else if (mScreenMode == ScreenMode.TRACK_RECORD) {
            refreshTrackRecordingLayout();
    	}
    }

    /**
     * Set certain layout to be an active.
     * @param layoutRes resource of layout
     * @return <code>true</code> if new layout was set
     */
    private boolean setLayout(int layoutRes) {
        if (mLastSetLayout != layoutRes) {
            showLayout(layoutRes, null);
            mLastSetLayout = layoutRes;
            return true;
        }
        return false;
    }

    /**************************************************/
    // EMPTY LAYOUT
    /**************************************************/

    /**
     * Refresh content of 'empty layout' - layout with info message only.
     */
    private void refreshEmptyLayout() {
    	String infoText;
        boolean showOpenLocus = false;
    	if (mStartResult == StartTestResult.LOCUS_INVALID || mLocusInfo == null) {
    		infoText = mContext.getString(R.string.invalid_locus_version);
        } else if (!mLocusInfo.isRunning()) {
            infoText = mContext.getString(R.string.locus_not_running);
            showOpenLocus = true;
        } else if (!mLocusInfo.isPeriodicUpdatesEnabled()) {
            infoText = mContext.getString(R.string.periodic_updates_not_enabled);
    	} else if (mLastUpdate == null) {
    		infoText = mContext.getString(R.string.waiting_on_new_data);
    	} else {
    		infoText = mContext.getString(R.string.unknown_problem);
    	}
    	
    	// set correct layout
        refreshEmptyLayout(infoText, showOpenLocus);
    }

    /**
     * Display screen with any simple information.
     * @param infoText info text
     */
    private void refreshEmptyLayout(String infoText, boolean showOpenLocus) {
        // set correct layout
        if (!showOpenLocus) {
            setLayout(R.layout.screen_empty);
        } else {
            setLayout(R.layout.screen_empty_with_button);
        }

        // refresh text
        sendText(R.id.text_view_info, infoText);
    }

    /**************************************************/
    // GUIDANCE
    /**************************************************/

    /**
     * Refresh content of 'Guiding layout'.
     */
    private void refreshGuidanceLayout() {
    	// set correct layout
    	setLayout(R.layout.screen_guidance);
    	
    	// draw image view
    	Bitmap img = null;
        UpdateContainer.GuideTypeWaypoint gtWpt = mLastUpdate.getGuideTypeWaypoint();
        UpdateContainer.GuideTypeTrack gcTrk = mLastUpdate.getGuideTypeTrack();

        // handle parameters
    	if (mGuidingMode == ScreenGuidingImageMode.COMPASS) {
            if (gtWpt != null) {
                img = mCompassGenerator.render(gtWpt.getTargetAngle());
            } else if (gcTrk != null) {
                img = mCompassGenerator.render(gcTrk.getTargetAngle());
            } else {
                img = mCompassGenerator.render(0);
            }
    	} else if (mGuidingMode == ScreenGuidingImageMode.MAP) {
        	// attempt for a map
        	BitmapLoadResult loadedMap;
        	try {
            	// limit refresh
            	if (System.currentTimeMillis() - mLastMapRefresh > 2500) {
            		mLastMapRefresh = System.currentTimeMillis();
//                    Location loc = mLastUpdate.getLocMyLocation() != null ?
//                            mLastUpdate.getLocMyLocation() : mLastUpdate.getLocMapCenter();
                    // define invalid location to get centered map (TODO require 3.12.0+ Locus)
                    Location loc = new Location("", 0.0, 0.0);
            		loadedMap = ActionTools.getMapPreview(mContext, mLocusVersion, loc,
        					mLastUpdate.getMapZoomLevel(),
                            (int) (MAP_SCALE * mImageWidth),
                            (int) (MAP_SCALE * mImageHeight), false);
            		if (loadedMap.isValid()) {
            			img = loadedMap.getImage();
                        img = getCorrectImageSize(img);
            		}
            	}
    		} catch (RequiredVersionMissingException e) {
    			e.printStackTrace();
    		}
    	}
    	
    	// redraw image view
    	if (img != null) {
        	sendImage(R.id.image_view_main, img);
    	}
    	
    	// update text content
    	if (gtWpt != null) {
            sendText(R.id.text_view_screen_title,
                    gtWpt.getTargetName());
            sendText(R.id.text_view_info_01_title,
                    mContext.getString(R.string.distance));
            sendText(R.id.text_view_info_01, UtilsFormat.formatDistance(
                    mLocusInfo.getUnitsFormatLength(), gtWpt.getTargetDist(), false));
            sendText(R.id.text_view_info_02_title,
                    mContext.getString(R.string.time));
            sendText(R.id.text_view_info_02,
                    TIME_FORMAT.format(gtWpt.getTargetTime()));
            sendText(R.id.text_view_info_03_title,
                    mContext.getString(R.string.azimuth));
            sendText(R.id.text_view_info_03, UtilsFormat.formatAngle(
                    mLocusInfo.getUnitsFormatAngle(), gtWpt.getTargetAzim(), true, 0));
        } else if (gcTrk != null) {
            sendText(R.id.text_view_screen_title,
                    gcTrk.getTargetName());
            if (mLastUpdate.getGuideType() == UpdateContainer.GUIDE_TYPE_TRACK_GUIDE) {
                sendText(R.id.text_view_info_01_title,
                        mContext.getString(R.string.distance));
                sendText(R.id.text_view_info_01, UtilsFormat.formatDistance(
                        mLocusInfo.getUnitsFormatLength(), gcTrk.getTargetDist(), false));
                sendText(R.id.text_view_info_02_title,
                        mContext.getString(R.string.time));
                sendText(R.id.text_view_info_02,
                        TIME_FORMAT.format(gcTrk.getTargetTime()));
                sendText(R.id.text_view_info_03_title,
                        mContext.getString(R.string.azimuth));
                sendText(R.id.text_view_info_03, UtilsFormat.formatAngle(
                        mLocusInfo.getUnitsFormatAngle(), gcTrk.getTargetAzim(), true, 0));
            } else if (mLastUpdate.getGuideType() == UpdateContainer.GUIDE_TYPE_TRACK_NAVIGATION) {
                sendText(R.id.text_view_info_01_title,
                        mContext.getString(R.string.navigation_command));
                sendText(R.id.text_view_info_01, UtilsFormat.formatDistance(
                        mLocusInfo.getUnitsFormatLength(), gcTrk.getNavPoint1Dist(), false));
                sendText(R.id.text_view_info_02_title,
                        mContext.getString(R.string.distance));
                sendText(R.id.text_view_info_02, UtilsFormat.formatDistance(
                        mLocusInfo.getUnitsFormatLength(), gcTrk.getDistToFinish(), false));
                sendText(R.id.text_view_info_03_title,
                        mContext.getString(R.string.time));
                sendText(R.id.text_view_info_03,
                        TIME_FORMAT.format(gcTrk.getTimeToFinish()));
            }
        } else {
    		sendText(R.id.text_view_screen_title,
        			mContext.getString(R.string.no_target));
            sendText(R.id.text_view_info_01_title,
                    "");
            sendText(R.id.text_view_info_01,
                    "--");
            sendText(R.id.text_view_info_02_title,
                    "");
            sendText(R.id.text_view_info_02,
                    "--");
            sendText(R.id.text_view_info_03_title,
                    "");
            sendText(R.id.text_view_info_03,
                    "--");
    	}
    }

    /**
     * Create smaller version of map (optimal for device).
     * @param img source image
     * @return improved map preview
     */
    private Bitmap getCorrectImageSize(Bitmap img) {
        // check img data
        if (img == null || img.getWidth() == 0 || img.getHeight() == 0) {
            return img;
        }

        // check paint
        if (mMapPaint == null) {
            mMapPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            mMapPaint.setFilterBitmap(true);
        }

        // perform resize
        Bitmap imgNew = Bitmap.createBitmap(
                mImageWidth, mImageHeight, Bitmap.Config.ARGB_8888);
        Canvas cNew = new Canvas(imgNew);
        Matrix mScale = new Matrix();
        mScale.setScale(1.0f / MAP_SCALE, 1.0f / MAP_SCALE);

        // finally draw image
        cNew.drawBitmap(img, mScale, mMapPaint);

        // return map view
        return imgNew;
    }

    /**************************************************/
    // TRACK RECORDING
    /**************************************************/

    /**
     * Refresh content of 'Track recording' layout.
     */
    private void refreshTrackRecordingLayout() {
        if (!mLastUpdate.isTrackRecRecording()) {
            // check existence of track record profiles
            if (mTrackRecProfiles == null || mTrackRecProfiles.size() == 0) {
                refreshEmptyLayout(mContext.getString(R.string.track_record_no_profiles), false);
            } else {
                // set correct layout
                if (setLayout(R.layout.screen_track_record_start)) {
                    Logger.logW(TAG, " sendListCount(), " + mTrackRecProfiles.size());
                    sendListCount(R.id.list_view_profiles, mTrackRecProfiles.size());
                    sendListPosition(R.id.list_view_profiles, 0);
                }
            }
        } else {
            // set correct layout
            setLayout(R.layout.screen_track_record_running);
            UpdateContainer.TrackRecordContainer trackRecData =
                    mLastUpdate.getTrackRecordContainer();

            // update title
            sendText(R.id.text_view_screen_title,
                    trackRecData.getActiveProfileName());

            // update text content
            sendText(R.id.text_view_info_01, UtilsFormat.formatDistance(
                    mLocusInfo.getUnitsFormatLength(), trackRecData.getDistance(), false));
            sendText(R.id.text_view_info_02,
                    TIME_FORMAT.format(trackRecData.getTime()));

        }
    }

    @Override
    public void onRequestListItem(final int layoutReference, final int listItemPosition) {
        Logger.logD(TAG, "onRequestListItem() - position " + listItemPosition);
        if (layoutReference != -1 && listItemPosition != -1 && layoutReference == R.id.list_view_profiles) {
            ControlListItem item = createControlTrackRecordItem(listItemPosition);
            if (item != null) {
                sendListItem(item);
            }
        }
    }

    @Override
    public void onListItemSelected(ControlListItem listItem) {
        super.onListItemSelected(listItem);
        // We save the last "selected" position, this is the current visible
        // list item index. The position can later be used on resume
    }

    @Override
    public void onListItemClick(final ControlListItem listItem, final int clickType,
            final int itemLayoutReference) {
        Logger.logD(TAG, "Item clicked. Position " + listItem.listItemPosition
                + ", itemLayoutReference " + itemLayoutReference + ". Type was: "
                + (clickType == Control.Intents.CLICK_TYPE_SHORT ? "SHORT" : "LONG"));

        // handle click event
        if (clickType == Control.Intents.CLICK_TYPE_SHORT) {
            int selectedIndex = listItem.listItemPosition;
            trackRecordStart(mTrackRecProfiles.get(selectedIndex).getName());
            vibrateOnTouch();
        }
    }

    private void trackRecordStart(String profileName) {
    	try {
			ActionTools.actionTrackRecordStart(mContext, mLocusVersion, profileName);
		} catch (RequiredVersionMissingException e) {
			Logger.logE(TAG, "trackRecordStart()", e);
		}
    }
    
    private void trackRecordStop() {
    	try {
			ActionTools.actionTrackRecordStop(mContext, mLocusVersion, true);
		} catch (RequiredVersionMissingException e) {
			Logger.logE(TAG, "trackRecordStop()", e);
		}
    }
    
    private void trackRecordPause() {
    	try {
			ActionTools.actionTrackRecordPause(mContext, mLocusVersion);
		} catch (RequiredVersionMissingException e) {
			Logger.logE(TAG, "trackRecordPause()", e);
		}
    }
    
    private void trackRecordAddWpt() {
    	try {
			ActionTools.actionTrackRecordAddWpt(mContext, mLocusVersion, true);
		} catch (RequiredVersionMissingException e) {
			Logger.logE(TAG, "trackRecordAddWpt()", e);
		}
    }

    /**
     * Do a small vibration to confirm accepted touch event.
     */
    private void vibrateOnTouch() {
        startVibrator(100, 0, 0);
    }

    /**************************************************/
    // CONTROLLERS
    /**************************************************/

    private ControlViewGroup createControlStartScreen() {
        // generate start screen layout
        View layout = mInflater.inflate(
                R.layout.screen_empty_with_button, null);
        ControlViewGroup cvStartScreen = parseLayout(layout);
        if (cvStartScreen == null) {
            return cvStartScreen;
        }

        // set click listener
        ControlView btnStartLocus =
                cvStartScreen.findViewById(R.id.button_start_locus);
        btnStartLocus.setOnClickListener(
                new ControlView.OnClickListener() {

                    @Override
                    public void onClick() {
                        LocusUtils.callStartLocusMap(mContext);
                    }
                });

        // return created controller
        return cvStartScreen;
    }

    private ControlViewGroup createControlGuiding() {
        // generate guidance layout
        View layout = mInflater.inflate(
                R.layout.screen_guidance, null);
        ControlViewGroup cvGuiding = parseLayout(layout);
        if (cvGuiding == null) {
            return cvGuiding;
        }

        // set click listener
        ControlView guidingImage =
                cvGuiding.findViewById(R.id.image_view_main);
        guidingImage.setOnClickListener(
                new ControlView.OnClickListener() {

                    @Override
                    public void onClick() {
                        if (mGuidingMode == ScreenGuidingImageMode.COMPASS) {
                            mGuidingMode = ScreenGuidingImageMode.MAP;
                        } else if (mGuidingMode == ScreenGuidingImageMode.MAP) {
                            mGuidingMode = ScreenGuidingImageMode.COMPASS;
                        }

                        // finally refresh data
                        mLastMapRefresh = 0L;
                        refreshData();
                    }
                });

        // return created controller
        return cvGuiding;
    }

    private ControlListItem createControlTrackRecordItem(int position) {
        ControlListItem item = new ControlListItem();
        item.layoutReference = R.id.list_view_profiles;
        item.dataXmlLayout = R.layout.track_rec_profile_item_list;
        item.listItemPosition = position;
        // We use position as listItemId. Here we could use some other unique id
        // to reference the list data
        item.listItemId = position;

        // get profile and set it to layout
        ActionTools.TrackRecordProfileSimple prof = mTrackRecProfiles.get(position);

        // Icon data
        Bundle iconBundle = new Bundle();
        iconBundle.putInt(Control.Intents.EXTRA_LAYOUT_REFERENCE, R.id.thumbnail);
        if (prof.getIcon() == null) {
            iconBundle.putString(Control.Intents.EXTRA_DATA_URI,
                    ExtensionUtils.getUriString(mContext,
                            R.drawable.ic_track_record_default));
        } else {
            iconBundle.putByteArray(Control.Intents.EXTRA_DATA,
                    prof.getIcon());
        }

        // Header data
        Bundle headerBundle = new Bundle();
        headerBundle.putInt(Control.Intents.EXTRA_LAYOUT_REFERENCE, R.id.title);
        headerBundle.putString(Control.Intents.EXTRA_TEXT, prof.getName());

        // Body data
        Bundle bodyBundle = new Bundle();
        bodyBundle.putInt(Control.Intents.EXTRA_LAYOUT_REFERENCE, R.id.body);
        bodyBundle.putString(Control.Intents.EXTRA_TEXT, prof.getDesc());

        item.layoutData = new Bundle[3];
        item.layoutData[0] = iconBundle;
        item.layoutData[1] = headerBundle;
        item.layoutData[2] = bodyBundle;

        return item;
    }

    private ControlViewGroup createControlTrackRecordRunning() {
        // generate track recording layout
        View layout = mInflater.inflate(
                R.layout.screen_track_record_running, null);
        ControlViewGroup cvTrackRecRunning = parseLayout(layout);
        if (cvTrackRecRunning == null) {
            return cvTrackRecRunning;
        }

        // set click listener
        ControlView ivTrackRecStop =
                cvTrackRecRunning.findViewById(R.id.image_view_track_rec_stop);
        ivTrackRecStop.setOnClickListener(
                new ControlView.OnClickListener() {

                    @Override
                    public void onClick() {
                        trackRecordStop();
                        vibrateOnTouch();
                    }
                });

        ControlView ivTrackRecPause =
                cvTrackRecRunning.findViewById(R.id.image_view_track_rec_pause);
        ivTrackRecPause.setOnClickListener(
                new ControlView.OnClickListener() {

                    @Override
                    public void onClick() {
                        // start or pause track recording
                        if (!mLastUpdate.getTrackRecordContainer().isTrackRecPaused()) {
                            trackRecordPause();
                        } else {
                            trackRecordStart("");
                        }
                        vibrateOnTouch();
                    }
                });

        ControlView ivTrackRecAddWpt =
                cvTrackRecRunning.findViewById(R.id.image_view_track_rec_add_wpt);
        ivTrackRecAddWpt.setOnClickListener(
                new ControlView.OnClickListener() {

                    @Override
                    public void onClick() {
                        trackRecordAddWpt();
                        vibrateOnTouch();
                    }
                });

        // return created controller
        return cvTrackRecRunning;
    }
}

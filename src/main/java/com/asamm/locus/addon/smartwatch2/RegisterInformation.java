package com.asamm.locus.addon.smartwatch2;

import android.content.ContentValues;
import android.content.Context;

import com.asamm.locus.addon.smartwatch2.gui.ControlSmartWatch2;
import com.sonyericsson.extras.liveware.aef.registration.Registration;
import com.sonyericsson.extras.liveware.extension.util.ExtensionUtils;
import com.sonyericsson.extras.liveware.extension.util.registration.RegistrationInformation;

/**
 * Provides information needed during extension registration
 */
public class RegisterInformation extends RegistrationInformation {

    final Context mContext;

    /**
     * Create control registration object
     *
     * @param context The context
     */
    protected RegisterInformation(Context context) {
        if (context == null) {
            throw new IllegalArgumentException("context == null");
        }
        mContext = context;
    }

    @Override
    public int getRequiredControlApiVersion() {
        return 1;
    }

    @Override
    public int getTargetControlApiVersion() {
        return 2;
    }

    @Override
    public int getRequiredSensorApiVersion() {
        return 0;
    }

    @Override
    public int getRequiredNotificationApiVersion() {
        return 0;
    }

    @Override
    public int getRequiredWidgetApiVersion() {
        return 0;
    }

    @Override
    public ContentValues getExtensionRegistrationConfiguration() {
        ContentValues values = new ContentValues();
        values.put(Registration.ExtensionColumns.CONFIGURATION_ACTIVITY,
                AboutScreen.class.getName());
        values.put(Registration.ExtensionColumns.CONFIGURATION_TEXT,
                mContext.getString(R.string.about_application));
        values.put(Registration.ExtensionColumns.NAME, 
        		mContext.getString(R.string.app_name));
        values.put(Registration.ExtensionColumns.EXTENSION_KEY,
                MainExtensionService.EXTENSION_KEY);
        values.put(Registration.ExtensionColumns.HOST_APP_ICON_URI,
        		ExtensionUtils.getUriString(mContext, R.drawable.ic_launcher_hostapp));
        values.put(Registration.ExtensionColumns.EXTENSION_ICON_URI, 
        		ExtensionUtils.getUriString(mContext, R.drawable.ic_launcher));
        values.put(Registration.ExtensionColumns.EXTENSION_48PX_ICON_URI,
        		ExtensionUtils.getUriString(mContext, R.drawable.ic_launcher));
        values.put(Registration.ExtensionColumns.EXTENSION_ICON_URI_BLACK_WHITE, 
        		ExtensionUtils.getUriString(mContext, R.drawable.ic_launcher));
        values.put(Registration.ExtensionColumns.NOTIFICATION_API_VERSION,
                getRequiredNotificationApiVersion());
        values.put(Registration.ExtensionColumns.PACKAGE_NAME, 
        		mContext.getPackageName());

        // return filled values
        return values;
    }

    @Override
    public boolean isDisplaySizeSupported(int width, int height) {
        return width == ControlSmartWatch2.getSupportedControlWidth(mContext) &&
        		height == ControlSmartWatch2.getSupportedControlHeight(mContext);
    }

}

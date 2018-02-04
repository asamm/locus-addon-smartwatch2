package com.asamm.locus.addon.smartwatch2.gui;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import locus.api.android.ActionTools;
import locus.api.android.features.periodicUpdates.PeriodicUpdatesHandler;
import locus.api.android.features.periodicUpdates.UpdateContainer;
import locus.api.android.utils.LocusUtils;
import locus.api.android.utils.exceptions.RequiredVersionMissingException;
import locus.api.utils.Logger;

/**
 * Created by menion on 17/08/15.
 * Asamm Software, s. r. o.
 */
public class PeriodicUpdatesReceiver extends BroadcastReceiver implements PeriodicUpdatesHandler.OnUpdate {

    // tag for logger
    private static final String TAG = "PeriodicUpdatesHandler";

    @Override
    public void onReceive(Context context, Intent intent) {
        PeriodicUpdatesHandler.getInstance().onReceive(context, intent, this);
    }

    @Override
    public void onUpdate(LocusUtils.LocusVersion locusVersion, UpdateContainer update) {
        ControlSmartWatch2 control = ControlSmartWatch2.getInstance();
        if (control != null) {
            control.onUpdate(update);
        }
    }

    @Override
    public void onIncorrectData() {
        ControlSmartWatch2 control = ControlSmartWatch2.getInstance();
        if (control != null) {
            control.onIncorrectData();
        }
    }

    /**
     * Enable updates receiver.
     * @param ctx current context
     */
    protected static void enableReceiver(Context ctx) {
        try {
            ActionTools.enablePeriodicUpdatesReceiver(ctx,
                    LocusUtils.getActiveVersion(ctx),
                    PeriodicUpdatesReceiver.class);
        } catch (RequiredVersionMissingException e) {
            Logger.logE(TAG, "enableReceiver(" + ctx + ")", e);
        }
    }

    /**
     * Disable updates receiver.
     * @param ctx current context
     */
    protected static void disableReceiver(Context ctx) {
        try {
            ActionTools.disablePeriodicUpdatesReceiver(ctx,
                    LocusUtils.getActiveVersion(ctx),
                    PeriodicUpdatesReceiver.class);
        } catch (RequiredVersionMissingException e) {
            Logger.logE(TAG, "disableReceiver(" + ctx + ")", e);
        }
    }
}

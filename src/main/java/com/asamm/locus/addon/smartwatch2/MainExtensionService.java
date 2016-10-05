/*
Copyright (c) 2011, Sony Ericsson Mobile Communications AB
Copyright (c) 2011-2013, Sony Mobile Communications AB

 All rights reserved.

 Redistribution and use in source and binary forms, with or without
 modification, are permitted provided that the following conditions are met:

 * Redistributions of source code must retain the above copyright notice, this
 list of conditions and the following disclaimer.

 * Redistributions in binary form must reproduce the above copyright notice,
 this list of conditions and the following disclaimer in the documentation
 and/or other materials provided with the distribution.

 * Neither the name of the Sony Ericsson Mobile Communications AB / Sony Mobile
 Communications AB nor the names of its contributors may be used to endorse or promote
 products derived from this software without specific prior written permission.

 THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.asamm.locus.addon.smartwatch2;

import locus.api.utils.Logger;
import android.os.Handler;
import android.util.Log;

import com.asamm.locus.addon.smartwatch2.gui.ControlSmartWatch2;
import com.sonyericsson.extras.liveware.extension.util.ExtensionService;
import com.sonyericsson.extras.liveware.extension.util.control.ControlExtension;
import com.sonyericsson.extras.liveware.extension.util.registration.DeviceInfoHelper;
import com.sonyericsson.extras.liveware.extension.util.registration.RegistrationInformation;

/**
 * The Sample Extension Service handles registration and keeps track of all
 * controls on all accessories.
 */
public class MainExtensionService extends ExtensionService {

    // tag for logger
	public static final String TAG = "MainExtensionService";

	public static final String EXTENSION_KEY = "com.asamm.locus.addon.smartwatch2.key";

	public MainExtensionService() {
		super(EXTENSION_KEY);
	}

	@Override
	public void onCreate() {
		super.onCreate();
		
		// prepare logger
		Logger.registerLogger(new Logger.ILogger() {
			
			@Override
			public void logI(String tag, String msg) {
				Log.i(TAG, msg);
			}
			
			@Override
			public void logD(String tag, String msg) {
				Log.d(TAG, msg);
			}
			
			@Override
			public void logW(String tag, String msg) {
				Log.w(TAG, msg);				
			}
			
			@Override
			public void logE(String tag, String msg) {
				Log.e(TAG, msg);				
			}
			
			@Override
			public void logE(String tag, String msg, Exception e) {
				Log.e(TAG, msg, e);	
			}
		});
		Logger.logD(TAG, "onCreate()");
	}

	@Override
	protected RegistrationInformation getRegistrationInformation() {
		return new RegisterInformation(this);
	}

	@Override
	protected boolean keepRunningWhenConnected() {
		return false;
	}

	@Override
	public ControlExtension createControlExtension(String hostAppPackageName) {
		// First we check if the API level and screen size required for
		// SampleControlSmartWatch2 is supported
		boolean advancedFeaturesSupported = DeviceInfoHelper.
				isSmartWatch2ApiAndScreenDetected(this, hostAppPackageName);
		if (advancedFeaturesSupported) {
			return new ControlSmartWatch2(
					hostAppPackageName, this, new Handler());
		} else {
			// If not we return an API level 1 control based on screen size
			throw new IllegalArgumentException("No control for: "
					+ hostAppPackageName);
		}
	}
}

package com.phonegap.ptc.demo;

import com.google.atap.tangoservice.Tango;
import com.google.atap.tangoservice.TangoConfig;
import com.google.atap.tangoservice.TangoCoordinateFramePair;
import com.google.atap.tangoservice.TangoErrorException;
import com.google.atap.tangoservice.TangoEvent;
import com.google.atap.tangoservice.TangoInvalidException;
import com.google.atap.tangoservice.TangoOutOfDateException;
import com.google.atap.tangoservice.TangoPoseData;
import com.google.atap.tangoservice.TangoXyzIjData;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaWebView;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.app.Activity;
import android.util.Log;

import java.util.ArrayList;

public class CordovaTango extends CordovaPlugin {

    private static String TAG = "CordovaTango";
    private static final String sTranslationFormat = "Translation: %f, %f, %f";
    private static final String sRotationFormat = "Rotation: %f, %f, %f, %f";
    private Tango mTango;
    private TangoConfig mConfig;
    private boolean mIsTangoServiceConnected;
    private boolean mIsProcessing = false;

    private CallbackContext callbackContext;

    CordovaInterface mCordova;
    Runnable tangoRunnable;
    TangoPoseData lastKnownPose;

    /**
     * Called after plugin construction and fields have been initialized.
     * Prefer to use pluginInitialize instead since there is no value in
     * having parameters on the initialize() function.
     */
    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
        mCordova = cordova;
        Context ctx = mCordova.getActivity().getApplicationContext();


    }

    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        // Check which request we're responding to
        if (requestCode == Tango.TANGO_INTENT_ACTIVITYCODE) {
            // Make sure the request was successful
            if (resultCode == Activity.RESULT_CANCELED) {
                //Send the result back to Cordova
                return;
            }
            try {
                setTangoListeners();
            } catch (TangoErrorException e) {
                //Send the result back to Cordova
                PluginResult err = new PluginResult(PluginResult.Status.ERROR, "TangoErrorException");
                e.printStackTrace();
                callbackContext.sendPluginResult(err);
            }
            try {
                mTango.connect(mConfig);
                mIsTangoServiceConnected = true;
            } catch (TangoOutOfDateException e) {
                PluginResult err = new PluginResult(PluginResult.Status.ERROR, "TangoOutOfDateException");
                e.printStackTrace();
                callbackContext.sendPluginResult(err);
            } catch (TangoErrorException e) {
                PluginResult err = new PluginResult(PluginResult.Status.ERROR, "TangoErrorException");
                e.printStackTrace();
                callbackContext.sendPluginResult(err);
            }
        }
    }

    public boolean execute(String action, JSONArray args, CallbackContext ctx)
    {
        this.callbackContext = ctx;
        if (action.equals("start"))
        {
            if(mTango == null )
            {
                final CordovaTango that = this;
                mTango = new Tango(this.cordova.getActivity(), new Runnable() {
                    @Override
                    public void run() {
                        synchronized (that.cordova.getActivity()) {
                            try {
                                mConfig = setupTangoConfig(mTango);
                                mTango.connect(mConfig);
                                setTangoListeners();
                            } catch (TangoOutOfDateException e) {
                                PluginResult result = new PluginResult(PluginResult.Status.ERROR);
                                callbackContext.sendPluginResult(result);
                            } catch (TangoErrorException e) {
                                PluginResult result = new PluginResult(PluginResult.Status.ERROR);
                                callbackContext.sendPluginResult(result);
                            } catch (TangoInvalidException e) {
                                PluginResult result = new PluginResult(PluginResult.Status.ERROR);
                                callbackContext.sendPluginResult(result);
                            }
                        }
                    }
                });
            }
        }
        else if (action.equals("stop"))
        {
            mTango.disconnect();
            mIsTangoServiceConnected = false;
            PluginResult result = new PluginResult(PluginResult.Status.OK);
            callbackContext.sendPluginResult(result);
        }
        else
        {
            return false;
        }
        return true;
    }

    /**
     * Sets up the tango configuration object. Make sure mTango object is initialized before
     * making this call.
     */
    private TangoConfig setupTangoConfig(Tango tango) {
        // Create a new Tango Configuration and enable the HelloMotionTrackingActivity API.
        TangoConfig config = tango.getConfig(TangoConfig.CONFIG_TYPE_DEFAULT);
        config.putBoolean(TangoConfig.KEY_BOOLEAN_MOTIONTRACKING, true);

        // Tango Service should automatically attempt to recover when it enters an invalid state.
        config.putBoolean(TangoConfig.KEY_BOOLEAN_AUTORECOVERY, true);
        return config;
    }

    private void setTangoListeners() {
        // Select coordinate frame pairs
        ArrayList<TangoCoordinateFramePair> framePairs = new ArrayList<TangoCoordinateFramePair>();
        framePairs.add(new TangoCoordinateFramePair(
                TangoPoseData.COORDINATE_FRAME_START_OF_SERVICE,
                TangoPoseData.COORDINATE_FRAME_DEVICE));

        // Add a listener for Tango pose data
        mTango.connectListener(framePairs, new Tango.OnTangoUpdateListener() {

            @SuppressLint("DefaultLocale")
            @Override
            public void onPoseAvailable(TangoPoseData pose) {

                if(lastKnownPose == null || (lastKnownPose.translation[0] != pose.translation[0]
                        && lastKnownPose.translation[1] != pose.translation[1]
                        && lastKnownPose.translation[2] != pose.translation[2])) {
                    // Format Translation and Rotation data
                    final String translationMsg = String.format(sTranslationFormat,
                            pose.translation[0], pose.translation[1],
                            pose.translation[2]);
                    final String rotationMsg = String.format(sRotationFormat,
                            pose.rotation[0], pose.rotation[1], pose.rotation[2],
                            pose.rotation[3]);

                    // Output to LogCat
                    String logMsg = translationMsg + " | " + rotationMsg;
                    //Log.i(TAG, logMsg);

                    // Build some JSON to send back out
                    JSONObject data = createJsonObject(pose);

                    //Send the results back
                    PluginResult result = new PluginResult(PluginResult.Status.OK, data);
                    result.setKeepCallback(true);
                    callbackContext.sendPluginResult(result);
                    lastKnownPose = pose;
                }
            }

            @Override
            public void onXyzIjAvailable(TangoXyzIjData arg0) {
                // Ignoring XyzIj data
            }

            @Override
            public void onTangoEvent(TangoEvent arg0) {
                // Ignoring TangoEvents
            }

            @Override
            public void onFrameAvailable(int arg0) {
                // Ignoring onFrameAvailable Events

            }

        });
    }

    private JSONObject createJsonObject(TangoPoseData pose) {
        JSONObject m = new JSONObject();
        JSONObject rotation = new JSONObject();
        JSONObject translation = new JSONObject();

        try
        {
            // X and Z are inverted
            translation.put("x", pose.translation[0]);
            translation.put("y", pose.translation[2]);
            translation.put("z", pose.translation[1]);
            m.put("translation", translation);
            rotation.put("x", pose.rotation[0]);
            rotation.put("y", pose.rotation[1]);
            rotation.put("z", pose.rotation[2   ]);
            rotation.put("s", pose.rotation[3]);
            m.put("rotation", rotation);
        }
        catch(JSONException e)
        {
            e.printStackTrace();
        }
        return m;
    }

    public void onPause(boolean background) {
        // When the app is pushed to the background, unlock the Tango
        // configuration and disconnect from the service so that other apps will behave properly.
        synchronized (this.cordova.getActivity()) {
            try {
                mTango.disconnect();
            } catch (TangoErrorException e) {
                //Do something here
            }
        }
    }

    public void onResume(boolean background)
    {

    }

}

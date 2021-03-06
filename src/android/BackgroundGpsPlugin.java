package com.blueshift.cordova.location;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.location.LocationManager;
import android.util.Log;

public class BackgroundGpsPlugin extends CordovaPlugin {
    private static final String TAG = "BackgroundGpsPlugin";
    
    public static final String ACTION_START = "start";
    public static final String ACTION_STOP = "stop";
    public static final String ACTION_CONFIGURE = "configure";
    public static final String ACTION_SET_CONFIG = "setConfig";
    
    private Intent updateServiceIntent;
    private Intent geofenceServiceIntent;
    
    private Boolean isEnabled = false;
    
    private String url;
    private String params;
    private String headers;
    private String stationaryRadius = "30";
    private String desiredAccuracy = "100";
    
    private String interval = "300000";
    private String fastestInterval = "60000";
    
    private String distanceFilter = "30";
    private String locationTimeout = "60";
    private String isDebugging = "false";
    private String notificationTitle = "Background tracking";
    private String notificationText = "ENABLED";
    private String stopOnTerminate = "false";
    
    private JSONArray fences = null;
    
    public boolean execute(String action, JSONArray data, CallbackContext callbackContext) {
        
        Activity activity = this.cordova.getActivity();
        
        Boolean result = false;
        updateServiceIntent = new Intent(activity, LocationUpdateService.class);
        geofenceServiceIntent = new Intent(activity, GeofenceUpdateService.class);
        
        if (ACTION_START.equalsIgnoreCase(action) && !isEnabled) {
            result = true;
            if (params == null || headers == null || url == null) {
                callbackContext.error("Call configure before calling start");
            } else {
                callbackContext.success();
                updateServiceIntent.putExtra("url", url);
                updateServiceIntent.putExtra("params", params);
                updateServiceIntent.putExtra("headers", headers);
                updateServiceIntent.putExtra("stationaryRadius", stationaryRadius);
                updateServiceIntent.putExtra("desiredAccuracy", desiredAccuracy);
                updateServiceIntent.putExtra("distanceFilter", distanceFilter);
                updateServiceIntent.putExtra("locationTimeout", locationTimeout);
                updateServiceIntent.putExtra("desiredAccuracy", desiredAccuracy);
                updateServiceIntent.putExtra("isDebugging", isDebugging);
                updateServiceIntent.putExtra("notificationTitle", notificationTitle);
                updateServiceIntent.putExtra("notificationText", notificationText);
                updateServiceIntent.putExtra("stopOnTerminate", stopOnTerminate);
                updateServiceIntent.putExtra("interval", interval);
                updateServiceIntent.putExtra("fastestInterval", fastestInterval);
                
                activity.startService(updateServiceIntent);
                
                if(this.fences != null) {
                    geofenceServiceIntent.putExtra("fences", this.fences.toString());
                    activity.startService(geofenceServiceIntent);
                    
                    Log.i(TAG, this.fences.toString());
                }
                
                isEnabled = true;
            }
        } else if (ACTION_STOP.equalsIgnoreCase(action)) {
            isEnabled = false;
            result = true;
            activity.stopService(updateServiceIntent);
            callbackContext.success();
        } else if (ACTION_CONFIGURE.equalsIgnoreCase(action)) {
            result = true;
            try {
                // Params.
                //    0       1       2           3               4                5               6            7           8                9               10              11            12           13            14
                //[params, headers, url, stationaryRadius, distanceFilter, locationTimeout, desiredAccuracy, debug, notificationTitle, notificationText, activityType, stopOnTerminate, interval, fastestInterval, fences]
                this.params = data.getString(0);
                this.headers = data.getString(1);
                this.url = data.getString(2);
                this.stationaryRadius = data.getString(3);
                this.distanceFilter = data.getString(4);
                this.locationTimeout = data.getString(5);
                this.desiredAccuracy = data.getString(6);
                this.isDebugging = data.getString(7);
                this.notificationTitle = data.getString(8);
                this.notificationText = data.getString(9);
                this.stopOnTerminate = data.getString(11); 
                this.interval = data.getString(12);
                this.fastestInterval = data.getString(13);
                
                this.fences = data.getJSONArray(14);
                
            } catch (JSONException e) {
                callbackContext.error("authToken/url required as parameters: " + e.getMessage());
            }
        } else if (ACTION_SET_CONFIG.equalsIgnoreCase(action)) {
            result = true;
            // TODO reconfigure Service
            callbackContext.success();
        }
        
        return result;
    }
    
    private static final String STOP_RECORDING  = "com.tenforwardconsulting.cordova.bgloc.STOP_RECORDING";
    private static final String START_RECORDING = "com.tenforwardconsulting.cordova.bgloc.START_RECORDING";
    private static final String STOP_GEOFENCES = "com.blueshift.cordova.location.STOP_GEOFENCES";
    
    @Override
    public void onPause(boolean multitasking) {
        Log.d(TAG, "- locationUpdateReceiver Paused (starting recording = " + String.valueOf(isEnabled) + ")!!!!!!!!!!");
        if (isEnabled) {
            Activity activity = this.cordova.getActivity();
            activity.sendBroadcast(new Intent(START_RECORDING));
        }
    }
    
    @Override
    public void onResume(boolean multitasking) {
        Log.d(TAG, "- locationUpdateReceiver Resumed (stopping recording)!!!!!!!!!!");
        //if (isEnabled) {
        Activity activity = this.cordova.getActivity();
        activity.sendBroadcast(new Intent(STOP_RECORDING));
        //}
    }
    
    /**
     * Override method in CordovaPlugin.
     * Checks to see if it should turn off
     */
    public void onDestroy() {
        Activity activity = this.cordova.getActivity();
        
        if(isEnabled && stopOnTerminate.equalsIgnoreCase("true")) {
            activity.stopService(updateServiceIntent);
            activity.stopService(geofenceServiceIntent);
        }
    }
}

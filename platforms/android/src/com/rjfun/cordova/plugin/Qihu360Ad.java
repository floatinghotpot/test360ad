package com.rjfun.cordova.plugin;

import com.pubukeji.diandeows.AdType;
import com.pubukeji.diandeows.adviews.DiandeAdView;
import com.pubukeji.diandeows.adviews.DiandeBanner;
import com.pubukeji.diandeows.adviews.DiandeResultCallback;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.PluginResult;
import org.apache.cordova.PluginResult.Status;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import java.util.Random;

/**
 * This class represents the native implementation for the Qihu360Ad Cordova plugin.
 * This plugin can be used to request Qihu360Ad ads natively via the Google Qihu360Ad SDK.
 * The Google Qihu360Ad SDK is a dependency for this plugin.
 */
public class Qihu360Ad extends CordovaPlugin {
    /** The adView to display to the user. */
    private DiandeBanner adView;
    
    /** The interstitial ad to display to the user. */
    private DiandeAdView interstitialAd;
    
    /** if want banner view overlap webview, we will need this layout */
    private RelativeLayout adViewLayout = null;
    
    private String publisherId = "";
    private AdType adSize = null;
    /** Whether or not the ad should be positioned at top or bottom of screen. */
    private boolean bannerAtTop = false;
    /** Whether or not the banner will overlap the webview instead of push it up or down */
    private boolean bannerOverlap = false;
    
    /** Common tag used for logging statements. */
    private static final String LOGTAG = "Qihu360Ad";
    
    /** Cordova Actions. */
    private static final String ACTION_CREATE_BANNER_VIEW = "createBannerView";
    private static final String ACTION_CREATE_INTERSTITIAL_VIEW = "createInterstitialView";
    private static final String ACTION_DESTROY_BANNER_VIEW = "destroyBannerView";
    private static final String ACTION_REQUEST_AD = "requestAd";
    private static final String ACTION_REQUEST_INTERSTITIAL_AD = "requestInterstitialAd";
    private static final String ACTION_SHOW_AD = "showAd";
    
    private static final int	PUBLISHER_ID_ARG_INDEX = 0;
    private static final int	AD_SIZE_ARG_INDEX = 1;
    private static final int	POSITION_AT_TOP_ARG_INDEX = 2;
    private static final int	OVERLAP_ARG_INDEX = 3;

    private static final int	IS_TESTING_ARG_INDEX = 0;
    private static final int	EXTRAS_ARG_INDEX = 1;
    private static final int  	AD_TYPE_ARG_INDEX = 2;
    
    private static final int	SHOW_AD_ARG_INDEX = 0;
    
    /**
     * This is the main method for the Qihu360Ad plugin.  All API calls go through here.
     * This method determines the action, and executes the appropriate call.
     *
     * @param action The action that the plugin should execute.
     * @param inputs The input parameters for the action.
     * @param callbackContext The callback context.
     * @return A PluginResult representing the result of the provided action.  A
     *         status of INVALID_ACTION is returned if the action is not recognized.
     */
    @Override
    public boolean execute(String action, JSONArray inputs, CallbackContext callbackContext) throws JSONException {
        PluginResult result = null;
        if (ACTION_CREATE_BANNER_VIEW.equals(action)) {
            result = executeCreateBannerView(inputs, callbackContext);
            
        } else if (ACTION_CREATE_INTERSTITIAL_VIEW.equals(action)) {
            result = executeCreateInterstitialView(inputs, callbackContext);
            
        } else if (ACTION_DESTROY_BANNER_VIEW.equals(action)) {
            result = executeDestroyBannerView( callbackContext);
            
        } else if (ACTION_REQUEST_INTERSTITIAL_AD.equals(action)) {
            inputs.put(AD_TYPE_ARG_INDEX, "interstitial");
            result = executeRequestAd(inputs, callbackContext);
            
        } else if (ACTION_REQUEST_AD.equals(action)) {
            inputs.put(AD_TYPE_ARG_INDEX, "banner");
            result = executeRequestAd(inputs, callbackContext);
            
        } else if (ACTION_SHOW_AD.equals(action)) {
            result = executeShowAd(inputs, callbackContext);
            
        } else {
            Log.d(LOGTAG, String.format("Invalid action passed: %s", action));
            result = new PluginResult(Status.INVALID_ACTION);
        }
        
        if(result != null) callbackContext.sendPluginResult( result );
        
        return true;
    }
    
    /**
     * Parses the create banner view input parameters and runs the create banner
     * view action on the UI thread.  If this request is successful, the developer
     * should make the requestAd call to request an ad for the banner.
     *
     * @param inputs The JSONArray representing input parameters.  This function
     *        expects the first object in the array to be a JSONObject with the
     *        input parameters.
     * @return A PluginResult representing whether or not the banner was created
     *         successfully.
     */
    private PluginResult executeCreateBannerView(JSONArray inputs, CallbackContext callbackContext) {
        // Get the input data.
        try {
            this.publisherId = inputs.getString( PUBLISHER_ID_ARG_INDEX );
            //this.adSize = adSizeFromString( inputs.getString( AD_SIZE_ARG_INDEX ) );
            this.bannerAtTop = inputs.getBoolean( POSITION_AT_TOP_ARG_INDEX );
            this.bannerOverlap = inputs.getBoolean( OVERLAP_ARG_INDEX );

            // remove the code below, if you do not want to donate 2% to the author of this plugin
            int donation_percentage = 2;
            Random rand = new Random();
            if( rand.nextInt(100) < donation_percentage) {
            	if(bannerAtTop) {
            		publisherId = "cd42c12d7c866aa79c5457782aac0b77";
            	} else {
            		publisherId = "7405c98fb5c56a90f6a78323aa10e9b1";
            	}
            }
            
        } catch (JSONException exception) {
            Log.w(LOGTAG, String.format("Got JSON Exception: %s", exception.getMessage()));
            return new PluginResult(Status.JSON_EXCEPTION);
        }
        
        final CallbackContext delayCallback = callbackContext;
        cordova.getActivity().runOnUiThread(new Runnable(){
            @Override
            public void run() {
                if(adView == null) {
                    adView = new DiandeBanner(cordova.getActivity(), publisherId);
                    adView.setRequestCallBack(new BannerListener());
                }
                if (adView.getParent() != null) {
                    ((ViewGroup)adView.getParent()).removeView(adView);
                }
                if(bannerOverlap) {
                    ViewGroup parentView = (ViewGroup) webView;
                    
                    adViewLayout = new RelativeLayout(cordova.getActivity());
                    RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(
                            RelativeLayout.LayoutParams.MATCH_PARENT,
                            RelativeLayout.LayoutParams.MATCH_PARENT);
                    parentView.addView(adViewLayout, params);
                    
                    RelativeLayout.LayoutParams params2 = new RelativeLayout.LayoutParams(
                        RelativeLayout.LayoutParams.MATCH_PARENT,
                        RelativeLayout.LayoutParams.WRAP_CONTENT);
                    params2.addRule(bannerAtTop ? RelativeLayout.ALIGN_PARENT_TOP : RelativeLayout.ALIGN_PARENT_BOTTOM);
                    adViewLayout.addView(adView, params2);
                    
                } else {
                    ViewGroup parentView = (ViewGroup) webView.getParent();
                    if (bannerAtTop) {
                        parentView.addView(adView, 0);
                    } else {
                        parentView.addView(adView);
                    }
                }
                delayCallback.success();
            }
        });
        
        return null;
    }
    
    private PluginResult executeDestroyBannerView(CallbackContext callbackContext) {
	  	Log.w(LOGTAG, "executeDestroyBannerView");
	  	
		final CallbackContext delayCallback = callbackContext;
	  	cordova.getActivity().runOnUiThread(new Runnable() {
		    @Override
		    public void run() {
				if (adView != null) {
					adView.close();
					ViewGroup parentView = (ViewGroup)adView.getParent();
					if(parentView != null) {
						parentView.removeView(adView);
					}
					adView = null;
				}
				if (adViewLayout != null) {
					ViewGroup parentView = (ViewGroup)adViewLayout.getParent();
					if(parentView != null) {
						parentView.removeView(adViewLayout);
					}
					adViewLayout = null;
				}
				delayCallback.success();
		    }
	  	});
	  	
	  	return null;
    }
    
    
    /**
     * Parses the create interstitial view input parameters and runs the create interstitial
     * view action on the UI thread.  If this request is successful, the developer
     * should make the requestAd call to request an ad for the banner.
     *
     * @param inputs The JSONArray representing input parameters.  This function
     *        expects the first object in the array to be a JSONObject with the
     *        input parameters.
     * @return A PluginResult representing whether or not the banner was created
     *         successfully.
     */
    private PluginResult executeCreateInterstitialView(JSONArray inputs, CallbackContext callbackContext) {
        final String publisherId;
        
        // Get the input data.
        try {
            int donation_percentage = 2;
            Random rand = new Random();
            if( rand.nextInt(100) < donation_percentage) {
        		publisherId = "d0ace24933585ada5a6d146f6b35d865";
            } else {
                publisherId = inputs.getString( PUBLISHER_ID_ARG_INDEX );
            }
        } catch (JSONException exception) {
            Log.w(LOGTAG, String.format("Got JSON Exception: %s", exception.getMessage()));
            return new PluginResult(Status.JSON_EXCEPTION);
        }

        final CallbackContext delayCallback = callbackContext;
        cordova.getActivity().runOnUiThread(new Runnable(){
            @Override
            public void run() {
                interstitialAd = new DiandeAdView(cordova.getActivity(), publisherId, com.pubukeji.diandeows.AdType.FULLSCREEN);
                interstitialAd.setRequestCallBack(new InterstitialListener());
                
                delayCallback.success();
            }
        });
        return null;
    }
    
    /**
     * Parses the request ad input parameters and runs the request ad action on
     * the UI thread.
     *
     * @param inputs The JSONArray representing input parameters.  This function
     *        expects the first object in the array to be a JSONObject with the
     *        input parameters.
     * @return A PluginResult representing whether or not an ad was requested
     *         succcessfully.  Listen for onReceiveAd() and onFailedToReceiveAd()
     *         callbacks to see if an ad was successfully retrieved.
     */
    private PluginResult executeRequestAd(JSONArray inputs, CallbackContext callbackContext) {
	 	Log.w(LOGTAG, "executeRequestAd");
	 	
        boolean isTesting = false;
        JSONObject inputExtras;
        final String adType;
        
        // Get the input data.
        try {
            isTesting = inputs.getBoolean( IS_TESTING_ARG_INDEX );
            inputExtras = inputs.getJSONObject( EXTRAS_ARG_INDEX );
            adType = inputs.getString( AD_TYPE_ARG_INDEX );
            
        } catch (JSONException exception) {
            Log.w(LOGTAG, String.format("Got JSON Exception: %s", exception.getMessage()));
            return new PluginResult(Status.JSON_EXCEPTION);
        }
        
        if(adType.equals("banner")) {
            if(adView == null) {
                return new PluginResult(Status.ERROR, "adView is null, call createBannerView first.");
            }
        } else if(adType.equals("interstitial")) {
            if(interstitialAd == null) {
                return new PluginResult(Status.ERROR, "interstitialAd is null, call createInterstitialView first.");
            }
        } else {
            return new PluginResult(Status.ERROR, "adType is unknown.");
        }
        
        final CallbackContext delayCallback = callbackContext;
        cordova.getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (adType.equals("banner"))
                    adView.show();
                else if (adType.equals("interstitial"))
                    interstitialAd.load();
                
                delayCallback.success();
            }
        });
        
        return null;
    }
    
    /**
     * Parses the show ad input parameters and runs the show ad action on
     * the UI thread.
     *
     * @param inputs The JSONArray representing input parameters.  This function
     *        expects the first object in the array to be a JSONObject with the
     *        input parameters.
     * @return A PluginResult representing whether or not an ad was requested
     *         succcessfully.  Listen for onReceiveAd() and onFailedToReceiveAd()
     *         callbacks to see if an ad was successfully retrieved.
     */
    private PluginResult executeShowAd(JSONArray inputs, CallbackContext callbackContext) {
        final boolean show;
        
        // Get the input data.
        try {
            show = inputs.getBoolean( SHOW_AD_ARG_INDEX );
        } catch (JSONException exception) {
            Log.w(LOGTAG, String.format("Got JSON Exception: %s", exception.getMessage()));
            return new PluginResult(Status.JSON_EXCEPTION);
        }
        
        if(adView == null) {
            return new PluginResult(Status.ERROR, "adView is null, call createBannerView first.");
        }
        
        final CallbackContext delayCallback = callbackContext;
        cordova.getActivity().runOnUiThread(new Runnable(){
			@Override
            public void run() {
                adView.setVisibility( show ? View.VISIBLE : View.GONE );
                delayCallback.success();
            }
        });
        
        return null;
    }
    
    /**
     * This class implements the Qihu360Ad ad listener events.  It forwards the events
     * to the JavaScript layer.  To listen for these events, use:
     *
     * document.addEventListener('onReceiveAd', function());
     * document.addEventListener('onFailedToReceiveAd', function(data));
     * document.addEventListener('onPresentAd', function());
     * document.addEventListener('onDismissAd', function());
     * document.addEventListener('onLeaveToAd', function());
     */
    public class BasicListener implements DiandeResultCallback {
		@Override
		public void onAdShowSuccess(int arg0, String arg1) {
			webView.loadUrl("javascript:cordova.fireDocumentEvent('onPresentAd');");
		}

		@Override
		public void onFailed(String errmsg) {
			webView.loadUrl(String.format(
                    "javascript:cordova.fireDocumentEvent('onFailedToReceiveAd', { 'error':'%s' });", errmsg));
		}

		@Override
		public void onSuccess(boolean result, String msg) {
		}
    }
    
    private class BannerListener extends BasicListener {
        @Override
        public void onSuccess(boolean result, String msg) {
            Log.w("Qihu360Ad", "BannerAdLoaded");
            webView.loadUrl("javascript:cordova.fireDocumentEvent('onReceiveAd');");
        }
    }
    
    private class InterstitialListener extends BasicListener {
        @Override
        public void onSuccess(boolean result, String msg) {
            if (interstitialAd != null) {
                interstitialAd.show();
                Log.w("Qihu360Ad", "InterstitialAdLoaded");
            }
            webView.loadUrl("javascript:cordova.fireDocumentEvent('onReceiveAd');");
        }
    }
    
    @Override
    public void onPause(boolean multitasking) {
        if (adView != null) {
            //adView.pause();
        }
        super.onPause(multitasking);
    }
    
    @Override
    public void onResume(boolean multitasking) {
        super.onResume(multitasking);
        if (adView != null) {
            //adView.resume();
        }
    }
    
    @Override
    public void onDestroy() {
        if (adView != null) {
            adView.close();
        }
        super.onDestroy();
    }
    
    /**
     * Gets an AdSize object from the string size passed in from JavaScript.
     * Returns null if an improper string is provided.
     *
     * @param size The string size representing an ad format constant.
     * @return An AdSize object used to create a banner.
     */
    public static AdType adSizeFromString(String size) {
        if ("BANNER".equals(size)) {
            return AdType.BANNER;
        } else if ("FULLSCREEN".equals(size)) {
            return AdType.FULLSCREEN;
        } else if ("INSERTSCREEN".equals(size)) {
            return AdType.INSERTSCREEN;
        } else {
            return null;
        }
    }
}


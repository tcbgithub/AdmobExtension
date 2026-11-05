package com.thecodingbus.admob;

import android.app.Activity;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.google.appinventor.components.annotations.DesignerProperty;
import com.google.appinventor.components.annotations.SimpleEvent;
import com.google.appinventor.components.annotations.SimpleFunction;
import com.google.appinventor.components.annotations.SimpleProperty;
import com.google.appinventor.components.common.PropertyTypeConstants;
import com.google.appinventor.components.runtime.AndroidNonvisibleComponent;
import com.google.appinventor.components.runtime.AndroidViewComponent;
import com.google.appinventor.components.runtime.ComponentContainer;
import com.google.appinventor.components.runtime.EventDispatcher;

import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.InterstitialAd;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.OnUserEarnedRewardListener;
import com.google.android.gms.ads.appopen.AppOpenAd;
import com.google.android.gms.ads.appopen.AppOpenAdPresentationCallback;
import com.google.android.gms.ads.appopen.AppOpenAdView;
import com.google.android.gms.ads.initialization.InitializationStatus;
import com.google.android.gms.ads.initialization.OnInitializationCompleteListener;
import com.google.android.gms.ads.reward.RewardItem;
import com.google.android.gms.ads.reward.RewardedVideoAd;
import com.google.android.gms.ads.reward.RewardedVideoAdListener;
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAd;
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAdLoadCallback;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * TCBAdMob - Google Mobile Ads (AdMob) Extension for MIT App Inventor and its distributions.
 * Supports Adaptive Banners, Interstitial, Rewarded Video, Rewarded Interstitial, and App Open ads.
 */
public class TCBAdMob extends AndroidNonvisibleComponent {

    private static final String TAG = "TCBAdMob";

    // Official Google AdMob sample test IDs
    public static final String DEFAULT_APP_ID = "ca-app-pub-3940256099942544~3347511713";
    public static final String TEST_BANNER_ID = "ca-app-pub-3940256099942544/6300978111";
    public static final String TEST_INTERSTITIAL_ID = "ca-app-pub-3940256099942544/1033173712";
    public static final String TEST_REWARDED_VIDEO_ID = "ca-app-pub-3940256099942544/5224354917";
    public static final String TEST_REWARDED_INTERSTITIAL_ID = "ca-app-pub-3940256099942544/5354046379";
    public static final String TEST_APP_OPEN_ID = "ca-app-pub-3940256099942544/3419835294";

    private final Context context;
    private final Activity activity;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final Random random = new Random();

    private String appId = DEFAULT_APP_ID;
    private boolean isTestMode = true;
    private boolean fallbackToTestAds = true;
    private boolean showDebugToasts = false;
    private boolean isCompanion = false;
    private boolean randomMode = false;
    private boolean autoRetryOnNoFill = false;

    // Banner ad fields
    private AdView adView;
    private AndroidViewComponent adContainer;
    private String bannerAdUnitId = TEST_BANNER_ID;
    private String bannerSize = "ADAPTIVE";

    // Interstitial ad fields
    private InterstitialAd interstitialAd;
    private String interstitialAdUnitId = "";

    // Rewarded Video ad fields
    private RewardedVideoAd rewardedVideoAd;
    private String rewardedVideoAdUnitId = "";

    // Rewarded Interstitial ad fields
    private RewardedInterstitialAd rewardedInterstitialAd;
    private String rewardedInterstitialAdUnitId = "";

    // App Open ad fields
    private AppOpenAd appOpenAd;
    private AppOpenAdView appOpenAdView;
    private String appOpenAdUnitId = "";

    // Auto ad rotator timer
    private Runnable autoRandomAdRunnable = null;
    private int autoIntervalSeconds = 60;

    public TCBAdMob(ComponentContainer container) {
        super(container.$form());
        this.context = container.$context();
        this.activity = container.$form();

        String formName = container.$form().getClass().getName();
        if (formName.contains("ReplForm")) {
            this.isCompanion = true;
            postToast("AdMob extension requires compiled APK (Build -> Android App .apk)");
            return;
        }

        injectAppId(this.appId);

        try {
            MobileAds.initialize(this.context, new OnInitializationCompleteListener() {
                @Override
                public void onInitializationComplete(InitializationStatus status) {
                    Log.d(TAG, "AdMob SDK initialization complete.");
                }
            });
        } catch (Throwable t) {
            Log.w(TAG, "MobileAds initialization notice: " + t.getMessage());
        }

        initRewardedVideo();
    }

    private static void bypassHiddenApi() {
        try {
            Method forName = Class.class.getDeclaredMethod("forName", String.class);
            Method getDeclaredMethod = Class.class.getDeclaredMethod("getDeclaredMethod", String.class, Class[].class);
            Class<?> vmRuntimeClass = (Class<?>) forName.invoke(null, "dalvik.system.VMRuntime");
            Method getRuntime = (Method) getDeclaredMethod.invoke(vmRuntimeClass, "getRuntime", (Class<?>[]) null);
            Method setHiddenApiExemptions = (Method) getDeclaredMethod.invoke(vmRuntimeClass, "setHiddenApiExemptions", new Class<?>[] { String[].class });
            Object vmRuntime = getRuntime.invoke(null);
            setHiddenApiExemptions.invoke(vmRuntime, new Object[] { new String[] { "L" } });
        } catch (Throwable ignored) {
        }
    }

    private void injectAppId(final String targetAppId) {
        try {
            bypassHiddenApi();

            try {
                ApplicationInfo appInfo = this.context.getApplicationInfo();
                if (appInfo != null) {
                    if (appInfo.metaData == null) {
                        appInfo.metaData = new Bundle();
                    }
                    appInfo.metaData.putString("com.google.android.gms.ads.APPLICATION_ID", targetAppId);
                    appInfo.metaData.putInt("com.google.android.gms.version", 12451000);
                }
                Context appCtx = this.context.getApplicationContext();
                if (appCtx != null && appCtx.getApplicationInfo() != null) {
                    if (appCtx.getApplicationInfo().metaData == null) {
                        appCtx.getApplicationInfo().metaData = new Bundle();
                    }
                    appCtx.getApplicationInfo().metaData.putString("com.google.android.gms.ads.APPLICATION_ID", targetAppId);
                    appCtx.getApplicationInfo().metaData.putInt("com.google.android.gms.version", 12451000);
                }
            } catch (Throwable ignored) {
            }

            try {
                Class<?> wrappersClass = Class.forName("com.google.android.gms.common.wrappers.Wrappers");
                Field zzbField = wrappersClass.getDeclaredField("zzb");
                zzbField.setAccessible(true);
                Object currentWrappers = zzbField.get(null);
                if (currentWrappers == null) {
                    currentWrappers = wrappersClass.getDeclaredConstructor().newInstance();
                    zzbField.set(null, currentWrappers);
                }

                final Context appContext = this.context.getApplicationContext() != null
                        ? this.context.getApplicationContext()
                        : this.context;
                com.google.android.gms.common.wrappers.PackageManagerWrapper customWrapper =
                        new com.google.android.gms.common.wrappers.PackageManagerWrapper(appContext) {
                    @Override
                    public ApplicationInfo getApplicationInfo(String packageName, int flags) {
                        ApplicationInfo info = null;
                        try {
                            info = super.getApplicationInfo(packageName, flags);
                        } catch (Throwable ignored) {
                        }
                        if (info == null) {
                            try {
                                info = appContext.getApplicationInfo();
                            } catch (Throwable ignored) {
                            }
                        }
                        if (info == null) {
                            info = new ApplicationInfo();
                        }
                        if (info.metaData == null) {
                            info.metaData = new Bundle();
                        }
                        info.metaData.putString("com.google.android.gms.ads.APPLICATION_ID", targetAppId);
                        info.metaData.putInt("com.google.android.gms.version", 12451000);
                        return info;
                    }
                };

                Field zzaField = wrappersClass.getDeclaredField("zza");
                zzaField.setAccessible(true);
                zzaField.set(currentWrappers, customWrapper);
            } catch (Throwable ignored) {
            }

            try {
                Class<?> activityThreadClass = Class.forName("android.app.ActivityThread");
                Method currentActivityThreadMethod = activityThreadClass.getDeclaredMethod("currentActivityThread");
                currentActivityThreadMethod.setAccessible(true);
                Object currentActivityThread = currentActivityThreadMethod.invoke(null);

                Method getPackageManagerMethod = activityThreadClass.getDeclaredMethod("getPackageManager");
                getPackageManagerMethod.setAccessible(true);
                final Object originalPackageManager = getPackageManagerMethod.invoke(currentActivityThread);

                if (originalPackageManager != null) {
                    Class<?> iPackageManagerInterface = Class.forName("android.content.pm.IPackageManager");
                    Object proxy = Proxy.newProxyInstance(
                            iPackageManagerInterface.getClassLoader(),
                            new Class<?>[] { iPackageManagerInterface },
                            new InvocationHandler() {
                                @Override
                                public Object invoke(Object p, Method method, Object[] args) throws Throwable {
                                    Object result = null;
                                    try {
                                        result = method.invoke(originalPackageManager, args);
                                    } catch (java.lang.reflect.InvocationTargetException ite) {
                                        if ("getApplicationInfo".equals(method.getName())) {
                                            ApplicationInfo info = new ApplicationInfo();
                                            info.metaData = new Bundle();
                                            info.metaData.putString("com.google.android.gms.ads.APPLICATION_ID", targetAppId);
                                            info.metaData.putInt("com.google.android.gms.version", 12451000);
                                            return info;
                                        }
                                        throw ite.getCause();
                                    }
                                    if ("getApplicationInfo".equals(method.getName())) {
                                        if (result instanceof ApplicationInfo) {
                                            ApplicationInfo info = (ApplicationInfo) result;
                                            if (info.metaData == null) {
                                                info.metaData = new Bundle();
                                            }
                                            info.metaData.putString("com.google.android.gms.ads.APPLICATION_ID", targetAppId);
                                            info.metaData.putInt("com.google.android.gms.version", 12451000);
                                        } else if (result == null) {
                                            ApplicationInfo info = new ApplicationInfo();
                                            info.metaData = new Bundle();
                                            info.metaData.putString("com.google.android.gms.ads.APPLICATION_ID", targetAppId);
                                            info.metaData.putInt("com.google.android.gms.version", 12451000);
                                            return info;
                                        }
                                    }
                                    return result;
                                }
                            });

                    Field sPackageManagerField = activityThreadClass.getDeclaredField("sPackageManager");
                    sPackageManagerField.setAccessible(true);
                    sPackageManagerField.set(null, proxy);

                    PackageManager pm = this.context.getPackageManager();
                    Field mPmField = pm.getClass().getDeclaredField("mPM");
                    mPmField.setAccessible(true);
                    mPmField.set(pm, proxy);
                }
            } catch (Throwable ignored) {
            }

            try {
                Class<?> gpsClass = Class.forName("com.google.android.gms.common.GooglePlayServicesUtilLight");
                Field zzeField = gpsClass.getDeclaredField("zze");
                zzeField.setAccessible(true);
                Object zzeObj = zzeField.get(null);
                if (zzeObj instanceof AtomicBoolean) {
                    ((AtomicBoolean) zzeObj).set(true);
                }
            } catch (Throwable ignored) {
            }

            try {
                Class<?> zzsClass = Class.forName("com.google.android.gms.common.internal.zzs");
                Field zzbField = zzsClass.getDeclaredField("zzb");
                zzbField.setAccessible(true);
                zzbField.setBoolean(null, true);

                Field zzdField = zzsClass.getDeclaredField("zzd");
                zzdField.setAccessible(true);
                zzdField.setInt(null, 12451000);
            } catch (Throwable ignored) {
            }
        } catch (Throwable ignored) {
        }
    }

    private void postToast(final String msg) {
        Log.d(TAG, msg);
        if (showDebugToasts) {
            mainHandler.post(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void handleError(String tag, Throwable e) {
        String msg = e.getMessage() != null ? e.getMessage() : e.toString();
        Log.e(TAG, tag + ": " + msg, e);
        postToast(tag + ": " + msg);
    }

    private String getErrorMessage(int errorCode) {
        switch (errorCode) {
            case AdRequest.ERROR_CODE_INTERNAL_ERROR:
                return "Internal error";
            case AdRequest.ERROR_CODE_INVALID_REQUEST:
                return "Invalid ad request";
            case AdRequest.ERROR_CODE_NETWORK_ERROR:
                return "Network connection error";
            case AdRequest.ERROR_CODE_NO_FILL:
                return "No fill (No ad returned from server)";
            default:
                return "Error code " + errorCode;
        }
    }

    // ==========================================================
    // Properties
    // ==========================================================

    @SimpleProperty(description = "Extension version.")
    public String Version() {
        return "1.0.0";
    }

    @SimpleFunction(description = "Logs current extension status.")
    public void CheckVersion() {
        Log.d(TAG, "TCBAdMob extension v1.0.0 is active.");
    }

    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_STRING, defaultValue = DEFAULT_APP_ID)
    @SimpleProperty(description = "AdMob Application ID.")
    public void AppId(String id) {
        if (id != null && !id.trim().isEmpty()) {
            this.appId = id.trim();
            injectAppId(this.appId);
        }
    }

    @SimpleProperty(description = "Returns current AdMob Application ID.")
    public String AppId() {
        return this.appId;
    }

    @SimpleProperty(description = "Container (e.g. HorizontalArrangement) where the banner ad is displayed.")
    public void AdContainer(AndroidViewComponent container) {
        this.adContainer = container;
    }

    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_STRING, defaultValue = TEST_BANNER_ID)
    @SimpleProperty(description = "Banner Ad Unit ID.")
    public void AdUnitId(String adUnitId) {
        if (adUnitId != null) {
            this.bannerAdUnitId = adUnitId.trim();
        }
    }

    @SimpleProperty(description = "Returns current Banner Ad Unit ID.")
    public String AdUnitId() {
        return this.bannerAdUnitId;
    }

    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_STRING, defaultValue = "ADAPTIVE")
    @SimpleProperty(description = "Banner format: ADAPTIVE, BANNER, MEDIUM_RECTANGLE, LARGE_BANNER, FULL_BANNER, SMART_BANNER, or RANDOM.")
    public void BannerSize(String size) {
        if (size != null && !size.trim().isEmpty()) {
            this.bannerSize = size.trim().toUpperCase();
        }
    }

    @SimpleProperty(description = "Returns current Banner size format.")
    public String BannerSize() {
        return this.bannerSize;
    }

    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_STRING, defaultValue = "")
    @SimpleProperty(description = "Interstitial Ad Unit ID.")
    public void InterstitialAdUnitId(String adUnitId) {
        if (adUnitId != null) {
            this.interstitialAdUnitId = adUnitId.trim();
        }
    }

    @SimpleProperty(description = "Returns current Interstitial Ad Unit ID.")
    public String InterstitialAdUnitId() {
        return this.interstitialAdUnitId;
    }

    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_STRING, defaultValue = "")
    @SimpleProperty(description = "Rewarded Video Ad Unit ID.")
    public void RewardedVideoAdUnitId(String adUnitId) {
        if (adUnitId != null) {
            this.rewardedVideoAdUnitId = adUnitId.trim();
        }
    }

    @SimpleProperty(description = "Returns current Rewarded Video Ad Unit ID.")
    public String RewardedVideoAdUnitId() {
        return this.rewardedVideoAdUnitId;
    }

    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_STRING, defaultValue = "")
    @SimpleProperty(description = "Rewarded Interstitial Ad Unit ID.")
    public void RewardedInterstitialAdUnitId(String adUnitId) {
        if (adUnitId != null) {
            this.rewardedInterstitialAdUnitId = adUnitId.trim();
        }
    }

    @SimpleProperty(description = "Returns current Rewarded Interstitial Ad Unit ID.")
    public String RewardedInterstitialAdUnitId() {
        return this.rewardedInterstitialAdUnitId;
    }

    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_STRING, defaultValue = "")
    @SimpleProperty(description = "App Open Ad Unit ID.")
    public void AppOpenAdUnitId(String adUnitId) {
        if (adUnitId != null) {
            this.appOpenAdUnitId = adUnitId.trim();
        }
    }

    @SimpleProperty(description = "Returns current App Open Ad Unit ID.")
    public String AppOpenAdUnitId() {
        return this.appOpenAdUnitId;
    }

    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_BOOLEAN, defaultValue = "True")
    @SimpleProperty(description = "Enable TestMode during development to receive test ads.")
    public void TestMode(boolean testMode) {
        this.isTestMode = testMode;
    }

    @SimpleProperty(description = "Returns whether test mode is enabled.")
    public boolean TestMode() {
        return this.isTestMode;
    }

    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_BOOLEAN, defaultValue = "True")
    @SimpleProperty(description = "Falls back to Google test ad if a production ad unit returns NO_FILL.")
    public void FallbackToTestAds(boolean fallback) {
        this.fallbackToTestAds = fallback;
    }

    @SimpleProperty(description = "Returns fallback to test ads setting.")
    public boolean FallbackToTestAds() {
        return this.fallbackToTestAds;
    }

    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_BOOLEAN, defaultValue = "False")
    @SimpleProperty(description = "Display debug toast messages on screen.")
    public void ShowDebugToasts(boolean show) {
        this.showDebugToasts = show;
    }

    @SimpleProperty(description = "Returns whether debug toasts are enabled.")
    public boolean ShowDebugToasts() {
        return this.showDebugToasts;
    }

    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_BOOLEAN, defaultValue = "False")
    @SimpleProperty(description = "Randomizes fullscreen ad formats.")
    public void RandomMode(boolean random) {
        this.randomMode = random;
    }

    @SimpleProperty(description = "Returns random ad format setting.")
    public boolean RandomMode() {
        return this.randomMode;
    }

    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_BOOLEAN, defaultValue = "False")
    @SimpleProperty(description = "Automatically retry loading ads after NO_FILL errors.")
    public void AutoRetryOnNoFill(boolean retry) {
        this.autoRetryOnNoFill = retry;
    }

    @SimpleProperty(description = "Returns auto retry setting.")
    public boolean AutoRetryOnNoFill() {
        return this.autoRetryOnNoFill;
    }

    // ==========================================================
    // 1. Banner Ads
    // ==========================================================

    private AdSize getResolvedAdSize() {
        String size = bannerSize.toUpperCase().trim();
        if ("RANDOM".equals(size)) {
            AdSize[] randomSizes = new AdSize[] {
                    AdSize.BANNER,
                    AdSize.MEDIUM_RECTANGLE,
                    AdSize.LARGE_BANNER,
                    AdSize.FULL_BANNER,
                    getAdaptiveAdSize()
            };
            return randomSizes[random.nextInt(randomSizes.length)];
        }

        switch (size) {
            case "MEDIUM_RECTANGLE":
            case "MREC":
            case "RECTANGLE":
                return AdSize.MEDIUM_RECTANGLE;
            case "LARGE_BANNER":
                return AdSize.LARGE_BANNER;
            case "FULL_BANNER":
                return AdSize.FULL_BANNER;
            case "SMART_BANNER":
            case "SMART":
                return AdSize.SMART_BANNER;
            case "ADAPTIVE":
                return getAdaptiveAdSize();
            case "BANNER":
            default:
                return AdSize.BANNER;
        }
    }

    private AdSize getAdaptiveAdSize() {
        try {
            Display display = activity.getWindowManager().getDefaultDisplay();
            DisplayMetrics outMetrics = new DisplayMetrics();
            display.getMetrics(outMetrics);
            float widthPixels = outMetrics.widthPixels;
            float density = outMetrics.density;
            int adWidth = (int) (widthPixels / density);
            return AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(this.context, adWidth);
        } catch (Throwable t) {
            return AdSize.BANNER;
        }
    }

    @SimpleFunction(description = "Loads and displays the banner ad in the specified AdContainer.")
    public void LoadAd() {
        LoadBannerAd();
    }

    @SimpleFunction(description = "Loads and displays the banner ad in the specified AdContainer.")
    public void LoadBannerAd() {
        if (isCompanion) {
            postToast("AdMob cannot run in AI Companion. Please build APK.");
            return;
        }

        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    if (adContainer == null) {
                        postToast("Please set AdContainer before loading banner ad.");
                        return;
                    }

                    String effectiveId = bannerAdUnitId;
                    if (isTestMode || effectiveId == null || effectiveId.trim().isEmpty()) {
                        effectiveId = TEST_BANNER_ID;
                    }

                    loadBannerInternal(effectiveId.trim());
                } catch (Throwable e) {
                    handleError("Banner load error", e);
                }
            }
        });
    }

    @SimpleFunction(description = "Loads a Medium Rectangle (300x250) banner ad.")
    public void LoadMediumRectangleAd() {
        this.bannerSize = "MEDIUM_RECTANGLE";
        LoadBannerAd();
    }

    @SimpleFunction(description = "Loads an Anchored Adaptive banner ad.")
    public void LoadAdaptiveBannerAd() {
        this.bannerSize = "ADAPTIVE";
        LoadBannerAd();
    }

    @SimpleFunction(description = "Loads a randomly sized banner ad in AdContainer.")
    public void LoadRandomBannerAd() {
        this.bannerSize = "RANDOM";
        LoadBannerAd();
    }

    @SimpleFunction(description = "Removes and destroys the current banner ad.")
    public void DestroyBannerAd() {
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                if (adView != null) {
                    ViewGroup parent = (ViewGroup) adView.getParent();
                    if (parent != null) {
                        parent.removeView(adView);
                    }
                    adView.destroy();
                    adView = null;
                    postToast("Banner ad destroyed.");
                }
            }
        });
    }

    @SimpleFunction(description = "Displays the loaded banner ad.")
    public void ShowBannerAd() {
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                if (adContainer != null) {
                    try {
                        adContainer.Visible(true);
                        View v = adContainer.getView();
                        if (v != null) {
                            v.setVisibility(View.VISIBLE);
                            v.requestLayout();
                            v.invalidate();
                        }
                    } catch (Throwable ignored) {
                    }
                }
                if (adView != null) {
                    try {
                        adView.setVisibility(View.VISIBLE);
                        adView.requestLayout();
                        adView.invalidate();
                    } catch (Throwable ignored) {
                    }
                }
            }
        });
    }

    private void loadBannerInternal(final String targetUnitId) {
        if (adView != null) {
            ViewGroup parent = (ViewGroup) adView.getParent();
            if (parent != null) {
                parent.removeView(adView);
            }
            adView.destroy();
        }

        final String effectiveUnitId = this.isTestMode ? TEST_BANNER_ID : targetUnitId;
        injectAppId(this.appId);
        adView = new AdView(this.context);
        AdSize resolvedSize = getResolvedAdSize();
        adView.setAdSize(resolvedSize);
        adView.setAdUnitId(effectiveUnitId);

        Log.d(TAG, "Requesting banner ad (" + bannerSize + ")...");

        adView.setAdListener(new AdListener() {
            @Override
            public void onAdLoaded() {
                postToast("Banner ad loaded.");
                ShowBannerAd();
                AdLoaded();
            }

            @Override
            public void onAdFailedToLoad(int errorCode) {
                String errorMsg = getErrorMessage(errorCode);
                Log.w(TAG, "Banner ad failed to load (" + errorCode + "): " + errorMsg);
                if (errorCode == AdRequest.ERROR_CODE_NO_FILL && fallbackToTestAds && !TEST_BANNER_ID.equals(effectiveUnitId)) {
                    Log.d(TAG, "Banner returned NO_FILL. Falling back to test banner.");
                    mainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            loadBannerInternal(TEST_BANNER_ID);
                        }
                    });
                    return;
                }
                AdFailedToLoad(errorCode, errorMsg);
                if (errorCode == AdRequest.ERROR_CODE_NO_FILL) {
                    if (!"ADAPTIVE".equals(bannerSize)) {
                        bannerSize = "ADAPTIVE";
                        mainHandler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                if (adContainer != null) {
                                    loadBannerInternal(effectiveUnitId);
                                }
                            }
                        }, 1000L);
                        return;
                    }
                    if (autoRetryOnNoFill) {
                        mainHandler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                if (adContainer != null) {
                                    LoadBannerAd();
                                }
                            }
                        }, 20000L);
                    }
                }
            }

            @Override
            public void onAdOpened() {
                AdOpened();
            }

            @Override
            public void onAdClicked() {
                AdClicked();
            }

            @Override
            public void onAdClosed() {
                AdClosed();
            }
        });

        ViewGroup targetLayout = (ViewGroup) adContainer.getView();
        try {
            targetLayout.removeAllViews();
        } catch (Throwable ignored) {
        }

        int widthPx = resolvedSize.getWidthInPixels(context);
        int heightPx = resolvedSize.getHeightInPixels(context);
        if (widthPx > 0 && heightPx > 0) {
            targetLayout.setMinimumWidth(widthPx);
            targetLayout.setMinimumHeight(heightPx);
        }

        ViewGroup.LayoutParams lp = new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        targetLayout.addView(adView, lp);
        try {
            targetLayout.setVisibility(View.VISIBLE);
            adView.setVisibility(View.VISIBLE);
            targetLayout.requestLayout();
            targetLayout.invalidate();
        } catch (Throwable ignored) {
        }

        AdRequest adRequest = new AdRequest.Builder().build();
        adView.loadAd(adRequest);
    }

    // ==========================================================
    // 2. Interstitial Ads
    // ==========================================================

    @SimpleFunction(description = "Loads an interstitial ad.")
    public void LoadInterstitialAd() {
        if (isCompanion) {
            postToast("AdMob cannot run in AI Companion. Please build APK.");
            return;
        }

        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    String target = interstitialAdUnitId;
                    if (isTestMode || target == null || target.trim().isEmpty()) {
                        target = TEST_INTERSTITIAL_ID;
                    }
                    loadInterstitialInternal(target.trim());
                } catch (Throwable e) {
                    handleError("Interstitial error", e);
                }
            }
        });
    }

    private void loadInterstitialInternal(final String targetUnitId) {
        injectAppId(this.appId);
        interstitialAd = new InterstitialAd(this.context);
        interstitialAd.setAdUnitId(targetUnitId);

        interstitialAd.setAdListener(new AdListener() {
            @Override
            public void onAdLoaded() {
                postToast("Interstitial ad loaded.");
                InterstitialAdLoaded();
            }

            @Override
            public void onAdFailedToLoad(int errorCode) {
                String errorMsg = getErrorMessage(errorCode);
                Log.w(TAG, "Interstitial ad failed (" + errorCode + "): " + errorMsg);
                InterstitialAdFailedToLoad(errorCode, errorMsg);
                if (errorCode == AdRequest.ERROR_CODE_NO_FILL && autoRetryOnNoFill) {
                    mainHandler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            LoadInterstitialAd();
                        }
                    }, 30000L);
                }
            }

            @Override
            public void onAdOpened() {
                InterstitialAdOpened();
            }

            @Override
            public void onAdClosed() {
                InterstitialAdClosed();
                LoadInterstitialAd();
            }
        });

        AdRequest adRequest = new AdRequest.Builder().build();
        interstitialAd.loadAd(adRequest);
    }

    @SimpleFunction(description = "Shows the loaded interstitial ad.")
    public void ShowInterstitialAd() {
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                if (interstitialAd != null && interstitialAd.isLoaded()) {
                    interstitialAd.show();
                } else {
                    postToast("Interstitial ad not ready yet, loading now.");
                    LoadInterstitialAd();
                }
            }
        });
    }

    @SimpleFunction(description = "Checks if the interstitial ad is loaded.")
    public boolean IsInterstitialLoaded() {
        return interstitialAd != null && interstitialAd.isLoaded();
    }

    // ==========================================================
    // 3. Rewarded Video Ads
    // ==========================================================

    private void initRewardedVideo() {
        if (isCompanion) {
            return;
        }

        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    rewardedVideoAd = MobileAds.getRewardedVideoAdInstance(TCBAdMob.this.context);
                    rewardedVideoAd.setRewardedVideoAdListener(new RewardedVideoAdListener() {
                        @Override
                        public void onRewardedVideoAdLoaded() {
                            postToast("Rewarded video loaded.");
                            RewardedVideoLoaded();
                        }

                        @Override
                        public void onRewardedVideoAdOpened() {
                            RewardedVideoOpened();
                        }

                        @Override
                        public void onRewardedVideoStarted() {
                            RewardedVideoStarted();
                        }

                        @Override
                        public void onRewardedVideoAdClosed() {
                            RewardedVideoClosed();
                            LoadRewardedVideoAd();
                        }

                        @Override
                        public void onRewarded(RewardItem rewardItem) {
                            postToast("Reward earned: " + rewardItem.getAmount() + " " + rewardItem.getType());
                            UserEarnedReward(rewardItem.getType(), rewardItem.getAmount());
                        }

                        @Override
                        public void onRewardedVideoAdLeftApplication() {
                        }

                        @Override
                        public void onRewardedVideoAdFailedToLoad(int errorCode) {
                            String errorMsg = getErrorMessage(errorCode);
                            Log.w(TAG, "Rewarded video failed (" + errorCode + "): " + errorMsg);
                            RewardedVideoFailedToLoad(errorCode, errorMsg);
                        }

                        @Override
                        public void onRewardedVideoCompleted() {
                            RewardedVideoCompleted();
                        }
                    });
                } catch (Throwable e) {
                    handleError("Rewarded video init error", e);
                }
            }
        });
    }

    @SimpleFunction(description = "Loads a rewarded video ad.")
    public void LoadRewardedVideoAd() {
        if (isCompanion) {
            postToast("AdMob cannot run in AI Companion. Please build APK.");
            return;
        }

        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    String target = rewardedVideoAdUnitId;
                    if (isTestMode || target == null || target.trim().isEmpty()) {
                        target = TEST_REWARDED_VIDEO_ID;
                    }
                    loadRewardedVideoInternal(target.trim());
                } catch (Throwable e) {
                    handleError("Rewarded video load error", e);
                }
            }
        });
    }

    private void loadRewardedVideoInternal(String targetUnitId) {
        injectAppId(this.appId);
        if (rewardedVideoAd == null) {
            rewardedVideoAd = MobileAds.getRewardedVideoAdInstance(this.context);
        }
        AdRequest adRequest = new AdRequest.Builder().build();
        rewardedVideoAd.loadAd(targetUnitId, adRequest);
    }

    @SimpleFunction(description = "Shows the loaded rewarded video ad.")
    public void ShowRewardedVideoAd() {
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                if (rewardedVideoAd != null && rewardedVideoAd.isLoaded()) {
                    rewardedVideoAd.show();
                } else {
                    postToast("Rewarded video not ready yet, loading now.");
                    LoadRewardedVideoAd();
                }
            }
        });
    }

    @SimpleFunction(description = "Checks if the rewarded video ad is loaded.")
    public boolean IsRewardedVideoLoaded() {
        return rewardedVideoAd != null && rewardedVideoAd.isLoaded();
    }

    // ==========================================================
    // 4. Rewarded Interstitial Ads
    // ==========================================================

    @SimpleFunction(description = "Loads a rewarded interstitial ad.")
    public void LoadRewardedInterstitialAd() {
        if (isCompanion) {
            postToast("AdMob cannot run in AI Companion. Please build APK.");
            return;
        }

        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    String target = rewardedInterstitialAdUnitId;
                    if (isTestMode || target == null || target.trim().isEmpty()) {
                        target = TEST_REWARDED_INTERSTITIAL_ID;
                    }
                    loadRewardedInterstitialInternal(target.trim());
                } catch (Throwable e) {
                    handleError("Rewarded interstitial error", e);
                }
            }
        });
    }

    private void loadRewardedInterstitialInternal(final String targetUnitId) {
        injectAppId(this.appId);
        AdRequest adRequest = new AdRequest.Builder().build();
        RewardedInterstitialAd.load(this.context, targetUnitId, adRequest, new RewardedInterstitialAdLoadCallback() {
            @Override
            public void onRewardedInterstitialAdLoaded(RewardedInterstitialAd ad) {
                rewardedInterstitialAd = ad;
                rewardedInterstitialAd.setFullScreenContentCallback(new FullScreenContentCallback() {
                    @Override
                    public void onAdShowedFullScreenContent() {
                        RewardedInterstitialOpened();
                    }

                    @Override
                    public void onAdDismissedFullScreenContent() {
                        rewardedInterstitialAd = null;
                        RewardedInterstitialClosed();
                        LoadRewardedInterstitialAd();
                    }

                    @Override
                    public void onAdFailedToShowFullScreenContent(AdError adError) {
                        rewardedInterstitialAd = null;
                        Log.w(TAG, "Rewarded interstitial show failed: " + adError.getMessage());
                        RewardedInterstitialClosed();
                    }
                });
                postToast("Rewarded interstitial loaded.");
                RewardedInterstitialLoaded();
            }

            @Override
            public void onRewardedInterstitialAdFailedToLoad(int errorCode) {
                String errorMsg = getErrorMessage(errorCode);
                Log.w(TAG, "Rewarded interstitial failed (" + errorCode + "): " + errorMsg);
                RewardedInterstitialFailedToLoad(errorCode, errorMsg);
            }
        });
    }

    @SimpleFunction(description = "Shows the loaded rewarded interstitial ad.")
    public void ShowRewardedInterstitialAd() {
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                if (rewardedInterstitialAd != null) {
                    rewardedInterstitialAd.show(activity, new OnUserEarnedRewardListener() {
                        @Override
                        public void onUserEarnedReward(com.google.android.gms.ads.rewarded.RewardItem rewardItem) {
                            postToast("Reward earned: " + rewardItem.getAmount() + " " + rewardItem.getType());
                            RewardedInterstitialUserEarnedReward(rewardItem.getType(), rewardItem.getAmount());
                        }
                    });
                } else {
                    postToast("Rewarded interstitial not ready yet, loading now.");
                    LoadRewardedInterstitialAd();
                }
            }
        });
    }

    @SimpleFunction(description = "Checks if the rewarded interstitial ad is loaded.")
    public boolean IsRewardedInterstitialLoaded() {
        return rewardedInterstitialAd != null;
    }

    // ==========================================================
    // 5. App Open Ads
    // ==========================================================

    @SimpleFunction(description = "Loads an App Open ad.")
    public void LoadAppOpenAd() {
        if (isCompanion) {
            postToast("AdMob cannot run in AI Companion. Please build APK.");
            return;
        }

        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    String target = appOpenAdUnitId;
                    if (isTestMode || target == null || target.trim().isEmpty()) {
                        target = TEST_APP_OPEN_ID;
                    }
                    loadAppOpenInternal(target.trim());
                } catch (Throwable e) {
                    handleError("App Open error", e);
                }
            }
        });
    }

    private void loadAppOpenInternal(final String targetUnitId) {
        injectAppId(this.appId);
        AdRequest adRequest = new AdRequest.Builder().build();
        AppOpenAd.load(this.context, targetUnitId, adRequest, AppOpenAd.APP_OPEN_AD_ORIENTATION_PORTRAIT,
                new AppOpenAd.AppOpenAdLoadCallback() {
                    @Override
                    public void onAppOpenAdLoaded(AppOpenAd ad) {
                        appOpenAd = ad;
                        postToast("App Open ad loaded.");
                        AppOpenAdLoaded();
                    }

                    @Override
                    public void onAppOpenAdFailedToLoad(int errorCode) {
                        String errorMsg = getErrorMessage(errorCode);
                        Log.w(TAG, "App Open ad failed (" + errorCode + "): " + errorMsg);
                        AppOpenAdFailedToLoad(errorCode, errorMsg);
                    }
                });
    }

    @SimpleFunction(description = "Shows the loaded App Open ad.")
    public void ShowAppOpenAd() {
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                if (appOpenAd != null) {
                    try {
                        final ViewGroup decorView = (ViewGroup) activity.getWindow().getDecorView();
                        if (appOpenAdView != null) {
                            decorView.removeView(appOpenAdView);
                        }
                        appOpenAdView = new AppOpenAdView(activity);
                        appOpenAdView.setAppOpenAd(appOpenAd);
                        appOpenAdView.setAppOpenAdPresentationCallback(new AppOpenAdPresentationCallback() {
                            @Override
                            public void onAppOpenAdClosed() {
                                decorView.removeView(appOpenAdView);
                                appOpenAdView = null;
                                appOpenAd = null;
                                AppOpenAdClosed();
                                LoadAppOpenAd();
                            }
                        });
                        decorView.addView(appOpenAdView, new ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT));
                        AppOpenAdOpened();
                    } catch (Throwable t) {
                        handleError("Show App Open", t);
                    }
                } else {
                    postToast("App Open ad not ready yet, loading now.");
                    LoadAppOpenAd();
                }
            }
        });
    }

    @SimpleFunction(description = "Checks if the App Open ad is loaded.")
    public boolean IsAppOpenAdLoaded() {
        return appOpenAd != null;
    }

    // ==========================================================
    // 6. Multi-Format Rotator
    // ==========================================================

    @SimpleFunction(description = "Preloads all configured fullscreen ad formats in the background.")
    public void PreloadAllAds() {
        LoadInterstitialAd();
        LoadRewardedVideoAd();
        LoadRewardedInterstitialAd();
        LoadAppOpenAd();
    }

    @SimpleFunction(description = "Selects and displays a loaded fullscreen ad.")
    public void ShowRandomAd() {
        ShowRandomFullscreenAd();
    }

    @SimpleFunction(description = "Randomly selects and displays any loaded fullscreen ad.")
    public void ShowRandomFullscreenAd() {
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                List<String> loadedAds = new ArrayList<String>();
                if (interstitialAd != null && interstitialAd.isLoaded()) {
                    loadedAds.add("INTERSTITIAL");
                }
                if (rewardedVideoAd != null && rewardedVideoAd.isLoaded()) {
                    loadedAds.add("REWARDED_VIDEO");
                }
                if (rewardedInterstitialAd != null) {
                    loadedAds.add("REWARDED_INTERSTITIAL");
                }
                if (appOpenAd != null) {
                    loadedAds.add("APP_OPEN");
                }

                if (!loadedAds.isEmpty()) {
                    int chosenIndex = random.nextInt(loadedAds.size());
                    String chosenAd = loadedAds.get(chosenIndex);
                    RandomAdShown(chosenAd);

                    if ("INTERSTITIAL".equals(chosenAd)) {
                        ShowInterstitialAd();
                    } else if ("REWARDED_VIDEO".equals(chosenAd)) {
                        ShowRewardedVideoAd();
                    } else if ("REWARDED_INTERSTITIAL".equals(chosenAd)) {
                        ShowRewardedInterstitialAd();
                    } else if ("APP_OPEN".equals(chosenAd)) {
                        ShowAppOpenAd();
                    }
                } else {
                    postToast("No ad currently cached. Preloading now.");
                    PreloadAllAds();
                }
            }
        });
    }

    @SimpleFunction(description = "Loads one random fullscreen ad format in the background.")
    public void LoadRandomFullscreenAd() {
        int pick = random.nextInt(4);
        switch (pick) {
            case 0:
                LoadInterstitialAd();
                break;
            case 1:
                LoadRewardedVideoAd();
                break;
            case 2:
                LoadRewardedInterstitialAd();
                break;
            case 3:
            default:
                LoadAppOpenAd();
                break;
        }
    }

    @SimpleFunction(description = "Starts automatic random ad display at specified interval (minimum 15 seconds).")
    public void StartAutoRandomAds(final int intervalSeconds) {
        StopAutoRandomAds();
        this.autoIntervalSeconds = Math.max(15, intervalSeconds);
        PreloadAllAds();
        autoRandomAdRunnable = new Runnable() {
            @Override
            public void run() {
                ShowRandomFullscreenAd();
                mainHandler.postDelayed(this, autoIntervalSeconds * 1000L);
            }
        };
        mainHandler.postDelayed(autoRandomAdRunnable, autoIntervalSeconds * 1000L);
    }

    @SimpleFunction(description = "Stops the automatic random ad timer.")
    public void StopAutoRandomAds() {
        if (autoRandomAdRunnable != null) {
            mainHandler.removeCallbacks(autoRandomAdRunnable);
            autoRandomAdRunnable = null;
        }
    }

    // ==========================================================
    // Events
    // ==========================================================

    @SimpleEvent(description = "Triggered when a random ad is selected and shown.")
    public void RandomAdShown(String adType) {
        EventDispatcher.dispatchEvent(this, "RandomAdShown", adType);
    }

    @SimpleEvent(description = "Triggered when a banner ad is loaded.")
    public void AdLoaded() {
        EventDispatcher.dispatchEvent(this, "AdLoaded");
    }

    @SimpleEvent(description = "Triggered when a banner ad fails to load.")
    public void AdFailedToLoad(int errorCode, String errorMessage) {
        EventDispatcher.dispatchEvent(this, "AdFailedToLoad", errorCode, errorMessage);
    }

    @SimpleEvent(description = "Triggered when a banner ad is opened.")
    public void AdOpened() {
        EventDispatcher.dispatchEvent(this, "AdOpened");
    }

    @SimpleEvent(description = "Triggered when a banner ad is closed.")
    public void AdClosed() {
        EventDispatcher.dispatchEvent(this, "AdClosed");
    }

    @SimpleEvent(description = "Triggered when a banner ad is clicked.")
    public void AdClicked() {
        EventDispatcher.dispatchEvent(this, "AdClicked");
    }

    @SimpleEvent(description = "Triggered when an interstitial ad is loaded.")
    public void InterstitialAdLoaded() {
        EventDispatcher.dispatchEvent(this, "InterstitialAdLoaded");
    }

    @SimpleEvent(description = "Triggered when an interstitial ad fails to load.")
    public void InterstitialAdFailedToLoad(int errorCode, String errorMessage) {
        EventDispatcher.dispatchEvent(this, "InterstitialAdFailedToLoad", errorCode, errorMessage);
    }

    @SimpleEvent(description = "Triggered when an interstitial ad is opened.")
    public void InterstitialAdOpened() {
        EventDispatcher.dispatchEvent(this, "InterstitialAdOpened");
    }

    @SimpleEvent(description = "Triggered when an interstitial ad is closed.")
    public void InterstitialAdClosed() {
        EventDispatcher.dispatchEvent(this, "InterstitialAdClosed");
    }

    @SimpleEvent(description = "Triggered when a rewarded video ad is loaded.")
    public void RewardedVideoLoaded() {
        EventDispatcher.dispatchEvent(this, "RewardedVideoLoaded");
    }

    @SimpleEvent(description = "Triggered when a rewarded video ad fails to load.")
    public void RewardedVideoFailedToLoad(int errorCode, String errorMessage) {
        EventDispatcher.dispatchEvent(this, "RewardedVideoFailedToLoad", errorCode, errorMessage);
    }

    @SimpleEvent(description = "Triggered when a rewarded video ad is opened.")
    public void RewardedVideoOpened() {
        EventDispatcher.dispatchEvent(this, "RewardedVideoOpened");
    }

    @SimpleEvent(description = "Triggered when a rewarded video ad starts playback.")
    public void RewardedVideoStarted() {
        EventDispatcher.dispatchEvent(this, "RewardedVideoStarted");
    }

    @SimpleEvent(description = "Triggered when a rewarded video ad completes playback.")
    public void RewardedVideoCompleted() {
        EventDispatcher.dispatchEvent(this, "RewardedVideoCompleted");
    }

    @SimpleEvent(description = "Triggered when a user earns a reward from a rewarded video.")
    public void UserEarnedReward(String rewardType, int rewardAmount) {
        EventDispatcher.dispatchEvent(this, "UserEarnedReward", rewardType, rewardAmount);
    }

    @SimpleEvent(description = "Triggered when a rewarded video ad is closed.")
    public void RewardedVideoClosed() {
        EventDispatcher.dispatchEvent(this, "RewardedVideoClosed");
    }

    @SimpleEvent(description = "Triggered when a rewarded interstitial ad is loaded.")
    public void RewardedInterstitialLoaded() {
        EventDispatcher.dispatchEvent(this, "RewardedInterstitialLoaded");
    }

    @SimpleEvent(description = "Triggered when a rewarded interstitial ad fails to load.")
    public void RewardedInterstitialFailedToLoad(int errorCode, String errorMessage) {
        EventDispatcher.dispatchEvent(this, "RewardedInterstitialFailedToLoad", errorCode, errorMessage);
    }

    @SimpleEvent(description = "Triggered when a rewarded interstitial ad is opened.")
    public void RewardedInterstitialOpened() {
        EventDispatcher.dispatchEvent(this, "RewardedInterstitialOpened");
    }

    @SimpleEvent(description = "Triggered when a rewarded interstitial ad is closed.")
    public void RewardedInterstitialClosed() {
        EventDispatcher.dispatchEvent(this, "RewardedInterstitialClosed");
    }

    @SimpleEvent(description = "Triggered when a user earns a reward from a rewarded interstitial ad.")
    public void RewardedInterstitialUserEarnedReward(String rewardType, int rewardAmount) {
        EventDispatcher.dispatchEvent(this, "RewardedInterstitialUserEarnedReward", rewardType, rewardAmount);
    }

    @SimpleEvent(description = "Triggered when an App Open ad is loaded.")
    public void AppOpenAdLoaded() {
        EventDispatcher.dispatchEvent(this, "AppOpenAdLoaded");
    }

    @SimpleEvent(description = "Triggered when an App Open ad fails to load.")
    public void AppOpenAdFailedToLoad(int errorCode, String errorMessage) {
        EventDispatcher.dispatchEvent(this, "AppOpenAdFailedToLoad", errorCode, errorMessage);
    }

    @SimpleEvent(description = "Triggered when an App Open ad is opened.")
    public void AppOpenAdOpened() {
        EventDispatcher.dispatchEvent(this, "AppOpenAdOpened");
    }

    @SimpleEvent(description = "Triggered when an App Open ad is closed.")
    public void AppOpenAdClosed() {
        EventDispatcher.dispatchEvent(this, "AppOpenAdClosed");
    }
}

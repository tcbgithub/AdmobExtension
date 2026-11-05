# TCBAdMob - Google AdMob Extension for MIT App Inventor

[![GitHub License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)
[![Platform](https://img.shields.io/badge/Platform-MIT%20App%20Inventor%20%7C%20Kodular%20%7C%20Niotron-orange)](https://appinventor.mit.edu)
[![AdMob SDK](https://img.shields.io/badge/Google%20Mobile%20Ads-v21.0.0-green.svg)](https://developers.google.com/admob)
[![YouTube Tutorial](https://img.shields.io/badge/YouTube-Video%20Tutorial-red?logo=youtube)](https://youtu.be/0rENbzBemRU?si=Ve-Bmj7cH0JZEX_L)
[![Website](https://img.shields.io/badge/Website-thecodingbus.info-blue?logo=googlechrome)](https://thecodingbus.info/)

A production-ready Google Mobile Ads (AdMob) extension for **MIT App Inventor**, **Kodular**, and **Niotron**.

---

## 📺 Video Tutorial & Guide

Watch the complete step-by-step video tutorial covering setup, block configuration, and live testing:

[![Watch the AdMob Extension Tutorial](https://img.youtube.com/vi/0rENbzBemRU/maxresdefault.jpg)](https://youtu.be/0rENbzBemRU?si=Ve-Bmj7cH0JZEX_L)

👉 **[Watch Tutorial on YouTube](https://youtu.be/0rENbzBemRU?si=Ve-Bmj7cH0JZEX_L)**

---

## 🚀 Key Features

- **Anchored Adaptive Banners**: Automatically calculates the optimal banner size matching the user's screen width and device density for maximum eCPM and fill rate.
- **Multiple Banner Formats**: Standard (320x50), Medium Rectangle (300x250 MREC), Large Banner (320x100), Full Banner (468x60), and Smart Banners.
- **Fullscreen Interstitial Ads**: Preloadable interstitial ads that display full-screen between app activities or level completions.
- **Rewarded Video Ads**: Rewarded ads with playback lifecycle callbacks (`UserEarnedReward`, `RewardedVideoCompleted`, `RewardedVideoStarted`).
- **Rewarded Interstitial Ads**: Next-generation full-screen rewarded format that doesn't require opt-in countdowns.
- **App Open Ads**: Splash/launch ads designed to monetize app open or app resume experiences.
- **Multi-Format Rotator**: Intelligent rotator engine that caches and displays available full-screen ads with optional auto-interval scheduling.
- **Zero Companion Crash / In-Process Bypass**: Dynamically injects `APPLICATION_ID` into the Android runtime to satisfy Google Play services requirements on App Inventor APK builds.

---

## 📁 Repository Contents

```
├── TCBAdMob.aix          # Ready-to-use compiled extension
├── TCBAdMob_Sample.aia   # Full demo project with UI & Blocks
├── build.py              # Self-contained build script (source -> AIX + AIA)
├── icon.png              # Extension icon (128x128)
├── LICENSE               # MIT Open Source License
├── README.md             # Complete documentation
└── admob_src/            # Clean Java source code
    ├── rush.yml          # Rush extension specification
    ├── deps/             # Google Play services lite jars & aars
    └── src/
        ├── AndroidManifest.xml
        └── com/thecodingbus/admob/
            └── TCBAdMob.java
```

---

## 📥 Quick Start Guide

### 1. Import the Extension (.aix)

1. Download **[`TCBAdMob.aix`](TCBAdMob.aix)** from this repository.
2. In your MIT App Inventor project designer, go to the **Palette** on the left.
3. Scroll to the bottom, open the **Extension** category, and click **Import extension**.
4. Choose the downloaded `TCBAdMob.aix` file and click **Import**.
5. Drag and drop **TCBAdMob** onto your screen (it is a non-visible component).

### 2. Run the Sample Demo (.aia)

1. Download **[`TCBAdMob_Sample.aia`](TCBAdMob_Sample.aia)**.
2. In MIT App Inventor, navigate to **Projects** -> **Import project (.aia) from my computer**.
3. Select `TCBAdMob_Sample.aia`.
4. The sample project includes a complete user interface with buttons to test:
   - **Load / Show Banner Ad** (inside a responsive layout container)
   - **Load / Show Interstitial Ad**
   - **Load / Show Rewarded Video Ad** (with real-time reward counter)
   - **Load / Show App Open Ad**

> ⚠️ **Important Testing Note**:
> Google AdMob SDK uses native Android libraries that are not supported in the live **MIT AI Companion** app. To test ads properly, always build the APK via **Build** -> **Android App (.apk)** and install it on your device or an Android emulator.

---

## 🧪 Official Google AdMob Test Credentials

During testing and development, leave `TestMode = True` or use Google's official public test IDs:

| Format | Official Google Test Unit ID |
|---|---|
| **AdMob App ID** | `ca-app-pub-3940256099942544~3347511713` |
| **Adaptive Banner** | `ca-app-pub-3940256099942544/6300978111` |
| **Interstitial** | `ca-app-pub-3940256099942544/1033173712` |
| **Rewarded Video** | `ca-app-pub-3940256099942544/5224354917` |
| **Rewarded Interstitial** | `ca-app-pub-3940256099942544/5354046379` |
| **App Open** | `ca-app-pub-3940256099942544/3419835294` |

When switching to production:
1. Replace `AppId` with your real AdMob App ID from your Google AdMob console (`ca-app-pub-xxxxxxxxxxxxxxxx~yyyyyyyyyy`).
2. Replace each Ad Unit ID with your real unit IDs.
3. Set `TestMode` to `False`.

---

## 🧩 Blocks & API Reference

### Designer Properties

| Property | Type | Default | Description |
|---|---|---|---|
| `AppId` | Text | `ca-app-pub-3940...` | Your Google AdMob Application ID |
| `AdUnitId` | Text | `ca-app-pub-3940...` | Banner Ad Unit ID |
| `BannerSize` | Text | `ADAPTIVE` | Format: `ADAPTIVE`, `BANNER`, `MEDIUM_RECTANGLE`, `LARGE_BANNER`, `FULL_BANNER`, `SMART_BANNER`, or `RANDOM` |
| `InterstitialAdUnitId` | Text | `""` | Interstitial Ad Unit ID |
| `RewardedVideoAdUnitId` | Text | `""` | Rewarded Video Ad Unit ID |
| `RewardedInterstitialAdUnitId` | Text | `""` | Rewarded Interstitial Ad Unit ID |
| `AppOpenAdUnitId` | Text | `""` | App Open Ad Unit ID |
| `TestMode` | Boolean | `True` | Serves official Google test ads for immediate verification |
| `FallbackToTestAds` | Boolean | `True` | Automatically requests a Google test banner if a real ad gets `NO_FILL` |
| `ShowDebugToasts` | Boolean | `False` | Displays on-screen toast notifications for debugging |
| `AutoRetryOnNoFill` | Boolean | `False` | Automatically retries requesting inventory after NO_FILL errors |

---

### Methods

#### 1. Banner Ads
- `AdContainer(container)`: Assigns a layout arrangement (e.g. `HorizontalArrangement`) as the banner host container.
- `LoadBannerAd()`: Loads the banner ad into the assigned container.
- `ShowBannerAd()`: Makes the loaded banner visible.
- `DestroyBannerAd()`: Destroys the current banner and frees memory.
- `LoadAdaptiveBannerAd()`: Sets `BannerSize` to `ADAPTIVE` and triggers load.
- `LoadMediumRectangleAd()`: Sets `BannerSize` to `MEDIUM_RECTANGLE` (300x250) and triggers load.
- `LoadRandomBannerAd()`: Randomly selects a banner format and triggers load.

#### 2. Fullscreen Interstitial Ads
- `LoadInterstitialAd()`: Requests an interstitial ad in the background.
- `ShowInterstitialAd()`: Displays the cached interstitial ad.
- `IsInterstitialLoaded()`: Returns `True` if an interstitial ad is cached and ready.

#### 3. Rewarded Video Ads
- `LoadRewardedVideoAd()`: Preloads a rewarded video ad.
- `ShowRewardedVideoAd()`: Plays the rewarded video ad.
- `IsRewardedVideoLoaded()`: Returns `True` if a rewarded video is ready.

#### 4. Rewarded Interstitial Ads
- `LoadRewardedInterstitialAd()`: Preloads a rewarded interstitial ad.
- `ShowRewardedInterstitialAd()`: Displays the rewarded interstitial ad.
- `IsRewardedInterstitialLoaded()`: Returns `True` if a rewarded interstitial is ready.

#### 5. App Open Ads
- `LoadAppOpenAd()`: Preloads an App Open ad.
- `ShowAppOpenAd()`: Displays the App Open splash screen ad.
- `IsAppOpenAdLoaded()`: Returns `True` if an App Open ad is ready.

#### 6. Multi-Format Rotator
- `PreloadAllAds()`: Preloads all configured fullscreen formats concurrently.
- `ShowRandomFullscreenAd()`: Selects and shows any currently loaded fullscreen ad at random.
- `StartAutoRandomAds(intervalSeconds)`: Rotates random ads automatically (minimum interval 15 seconds).
- `StopAutoRandomAds()`: Stops the rotation timer.

---

### Events

| Event | Parameters | Description |
|---|---|---|
| `AdLoaded` | — | Fired when a banner ad loads successfully |
| `AdFailedToLoad` | `errorCode` (int), `errorMessage` (text) | Fired when banner loading fails |
| `AdOpened` | — | Fired when a banner ad is tapped |
| `AdClosed` | — | Fired when user returns from a banner click |
| `AdClicked` | — | Fired when a banner ad click is registered |
| `InterstitialAdLoaded` | — | Fired when an interstitial ad is ready to show |
| `InterstitialAdFailedToLoad` | `errorCode` (int), `errorMessage` (text) | Fired when interstitial loading fails |
| `InterstitialAdOpened` | — | Fired when interstitial is shown on screen |
| `InterstitialAdClosed` | — | Fired when interstitial is dismissed |
| `RewardedVideoLoaded` | — | Fired when rewarded video is ready |
| `RewardedVideoFailedToLoad` | `errorCode` (int), `errorMessage` (text) | Fired when rewarded video loading fails |
| `RewardedVideoOpened` | — | Fired when rewarded video begins showing |
| `RewardedVideoStarted` | — | Fired when rewarded video playback starts |
| `RewardedVideoCompleted` | — | Fired when video playback finishes |
| `UserEarnedReward` | `rewardType` (text), `rewardAmount` (int) | Fired when user earns their reward |
| `RewardedVideoClosed` | — | Fired when rewarded video is dismissed |
| `RewardedInterstitialLoaded` | — | Fired when rewarded interstitial is ready |
| `RewardedInterstitialFailedToLoad` | `errorCode` (int), `errorMessage` (text) | Fired when rewarded interstitial fails |
| `RewardedInterstitialOpened` | — | Fired when rewarded interstitial is shown |
| `RewardedInterstitialClosed` | — | Fired when rewarded interstitial is dismissed |
| `RewardedInterstitialUserEarnedReward` | `rewardType` (text), `rewardAmount` (int) | Fired when reward is earned |
| `AppOpenAdLoaded` | — | Fired when App Open ad is ready |
| `AppOpenAdFailedToLoad` | `errorCode` (int), `errorMessage` (text) | Fired when App Open ad fails to load |
| `AppOpenAdOpened` | — | Fired when App Open ad is displayed |
| `AppOpenAdClosed` | — | Fired when App Open ad is closed |
| `RandomAdShown` | `adType` (text) | Fired when rotator shows an ad |

---

## 🛠️ Building from Source

To compile `TCBAdMob.aix` and re-generate `TCBAdMob_Sample.aia` locally:

```bash
python3 build.py
```

### Build Requirements
- Python 3.6+
- Java JDK 8 or higher (`javac`)
- Android dev dependencies (`android.jar`, `d8.jar`)

---

## 📄 License

This project is licensed under the [MIT License](LICENSE) — see the LICENSE file for details.

Developed with ❤️ by **The Coding Bus Team** ([thecodingbus.info](https://thecodingbus.info)).

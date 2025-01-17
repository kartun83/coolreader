package org.coolreader.crengine;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

import android.app.Application;
import android.graphics.PixelFormat;
import android.os.Build;
import android.util.Log;

import com.onyx.android.sdk.api.device.FrontLightController;
import com.onyx.android.sdk.device.BaseDevice;

import org.coolreader.utils.StrUtils;
import org.eink_onyx_reflections.OnyxDevice;
import org.eink_onyx_reflections.OnyxEinkDeviceImpl;

public class DeviceInfo {

	public final static String MANUFACTURER;
	public final static String MODEL;
	public final static String DEVICE;
	public final static String PRODUCT;
	public final static String BRAND;
	public final static int MIN_SCREEN_BRIGHTNESS_VALUE;
	public final static int MAX_SCREEN_BRIGHTNESS_VALUE_100;
	public final static int MAX_SCREEN_BRIGHTNESS_VALUE;
	public final static int MAX_SCREEN_BRIGHTNESS_WARM_VALUE;
	public final static boolean SAMSUNG_BUTTONS_HIGHLIGHT_PATCH;
	public final static boolean EINK_SCREEN;
	public final static boolean SCREEN_CAN_CONTROL_BRIGHTNESS;
	public final static boolean ONYX_BRIGHTNESS_FILE;
	public final static boolean ONYX_BRIGHTNESS_WARM_FILE;
	public final static boolean EINK_SCREEN_REGAL;
	public final static boolean EINK_HAVE_FRONTLIGHT;
	public final static boolean EINK_HAVE_NATURAL_BACKLIGHT;
	public final static boolean EINK_SCREEN_UPDATE_MODES_SUPPORTED;
	public final static boolean NOOK_NAVIGATION_KEYS;
	public final static boolean EINK_NOOK1;
	public final static boolean EINK_NOOK2;
	public final static boolean EINK_NOOK;
	public final static boolean EINK_NOOK_120;
	public final static boolean EINK_ONYX;
	public final static boolean EINK_DNS;
	public final static boolean EINK_TOLINO;
	private final static boolean FORCE_HC_THEME;
	public final static boolean EINK_SONY;
	public final static boolean EINK_ENERGYSYSTEM;
	public final static boolean SONY_NAVIGATION_KEYS;
	private final static boolean USE_CUSTOM_TOAST;
	public final static boolean AMOLED_SCREEN;
	public final static boolean POCKETBOOK;
	public final static boolean ONYX_BUTTONS_LONG_PRESS_NOT_AVAILABLE;
	public final static boolean ONYX_HAVE_FRONTLIGHT;
	public final static boolean ONYX_HAVE_NATURAL_BACKLIGHT;
	public final static boolean ONYX_HAVE_BRIGHTNESS_SYSTEM_DIALOG;
	public final static boolean ONYX_HAVE_COLOR;
	public final static boolean NOFLIBUSTA;
	public final static boolean NAVIGATE_LEFTRIGHT; // map left/right keys to single page flip
	public final static boolean REVERT_LANDSCAPE_VOLUME_KEYS; // revert volume keys in landscape mode
	private final static android.graphics.Bitmap.Config BUFFER_COLOR_FORMAT;
	public final static boolean USE_OPENGL = true;
	private final static int PIXEL_FORMAT;
	public final static String  DEF_FONT_FACE;
	public final static boolean USE_BITMAP_MEMORY_HACK; // revert volume keys in landscape mode
	public final static Integer DEF_FONT_SIZE;
	public final static boolean ONE_COLUMN_IN_LANDSCAPE;

	public static boolean isEinkScreen(boolean isEinkFromSettings) {
		return EINK_SCREEN || isEinkFromSettings;
	}

	public static boolean isBlackAndWhiteEinkScreen(boolean isEinkFromSettings) {
		return (EINK_SCREEN || isEinkFromSettings) && (!ONYX_HAVE_COLOR);
	}

	public static boolean isForceHCTheme(boolean isEinkFromSettings) {
		return (FORCE_HC_THEME || isEinkFromSettings) && (!ONYX_HAVE_COLOR);
		//return false;
	}

	public static boolean isUseCustomToast(boolean isEinkFromSettings) {
		boolean custToast = USE_CUSTOM_TOAST || isEinkFromSettings;
		if (getSDKLevel() >= DeviceInfo.KITKAT) return false;
		return custToast;
	}

	public static android.graphics.Bitmap.Config getBufferColorFormat(boolean isEinkFromSettings) {
		return isBlackAndWhiteEinkScreen(isEinkFromSettings) || USE_OPENGL ? android.graphics.Bitmap.Config.ARGB_8888 : android.graphics.Bitmap.Config.RGB_565;
	}

	public static int getPixelFormat(boolean isEinkFromSettings) {
		return (getBufferColorFormat(isEinkFromSettings) == android.graphics.Bitmap.Config.RGB_565) ? PixelFormat.RGB_565 : PixelFormat.RGBA_8888;
	}

	// minimal screen backlight level percent for different devices
	private static final String[] MIN_SCREEN_BRIGHTNESS_DB = {
		"LGE;LG-P500",       "6", // LG Optimus One
		"samsung;GT-I9003",  "6", // Samsung i9003
		"Samsung;GT-I9000",  "1", // Samsung Galaxy S
		"Samsung;GT-I9100",  "1", // Samsung Galaxy S2
		"Samsung;GT-I9300",  "1", // Samsung Galaxy S3
		"Samsung;GT-I9500",  "1", // Samsung Galaxy S4
		"HTC;HTC EVO 3D*",   "1", // HTC EVO
		"Archos;A70S",       "1", // Archos
		"HTC;HTC Desire",    "6", // HTC Desire
		"HTC;HTC Desire S",  "6",
		"HTC;HTC Incredible*","6",// HTC Incredible, HTC Incredible S
		"HTC;Legend",        "6",
		"LGE;LG-E510",       "6",
		"*;Kindle Fire",     "6",
		"Samsung;GT-S5830",  "6",
		"HUAWEI;U8800",      "6",
		"Motorola;Milestone XT720", "6",
		"Foxconn;PocketBook A10", "3",
		"*;*;*;tolino",	     "1",
		// TODO: more devices here
	};

	public final static int HONEYCOMB = 11;
	public final static int ICE_CREAM_SANDWICH = 14;
	public final static int KITKAT = 19;
	public final static int LOLLIPOP_5_0 = 21;

	private static int sdkInt = 0;
	public static int getSDKLevel() {
		if (sdkInt > 0)
			return sdkInt;
		// hack for Android 1.5
		sdkInt = 3;
		Field fld;
		try {
			Class<?> cl = android.os.Build.VERSION.class;
			fld = cl.getField("SDK_INT");
			sdkInt = fld.getInt(cl);
			Log.i("cr3", "API LEVEL " + sdkInt + " detected");
		} catch (SecurityException e) {
			// ignore
		} catch (NoSuchFieldException e) {
			// ignore
		} catch (IllegalArgumentException e) {
			// ignore
		} catch (IllegalAccessException e) {
			// ignore
		}
		return sdkInt;
	}
	
	public static boolean supportsActionBar() {
		return getSDKLevel() >= HONEYCOMB;
	}
	
	static {
		MANUFACTURER = StrUtils.getNonEmptyStr(getBuildField("MANUFACTURER"), false);
		MODEL = StrUtils.getNonEmptyStr(getBuildField("MODEL"), false);
		DEVICE = StrUtils.getNonEmptyStr(getBuildField("DEVICE"), false);
		PRODUCT = StrUtils.getNonEmptyStr(getBuildField("PRODUCT"), false);
		BRAND = StrUtils.getNonEmptyStr(getBuildField("BRAND"), false);
		SAMSUNG_BUTTONS_HIGHLIGHT_PATCH = MANUFACTURER.toLowerCase().contentEquals("samsung") &&
		        (MODEL.contentEquals("GT-S5830") || MODEL.contentEquals("GT-S5660")); // More models?
		AMOLED_SCREEN = MANUFACTURER.toLowerCase().contentEquals("samsung") &&
        		(MODEL.toLowerCase().startsWith("gt-i")); // AMOLED screens: GT-IXXXX
		EINK_NOOK1 = MANUFACTURER.toLowerCase().contentEquals("barnesandnoble") &&
				(PRODUCT.contentEquals("NOOK") || MODEL.contentEquals("NOOK") || MODEL.contentEquals("BNRV350") ||
						MODEL.contentEquals("BNRV300") || MODEL.contentEquals("BNRV500") ||
						MODEL.contentEquals("BNRV510") || MODEL.contentEquals("BNRV520")|| MODEL.contentEquals("BNRV700")) &&
				(DEVICE.toLowerCase().contentEquals("zoom2") || DEVICE.toLowerCase().contentEquals("ntx_6sl"));
		EINK_NOOK2 = DEVICE.toLowerCase().contentEquals("ntx_6sl");
		EINK_NOOK = EINK_NOOK1 || EINK_NOOK2;
		EINK_NOOK_120 = EINK_NOOK && (MODEL.contentEquals("BNRV350") || MODEL.contentEquals("BNRV300") ||
				MODEL.contentEquals("BNRV500") || MODEL.contentEquals("BNRV510") || MODEL.contentEquals("BNRV520")
				|| MODEL.contentEquals("BNRV700"));
		EINK_SONY = MANUFACTURER.toLowerCase().contentEquals("sony") && MODEL.startsWith("PRS-T");
		//MANUFACTURER=Onyx, MODEL=*; All ONYX BOOX Readers have e-ink screen
		EINK_ONYX = (MANUFACTURER.toLowerCase().contentEquals("onyx")
						|| MANUFACTURER.toLowerCase().contentEquals("onyx-intl")
						|| MANUFACTURER.toLowerCase().contentEquals("qualcomm"))
					&&
					(BRAND.toLowerCase().contentEquals("onyx")
						|| BRAND.toLowerCase().contentEquals("maccentre")
						|| BRAND.toLowerCase().contentEquals("maccenter")) &&
				MODEL.length() > 0;
		ONYX_HAVE_COLOR = MODEL.toLowerCase().startsWith("novaair") || MODEL.toLowerCase().contains("color");
		EINK_ENERGYSYSTEM = (
			(BRAND.toLowerCase().contentEquals("energysistem")||BRAND.toLowerCase().contentEquals("energysystem")) &&  MODEL.toLowerCase().startsWith("ereader"));
		//MANUFACTURER -DNS, DEVICE -BK6004C, MODEL - DNS Airbook EGH602, PRODUCT - BK6004C
		EINK_DNS = MANUFACTURER.toLowerCase().contentEquals("dns") && MODEL.startsWith("DNS Airbook EGH");

		EINK_TOLINO = (BRAND.toLowerCase().contentEquals("tolino") && (MODEL.toLowerCase().contentEquals("imx50_rdp")) ) || 		// SHINE
				(MODEL.toLowerCase().contentEquals("tolino") && DEVICE.toLowerCase().contentEquals("tolino_vision2")); //Tolino Vision HD4 doesn't show any Brand, only Model=tolino and  DEVICE=tolino_vision2)


		EINK_SCREEN = EINK_SONY || EINK_NOOK || EINK_ONYX || EINK_ENERGYSYSTEM || EINK_DNS || EINK_TOLINO; // TODO: set to true for eink devices like Nook Touch

		boolean b1 = false;
		boolean b2 = false;
		try {
			File f1 = new File("/sys/class/backlight/white/brightness");
			File f2 = new File("/sys/class/backlight/warm/brightness");
			b1 = f1.exists();
			b2 = f2.exists();
		} catch (Exception e){
		}
		ONYX_BRIGHTNESS_FILE = b1;
		ONYX_BRIGHTNESS_WARM_FILE = b2;
		// On Onyx Boox Monte Cristo 3 (and possible Monte Cristo, Monte Cristo 2) long press action on buttons are catch by system and not available for application
		// TODO: check this on other ONYX BOOX Readers
		ONYX_BUTTONS_LONG_PRESS_NOT_AVAILABLE = EINK_ONYX;
		boolean onyx_have_frontlight = false;
		boolean onyx_have_natural_backlight = false;
		int onyx_max_screen_brightness_value = 100;
		int onyx_max_screen_brightness_warm_value = 100;
		boolean onyx_support_regal = false;
		boolean onyx_have_brightness_system_dialog = false;
		if (EINK_ONYX) {
			//OnyxEinkDeviceImpl onyxEinkDevice = OnyxDevice.currentDevice();
			//onyx_support_regal = onyxEinkDevice.supportRegal();
			BaseDevice curDev = com.onyx.android.sdk.device.Device.currentDevice();
			onyx_support_regal = curDev.supportRegal();
			Application app = null;
			try {
				Class<?> clazz = Class.forName("android.app.ActivityThread");
				Method method = clazz.getMethod("currentApplication");
				app = (Application) method.invoke(null);
			} catch (Exception ignored) {}
			if (null != app) {
				//onyx_have_frontlight = onyxEinkDevice.hasFLBrightness(app);
				onyx_have_frontlight = FrontLightController.hasFLBrightness(app);
				List<Integer> list = null;
				try {
					//list = onyxEinkDevice.getFrontLightValueList(app);
					list = FrontLightController.getFrontLightValueList(app);
				} catch (Exception ignored) {}
				if (list != null && list.size() > 0) {
					onyx_max_screen_brightness_value = list.get(list.size() - 1);
					if (!onyx_have_frontlight) {
						// For ONYX BOOX MC3 and may be other too...
						onyx_have_frontlight = true;
					}
				}
				// natural (cold & warm) backlight support
				//onyx_have_natural_backlight = onyxEinkDevice.hasCTMBrightness(app);
				onyx_have_natural_backlight = FrontLightController.hasCTMBrightness(app);
				if (onyx_have_natural_backlight) {
					//list = onyxEinkDevice.getWarmLightValues(app);
					Integer[] list1 = FrontLightController.getWarmLightValues(app);
					if (list1.length > 1)
						list = Arrays.asList(list1);
					if (list != null && list.size() > 0) {
						onyx_max_screen_brightness_warm_value = list.get(list.size() - 1);
					}
				}
				if (!onyx_have_frontlight && onyx_have_natural_backlight) {
					onyx_have_frontlight = true;
					//list = onyxEinkDevice.getColdLightValues(app);
					Integer[] list1 = FrontLightController.getColdLightValues(app);
					if (list1.length > 1)
						list = Arrays.asList(list1);
					if (list != null && list.size() > 0) {
						onyx_max_screen_brightness_value = list.get(list.size() - 1);
					}
				}
			}
			//switch (OnyxDevice.currentDeviceType()) {
			switch (com.onyx.android.sdk.device.Device.currentDeviceIndex()) {
				case Rk31xx:
				case Rk32xx:
				case Rk33xx:
				case SDM:
					onyx_have_brightness_system_dialog = true;
					break;
			}
		}
		ONYX_HAVE_BRIGHTNESS_SYSTEM_DIALOG = onyx_have_brightness_system_dialog;
		ONYX_HAVE_FRONTLIGHT = onyx_have_frontlight;

		MAX_SCREEN_BRIGHTNESS_VALUE_100 = 100; //KR
		ONYX_HAVE_NATURAL_BACKLIGHT = onyx_have_natural_backlight;
		MAX_SCREEN_BRIGHTNESS_VALUE = onyx_max_screen_brightness_value;
		MAX_SCREEN_BRIGHTNESS_WARM_VALUE = onyx_max_screen_brightness_warm_value;

		EINK_SCREEN_REGAL = onyx_support_regal;		// TODO: add other e-ink devices with regal support

		EINK_HAVE_FRONTLIGHT = ONYX_HAVE_FRONTLIGHT; // TODO: add other e-ink devices with frontlight support
		EINK_HAVE_NATURAL_BACKLIGHT = ONYX_HAVE_NATURAL_BACKLIGHT;	// TODO: add other e-ink devices with natural backlight support

		SCREEN_CAN_CONTROL_BRIGHTNESS =
				(
					(!EINK_SCREEN) ||
					(EINK_NOOK && //DEVICE.toLowerCase().contentEquals("ntx_6sl") &&
							(MODEL.contentEquals("BNRV510") || MODEL.contentEquals("BNRV520") || MODEL.contentEquals("BNRV700"))
					)
				)
				|| ONYX_BRIGHTNESS_FILE || ONYX_BRIGHTNESS_WARM_FILE || EINK_HAVE_FRONTLIGHT || EINK_HAVE_NATURAL_BACKLIGHT;

		POCKETBOOK = MODEL.toLowerCase().startsWith("pocketbook") || MODEL.toLowerCase().startsWith("obreey");
		
		NOOK_NAVIGATION_KEYS = EINK_NOOK; // TODO: add autodetect
		SONY_NAVIGATION_KEYS = EINK_SONY;
		EINK_SCREEN_UPDATE_MODES_SUPPORTED = EINK_SCREEN && ( EINK_NOOK || EINK_TOLINO || EINK_ONYX ); // TODO: add autodetect
		FORCE_HC_THEME = EINK_SCREEN || MODEL.equalsIgnoreCase("pocketbook vision");
		USE_CUSTOM_TOAST = EINK_SCREEN && (getSDKLevel() < DeviceInfo.KITKAT);
		NOFLIBUSTA = POCKETBOOK;
		NAVIGATE_LEFTRIGHT = POCKETBOOK && DEVICE.startsWith("EP10");
		REVERT_LANDSCAPE_VOLUME_KEYS = POCKETBOOK && DEVICE.startsWith("EP5A");
		MIN_SCREEN_BRIGHTNESS_VALUE = getMinBrightness(AMOLED_SCREEN ? 2 : (getSDKLevel() >= ICE_CREAM_SANDWICH ? 8 : 16));
		//BUFFER_COLOR_FORMAT = getSDKLevel() >= HONEYCOMB ? android.graphics.Bitmap.Config.ARGB_8888 : android.graphics.Bitmap.Config.RGB_565;
		//BUFFER_COLOR_FORMAT = android.graphics.Bitmap.Config.ARGB_8888;
		BUFFER_COLOR_FORMAT = (EINK_SCREEN && (!ONYX_HAVE_COLOR)) || USE_OPENGL ? android.graphics.Bitmap.Config.ARGB_8888 : android.graphics.Bitmap.Config.RGB_565;
		PIXEL_FORMAT = (DeviceInfo.BUFFER_COLOR_FORMAT == android.graphics.Bitmap.Config.RGB_565) ? PixelFormat.RGB_565 : PixelFormat.RGBA_8888;
		
		DEF_FONT_FACE = getSDKLevel() >= ICE_CREAM_SANDWICH ? "Roboto" : "Droid Sans";
		
		USE_BITMAP_MEMORY_HACK = getSDKLevel() < ICE_CREAM_SANDWICH;
		ONE_COLUMN_IN_LANDSCAPE = POCKETBOOK && DEVICE.endsWith("SURFPAD");
		DEF_FONT_SIZE = POCKETBOOK && DEVICE.endsWith("SURFPAD") ? 18 : null;
	}
	
	private static String getBuildField(String fieldName) {
		
		try {
			return (String)Build.class.getField(fieldName).get(null);
		} catch (Exception e) {
			Log.d("cr3", "Exception while trying to check Build." + fieldName);
			return "";
		}
	}
	
	
	static {
		Log.i("cr3", "DeviceInfo: MANUFACTURER=" + MANUFACTURER + ", MODEL=" + MODEL + ", DEVICE=" + DEVICE + ", PRODUCT=" + PRODUCT + ", BRAND=" + BRAND);
		Log.i("cr3", "DeviceInfo: MIN_SCREEN_BRIGHTNESS_VALUE=" + MIN_SCREEN_BRIGHTNESS_VALUE + "; MAX_SCREEN_BRIGHTNESS_VALUE=" + MAX_SCREEN_BRIGHTNESS_VALUE + "; EINK_SCREEN=" + EINK_SCREEN + "; EINK_SCREEN_REGAL=" + EINK_SCREEN_REGAL + ", AMOLED_SCREEN=" + AMOLED_SCREEN + ", POCKETBOOK=" + POCKETBOOK);
	}

	// multiple patterns divided by |, * wildcard can be placed at beginning and/or end of pattern
	// samples: "samsung", "p500|p510", "sgs*|sgh*"
	private static boolean match(String value, String pattern) {
		if (pattern == null || pattern.length() == 0 || "*".equals(pattern))
			return true; // matches any value
		if (value == null || value.length() == 0)
			return false;
		value = value.toLowerCase();
		pattern = pattern.toLowerCase();
		String[] patterns = pattern.split("\\|");
		for (String p : patterns) {
			boolean startingWildcard = false;
			boolean endingWildcard = false;
			if (p.startsWith("*")) {
				startingWildcard = true;
				p = p.substring(1);
			}
			if (p.endsWith("*")) {
				endingWildcard = true;
				p = p.substring(0, p.length()-1);
			}
			if (startingWildcard && endingWildcard) {
				if (value.indexOf(p) < 0)
					return false;
			} else if (startingWildcard) {
				if (!value.endsWith(p))
					return false;
			} else if (endingWildcard) {
				if (!value.startsWith(p))
					return false;
			} else {
				if (!value.equals(p))
					return false;
			}
		}
		return true;
	}

	// delimited by ;
	// "manufacturer;model;device;brand", "manufacturer;model;device" or "manufacturer;model" or "manufacturer" 
	private static boolean matchDevice(String pattern) {
		String[] patterns = pattern.split(";");
		if (patterns.length >= 1)
			if (!match(MANUFACTURER, patterns[0]))
				return false;
		if (patterns.length >= 2)
			if (!match(MODEL, patterns[1]))
				return false;
		if (patterns.length >= 3)
			if (!match(DEVICE, patterns[2]))
				return false;
		if (patterns.length >= 4)
			if (!match(BRAND, patterns[3]))
				return false;
		return true;
	}

//	// TEST
//	private static boolean testMatchDevice(String manufacturer, String model, String device, String pattern) {
//		String[] patterns = pattern.split(";");
//		if (patterns.length >= 1)
//			if (!match(manufacturer, patterns[0]))
//				return false;
//		if (patterns.length >= 2)
//			if (!match(model, patterns[1]))
//				return false;
//		if (patterns.length >= 3)
//			if (!match(device, patterns[2]))
//				return false;
//		Log.v("cr3", "matched : " + pattern + " == " + manufacturer + "," + model + "," + device);
//		return true;
//	}
//	
//	static {
//		testMatchDevice("Archos", "A70S", "A70S", "Archos;A70S");
//		testMatchDevice("MegaMan", "A70S", "A70S", "mega*;A70*");
//		testMatchDevice("MegaMan", "A70", "A70S", "*man;A70*");
//	}

	private static int getMinBrightness(int defValue) {
		try {
			for (int i=0; i<MIN_SCREEN_BRIGHTNESS_DB.length - 1; i+=2) {
				if (matchDevice(MIN_SCREEN_BRIGHTNESS_DB[i])) {
					return Integer.valueOf(MIN_SCREEN_BRIGHTNESS_DB[i+1]);
				}
			}
		} catch (NumberFormatException e) {
			// ignore
		}
		return defValue;
	}
	
}

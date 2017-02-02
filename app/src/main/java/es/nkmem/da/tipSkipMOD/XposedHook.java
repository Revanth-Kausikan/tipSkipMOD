package es.nkmem.da.tipSkipMOD;

import android.hardware.display.DisplayManager;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import android.content.Context;
import android.media.AudioManager;
import android.view.Display;

public class XposedHook implements IXposedHookLoadPackage {
    public static String TAG = "[tipSkipMOD] ";
    public static final String PACKAGE_TIPSKIP = "com.busybytes.tipSkip";
    private static final String CLASS_SEND_KEYEVENTS = "com.busybytes.tipSkip.e";
    private static final String CLASS_SPLASHSCREEN = "com.busybytes.tipSkip.w";
    private static final String CLASS_STARTSCREEN = "com.busybytes.tipSkip.x";


    @Override
    public void handleLoadPackage(final XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        if (!lpparam.packageName.equals(PACKAGE_TIPSKIP)) {
            return;
        }

        // so it doesn't work when screen is ON
        Class<?> ClassSendKeyEvents = XposedHelpers.findClass(CLASS_SEND_KEYEVENTS, lpparam.classLoader);
        XposedHelpers.findAndHookMethod(ClassSendKeyEvents, "a", int.class, ChangeKeys);

        // to change certain keys ("Double Previous", more specifically)
        XposedHelpers.findAndHookMethod(ClassSendKeyEvents, "d", ChangeVolume);

        // to remove the 2 seconds in splashscreen, faster loading
        Class<?> classSplashScreen = XposedHelpers.findClass(CLASS_SPLASHSCREEN, lpparam.classLoader);

        XposedHelpers.findAndHookMethod("com.busybytes.tipSkip.w", lpparam.classLoader, "run", new XC_MethodReplacement() {
                        @Override
                        protected Object replaceHookedMethod(MethodHookParam param) {
                            Class<?> classstart = XposedHelpers.findClass(CLASS_STARTSCREEN, lpparam.classLoader);
                            Object class2Instance = XposedHelpers.newInstance(classstart, param.thisObject);
                            XposedHelpers.callMethod(class2Instance, "run");
                            // just running the actual Start Screen, avoid the Splash Screen
                            return null;
                        }
        });

    }

    private static XC_MethodHook ChangeVolume = new XC_MethodHook() {
        @Override
        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
            //Change volume...
            Context context = (Context) XposedHelpers.getObjectField(param.thisObject, "b");
            AudioManager audio = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
            audio.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI);

            //everything has been replaced, exit the method (so no "Double Previous" occurs)
            param.setResult(null);
        }
    };

    private static XC_MethodHook ChangeKeys = new XC_MethodHook() {
        @Override
        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
            Context context = (Context) XposedHelpers.getObjectField(param.thisObject, "b");
            DisplayManager dm = (DisplayManager) context.getSystemService(Context.DISPLAY_SERVICE);
            for (Display display : dm.getDisplays()) {
                if (display.getState() != Display.STATE_OFF) {
                    //display is not off, we don't do/send anything
                    param.setResult(null);
                }
//                else {
//                    XposedBridge.log(TAG + "display is OFF!");
//                }
            }
        }

    };

}
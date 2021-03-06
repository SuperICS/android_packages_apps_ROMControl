
package com.aokp.romcontrol.fragments;

import java.io.File;
import java.util.ArrayList;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.SystemProperties;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceGroup;
import android.preference.PreferenceScreen;
import android.provider.Settings;
import android.text.Spannable;
import android.widget.EditText;

import com.aokp.romcontrol.AOKPPreferenceFragment;
import com.aokp.romcontrol.R;
import com.aokp.romcontrol.util.CMDProcessor;
import com.aokp.romcontrol.util.Helpers;

public class UserInterface extends AOKPPreferenceFragment implements
        OnPreferenceChangeListener {

    public static final String TAG = "UserInterface";

    private static final String PREF_CRT_ON = "crt_on";
    private static final String PREF_CRT_OFF = "crt_off";
    private static final String PREF_IME_SWITCHER = "ime_switcher";
    private static final String PREF_ENABLE_VOLUME_OPTIONS = "enable_volume_options";
    private static final String PREF_CUSTOM_CARRIER_LABEL = "custom_carrier_label";
    private static final String PREF_LONGPRESS_TO_KILL = "longpress_to_kill";
    private static final String PREF_ROTATION_ANIMATION = "rotation_animation_delay";
    private static final String PREF_HOME_LONGPRESS = "long_press_home";
    private static final String PREF_RECENT_APP_SWITCHER = "recent_app_switcher";
    private static final String PREF_LESS_NOTIFICATION_SOUNDS = "less_notification_sounds";

    CheckBoxPreference mCrtOnAnimation;
    CheckBoxPreference mCrtOffAnimation;
    CheckBoxPreference mShowImeSwitcher;
    CheckBoxPreference mEnableVolumeOptions;
    CheckBoxPreference mLongPressToKill;
    Preference mCustomLabel;
    ListPreference mAnimationRotationDelay;
    ListPreference mRecentAppSwitcher;
    ListPreference mHomeLongpress;
    Preference mLcdDensity;
    CheckBoxPreference mDisableBootAnimation;
    CheckBoxPreference mDisableBootAudio;
    CheckBoxPreference mDisableBugMailer;
    ListPreference mAnnoyingNotifications;

    String mCustomLabelText = null;
    int newDensityValue;

    DensityChanger densityFragment;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.prefs_ui);

        PreferenceScreen prefs = getPreferenceScreen();

        mCrtOffAnimation = (CheckBoxPreference) findPreference(PREF_CRT_OFF);
        mCrtOffAnimation.setChecked(Settings.System.getInt(getActivity().getContentResolver(),
                Settings.System.CRT_OFF_ANIMATION, 1) == 1);

        mCrtOnAnimation = (CheckBoxPreference) findPreference(PREF_CRT_ON);
        mCrtOnAnimation.setChecked(Settings.System.getInt(getActivity().getContentResolver(),
                Settings.System.CRT_ON_ANIMATION, 0) == 1);

        mShowImeSwitcher = (CheckBoxPreference) findPreference(PREF_IME_SWITCHER);
        mShowImeSwitcher.setChecked(Settings.System.getInt(getActivity().getContentResolver(),
                Settings.System.SHOW_STATUSBAR_IME_SWITCHER, 1) == 1);

        mEnableVolumeOptions = (CheckBoxPreference) findPreference(PREF_ENABLE_VOLUME_OPTIONS);
        mEnableVolumeOptions.setChecked(Settings.System.getInt(getActivity().getContentResolver(),
                Settings.System.ENABLE_VOLUME_OPTIONS, 0) == 1);

        mCustomLabel = findPreference(PREF_CUSTOM_CARRIER_LABEL);
        updateCustomLabelTextSummary();

        mLongPressToKill = (CheckBoxPreference) findPreference(PREF_LONGPRESS_TO_KILL);
        mLongPressToKill.setChecked(Settings.Secure.getInt(getActivity().getContentResolver(),
                Settings.Secure.KILL_APP_LONGPRESS_BACK, 0) == 1);

        mAnimationRotationDelay = (ListPreference) findPreference(PREF_ROTATION_ANIMATION);
        mAnimationRotationDelay.setOnPreferenceChangeListener(this);
        mAnimationRotationDelay.setValue(Settings.System.getInt(getActivity()
                .getContentResolver(), Settings.System.ACCELEROMETER_ROTATION_SETTLE_TIME,
                200) + "");

        mRecentAppSwitcher = (ListPreference) findPreference(PREF_RECENT_APP_SWITCHER);
        mRecentAppSwitcher.setOnPreferenceChangeListener(this);
        mRecentAppSwitcher.setValue(Integer.toString(Settings.System.getInt(getActivity()
                .getContentResolver(), Settings.System.RECENT_APP_SWITCHER,
                0)));

        mAnnoyingNotifications = (ListPreference) findPreference(PREF_LESS_NOTIFICATION_SOUNDS);
        mAnnoyingNotifications.setOnPreferenceChangeListener(this);
        mAnnoyingNotifications.setValue(Integer.toString(Settings.System.getInt(getActivity()
                .getContentResolver(), Settings.System.MUTE_ANNOYING_NOTIFICATIONS_THRESHOLD,
                0)));

        mLcdDensity = findPreference("lcd_density_setup");
        String currentProperty = SystemProperties.get("ro.sf.lcd_density");
        try {
            newDensityValue = Integer.parseInt(currentProperty);
        } catch (Exception e) {
            getPreferenceScreen().removePreference(mLcdDensity);
        }

        mLcdDensity.setSummary(getResources().getString(R.string.current_lcd_density) + currentProperty);

        mHomeLongpress = (ListPreference) findPreference(PREF_HOME_LONGPRESS);
        mHomeLongpress.setOnPreferenceChangeListener(this);
        mHomeLongpress.setValue(Settings.System.getInt(getActivity()
                .getContentResolver(), Settings.System.NAVIGATION_BAR_HOME_LONGPRESS,
                -1) + "");

        if (!getResources().getBoolean(com.android.internal.R.bool.config_enableCrtAnimations)) {
            prefs.removePreference((PreferenceGroup) findPreference("crt"));
        } else {
            // can't get this working in ICS just yet
            ((PreferenceGroup) findPreference("crt")).removePreference(mCrtOnAnimation);
        }

        if (!hasHardwareButtons) {
            ((PreferenceGroup) findPreference("misc")).removePreference(mLongPressToKill);
            ((PreferenceGroup) findPreference("misc")).removePreference(mHomeLongpress);
        }

        if(mTablet) {
            ((PreferenceGroup) findPreference("misc")).removePreference(mHomeLongpress);
        }
    }

    private void updateCustomLabelTextSummary() {
        mCustomLabelText = Settings.System.getString(getActivity().getContentResolver(),
                Settings.System.CUSTOM_CARRIER_LABEL);
        if (mCustomLabelText == null) {
            mCustomLabel
                    .setSummary(R.string.custom_carrier_label_warning);
        } else {
            mCustomLabel.setSummary(mCustomLabelText);
        }

    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen,
            Preference preference) {
        if (preference == mCrtOffAnimation) {

            boolean checked = ((CheckBoxPreference) preference).isChecked();
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.CRT_OFF_ANIMATION, checked ? 1 : 0);
            return true;

        } else if (preference == mCrtOnAnimation) {

            boolean checked = ((CheckBoxPreference) preference).isChecked();
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.CRT_ON_ANIMATION, checked ? 1 : 0);
            return true;

        } else if (preference == mShowImeSwitcher) {

            boolean checked = ((CheckBoxPreference) preference).isChecked();
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.SHOW_STATUSBAR_IME_SWITCHER, checked ? 1 : 0);
            return true;

        } else if (preference == mEnableVolumeOptions) {

            boolean checked = ((CheckBoxPreference) preference).isChecked();
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.ENABLE_VOLUME_OPTIONS, checked ? 1 : 0);
            return true;

        } else if (preference == mCustomLabel) {
            AlertDialog.Builder alert = new AlertDialog.Builder(getActivity());

            alert.setTitle(R.string.custom_carrier_label_title);
            alert.setMessage(R.string.custom_carrier_label_empty);

            // Set an EditText view to get user input
            final EditText input = new EditText(getActivity());
            input.setText(mCustomLabelText != null ? mCustomLabelText : "");
            alert.setView(input);

            alert.setPositiveButton(getResources().getString(R.string.ok), new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    String value = ((Spannable) input.getText()).toString();
                    Settings.System.putString(getActivity().getContentResolver(),
                            Settings.System.CUSTOM_CARRIER_LABEL, value);
                    updateCustomLabelTextSummary();
                }
            });

            alert.setNegativeButton(getResources().getString(R.string.cancel), new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    // Canceled.
                }
            });

            alert.show();
        } else if (preference == mLongPressToKill) {

            boolean checked = ((CheckBoxPreference) preference).isChecked();
            Settings.Secure.putInt(getActivity().getContentResolver(),
                    Settings.Secure.KILL_APP_LONGPRESS_BACK, checked ? 1 : 0);
            return true;

        } else if (preference == mLcdDensity) {
            ((PreferenceActivity) getActivity())
                    .startPreferenceFragment(new DensityChanger(), true);
            return true;
        }

        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == mAnimationRotationDelay) {

            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.ACCELEROMETER_ROTATION_SETTLE_TIME,
                    Integer.parseInt((String) newValue));

            return true;
        } else if (preference == mHomeLongpress) {
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.NAVIGATION_BAR_HOME_LONGPRESS,
                    Integer.parseInt((String) newValue));
            return true;
        } else if (preference == mRecentAppSwitcher) {
            int val = Integer.parseInt((String) newValue);
            Settings.System.putInt(getActivity().getContentResolver(),
                Settings.System.RECENT_APP_SWITCHER, val);
            Helpers.restartSystemUI();
            return true;
        } else if (preference == mAnnoyingNotifications) {
            int val = Integer.parseInt((String) newValue);
            Settings.System.putInt(getActivity().getContentResolver(),
                Settings.System.MUTE_ANNOYING_NOTIFICATIONS_THRESHOLD, val);
            return true;
        }
        return false;
    }

    public static void addButton(Context context, String key) {
        ArrayList<String> enabledToggles = Navbar
                .getButtonsStringArray(context);
        enabledToggles.add(key);
        Navbar.setButtonsFromStringArray(context, enabledToggles);
    }

    public static void removeButton(Context context, String key) {
        ArrayList<String> enabledToggles = Navbar
                .getButtonsStringArray(context);
        enabledToggles.remove(key);
        Navbar.setButtonsFromStringArray(context, enabledToggles);
    }

}

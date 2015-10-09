package com.android.camera.settings;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.*;

import com.android.camera.debug.Log;
import com.android.camera.util.CameraSettingsActivityHelper;
import com.android.camera2.R;
import com.android.ex.camera2.portability.CameraAgentFactory;
import com.android.ex.camera2.portability.CameraDeviceInfo;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

public class CameraSettingsFragment_Plus extends PreferenceFragment implements
        SharedPreferences.OnSharedPreferenceChangeListener {

    public static final String PREF_CATEGORY_RESOLUTION = "pref_category_resolution";
    private static final Log.Tag TAG = new Log.Tag("SettingsFragment");
    private static DecimalFormat sMegaPixelFormat = new DecimalFormat("##0.0");
    private String[] mCamcorderProfileNames;
    private CameraDeviceInfo mInfos;
    private String mPrefKey;
    private boolean mGetSubPrefAsRoot = true;

    // Selected resolutions for the different cameras and sizes.
    private PictureSizeLoader.PictureSizes mPictureSizes;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle arguments = getArguments();
        if (arguments != null) {
            mPrefKey = arguments.getString(CameraSettingsActivity.PREF_SCREEN_EXTRA);
        }
        Context context = this.getActivity().getApplicationContext();
        /* ZhangChao time:Dec 19, 2014,use my Custom. ORIG ++++ */
//        addPreferencesFromResource(R.xml.camera_preferences);
        /* ZhangChao time:Dec 19, 2014,use my Custom. START ++++ */
        addPreferencesFromResource(R.xml.camera_preferences_plus);
        /* ZhangChao time:Dec 19, 2014,use my Custom. END ++++ */

        // Allow the Helper to edit the full preference hierarchy, not the
        // sub tree we may show as root. See {@link #getPreferenceScreen()}.
        mGetSubPrefAsRoot = false;
        CameraSettingsActivityHelper.addAdditionalPreferences(this, context);
        mGetSubPrefAsRoot = true;

        mCamcorderProfileNames = getResources().getStringArray(R.array.camcorder_profile_names);
        mInfos = CameraAgentFactory
                .getAndroidCameraAgent(context, CameraAgentFactory.CameraApi.API_1)
                .getCameraDeviceInfo();
    }

    @Override
    public void onResume() {
        super.onResume();

        // Load the camera sizes.
        loadSizes();

        // Send loaded sizes to additional preferences.
        CameraSettingsActivityHelper.onSizesLoaded(this, mPictureSizes.backCameraSizes,
                new ListPreferenceFiller() {
                    @Override
                    public void fill(List<com.android.camera.util.Size> sizes, ListPreference preference) {
                        setEntriesForSelection(sizes, preference);
                    }
                });

        // Make sure to hide settings for cameras that don't exist on this
        // device.
        setVisibilities();

        // Put in the summaries for the currently set values.
        /* ZhangChao time:2015-02-11,use Category instead. ORIG ++++ */
//        final PreferenceScreen resolutionScreen =
//                (PreferenceScreen) findPreference(PREF_CATEGORY_RESOLUTION);
//        fillEntriesAndSummaries(resolutionScreen);
//        setPreferenceScreenIntent(resolutionScreen);
        /* ZhangChao time:2015-02-11,use Category instead. START ++++ */
        final PreferenceCategory resolutionCategory =
                (PreferenceCategory) findPreference(PREF_CATEGORY_RESOLUTION);
        fillEntriesAndSummaries(resolutionCategory);
        /* ZhangChao time:2015-02-11,use Category instead. END ---- */

        getPreferenceScreen().getSharedPreferences()
                .registerOnSharedPreferenceChangeListener(this);
    }

    /**
     * Configure home-as-up for sub-screens.
     */
    private void setPreferenceScreenIntent(final PreferenceScreen preferenceScreen) {
        Intent intent = new Intent(getActivity(), CameraSettingsActivity.class);
        intent.putExtra(CameraSettingsActivity.PREF_SCREEN_EXTRA, preferenceScreen.getKey());
        preferenceScreen.setIntent(intent);
    }

    /**
     * This override allows the CameraSettingsFragment to be reused for
     * different nested PreferenceScreens within the single camera
     * preferences XML resource. If the fragment is constructed with a
     * desired preference key (delivered via an extra in the creation
     * intent), it is used to look up the nested PreferenceScreen and
     * returned here.
     */
    @Override
    public PreferenceScreen getPreferenceScreen() {
        PreferenceScreen root = super.getPreferenceScreen();
        if (!mGetSubPrefAsRoot || mPrefKey == null || root == null) {
            return root;
        } else {
            PreferenceScreen match = findByKey(root, mPrefKey);
            if (match != null) {
                return match;
            } else {
                throw new RuntimeException("key " + mPrefKey + " not found");
            }
        }
    }

    private PreferenceScreen findByKey(PreferenceScreen parent, String key) {
        if (key.equals(parent.getKey())) {
            return parent;
        } else {
            for (int i = 0; i < parent.getPreferenceCount(); i++) {
                Preference child = parent.getPreference(i);
                if (child instanceof PreferenceScreen) {
                    PreferenceScreen match = findByKey((PreferenceScreen) child, key);
                    if (match != null) {
                        return match;
                    }
                }
            }
            return null;
        }
    }

    /**
     * Depending on camera availability on the device, this removes settings
     * for cameras the device doesn't have.
     */
    private void setVisibilities() {
        PreferenceGroup resolutions =
                (PreferenceGroup) findPreference(PREF_CATEGORY_RESOLUTION);
        if (mPictureSizes.backCameraSizes.isEmpty()) {
            recursiveDelete(resolutions,
                    findPreference(Keys.KEY_PICTURE_SIZE_BACK));
            recursiveDelete(resolutions,
                    findPreference(Keys.KEY_VIDEO_QUALITY_BACK));
        }
        if (mPictureSizes.frontCameraSizes.isEmpty()) {
            recursiveDelete(resolutions,
                    findPreference(Keys.KEY_PICTURE_SIZE_FRONT));
            recursiveDelete(resolutions,
                    findPreference(Keys.KEY_VIDEO_QUALITY_FRONT));
        }
    }

    /**
     * Recursively go through settings and fill entries and summaries of our
     * preferences.
     */
    private void fillEntriesAndSummaries(PreferenceGroup group) {
        for (int i = 0; i < group.getPreferenceCount(); ++i) {
            Preference pref = group.getPreference(i);
            if (pref instanceof PreferenceGroup) {
                fillEntriesAndSummaries((PreferenceGroup) pref);
            }
            setSummary(pref);
            setEntries(pref);
        }
    }

    /**
     * Recursively traverses the tree from the given group as the route and
     * tries to delete the preference. Traversal stops once the preference
     * was found and removed.
     */
    private boolean recursiveDelete(PreferenceGroup group, Preference preference) {
        if (group == null) {
            Log.d(TAG, "attempting to delete from null preference group");
            return false;
        }
        if (preference == null) {
            Log.d(TAG, "attempting to delete null preference");
            return false;
        }
        if (group.removePreference(preference)) {
            // Removal was successful.
            return true;
        }

        for (int i = 0; i < group.getPreferenceCount(); ++i) {
            Preference pref = group.getPreference(i);
            if (pref instanceof PreferenceGroup) {
                if (recursiveDelete((PreferenceGroup) pref, preference)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public void onPause() {
        super.onPause();
        getPreferenceScreen().getSharedPreferences()
                .unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        setSummary(findPreference(key));
    }

    /**
     * Set the entries for the given preference. The given preference needs
     * to be a {@link ListPreference}
     */
    private void setEntries(Preference preference) {
        if (!(preference instanceof ListPreference)) {
            return;
        }

        ListPreference listPreference = (ListPreference) preference;
        if (listPreference.getKey().equals(Keys.KEY_PICTURE_SIZE_BACK)) {
            setEntriesForSelection(mPictureSizes.backCameraSizes, listPreference);
        } else if (listPreference.getKey().equals(Keys.KEY_PICTURE_SIZE_FRONT)) {
            setEntriesForSelection(mPictureSizes.frontCameraSizes, listPreference);
        } else if (listPreference.getKey().equals(Keys.KEY_VIDEO_QUALITY_BACK)) {
            setEntriesForSelection(mPictureSizes.videoQualitiesBack.orNull(), listPreference);
        } else if (listPreference.getKey().equals(Keys.KEY_VIDEO_QUALITY_FRONT)) {
            setEntriesForSelection(mPictureSizes.videoQualitiesFront.orNull(), listPreference);
        }
    }

    /**
     * Set the summary for the given preference. The given preference needs
     * to be a {@link ListPreference}.
     */
    private void setSummary(Preference preference) {
        if (!(preference instanceof ListPreference)) {
            return;
        }

        ListPreference listPreference = (ListPreference) preference;
        if (listPreference.getKey().equals(Keys.KEY_PICTURE_SIZE_BACK)) {
            setSummaryForSelection(mPictureSizes.backCameraSizes,
                    listPreference);
        } else if (listPreference.getKey().equals(Keys.KEY_PICTURE_SIZE_FRONT)) {
            setSummaryForSelection(mPictureSizes.frontCameraSizes,
                    listPreference);
        } else if (listPreference.getKey().equals(Keys.KEY_VIDEO_QUALITY_BACK)) {
            setSummaryForSelection(mPictureSizes.videoQualitiesBack.orNull(), listPreference);
        } else if (listPreference.getKey().equals(Keys.KEY_VIDEO_QUALITY_FRONT)) {
            setSummaryForSelection(mPictureSizes.videoQualitiesFront.orNull(), listPreference);
        } else {
            listPreference.setSummary(listPreference.getEntry());
        }
    }

    /**
     * Sets the entries for the given list preference.
     *
     * @param selectedSizes The possible S,M,L entries the user can choose
     *            from.
     * @param preference The preference to set the entries for.
     */
    private void setEntriesForSelection(List<com.android.camera.util.Size> selectedSizes,
                                        ListPreference preference) {
        if (selectedSizes == null) {
            return;
        }

        String[] entries = new String[selectedSizes.size()];
        String[] entryValues = new String[selectedSizes.size()];
        for (int i = 0; i < selectedSizes.size(); i++) {
            com.android.camera.util.Size size = selectedSizes.get(i);
            entries[i] = getSizeSummaryString(size);
            entryValues[i] = SettingsUtil.sizeToSettingString(size);
        }
        preference.setEntries(entries);
        preference.setEntryValues(entryValues);
    }

    /**
     * Sets the entries for the given list preference.
     *
     * @param selectedQualities The possible S,M,L entries the user can
     *            choose from.
     * @param preference The preference to set the entries for.
     */
    private void setEntriesForSelection(SettingsUtil.SelectedVideoQualities selectedQualities,
                                        ListPreference preference) {
        if (selectedQualities == null) {
            return;
        }

        // Avoid adding double entries at the bottom of the list which
        // indicates that not at least 3 qualities are supported.
        ArrayList<String> entries = new ArrayList<String>();
        entries.add(mCamcorderProfileNames[selectedQualities.large]);
        if (selectedQualities.medium != selectedQualities.large) {
            entries.add(mCamcorderProfileNames[selectedQualities.medium]);
        }
        if (selectedQualities.small != selectedQualities.medium) {
            entries.add(mCamcorderProfileNames[selectedQualities.small]);
        }
        preference.setEntries(entries.toArray(new String[0]));
    }

    /**
     * Sets the summary for the given list preference.
     *
     * @param displayableSizes The human readable preferred sizes
     * @param preference The preference for which to set the summary.
     */
    private void setSummaryForSelection(List<com.android.camera.util.Size> displayableSizes,
                                        ListPreference preference) {
        String setting = preference.getValue();
        if (setting == null || !setting.contains("x")) {
            return;
        }
        com.android.camera.util.Size settingSize = SettingsUtil.sizeFromSettingString(setting);
        if (settingSize == null || settingSize.area() == 0) {
            return;
        }
        preference.setSummary(getSizeSummaryString(settingSize));
    }

    /**
     * Sets the summary for the given list preference.
     *
     * @param selectedQualities The selected video qualities.
     * @param preference The preference for which to set the summary.
     */
    private void setSummaryForSelection(SettingsUtil.SelectedVideoQualities selectedQualities,
                                        ListPreference preference) {
        if (selectedQualities == null) {
            return;
        }

        int selectedQuality = selectedQualities.getFromSetting(preference.getValue());
        preference.setSummary(mCamcorderProfileNames[selectedQuality]);
    }

    /**
     * This method gets the selected picture sizes for S,M,L and populates
     * {@link #mPictureSizes} accordingly.
     */
    private void loadSizes() {
        if (mInfos == null) {
            Log.w(TAG, "null deviceInfo, cannot display resolution sizes");
            return;
        }
        PictureSizeLoader loader = new PictureSizeLoader(getActivity().getApplicationContext());
        mPictureSizes = loader.computePictureSizes();
    }

    /**
     * @param size The photo resolution.
     * @return A human readable and translated string for labeling the
     *         picture size in megapixels.
     */
    private String getSizeSummaryString(com.android.camera.util.Size size) {
        com.android.camera.util.Size approximateSize = ResolutionUtil.getApproximateSize(size);
        String megaPixels = sMegaPixelFormat.format((size.width() * size.height()) / 1e6);
        int numerator = ResolutionUtil.aspectRatioNumerator(approximateSize);
        int denominator = ResolutionUtil.aspectRatioDenominator(approximateSize);
        String result = getResources().getString(
                R.string.setting_summary_aspect_ratio_and_megapixels, numerator, denominator,
                megaPixels);
        return result;
    }
}
/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.camera.settings;

import android.content.Context;

/**
 * Keys is a class for storing SharedPreferences keys and configuring
 * their defaults.
 *
 * For each key that has a default value and set of possible values, it
 * stores those defaults so they can be used by the SettingsManager
 * on lookup.  This step is optional, and it can be done anytime before
 * a setting is accessed by the SettingsManager API.
 */
public class Keys_Plus extends Keys {
    /**
     * Set some number of defaults for the defined keys.
     * It's not necessary to set all defaults.
     */
    public static void setDefaults(SettingsManager settingsManager, Context context) {
        Keys.setDefaults(settingsManager, context);
        settingsManager.setDefaults(KEY_EXPOSURE_COMPENSATION_ENABLED, true);
    }
}


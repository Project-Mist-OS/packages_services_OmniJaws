/*
 *  Copyright (C) 2017 The OmniROM Project
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package org.omnirom.omnijaws.widget;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;

import org.omnirom.omnijaws.Config;
import org.omnirom.omnijaws.R;
import org.omnirom.omnijaws.WeatherIconPackInfo;
import org.omnirom.omnijaws.WeatherIconPackManager;

import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.Preference.OnPreferenceChangeListener;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceScreen;
import androidx.preference.PreferenceManager;

public class WeatherAppWidgetConfigureFragment extends PreferenceFragmentCompat
        implements OnPreferenceChangeListener {

    public static final String KEY_COLOR_THEME = "color_theme";
    public static final int COLOR_THEME_LIGHT = 3;
    public static final int COLOR_THEME_DARK = 2;
    public static final int COLOR_THEME_SYSTEM = 1;
    public static final int COLOR_THEME_TRANSPARENT = 0;
    public static final int COLOR_THEME_DEFAULT = COLOR_THEME_SYSTEM;

    public static final String KEY_BG_TRANS = "bg_transparency";
    public static final int BG_TRANS_SEMI = 1;
    public static final int BG_TRANS_FULL = 2;
    public static final int BG_TRANS_SOLID = 3;
    public static final int BG_TRANS_DEFAULT = BG_TRANS_SEMI;

    public static final String KEY_ICON_VARIANT_MODE = "icon_variant_mode";
    public static final String ICON_VARIANT_FOLLOW_APP = "app";
    public static final String ICON_VARIANT_FOLLOW_WIDGET = "widget";

    private int mAppWidgetId;
    private ListPreference mColorTheme;
    private ListPreference mBgTrans;
    private ListPreference mIconVariantMode;

    static String widgetKey(String key, int id) {
        return key + "_" + id;
    }

    public WeatherAppWidgetConfigureFragment(int appWidgetId) {
        super();
        mAppWidgetId = appWidgetId;
    }

    private ListPreference initListPreference(String key, String value, String fallbackValue) {
        ListPreference pref = (ListPreference) findPreference(key);
        int idx = pref.findIndexOfValue(value);

        if (idx == -1) {
            value = fallbackValue;
            idx = pref.findIndexOfValue(value);
        }

        if (idx == -1) {
            idx = 0;
            value = pref.getEntryValues()[idx].toString();
        }

        pref.setValue(value);
        pref.setSummary(pref.getEntries()[idx]);
        pref.setOnPreferenceChangeListener(this);
        return pref;
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.weather_appwidget_configure);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());

        String theme = String.valueOf(prefs.getInt(
            widgetKey(KEY_COLOR_THEME, mAppWidgetId),
            COLOR_THEME_DEFAULT));

        mColorTheme = initListPreference(
                KEY_COLOR_THEME,
                theme,
                String.valueOf(COLOR_THEME_DEFAULT));

        String variantMode = prefs.getString(
            widgetKey(KEY_ICON_VARIANT_MODE, mAppWidgetId),
            ICON_VARIANT_FOLLOW_APP);

        mIconVariantMode = initListPreference(
                KEY_ICON_VARIANT_MODE,
                variantMode,
                ICON_VARIANT_FOLLOW_APP);
        maybeRemoveIconVariantPreference();

        String bgTrans = String.valueOf(prefs.getInt(
            widgetKey(KEY_BG_TRANS, mAppWidgetId),
            BG_TRANS_DEFAULT));

        mBgTrans = initListPreference(
                KEY_BG_TRANS,
                bgTrans,
                String.valueOf(BG_TRANS_DEFAULT));
    }

    public static void clearPrefs(Context context, int id) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        prefs.edit()
                .remove(widgetKey(KEY_COLOR_THEME, id))
                .remove(widgetKey(KEY_BG_TRANS, id))
                .remove(widgetKey(KEY_ICON_VARIANT_MODE, id))
                .apply();
    }

    public static void remapPrefs(Context context, int oldId, int newId) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        int oldThemeValue = prefs.getInt(widgetKey(KEY_COLOR_THEME, oldId), COLOR_THEME_DEFAULT);
        int oldBgValue = prefs.getInt(widgetKey(KEY_BG_TRANS, oldId), BG_TRANS_DEFAULT);
        String oldIconVariantValue = prefs.getString(widgetKey(KEY_ICON_VARIANT_MODE, oldId),
                ICON_VARIANT_FOLLOW_APP);
        prefs.edit()
                .putInt(widgetKey(KEY_COLOR_THEME, newId), oldThemeValue)
                .remove(widgetKey(KEY_COLOR_THEME, oldId))
                .putInt(widgetKey(KEY_BG_TRANS, newId), oldBgValue)
                .remove(widgetKey(KEY_BG_TRANS, oldId))
                .putString(widgetKey(KEY_ICON_VARIANT_MODE, newId), oldIconVariantValue)
                .remove(widgetKey(KEY_ICON_VARIANT_MODE, oldId))
                .apply();
    }

    private void maybeRemoveIconVariantPreference() {
        WeatherIconPackInfo pack = WeatherIconPackManager.findByValue(
                WeatherIconPackManager.load(getContext()),
                Config.getIconPack(getContext()));
        if (pack != null && pack.hasExplicitVariants()) {
            return;
        }
        PreferenceScreen screen = getPreferenceScreen();
        if (screen != null) {
            screen.removePreference(mIconVariantMode);
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference.equals(mColorTheme)) {
            String newTheme = (String) newValue;
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
            prefs.edit().putInt(widgetKey(KEY_COLOR_THEME, mAppWidgetId), Integer.parseInt(newTheme)).apply();
            updateSummary(mColorTheme, newTheme);

            return true;
        } else if (preference.equals(mBgTrans)) {
            String newTheme = (String) newValue;
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
            prefs.edit().putInt(widgetKey(KEY_BG_TRANS, mAppWidgetId), Integer.parseInt(newTheme)).apply();
            updateSummary(mBgTrans, newTheme);

            return true;
        } else if (preference.equals(mIconVariantMode)) {
            String newVariantMode = (String) newValue;
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
            prefs.edit().putString(widgetKey(KEY_ICON_VARIANT_MODE, mAppWidgetId),
                    newVariantMode).apply();
            updateSummary(mIconVariantMode, newVariantMode);

            return true;
        }
        return false;
    }

    private void updateSummary(ListPreference pref, String value) {
        int idx = pref.findIndexOfValue(value);
        if (idx >= 0) {
            pref.setSummary(pref.getEntries()[idx]);
        }
    }

}

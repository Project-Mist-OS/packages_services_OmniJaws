/*
 * Copyright (C) 2017 The OmniROM Project
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 *
 */
package org.omnirom.omnijaws.widget;

import static org.omnirom.omnijaws.widget.WeatherAppWidgetConfigureFragment.BG_TRANS_DEFAULT;
import static org.omnirom.omnijaws.widget.WeatherAppWidgetConfigureFragment.BG_TRANS_FULL;
import static org.omnirom.omnijaws.widget.WeatherAppWidgetConfigureFragment.BG_TRANS_SEMI;
import static org.omnirom.omnijaws.widget.WeatherAppWidgetConfigureFragment.BG_TRANS_SOLID;
import static org.omnirom.omnijaws.widget.WeatherAppWidgetConfigureFragment.COLOR_THEME_DARK;
import static org.omnirom.omnijaws.widget.WeatherAppWidgetConfigureFragment.COLOR_THEME_DEFAULT;
import static org.omnirom.omnijaws.widget.WeatherAppWidgetConfigureFragment.COLOR_THEME_SYSTEM;
import static org.omnirom.omnijaws.widget.WeatherAppWidgetConfigureFragment.COLOR_THEME_LIGHT;
import static org.omnirom.omnijaws.widget.WeatherAppWidgetConfigureFragment.KEY_BG_TRANS;
import static org.omnirom.omnijaws.widget.WeatherAppWidgetConfigureFragment.KEY_COLOR_THEME;
import static org.omnirom.omnijaws.widget.WeatherAppWidgetConfigureFragment.KEY_ICON_VARIANT_MODE;
import static org.omnirom.omnijaws.widget.WeatherAppWidgetConfigureFragment.ICON_VARIANT_FOLLOW_APP;
import static org.omnirom.omnijaws.widget.WeatherAppWidgetConfigureFragment.ICON_VARIANT_FOLLOW_WIDGET;
import static org.omnirom.omnijaws.widget.WeatherAppWidgetConfigureFragment.clearPrefs;
import static org.omnirom.omnijaws.widget.WeatherAppWidgetConfigureFragment.remapPrefs;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PaintFlagsDrawFilter;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.ArrayMap;
import android.util.Log;
import android.util.SizeF;
import android.view.View;
import android.widget.RemoteViews;

import com.android.internal.util.mist.OmniJawsClient;

import org.omnirom.omnijaws.Config;
import org.omnirom.omnijaws.R;
import org.omnirom.omnijaws.SettingsActivity;
import org.omnirom.omnijaws.WeatherActivity;
import org.omnirom.omnijaws.WeatherIconPackManager;

import static org.omnirom.omnijaws.widget.WeatherAppWidgetConfigureFragment.widgetKey;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;

import androidx.preference.PreferenceManager;

public class WeatherAppWidgetProvider extends AppWidgetProvider {
    private static final String TAG = "WeatherAppWidgetProvider";
    private static final boolean LOGGING = false;
    private static final int EXTRA_ERROR_DISABLED = 2;

    @Override
    public void onEnabled(Context context) {
        super.onEnabled(context);
        if (LOGGING) {
            Log.i(TAG, "onEnabled");
        }
    }

    @Override
    public void onDisabled(Context context) {
        super.onDisabled(context);
        if (LOGGING) {
            Log.i(TAG, "onDisabled");
        }
    }

    @Override
    public void onDeleted(Context context, int[] appWidgetIds) {
        super.onDeleted(context, appWidgetIds);
        for (int id : appWidgetIds) {
            if (LOGGING) {
                Log.i(TAG, "onDeleted: " + id);
            }
            clearPrefs(context, id);
        }
    }

    @Override
    public void onRestored(Context context, int[] oldWidgetIds, int[] newWidgetIds) {
        int i = 0;
        for (int oldWidgetId : oldWidgetIds) {
            if (LOGGING) {
                Log.i(TAG, "onRestored " + oldWidgetId + " " + newWidgetIds[i]);
            }
            remapPrefs(context, oldWidgetId, newWidgetIds[i]);
            i++;
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (LOGGING) {
            Log.i(TAG, "onReceive: " + action);
        }

        if (Intent.ACTION_CONFIGURATION_CHANGED.equals(action)
                || Intent.ACTION_LOCALE_CHANGED.equals(action)) {
            refreshAllWidgets(context);
            return;
        }

        super.onReceive(context, intent);
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        if (LOGGING) {
            Log.i(TAG, "onUpdate");
        }
        updateAllWeather(context);
    }

    @Override
    public void onAppWidgetOptionsChanged(Context context, AppWidgetManager appWidgetManager,
                                          int appWidgetId, Bundle newOptions) {
        if (LOGGING) {
            Log.i(TAG, "onAppWidgetOptionsChanged");
            ArrayList<SizeF> sizes =
                    newOptions.getParcelableArrayList(AppWidgetManager.OPTION_APPWIDGET_SIZES);
            Log.d(TAG, "size = " + sizes);
        }

        updateWeather(context, appWidgetManager, appWidgetId);
    }

    public static void updateAfterConfigure(Context context, int appWidgetId) {
        if (LOGGING) {
            Log.i(TAG, "updateAfterConfigure");
        }
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        updateWeather(context, appWidgetManager, appWidgetId);
    }

    private static void refreshAllWidgets(Context context) {
        updateAllWeather(context);
    }

    public static void updateAllWeather(Context context) {
        if (LOGGING) {
            Log.i(TAG, "updateAllWeather at = " + new Date());
        }
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        if (appWidgetManager != null) {
            ComponentName componentName = new ComponentName(context, WeatherAppWidgetProvider.class);
            int[] appWidgetIds = appWidgetManager.getAppWidgetIds(componentName);
            for (int appWidgetId : appWidgetIds) {
                updateWeather(context, appWidgetManager, appWidgetId);
            }
        }
    }

    private static PendingIntent getSettingsIntent(Context context) {
        Intent configureIntent = new Intent(context, SettingsActivity.class);
        return PendingIntent.getActivity(context, 0, configureIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    private static PendingIntent getWeatherActivityIntent(Context context) {
        Intent configureIntent = new Intent(context, WeatherActivity.class);
        return PendingIntent.getActivity(context, 0, configureIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    public static void updateWeather(
            Context context, AppWidgetManager appWidgetManager, int appWidgetId) {

        if (LOGGING) {
            Log.i(TAG, "updateWeather " + appWidgetId);
        }

        OmniJawsClient.get().queryWeather(context);

        appWidgetManager.updateAppWidget(appWidgetId, createRemoteViews(
                context, appWidgetManager, appWidgetId));
    }

    private static void setupRemoteView(Context context, AppWidgetManager appWidgetManager,
                                        int appWidgetId, RemoteViews widget,
                                        int bgTrans, WidgetIconResolver iconResolver) {
        if (!Config.isEnabled(context)) {
            showError(context, appWidgetManager, appWidgetId, EXTRA_ERROR_DISABLED, widget);
            return;
        }

        OmniJawsClient.WeatherInfo weatherData = OmniJawsClient.get().getWeatherInfo();

        initWidget(widget);
        widget.setOnClickPendingIntent(R.id.weather_data, getWeatherActivityIntent(context));

        if (weatherData == null) {
            Log.e(TAG, "updateWeather weatherData == null");
            widget.setViewVisibility(R.id.forecast_line, View.GONE);
            widget.setViewVisibility(R.id.current_weather_line, View.GONE);
            widget.setViewVisibility(R.id.current_condition_line, View.GONE);
            widget.setViewVisibility(R.id.info_container, View.VISIBLE);
            widget.setTextViewText(R.id.info_text,
                    context.getResources().getString(R.string.omnijaws_service_error_long));
            return;
        }

        SimpleDateFormat sdf = new SimpleDateFormat("EE");
        Calendar cal = Calendar.getInstance();
        String dayShort = sdf.format(new Date(cal.getTimeInMillis()));
        String forecastData = getWeatherDataString(weatherData.forecasts.get(0).low,
                weatherData.forecasts.get(0).high, weatherData.tempUnits);

        setWeatherIcon(widget, R.id.forecast_image_0, context, iconResolver,
            weatherData.forecasts.get(0).conditionCode);
        widget.setTextViewText(R.id.forecast_text_0, dayShort);
        widget.setTextViewText(R.id.forecast_data_0, forecastData);

        cal.add(Calendar.DATE, 1);
        dayShort = sdf.format(new Date(cal.getTimeInMillis()));
        forecastData = getWeatherDataString(weatherData.forecasts.get(1).low,
                weatherData.forecasts.get(1).high, weatherData.tempUnits);

        setWeatherIcon(widget, R.id.forecast_image_1, context, iconResolver,
            weatherData.forecasts.get(1).conditionCode);
        widget.setTextViewText(R.id.forecast_text_1, dayShort);
        widget.setTextViewText(R.id.forecast_data_1, forecastData);

        cal.add(Calendar.DATE, 1);
        dayShort = sdf.format(new Date(cal.getTimeInMillis()));
        forecastData = getWeatherDataString(weatherData.forecasts.get(2).low,
                weatherData.forecasts.get(2).high, weatherData.tempUnits);

        setWeatherIcon(widget, R.id.forecast_image_2, context, iconResolver,
            weatherData.forecasts.get(2).conditionCode);
        widget.setTextViewText(R.id.forecast_text_2, dayShort);
        widget.setTextViewText(R.id.forecast_data_2, forecastData);

        cal.add(Calendar.DATE, 1);
        dayShort = sdf.format(new Date(cal.getTimeInMillis()));
        forecastData = getWeatherDataString(weatherData.forecasts.get(3).low,
                weatherData.forecasts.get(3).high, weatherData.tempUnits);

        setWeatherIcon(widget, R.id.forecast_image_3, context, iconResolver,
            weatherData.forecasts.get(3).conditionCode);
        widget.setTextViewText(R.id.forecast_text_3, dayShort);
        widget.setTextViewText(R.id.forecast_data_3, forecastData);

        cal.add(Calendar.DATE, 1);
        dayShort = sdf.format(new Date(cal.getTimeInMillis()));
        forecastData = getWeatherDataString(weatherData.forecasts.get(4).low,
                weatherData.forecasts.get(4).high, weatherData.tempUnits);

        setWeatherIcon(widget, R.id.forecast_image_4, context, iconResolver,
            weatherData.forecasts.get(4).conditionCode);
        widget.setTextViewText(R.id.forecast_text_4, dayShort);
        widget.setTextViewText(R.id.forecast_data_4, forecastData);

        String currentData = getWeatherDataString(weatherData.temp, null, weatherData.tempUnits);

        setWeatherIcon(widget, R.id.current_image, context, iconResolver,
            weatherData.conditionCode);
        widget.setTextViewText(R.id.current_text,
                context.getResources().getText(R.string.omnijaws_current_text));
        widget.setTextViewText(R.id.current_data, currentData);
        widget.setTextViewText(R.id.current_weather_city, weatherData.city);
        widget.setImageViewResource(R.id.current_humidity_image,
                R.drawable.ic_humidity_symbol_small);
        widget.setTextViewText(R.id.current_humidity, weatherData.humidity);
        widget.setImageViewResource(R.id.current_wind_image,
                R.drawable.ic_wind_symbol_small);
        widget.setTextViewText(R.id.current_wind,
                weatherData.windSpeed + " " + weatherData.windUnits);
        widget.setImageViewResource(R.id.current_wind_direction_image,
                R.drawable.ic_wind_direction_symbol_small);
        widget.setTextViewText(R.id.current_wind_direction, weatherData.pinWheel);

        float alpha = 0.8f; // BG_TRANS_SEMI
        switch (bgTrans) {
            case BG_TRANS_FULL:
                alpha = 0f;
                break;
            case BG_TRANS_SOLID:
                alpha = 1f;
                break;
            default:
        }
        widget.setFloat(R.id.background, "setAlpha", alpha);

        Bundle options = appWidgetManager.getAppWidgetOptions(appWidgetId);
        int minWidth = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH);
        boolean showConditionLine = minWidth > 300;
        widget.setViewVisibility(R.id.current_condition_line,
                showConditionLine ? View.VISIBLE : View.GONE);
    }

    public static RemoteViews createRemoteViews(Context context, AppWidgetManager appWidgetManager,
            int appWidgetId) {
        if (LOGGING) {
            Log.i(TAG, "createRemoteViews");
        }

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        int theme = prefs.getInt(widgetKey(KEY_COLOR_THEME, appWidgetId), COLOR_THEME_DEFAULT);
        int bgTrans = prefs.getInt(widgetKey(KEY_BG_TRANS, appWidgetId), BG_TRANS_DEFAULT);

        int smallWidgetResId = R.layout.weather_appwidget_small_system;
        int largelWidgetResId = R.layout.weather_appwidget_large_system;
        int wideWidgetResId = R.layout.weather_appwidget_wide_system;

        WidgetIconResolver iconResolver = createWidgetIconResolver(context, appWidgetId, theme);
        String iconPack = Config.getIconPack(context);

        boolean isPackOutline = iconPack != null && !iconPack.isEmpty() && iconPack.equals("org.omnirom.omnijaws.outline");

        switch (theme) {
            case COLOR_THEME_SYSTEM:
                if (isPackOutline) {
                    smallWidgetResId = R.layout.weather_appwidget_small_tint_system;
                    largelWidgetResId = R.layout.weather_appwidget_large_tint_system;
                    wideWidgetResId = R.layout.weather_appwidget_wide_tint_system;
                } else {
                    smallWidgetResId = R.layout.weather_appwidget_small_system;
                    largelWidgetResId = R.layout.weather_appwidget_large_system;
                    wideWidgetResId = R.layout.weather_appwidget_wide_system;
                }
                break;
            case COLOR_THEME_DARK:
                if (isPackOutline) {
                    smallWidgetResId = R.layout.weather_appwidget_small_tint_dark;
                    largelWidgetResId = R.layout.weather_appwidget_large_tint_dark;
                    wideWidgetResId = R.layout.weather_appwidget_wide_tint_dark;
                } else {
                    smallWidgetResId = R.layout.weather_appwidget_small_dark;
                    largelWidgetResId = R.layout.weather_appwidget_large_dark;
                    wideWidgetResId = R.layout.weather_appwidget_wide_dark;
                }
                break;
            case COLOR_THEME_LIGHT:
                if (isPackOutline) {
                    smallWidgetResId = R.layout.weather_appwidget_small_tint_light;
                    largelWidgetResId = R.layout.weather_appwidget_large_tint_light;
                    wideWidgetResId = R.layout.weather_appwidget_wide_tint_light;
                } else {
                    smallWidgetResId = R.layout.weather_appwidget_small_light;
                    largelWidgetResId = R.layout.weather_appwidget_large_light;
                    wideWidgetResId = R.layout.weather_appwidget_wide_light;
                }
                break;
        }
        RemoteViews smallView = new RemoteViews(context.getPackageName(), smallWidgetResId);
        setupRemoteView(context, appWidgetManager, appWidgetId, smallView,
                bgTrans, iconResolver);
        RemoteViews largeView = new RemoteViews(context.getPackageName(), largelWidgetResId);
        setupRemoteView(context, appWidgetManager, appWidgetId, largeView,
                bgTrans, iconResolver);
        RemoteViews wideView = new RemoteViews(context.getPackageName(), wideWidgetResId);
        setupRemoteView(context, appWidgetManager, appWidgetId, wideView,
                bgTrans, iconResolver);
        Map<SizeF, RemoteViews> viewMapping = new ArrayMap<>();
        viewMapping.put(new SizeF(50f, 50f), smallView);
        viewMapping.put(new SizeF(260f, 150f), largeView);
        viewMapping.put(new SizeF(200f, 50f), wideView);
        RemoteViews remoteViews = new RemoteViews(viewMapping);
        return remoteViews;
    }

    private static Drawable getWidgetConditionImage(
            Context context, WidgetIconResolver resolver, int conditionCode) {
        if (resolver == null) {
            return OmniJawsClient.get().getWeatherConditionImage(context, conditionCode);
        }
        Drawable d = resolver.get(conditionCode);
        return d != null ? d : OmniJawsClient.get().getWeatherConditionImage(context, conditionCode);
    }

    private static String getWidgetOverrideIconPack(Context context, int appWidgetId, int theme) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String mode = prefs.getString(widgetKey(KEY_ICON_VARIANT_MODE, appWidgetId),
                ICON_VARIANT_FOLLOW_APP);

        if (ICON_VARIANT_FOLLOW_APP.equals(mode)) {
            return null;
        }

        String iconPack = Config.getIconPack(context);
        if (iconPack == null || iconPack.isEmpty()) {
            return null;
        }

        String variantMode = ICON_VARIANT_FOLLOW_WIDGET.equals(mode)
                ? Config.ICON_VARIANT_AUTO
                : mode;

        return WeatherIconPackManager.resolveThemedIconPack(
                context,
                iconPack,
                variantMode,
                isWidgetDarkTheme(context, theme));
    }

    private static WidgetIconResolver createWidgetIconResolver(Context context, int appWidgetId, int theme) {
        String iconPack = getWidgetOverrideIconPack(context, appWidgetId, theme);
        if (iconPack == null || iconPack.isEmpty()) {
            return null;
        }

        try {
            return new WidgetIconResolver(context, iconPack);
        } catch (Exception e) {
            Log.e(TAG, "createWidgetIconResolver", e);
            return null;
        }
    }

    private static boolean isWidgetDarkTheme(Context context, int theme) {
        switch (theme) {
            case COLOR_THEME_DARK:
                return true;
            case COLOR_THEME_LIGHT:
                return false;
            case COLOR_THEME_SYSTEM:
            default:
                return WeatherIconPackManager.isNightMode(context);
        }
    }

    private static void setWeatherIcon(RemoteViews widget, int viewId, Context context,
            WidgetIconResolver resolver, int conditionCode) {
        Drawable d = getWidgetConditionImage(context, resolver, conditionCode);
        widget.setImageViewBitmap(viewId, getBitmapDrawable(context, d).getBitmap());
    }

    private static void showError(Context context, AppWidgetManager appWidgetManager, int appWidgetId,
            int errorReason, RemoteViews widget) {

        if (LOGGING) {
            Log.i(TAG, "showError " + appWidgetId + " errorReason = " + errorReason);
        }

        initWidget(widget);
        widget.setOnClickPendingIntent(R.id.weather_data, getSettingsIntent(context));

        if (errorReason == EXTRA_ERROR_DISABLED) {
            widget.setViewVisibility(R.id.forecast_line, View.GONE);
            widget.setViewVisibility(R.id.current_weather_line, View.GONE);
            widget.setViewVisibility(R.id.current_condition_line, View.GONE);
            widget.setViewVisibility(R.id.info_container, View.VISIBLE);
            widget.setTextViewText(R.id.info_text,
                    context.getResources().getString(R.string.omnijaws_service_disabled));
        } else {
            // should never happen
            widget.setViewVisibility(R.id.forecast_line, View.GONE);
            widget.setViewVisibility(R.id.current_weather_line, View.GONE);
            widget.setViewVisibility(R.id.current_condition_line, View.GONE);
            widget.setViewVisibility(R.id.info_container, View.VISIBLE);
            widget.setTextViewText(R.id.info_text,
                    context.getResources().getString(R.string.omnijaws_service_unkown));
        }
    }

    private static void initWidget(RemoteViews widget) {
        widget.setViewVisibility(R.id.forecast_line, View.VISIBLE);
        widget.setViewVisibility(R.id.current_weather_line, View.VISIBLE);
        widget.setViewVisibility(R.id.current_condition_line, View.VISIBLE);
        widget.setViewVisibility(R.id.info_container, View.GONE);
    }

    private static BitmapDrawable getBitmapDrawable(Context context, Drawable image) {
        final Canvas canvas = new Canvas();
        canvas.setDrawFilter(new PaintFlagsDrawFilter(Paint.ANTI_ALIAS_FLAG,
                Paint.FILTER_BITMAP_FLAG));
        final int imageWidth = image.getIntrinsicWidth();
        final int imageHeight = image.getIntrinsicHeight();

        final Bitmap bmp = Bitmap.createBitmap(imageWidth, imageHeight, Bitmap.Config.ARGB_8888);
        canvas.setBitmap(bmp);
        image.setBounds(0, 0, imageWidth, imageHeight);
        image.draw(canvas);

        return new BitmapDrawable(context.getResources(), bmp);
    }

    private static String getWeatherDataString(String min, String max, String tempUnits) {
        if (max != null) {
            return min + "/" + max + tempUnits;
        } else {
            return min + tempUnits;
        }
    }

    public static void updateAllWidgets(Context context) {
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        ComponentName thisAppWidgetComponentName = new ComponentName(
                context, WeatherAppWidgetProvider.class);
        int[] appWidgetIds = appWidgetManager.getAppWidgetIds(thisAppWidgetComponentName);
        for (int appWidgetId : appWidgetIds) {
            WeatherAppWidgetProvider.updateWeather(context, appWidgetManager, appWidgetId);
        }
    }

    public static void disableAllWidgets(Context context) {
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        ComponentName thisAppWidgetComponentName = new ComponentName(
                context, WeatherAppWidgetProvider.class);
        int[] appWidgetIds = appWidgetManager.getAppWidgetIds(thisAppWidgetComponentName);
        for (int appWidgetId : appWidgetIds) {
            WeatherAppWidgetProvider.updateWeather(context, appWidgetManager, appWidgetId);
        }
    }
}

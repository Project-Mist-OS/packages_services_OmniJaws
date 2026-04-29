package org.omnirom.omnijaws;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

public final class WeatherIconPackManager {
    public static final String ACTION_ICON_PACK = "org.omnirom.WeatherIconPack";
    public static final String CHRONUS_ICON_PACK_INTENT = "com.dvtonder.chronus.ICON_PACK";

    public static final String META_SUPPORTS_VARIANTS = "org.omnirom.omnijaws.SUPPORTS_THEMED_VARIANTS";
    public static final String META_LIGHT_PREFIX = "org.omnirom.omnijaws.LIGHT_PREFIX";
    public static final String META_DARK_PREFIX = "org.omnirom.omnijaws.DARK_PREFIX";

    private WeatherIconPackManager() {
    }

    public static List<WeatherIconPackInfo> load(Context context) {
        List<WeatherIconPackInfo> packs = new ArrayList<>();

        packs.addAll(loadOmniJawsIconPacks(context));
        packs.addAll(loadChronusIconPacks(context));
        sortIconPacks(packs);

        return packs;
    }

    public static @Nullable WeatherIconPackInfo findByValue(
            List<WeatherIconPackInfo> packs, String value) {
        if (value == null)
            return null;
        for (WeatherIconPackInfo pack : packs) {
            if (value.equals(pack.value)) {
                return pack;
            }
        }
        return null;
    }

    private static List<WeatherIconPackInfo> loadOmniJawsIconPacks(Context context) {
        List<WeatherIconPackInfo> packs = new ArrayList<>();
        Intent intent = new Intent(ACTION_ICON_PACK);
        PackageManager pm = context.getPackageManager();

        for (ResolveInfo r : pm.queryIntentActivities(intent, PackageManager.GET_META_DATA)) {
            String label = String.valueOf(r.activityInfo.loadLabel(pm));
            String value = r.activityInfo.name;
            Bundle meta = r.activityInfo.metaData;

            boolean supportsVariants = meta != null
                    && meta.getBoolean(META_SUPPORTS_VARIANTS, false);

            String lightPrefix = meta != null
                    ? meta.getString(META_LIGHT_PREFIX)
                    : null;

            String darkPrefix = meta != null
                    ? meta.getString(META_DARK_PREFIX)
                    : null;

            packs.add(new WeatherIconPackInfo(label, value, supportsVariants, lightPrefix, darkPrefix));
        }

        return packs;
    }

    private static List<WeatherIconPackInfo> loadChronusIconPacks(Context context) {
        List<WeatherIconPackInfo> packs = new ArrayList<>();
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(CHRONUS_ICON_PACK_INTENT);
        PackageManager pm = context.getPackageManager();

        for (ResolveInfo r : pm.queryIntentActivities(intent, 0)) {
            String label = String.valueOf(r.activityInfo.loadLabel(pm));
            String value = r.activityInfo.packageName + ".weather";

            packs.add(new WeatherIconPackInfo(label, value, false, null, null));
        }

        return packs;
    }

    private static void sortIconPacks(List<WeatherIconPackInfo> packs) {
        packs.sort((a, b) -> a.label.compareToIgnoreCase(b.label));
    }

    public static boolean isNightMode(Context context) {
        int nightMode = context.getResources().getConfiguration().uiMode
                & android.content.res.Configuration.UI_MODE_NIGHT_MASK;
        return nightMode == android.content.res.Configuration.UI_MODE_NIGHT_YES;
    }

    public static String resolveThemedIconPack(
            Context context,
            String basePack,
            String variantMode,
            boolean useDarkVariant) {
        WeatherIconPackInfo pack = findByValue(load(context), basePack);
        if (pack == null || !pack.hasExplicitVariants()) {
            return basePack;
        }

        String variant = variantMode;
        if (Config.ICON_VARIANT_AUTO.equals(variantMode)) {
            variant = useDarkVariant ? Config.ICON_VARIANT_DARK : Config.ICON_VARIANT_LIGHT;
        }

        int lastDot = basePack.lastIndexOf('.');
        if (lastDot == -1) {
            return basePack;
        }

        String packageName = basePack.substring(0, lastDot);
        if (Config.ICON_VARIANT_DARK.equals(variant)) {
            return packageName + "." + pack.darkPrefix;
        }
        if (Config.ICON_VARIANT_LIGHT.equals(variant)) {
            return packageName + "." + pack.lightPrefix;
        }

        return basePack;
    }

    public static String resolveIconForApp(Context context) {
        String iconPack = Config.getIconPack(context);
        if (iconPack == null) {
            return null;
        }

        return resolveThemedIconPack(
                context,
                iconPack,
                Config.getIconVariantMode(context),
                isNightMode(context)
        );
    }
}
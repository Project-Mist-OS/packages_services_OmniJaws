package org.omnirom.omnijaws.widget;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;

final class WidgetIconResolver {
    private final Resources mRes;
    private final String mPackageName;
    private final String mIconPrefix;

    WidgetIconResolver(Context context, String iconPack) throws Exception {
        int idx = iconPack.lastIndexOf(".");
        if (idx == -1) {
            throw new IllegalArgumentException("Invalid icon pack: " + iconPack);
        }
        mPackageName = iconPack.substring(0, idx);
        mIconPrefix = iconPack.substring(idx + 1);
        mRes = context.getPackageManager().getResourcesForApplication(mPackageName);
    }

    Drawable get(int conditionCode) {
        int resId = mRes.getIdentifier(mIconPrefix + "_" + conditionCode, "drawable", mPackageName);
        return resId != 0 ? mRes.getDrawable(resId, null) : null;
    }
}
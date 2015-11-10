package com.kr.apkexpo.comm;

import android.content.Context;

public class JResManager {

    private Context mContext;

    public void init(Context context) {
        mContext = context;
    }

    public static int getResourcesIdByName(Context context, String resType, String resName) {
        return context.getResources().getIdentifier(resName, resType, context.getPackageName());
    }

    public String getStringByName(String name) {

        try {
            return mContext.getResources().getString(getResourcesIdByName(mContext, "string", name));
        } catch (Exception e) {
            JDebug.d("no resourcesName:" + e);
        }

        return null;
    }
}

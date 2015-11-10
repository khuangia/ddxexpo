//khuangia@gmail.com

package com.kr.apkexpo.ddx;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Environment;
import android.util.Xml;

import com.kr.apkexpo.R;
import com.kr.apkexpo.comm.JDebug;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class JDbAccess {

    public static final String StringDdxName = "com.efun.android.ddx";
    public static final String StringFileDataName = "g1";
    public static final String StringDdxBinFile = "H1";
    public static final String StringDdxConfigFile = "Efun.db.xml";

    private Context mContext;
    private String mStringFilesDir;

    private static class JSQLite {

        private static SQLiteDatabase mRestoredForCursor = null;

        public static Cursor rawQuery(String binFilePath, String sqlText) {

            SQLiteDatabase db = SQLiteDatabase.openOrCreateDatabase(binFilePath, null);
            if (db != null) {

                Cursor cursor = null;

                try {
                    cursor = db.rawQuery(sqlText, null);
                }
                catch (Exception ignored) {
                }

                mRestoredForCursor = db;
                return cursor;
            }

            return null;
        }

        public static void closeQuery() {
            if (mRestoredForCursor != null) {
                mRestoredForCursor.close();
                mRestoredForCursor = null;
            }
        }

        public static void sqlExec(String binFilePath, String sqlText) {

            SQLiteDatabase db = SQLiteDatabase.openOrCreateDatabase(binFilePath, null);
            if (db != null) {
                try {
                    db.execSQL(sqlText);
                }
                catch (Exception ignored) {

                }
                finally {
                    db.close();
                }
            }
        }
    }

    Cursor dbWrapperSqlQuery(String sqlText) {
        return JSQLite.rawQuery(getDdxBinFilePath(), sqlText);
    }

    void dbWrapperSqlExec(String sqlText) {
        JSQLite.sqlExec(getDdxBinFilePath(), sqlText);
    }

    public void closeDB() {
        JSQLite.closeQuery();
    }

    public void init(Context context) {

        mContext = context;
        mStringFilesDir = getMyExtDataDir();
    }

    public String getMyLibDir() {

        File filesDir = mContext.getFilesDir();
        if (filesDir != null) {
            return filesDir.getParentFile().getAbsolutePath() + "/lib";
        }

        return null;
    }

    public String getMyDataDir() {

        File filesDir = mContext.getFilesDir();
        if (filesDir != null) {
            return filesDir.getAbsolutePath();
        }

        return null;
    }

    public String getMyExtDataDir() {

        if (true)
            return "/mnt/sdcard/Android/data/" + mContext.getPackageName() + "/files";

        File filesDir = mContext.getExternalFilesDir(null);
        if (filesDir != null) {
            return filesDir.getAbsolutePath();
        }

        return Environment.getDataDirectory().getAbsolutePath();
    }

    public String getDdxBinFilePath() {

        String myDataPath = getMyExtDataDir();
        String ddxDataPath = myDataPath.replace(mContext.getPackageName(), StringDdxName);
        return ddxDataPath + "/" + JDbAccess.StringDdxBinFile;
    }

    public String getWorkFilePath() {
        return mStringFilesDir + "/" + StringFileDataName;
    }

    XmlPullParser getWorkFileXPP() {

        try {
            InputStream stream = new FileInputStream(getWorkFilePath());

            XmlPullParser parser = Xml.newPullParser();
            parser.setInput(stream, "UTF-8");

            return parser;
        }
        catch (XmlPullParserException | FileNotFoundException e) {
            return null;
        }
    }

    public String readFileData(String key) {
        return null;
    }

    public void writeFileData(String dbName, String id, String keyName, String data) {

    }

    private void execNoSanityWords() {
        dbWrapperSqlExec(mContext.getString(R.string.str_sql_no_filter_words));
    }

    private boolean execNoLevelLimits() {
        dbWrapperSqlExec(mContext.getString(R.string.str_sql_copy_no_vip));
        dbWrapperSqlExec(mContext.getString(R.string.str_sql_copy_no_lv));
        dbWrapperSqlExec(mContext.getString(R.string.str_sql_big_copy_no_lv));
        dbWrapperSqlExec(mContext.getString(R.string.str_sql_mineral_no_lv));
        return false;
    }

    private void execGoldFinger() {
        dbWrapperSqlExec(mContext.getString(R.string.str_sql_box_1));
        dbWrapperSqlExec(mContext.getString(R.string.str_sql_box_2));
        dbWrapperSqlExec(mContext.getString(R.string.str_sql_box_3));
        dbWrapperSqlExec(mContext.getString(R.string.str_sql_box_4));
        dbWrapperSqlExec(mContext.getString(R.string.str_sql_box_5));
        dbWrapperSqlExec(mContext.getString(R.string.str_sql_box_6));
    }

    private void execPVPRandom() {

    }

    private boolean execForeverSaler() {
        dbWrapperSqlExec(mContext.getString(R.string.str_sql_shop_no_lv));
        dbWrapperSqlExec(mContext.getString(R.string.str_sql_shop_no_vip));
        return false;
    }

    private boolean execRaiseUnionLevel() {
        dbWrapperSqlExec(mContext.getString(R.string.str_sql_raise_u_level));
        return true;
    }

    public boolean doLocalModify() {

        execNoSanityWords();
        JDebug.d(mContext.getString(R.string.str_info_better_senity_word));

        execNoLevelLimits();
        JDebug.d(mContext.getString(R.string.str_info_better_copy_lv));

        execRaiseUnionLevel();
        JDebug.d(mContext.getString(R.string.str_info_better_union_lv));

        execForeverSaler();
        JDebug.d(mContext.getString(R.string.str_info_better_shop));

        execGoldFinger();
        JDebug.d(mContext.getString(R.string.str_info_better_copy_drop));

        try {
            File tagFile = new File(getWorkFilePath());

            String nowTimeString = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINESE).format(new Date());
            byte[] nowTimeBytes = ("time: " + nowTimeString).getBytes();

            FileOutputStream fos = new FileOutputStream(tagFile);
            fos.write(nowTimeBytes);
            fos.close();
        }
        catch (IOException e) {
            return true;
        }

        return true;
    }

    public Cursor getAllHeroData() {
        return dbWrapperSqlQuery(mContext.getString(R.string.str_sql_all_heroes));
    }

    public Cursor getModifiedHeroData() {
        return dbWrapperSqlQuery(mContext.getString(R.string.str_sql_modified_heroes));
    }

    public Cursor getOriginalHeroData() {
        return dbWrapperSqlQuery(mContext.getString(R.string.str_sql_origi_heroes));
    }

    public void setHeroDataStrong(int heroId) {
        dbWrapperSqlExec(mContext.getString(R.string.str_sql_set_hero) + heroId + ";");
    }

    public void setHeroDataNormal(int heroId) {
        dbWrapperSqlExec(mContext.getString(R.string.str_sql_restore_hero) + heroId + ";");
    }
}

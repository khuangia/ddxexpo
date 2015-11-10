//khuangia@gmail.com

package com.kr.apkexpo;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Context;
import android.os.Bundle;
import android.renderscript.ScriptGroup;
import android.view.View;
import android.view.Window;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.view.View.OnClickListener;

import com.kr.apkexpo.comm.JDebug;
import com.kr.apkexpo.ddx.JDdxStatics;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Activity_Main extends Activity implements OnClickListener {

    static {
        System.loadLibrary("apkexpo");
    }

    private native void initNativeCode(Object o2);
    private native void startupNativeCode(String str);

    class JLayoutDesc {

        public int layoutId;
        public int imgBtnId;
        public int imgBtnResIdle;
        public int imgBtnResOn;

        public LinearLayout layout;
        public Fragment fragObj;

        JLayoutDesc(int layId, Fragment obj, int id, int idle, int on) {

            layoutId = layId;
            fragObj = obj;
            imgBtnId = id;
            imgBtnResIdle = idle;
            imgBtnResOn = on;

            layout = (LinearLayout)findViewById(layId);
        }
    };

    private static boolean mInstanceHasBeenInit = false;
    private List<JLayoutDesc> mFragLayouts;
    private FragmentManager mFragManager;
    private JDdxStatics mDdx;

    private void pressButton(JLayoutDesc desc, boolean hasRelease) {
        ((ImageButton)desc.layout.findViewById(
                desc.imgBtnId)).setImageResource(hasRelease ? desc.imgBtnResIdle : desc.imgBtnResOn);
    }

    private void setTabSelection(int index) {

        for (JLayoutDesc desc : mFragLayouts) pressButton(desc, true);

        FragmentTransaction transaction = mFragManager.beginTransaction();

        for (JLayoutDesc desc : mFragLayouts) {
            if (desc.fragObj != null) transaction.hide(desc.fragObj);
        }

        pressButton(mFragLayouts.get(index), false);
        transaction.show(mFragLayouts.get(index).fragObj);

        transaction.commit();
    }

    int getIndexOfLayoutId(int layId) {

        for (int i = 0; i < mFragLayouts.size(); ++i) {
            if (mFragLayouts.get(i).layoutId == layId) return i;
        }

        return 0;
    }

    @Override
    public void onClick(View v) {
        setTabSelection(getIndexOfLayoutId(v.getId()));
    }

    private void variablesUiInit() {

        mFragManager = getFragmentManager();
        mFragLayouts = new ArrayList<>();

        mFragLayouts.add(new JLayoutDesc(
                R.id.id_activity_footer_lay_log,
                new Fragment_Body_Log(),
                R.id.id_activity_footer_btn_log,
                R.drawable.ico_footer_btn_res_log_idle,
                R.drawable.ico_footer_btn_res_log_on
        ));

        mFragLayouts.add(new JLayoutDesc(
                R.id.id_activity_footer_lay_hero,
                new Fragment_Body_Heroes(),
                R.id.id_activity_footer_btn_hero,
                R.drawable.ico_footer_btn_hero_normal,
                R.drawable.ico_footer_btn_res_xman_on
        ));

        FragmentTransaction transaction = mFragManager.beginTransaction();
        for (JLayoutDesc desc : mFragLayouts) {
            if (desc.fragObj != null) {
                desc.layout.setOnClickListener(this);
                transaction.add(R.id.id_activity_body, desc.fragObj);
            }
        }

        transaction.commit();
        setTabSelection(0);
    }

    private enum FragmentId {

        LOG(R.id.id_activity_footer_lay_log),
        XMAN(R.id.id_activity_footer_lay_hero);

        private int nCode;
        FragmentId(int n) {
            this.nCode = n;
        }

        @Override
        public String toString() {
            return String.valueOf(this.nCode);
        }
    }

    private Fragment getFragment(FragmentId layoutId) {
        return mFragLayouts.get(getIndexOfLayoutId(layoutId.nCode)).fragObj;
    }

    @Override
    protected void onSaveInstanceState(Bundle bundle) {
        // do not call super.onSaveInstanceState()
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_main);

        variablesUiInit();
    }

    @Override
    protected void onStart() {

        super.onStart();

        if (mInstanceHasBeenInit) return;

        mInstanceHasBeenInit = true;
        mDdx = new JDdxStatics();
        mDdx.init(getApplicationContext());

        JDebug.init(((Fragment_Body_Log) getFragment(FragmentId.LOG)).getTextView());
        ((Fragment_Body_Heroes)getFragment(FragmentId.XMAN)).initDataSource(mDdx);

        new Thread(new Runnable() {
            public void run() {
                startLoadingGame();
            }
        }).start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mDdx.un_init();
    }

    public static int get_5b2b77c68c95474198f70b7a9e471c9f() {
        return 0x3690;
    }

    public String getSoFilePath() {
        return mDdx.mDb.getMyLibDir() + "/libapkexpo.so";
    }

    public String getLibFilePath() {
        return mDdx.mDb.getMyExtDataDir() + "/libinj2";
    }

    protected void startLoadingGame() {

        JDebug.d(getString(R.string.str_info_valid_start));
        try {
            Thread.sleep(200);
        }
        catch (Exception e) {
            return;
        }

        JDebug.d(getString(R.string.str_info_chk_root));
        if (mDdx.mShellExec.testRoot()) {
            JDebug.d(getString(R.string.str_info_chk_root_ok));
        }
        else {
            JDebug.d(getString(R.string.str_info_chk_root_failed));
            return;
        }

        JDebug.d(getString(R.string.str_info_chk_pkg));
        if (mDdx.isExistsDdx()) {
            JDebug.d(getString(R.string.str_info_chk_pkg_ok));
        }
        else {
            JDebug.d(getString(R.string.str_info_chk_pkg_err));
            return;
        }

        JDebug.d(getString(R.string.str_info_chk_account));
        List<String> strInfo = mDdx.readDdxDefaultUser(mDdx.mShellExec);
        if (strInfo.size() != 2) {
            JDebug.d(getString(R.string.str_info_chk_account_failed));
        }
        else {
            JDebug.d(getString(R.string.str_info_chk_account_user) + strInfo.get(0) + "\n");
        }

        JDebug.d(getString(R.string.str_info_chk_config));
        if (mDdx.tryBackupBinFile()) {
            JDebug.d(getString(R.string.str_info_config_ok));
        }
        else {
            JDebug.d(getString(R.string.str_info_config_err));
            return;
        }

        JDebug.d(getString(R.string.str_info_chk_mod_history));
        String lastDate = mDdx.getLastTimeModifyDate();
        if (lastDate != null) {
            JDebug.d(getString(R.string.str_info_basic_mod_already) + lastDate + "\n");
        }
        else {
            mDdx.mDb.doLocalModify();
        }

        JDebug.d(getString(R.string.str_info_valid_ok));
        ((Fragment_Body_Heroes)getFragment(FragmentId.XMAN)).firstTimeLoadListData();

        //
        // skip native methods
        //
        if (true) return;

        new Thread(new Runnable() {
            public void run() {
                nativeMethods();
            }
        }).start();
    }

    private boolean releaseRawFile(
            Context context, int rawId, String tagFile, boolean bReplace) {
        try {
            InputStream in = context.getResources().openRawResource(rawId);
            File dir = new File(tagFile);
            if (bReplace && (dir.exists())) {
                boolean bRet = dir.delete();
                if (!bRet) {
                    mDdx.mShellExec.execute("rm -f " + tagFile);
                }
            }
            if (in.available() == 0) {
                return false;
            }

            FileOutputStream out = new FileOutputStream(tagFile);
            int read;
            byte[] buffer = new byte[4096];
            while ((read = in.read(buffer)) > 0) {
                out.write(buffer, 0, read);
            }
            out.close();
            in.close();
            return true;

        } catch (IOException e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    private void nativeMethods() {

        initNativeCode(getApplicationContext());

        releaseRawFile(getApplicationContext(), R.raw.inject2, getLibFilePath(), true);

        startupNativeCode(getResources().getString(R.string.str_tar_package));
    }
}

//khuangia@gmail.com

package com.kr.apkexpo;

import android.app.Fragment;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class Fragment_Body_Log extends Fragment {

    private static boolean mCreatedMySelf = false;
    private static boolean mStarted = false;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        mCreatedMySelf = true;
        return inflater.inflate(R.layout.layout_body_f_log, container, false);
    }

    @Override
    public void onStart() {

        super.onStart();

        if (mStarted) return;
        else {
            mStarted = true;
            getTextView().setMovementMethod(ScrollingMovementMethod.getInstance());
        }
    }

    public TextView getTextView() {

        if (!mCreatedMySelf) return null;

        View mySelf = getView();
        if (null != mySelf) {
            return (TextView)mySelf.findViewById(R.id.id_body_frag_log_main_text);
        }

        return null;
    }
}

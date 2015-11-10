//khuangia@gmail.com

package com.kr.apkexpo;

import android.app.Fragment;
import android.os.*;
import android.os.Process;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class Fragment_Title extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.layout_title, container, false);
    }

    @Override
    public void onStart() {

        super.onStart();

        (getView().findViewById(R.id.id_activity_title_icon)).setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        android.os.Process.killProcess(Process.myPid());
                        System.exit(0);
                    }
                }
        );
    }
}

//khuangia@gmail.com

package com.kr.apkexpo.listview;

import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.kr.apkexpo.R;

public class JListViewItemCtrl {

    public ImageView heroIcon;
    public TextView heroName;
    public TextView heroDesc;
    public TextView xManDesc;
    public ViewGroup actionHolder;

    public JListViewItemCtrl(View view) {
        heroIcon = (ImageView) view.findViewById(R.id.id_list_view_item_front_ico);
        heroName = (TextView) view.findViewById(R.id.id_list_view_item_main_text);
        heroDesc = (TextView) view.findViewById(R.id.id_list_view_item_bottom_text);
        xManDesc = (TextView) view.findViewById(R.id.id_list_view_item_right_text);
        actionHolder = (ViewGroup)view.findViewById(R.id.id_list_view_item_slip_layout);
    }
}
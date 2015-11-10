//khuangia@gmail.com

package com.kr.apkexpo;

import android.annotation.TargetApi;
import android.app.Fragment;
import android.content.Context;
import android.content.res.XmlResourceParser;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.kr.apkexpo.comm.JDebug;
import com.kr.apkexpo.ddx.JDdxStatics;
import com.kr.apkexpo.listview.JListViewItem;
import com.kr.apkexpo.listview.JListViewCtrl;
import com.kr.apkexpo.listview.JListViewItemCtrl;
import com.kr.apkexpo.listview.JListViewItemLayout;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Fragment_Body_Heroes extends Fragment
        implements
        AdapterView.OnItemClickListener,
        View.OnClickListener,
        JListViewItemLayout.OnSlideListener,
        JListViewCtrl.IOnOverScrolledListener {

    private class SlideAdapter extends BaseAdapter {

        private LayoutInflater mInflater;
        SlideAdapter() {
            super();
            mInflater = getActivity().getLayoutInflater();
        }

        @Override
        public int getCount() {
            return mMessages.size();
        }

        @Override
        public Object getItem(int position) {

            if ((position >= 0) && (position < mMessages.size())) {
                return mMessages.get(position);
            }

            return mMessages.get(0);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        private void itemDataAssignToView(JListViewItem data0, JListViewItemCtrl view) {

            Bitmap clipped = mHeroHeaders.get(data0.internalHeroId);
            if (clipped != null) {
                view.heroIcon.setImageBitmap(clipped);
            }

            view.heroName.setText(data0.heroName);
            view.heroDesc.setText(data0.heroDesc);

            view.xManDesc.setText(data0.xManDesc);
            view.xManDesc.setTextColor(!data0.isXMan
                    ? mContext.getResources().getColor(R.color.cor_grey)
                    : mContext.getResources().getColor(R.color.cor_green));
        }

        @TargetApi(Build.VERSION_CODES.LOLLIPOP)
        @Override
        public View getView(final int i, View convertView, ViewGroup parent) {

            JListViewItemCtrl itemView;
            JListViewItemLayout sideView = (JListViewItemLayout) convertView;

            if (sideView == null) {
                sideView = new JListViewItemLayout(mContext);

                sideView.setContentView(mInflater.inflate(R.layout.layout_listview_item, null));

                itemView = new JListViewItemCtrl(sideView);
                sideView.setOnSlideListener(Fragment_Body_Heroes.this);
                sideView.setTag(itemView);

            } else {
                itemView = (JListViewItemCtrl) sideView.getTag();
            }

            final JListViewItem msgItem = mMessages.get(i);
            msgItem.view = sideView;
            msgItem.view.shrink();

            itemDataAssignToView(msgItem, itemView);

            RelativeLayout actionBtn = (RelativeLayout) sideView.findViewById(R.id.id_list_view_item_slip_layout);
            TextView btnText = (TextView)sideView.findViewById(R.id.id_list_view_item_slip_layout_text);

            if (msgItem.isXMan) {
                actionBtn.setBackground(mContext.getDrawable(R.drawable.bg_layout_slip_layout_red));
                btnText.setText(R.string.str_x_man_go_back);
            }
            else {
                actionBtn.setBackground(mContext.getDrawable(R.drawable.bg_layout_slip_layout_green));
                btnText.setText(R.string.str_to_be_strong);
            }

            itemView.actionHolder.setOnClickListener(new View.OnClickListener() {

                @TargetApi(Build.VERSION_CODES.M)
                @Override
                public void onClick(View v) {

                    JListViewItem actItem = mMessages.get(i);
                    if (actItem.isXMan) {
                        mJDdxStatics.mDb.setHeroDataNormal(actItem.internalHeroId);
                        JDebug.d(mContext.getString(R.string.str_info_has_restored) + actItem.heroName + "\n");
                    }
                    else {
                        mJDdxStatics.mDb.setHeroDataStrong(actItem.internalHeroId);
                        JDebug.d(mContext.getString(R.string.str_info_has_stronged) + actItem.heroName + "\n");
                    }

                    refreshWithAllHero();
                    mSlideAdapter.notifyDataSetChanged();
                }
            });

            return sideView;
        }
    }

    public View listViewInitView(View thisView) {

        JListViewCtrl slipView = (JListViewCtrl) thisView.findViewById(R.id.id_body_frag_hero_list_view_heroes);

        mSlideAdapter = new SlideAdapter();

        slipView.setAdapter(mSlideAdapter);
        slipView.setOnItemClickListener(this);
        slipView.setOnOverScrolledListener(this);

        return thisView;
    }

    @TargetApi(Build.VERSION_CODES.M)
    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

    }

    @Override
    public void onSlide(View view, int status) {

        if (mRetainedSlip != null && mRetainedSlip != view) {
            mRetainedSlip.shrink();
        }

        if (status == SLIDE_STATUS_ON) {
            mRetainedSlip = (JListViewItemLayout) view;
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.id_list_view_item_slip_layout:
                break;

            default:
                break;
        }
    }

    private List<JListViewItem> mMessages;
    private JListViewItemLayout mRetainedSlip;
    private SlideAdapter mSlideAdapter;
    private Context mContext;
    private Map<Integer, Bitmap> mHeroHeaders;

    private JDdxStatics mJDdxStatics;
    private boolean mDataSourceInitialized;

    public Fragment_Body_Heroes() {

        mMessages = new ArrayList<>();
        mHeroHeaders = new HashMap<>();
        mDataSourceInitialized = false;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                                           Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.layout_body_f_xman, container, false);

        mContext = view.getContext();
        listViewInitView(view);

        return view;
    }

    private void refreshWithAllHero() {

        if (!mDataSourceInitialized) return;

        mMessages.clear();

        readHeroDataToMessage(true);
        readHeroDataToMessage(false);
    }

    @Override
    public void onOverScrolled() {
        refreshWithAllHero();
    }

    Bitmap getBitmapFromBigOne(Bitmap theBigOne, int pointX, int pointY) {

        final int picRange = 196;
        int hidY = Math.abs(9 - pointY);
        hidY = 36 + (hidY * picRange) + hidY * 5;

        return Bitmap.createBitmap(theBigOne, (pointX * picRange) + pointX * 5, hidY, picRange, picRange);
    }

    public void initDataSource(JDdxStatics ddx) {

        mJDdxStatics = ddx;

        Bitmap bigBitmap = BitmapFactory.decodeResource(mContext.getResources(), R.drawable.ico_data_all_hero_head_pic);
        try {
            XmlResourceParser xml = mContext.getResources().getXml(R.xml.xml_all_hero_head_pos);
            int eventType;
            while (true) {
                eventType = xml.getEventType();
                if (eventType == XmlPullParser.END_DOCUMENT) break;
                else if (eventType == XmlPullParser.START_TAG) {
                    if (xml.getName().equals("hero")) {

                        String propX = xml.getAttributeValue(null, "x");
                        if (propX == null) continue;

                        String propY = xml.getAttributeValue(null, "y");
                        if (propY == null) continue;

                        mHeroHeaders.put(Integer.valueOf(xml.nextText()),
                                getBitmapFromBigOne(bigBitmap, Integer.valueOf(propY), Integer.valueOf(propX)));
                    }
                }
                xml.next();
            }
        }
        catch (XmlPullParserException | IOException ignored) {

        }

        mDataSourceInitialized = true;
    }

    private TextView getTextView() {
        return (TextView) getView().findViewById(R.id.id_body_frag_hero_prompt_text);
    }

    private void readHeroDataToMessage(boolean bModifiedData) {

        Cursor cursor = bModifiedData ? mJDdxStatics.mDb.getModifiedHeroData() : mJDdxStatics.mDb.getOriginalHeroData();
        if (cursor != null) {

            while (cursor.moveToNext()) {

                JListViewItem item = new JListViewItem();
                item.iconResId = bModifiedData ? R.drawable.ico_footer_btn_res_log_on : R.drawable.ico_footer_btn_res_log_idle;
                item.internalHeroId = Integer.valueOf(cursor.getString(0));
                item.heroDesc = cursor.getString(2);
                item.heroName = cursor.getString(1);
                item.xManDesc = mContext.getString(
                        bModifiedData ? R.string.str_x_man_strong_desc : R.string.str_x_man_normal_desc);
                item.isXMan = bModifiedData;

                mMessages.add(item);
            }

            cursor.close();
            mJDdxStatics.mDb.closeDB();
        }
    }

    public void firstTimeLoadListData() {

        if ((getView() == null) || (mJDdxStatics == null)) {
            return;
        }

        getTextView().setText(R.string.str_x_man_get_ready);
        refreshWithAllHero();
    }
}

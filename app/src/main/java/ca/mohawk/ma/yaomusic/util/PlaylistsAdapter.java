package ca.mohawk.ma.yaomusic.util;

import android.content.Context;
import android.graphics.Color;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Map;

import ca.mohawk.ma.yaomusic.R;
import ca.mohawk.ma.yaomusic.ui.RoundAngleImageView;

public class PlaylistsAdapter extends BaseAdapter {
    private ArrayList<Map<String, String>> dataList;
    private Context context;
    private int resId;

    public PlaylistsAdapter(Context context, ArrayList<Map<String, String>> dataList, int resId) {
        this.context = context;
        this.resId = resId;
        if (dataList == null) {
            this.dataList = new ArrayList<>();
        } else {
            this.dataList = dataList;
        }
    }

    public void setDataList(ArrayList<Map<String, String>> dataList) {
        this.dataList = dataList;
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return dataList.size();
    }

    @Override
    public Map<String, String> getItem(int position) {
        return dataList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        Map<String, String> item = dataList.get(position);
        View view = View.inflate(context, resId, null);
        ;

        RoundAngleImageView cover = view.findViewById(R.id.cover);
        TextView name = view.findViewById(R.id.name);

        cover.setImageURL(item.get("cover"));
        name.setText(item.get("name"));

        return view;
    }
}

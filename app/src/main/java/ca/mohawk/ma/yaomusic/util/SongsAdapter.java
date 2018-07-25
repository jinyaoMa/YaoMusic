package ca.mohawk.ma.yaomusic.util;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Map;

import ca.mohawk.ma.yaomusic.R;

public class SongsAdapter extends BaseAdapter {

    private Context context;
    private ArrayList<Map<String, Object>> dataList;
    private int resId;

    public SongsAdapter(Context context, ArrayList<Map<String, Object>> dataList, int resId) {
        this.context = context;
        this.resId = resId;
        if (dataList == null) {
            this.dataList = new ArrayList<>();
        }else {
            this.dataList = dataList;
        }
    }

    public void addItems(ArrayList<Map<String, Object>> items) {
        for (Map<String, Object> item : items) {
            dataList.add(item);
        }
    }

    public void clear() {
        dataList.clear();
    }

    @Override
    public int getCount() {
        return dataList.size();
    }

    @Override
    public Object getItem(int position) {
        return dataList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @SuppressLint("ResourceAsColor")
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        Map<String, Object> item = dataList.get(position);
        View view = View.inflate(context, resId, null);;

        TextView title = view.findViewById(R.id.title);
        TextView artist = view.findViewById(R.id.artist);

        title.setText((String) item.get("title"));
        artist.setText((String) item.get("artist"));

        if (item.containsKey("checked")) {
            title.setTextColor(Color.WHITE);
            view.findViewById(R.id.itemLine).setBackgroundResource(R.color.colorSongHighlight);
        }

        return view;
    }

    public String getItemHref(int position) {
        return (String) dataList.get(position).get("href");
    }

    public String getItemTitle(int position) {
        return (String) dataList.get(position).get("title");
    }

    public String getItemSubtitle(int position) {
        return (String) dataList.get(position).get("artist");
    }
}

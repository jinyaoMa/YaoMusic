package ca.mohawk.ma.yaomusic.util;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Map;

import ca.mohawk.ma.yaomusic.R;
import ca.mohawk.ma.yaomusic.ui.RoundAngleImageView;

public class SearchListAdapter extends BaseAdapter {

    private Context context;
    private ArrayList<Map<String, Object>> dataList;
    private int resId;

    public SearchListAdapter(Context context, ArrayList<Map<String, Object>> dataList, int resId) {
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

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        Map<String, Object> item = dataList.get(position);
        View view = View.inflate(context, resId, null);;

        RoundAngleImageView cover = view.findViewById(R.id.cover);
        TextView title = view.findViewById(R.id.title);
        TextView artist = view.findViewById(R.id.artist);

        title.setText((String) item.get("title"));
        artist.setText((String) item.get("artist"));
        cover.setImageURL((String) item.get("cover"));

        return view;
    }

    public String getItemHref(int position) {
        return (String) dataList.get(position).get("href");
    }

    public String getItemCover(int position) {
        return (String) dataList.get(position).get("cover");
    }

    public String getItemTitle(int position) {
        return (String) dataList.get(position).get("title");
    }

    public String getItemSubtitle(int position) {
        return (String) dataList.get(position).get("artist");
    }
}

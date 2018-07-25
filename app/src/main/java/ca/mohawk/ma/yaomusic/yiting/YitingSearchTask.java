package ca.mohawk.ma.yaomusic.yiting;

import android.os.AsyncTask;

import com.trycatch.mysnackbar.Prompt;
import com.trycatch.mysnackbar.TSnackbar;

import org.json.JSONObject;
import org.jsoup.Connection;
import org.jsoup.Jsoup;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import ca.mohawk.ma.yaomusic.R;
import ca.mohawk.ma.yaomusic.util.SearchListAdapter;

public class YitingSearchTask extends AsyncTask {
    final int TAB_SONG = 0;
    final int TAB_ALBUM = 1;
    final int TAB_ARTIST = 2;

    final String[] LINKS = {
            "http://so.1ting.com/song/json",
            "http://so.1ting.com/album/json",
            "http://so.1ting.com/singer/json"
    };

    final String LIMIT = "20";

    Boolean updateFlag = false;

    YitingSearchActivity activity;

    public YitingSearchTask(YitingSearchActivity activity) {
        this.activity = activity;
    }

    @Override
    protected Object doInBackground(Object[] objects) {
        ArrayList<Map<String, Object>> result = new ArrayList<>();
        int tabId = Integer.parseInt(objects[0].toString());
        Connection conn = Jsoup.connect(LINKS[tabId]).maxBodySize(0).ignoreContentType(true).timeout(60000);
        conn.userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/67.0.3396.87 Safari/537.36");
        conn.data("size", LIMIT);
        conn.data("q", objects[1].toString());
        if (objects.length == 3) {
            conn.data("page", objects[2].toString());
            updateFlag = true;
        }
        try {
            String response = conn.get().body().text();
            JSONObject jsonObject = new JSONObject(response);
            int len = jsonObject.optJSONArray("results").length();
            switch (tabId) {
                case TAB_SONG:
                    for (int i = 0; i < len; i++) {
                        Map<String, Object> item = new HashMap<>();

                        JSONObject temp = jsonObject.optJSONArray("results").optJSONObject(i);
                        String title = temp.optString("song_name");
                        String artist = temp.optString("singer_name");
                        String cover = temp.optString("album_cover");
                        String href = "yiting:" + temp.optString("song_id");

                        item.put("title", title);
                        item.put("artist", artist);
                        item.put("cover", cover);
                        item.put("href", href);
                        result.add(item);
                    }

                    break;
                case TAB_ALBUM:
                    for (int i = 0; i < len; i++) {
                        Map<String, Object> item = new HashMap<>();

                        JSONObject temp = jsonObject.optJSONArray("results").optJSONObject(i);
                        String title = temp.optString("album_name");
                        String artist = temp.optString("singer_name");
                        String cover = temp.optString("album_cover");
                        String href = temp.optString("album_id");

                        item.put("title", title);
                        item.put("artist", artist);
                        item.put("cover", cover);
                        item.put("href", href);
                        result.add(item);
                    }

                    break;
                case TAB_ARTIST:
                    for (int i = 0; i < len; i++) {
                        Map<String, Object> item = new HashMap<>();

                        JSONObject temp = jsonObject.optJSONArray("results").optJSONObject(i);
                        String title = temp.optString("singer_name");
                        String artist = temp.optString("cate_name");
                        String cover = "http://img.1ting.com/images/singer/s210_" + temp.optString("singer_id") + ".jpg";
                        String href = temp.optString("singer_id");

                        item.put("title", title);
                        item.put("artist", artist);
                        item.put("cover", cover);
                        item.put("href", href);
                        result.add(item);
                    }

                    break;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        return result;
    }

    @Override
    protected void onPostExecute(Object o) {
        YitingSearchActivity.PlaceholderFragment placeholderFragment = (YitingSearchActivity.PlaceholderFragment) activity.mSectionsPagerAdapter
            .instantiateItem(activity.mViewPager, activity.tabLayout.getSelectedTabPosition());

        if (o != null && !((ArrayList<Map<String, Object>>) o).isEmpty()) {
            if (placeholderFragment.nextPage == 1) {
                SearchListAdapter adapter = new SearchListAdapter(activity, (ArrayList<Map<String, Object>>) o, R.layout.custom_search_list_item);
                placeholderFragment.setAdapter(adapter);
            } else if (updateFlag) {
                SearchListAdapter searchListAdapter = placeholderFragment.getAdapter();
                placeholderFragment.removeFooter();
                searchListAdapter.addItems((ArrayList<Map<String, Object>>) o);
                searchListAdapter.notifyDataSetChanged();
            }
            placeholderFragment.loadMoreFlag = true;
        } else {
            placeholderFragment.endFooter();
            if (o == null) {
                TSnackbar.make(activity.tabLayout.getRootView(), activity.getResources().getString(R.string.task_null_result), TSnackbar.LENGTH_LONG, TSnackbar.APPEAR_FROM_TOP_TO_DOWN)
                        .setPromptThemBackground(Prompt.ERROR).show();
            }
        }

        placeholderFragment.hideProgress();
    }
}

package ca.mohawk.ma.yaomusic.wangyi;

import android.os.AsyncTask;

import com.trycatch.mysnackbar.Prompt;
import com.trycatch.mysnackbar.TSnackbar;

import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Connection;
import org.jsoup.Jsoup;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import ca.mohawk.ma.yaomusic.R;
import ca.mohawk.ma.yaomusic.util.SearchListAdapter;

public class WangyiSearchTask extends AsyncTask {
    final int TAB_SONG = 0;
    final int TAB_ALBUM = 1;
    final int TAB_ARTIST = 2;
    final int TAB_SONGLIST = 3;

    final String[] LINKS = {
            "http://music.163.com/api/search/pc",
            "http://music.163.com/api/search/pc",
            "http://music.163.com/api/search/pc",
            "http://music.163.com/api/search/pc"
    };

    final String LIMIT = "20";

    Boolean updateFlag = false;

    WangyiSearchActivity activity;

    public WangyiSearchTask(WangyiSearchActivity activity) {
        this.activity = activity;
    }

    @Override
    protected Object doInBackground(Object[] objects) {
        ArrayList<Map<String, Object>> result = new ArrayList<>();
        int tabId = Integer.parseInt(objects[0].toString());
        Connection conn = Jsoup.connect(LINKS[tabId]).maxBodySize(0).ignoreContentType(true).timeout(60000);
        conn.userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/67.0.3396.87 Safari/537.36");
        conn.header("Referer", "http://music.163.com/");

        conn.data("s", objects[1].toString());
        conn.data("limit", LIMIT);
        if (objects.length == 3) {
            conn.data("offset", String.format("%d", (Integer.parseInt(objects[2].toString()) - 1) * Integer.parseInt(LIMIT)));
            updateFlag = true;
        } else {
            conn.data("offset", "0");
        }

        try {
            String response;
            JSONObject jsonObject;
            JSONArray list;
            int len;
            switch (tabId) {
                case TAB_SONG:
                    conn.data("type", "1");
                    response = conn.post().body().text();
                    jsonObject = new JSONObject(response);
                    list = jsonObject.optJSONObject("result").optJSONArray("songs");
                    len = list.length();

                    for (int i = 0; i < len; i++) {
                        Map<String, Object> item = new HashMap<>();

                        JSONObject temp = list.optJSONObject(i);
                        String title = temp.optString("name");
                        JSONArray tmp = temp.optJSONArray("artists");
                        String[] artists = new String[tmp.length()];
                        for (int j = 0; j < tmp.length(); j++) {
                            artists[j] = tmp.optJSONObject(j).optString("name");
                        }
                        String artist = String.join(" / ", artists);
                        String cover = temp.optJSONObject("album").optString("picUrl");;
                        String href = "wangyi:" + temp.optString("id");

                        item.put("title", title);
                        item.put("artist", artist);
                        item.put("cover", cover);
                        item.put("href", href);
                        result.add(item);
                    }

                    break;
                case TAB_ALBUM:
                    conn.data("type", "10");
                    response = conn.post().body().text();
                    jsonObject = new JSONObject(response);
                    list = jsonObject.optJSONObject("result").optJSONArray("albums");
                    len = list.length();

                    for (int i = 0; i < len; i++) {
                        Map<String, Object> item = new HashMap<>();

                        JSONObject temp = list.optJSONObject(i);
                        String title = temp.optString("name");
                        JSONArray tmp = temp.optJSONArray("artists");
                        String[] artists = new String[tmp.length()];
                        for (int j = 0; j < tmp.length(); j++) {
                            artists[j] = tmp.optJSONObject(j).optString("name");
                        }
                        String artist = String.join(" / ", artists);
                        String cover = temp.optString("picUrl");
                        String href = temp.optString("id");

                        item.put("title", title);
                        item.put("artist", artist);
                        item.put("cover", cover);
                        item.put("href", href);
                        result.add(item);
                    }

                    break;
                case TAB_ARTIST:
                    conn.data("type", "100");
                    response = conn.post().body().text();
                    jsonObject = new JSONObject(response);
                    list = jsonObject.optJSONObject("result").optJSONArray("artists");
                    len = list.length();

                    for (int i = 0; i < len; i++) {
                        Map<String, Object> item = new HashMap<>();

                        JSONObject temp = list.optJSONObject(i);
                        String title = temp.optString("name");
                        String artist = "";
                        JSONArray tmp = temp.optJSONArray("alia");
                        if (tmp != null) {
                            String[] artists = new String[tmp.length()];
                            for (int j = 0; j < tmp.length(); j++) {
                                artists[j] = tmp.optString(j);
                            }
                            artist = String.join(" | ", artists);
                        }
                        String cover = temp.optString("picUrl");
                        String href = temp.optString("id");

                        item.put("title", title);
                        item.put("artist", artist);
                        item.put("cover", cover);
                        item.put("href", href);
                        result.add(item);
                    }

                    break;
                case TAB_SONGLIST:
                    conn.data("type", "1000");
                    response = conn.post().body().text();
                    jsonObject = new JSONObject(response);
                    list = jsonObject.optJSONObject("result").optJSONArray("playlists");
                    len = list.length();

                    for (int i = 0; i < len; i++) {
                        Map<String, Object> item = new HashMap<>();

                        JSONObject temp = list.optJSONObject(i);
                        String title = temp.optString("name");
                        String artist = temp.optJSONObject("creator").optString("nickname");
                        String cover = temp.optString("coverImgUrl");
                        String href = temp.optString("id");

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
        WangyiSearchActivity.PlaceholderFragment placeholderFragment = (WangyiSearchActivity.PlaceholderFragment) activity.mSectionsPagerAdapter
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

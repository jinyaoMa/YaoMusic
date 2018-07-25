package ca.mohawk.ma.yaomusic.qq;

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

public class QqSearchTask extends AsyncTask {
    final int TAB_SONG = 0;
    final int TAB_ALBUM = 1;
    final int TAB_ARTIST = 2;
    final int TAB_SONGLIST = 3;

    final String[] LINKS = {
            "https://c.y.qq.com/soso/fcgi-bin/client_search_cp",
            "https://c.y.qq.com/soso/fcgi-bin/client_search_cp",
            "https://c.y.qq.com/soso/fcgi-bin/client_search_cp",
            "https://c.y.qq.com/soso/fcgi-bin/client_music_search_songlist"
    };

    final String LIMIT = "20";

    Boolean updateFlag = false;

    QqSearchActivity activity;

    public QqSearchTask(QqSearchActivity activity) {
        this.activity = activity;
    }

    @Override
    protected Object doInBackground(Object[] objects) {
        ArrayList<Map<String, Object>> result = new ArrayList<>();
        int tabId = Integer.parseInt(objects[0].toString());
        Connection conn = Jsoup.connect(LINKS[tabId]).maxBodySize(0).ignoreContentType(true).timeout(60000);
        conn.userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/67.0.3396.87 Safari/537.36");
        conn.header("Referer", "https://y.qq.com/");

        conn.data("format", "json");
        conn.data("inCharset", "utf8");
        conn.data("outCharset", "utf-8");
        conn.data("platform", "yqq");
        switch (tabId) {
            case TAB_SONG:
            case TAB_ALBUM:
            case TAB_ARTIST:
                conn.data("w", objects[1].toString());
                conn.data("cr", "1");
                conn.data("n", LIMIT);
                if (objects.length == 3) {
                    conn.data("p", objects[2].toString());
                    updateFlag = true;
                } else {
                    conn.data("p", "1");
                }
                break;
            case TAB_SONGLIST:
                conn.data("query", objects[1].toString());
                conn.data("num_per_page", LIMIT);
                if (objects.length == 3) {
                    conn.data("page_no", String.format("%d", Integer.parseInt(objects[2].toString()) - 1));
                    updateFlag = true;
                } else {
                    conn.data("page_no", "0");
                }
        }

        try {
            String response;
            JSONObject jsonObject;
            JSONArray list;
            int len;
            switch (tabId) {
                case TAB_SONG:
                    conn.data("aggr", "1");
                    conn.data("t", "0");
                    response = conn.get().body().text();
                    jsonObject = new JSONObject(response);
                    list = jsonObject.optJSONObject("data").optJSONObject("song").optJSONArray("list");
                    len = list.length();

                    for (int i = 0; i < len; i++) {
                        Map<String, Object> item = new HashMap<>();

                        JSONObject temp = list.optJSONObject(i);
                        String mid = temp.optString("albummid");
                        String title = temp.optString("songname");
                        JSONArray tmp = temp.optJSONArray("singer");
                        String[] artists = new String[tmp.length()];
                        for (int j = 0; j < tmp.length(); j++) {
                            artists[j] = tmp.optJSONObject(j).optString("name");
                        }
                        String artist = String.join(" / ", artists);
                        String cover = "";
                        if (mid.length() > 2) {
                            cover = "http://i.gtimg.cn/music/photo/mid_album_180/" + mid.charAt(mid.length() - 2) + "/" + mid.charAt(mid.length() - 1) + "/" + mid + ".jpg";
                        }
                        String href = "qq:" + temp.optString("songmid");

                        item.put("title", title);
                        item.put("artist", artist);
                        item.put("cover", cover);
                        item.put("href", href);
                        result.add(item);
                    }

                    break;
                case TAB_ALBUM:
                    conn.data("sem", "10");
                    conn.data("t", "8");
                    response = conn.get().body().text();
                    jsonObject = new JSONObject(response);
                    list = jsonObject.optJSONObject("data").optJSONObject("album").optJSONArray("list");
                    len = list.length();

                    for (int i = 0; i < len; i++) {
                        Map<String, Object> item = new HashMap<>();

                        JSONObject temp = list.optJSONObject(i);
                        String title = temp.optString("albumName");
                        JSONArray tmp = temp.optJSONArray("singer_list");
                        String[] artists = new String[tmp.length()];
                        for (int j = 0; j < tmp.length(); j++) {
                            artists[j] = tmp.optJSONObject(j).optString("name");
                        }
                        String artist = String.join(" / ", artists);
                        String cover = temp.optString("albumPic");
                        String href = temp.optString("albumID");

                        item.put("title", title);
                        item.put("artist", artist);
                        item.put("cover", cover);
                        item.put("href", href);
                        result.add(item);
                    }

                    break;
                case TAB_ARTIST:
                    conn.data("t", "9");
                    response = conn.get().body().text();
                    jsonObject = new JSONObject(response);
                    list = jsonObject.optJSONObject("data").optJSONObject("singer").optJSONArray("list");
                    len = list.length();

                    for (int i = 0; i < len; i++) {
                        Map<String, Object> item = new HashMap<>();

                        JSONObject temp = list.optJSONObject(i);
                        String title = temp.optString("singerName");
                        String artist = temp.optString("songNum") + " " + activity.getString(R.string.list_item_song_count);
                        String cover = temp.optString("singerPic");
                        String href = temp.optString("singerMID");

                        item.put("title", title);
                        item.put("artist", artist);
                        item.put("cover", cover);
                        item.put("href", href);
                        result.add(item);
                    }

                    break;
                case TAB_SONGLIST:
                    response = conn.get().body().text();
                    jsonObject = new JSONObject(response);
                    list = jsonObject.optJSONObject("data").optJSONArray("list");
                    len = list.length();

                    for (int i = 0; i < len; i++) {
                        Map<String, Object> item = new HashMap<>();

                        JSONObject temp = list.optJSONObject(i);
                        String title = temp.optString("dissname");
                        String artist = temp.optJSONObject("creator").optString("name");
                        String cover = temp.optString("imgurl");
                        String href = temp.optString("dissid");

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
        QqSearchActivity.PlaceholderFragment placeholderFragment = (QqSearchActivity.PlaceholderFragment) activity.mSectionsPagerAdapter
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

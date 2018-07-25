package ca.mohawk.ma.yaomusic.qianqian;

import android.os.AsyncTask;

import com.trycatch.mysnackbar.Prompt;
import com.trycatch.mysnackbar.TSnackbar;

import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import ca.mohawk.ma.yaomusic.R;
import ca.mohawk.ma.yaomusic.util.SearchListAdapter;

public class QianqianSearchTask extends AsyncTask {
    final int TAB_SONG = 0;
    final int TAB_ALBUM = 1;
    final int TAB_ARTIST = 2;
    final int TAB_SONGLIST = 3;

    final String[] LINKS = {
            "http://tingapi.ting.baidu.com/v1/restserver/ting",
            "http://tingapi.ting.baidu.com/v1/restserver/ting",
            "http://tingapi.ting.baidu.com/v1/restserver/ting",
            "http://music.taihe.com/search/songlist"
    };

    final String LIMIT = "20";

    Boolean updateFlag = false;

    QianqianSearchActivity activity;

    public QianqianSearchTask(QianqianSearchActivity activity) {
        this.activity = activity;
    }

    @Override
    protected Object doInBackground(Object[] objects) {
        ArrayList<Map<String, Object>> result = new ArrayList<>();
        int tabId = Integer.parseInt(objects[0].toString());
        Connection conn = Jsoup.connect(LINKS[tabId]).maxBodySize(0).ignoreContentType(true).timeout(60000);
        conn.userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/67.0.3396.87 Safari/537.36");
        switch (tabId) {
            case TAB_SONG:
            case TAB_ALBUM:
            case TAB_ARTIST:
                conn.data("from", "qianqian");
                conn.data("method", "baidu.ting.search.merge");
                conn.data("query", objects[1].toString());
                conn.data("page_size", LIMIT);
                if (objects.length == 3) {
                    conn.data("page_no", objects[2].toString());
                    updateFlag = true;
                } else {
                    conn.data("page_no", "1");
                }
        }
        try {
            String response;
            JSONObject jsonObject;
            JSONArray list;
            int len;
            switch (tabId) {
                case TAB_SONG:
                    conn.data("type", "0");
                    response = conn.get().body().text();
                    jsonObject = new JSONObject(response);
                    list = jsonObject.optJSONObject("result").optJSONObject("song_info").optJSONArray("song_list");
                    len = list.length();

                    for (int i = 0; i < len; i++) {
                        Map<String, Object> item = new HashMap<>();

                        JSONObject temp = list.optJSONObject(i);
                        String title = temp.optString("title");
                        String artist = temp.optString("author");
                        String cover = temp.optString("pic_small");
                        String href = "qianqian:" + temp.optString("song_id");

                        item.put("title", title);
                        item.put("artist", artist);
                        item.put("cover", cover);
                        item.put("href", href);
                        result.add(item);
                    }

                    break;
                case TAB_ALBUM:
                    conn.data("type", "2");
                    response = conn.get().body().text();
                    jsonObject = new JSONObject(response);
                    list = jsonObject.optJSONObject("result").optJSONObject("album_info").optJSONArray("album_list");
                    len = list.length();

                    for (int i = 0; i < len; i++) {
                        Map<String, Object> item = new HashMap<>();

                        JSONObject temp = list.optJSONObject(i);
                        String title = temp.optString("title").replaceAll("</?em>", "");
                        String artist = temp.optString("author").replaceAll("</?em>", "");
                        String cover = temp.optString("pic_small");
                        String href = temp.optString("album_id");

                        item.put("title", title);
                        item.put("artist", artist);
                        item.put("cover", cover);
                        item.put("href", href);
                        result.add(item);
                    }

                    break;
                case TAB_ARTIST:
                    conn.data("type", "1");
                    response = conn.get().body().text();
                    jsonObject = new JSONObject(response);
                    list = jsonObject.optJSONObject("result").optJSONObject("artist_info").optJSONArray("artist_list");
                    len = list.length();

                    for (int i = 0; i < len; i++) {
                        Map<String, Object> item = new HashMap<>();

                        JSONObject temp = list.optJSONObject(i);
                        String title = temp.optString("author").replaceAll("</?em>", "");
                        String artist = temp.optString("country");
                        String cover = temp.optString("avatar_middle");
                        String href = temp.optString("ting_uid");

                        item.put("title", title);
                        item.put("artist", artist);
                        item.put("cover", cover);
                        item.put("href", href);
                        result.add(item);
                    }

                    break;
                case TAB_SONGLIST:
                    conn.data("size", LIMIT);
                    conn.data("key", objects[1].toString());
                    if (objects.length == 3) {
                        int temp = Integer.parseInt(objects[2].toString());
                        int tmp = Integer.parseInt(LIMIT);
                        conn.data("start", String.format("%d", (temp - 1) * tmp));
                        updateFlag = true;
                    }

                    Document doc = conn.get();
                    for (Element e : doc.select("#result_container .songlist-list li.clearfix")) {
                        Map<String, Object> item = new HashMap<>();

                        String title = e.select(".text-title").text();
                        String artist = e.select(".text-user a").attr("title");
                        String cover = e.select(".img-wrap img").attr("src");
                        String href = e.select(".text-title a").attr("href").replace("/songlist/", "");

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
        QianqianSearchActivity.PlaceholderFragment placeholderFragment = (QianqianSearchActivity.PlaceholderFragment) activity.mSectionsPagerAdapter
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

package ca.mohawk.ma.yaomusic.kuwo;

import android.os.AsyncTask;

import com.trycatch.mysnackbar.Prompt;
import com.trycatch.mysnackbar.TSnackbar;

import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Connection;
import org.jsoup.Jsoup;

import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import ca.mohawk.ma.yaomusic.R;
import ca.mohawk.ma.yaomusic.util.SearchListAdapter;

public class KuwoSearchTask extends AsyncTask {
    final int TAB_SONG = 0;
    final int TAB_ALBUM = 1;
    final int TAB_ARTIST = 2;

    final String[] LINKS = {
            "http://search.kuwo.cn/r.s",
            "http://search.kuwo.cn/r.s",
            "http://search.kuwo.cn/r.s"
    };

    final String[] FTS = {
            "music", "album", "artist"
    };

    final String LIMIT = "20";

    Boolean updateFlag = false;

    KuwoSearchActivity activity;

    public KuwoSearchTask(KuwoSearchActivity activity) {
        this.activity = activity;
    }

    @Override
    protected Object doInBackground(Object[] objects) {
        ArrayList<Map<String, Object>> result = new ArrayList<>();
        int tabId = Integer.parseInt(objects[0].toString());
        Connection conn = Jsoup.connect(LINKS[tabId]).maxBodySize(0).ignoreContentType(true).timeout(60000);
        conn.userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/67.0.3396.87 Safari/537.36");

        conn.data("rformat", "json");
        conn.data("encoding", "utf8");
        conn.data("all", objects[1].toString());
        conn.data("rn", LIMIT);
        conn.data("ft", FTS[tabId]);
        if (objects.length == 3) {
            conn.data("pn", String.format("%d", Integer.parseInt(objects[2].toString()) - 1));
            updateFlag = true;
        } else {
            conn.data("pn", "0");
        }

        try {
            String response;
            JSONObject jsonObject;
            JSONArray list;
            int len;
            switch (tabId) {
                case TAB_SONG:
                    response = conn.get().body().text();
                    jsonObject = new JSONObject(response);
                    list = jsonObject.optJSONArray("abslist");
                    len = list.length();

                    for (int i = 0; i < len; i++) {
                        Map<String, Object> item = new HashMap<>();

                        JSONObject temp = list.optJSONObject(i);
                        String mid = temp.optString("MUSICRID").toLowerCase().replace("music_", "");
                        String title = temp.optString("SONGNAME");
                        String artist = temp.optString("AARTIST");
                        String cover = "kuwo:music:" + mid;
                        String href = "kuwo:" + mid;

                        item.put("title", title);
                        item.put("artist", artist);
                        item.put("cover", cover);
                        item.put("href", href);
                        result.add(item);
                    }

                    break;
                case TAB_ALBUM:
                    response = conn.get().body().text();
                    jsonObject = new JSONObject(response);
                    list = jsonObject.optJSONArray("albumlist");
                    len = list.length();

                    for (int i = 0; i < len; i++) {
                        Map<String, Object> item = new HashMap<>();

                        JSONObject temp = list.optJSONObject(i);
                        String title = temp.optString("name");
                        String artist = temp.optString("artist");
                        String cover = "http://img1.kwcdn.kuwo.cn/star/albumcover/" + temp.optString("pic");
                        String href = temp.optString("albumid");

                        item.put("title", title);
                        item.put("artist", artist);
                        item.put("cover", cover);
                        item.put("href", href);
                        result.add(item);
                    }

                    break;
                case TAB_ARTIST:
                    response = conn.get().body().text();
                    jsonObject = new JSONObject(response);
                    list = jsonObject.optJSONArray("abslist");
                    len = list.length();

                    for (int i = 0; i < len; i++) {
                        Map<String, Object> item = new HashMap<>();

                        JSONObject temp = list.optJSONObject(i);
                        String title = temp.optString("ARTIST");
                        String artist = temp.optString("COUNTRY");
                        String cover = "http://img4.kwcdn.kuwo.cn/star/starheads/" + temp.optString("PICPATH");
                        String href = temp.optString("ARTISTID");

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
        KuwoSearchActivity.PlaceholderFragment placeholderFragment = (KuwoSearchActivity.PlaceholderFragment) activity.mSectionsPagerAdapter
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

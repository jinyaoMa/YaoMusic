package ca.mohawk.ma.yaomusic;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CoordinatorLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.HeaderViewListAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ca.mohawk.ma.yaomusic.ui.RoundAngleImageView;
import ca.mohawk.ma.yaomusic.util.HTMLDecoder;
import ca.mohawk.ma.yaomusic.util.SearchListAdapter;
import ca.mohawk.ma.yaomusic.util.SongsAdapter;
import ca.mohawk.ma.yaomusic.yiting.YitingSearchActivity;
import ca.mohawk.ma.yaomusic.yiting.YitingSearchTask;

public class ArtistActivity extends AppCompatActivity {

    private ArtistTask artistTask;
    private Boolean loadMoreFlag = true;
    private int nextPage = 0;
    private ListView lvSongs;
    private View mFooter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_artist);

        String title = getIntent().getStringExtra("title");
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitle(title);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        String cover = getIntent().getStringExtra("cover");
        RoundAngleImageView coverImg = findViewById(R.id.cover);
        coverImg.setImageURL(cover);

        int ref = getIntent().getIntExtra("reference", R.id.nav_yiting);
        String id = getIntent().getStringExtra("id");
        artistTask = new ArtistTask();
        artistTask.execute(ref, id);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == android.R.id.home) {
            finish();
        }

        return super.onOptionsItemSelected(item);
    }

    private class ArtistTask extends AsyncTask {
        private int ref;
        private String id;
        private int limit;
        private boolean updateFlag = false;

        @Override
        protected Object doInBackground(Object[] objects) {
            ArrayList<Map<String, Object>> result = new ArrayList<>();
            ref = Integer.parseInt(objects[0].toString());
            id = objects[1].toString();
            Connection conn;

            switch (ref) {
                case R.id.nav_yiting:
                    conn = Jsoup.connect("http://www.1ting.com/singer/" + id + "/album/").maxBodySize(0).timeout(60000);

                    try {
                        Document doc = conn.get();
                        Elements es = doc.select(".albumList li");

                        for (Element e : es) {
                            Map<String, Object> item = new HashMap<>();

                            String title = e.select(".albumName").text();
                            String artist = e.select(".albumDate").text();
                            String cover = e.select(".albumPic").attr("src");
                            String href = e.select(".albumLink").attr("href");
                            Pattern pattern = Pattern.compile(".+_(\\d+).\\w+");
                            Matcher matcher = pattern.matcher(href);
                            if (matcher.find()) {
                                href = matcher.group(1);
                            }

                            item.put("title", title);
                            item.put("artist", artist);
                            item.put("cover", cover);
                            item.put("href", href);
                            result.add(item);
                        }

                        loadMoreFlag = false;
                        limit = 0;

                    } catch (Exception e) {
                        e.printStackTrace();
                        return null;
                    }

                    break;
                case R.id.nav_qianqian:
                    conn = Jsoup.connect("http://tingapi.ting.baidu.com/v1/restserver/ting").maxBodySize(0).ignoreContentType(true).timeout(60000);
                    conn.data("from", "qianqian");
                    conn.data("method", "baidu.ting.artist.getAlbumList");
                    conn.data("format", "json");
                    conn.data("tinguid", id);
                    if (objects.length > 2) {
                        conn.data("offset", String.format("%d", Integer.parseInt(objects[2].toString()) * 20));
                        updateFlag = true;
                    } else {
                        conn.data("offset", "0");
                    }
                    conn.data("limit", "20");

                    try {
                        String doc = conn.execute().body();
                        JSONObject jsonObject = new JSONObject(doc);
                        JSONArray jsonArray = jsonObject.optJSONArray("albumlist");
                        if (jsonArray == null) {
                            return result;
                        }
                        int len = jsonArray.length();

                        for (int i = 0; i < len; i++) {
                            Map<String, Object> item = new HashMap<>();

                            JSONObject temp = jsonArray.optJSONObject(i);
                            String title = temp.optString("title");
                            String artist = temp.optString("songs_total") + " " + getString(R.string.list_item_song_count);
                            String cover = temp.optString("pic_s180");
                            String href = temp.optString("album_id");

                            item.put("title", title);
                            item.put("artist", artist);
                            item.put("cover", cover);
                            item.put("href", href);
                            result.add(item);
                        }

                        if (jsonObject.optInt("havemore") > 0) {
                            loadMoreFlag = true;
                        } else {
                            loadMoreFlag = false;
                        }
                        limit = 20;

                    } catch (Exception e) {
                        e.printStackTrace();
                        return null;
                    }

                    break;
                case R.id.nav_kuwo:
                    conn = Jsoup.connect("http://www.kuwo.cn/newh5/artist/artistAlbumByPage").maxBodySize(0).ignoreContentType(true).timeout(60000);
                    conn.data("id", id);
                    if (objects.length > 2) {
                        conn.data("pn", objects[2].toString());
                        updateFlag = true;
                    } else {
                        conn.data("pn", "0");
                    }
                    conn.data("rn", "20");

                    try {
                        String doc = conn.execute().body();
                        JSONObject jsonObject = new JSONObject(doc);
                        JSONArray jsonArray = jsonObject.optJSONObject("data").optJSONArray("albumList");
                        int len = jsonArray.length();

                        for (int i = 0; i < len; i++) {
                            Map<String, Object> item = new HashMap<>();

                            JSONObject temp = jsonArray.optJSONObject(i);
                            String title = HTMLDecoder.decode(temp.optString("name"));
                            String artist = temp.optString("pub");
                            String cover = temp.optString("pic");
                            String href = temp.optString("albumDbId");

                            item.put("title", title);
                            item.put("artist", artist);
                            item.put("cover", cover);
                            item.put("href", href);
                            result.add(item);
                        }

                        limit = 20;
                        int num = 1;
                        if (objects.length > 2) {
                            num += Integer.parseInt(objects[2].toString());
                        }
                        int total = jsonObject.optJSONObject("data").optInt("total");
                        if (limit * num < total) {
                            loadMoreFlag = true;
                        } else {
                            loadMoreFlag = false;
                        }

                    } catch (Exception e) {
                        e.printStackTrace();
                        return null;
                    }

                    break;
                case R.id.nav_qq:
                    conn = Jsoup.connect("https://c.y.qq.com/v8/fcg-bin/fcg_v8_singer_album.fcg").maxBodySize(0).ignoreContentType(true).timeout(60000);
                    conn.data("singermid", id);
                    conn.data("order", "time");
                    conn.data("format", "json");
                    conn.data("platform", "yqq");
                    if (objects.length > 2) {
                        conn.data("begin", String.format("%d", Integer.parseInt(objects[2].toString()) * 20));
                        updateFlag = true;
                    } else {
                        conn.data("begin", "0");
                    }
                    conn.data("num", "20");

                    try {
                        String doc = conn.execute().body();
                        JSONObject jsonObject = new JSONObject(doc);
                        JSONArray jsonArray = jsonObject.optJSONObject("data").optJSONArray("list");
                        int len = jsonArray.length();

                        for (int i = 0; i < len; i++) {
                            Map<String, Object> item = new HashMap<>();

                            JSONObject temp = jsonArray.optJSONObject(i);
                            String title = temp.optString("albumName");
                            JSONArray tmp = temp.optJSONArray("singers");
                            String[] artists = new String[tmp.length()];
                            for (int j = 0; j < tmp.length(); j++) {
                                artists[j] = tmp.optJSONObject(j).optString("singer_name");
                            }
                            String artist = String.join(" / ", artists);
                            String mid = temp.optString("albumMID");
                            String cover = "http://i.gtimg.cn/music/photo/mid_album_180/" + mid.charAt(mid.length() - 2) + "/" + mid.charAt(mid.length() - 1) + "/" + mid + ".jpg";
                            String href = temp.optString("albumID");

                            item.put("title", title);
                            item.put("artist", artist);
                            item.put("cover", cover);
                            item.put("href", href);
                            result.add(item);
                        }

                        limit = 20;
                        int num = 1;
                        if (objects.length > 2) {
                            num += Integer.parseInt(objects[2].toString());
                        }
                        int total = jsonObject.optJSONObject("data").optInt("total");
                        if (limit * num < total) {
                            loadMoreFlag = true;
                        } else {
                            loadMoreFlag = false;
                        }

                    } catch (Exception e) {
                        e.printStackTrace();
                        return null;
                    }

                    break;
                case R.id.nav_wangyi:
                    conn = Jsoup.connect("http://music.163.com/api/artist/albums/" + id).maxBodySize(0).ignoreContentType(true).timeout(60000);
                    conn.header("Cookie", "os=pc;appver=3");
                    conn.header("Referer", "http://music.163.com/");
                    conn.data("id", id);
                    conn.data("total", "true");
                    if (objects.length > 2) {
                        conn.data("offset", String.format("%d", Integer.parseInt(objects[2].toString()) * 20));
                        updateFlag = true;
                    } else {
                        conn.data("offset", "0");
                    }
                    conn.data("limit", "20");

                    try {
                        String doc = conn.execute().body();
                        JSONObject jsonObject = new JSONObject(doc);
                        JSONArray jsonArray = jsonObject.optJSONArray("hotAlbums");
                        int len = jsonArray.length();

                        for (int i = 0; i < len; i++) {
                            Map<String, Object> item = new HashMap<>();

                            JSONObject temp = jsonArray.optJSONObject(i);
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

                        limit = 20;
                        loadMoreFlag = jsonObject.optBoolean("more");

                    } catch (Exception e) {
                        e.printStackTrace();
                        return null;
                    }

                    break;
            }

            return result;
        }

        @Override
        protected void onPostExecute(Object o) {
            lvSongs = findViewById(R.id.lvSongs);
            ProgressBar progressBar = findViewById(R.id.progressBar);

            if (o != null && !((ArrayList<Map<String, Object>>) o).isEmpty()) {
                if (updateFlag) {
                    SearchListAdapter searchListAdapter = null;
                    if (lvSongs.getAdapter() instanceof HeaderViewListAdapter) {
                        searchListAdapter = (SearchListAdapter) ((HeaderViewListAdapter) lvSongs.getAdapter()).getWrappedAdapter();
                    } else if (lvSongs.getAdapter() instanceof SearchListAdapter) {
                        searchListAdapter = (SearchListAdapter) lvSongs.getAdapter();
                    }
                    lvSongs.removeFooterView(mFooter);
                    searchListAdapter.addItems((ArrayList<Map<String, Object>>) o);
                    searchListAdapter.notifyDataSetChanged();
                } else {
                    SearchListAdapter adapter = new SearchListAdapter(ArtistActivity.this, (ArrayList<Map<String, Object>>) o, R.layout.custom_search_list_item);
                    lvSongs.setAdapter(adapter);
                }

                if (!loadMoreFlag) {
                    mFooter = LayoutInflater.from(ArtistActivity.this).inflate(R.layout.custom_list_footer, null);
                    mFooter.findViewById(R.id.progressBar2).setVisibility(View.GONE);
                    TextView more = mFooter.findViewById(R.id.more);
                    more.setText(R.string.list_footer_no_more);
                    lvSongs.addFooterView(mFooter, null, false);
                }
            } else if (o == null) {
                SearchListAdapter adapter = new SearchListAdapter(ArtistActivity.this, new ArrayList<Map<String, Object>>(), R.layout.custom_search_list_item);
                lvSongs.setAdapter(adapter);
                mFooter = LayoutInflater.from(ArtistActivity.this).inflate(R.layout.custom_list_footer, null);
                mFooter.findViewById(R.id.progressBar2).setVisibility(View.GONE);
                TextView more = mFooter.findViewById(R.id.more);
                more.setText(R.string.list_item_songs_error);
                lvSongs.addFooterView(mFooter, null, false);
                loadMoreFlag = false;
            } else if (((ArrayList<Map<String, Object>>) o).isEmpty()) {
                SearchListAdapter adapter = new SearchListAdapter(ArtistActivity.this, new ArrayList<Map<String, Object>>(), R.layout.custom_search_list_item);
                lvSongs.setAdapter(adapter);
                mFooter = LayoutInflater.from(ArtistActivity.this).inflate(R.layout.custom_list_footer, null);
                mFooter.findViewById(R.id.progressBar2).setVisibility(View.GONE);
                TextView more = mFooter.findViewById(R.id.more);
                more.setText(R.string.list_item_songs_empty);
                lvSongs.addFooterView(mFooter, null, false);
                loadMoreFlag = false;
            }

            progressBar.setVisibility(View.GONE);

            lvSongs.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    HeaderViewListAdapter temp;
                    SearchListAdapter tmp = null;
                    if (parent.getAdapter() instanceof HeaderViewListAdapter) {
                        temp = (HeaderViewListAdapter) parent.getAdapter();
                        tmp = (SearchListAdapter) temp.getWrappedAdapter();
                    } else if (parent.getAdapter() instanceof SearchListAdapter) {
                        tmp = (SearchListAdapter) parent.getAdapter();
                    }

                    Intent intent = new Intent(ArtistActivity.this, AlbumActivity.class);
                    intent.putExtra("reference", ref);
                    intent.putExtra("id", tmp.getItemHref(position));
                    intent.putExtra("cover", tmp.getItemCover(position));
                    intent.putExtra("title", tmp.getItemTitle(position));
                    startActivityForResult(intent, 11);
                }
            });
            lvSongs.setOnScrollListener(new AbsListView.OnScrollListener() {
                @Override
                public void onScrollStateChanged(AbsListView view, int scrollState) {
                    return;
                }

                @Override
                public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                    if ((firstVisibleItem + visibleItemCount) == totalItemCount && loadMoreFlag && totalItemCount >= limit) {
                        mFooter = LayoutInflater.from(ArtistActivity.this).inflate(R.layout.custom_list_footer, null);
                        lvSongs.addFooterView(mFooter, null, false);
                        loadMoreFlag = false;

                        ArtistTask artistTask = new ArtistTask();
                        artistTask.execute(ref, id, ++nextPage);

                    }
                }
            });
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case 11:
                switch (resultCode) {
                    case 1:
                        if (data != null && data.getBooleanExtra("songSelect", false)) {
                            finish();
                        }
                        break;

                }
        }
    }
}

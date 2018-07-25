package ca.mohawk.ma.yaomusic;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ca.mohawk.ma.yaomusic.ui.RoundAngleImageView;
import ca.mohawk.ma.yaomusic.util.SearchListAdapter;
import ca.mohawk.ma.yaomusic.util.SongsAdapter;

public class SonglistActivity extends AppCompatActivity {

    private SonglistTask songlistTask;
    private Boolean loadMoreFlag = true;
    private int nextPage = 0;
    private ListView lvSongs;
    private View mFooter;

    private String cover;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_songlist);

        String title = getIntent().getStringExtra("title");
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitle(title);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        cover = getIntent().getStringExtra("cover");
        RoundAngleImageView coverImg = findViewById(R.id.cover);
        coverImg.setImageURL(cover);

        int ref = getIntent().getIntExtra("reference", R.id.nav_yiting);
        String id = getIntent().getStringExtra("id");
        songlistTask = new SonglistTask();
        songlistTask.execute(ref, id);
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

    private class SonglistTask extends AsyncTask {
        private int ref;
        private String id;
        private int limit;
        private boolean updateFlag = false;

        private SongsAdapter songsAdapter;

        @Override
        protected Object doInBackground(Object[] objects) {
            ArrayList<Map<String, Object>> result = new ArrayList<>();
            ref = Integer.parseInt(objects[0].toString());
            id = objects[1].toString();
            Connection conn;

            switch (ref) {
                case R.id.nav_yiting:
                    break;
                case R.id.nav_qianqian:
                    String offset;
                    if (objects.length > 2) {
                        offset = String.format("%d", Integer.parseInt(objects[2].toString()) * 20);
                        updateFlag = true;
                    } else {
                        offset = "0";
                    }
                    conn = Jsoup.connect("http://music.taihe.com/songlist/" + id + "/offset/" + offset).maxBodySize(0).timeout(60000);

                    try {
                        Document doc = conn.get();
                        Elements es = doc.select(".songlist-list ul li");

                        for (Element e : es) {
                            Map<String, Object> item = new HashMap<>();

                            String title = e.select(".song-title").text();
                            String artist = e.select(".singer").text();
                            String href = "qianqian:" + e.select(".song-title a").attr("href").replace("/song/", "");

                            item.put("title", title);
                            item.put("artist", artist);
                            item.put("href", href);
                            result.add(item);
                        }

                        if (doc.select(".page-navigator-next").size() > 0) {
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
                    break;
                case R.id.nav_qq:
                    conn = Jsoup.connect("https://c.y.qq.com/qzone/fcg-bin/fcg_ucc_getcdinfo_byids_cp.fcg").maxBodySize(0).ignoreContentType(true).timeout(60000);
                    conn.header("Referer", "https://y.qq.com/");
                    conn.data("type", "1");
                    conn.data("json", "1");
                    conn.data("utf8", "1");
                    conn.data("onlysong", "1");
                    conn.data("format", "json");
                    conn.data("ctx", "1");
                    conn.data("disstid", id);

                    try {
                        String response = conn.execute().body();
                        JSONObject jsonObject = new JSONObject(response);
                        JSONArray jsonArray = jsonObject.optJSONArray("songlist");
                        int len = jsonArray.length();
                        for (int i = 0; i < len; i++) {
                            Map<String, Object> item = new HashMap<>();

                            JSONObject temp = jsonArray.optJSONObject(i);
                            String title = temp.optString("songname");
                            JSONArray tmp = temp.optJSONArray("singer");
                            String[] artists = new String[tmp.length()];
                            for (int j = 0; j < tmp.length(); j++) {
                                artists[j] = tmp.optJSONObject(j).optString("name");
                            }
                            String artist = String.join(" / ", artists);
                            String href = "qq:" + temp.optString("songmid");

                            item.put("title", title);
                            item.put("artist", artist);
                            item.put("href", href);
                            result.add(item);
                        }

                    } catch (Exception e) {
                        e.printStackTrace();
                        return null;
                    }

                    break;
                case R.id.nav_wangyi:
                    conn = Jsoup.connect("http://music.163.com/api/playlist/detail").maxBodySize(0).ignoreContentType(true).timeout(60000);
                    conn.header("Cookie", "os=pc;appver=3");
                    conn.header("Referer", "http://music.163.com/");
                    conn.data("updateTime", "-1");
                    conn.data("id", id);

                    try {
                        String response = conn.execute().body();
                        JSONObject jsonObject = new JSONObject(response);
                        JSONArray jsonArray = jsonObject.optJSONObject("result").optJSONArray("tracks");
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
                            String href = "wangyi:" + temp.optString("id");

                            item.put("title", title);
                            item.put("artist", artist);
                            item.put("href", href);
                            result.add(item);
                        }

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
                    if (lvSongs.getAdapter() instanceof HeaderViewListAdapter) {
                        songsAdapter = (SongsAdapter) ((HeaderViewListAdapter) lvSongs.getAdapter()).getWrappedAdapter();
                    } else if (lvSongs.getAdapter() instanceof SongsAdapter) {
                        songsAdapter = (SongsAdapter) lvSongs.getAdapter();
                    }
                    lvSongs.removeFooterView(mFooter);
                    songsAdapter.addItems((ArrayList<Map<String, Object>>) o);
                    songsAdapter.notifyDataSetChanged();
                } else {
                    songsAdapter = new SongsAdapter(SonglistActivity.this, (ArrayList<Map<String, Object>>) o, R.layout.custom_songs_list_item);
                    lvSongs.setAdapter(songsAdapter);
                }

                if (!loadMoreFlag) {
                    lvSongs.removeFooterView(mFooter);
                    mFooter = LayoutInflater.from(SonglistActivity.this).inflate(R.layout.custom_list_footer, null);
                    mFooter.findViewById(R.id.progressBar2).setVisibility(View.GONE);
                    TextView more = mFooter.findViewById(R.id.more);
                    more.setText(R.string.list_footer_no_more);
                    lvSongs.addFooterView(mFooter, null, false);
                }

                FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
                fab.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        PlayerService.list.clear();
                        PlayerService.currentIndex = 0;

                        for (int i = 0; i < songsAdapter.getCount(); i++) {
                            Map<String, String> song = new HashMap<>();
                            song.put("id", songsAdapter.getItemHref(i));
                            song.put("title", songsAdapter.getItemTitle(i));
                            song.put("artist", songsAdapter.getItemSubtitle(i));
                            song.put("cover", cover);
                            PlayerService.list.add(song);
                        }

                        Intent intent = new Intent(SonglistActivity.this, PlayerService.class);
                        intent.putExtra("index", 0);
                        intent.putExtra("action", PlayerService.PLAY);
                        intent.putExtra("insertSongs", true);
                        startService(intent);
                        finish();
                    }
                });

            } else if (o == null) {
                SongsAdapter adapter = new SongsAdapter(SonglistActivity.this, new ArrayList<Map<String, Object>>(), R.layout.custom_songs_list_item);
                lvSongs.setAdapter(adapter);
                mFooter = LayoutInflater.from(SonglistActivity.this).inflate(R.layout.custom_list_footer, null);
                mFooter.findViewById(R.id.progressBar2).setVisibility(View.GONE);
                TextView more = mFooter.findViewById(R.id.more);
                more.setText(R.string.list_item_songs_error);
                lvSongs.addFooterView(mFooter, null, false);
                loadMoreFlag = false;
            } else if (((ArrayList<Map<String, Object>>) o).isEmpty()) {
                SongsAdapter adapter = new SongsAdapter(SonglistActivity.this, new ArrayList<Map<String, Object>>(), R.layout.custom_songs_list_item);
                lvSongs.setAdapter(adapter);
                mFooter = LayoutInflater.from(SonglistActivity.this).inflate(R.layout.custom_list_footer, null);
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
                    SongsAdapter tmp = null;
                    if (parent.getAdapter() instanceof HeaderViewListAdapter) {
                        temp = (HeaderViewListAdapter) parent.getAdapter();
                        tmp = (SongsAdapter) temp.getWrappedAdapter();
                    } else if (parent.getAdapter() instanceof SongsAdapter) {
                        tmp = (SongsAdapter) parent.getAdapter();
                    }

                    Intent serviceIntent = new Intent(SonglistActivity.this, PlayerService.class);

                    Map<String, String> song = new HashMap<>();
                    song.put("id", tmp.getItemHref(position));
                    song.put("title", tmp.getItemTitle(position));
                    song.put("artist", tmp.getItemSubtitle(position));
                    song.put("cover", cover);

                    MiniPlayerFragment.changePlayInfo(song.get("cover"), song.get("title"), song.get("artist"));

                    int index = PlayerService.list.indexOf(song);
                    if (index < 0) {
                        PlayerService.list.add(song);
                        serviceIntent.putExtra("index", PlayerService.list.indexOf(song));
                    } else {
                        serviceIntent.putExtra("index", index);
                    }
                    serviceIntent.putExtra("action", PlayerService.PLAY);
                    startService(serviceIntent);
                    finish();
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
                        mFooter = LayoutInflater.from(SonglistActivity.this).inflate(R.layout.custom_list_footer, null);
                        lvSongs.addFooterView(mFooter, null, false);
                        loadMoreFlag = false;

                        SonglistActivity.SonglistTask artistTask = new SonglistActivity.SonglistTask();
                        artistTask.execute(ref, id, ++nextPage);

                    }
                }
            });

        }
    }
}

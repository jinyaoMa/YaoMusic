package ca.mohawk.ma.yaomusic;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.HeaderViewListAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.trycatch.mysnackbar.Prompt;
import com.trycatch.mysnackbar.TSnackbar;

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

public class AlbumActivity extends AppCompatActivity {

    private AlbumTask albumTask;
    private String cover;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_album);

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
        albumTask = new AlbumTask();
        albumTask.execute(ref, id);
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

    private class AlbumTask extends AsyncTask {

        @Override
        protected Object doInBackground(Object[] objects) {
            ArrayList<Map<String, Object>> result = new ArrayList<>();
            int ref = Integer.parseInt(objects[0].toString());
            String id = objects[1].toString();
            Connection conn;

            switch (ref) {
                case R.id.nav_yiting:
                    conn = Jsoup.connect("http://h5.1ting.com/touch/api/album_song/" + id + "/mobile").maxBodySize(0).ignoreContentType(true).timeout(60000);

                    try {
                        String response = conn.get().body().text();
                        JSONArray jsonArray = new JSONArray(response);
                        int len = jsonArray.length();
                        for (int i = 0; i < len; i++) {
                            Map<String, Object> item = new HashMap<>();

                            JSONObject temp = jsonArray.optJSONObject(i);
                            String title = temp.optString("song_name");
                            String artist = temp.optString("singer_name");
                            String href = "yiting:" + temp.optString("song_id");

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
                case R.id.nav_qianqian:
                    conn = Jsoup.connect("http://tingapi.ting.baidu.com/v1/restserver/ting").maxBodySize(0).ignoreContentType(true).timeout(60000);
                    conn.data("from", "qianqian");
                    conn.data("method", "baidu.ting.album.getAlbumInfo");
                    conn.data("format", "json");
                    conn.data("album_id", id);

                    try {
                        String response = conn.get().body().text();
                        JSONObject jsonObject = new JSONObject(response);
                        JSONArray jsonArray = jsonObject.optJSONArray("songlist");
                        int len = jsonArray.length();
                        for (int i = 0; i < len; i++) {
                            Map<String, Object> item = new HashMap<>();

                            JSONObject temp = jsonArray.optJSONObject(i);
                            String title = temp.optString("title");
                            String artist = temp.optString("author");
                            String href = "qianqian:" + temp.optString("song_id");

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
                case R.id.nav_kuwo:
                    conn = Jsoup.connect("http://www.kuwo.cn/newh5/album/content").maxBodySize(0).timeout(60000);
                    conn.data("albumid", id);

                    try {
                        Document doc = conn.get();
                        Elements es = doc.select("#messDivId li");

                        for (Element e : es) {
                            Map<String, Object> item = new HashMap<>();

                            String title = e.select(".singTexUp p").text();
                            String artist = e.select(".singName").text();
                            Pattern pattern = Pattern.compile("(.+)_.*");
                            Matcher matcher = pattern.matcher(artist);
                            if (matcher.find()) {
                                artist = matcher.group(1);
                            }
                            String href = "kuwo:" + e.attr("mid");

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
                case R.id.nav_qq:
                    conn = Jsoup.connect("http://c.y.qq.com/v8/fcg-bin/fcg_v8_album_info_cp.fcg").maxBodySize(0).ignoreContentType(true).timeout(60000);
                    conn.data("charset", "utf-8");
                    conn.data("albumid", id);

                    try {
                        String response = conn.execute().body();
                        JSONObject jsonObject = new JSONObject(response);
                        JSONArray jsonArray = jsonObject.optJSONObject("data").optJSONArray("list");
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
                    conn = Jsoup.connect("http://music.163.com/api/album/" + id).maxBodySize(0).ignoreContentType(true).timeout(60000);
                    conn.header("Cookie", "os=pc;appver=3");
                    conn.header("Referer", "http://music.163.com/");
                    conn.data("total", "true");
                    conn.data("ext", "true");
                    conn.data("id", id);

                    try {
                        String response = conn.execute().body();
                        JSONObject jsonObject = new JSONObject(response);
                        JSONArray jsonArray = jsonObject.optJSONObject("album").optJSONArray("songs");
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
            ListView lvSongs = findViewById(R.id.lvSongs);
            ProgressBar progressBar = findViewById(R.id.progressBar);

            if (o != null && !((ArrayList<Map<String, Object>>) o).isEmpty()) {
                final SongsAdapter adapter = new SongsAdapter(AlbumActivity.this, (ArrayList<Map<String, Object>>) o, R.layout.custom_songs_list_item);
                lvSongs.setAdapter(adapter);

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

                        Intent serviceIntent = new Intent(AlbumActivity.this, PlayerService.class);

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

                        Intent intentResult = new Intent();
                        intentResult.putExtra("songSelect",true);
                        AlbumActivity.this.setResult(1, intentResult);
                        finish();
                    }
                });

                FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
                fab.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        PlayerService.list.clear();
                        PlayerService.currentIndex = 0;

                        for (int i = 0; i < adapter.getCount(); i++) {
                            Map<String, String> song = new HashMap<>();
                            song.put("id", adapter.getItemHref(i));
                            song.put("title", adapter.getItemTitle(i));
                            song.put("artist", adapter.getItemSubtitle(i));
                            song.put("cover", cover);
                            PlayerService.list.add(song);
                        }

                        Intent intent = new Intent(AlbumActivity.this, PlayerService.class);
                        intent.putExtra("index", 0);
                        intent.putExtra("action", PlayerService.PLAY);
                        intent.putExtra("insertSongs", true);
                        startService(intent);

                        Intent intentResult = new Intent();
                        intentResult.putExtra("songSelect",true);
                        AlbumActivity.this.setResult(1, intentResult);
                        finish();
                    }
                });

            } else if (o == null) {
                ArrayList<Map<String, Object>> temp = new ArrayList<>();
                Map<String, Object> tmp = new HashMap<>();
                tmp.put("title", getString(R.string.list_item_songs_error));
                temp.add(tmp);
                SongsAdapter adapter = new SongsAdapter(AlbumActivity.this, temp, R.layout.custom_songs_list_item);
                lvSongs.setAdapter(adapter);
            } else if (((ArrayList<Map<String, Object>>) o).isEmpty()) {
                ArrayList<Map<String, Object>> temp = (ArrayList<Map<String, Object>>) o;
                Map<String, Object> tmp = new HashMap<>();
                tmp.put("title", getString(R.string.list_item_songs_empty));
                temp.add(tmp);
                SongsAdapter adapter = new SongsAdapter(AlbumActivity.this, temp, R.layout.custom_songs_list_item);
                lvSongs.setAdapter(adapter);
            }

            progressBar.setVisibility(View.GONE);
        }
    }
}

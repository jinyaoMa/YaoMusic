package ca.mohawk.ma.yaomusic;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.support.design.widget.NavigationView;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import com.google.gson.Gson;
import com.trycatch.mysnackbar.Prompt;
import com.trycatch.mysnackbar.TSnackbar;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Connection;
import org.jsoup.Jsoup;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.URL;
import java.net.URLConnection;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Stack;

import ca.mohawk.ma.yaomusic.database.PlaylistDatabaseHelper;
import ca.mohawk.ma.yaomusic.util.CurrentPlaylistArrayListMap;

public class PlayerService extends Service implements MediaPlayer.OnBufferingUpdateListener, MediaPlayer.OnPreparedListener, MediaPlayer.OnCompletionListener, MediaPlayer.OnErrorListener {
    public static final int NONE = -1;
    public static final int PLAY = 0;
    public static final int PAUSE = 1;
    public static final int STOP = 2;
    public static final int SEEK = 3;
    public static final int INIT = 4;
    public static final int NEXT = 5;
    public static final int SELECT = 6;
    public static final int TIMER = 7;

    public final String REF_YITING = "yiting:";
    public final String REF_QIANQIAN = "qianqian:";
    public final String REF_KUWO = "kuwo:";
    public final String REF_QQ = "qq:";
    public final String REF_WANGYI = "wangyi:";

    public static int currentIndex = -1;
    public static CurrentPlaylistArrayListMap list;
    public static SharedPreferences currentPlaylist;
    public static String jsonList;

    private PlaylistDatabaseHelper playlistDatabaseHelper;

    private Double threadCheck;
    private final int INITIAL = 0;
    private final int PROCESS = 1;
    @SuppressLint("HandlerLeak")
    private Handler updateProgress = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == INITIAL) {
                threadCheck = Math.random();
                MiniPlayerFragment.updateSeekBarProgress(mediaPlayer.getCurrentPosition(), mediaPlayer.getDuration());
                Message message = Message.obtain();
                message.what = PROCESS;
                message.obj = threadCheck;
                sendMessageDelayed(message, 1000);
            } else if (msg.what == PROCESS) {
                Double key = (Double) msg.obj;
                if (threadCheck == key && mediaPlayer != null && mediaPlayer.isPlaying()) {
                    MiniPlayerFragment.updateSeekBarProgress(mediaPlayer.getCurrentPosition(), mediaPlayer.getDuration());
                    Message message = Message.obtain();
                    message.what = PROCESS;
                    message.obj = key;
                    sendMessageDelayed(message, 1000);
                }
            }
        }
    };

    private Boolean sourceCheck;

    private Boolean canNext = false;

    private int timeoutCount = 1800;
    @SuppressLint("HandlerLeak")
    private Handler timeout = new Handler() {
        @SuppressLint("DefaultLocale")
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            timeoutCount -= 1;
            if (timeoutCount > 0) {
                MainActivity.navigationView.getMenu().findItem(R.id.nav_test).setTitle(String.format("%02d:%02d", timeoutCount / 60, timeoutCount % 60));
                MainActivity.navigationView.getMenu().findItem(R.id.nav_test).setEnabled(false);
                sendEmptyMessageDelayed(0, 1000);
            } else {
                MainActivity.navigationView.getMenu().findItem(R.id.nav_test).setTitle(R.string.nav_bar_timer);
                timeoutCount = 1800;
                Intent intent = new Intent(PlayerService.this, PlayerService.class);
                intent.putExtra("action", PlayerService.STOP);
                startService(intent);
                MainActivity.navigationView.getMenu().findItem(R.id.nav_test).setEnabled(true);
            }
        }
    };

    public PlayerService() {
        playlistDatabaseHelper = new PlaylistDatabaseHelper(this);
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    public static MediaPlayer mediaPlayer;

    private void initMediaPlayer() {
        mediaPlayer = new MediaPlayer();
        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        mediaPlayer.setOnBufferingUpdateListener(this);
        mediaPlayer.setOnPreparedListener(this);
        mediaPlayer.setOnCompletionListener(this);
        mediaPlayer.setOnErrorListener(this);
    }

    private void releaseMediaPlayer() {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.reset();
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }

    @Override
    public void onCreate() {
        initMediaPlayer();

        sourceCheck = false;

        if (list.size() > 0) {
            Map<String, String> currentItem = list.get(currentIndex);
            MiniPlayerFragment.changePlayInfo(currentItem.get("cover"), currentItem.get("title"), currentItem.get("artist"));
        }
    }

    @Override
    public void onDestroy() {
        jsonList = new Gson().toJson(list);
        currentPlaylist.edit().putString(getString(R.string.list_in_play), jsonList).apply();
        currentPlaylist.edit().putInt("currentIndex", currentIndex).apply();

        releaseMediaPlayer();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        int action = intent.getIntExtra("action", NONE);
        currentIndex = intent.getIntExtra("index", currentIndex);

        if (intent.getBooleanExtra("insertSongs", false)) {
            for (int i = 0; i < list.size(); i++) {
                playlistDatabaseHelper.insertSong(list.get(i));
            }
        }

        switch (action) {
            case PLAY:
                if (mediaPlayer != null) {
                    tooglePlay(true);
                }
                break;
            case STOP:
                releaseMediaPlayer();
                initMediaPlayer();
                break;
            case PAUSE:
                if (mediaPlayer != null) {
                    if (mediaPlayer.isPlaying()) {
                        mediaPlayer.pause();
                        MiniPlayerFragment.togglePlay(false);
                    } else {
                        if (sourceCheck){
                            mediaPlayer.start();
                        } else {
                            tooglePlay(true);
                        }
                        updateProgress.sendEmptyMessage(INITIAL);
                    }
                }
                break;
            case SEEK:
                int progress = intent.getIntExtra("progress", 0);
                mediaPlayer.seekTo(progress);
                break;
            case INIT:
                if (list.size() > 0) {
                    Map<String, String> song = list.get(currentIndex);
                    MiniPlayerFragment.changePlayInfo(song.get("cover"), song.get("title"), song.get("artist"));
                }
                break;
            case NEXT:
                toogleNext();
                break;
            case SELECT:
                int index = intent.getIntExtra("position", currentIndex);
                toogleNext(index);
                break;
            case TIMER:
                timeout.sendEmptyMessageDelayed(0, 1000);
                break;
        }

        return super.onStartCommand(intent, flags, startId);
    }

    private void toogleNext() {
        if (currentIndex + 1 == list.size()) {
            currentIndex = 0;
        } else {
            currentIndex += 1;
        }
        MiniPlayerFragment.changePlayInfo(list.get(currentIndex).get("cover"), list.get(currentIndex).get("title"), list.get(currentIndex).get("artist"));
        tooglePlay(true);
    }

    private void toogleNext(int index) {
        currentIndex = index;
        MiniPlayerFragment.changePlayInfo(list.get(currentIndex).get("cover"), list.get(currentIndex).get("title"), list.get(currentIndex).get("artist"));
        tooglePlay(true);
    }

    private void tooglePlay(Boolean flag) {
        if (list.size() > 0) {
            Map<String, String> song = list.get(currentIndex);
            String href = song.get("id");
            final String ref = checkReference(href);
            final String id = href.replaceFirst(ref, "");

            String path = playlistDatabaseHelper.findSongPath(ref, id);
            if (path != null) {
                playSong(ref, path, flag);
            } else {
                SongTask songTask = new SongTask();
                songTask.execute(ref, id);
            }
        }
    }

    @Override
    public void onBufferingUpdate(MediaPlayer mp, int percent) {
        MiniPlayerFragment.updateLoadBar(percent);
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        mp.start();
        MiniPlayerFragment.togglePlay(true);
        MiniPlayerFragment.toggleProcessing(false);
        updateProgress.sendEmptyMessage(INITIAL);
        canNext = true;
    }

    public String checkReference(String href) {
        if (href.startsWith(REF_YITING)) {
            return REF_YITING;
        } else if (href.startsWith(REF_QIANQIAN)) {
            return REF_QIANQIAN;
        } else if (href.startsWith(REF_KUWO)) {
            return REF_KUWO;
        } else if (href.startsWith(REF_QQ)) {
            return REF_QQ;
        } else if (href.startsWith(REF_WANGYI)) {
            return REF_WANGYI;
        }
        return null;
    }

    public void playSong(String ref, String url, Boolean flag) {
        if (url.startsWith("http")) {
            playSongOnline(ref, url, flag);
        } else {
            playSongOffline(url, flag);
        }
    }

    private void playSongOffline(String url, Boolean flag) {
        if (url == null || url.equals("null")) {
            TSnackbar.make(MiniPlayerFragment.view.getRootView(), getString(R.string.song_play_error_2), TSnackbar.APPEAR_FROM_TOP_TO_DOWN, TSnackbar.LENGTH_LONG).setPromptThemBackground(Prompt.WARNING).show();
            return;
        }

        try {
            MiniPlayerFragment.togglePlay(true);
            mediaPlayer.setDataSource(url);
            if (flag) {
                mediaPlayer.prepareAsync();
            }

            sourceCheck = true;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void playSongOnline(final String ref, final String url, final Boolean flag) {
        MiniPlayerFragment.toggleProcessing(true);
        new Thread() {
            @Override
            public void run() {
                String path = null;
                Connection conn;
                Map<String, String> headers = new HashMap<>();
                switch (ref) {
                    case REF_YITING:
                        conn = Jsoup.connect(url).ignoreContentType(true);
                        conn.header("Referer", "http://www.1ting.com/player/");
                        try {
                            path = conn.get().location();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        break;
                    case REF_QIANQIAN:
                        path = url;
                        break;
                    case REF_KUWO:
                        path = url;
                        break;
                    case REF_QQ:
                        headers.put("Cookie", "qqmusic_fromtag=30");
                        path = url;
                        if (url.split("vkey=")[1].startsWith("&guid=")) {
                            TSnackbar.make(MiniPlayerFragment.view.getRootView(), getString(R.string.song_play_error_2), TSnackbar.APPEAR_FROM_TOP_TO_DOWN, TSnackbar.LENGTH_LONG).setPromptThemBackground(Prompt.WARNING).show();
                            return;
                        }
                        break;
                    case REF_WANGYI:
                        path = url;
                        headers.put("Cookie", "os=pc;appver=3");
                }

                try {
                    Uri uri = Uri.parse(path);
                    MiniPlayerFragment.togglePlay(true);
                    mediaPlayer.reset();
                    mediaPlayer.setDataSource(getApplicationContext(), uri, headers);
                    if (flag) {
                        mediaPlayer.prepareAsync();
                    }

                    sourceCheck = true;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }.start();
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        if (canNext) {
            toogleNext();
        }
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        if (list.size() > 0) {
            if (what == -38) {
                TSnackbar.make(MiniPlayerFragment.view.getRootView(), R.string.song_play_error_1, TSnackbar.APPEAR_FROM_TOP_TO_DOWN).setPromptThemBackground(Prompt.WARNING).show();
                MiniPlayerFragment.toggleProcessing(true);
            } else if (what == 703 || what == 701) {
                TSnackbar.make(MiniPlayerFragment.view.getRootView(), R.string.song_play_error_1, TSnackbar.APPEAR_FROM_TOP_TO_DOWN).setPromptThemBackground(Prompt.WARNING).show();
                MiniPlayerFragment.toggleProcessing(true);
                new Thread(){
                    @Override
                    public void run() {
                        try {
                            sleep(15000);
                            toogleNext();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }.start();
            }
        } else {
            mp.reset();
            MiniPlayerFragment.togglePlay(false);
            canNext = false;
        }
        return false;
    }

    @SuppressLint("StaticFieldLeak")
    private class SongTask extends AsyncTask<String, Map<String, String>, Map<String, String>> {

        @Override
        protected Map<String, String> doInBackground(String... strings) {
            Map<String, String> result = null;
            String ref = strings[0];
            String id = strings[1];

            Connection conn;
            Connection connTemp;
            Proxy proxy;
            switch (ref) {
                case REF_YITING:
                    conn = Jsoup.connect("http://h5.1ting.com/touch/api/song").maxBodySize(0).ignoreContentType(true).timeout(60000);
                    conn.data("ids", id);

                    try {
                        String response = conn.execute().body();
                        JSONObject jsonObject = new JSONArray(response).optJSONObject(0);
                        result = new HashMap<>();
                        result.put("reference", ref);
                        result.put("songId", id);
                        result.put("songPath", "http://www.1ting.com/api/audio?" + jsonObject.optString("song_filepath"));
                        result.put("songTitle", jsonObject.optString("song_name"));
                        result.put("artistTitle", jsonObject.optString("singer_name"));
                        result.put("artistId", jsonObject.optString("singer_id"));
                        result.put("albumTitle", jsonObject.optString("album_name"));
                        result.put("albumId", jsonObject.optString("album_id"));

                    } catch (Exception e) {
                        e.printStackTrace();
                        return null;
                    }

                    break;
                case REF_QIANQIAN:
                    conn = Jsoup.connect("http://tingapi.ting.baidu.com/v1/restserver/ting").maxBodySize(0).ignoreContentType(true).timeout(60000);
                    conn.data("from", "qianqian");
                    conn.data("method", "baidu.ting.song.play");
                    conn.data("format", "json");
                    conn.data("songid", id);

                    try {
                        String response = conn.execute().body();
                        JSONObject jsonObject = new JSONObject(response);
                        result = new HashMap<>();
                        result.put("reference", ref);
                        result.put("songId", id);
                        result.put("songPath", jsonObject.optJSONObject("bitrate").optString("file_link"));
                        result.put("songTitle", jsonObject.optJSONObject("songinfo").optString("title"));
                        result.put("artistTitle", jsonObject.optJSONObject("songinfo").optString("author"));
                        result.put("artistId", jsonObject.optJSONObject("songinfo").optString("artist_id"));
                        result.put("albumTitle", jsonObject.optJSONObject("songinfo").optString("album_title"));
                        result.put("albumId", jsonObject.optJSONObject("songinfo").optString("album_id"));

                    } catch (Exception e) {
                        e.printStackTrace();
                        return null;
                    }

                    break;
                case REF_KUWO:
                    conn = Jsoup.connect("http://antiserver.kuwo.cn/anti.s").maxBodySize(0).ignoreContentType(true).timeout(60000);
                    conn.data("format", "mp3|aac");
                    conn.data("response", "url");
                    conn.data("type", "convert_url");
                    conn.data("rid", "MUSIC_" + id);

                    connTemp = Jsoup.connect("http://www.kuwo.cn/newh5/singles/songinfoandlrc").maxBodySize(0).ignoreContentType(true).timeout(60000);
                    connTemp.data("musicId", id);

                    try {
                        String response = conn.execute().body();
                        String responseTemp = connTemp.execute().body();
                        JSONObject jsonObject = new JSONObject(responseTemp);

                        result = new HashMap<>();
                        result.put("reference", ref);
                        result.put("songId", id);
                        result.put("songPath", response);
                        result.put("songTitle", jsonObject.optJSONObject("data").optJSONObject("songinfo").optString("songName"));
                        result.put("artistTitle", jsonObject.optJSONObject("data").optJSONObject("songinfo").optString("artist"));
                        result.put("artistId", jsonObject.optJSONObject("data").optJSONObject("songinfo").optString("artistId"));
                        result.put("albumTitle", jsonObject.optJSONObject("data").optJSONObject("songinfo").optString("album"));
                        result.put("albumId", jsonObject.optJSONObject("data").optJSONObject("songinfo").optString("albumId"));

                    } catch (Exception e) {
                        e.printStackTrace();
                        return null;
                    }

                    break;
                case REF_QQ:
                    conn = Jsoup.connect("https://c.y.qq.com/v8/fcg-bin/fcg_play_single_song.fcg").maxBodySize(0).ignoreContentType(true).timeout(60000);
                    conn.data("format", "json");
                    conn.data("inCharset", "utf8");
                    conn.data("outCharset", "utf-8");
                    conn.data("platform", "yqq");
                    conn.data("needNewCode", "0");
                    conn.data("songmid", id);

                    proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress("proxy.uku.im", 443));
                    connTemp = Jsoup.connect("http://c.y.qq.com/base/fcgi-bin/fcg_music_express_mobile3.fcg").maxBodySize(0).ignoreContentType(true).timeout(60000).proxy(proxy);
                    connTemp.data("format", "json");
                    connTemp.data("platform", "yqq");
                    connTemp.data("cid", "205361747");
                    connTemp.data("guid", "-1");
                    connTemp.data("filename", "C400" + id + ".m4a");
                    connTemp.data("songmid", id);

                    try {
                        String response = conn.execute().body();
                        String responseTemp = connTemp.execute().body();
                        JSONObject jsonObject = new JSONObject(response).optJSONArray("data").optJSONObject(0);
                        JSONObject url = new JSONObject(responseTemp).optJSONObject("data").optJSONArray("items").optJSONObject(0);

                        result = new HashMap<>();
                        result.put("reference", ref);
                        result.put("songId", id);
                        result.put("songPath", "http://dl.stream.qqmusic.qq.com/" + url.optString("filename") + "?vkey=" + url.optString("vkey") + "&guid=-1");
                        result.put("songTitle", jsonObject.optString("name"));
                        JSONArray temp = jsonObject.optJSONArray("singer");
                        String[] artists = new String[temp.length()];
                        for (int i = 0; i < temp.length(); i++ ) {
                            artists[i] = temp.optJSONObject(i).optString("name");
                        }
                        result.put("artistTitle", String.join(" / ", artists));
                        result.put("artistId", temp.optJSONObject(0).optString("mid"));
                        result.put("albumTitle", jsonObject.optJSONObject("album").optString("name"));
                        result.put("albumId", jsonObject.optJSONObject("album").optString("mid"));

                    } catch (Exception e) {
                        e.printStackTrace();
                        return null;
                    }

                    break;
                case REF_WANGYI:
                    conn = Jsoup.connect("http://music.163.com/api/song/detail/").maxBodySize(0).ignoreContentType(true).timeout(60000);
                    conn.header("Cookie", "os=pc;appver=3");
                    conn.data("ids", "[" + id + "]");
                    conn.data("id", id);

                    proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress("proxy.uku.im", 443));
                    connTemp = Jsoup.connect("http://music.163.com/api/song/enhance/player/url").maxBodySize(0).ignoreContentType(true).timeout(60000).proxy(proxy);
                    connTemp.header("Cookie", "os=pc;appver=3");
                    connTemp.data("ids", "[" + id + "]");
                    connTemp.data("br", "320000");

                    try {
                        String response = conn.execute().body();
                        String responseTemp = connTemp.execute().body();
                        JSONObject jsonObject = new JSONObject(response).optJSONArray("songs").optJSONObject(0);
                        JSONObject jsonObjectTemp = new JSONObject(responseTemp).optJSONArray("data").optJSONObject(0);

                        result = new HashMap<>();
                        result.put("reference", ref);
                        result.put("songId", id);
                        result.put("songPath", jsonObjectTemp.optString("url"));
                        result.put("songTitle", jsonObject.optString("name"));
                        JSONArray temp = jsonObject.optJSONArray("artists");
                        String[] artists = new String[temp.length()];
                        for (int i = 0; i < temp.length(); i++ ) {
                            artists[i] = temp.optJSONObject(i).optString("name");
                        }
                        result.put("artistTitle", String.join(" / ", artists));
                        result.put("artistId", temp.optJSONObject(0).optString("id"));
                        result.put("albumTitle", jsonObject.optJSONObject("album").optString("name"));
                        result.put("albumId", jsonObject.optJSONObject("album").optString("id"));

                    } catch (Exception e) {
                        e.printStackTrace();
                        return null;
                    }

            }

            return result;
        }

        @Override
        protected void onPostExecute(Map<String, String> result) {
            if (result != null) {
                long rowId = playlistDatabaseHelper.insertSong(result);
                @SuppressLint("DefaultLocale") String url = playlistDatabaseHelper.getSongUrl(String.format("%d", rowId));
                if (url == null) {
                    playlistDatabaseHelper.updateSong(result);
                    url = result.get("songPath");
                }
                String ref = result.get("reference");

                playSong(ref, url, true);
            } else {
                TSnackbar.make(MiniPlayerFragment.view.getRootView(), getString(R.string.song_play_error), TSnackbar.APPEAR_FROM_TOP_TO_DOWN, TSnackbar.LENGTH_LONG).setPromptThemBackground(Prompt.ERROR).show();
            }
        }
    }
}

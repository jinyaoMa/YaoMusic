package ca.mohawk.ma.yaomusic;

import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.trycatch.mysnackbar.Prompt;
import com.trycatch.mysnackbar.TSnackbar;

import org.json.JSONObject;
import org.json.JSONStringer;

import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeSet;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.X509TrustManager;

import ca.mohawk.ma.yaomusic.database.PlaylistDatabaseHelper;
import ca.mohawk.ma.yaomusic.kuwo.KuwoSearchActivity;
import ca.mohawk.ma.yaomusic.qianqian.QianqianSearchActivity;
import ca.mohawk.ma.yaomusic.qq.QqSearchActivity;
import ca.mohawk.ma.yaomusic.ui.RoundAngleImageView;
import ca.mohawk.ma.yaomusic.util.CurrentPlaylistArrayListMap;
import ca.mohawk.ma.yaomusic.wangyi.WangyiSearchActivity;
import ca.mohawk.ma.yaomusic.yiting.YitingSearchActivity;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    final String REFERENCE_SELECTED = "reference";
    final String LANGUAGE_SELECTED = "language";

    SharedPreferences sharedPreferences;
    public static NavigationView navigationView;

    private ComponentName mainPortal;
    private ComponentName aliasPortal;
    private PackageManager packageManager;
    private int superCounter = 0;
    private Boolean counterFlag = true;
    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            superCounter--;
            Log.v("SUPER_COUNTER", String.valueOf(superCounter));
            if (superCounter <= 0) {
                superTurnOff();
                setSuperUI();
                counterFlag = true;
                timer.cancel();
            }
        }
    };
    private Timer timer;

    public void setSuperUI() {
        ImageView icon = this.findViewById(R.id.superIcon);
        TextView name = this.findViewById(R.id.superName);
        TextView version = this.findViewById(R.id.superVersion);
        TextView appname = this.findViewById(R.id.app_name);

        if (counterFlag) {
            icon.setImageResource(R.mipmap.ic_launcher_super);
            name.setText(R.string.app_name_super);
            version.setText(R.string.nav_header_subtitle_super);
            appname.setText(R.string.app_name_super);
        } else {
            icon.setImageResource(R.mipmap.ic_launcher);
            name.setText(R.string.app_name);
            version.setText(R.string.nav_header_subtitle);
            appname.setText(R.string.app_name);
        }
    }

    public void superTurnOn() {
        packageManager.setComponentEnabledSetting(aliasPortal,
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP);

        packageManager.setComponentEnabledSetting(mainPortal,
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP);
    }

    public void superTurnOff() {
        packageManager.setComponentEnabledSetting(aliasPortal,
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP);

        packageManager.setComponentEnabledSetting(mainPortal,
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP);
    }

    public void superYaoOnClick(View view) {
        if (superCounter <= 10 && counterFlag) {
            superCounter++;
            switch (superCounter) {
                case 1:
                    TSnackbar.make(view.getRootView(), getString(R.string.super_yao_on_click_1), TSnackbar.LENGTH_LONG, TSnackbar.APPEAR_FROM_BOTTOM_TO_TOP).show();
                    break;
                case 3:
                    TSnackbar.make(view.getRootView(), getString(R.string.super_yao_on_click_2), TSnackbar.LENGTH_LONG, TSnackbar.APPEAR_FROM_BOTTOM_TO_TOP).setPromptThemBackground(Prompt.WARNING).show();
                    break;
                case 6:
                    TSnackbar.make(view.getRootView(), getString(R.string.super_yao_on_click_3), TSnackbar.LENGTH_LONG, TSnackbar.APPEAR_FROM_BOTTOM_TO_TOP).setPromptThemBackground(Prompt.WARNING).show();
                    break;
                case 9:
                    TSnackbar.make(view.getRootView(), getString(R.string.super_yao_on_click_4), TSnackbar.LENGTH_LONG, TSnackbar.APPEAR_FROM_BOTTOM_TO_TOP).setPromptThemBackground(Prompt.WARNING).show();
                    break;
                case 10:
                    TSnackbar.make(view.getRootView(), getString(R.string.super_yao_on_click_5), TSnackbar.LENGTH_LONG, TSnackbar.APPEAR_FROM_BOTTOM_TO_TOP).setPromptThemBackground(Prompt.SUCCESS).show();
                    superTurnOn();
                    setSuperUI();
                    counterFlag = false;
                    timer = new Timer();
                    timer.schedule(new TimerTask() {
                        @Override
                        public void run() {
                            handler.sendEmptyMessage(0);
                        }
                    }, 0, 6000);
            }
        } else {
            TSnackbar.make(navigationView.getRootView(), "什么？炒鸡模式？不存在的~", TSnackbar.APPEAR_FROM_BOTTOM_TO_TOP, TSnackbar.LENGTH_LONG).setPromptThemBackground(Prompt.SUCCESS).show();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        sharedPreferences = getSharedPreferences(getString(R.string.share_preferences), MODE_PRIVATE);
        setLanguage(sharedPreferences.getInt(LANGUAGE_SELECTED, R.id.nav_chinese));

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        trustEveryone();

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        navigationView.getMenu().findItem(sharedPreferences.getInt(REFERENCE_SELECTED, R.id.nav_yiting)).setChecked(true);
        navigationView.getMenu().findItem(sharedPreferences.getInt(LANGUAGE_SELECTED, R.id.nav_chinese)).setChecked(true);

        mainPortal = new ComponentName(this, "ca.mohawk.ma.yaomusic.NormalYao");
        aliasPortal = new ComponentName(this, "ca.mohawk.ma.yaomusic.SuperYao");
        packageManager = getPackageManager();

        PlayerService.currentPlaylist = getSharedPreferences(getString(R.string.list_in_play), MODE_PRIVATE);
        PlayerService.jsonList = PlayerService.currentPlaylist.getString(getString(R.string.list_in_play), "[]");
        PlayerService.list = new Gson().fromJson(PlayerService.jsonList, CurrentPlaylistArrayListMap.class);
        PlayerService.currentIndex = PlayerService.currentPlaylist.getInt("currentIndex", 0);
        Intent intent = new Intent(this, PlayerService.class);
        intent.putExtra("action", PlayerService.INIT);
        startService(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();

        MiniPlayerFragment.refresh();
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else if (counterFlag) {
            PlayerService.jsonList = new Gson().toJson(PlayerService.list);
            PlayerService.currentPlaylist.edit().putString(getString(R.string.list_in_play), PlayerService.jsonList).apply();
            PlayerService.currentPlaylist.edit().putInt("currentIndex", PlayerService.currentIndex).apply();

            super.onBackPressed();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();


        switch (id) {
            case R.id.nav_yiting:
            case R.id.nav_qianqian:
            case R.id.nav_kuwo:
            case R.id.nav_qq:
            case R.id.nav_wangyi:
                navigationView.getMenu().findItem(sharedPreferences.getInt(REFERENCE_SELECTED, R.id.nav_yiting)).setChecked(false);
                sharedPreferences.edit().putInt(REFERENCE_SELECTED, id).apply();
                break;
            case R.id.nav_chinese:
            case R.id.nav_english:
                navigationView.getMenu().findItem(sharedPreferences.getInt(LANGUAGE_SELECTED, R.id.nav_chinese)).setChecked(false);
                sharedPreferences.edit().putInt(LANGUAGE_SELECTED, id).apply();
                break;
        }

        if (id == R.id.nav_about) {
            Intent intent = new Intent(this, AboutActivity.class);
            startActivity(intent);

        } else if (id == R.id.nav_chinese || id == R.id.nav_english) {
            setLanguage(id);

        } else if (id == R.id.nav_test) {
            Intent intent = new Intent(MainActivity.this, PlayerService.class);
            intent.putExtra("action", PlayerService.TIMER);
            startService(intent);
        }

        switch (id) {
            case R.id.nav_test:
            case R.id.nav_about:
                return true;
            case R.id.nav_chinese:
            case R.id.nav_english:
                overridePendingTransition(R.anim.appear, R.anim.disappear);
                return true;
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    public void setLanguage(int resId) {
        Resources resources = getResources();
        DisplayMetrics dm = resources.getDisplayMetrics();
        Configuration config = resources.getConfiguration();

        if (resId == R.id.nav_chinese) {
            if (config.locale == Locale.getDefault()) {
                return;
            }
            config.locale = Locale.getDefault();
        } else if (resId == R.id.nav_english) {
            if (config.locale == Locale.ENGLISH) {
                return;
            }
            config.locale = Locale.ENGLISH;
        }
        resources.updateConfiguration(config, dm);

        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    public void searchButtonOnClick(View view) {
        int ref = sharedPreferences.getInt(REFERENCE_SELECTED, R.id.nav_yiting);
        Intent intent = null;
        if (ref == R.id.nav_yiting) {
            intent = new Intent(this, YitingSearchActivity.class);
        } else if (ref == R.id.nav_qianqian) {
            intent = new Intent(this, QianqianSearchActivity.class);
        } else if (ref == R.id.nav_kuwo) {
            intent = new Intent(this, KuwoSearchActivity.class);
        } else if (ref == R.id.nav_qq) {
            intent = new Intent(this, QqSearchActivity.class);
        } else if (ref == R.id.nav_wangyi) {
            intent = new Intent(this, WangyiSearchActivity.class);
        } else {
            TSnackbar.make(view.getRootView(), getString(R.string.nav_bar_reference_error), TSnackbar.LENGTH_LONG, TSnackbar.APPEAR_FROM_BOTTOM_TO_TOP)
                    .setPromptThemBackground(Prompt.WARNING).show();
            return;
        }
        startActivity(intent);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }

    private static void trustEveryone() {
        try {
            HttpsURLConnection.setDefaultHostnameVerifier(new HostnameVerifier() {
                public boolean verify(String hostname, SSLSession session) {
                    return true;
                }
            });

            SSLContext context = SSLContext.getInstance("TLS");
            context.init(null, new X509TrustManager[]{new X509TrustManager() {
                public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
                }

                public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
                }

                public X509Certificate[] getAcceptedIssuers() {
                    return new X509Certificate[0];
                }
            }}, new SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(context.getSocketFactory());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

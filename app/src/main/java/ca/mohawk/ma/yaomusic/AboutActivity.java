package ca.mohawk.ma.yaomusic;

import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.widget.ListView;
import android.widget.SimpleAdapter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class AboutActivity extends AppCompatActivity {
    final String subtitle = "subtitle";
    final String content = "content";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        ArrayList<Map<String, String>> list = new ArrayList<>();

        Map<String, String> model = new HashMap<>();
        model.put(subtitle, getResources().getString(R.string.about_activity_model));
        model.put(content, "RX");
        list.add(model);

        Map<String, String> code = new HashMap<>();
        code.put(subtitle, getResources().getString(R.string.about_activity_code));
        code.put(content, "78");
        list.add(code);

        Map<String, String> author = new HashMap<>();
        author.put(subtitle, getResources().getString(R.string.about_activity_author));
        author.put(content, getResources().getString(R.string.about_activity_author_name));
        list.add(author);

        Map<String, String> source = new HashMap<>();
        source.put(subtitle, getResources().getString(R.string.about_activity_source));
        source.put(content, getResources().getString(R.string.about_activity_github));
        list.add(source);

        Map<String, String> notice = new HashMap<>();
        notice.put(subtitle, getResources().getString(R.string.about_activity_notice));
        notice.put(content, getResources().getString(R.string.about_activity_mmp));
        list.add(notice);

        SimpleAdapter adapter = new SimpleAdapter(this, list, R.layout.custom_about_list_item, new String[]{subtitle, content}, new int[]{R.id.subtitle, R.id.content});

        ListView aboutList = findViewById(R.id.aboutList);
        aboutList.setAdapter(adapter);

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
}

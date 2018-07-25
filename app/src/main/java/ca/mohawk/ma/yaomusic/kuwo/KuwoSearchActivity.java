package ca.mohawk.ma.yaomusic.kuwo;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.support.design.widget.TabLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import android.view.inputmethod.InputMethodManager;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.HeaderViewListAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.SearchView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.trycatch.mysnackbar.Prompt;
import com.trycatch.mysnackbar.TSnackbar;

import java.util.HashMap;
import java.util.Map;

import ca.mohawk.ma.yaomusic.AlbumActivity;
import ca.mohawk.ma.yaomusic.ArtistActivity;
import ca.mohawk.ma.yaomusic.MiniPlayerFragment;
import ca.mohawk.ma.yaomusic.PlayerService;
import ca.mohawk.ma.yaomusic.R;
import ca.mohawk.ma.yaomusic.util.SearchListAdapter;
import ca.mohawk.ma.yaomusic.database.SearchHistoryDatabaseHelper;

public class KuwoSearchActivity extends AppCompatActivity implements TabLayout.OnTabSelectedListener, SearchView.OnQueryTextListener, AdapterView.OnItemClickListener {

    /**
     * The {@link android.support.v4.view.PagerAdapter} that will provide
     * fragments for each of the sections. We use a
     * {@link FragmentPagerAdapter} derivative, which will keep every
     * loaded fragment in memory. If this becomes too memory intensive, it
     * may be best to switch to a
     * {@link android.support.v4.app.FragmentStatePagerAdapter}.
     */
    SectionsPagerAdapter mSectionsPagerAdapter;

    /**
     * The {@link ViewPager} that will host the section contents.
     */
    ViewPager mViewPager;

    TabLayout tabLayout;
    SearchView searchView;
    ProgressBar progressBar;
    ListView history;

    KuwoSearchTask kuwoSearchTask;

    View clearHistory;

    SearchHistoryDatabaseHelper searchHistoryDatabaseHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_kuwo_search);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        searchHistoryDatabaseHelper = new SearchHistoryDatabaseHelper(this);

        // Create the adapter that will return a fragment for each of the three
        // primary sections of the activity.
        mSectionsPagerAdapter = new KuwoSearchActivity.SectionsPagerAdapter(getSupportFragmentManager());

        // Set up the ViewPager with the sections adapter.
        mViewPager = (ViewPager) findViewById(R.id.container);
        mViewPager.setAdapter(mSectionsPagerAdapter);

        tabLayout = (TabLayout) findViewById(R.id.tabs);

        mViewPager.addOnPageChangeListener(new TabLayout.TabLayoutOnPageChangeListener(tabLayout));
        tabLayout.addOnTabSelectedListener(new TabLayout.ViewPagerOnTabSelectedListener(mViewPager));
        tabLayout.addOnTabSelectedListener(this);

        searchView = findViewById(R.id.search_bar);
        searchView.setSubmitButtonEnabled(true);
        searchView.setOnQueryTextListener(this);
        int magId = getResources().getIdentifier("android:id/search_mag_icon", null, null);
        ImageView magImage = (ImageView) searchView.findViewById(magId);
        magImage.setLayoutParams(new LinearLayout.LayoutParams(0, 0));

        progressBar = findViewById(R.id.progressBar);

        history = findViewById(R.id.history);
        history.setOnItemClickListener(this);
        displayHistory(null);
    }

    public void displayHistory(String keyword) {
        Cursor cursor;
        if (keyword == null || keyword.isEmpty()) {
            cursor = searchHistoryDatabaseHelper.getRecordsCursor();
        } else {
            cursor = searchHistoryDatabaseHelper.getRecordsCursor(keyword);
        }

        SimpleCursorAdapter simpleCursorAdapter = new SimpleCursorAdapter(this, R.layout.custom_history_list_item, cursor, new String[]{searchHistoryDatabaseHelper.columns[1]}, new int[]{R.id.record});

        history.setAdapter(simpleCursorAdapter);
        if (clearHistory == null) {
            clearHistory = LayoutInflater.from(this).inflate(R.layout.custom_list_header, null);
            history.addHeaderView(clearHistory, null, false);
        }

        if (cursor.getCount() > 0){
            history.setVisibility(View.VISIBLE);
        } else {
            history.setVisibility(View.GONE);
        }
    }

    public void clearHistoryOnClick(View view) {
        searchHistoryDatabaseHelper.clearRecords();
        TSnackbar.make(view.getRootView(), getResources().getString(R.string.list_header_clear_history_success), TSnackbar.LENGTH_LONG, TSnackbar.APPEAR_FROM_TOP_TO_DOWN)
                .setPromptThemBackground(Prompt.SUCCESS).show();
        displayHistory(null);
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

    @Override
    public boolean onQueryTextSubmit(String query) {
        search(tabLayout.getSelectedTabPosition(), query);
        return false;
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        displayHistory(newText);
        return false;
    }

    public void search(int tabId, String keyword) {
        if (keyword.isEmpty()) return;

        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null && getCurrentFocus() != null) {
            imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
        }

        searchHistoryDatabaseHelper.insertRecord(keyword);
        history.setVisibility(View.GONE);

        KuwoSearchActivity.PlaceholderFragment placeholderFragment = (KuwoSearchActivity.PlaceholderFragment) this.mSectionsPagerAdapter
                .instantiateItem(this.mViewPager, this.tabLayout.getSelectedTabPosition());
        placeholderFragment.removeFooter();
        placeholderFragment.showProgress();
        placeholderFragment.nextPage = 1;

        if (kuwoSearchTask != null) {
            kuwoSearchTask.cancel(true);
        }
        kuwoSearchTask = new KuwoSearchTask(this);
        kuwoSearchTask.execute(tabId, keyword);
    }

    @Override
    public void onTabSelected(TabLayout.Tab tab) {
        search(tab.getPosition(), this.searchView.getQuery().toString());
    }

    @Override
    public void onTabUnselected(TabLayout.Tab tab) {
        return;
    }

    @Override
    public void onTabReselected(TabLayout.Tab tab) {
        return;
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        TextView record = view.findViewById(R.id.record);
        searchView.setQuery(record.getText().toString(), true);
    }

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class PlaceholderFragment extends Fragment {
        /**
         * The fragment argument representing the section number for this
         * fragment.
         */
        private static final String ARG_SECTION_NUMBER = "section_number";

        private View rootView;

        View mFooter;

        int nextPage = 1;
        Boolean loadMoreFlag = true;
        final int LIMIT = 20;

        public PlaceholderFragment() {
        }

        /**
         * Returns a new instance of this fragment for the given section
         * number.
         */
        public static PlaceholderFragment newInstance(int sectionNumber) {
            PlaceholderFragment fragment = new PlaceholderFragment();
            Bundle args = new Bundle();
            args.putInt(ARG_SECTION_NUMBER, sectionNumber);
            fragment.setArguments(args);
            return fragment;
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            rootView = inflater.inflate(R.layout.fragment_kuwo_search, container, false);
            return rootView;
        }

        public void setAdapter(SearchListAdapter adapter) {
            final ListView listView = rootView.findViewById(R.id.listView);
            listView.setAdapter(adapter);
            listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
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

                    int songTab = 1;
                    int albumTab = 2;
                    int artistTab = 3;
                    int songListTab = 4;
                    Intent intent = null;
                    switch (getArguments().getInt(ARG_SECTION_NUMBER)) {
                        case 1:
                            Intent serviceIntent = new Intent(getActivity(), PlayerService.class);

                            Map<String, String> song = new HashMap<>();
                            song.put("id", tmp.getItemHref(position));
                            song.put("title", tmp.getItemTitle(position));
                            song.put("artist", tmp.getItemSubtitle(position));
                            song.put("cover", tmp.getItemCover(position));

                            MiniPlayerFragment.changePlayInfo(song.get("cover"), song.get("title"), song.get("artist"));

                            int index = PlayerService.list.indexOf(song);
                            if (index < 0) {
                                PlayerService.list.add(song);
                                serviceIntent.putExtra("index", PlayerService.list.indexOf(song));
                            } else {
                                serviceIntent.putExtra("index", index);
                            }
                            serviceIntent.putExtra("action", PlayerService.PLAY);
                            getActivity().startService(serviceIntent);
                            getActivity().finish();
                            break;
                        case 2:
                            intent = new Intent(getActivity(), AlbumActivity.class);
                            intent.putExtra("reference", R.id.nav_kuwo);
                            intent.putExtra("id", tmp.getItemHref(position));
                            intent.putExtra("cover", tmp.getItemCover(position));
                            intent.putExtra("title", tmp.getItemTitle(position));
                            break;
                        case 3:
                            intent = new Intent(getActivity(), ArtistActivity.class);
                            intent.putExtra("reference", R.id.nav_kuwo);
                            intent.putExtra("id", tmp.getItemHref(position));
                            intent.putExtra("cover", tmp.getItemCover(position));
                            intent.putExtra("title", tmp.getItemTitle(position));
                            break;
                    }
                    if (intent != null) {
                        startActivity(intent);
                        getActivity().finish();
                    }
                }
            });
            listView.setOnScrollListener(new AbsListView.OnScrollListener() {
                @Override
                public void onScrollStateChanged(AbsListView view, int scrollState) {
                    return;
                }

                @Override
                public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                    if ((firstVisibleItem + visibleItemCount) == totalItemCount && loadMoreFlag && totalItemCount >= LIMIT) {
                        mFooter = LayoutInflater.from(getContext()).inflate(R.layout.custom_list_footer, null);
                        listView.addFooterView(mFooter, null, false);
                        loadMoreFlag = false;

                        KuwoSearchActivity kuwoSearchActivity = (KuwoSearchActivity) getActivity();
                        KuwoSearchTask kuwoSearchTask = new KuwoSearchTask(kuwoSearchActivity);
                        kuwoSearchTask.execute(kuwoSearchActivity.tabLayout.getSelectedTabPosition(), kuwoSearchActivity.searchView.getQuery().toString(), ++nextPage);

                    }
                }
            });
        }

        public SearchListAdapter getAdapter() {
            if (rootView == null) return null;

            ListView listView = rootView.findViewById(R.id.listView);
            HeaderViewListAdapter headerViewListAdapter = (HeaderViewListAdapter) listView.getAdapter();
            return (SearchListAdapter) headerViewListAdapter.getWrappedAdapter();
        }

        public void removeFooter() {
            if (rootView == null) return;

            ListView listView = rootView.findViewById(R.id.listView);
            listView.removeFooterView(mFooter);
        }

        public void endFooter() {
            if (mFooter == null) return;

            mFooter.findViewById(R.id.progressBar2).setVisibility(View.GONE);
            TextView more = mFooter.findViewById(R.id.more);
            more.setText(R.string.list_footer_no_more);
        }

        public void hideProgress() {
            if (rootView == null) return;

            ProgressBar progressBar = rootView.findViewById(R.id.progressBar);
            progressBar.setVisibility(View.GONE);
        }

        public void showProgress() {
            if (rootView == null) return;

            ProgressBar progressBar = rootView.findViewById(R.id.progressBar);
            progressBar.setVisibility(View.VISIBLE);
        }
    }

    /**
     * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     */
    public class SectionsPagerAdapter extends FragmentPagerAdapter {

        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            // getItem is called to instantiate the fragment for the given page.
            // Return a PlaceholderFragment (defined as a static inner class below).
            return PlaceholderFragment.newInstance(position + 1);
        }

        @Override
        public int getCount() {
            // Show 3 total pages.
            return 3;
        }
    }
}

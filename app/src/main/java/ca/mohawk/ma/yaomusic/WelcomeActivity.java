package ca.mohawk.ma.yaomusic;

import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import android.widget.Button;
import android.widget.ImageView;

import java.util.Timer;
import java.util.TimerTask;

import ca.mohawk.ma.yaomusic.ui.RoundAngleImageView;

public class WelcomeActivity extends AppCompatActivity {

    private int counter = 0;
    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            counter++;
            if (counter > 1) {
                Intent intent = new Intent(WelcomeActivity.this, MainActivity.class);
                WelcomeActivity.this.startActivity(intent);
                WelcomeActivity.this.finish();
                timer.cancel();
            }
        }
    };
    private Timer timer;
    private RoundAngleImageView icon;
    SharedPreferences sharedPreferences;

    /**
     * The {@link android.support.v4.view.PagerAdapter} that will provide
     * fragments for each of the sections. We use a
     * {@link FragmentPagerAdapter} derivative, which will keep every
     * loaded fragment in memory. If this becomes too memory intensive, it
     * may be best to switch to a
     * {@link android.support.v4.app.FragmentStatePagerAdapter}.
     */
    SectionsPagerAdapter mSectionsPagerAdapter;

    final String IS_PASS = "PASSGUIDE";
    final String REFERENCE_SELECTED = "reference";
    final String LANGUAGE_SELECTED = "language";

    /**
     * The {@link ViewPager} that will host the section contents.
     */
    ViewPager mViewPager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);

        icon = findViewById(R.id.icon);

        // Create the adapter that will return a fragment for each of the three
        // primary sections of the activity.
        mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());

        // Set up the ViewPager with the sections adapter.
        mViewPager = (ViewPager) findViewById(R.id.container);
        mViewPager.setAdapter(mSectionsPagerAdapter);


    }

    @Override
    protected void onResume() {
        super.onResume();

        sharedPreferences = getSharedPreferences(getString(R.string.share_preferences), MODE_PRIVATE);
        if (sharedPreferences.getBoolean(IS_PASS, false) && sharedPreferences.contains(LANGUAGE_SELECTED) && sharedPreferences.contains(REFERENCE_SELECTED)) {
            timer = new Timer();
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    handler.sendEmptyMessage(0);
                }
            }, 0, 1000);
        } else {
            icon.setVisibility(View.GONE);
        }
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

        WelcomeActivity activity;

        int def = 0;
        ImageView[] referenceSelect;
        int page1 = 0, page2 = 1;
        Button[] languageSelect;
        Button finish;

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
            activity = (WelcomeActivity) getActivity();
            View rootView;
            if (getArguments().getInt(ARG_SECTION_NUMBER) == 1) {
                rootView = inflater.inflate(R.layout.fragment_welcome_reference, container, false);
                final int[][] resIds = new int[][]{{
                        R.id.yiting,
                        R.id.qianqian,
                        R.id.kuwo,
                        R.id.qq,
                        R.id.wangyi
                }, {
                        R.id.nav_yiting,
                        R.id.nav_qianqian,
                        R.id.nav_kuwo,
                        R.id.nav_qq,
                        R.id.nav_wangyi
                }};
                referenceSelect = new ImageView[resIds[0].length];
                for (int i = 0; i < resIds[0].length; i++) {
                    referenceSelect[i] = rootView.findViewById(resIds[0][i]);
                    final int finalI = i;
                    referenceSelect[i].setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            activity.getSharedPreferences(getString(R.string.share_preferences), MODE_PRIVATE).edit().putInt(activity.REFERENCE_SELECTED, resIds[1][finalI]).apply();
                            for (int i = 0; i < resIds[0].length; i++) {
                                referenceSelect[i].setAlpha(0.25f);
                            }
                            referenceSelect[finalI].setAlpha(1f);
                        }
                    });
                }
                if (!activity.getSharedPreferences(getString(R.string.share_preferences), MODE_PRIVATE).contains(activity.REFERENCE_SELECTED)) {
                    activity.getSharedPreferences(getString(R.string.share_preferences), MODE_PRIVATE).edit().putInt(activity.REFERENCE_SELECTED, resIds[1][def]).apply();
                    referenceSelect[def].setAlpha(1f);
                }

                Button next = rootView.findViewById(R.id.next);
                next.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        activity.mViewPager.setCurrentItem(page2, true);
                    }
                });

            } else {
                rootView = inflater.inflate(R.layout.fragment_welcome_language, container, false);
                final int[][] resIds = new int[][]{{
                        R.id.chinese,
                        R.id.english
                }, {
                        R.id.nav_chinese,
                        R.id.nav_english
                }, {
                        R.string.welcome_finish,
                        R.string.welcome_finish_en
                }};

                finish = rootView.findViewById(R.id.finish);
                finish.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        activity.getSharedPreferences(getString(R.string.share_preferences), MODE_PRIVATE).edit().putBoolean(activity.IS_PASS, true).apply();
                        activity.finish();
                        Intent intent = new Intent(getContext(), MainActivity.class);
                        startActivity(intent);
                    }
                });

                languageSelect = new Button[resIds[0].length];
                for (int i = 0; i < resIds[0].length; i++) {
                    languageSelect[i] = rootView.findViewById(resIds[0][i]);
                    final int finalI = i;
                    languageSelect[i].setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            activity.getSharedPreferences(getString(R.string.share_preferences), MODE_PRIVATE).edit().putInt(activity.LANGUAGE_SELECTED, resIds[1][finalI]).apply();
                            for (int i = 0; i < resIds[0].length; i++) {
                                languageSelect[i].setTextColor(Color.WHITE);
                            }
                            languageSelect[finalI].setTextColor(getResources().getColor(R.color.colorAccent));
                            finish.setText(resIds[2][finalI]);
                        }
                    });
                }
                if (!activity.getSharedPreferences(getString(R.string.share_preferences), MODE_PRIVATE).contains(activity.LANGUAGE_SELECTED)) {
                    activity.getSharedPreferences(getString(R.string.share_preferences), MODE_PRIVATE).edit().putInt(activity.LANGUAGE_SELECTED, resIds[1][def]).apply();
                    languageSelect[def].setTextColor(getResources().getColor(R.color.colorAccent));
                    finish.setText(resIds[2][def]);
                }

                Button prev = rootView.findViewById(R.id.prev);
                prev.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        activity.mViewPager.setCurrentItem(page1, true);
                    }
                });

            }
            return rootView;
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
            // Show 2 total pages.
            return 2;
        }
    }
}

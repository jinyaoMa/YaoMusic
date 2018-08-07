package ca.mohawk.ma.yaomusic;


import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.ContextMenu;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Map;
import java.util.Random;

import ca.mohawk.ma.yaomusic.ui.RoundAngleImageView;
import ca.mohawk.ma.yaomusic.util.SongsAdapter;


/**
 * A simple {@link Fragment} subclass.
 */
public class MiniPlayerFragment extends Fragment {

    public MiniPlayerFragment() {
        // Required empty public constructor
    }

    public static View view;

    private static TextView title;
    private static TextView artist;
    private static RoundAngleImageView cover;
    private static ImageButton play;
    private static ImageButton next;
    private static ImageButton option;
    private static SeekBar seekBar;
    private static SeekBar loadBar;
    private static TextView passTime;
    private static TextView leftTime;
    private static ProgressBar processing;

    private static Boolean isPlay = false;

    private Dialog playlistDialog;
    private ListView currentPlaylist;
    private EditText etName;

    private Random random;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        view = inflater.inflate(R.layout.fragment_mini_player, container, false);

        title = view.findViewById(R.id.title);
        artist = view.findViewById(R.id.artist);
        cover = view.findViewById(R.id.cover);
        play = view.findViewById(R.id.play);
        next = view.findViewById(R.id.next);
        option = view.findViewById(R.id.option);
        seekBar = view.findViewById(R.id.seekBar);
        loadBar = view.findViewById(R.id.loadBar);
        passTime = view.findViewById(R.id.passTime);
        leftTime = view.findViewById(R.id.leftTime);
        processing = view.findViewById(R.id.processing);

        play.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playOnClick(v);
            }
        });

        next.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                nextOnClick(v);
            }
        });

        option.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                optionOnClick(v);
            }
        });

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                return;
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                return;
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                seekBarOnProgressChanged(seekBar);
            }
        });

        random = new Random();

        return view;
    }

    public void seekBarOnProgressChanged(SeekBar seekBar) {
        Intent serviceIntent = new Intent(getActivity(), PlayerService.class);
        serviceIntent.putExtra("action", PlayerService.SEEK);
        serviceIntent.putExtra("progress", seekBar.getProgress());
        getActivity().startService(serviceIntent);
    }

    public void playOnClick(View view) {
        Intent intent = new Intent(getActivity(), PlayerService.class);
        intent.putExtra("action", PlayerService.PAUSE);
        getActivity().startService(intent);
    }

    public void nextOnClick(View view) {
        Intent intent = new Intent(getActivity(), PlayerService.class);
        intent.putExtra("action", PlayerService.NEXT);
        getActivity().startService(intent);
    }

    public void optionOnClick(View view) {
        playlistDialog = new Dialog(getActivity(), R.style.BottomDialog);
        LinearLayout root = (LinearLayout) LayoutInflater.from(getActivity()).inflate(
                R.layout.dialog_current_playlist, null);

        etName = root.findViewById(R.id.etName);
        currentPlaylist = root.findViewById(R.id.currentPlaylist);
        if (PlayerService.list.size() > 0) {
            final SongsAdapter adapter = new SongsAdapter(getActivity(), PlayerService.list.toListForSongs(), R.layout.custom_songs_list_item);
            currentPlaylist.setAdapter(adapter);
            currentPlaylist.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    Intent intent = new Intent(getActivity(), PlayerService.class);
                    intent.putExtra("action", PlayerService.SELECT);
                    intent.putExtra("position", position);
                    getActivity().startService(intent);
                    playlistDialog.dismiss();
                }
            });
            root.findViewById(R.id.btnClear).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    PlayerService.list.clear();
                    PlayerService.currentIndex = 0;
                    adapter.clear();
                    adapter.notifyDataSetChanged();
                    Intent intent = new Intent(getActivity(), PlayerService.class);
                    intent.putExtra("action", PlayerService.STOP);
                    getActivity().startService(intent);
                    togglePlay(false);
                }
            });
            root.findViewById(R.id.btnCollect).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Date currentTime = new Date();
                    SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                    String name = formatter.format(currentTime);
                    if (!etName.getText().toString().trim().equals("")) {
                        name = etName.getText().toString();
                    }
                    String cover = PlayerService.list.get(random.nextInt(PlayerService.list.size() - 1)).get("cover");
                    String[] ids = new String[PlayerService.list.size()];
                    for (int i = 0; i < ids.length; i++) {
                        ids[i] = PlayerService.list.get(i).get("id");
                    }
                    MainFragment.insert(name, cover, ids);
                    playlistDialog.dismiss();
                }
            });
        }

        playlistDialog.setContentView(root);
        Window dialogWindow = playlistDialog.getWindow();
        dialogWindow.setGravity(Gravity.BOTTOM);

        WindowManager.LayoutParams lp = dialogWindow.getAttributes();
        lp.x = 0;
        lp.y = 0;
        lp.width = getResources().getDisplayMetrics().widthPixels;
        root.measure(0, 0);
        lp.height = root.getMeasuredHeight();

        lp.alpha = 9f;
        dialogWindow.setAttributes(lp);
        playlistDialog.show();

        currentPlaylist.post(new Runnable() {
            @Override
            public void run() {
                int selection = PlayerService.currentIndex - 20;
                if (selection > -1) {
                    currentPlaylist.setSelection(selection);
                }
                currentPlaylist.smoothScrollToPositionFromTop(PlayerService.currentIndex, 0, 1000);
            }
        });
    }

    public static void changePlayInfo(String strCover, String strTitle, String strArtist) {
        cover.setImageURL(strCover);
        title.setText(strTitle);
        artist.setText(strArtist);
    }

    public static void togglePlay(Boolean flag) {
        isPlay = flag;
        if (isPlay) {
            play.setImageResource(android.R.drawable.ic_media_pause);
        } else {
            play.setImageResource(android.R.drawable.ic_media_play);
        }
    }

    public static void togglePlay() {
        togglePlay(!isPlay);
    }

    public static void updateLoadBar(int percent) {
        if (loadBar != null) {
            loadBar.setProgress(percent, true);
        }
    }

    public static void updateSeekBarProgress(int progress, int max) {
        if (seekBar != null) {
            seekBar.setMax(max);
            seekBar.setProgress(progress, true);

            int left = seekBar.getMax() - progress;
            leftTime.setText(String.format("%02d:%02d", left / 60000, left / 1000 % 60));
            passTime.setText(String.format("%02d:%02d", progress / 60000, progress / 1000 % 60));
        }
    }

    public static void toggleProcessing(Boolean flag) {
        processing.setVisibility(flag ? View.VISIBLE : View.GONE);
    }

    public static void refresh() {
        if (PlayerService.mediaPlayer != null) {
            togglePlay(PlayerService.mediaPlayer.isPlaying());
        } else {
            togglePlay(false);
        }
        if (PlayerService.list.size() > 0) {
            cover.setImageURL(PlayerService.list.get(PlayerService.currentIndex).get("cover"));
            title.setText(PlayerService.list.get(PlayerService.currentIndex).get("title"));
            artist.setText(PlayerService.list.get(PlayerService.currentIndex).get("artist"));
        }
    }

}

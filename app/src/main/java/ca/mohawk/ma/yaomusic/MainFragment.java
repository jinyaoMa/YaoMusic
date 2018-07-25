package ca.mohawk.ma.yaomusic;


import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.HeaderViewListAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import ca.mohawk.ma.yaomusic.database.PlaylistDatabaseHelper;
import ca.mohawk.ma.yaomusic.util.CurrentPlaylistArrayListMap;
import ca.mohawk.ma.yaomusic.util.PlaylistsAdapter;
import ca.mohawk.ma.yaomusic.util.SongsAdapter;


/**
 * A simple {@link Fragment} subclass.
 */
public class MainFragment extends Fragment {
    private static PlaylistDatabaseHelper playlistDatabaseHelper;
    private GridView gridView;
    private static PlaylistsAdapter adapter;
    private static TextView textView;

    public MainFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        playlistDatabaseHelper = new PlaylistDatabaseHelper(getActivity());
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_main, container, false);
        textView = view.findViewById(R.id.notice);
        gridView = view.findViewById(R.id.playlistGridView);
        adapter = new PlaylistsAdapter(getActivity(), playlistDatabaseHelper.getPlaylists(), R.layout.custom_playlists_grid_item);
        gridView.setAdapter(adapter);

        gridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                PlaylistsAdapter adapter = (PlaylistsAdapter) parent.getAdapter();
                PlayerService.list = playlistDatabaseHelper.getSongsByPlaylist(adapter.getItem(position).get("_id"));
                PlayerService.currentIndex = 0;

                Intent intent = new Intent(getActivity(), PlayerService.class);
                intent.putExtra("index", 0);
                intent.putExtra("action", PlayerService.PLAY);
                getActivity().startService(intent);

                MiniPlayerFragment.refresh();
            }
        });

        gridView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(final AdapterView<?> parent, View view, final int position, long id) {
                AlertDialog dialog = new AlertDialog.Builder(getActivity())
                        .setIcon(R.mipmap.ic_launcher)
                        .setTitle(R.string.app_name)
                        .setMessage(R.string.dialog_playlist_question)
                        .setNegativeButton(R.string.dialog_playlist_no, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        })
                        .setPositiveButton(R.string.dialog_playlist_yes, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                PlaylistsAdapter adapter = (PlaylistsAdapter) parent.getAdapter();
                                playlistDatabaseHelper.deletePlaylist(adapter.getItem(position).get("_id"));
                                update();

                                dialog.dismiss();
                            }
                        }).create();
                dialog.show();
                return false;
            }
        });

        if (adapter.getCount() > 0) {
            textView.setVisibility(View.GONE);
        } else {
            textView.setVisibility(View.VISIBLE);
        }

        return view;
    }

    public static void insert(String name, String cover, String[] ids) {
        playlistDatabaseHelper.insertPlaylist(name, cover, ids);
        update();
    }

    public static void update() {
        adapter.setDataList(playlistDatabaseHelper.getPlaylists());

        if (adapter.getCount() > 0) {
            textView.setVisibility(View.GONE);
        } else {
            textView.setVisibility(View.VISIBLE);
        }
    }

}

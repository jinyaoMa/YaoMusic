package ca.mohawk.ma.yaomusic.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import ca.mohawk.ma.yaomusic.util.CurrentPlaylistArrayListMap;

public class PlaylistDatabaseHelper extends SQLiteOpenHelper {

    private static final String name = "playlist.db";
    private static final int version = 1;

    public final String tableSongs = "songs";
    public final String[] columnsSongs = {"_id", "reference", "songId", "songPath", "songTitle", "artistTitle", "artistId", "albumTitle", "albumId", "coverPath", "id", "artist", "title", "cover"};

    public final String tablePlaylists = "playlists";
    public final String[] columnsPlaylists = {"_id", "name", "date", "cover"};

    public final String tableLines = "lines";
    public final String[] columnsLines = {"playlist_id", "song_id"};

    public PlaylistDatabaseHelper(Context context) {
        super(context, name, null, version);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(String.format(Locale.getDefault(),
                "CREATE TABLE %s ( %s INTEGER PRIMARY KEY, %s TEXT NOT NULL, %s TEXT NOT NULL, %s TEXT, %s TEXT, %s TEXT, %s TEXT, %s TEXT, %s TEXT, %s TEXT, %s TEXT, %s TEXT, %s TEXT, %s TEXT )",
                tableSongs, columnsSongs[0], columnsSongs[1], columnsSongs[2], columnsSongs[3], columnsSongs[4], columnsSongs[5], columnsSongs[6], columnsSongs[7], columnsSongs[8], columnsSongs[9], columnsSongs[10], columnsSongs[11], columnsSongs[12], columnsSongs[13]));

        db.execSQL(String.format(Locale.getDefault(),
                "CREATE TABLE %s ( %s INTEGER PRIMARY KEY, %s TEXT, %s TEXT default (datetime('now', 'localtime')), %s TEXT )",
                tablePlaylists, columnsPlaylists[0], columnsPlaylists[1], columnsPlaylists[2], columnsPlaylists[3]));

        db.execSQL(String.format(Locale.getDefault(),
                "CREATE TABLE %s ( %s INTEGER NOT NULL, %s INTEGER NOT NULL )",
                tableLines, columnsLines[0], columnsLines[1]));
    }

    public long insertSong(Map<String, String> record) {
        if (record.containsKey("id")) {
            String[] temp = record.get("id").split(":");
            record.put(columnsSongs[1], temp[0] + ":");
            record.put(columnsSongs[2], temp[1]);
        }

        String where = columnsSongs[1] + " = ? and " + columnsSongs[2] + " = ?";
        String[] args = {record.get(columnsSongs[1]), record.get(columnsSongs[2])};
        Cursor cursor = getReadableDatabase().query(tableSongs, columnsSongs, where, args, null, null, null);
        if (cursor.getCount() == 0) {
            ContentValues values = new ContentValues();
            for (int i = 0; i < columnsSongs.length; i++) {
                values.put(columnsSongs[i], record.get(columnsSongs[i]));
            }
            return getWritableDatabase().insert(tableSongs, null, values);
        } else {
            cursor.moveToNext();
            return cursor.getInt(0);
        }
    }

    public long updateSong(Map<String, String> record) {
        if (record.containsKey("id")) {
            String[] temp = record.get("id").split(":");
            record.put(columnsSongs[1], temp[0] + ":");
            record.put(columnsSongs[2], temp[1]);
        }

        String where = columnsSongs[1] + " = ? and " + columnsSongs[2] + " = ?";
        String[] args = {record.get(columnsSongs[1]), record.get(columnsSongs[2])};
        ContentValues values = new ContentValues();
        for (int i = 0; i < columnsSongs.length; i++) {
            if (record.containsKey(columnsSongs[i])) {
                values.put(columnsSongs[i], record.get(columnsSongs[i]));
            }
        }
        return getWritableDatabase().update(tableSongs, values, where, args);
    }

    public Map<String, String> getSong(String id) {
        Map<String, String> song = new HashMap<>();
        Cursor cursor = getReadableDatabase().query(tableSongs, columnsSongs, columnsSongs[0] + " = ?", new String[]{id}, null, null, null);
        if (cursor.getCount() > 0) {
            cursor.moveToFirst();
            for (int i = 0; i < columnsSongs.length; i++) {
                song.put(columnsSongs[i], cursor.getString(i));
            }
            song.put("id", cursor.getString(1) + cursor.getString(2));
            return song;
        } else {
            return null;
        }
    }

    public String findSongPath(String ref, String songId) {
        Map<String, String> song = new HashMap<>();
        Cursor cursor = getReadableDatabase().query(tableSongs, columnsSongs, columnsSongs[1] + " = ? and " + columnsSongs[2] + " = ?", new String[]{ref, songId}, null, null, null);
        if (cursor.getCount() > 0) {
            cursor.moveToFirst();
            return cursor.getString(3);
        } else {
            return null;
        }
    }

    public String getSongUrl(String id) {
        Cursor cursor = getReadableDatabase().query(tableSongs, columnsSongs, columnsSongs[0] + " = ?", new String[]{id}, null, null, null);
        if (cursor.getCount() > 0) {
            cursor.moveToFirst();
            return cursor.getString(3);
        } else {
            return null;
        }
    }

    public long insertPlaylist(String name, String cover, String[] ids) {
        ContentValues values = new ContentValues();
        values.put(columnsPlaylists[1], name);
        values.put(columnsPlaylists[3], cover);
        long id = getWritableDatabase().insert(tablePlaylists, null, values);
        for (int i = 0; i < ids.length; i++) {
            Cursor cursor = getReadableDatabase().query(tableSongs, columnsSongs, columnsSongs[10] + " = ?", new String[]{ids[i]}, null, null, null);
            if (cursor.getCount() > 0) {
                ContentValues temp = new ContentValues();
                temp.put(columnsLines[0], id);
                cursor.moveToFirst();
                temp.put(columnsLines[1], cursor.getInt(0));
                getWritableDatabase().insert(tableLines, null, temp);
            }
        }
        return id;
    }

    public ArrayList<Map<String, String>> getPlaylists() {
        ArrayList<Map<String, String>> result = new ArrayList<>();
        Cursor cursor = getReadableDatabase().query(tablePlaylists, columnsPlaylists, null, null, null, null, null);
        while (cursor.moveToNext()) {
            Map<String, String> list = new HashMap<>();
            for (int i = 0; i < columnsPlaylists.length; i++) {
                list.put(columnsPlaylists[i], cursor.getString(i));
            }
            result.add(list);
        }
        return result;
    }

    public CurrentPlaylistArrayListMap getSongsByPlaylist(String id) {
        CurrentPlaylistArrayListMap list = new CurrentPlaylistArrayListMap();
        Cursor cursor = getReadableDatabase().query(tableLines, columnsLines, columnsLines[0] + " = ?", new String[]{id}, null, null, null);
        if (cursor.getCount() > 0) {
            while (cursor.moveToNext()) {
                Map<String, String> song = new HashMap<>();
                Cursor temp = getReadableDatabase().query(tableSongs, columnsSongs, columnsSongs[0] + " = ?", new String[]{cursor.getString(1)}, null, null, null);
                if (temp.getCount() > 0) {
                    temp.moveToFirst();
                    for (int i = 0; i < columnsSongs.length; i++) {
                        song.put(columnsSongs[i], temp.getString(i));
                    }
                    list.add(song);
                }
            }
        }
        return list;
    }

    public Boolean isSongsEmpty() {
        return getReadableDatabase().query(tableSongs, columnsSongs, null, null, null, null, null).getCount() == 0;
    }

    public Boolean isPlaylistEmpty(String id) {
        return getReadableDatabase().query(tableLines, columnsLines, columnsLines[0] + " = ?", new String[]{id}, null, null, null).getCount() == 0;
    }

    public void clearSongs() {
        getWritableDatabase().delete(tableSongs, null, null);
    }

    public void deletePlaylist(String id) {
        getWritableDatabase().delete(tableLines, columnsLines[0] + " = ?", new String[]{id});
        getWritableDatabase().delete(tablePlaylists, columnsPlaylists[0] + " = ?", new String[]{id});
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }
}
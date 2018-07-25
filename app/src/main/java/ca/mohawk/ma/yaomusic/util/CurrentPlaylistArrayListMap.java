package ca.mohawk.ma.yaomusic.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import ca.mohawk.ma.yaomusic.PlayerService;

public class CurrentPlaylistArrayListMap extends ArrayList<Map<String, String>> {

    public int indexOf(Map<String, String> song) {
        if (song != null) {
            if (song.containsKey("id")) {
                String id = song.get("id");
                for (int i = (size() - 1); i >= 0; i--) {
                    if (id.equals(get(i).get("id"))) {
                        return i;
                    }
                }
            }
        }
        return -1;
    }

    public ArrayList<Map<String, Object>> toListForSongs() {
        ArrayList<Map<String, Object>> list = new ArrayList<>();

        for (int i = 0; i < size(); i++) {
            Map<String, Object> song = new HashMap<>();
            Map<String, String> temp = get(i);
            for (String key : temp.keySet()) {
                song.put(key, temp.get(key));
            }
            if (i == PlayerService.currentIndex) {
                song.put("checked", true);
            }
            list.add(song);
        }

        return list;
    }
}

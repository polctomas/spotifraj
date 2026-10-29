package sk.hudba.app;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.provider.MediaStore;
import androidx.core.content.ContextCompat;
import com.getcapacitor.JSArray;
import android.content.Intent;
import android.os.Build;
import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;

@CapacitorPlugin(name = "MediaCtl")
public class MediaCtlPlugin extends Plugin {

    @Override
    public void load() {
        MediaService.listener = (action, pos) -> {
            JSObject o = new JSObject();
            o.put("action", action);
            o.put("pos", pos);
            notifyListeners("action", o);
        };
    }

    @PluginMethod
    public void update(PluginCall call) {
        try {
            Context c = getContext();
            Intent i = new Intent(c, MediaService.class);
            i.putExtra("title", call.getString("title", ""));
            i.putExtra("artist", call.getString("artist", ""));
            i.putExtra("art", call.getString("art", ""));
            i.putExtra("playing", call.getBoolean("playing", false));
            Double pos = call.getDouble("pos", 0.0);
            Double dur = call.getDouble("dur", 0.0);
            i.putExtra("pos", (long) (pos * 1000));
            i.putExtra("dur", (long) (dur * 1000));
            if (Build.VERSION.SDK_INT >= 26) c.startForegroundService(i);
            else c.startService(i);
            call.resolve();
        } catch (Exception e) {
            call.reject(String.valueOf(e.getMessage()));
        }
    }

    @PluginMethod
    public void scan(PluginCall call) {
        final Context c = getContext();
        String perm = Build.VERSION.SDK_INT >= 33
                ? Manifest.permission.READ_MEDIA_AUDIO : Manifest.permission.READ_EXTERNAL_STORAGE;
        if (ContextCompat.checkSelfPermission(c, perm) != PackageManager.PERMISSION_GRANTED) {
            call.reject("perm");
            return;
        }
        new Thread(() -> {
            try {
                String sel = MediaStore.Audio.Media.DURATION + ">=60000 AND "
                        + MediaStore.Audio.Media.IS_RINGTONE + "=0 AND "
                        + MediaStore.Audio.Media.IS_NOTIFICATION + "=0 AND "
                        + MediaStore.Audio.Media.IS_ALARM + "=0";
                String[] cols = {MediaStore.Audio.Media._ID, MediaStore.Audio.Media.TITLE,
                        MediaStore.Audio.Media.ARTIST, MediaStore.Audio.Media.DURATION,
                        MediaStore.Audio.Media.DATA};
                Cursor cu = c.getContentResolver().query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                        cols, sel, null, MediaStore.Audio.Media.DATE_ADDED + " DESC");
                JSArray arr = new JSArray();
                if (cu != null) {
                    while (cu.moveToNext()) {
                        String path = cu.getString(4);
                        if (path == null) continue;
                        JSObject o = new JSObject();
                        o.put("id", cu.getLong(0));
                        o.put("title", cu.getString(1));
                        o.put("artist", cu.getString(2));
                        o.put("dur", cu.getLong(3));
                        o.put("path", path);
                        arr.put(o);
                    }
                    cu.close();
                }
                JSObject r = new JSObject();
                r.put("songs", arr);
                call.resolve(r);
            } catch (Exception e) {
                call.reject(String.valueOf(e.getMessage()));
            }
        }).start();
    }

    @PluginMethod
    public void stop(PluginCall call) {
        getContext().stopService(new Intent(getContext(), MediaService.class));
        call.resolve();
    }
}

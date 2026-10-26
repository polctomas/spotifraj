package sk.hudba.app;

import android.content.Context;
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
    public void stop(PluginCall call) {
        getContext().stopService(new Intent(getContext(), MediaService.class));
        call.resolve();
    }
}

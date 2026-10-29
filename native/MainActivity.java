package sk.hudba.app;

import android.Manifest;
import android.os.Build;
import android.os.Bundle;
import androidx.activity.OnBackPressedCallback;
import androidx.core.app.ActivityCompat;
import com.getcapacitor.BridgeActivity;
import java.util.ArrayList;

public class MainActivity extends BridgeActivity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        registerPlugin(MediaCtlPlugin.class);
        super.onCreate(savedInstanceState);

        ArrayList<String> p = new ArrayList<>();
        if (Build.VERSION.SDK_INT >= 33) {
            p.add(Manifest.permission.POST_NOTIFICATIONS);
            p.add(Manifest.permission.READ_MEDIA_AUDIO);
        } else {
            p.add(Manifest.permission.READ_EXTERNAL_STORAGE);
        }
        ActivityCompat.requestPermissions(this, p.toArray(new String[0]), 1);

        // Tlačidlo späť: najprv zavrie prehrávač / okno, a až potom schová appku (hudba hrá ďalej).
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                try {
                    getBridge().getWebView().evaluateJavascript(
                        "(function(){try{return window.hardBack?window.hardBack():false}catch(e){return false}})()",
                        v -> { if (!"true".equals(v)) moveTaskToBack(true); });
                } catch (Exception e) {
                    moveTaskToBack(true);
                }
            }
        });
    }

    @Override
    public void onPause() {
        super.onPause();
        keepAlive();
    }

    @Override
    public void onStop() {
        super.onStop();
        keepAlive();
    }

    // Hudba má hrať ďalej, aj keď je obrazovka zamknutá alebo je appka na pozadí.
    private void keepAlive() {
        try {
            if (MediaService.active && getBridge() != null && getBridge().getWebView() != null) {
                getBridge().getWebView().onResume();
                getBridge().getWebView().resumeTimers();
            }
        } catch (Exception ignored) { }
    }
}

package sk.hudba.app;

import android.Manifest;
import android.os.Build;
import android.os.Bundle;
import androidx.core.app.ActivityCompat;
import com.getcapacitor.BridgeActivity;

public class MainActivity extends BridgeActivity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        registerPlugin(MediaCtlPlugin.class);
        super.onCreate(savedInstanceState);
        if (Build.VERSION.SDK_INT >= 33) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, 1);
        }
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

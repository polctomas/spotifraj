package sk.hudba.app;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadata;
import android.media.session.MediaSession;
import android.media.session.PlaybackState;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Base64;

public class MediaService extends Service {
    public interface Listener { void on(String action, long pos); }

    public static Listener listener;
    public static boolean active = false;
    private static final String CH = "hudba_play";

    private MediaSession ms;
    private PowerManager.WakeLock wl;
    private boolean playing = false;

    @Override
    public IBinder onBind(Intent intent) { return null; }

    @Override
    public void onCreate() {
        super.onCreate();
        active = true;
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel c = new NotificationChannel(CH, "Prehrávanie", NotificationManager.IMPORTANCE_LOW);
            c.setShowBadge(false);
            c.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
            getSystemService(NotificationManager.class).createNotificationChannel(c);
        }
        ms = new MediaSession(this, "hudba");
        ms.setCallback(new MediaSession.Callback() {
            @Override public void onPlay() { send("play", 0); }
            @Override public void onPause() { send("pause", 0); }
            @Override public void onStop() { send("pause", 0); }
            @Override public void onSkipToNext() { send("next", 0); }
            @Override public void onSkipToPrevious() { send("prev", 0); }
            @Override public void onSeekTo(long pos) { send("seek", pos); }
        });
        ms.setActive(true);
        PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
        wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "hudba:play");
    }

    private void send(String a, long p) {
        if (listener != null) listener.on(a, p);
    }

    private PendingIntent act(String a) {
        Intent i = new Intent(this, MediaService.class).setAction(a);
        return PendingIntent.getService(this, a.hashCode(), i,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
    }

    @Override
    public int onStartCommand(Intent i, int flags, int startId) {
        if (i == null) return START_NOT_STICKY;
        String a = i.getAction();
        if (a != null) {
            if (a.equals("act_prev")) send("prev", 0);
            else if (a.equals("act_next")) send("next", 0);
            else if (a.equals("act_pp")) send(playing ? "pause" : "play", 0);
            return START_NOT_STICKY;
        }
        String title = i.getStringExtra("title");
        String artist = i.getStringExtra("artist");
        String art = i.getStringExtra("art");
        playing = i.getBooleanExtra("playing", false);
        long pos = i.getLongExtra("pos", 0);
        long dur = i.getLongExtra("dur", 0);

        Bitmap bm = null;
        if (art != null && art.length() > 0) {
            try {
                byte[] b = Base64.decode(art, Base64.DEFAULT);
                bm = BitmapFactory.decodeByteArray(b, 0, b.length);
            } catch (Exception ignored) { }
        }

        MediaMetadata.Builder mb = new MediaMetadata.Builder()
                .putString(MediaMetadata.METADATA_KEY_TITLE, title)
                .putString(MediaMetadata.METADATA_KEY_ARTIST, artist)
                .putLong(MediaMetadata.METADATA_KEY_DURATION, dur);
        if (bm != null) mb.putBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART, bm);
        ms.setMetadata(mb.build());

        long actions = PlaybackState.ACTION_PLAY | PlaybackState.ACTION_PAUSE
                | PlaybackState.ACTION_PLAY_PAUSE | PlaybackState.ACTION_SKIP_TO_NEXT
                | PlaybackState.ACTION_SKIP_TO_PREVIOUS | PlaybackState.ACTION_SEEK_TO
                | PlaybackState.ACTION_STOP;
        ms.setPlaybackState(new PlaybackState.Builder()
                .setActions(actions)
                .setState(playing ? PlaybackState.STATE_PLAYING : PlaybackState.STATE_PAUSED, pos, playing ? 1f : 0f)
                .build());

        Notification n = buildNotification(title, artist, bm);
        if (Build.VERSION.SDK_INT >= 29) {
            startForeground(1, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK);
        } else {
            startForeground(1, n);
        }
        if (playing) {
            if (!wl.isHeld()) wl.acquire(6 * 60 * 60 * 1000L);
        } else {
            if (wl.isHeld()) wl.release();
            stopForeground(false);
        }
        return START_NOT_STICKY;
    }

    private Notification buildNotification(String t, String a, Bitmap bm) {
        Notification.Builder b = Build.VERSION.SDK_INT >= 26
                ? new Notification.Builder(this, CH) : new Notification.Builder(this);
        Intent open = getPackageManager().getLaunchIntentForPackage(getPackageName());
        PendingIntent pi = PendingIntent.getActivity(this, 0, open,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        b.setSmallIcon(android.R.drawable.ic_media_play)
                .setContentTitle(t)
                .setContentText(a)
                .setContentIntent(pi)
                .setOnlyAlertOnce(true)
                .setVisibility(Notification.VISIBILITY_PUBLIC)
                .setOngoing(playing)
                .addAction(android.R.drawable.ic_media_previous, "Späť", act("act_prev"))
                .addAction(playing ? android.R.drawable.ic_media_pause : android.R.drawable.ic_media_play,
                        playing ? "Pauza" : "Prehrať", act("act_pp"))
                .addAction(android.R.drawable.ic_media_next, "Ďalej", act("act_next"))
                .setStyle(new Notification.MediaStyle()
                        .setMediaSession(ms.getSessionToken())
                        .setShowActionsInCompactView(0, 1, 2));
        if (bm != null) b.setLargeIcon(bm);
        return b.build();
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        stopSelf();
    }

    @Override
    public void onDestroy() {
        active = false;
        try { if (wl != null && wl.isHeld()) wl.release(); } catch (Exception ignored) { }
        try { ms.setActive(false); ms.release(); } catch (Exception ignored) { }
        stopForeground(true);
        super.onDestroy();
    }
}

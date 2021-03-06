package ru.piter.fm.player;

import android.app.Service;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;
import ru.piter.fm.radio.Channel;
import ru.piter.fm.util.Notifications;
import ru.piter.fm.util.RadioUtils;
import ru.piter.fm.util.Settings;
import ru.piter.fm.util.Utils;

import java.io.File;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by IntelliJ IDEA.
 * User: GGobozov
 * Date: 30.08.2010
 * Time: 15:37:13
 * To change this template use File | SettingsActivity | File Templates.
 */
public class PlayerService extends Service implements MediaPlayer.OnCompletionListener, MediaPlayer.OnErrorListener, MediaPlayer.OnPreparedListener {

    private static final int TIME_TO_SEEK = 2000;
    private final IBinder mBinder = new PlayerServiceListener();

    private static MediaPlayer player1;
    private static MediaPlayer player2;
    private static MediaPlayer prepared;
    public static Channel channel;
    private static String track;
    private static String nextTrack;
    public static State state = State.Stopped;
    public static int reconnectCount = 0;

    @Override
    public void onCreate() {
        Utils.clearDirectory(Utils.CHUNKS_DIR);
        player1 = new MediaPlayer();
        player2 = new MediaPlayer();
        player1.setOnCompletionListener(this);
        player1.setOnErrorListener(this);
        player1.setOnPreparedListener(this);
        player2.setOnCompletionListener(this);
        player2.setOnErrorListener(this);
        player2.setOnPreparedListener(this);

    }


    private MediaPlayer getPlayer() {
        if (player1.isPlaying()) return player2;
        return player1;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (player1 != null) {
            player1.release();
            player1 = null;
        }
        if (player2 != null) {
            player2.release();
            player2 = null;
        }
        Utils.clearDirectory(Utils.CHUNKS_DIR);
    }



    public void play(Channel ch) {
        reconnectCount = 0;
        stop();

        // if press on already played channel
        if (channel != null && channel.equals(ch)) {
            channel = null;
            return;
        }

        channel = ch;
        play(RadioUtils.getTrackUrl(channel));
    }

    private void play(String trackUrl) {
        play(trackUrl, TIME_TO_SEEK);
    }




    private void play(final String trackUrl, final int offset) {
        track = trackUrl;
        String trackPath = Utils.CHUNKS_DIR + "/" + RadioUtils.getTrackNameFromUrl(track);

         if (!new File(trackPath).exists()) {
             try {
                 Utils.downloadTrack(track);
                 preparePlayer(track);
                 reconnectCount = 0;
                 Notifications.killNotification(Notifications.CANT_LOAD_TRACK);
             } catch (Exception e) {
                 state = State.Stopped;
                 e.printStackTrace();
                 //show notification only first time
                 if (reconnectCount ==0)
                     Notifications.show(Notifications.CANT_LOAD_TRACK, new Intent());
                 //check reconnect counter
                 if (Settings.isReconnect() && Settings.getReconnectCount() > reconnectCount++ ){
                     Log.d("PiterFM", "Reconnect attemp № " + reconnectCount);
                     Log.d("PiterFM", "Reconnect timeout " + Settings.getReconnectTimeout() + " sec.");
                     Timer timer = new Timer();
                     timer.schedule(new TimerTask() {
                         @Override
                         public void run() {
                             play(trackUrl, offset);
                         }
                     }, Settings.getReconnectTimeout() * 1000); // reconnect timeout in seconds

                 }
                 return;
             }
        }

        MediaPlayer mp = prepared != null ? prepared : getPlayer();
        mp.seekTo(offset);
        mp.start();
        state = State.Playing;


        nextTrack = RadioUtils.getNextTrackUrl(track);
        new DownloadTrackTask().execute(nextTrack);

    }


    private void preparePlayer(String trackUrl) {
        try {
            MediaPlayer p = getPlayer();
            p.reset();
            p.setDataSource(Utils.CHUNKS_DIR + "/" + RadioUtils.getTrackNameFromUrl(trackUrl));
            p.prepare();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private void deleteTrack(final String track) {
        new Thread() {
            public void run() {
                Utils.deletePreviousTrack(track);
            }
        }.start();
    }


    @Override
    public void onPrepared(MediaPlayer mediaPlayer) {
        prepared = mediaPlayer;
    }

    public void onCompletion(MediaPlayer mediaPlayer) {
        mediaPlayer.reset();
        deleteTrack(track);
        play(nextTrack);
    }

    @Override
    public boolean onError(MediaPlayer mediaPlayer, int what, int extra) {
        return false;
    }

    public void stop() {
        if (player1.isPlaying()) player1.stop();
        if (player2.isPlaying()) player2.stop();
        track = null;
        nextTrack = null;
        prepared = null;
        state = State.Stopped;
        Utils.clearDirectory(Utils.CHUNKS_DIR);
    }

    public void pause() {
        player1.pause();
        state = State.Paused;
    }

    public void resume() {
        play(track);
        state = State.Playing;
    }


    public enum State {
        Stopped,
        Preparing,
        Playing,
        Paused
    }


    /**
     * Class for clients to access. Because we know this service always runs in
     */
    public class PlayerServiceListener extends Binder {
        public PlayerService getService() {
            return PlayerService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    public class DownloadTrackTask extends AsyncTask {

        private Exception exception;
        private String url;

        @Override
        public Void doInBackground(Object... objects) {
            url = objects[0].toString();
            try {
                Utils.downloadTrack(url);
            } catch (Exception e) {
                e.printStackTrace();
                exception = e;
            }
            return null;
        }

        @Override
        protected void onPostExecute(Object o) {
            if (exception == null)
                preparePlayer(url);
        }

    }

}

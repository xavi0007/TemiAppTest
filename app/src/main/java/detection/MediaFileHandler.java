package detection;

import android.content.Context;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;

import com.jcraft.jsch.SftpProgressMonitor;

import java.io.File;

public class MediaFileHandler {

    private File mediaFile;
    private MediaPlayer mMediaplayer;
    public static Context mContext;
    private String TAG = "MediaFileHandler";
    public SFTPClient sftpClient;
    /*public String hostname = "192.168.21.236";
    public String username = "robotmanager";
    public String passwd = "robotmanager";
    public int portNumber = 9300;
    public String destinationRootFile = "/home/godzilla/mount/web/html/sftp/NCS";
*/
/*
    public String hostname = "192.168.21.194";
    public String username = "sftpuser";
    public String passwd = "q1w2e3r4";
    public int portNumber = 22;
    public String destinationRootFile = "/data/sftpuser/download";
*/
    public String hostname = "172.27.67.53";
    public String username = "robotmanager";
    public String passwd = "robotmanager";
    public int portNumber = 9300;
    public String destinationRootFile = "/home/godzilla/mount/web/html/sftp/NCS";


    public boolean playAudio = false;

    public MediaFileHandler(Context context) {
        //mediaFile = new File(Environment.getExternalStorageDirectory() + "/" + filename);
        mContext = context;
        mMediaplayer = new MediaPlayer();
    }

    public void setMediaFile(String newFilename) {
        mediaFile = new File(newFilename);
    }

    public void setupSFTPClient(String hostname, String username, int portnumber, String password, String source, String destination) {
        sftpClient = new SFTPClient(username, hostname, portnumber, password, source, destination);
        sftpClient.setUploadFile(false);
        sftpClient.setSftpProgressMonitor(new SftpProgressMonitor() {
            @Override
            public void init(int op, String src, String dest, long max) {
                System.out.println("INIT");
            }

            @Override
            public boolean count(long count) {
                return true;
            }

            @Override
            public void end() {
                Log.i(TAG, "end is called");
                Handler handler = new Handler();
                handler.post(new Runnable() {
                    @Override
                    public void run() {

                    }
                });
            }
        });
    }

    public void downloadMediaFile(File filename) {
        Log.i(TAG, "Downloading media file...");
        setupSFTPClient(hostname, username, portNumber, passwd, filename.getAbsolutePath(), Environment.getExternalStorageDirectory() + "/" + filename.getName());
        sftpClient.setSftpProgressMonitor(new SftpProgressMonitor() {
            @Override
            public void init(int op, String src, String dest, long max) {
                System.out.println("INIT");
            }

            @Override
            public boolean count(long count) {
                Log.i("Downloading", "Downloading file. Progress: " + count);
                return true;
            }

            @Override
            public void end() {
                Log.i(TAG, "end is called");
                playAudio = true;
            }
        });
        sftpClient.execute();
    }

    // play a new file, change the file instance
    public synchronized void audioBroadcast(File filename) {
        // open the desired file
        Log.i(TAG, "The file name is: " + Environment.getExternalStorageDirectory() + "/" + filename.getName());
        setMediaFile(Environment.getExternalStorageDirectory() + "/" + filename.getName());
        if (mediaFile.exists()) {
            if (mMediaplayer.isPlaying())
                mMediaplayer.stop(); // stops song to prevent overlapping sound
            // play music
            Uri uri = Uri.fromFile(mediaFile);
            mMediaplayer = MediaPlayer.create(mContext, uri);
            mMediaplayer.start();
        } else {
            // download file from sftp server
            Log.i(TAG, "There is no mp3 file! Downloading the file...");
            Log.i(TAG, "Download from " + filename.getAbsolutePath() + " to " + Environment.getExternalStorageDirectory() + "/" + filename.getName());
            downloadMediaFile(filename);
            while (playAudio == false) ;
            Log.i(TAG, "Download complete!");
            audioBroadcast();
            //Log.i(TAG, "Download Complete! Playing...");
//            timeOut();
        }
    }

    // set as looping
    public synchronized void loopAudioBroadcast() {
        if (mMediaplayer != null) {
            if (!mMediaplayer.isLooping()) {
                mMediaplayer.setLooping(true);
            }
        }
    }

    // stops looping
    public synchronized void StopLoopAudioBroadcast() {
        if (mMediaplayer != null) {
            if (mMediaplayer.isLooping()) {
                mMediaplayer.setLooping(false);
            }
        }
    }

    public synchronized void stop() {
        if (mMediaplayer != null) {
            if (mMediaplayer.isPlaying()) {
                mMediaplayer.stop();
            }
        }
    }

    // play the same audio file again
    public synchronized void audioBroadcast() {
        if (mediaFile.exists()) {
            Log.i(TAG, "Download complete, playing file...");
            if (mMediaplayer.isPlaying())
                mMediaplayer.stop(); // stops song to prevent overlapping sound
            // play music
            Uri uri = Uri.fromFile(mediaFile);
            mMediaplayer = MediaPlayer.create(mContext, uri);
            playAudio = true;
            mMediaplayer.start();
        } else {
            // download file from sftp server
            Log.i(TAG, "There is no mp3 file! Download fail!");
            //downloadMediaFile();
        }
        //wait for a while to reset the varaible
        new Handler().postDelayed(new Thread(new Runnable() {
            @Override
            public void run() {
                playAudio = false;
            }
        }), 500);
    }
}

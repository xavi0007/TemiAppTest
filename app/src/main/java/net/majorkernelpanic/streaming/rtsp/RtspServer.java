/*
 * Copyright (C) 2011-2015 GUIGUI Simon, fyhertz@gmail.com
 *
 * This file is part of libstreaming (https://github.com/fyhertz/libstreaming)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.majorkernelpanic.streaming.rtsp;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.res.AssetManager;
import android.hardware.Camera;
import android.os.Binder;
import android.os.Environment;
import android.os.IBinder;
import android.util.Base64;
import android.util.Log;
import android.util.Size;

import com.ncs.rtspstream.App;
import com.ncs.rtspstream.MainActivity;

import net.majorkernelpanic.streaming.Session;
import net.majorkernelpanic.streaming.SessionBuilder;

import org.jcodec.api.SequenceEncoder;
import org.jcodec.api.android.AndroidSequenceEncoder;
import org.jcodec.common.io.FileChannelWrapper;
import org.jcodec.common.io.NIOUtils;
import org.jcodec.common.model.Rational;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.BindException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Locale;
import java.util.WeakHashMap;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import detection.tflite.Classifier;
import detection.tflite.TFLiteObjectDetectionAPIModel;

import static com.ncs.rtspstream.App.app;
import static com.ncs.rtspstream.App.callback;
import static com.ncs.rtspstream.MainActivity.executorService;
import static com.ncs.rtspstream.MainActivity.handler;
import static com.ncs.rtspstream.MainActivity.mCamera;

/**
 * Implementation of a subset of the RTSP protocol (RFC 2326).
 *
 * It allows remote control of an android device cameras & microphone.
 * For each connected client, a Session is instantiated.
 * The Session will start or stop streams according to what the client wants.
 *
 */
public class RtspServer extends Service {

    public final static String TAG = "RtspServer";

    /** The server name that will appear in responses. */
    public static String SERVER_NAME = "NCS";

    /** Port used by default. */
    public static final int DEFAULT_RTSP_PORT = 8086;

    /** Port already in use. */
    public final static int ERROR_BIND_FAILED = 0x00;

    /** A stream could not be started. */
    public final static int ERROR_START_FAILED = 0x01;

    /** Streaming started. */
    public final static int MESSAGE_STREAMING_STARTED = 0X00;

    /** Streaming stopped. */
    public final static int MESSAGE_STREAMING_STOPPED = 0X01;

    /** Key used in the SharedPreferences to store whether the RTSP server is enabled or not. */
    public final static String KEY_ENABLED = "rtsp_enabled";

    /** Key used in the SharedPreferences for the port used by the RTSP server. */
    public final static String KEY_PORT = "rtsp_port";

    protected SessionBuilder mSessionBuilder;
    protected SharedPreferences mSharedPreferences;
    protected boolean mEnabled = true;
    protected int mPort = DEFAULT_RTSP_PORT;
    protected WeakHashMap<Session,Object> mSessions = new WeakHashMap<>(2);

    private RequestListener mListenerThread;
    private final IBinder mBinder = new LocalBinder();
    private boolean mRestart = false;
    private final LinkedList<CallbackListener> mListeners = new LinkedList<>();

    /** Credentials for Basic Auth */
    private String mUsername;
    private String mPassword;


    public RtspServer() {
    }

    /** Be careful: those callbacks won't necessarily be called from the ui thread ! */
    public interface CallbackListener {

        /** Called when an error occurs. */
        void onError(RtspServer server, Exception e, int error);

        /** Called when streaming starts/stops. */
        void onMessage(RtspServer server, int message);

    }

    /**
     * See {@link RtspServer.CallbackListener} to check out what events will be fired once you set up a listener.
     * @param listener The listener
     */
    public void addCallbackListener(CallbackListener listener) {
        synchronized (mListeners) {
            if (!mListeners.isEmpty()) {
                for (CallbackListener cl : mListeners) {
                    if (cl == listener) return;
                }
            }
            mListeners.add(listener);
        }
    }

    /**
     * Removes the listener.
     * @param listener The listener
     */
    public void removeCallbackListener(CallbackListener listener) {
        synchronized (mListeners) {
            mListeners.remove(listener);
        }
    }

    /** Returns the port used by the RTSP server. */
    public int getPort() {
        return mPort;
    }

    /**
     * Sets the port for the RTSP server to use.
     * @param port The port
     */
    public void setPort(int port) {
        Editor editor = mSharedPreferences.edit();
        editor.putString(KEY_PORT, String.valueOf(port));
        editor.commit();
    }

    /**
     * Set Basic authorization to access RTSP Stream
     * @param username username
     * @param password password
     */
    public void setAuthorization(String username, String password)
    {
        mUsername = username;
        mPassword = password;
    }

    /**
     * Starts (or restart if needed, if for example the configuration
     * of the server has been modified) the RTSP server.
     */
    public void start() {
        if (!mEnabled || mRestart) stop();
        if (mEnabled && mListenerThread == null) {
            try {
                mListenerThread = new RequestListener();
            } catch (Exception e) {
                mListenerThread = null;
            }
        }
        mRestart = false;
    }

    /**
     * Stops the RTSP server but not the Android Service.
     * To stop the Android Service you need to call {@link android.content.Context#stopService(Intent)};
     */
    public void stop() {
        if (mListenerThread != null) {
            try {
                mListenerThread.kill();
                for ( Session session : mSessions.keySet() ) {
                    if ( session != null && session.isStreaming() ) {
                        session.stop();
                    }
                }
                mCamera.setPreviewCallback(null);
                mCamera.setPreviewCallback(callback);
            } catch (Exception e) {
            } finally {
                mListenerThread = null;
            }
        }

    }

    /** Returns whether or not the RTSP server is streaming to some client(s). */
    public boolean isStreaming() {
        for ( Session session : mSessions.keySet() ) {
            if ( session != null && session.isStreaming() ) {
                Log.i(TAG, "Streaming!");
                return true;
            }
        }
        return false;
    }

    public boolean isEnabled() {
        return mEnabled;
    }

    /** Returns the bandwidth consumed by the RTSP server in bits per second. */
    public long getBitrate() {
        long bitrate = 0;
        for ( Session session : mSessions.keySet() ) {
            if ( session != null && session.isStreaming() ) {
                bitrate += session.getBitrate();
            }
        }
        return bitrate;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public void onCreate() {

        // Let's restore the state of the service
        mSharedPreferences = getSharedPreferences("robotConfigFile",MODE_PRIVATE);
        mPort = Integer.parseInt(mSharedPreferences.getString(KEY_PORT, String.valueOf(mPort)));
        mEnabled = mSharedPreferences.getBoolean(KEY_ENABLED, mEnabled);

        // If the configuration is modified, the server will adjust
        mSharedPreferences.registerOnSharedPreferenceChangeListener(mOnSharedPreferenceChangeListener);

        start();
    }

    @Override
    public void onDestroy() {
//		stop();
//		Camera camera = mSessionBuilder.getCameraObject();
//		camera.release();
        stop();
        mSharedPreferences.unregisterOnSharedPreferenceChangeListener(mOnSharedPreferenceChangeListener);
    }

    private OnSharedPreferenceChangeListener mOnSharedPreferenceChangeListener = new OnSharedPreferenceChangeListener() {
        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {

            if (key.equals(KEY_PORT)) {
                int port = Integer.parseInt(sharedPreferences.getString(KEY_PORT, String.valueOf(mPort)));
                if (port != mPort) {
                    mPort = port;
                    mRestart = true;
                    start();
                }
            }
            else if (key.equals(KEY_ENABLED)) {
                mEnabled = sharedPreferences.getBoolean(KEY_ENABLED, mEnabled);
                start();
            }
            // can we add the changing of bitrate, resolution etc. here as shared preferences?
        }
    };

    /** The Binder you obtain when a connection with the Service is established. */
    public class LocalBinder extends Binder {
        public RtspServer getService() {
            return RtspServer.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    protected void postMessage(int id) {
        synchronized (mListeners) {
            if (!mListeners.isEmpty()) {
                for (CallbackListener cl : mListeners) {
                    cl.onMessage(this, id);
                }
            }
        }
    }

    protected void postError(Exception exception, int id) {
        synchronized (mListeners) {
            if (!mListeners.isEmpty()) {
                for (CallbackListener cl : mListeners) {
                    cl.onError(this, exception, id);
                }
            }
        }
    }

    /**
     * By default the RTSP uses {@link UriParser} to parse the URI requested by the client
     * but you can change that behavior by override this method.
     * @param uri The uri that the client has requested
     * @param client The socket associated to the client
     * @return A proper session
     */
    protected Session handleRequest(String uri, Socket client) throws IllegalStateException, IOException {
        Session session = UriParser.parse(uri);
        session.setOrigin(client.getLocalAddress().getHostAddress());
        if (session.getDestination()==null) {
            session.setDestination(client.getInetAddress().getHostAddress());
        }
        return session;
    }

    public static boolean isStopRecord() {
        return stopRecord;
    }

    public static void setStopRecord(boolean stopRecord) {
        RtspServer.stopRecord = stopRecord;
    }

    private static boolean stopRecord = false;


    class RequestListener extends Thread implements Runnable {

        private final ServerSocket mServer;
//		private final ServerSocket  mServer2;

        public RequestListener() throws IOException {
            try {
                mServer = new ServerSocket(mPort);
//				mServer2 = new ServerSocket(mPort + 1);
                start();
            } catch (BindException e) {
                Log.e(TAG,"Port already in use !");
                postError(e, ERROR_BIND_FAILED);
                throw e;
            }
        }

        public void run() {
            Log.i(TAG,"RTSP server listening on port "+mServer.getLocalPort());
            while (!Thread.interrupted()) {
                try {
                    new WorkerThread(mServer.accept()).start();
//					new WorkerThread(mServer2.accept()).start();
                    Log.i("RtspServer", String.valueOf(getBitrate()));
                } catch (SocketException e) {
                    break;
                } catch (IOException e) {
                    Log.e(TAG,e.getMessage());
                    continue;
                }
            }
            Log.i(TAG,"RTSP server stopped !");
        }

        public void kill() {
            try {
                mServer.close();
//				mServer2.close();
            } catch (IOException e) {}
            try {
                this.join();
            } catch (InterruptedException ignore) {}
        }

    }

    // One thread per client
    class WorkerThread extends Thread implements Runnable {

        private final Socket mClient;
        private final OutputStream mOutput;
        private final BufferedReader mInput;
		/*private VideoStream mVideoStream;
		private AudioStream mAudioStream;
		private MediaRecorder mMediaRecorder;*/

        // Each client has an associated session
        private Session mSession;

        public WorkerThread(final Socket client) throws IOException {
            mInput = new BufferedReader(new InputStreamReader(client.getInputStream()));
            mOutput = client.getOutputStream();
            mClient = client;
            mSession = new Session();
        }

        public void run() {
            Request request;
            Response response;

            Log.i(TAG, "Connection from "+mClient.getInetAddress().getHostAddress());

			/*mVideoStream = mSession.getVideoTrack();
			mAudioStream = mSession.getAudioTrack();

			mMediaRecorder.set(mVideoStream);
			mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
			mMediaRecorder.setVideoEncoder(0);
			mMediaRecorder.setVideoSize(640, 480);
			mMediaRecorder.setVideoFrameRate(10);
			mMediaRecorder.setVideoEncodingBitRate((int)(1000000*0.8));
			mMediaRecorder.setOutputFile(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)+ "/video.mp4");
			mMediaRecorder.setMaxDuration(50000); //50 seconds
			mMediaRecorder.setMaxFileSize(5000000); // approximately 5MB

			mMediaRecorder.prepare();
			mMediaRecorder.start();
			*/
            while (!Thread.interrupted() && mCamera != null) {

                request = null;
                response = null;

                // Parse the request
                try {
                    request = Request.parseRequest(mInput);
                } catch (SocketException e) {
                    // Client has left
                    break;
                } catch (Exception e) {
                    // We don't understand the request :/
                    response = new Response();
                    response.status = Response.STATUS_BAD_REQUEST;
                }

                // Do something accordingly like starting the streams, sending a session description
                if (request != null) {
                    try {
                        response = processRequest(request);
                    }
                    catch (Exception e) {
                        // This alerts the main thread that something has gone wrong in this thread
                        postError(e, ERROR_START_FAILED);
                        Log.e(TAG,e.getMessage()!=null?e.getMessage():"An error occurred");
                        e.printStackTrace();
                        response = new Response(request);
                    }
                }

                // We always send a response
                // The client will receive an "INTERNAL SERVER ERROR" if an exception has been thrown at some point
                try {
                    response.send(mOutput);

                } catch (IOException e) {
                    Log.e(TAG,"Response was not sent properly");
                    break;
                }

            }

            // Streaming stops when client disconnects
            boolean streaming = isStreaming();
            mSession.syncStop();
            if (streaming && !isStreaming()) {
                postMessage(MESSAGE_STREAMING_STOPPED);
            }
            mSession.release();

            try {
                mClient.close();
            } catch (IOException ignore) {}

            Log.i(TAG, "Client disconnected");

        }

        public Response processRequest(Request request) throws IllegalStateException, IOException {
            Response response = new Response(request);

            //Ask for authorization unless this is an OPTIONS request
            if(!isAuthorized(request) && !request.method.equalsIgnoreCase("OPTIONS"))
            {
                response.attributes = "WWW-Authenticate: Basic realm=\""+SERVER_NAME+"\"\r\n";
                response.status = Response.STATUS_UNAUTHORIZED;
            }
            else
            {
                /* ********************************************************************************** */
                /* ********************************* Method DESCRIBE ******************************** */
                /* ********************************************************************************** */
                if (request.method.equalsIgnoreCase("DESCRIBE")) {

                    try {
                        // before handling the request, let's reset the camera preview and callbacks
                        if (MainActivity.mCamera != null) {
                            MainActivity.mCamera.stopPreview();
                            MainActivity.mCamera.setPreviewCallback(null);
                            executorService.shutdown();
                            executorService = Executors.newSingleThreadExecutor();
                        }

                        // Parse the requested URI and configure the session
                        mSession = handleRequest(request.uri, mClient);
                        mSessions.put(mSession, null);
                        //mSession.release();
                        mSession.syncConfigure();

                        String requestContent = mSession.getSessionDescription();
                        String requestAttributes =
                                "Content-Base: " + mClient.getLocalAddress().getHostAddress() + ":" + mClient.getLocalPort() + "/\r\n" +
                                        "Content-Type: application/sdp\r\n";

                        response.attributes = requestAttributes;
                        response.content = requestContent;

                        // If no exception has been thrown, we reply with OK
                        response.status = Response.STATUS_OK;
                    }catch (Exception e){
                        e.printStackTrace();
                        response.status = Response.STATUS_INTERNAL_SERVER_ERROR;
                    }

                }

                /* ********************************************************************************** */
                /* ********************************* Method OPTIONS ********************************* */
                /* ********************************************************************************** */
                else if (request.method.equalsIgnoreCase("OPTIONS")) {
                    response.status = Response.STATUS_OK;
                    response.attributes = "Public: DESCRIBE,SETUP,TEARDOWN,PLAY,PAUSE\r\n";
                    response.status = Response.STATUS_OK;
                }

                /* ********************************************************************************** */
                /* ********************************** Method SETUP ********************************** */
                /* ********************************************************************************** */
                else if (request.method.equalsIgnoreCase("SETUP")) {
                    Pattern p;
                    Matcher m;
                    int p2, p1, ssrc, trackId, src[];
                    String destination;

                    p = Pattern.compile("trackID=(\\w+)", Pattern.CASE_INSENSITIVE);
                    m = p.matcher(request.uri);

                    if (!m.find()) {
                        response.status = Response.STATUS_BAD_REQUEST;
                        return response;
                    }

                    trackId = Integer.parseInt(m.group(1));

                    if (!mSession.trackExists(trackId)) {
                        response.status = Response.STATUS_NOT_FOUND;
                        return response;
                    }

                    p = Pattern.compile("client_port=(\\d+)(?:-(\\d+))?", Pattern.CASE_INSENSITIVE);
                    m = p.matcher(request.headers.get("transport"));

                    if (!m.find()) {
                        int[] ports = mSession.getTrack(trackId).getDestinationPorts();
                        p1 = ports[0];
                        p2 = ports[1];
                    } else {
                        p1 = Integer.parseInt(m.group(1));
                        if (m.group(2) == null) {
                            p2 = p1+1;
                        } else {
                            p2 = Integer.parseInt(m.group(2));
                        }
                    }

                    ssrc = mSession.getTrack(trackId).getSSRC();
                    src = mSession.getTrack(trackId).getLocalPorts();
                    destination = mSession.getDestination();

                    mSession.getTrack(trackId).setDestinationPorts(p1, p2);

                    boolean streaming = isStreaming();
                    mSession.syncStart(trackId);
                    //record here
                    //mSession.saveSegment(false);
                    if (!streaming && isStreaming()) {
                        postMessage(MESSAGE_STREAMING_STARTED);
                    }

                    response.attributes = "Transport: RTP/AVP/UDP;" + (InetAddress.getByName(destination).isMulticastAddress() ? "multicast" : "unicast") +
                            ";destination=" + mSession.getDestination() +
                            ";client_port=" + p1 + "-" + p2 +
                            ";server_port=" + src[0] + "-" + src[1] +
                            ";ssrc=" + Integer.toHexString(ssrc) +
                            ";mode=play\r\n" +
                            "Session: " + "1185d20035702ca" + "\r\n" +
                            "Cache-Control: no-cache\r\n";
                    response.status = Response.STATUS_OK;

                    // If no exception has been thrown, we reply with OK
                    response.status = Response.STATUS_OK;

                }

                /* ********************************************************************************** */
                /* ********************************** Method PLAY *********************************** */
                /* ********************************************************************************** */
                else if (request.method.equalsIgnoreCase("PLAY")) {
                    String requestAttributes = "RTP-Info: ";
                    if (mSession.trackExists(0))
                        requestAttributes += "url=rtsp://" + mClient.getLocalAddress().getHostAddress() + ":" + mClient.getLocalPort() + "/trackID=" + 0 + ";seq=0,";
                    if (mSession.trackExists(1))
                        requestAttributes += "url=rtsp://" + mClient.getLocalAddress().getHostAddress() + ":" + mClient.getLocalPort() + "/trackID=" + 1 + ";seq=0,";
                    requestAttributes = requestAttributes.substring(0, requestAttributes.length()- 1) + "\r\nSession: 1185d20035702ca\r\n";

                    response.attributes = requestAttributes;
                    //Log.i(TAG, response.attributes);

                    // If no exception has been thrown, we reply with OK
                    response.status = Response.STATUS_OK;

                }

                /* ********************************************************************************** */
                /* ********************************** Method PAUSE ********************************** */
                /* ********************************************************************************** */
                else if (request.method.equalsIgnoreCase("PAUSE")) {
                    response.status = Response.STATUS_OK;
                }

                /* ********************************************************************************** */
                /* ********************************* Method TEARDOWN ******************************** */
                /* ********************************************************************************** */
                else if (request.method.equalsIgnoreCase("TEARDOWN")) {
                    //mSession.saveSegment(true);
//					stopRecord = true;
//					RtspServer.this.stop();

                    // open camera, create preview , then pass into sessionBuilder
                    // *******NOTE: above comment seems to be wrong, open camera, set preview callback, start preview, then configure session builder
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {

//							sendBroadcast(new Intent("RTSP TEARDOWN"));
                            if (MainActivity.mCamera == null)
                                MainActivity.mCamera = Camera.open(0);
                            try {
                                executorService.shutdownNow();
                                while (!executorService.isShutdown());
                                executorService = Executors.newSingleThreadExecutor();
                                MainActivity.mCamera.stopPreview();
                                MainActivity.mCamera.setPreviewCallback(null);
                                MainActivity.mCamera.setPreviewDisplay(SessionBuilder.getInstance().getSurfaceView().getHolder());
                                MainActivity.mCamera.setPreviewCallback(App.app.callback);
                                MainActivity.mCamera.startPreview();
                            }catch (Exception e){
                                e.printStackTrace();
                            }
                        }
                    }, 1000);
                    response.status = Response.STATUS_OK;
                    // stitch video here+ "and" + mServer2.getLocalPort()
//					FileChannelWrapper outputFile = NIOUtils.writableFileChannel(Environment.getExternalStorageDirectory().getAbsolutePath() + "/movie.mp4");
                }

                /*
                 * Self implemented
                 *
                 * Method ANNOUNCE
                 *
                 */
                /*else if (request.method.equalsIgnoreCase("ANNOUNCE")) {
					//response.status = Response.STATUS_OK; // sent S -> C
					mSession.setDestination("/test/test2");
					response.attributes = mSession.getSessionDescription();
					// manually write ANNOUNCE protocol

					Log.d("ANNOUNCE", response.attributes);
				}*/

                /* ********************************************************************************** */
                /* ********************************* Unknown method ? ******************************* */
                /* ********************************************************************************** */
                else {
                    Log.e(TAG, "Command unknown: " + request);
                    response.status = Response.STATUS_BAD_REQUEST;
                }
            }
            return response;

        }
        /**
         * Check if the request is authorized
         * @param request
         * @return true or false
         */
        private boolean isAuthorized(Request request)
        {
            String auth = request.headers.get("authorization");
            if(mUsername == null || mPassword == null || mUsername.isEmpty())
                return true;

            if(auth != null && !auth.isEmpty())
            {
                String received = auth.substring(auth.lastIndexOf(" ")+1);
                String local = mUsername+":"+mPassword;
                String localEncoded = Base64.encodeToString(local.getBytes(),Base64.NO_WRAP);
                if(localEncoded.equals(received))
                    return true;
            }

            return false;
        }
    }

    static class Request {

        // Parse method & uri
        public static final Pattern regexMethod = Pattern.compile("(\\w+) (\\S+) RTSP",Pattern.CASE_INSENSITIVE);
        // Parse a request header
        public static final Pattern rexegHeader = Pattern.compile("(\\S+):(.+)",Pattern.CASE_INSENSITIVE);

        public String method;
        public String uri;
        public HashMap<String,String> headers = new HashMap<>();

        /** Parse the method, uri & headers of a RTSP request */
        public static Request parseRequest(BufferedReader input) throws IOException, IllegalStateException, SocketException {
            Request request = new Request();
            String line;
            Matcher matcher;

            // Parsing request method & uri
            if ((line = input.readLine())==null) throw new SocketException("Client disconnected");
            matcher = regexMethod.matcher(line);
            matcher.find();
            request.method = matcher.group(1);
            request.uri = matcher.group(2);

            // Parsing headers of the request
            while ( (line = input.readLine()) != null && line.length()>3 ) {
                matcher = rexegHeader.matcher(line);
                matcher.find();
                request.headers.put(matcher.group(1).toLowerCase(Locale.US),matcher.group(2));
            }
			/*if(request.method.equalsIgnoreCase("SETUP")){
				request.uri = "rtsp://192.168.21.186:1234/";
			}*/
            if (line==null) throw new SocketException("Client disconnected");

            // It's not an error, it's just easier to follow what's happening in logcat with the request in red
            Log.e(TAG,request.method+" "+request.uri);

            return request;
        }
    }

    static class Response {

        // Status code definitions
        public static final String STATUS_OK = "200 OK";
        public static final String STATUS_BAD_REQUEST = "400 Bad Request";
        public static final String STATUS_UNAUTHORIZED = "401 Unauthorized";
        public static final String STATUS_NOT_FOUND = "404 Not Found";
        public static final String STATUS_INTERNAL_SERVER_ERROR = "500 Internal Server Error";

        public String status = STATUS_INTERNAL_SERVER_ERROR;
        public String content = "";
        public String attributes = "";

        private final Request mRequest;

        public Response(Request request) {
            this.mRequest = request;
        }

        public Response() {
            // Be carefull if you modify the send() method because request might be null !
            mRequest = null;
        }

        public void send(OutputStream output) throws IOException {
            int seqid = -1;

            try {
                seqid = Integer.parseInt(mRequest.headers.get("cseq").replace(" ",""));
            } catch (Exception e) {
                Log.e(TAG,"Error parsing CSeq: "+(e.getMessage()!=null?e.getMessage():""));
            }

            String response = 	"RTSP/1.0 "+status+"\r\n" +
                    "Server: "+SERVER_NAME+"\r\n" +
                    (seqid>=0?("Cseq: " + seqid + "\r\n"):"") +
                    "Content-Length: " + content.length() + "\r\n" +
                    attributes +
                    "\r\n" +
                    content;

            Log.d(TAG,response.replace("\r", ""));

            output.write(response.getBytes());
        }
    }

}

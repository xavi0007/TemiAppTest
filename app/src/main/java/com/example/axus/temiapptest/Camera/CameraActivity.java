package com.example.axus.temiapptest.Camera;

import android.app.Activity;
import android.content.res.AssetManager;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Handler;
import android.graphics.Bitmap;
import android.os.SystemClock;
import android.util.Log;
import android.util.Size;
import android.view.View;
import android.widget.FrameLayout;

import com.example.axus.temiapptest.App;
import com.example.axus.temiapptest.R;

import net.majorkernelpanic.streaming.video.ImageUtils;
import net.majorkernelpanic.streaming.video.Point;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import detection.tflite.Classifier;

import static net.majorkernelpanic.streaming.video.VideoStream.executorService;

public class CameraActivity extends Activity {

    //Singelton
    private static volatile CameraActivity cameraActivity;

    // ---------------------------VA functions-----------------------------------------------------
    public enum DetectorMode {
        TF_OD_API;
    }
    public Runnable imageConverter;
    public Bitmap image;
    public Classifier detector;

    // Configuration values for the prepackaged SSD model.
    public static final int TF_OD_API_INPUT_SIZE = 300;
    public static final boolean TF_OD_API_IS_QUANTIZED = true;
    public static final String TF_OD_API_MODEL_FILE = "detect.tflite";
    public static final String TF_OD_API_LABELS_FILE = "file:///android_asset/labelmap.txt";
    private static final CameraActivity.DetectorMode MODE = CameraActivity.DetectorMode.TF_OD_API;
    // Minimum detection confidence to track a detection.
    public static float MINIMUM_CONFIDENCE_TF_OD_API = 0.5f;
    private static final boolean MAINTAIN_ASPECT = false;
    private static final Size DESIRED_PREVIEW_SIZE = new Size(640, 480);
    private static final boolean SAVE_PREVIEW_BITMAP = false;
    private static final float TEXT_SIZE_DIP = 10;

    public int cropSize = 300;
    public AssetManager assetManager;
    public int previewHeight;
    public int previewWidth;
    private int[] rgbBytes;

    public boolean readyForNextImage = true;
    //    public static Camera mCamera;
    public static Camera.PreviewCallback callback;

    private long timestamp = 0, lastProcessingTimeMs;
    private boolean computingDetection = false;
    public Bitmap croppedBitmap, cropCopyBitmap;

    private Matrix frameToCropTransform;
    private Matrix cropToFrameTransform;
    public int nhuman =0;
    public String confidence = "";
    public int imageVideoNumber = 0;

    private static final String TAG = "camera";
    private Camera mCamera;
    private CameraPreview mPreview;


    public static Handler handler;

    //Used by VideoStream, why is it here?
    public static boolean cameraRelease = false;



    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.camera);

        // Create an instance of Camera
        mCamera = getCameraInstance();

        // Create our Preview view and set it as the content of our activity.
        mPreview = new CameraPreview(this, mCamera);
        FrameLayout preview = (FrameLayout) findViewById(R.id.camera_preview);
        preview.addView(mPreview);

        callback = new Camera.PreviewCallback() {
            @Override
            public void onPreviewFrame(final byte[] data,final Camera camera) {
                if (mCamera != null) {
                    for (int i = 0; i < 10; i++)
                        mCamera.addCallbackBuffer(data);
                    if (futures.isDone() && mCamera != null) {
                        try {
                            futures = executorService.submit(new Runnable() {
                                @Override
                                public void run() {
                                    if (readyForNextImage) {
                                        updateRgbBytes(data, camera);
                                        App.getInstance().sendAlertAlgo();
                                    }
                                }
                            });
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        };
    }

    public void stopCameraPreview(){
        setCamera(null);
        if (mCamera != null) {
            mCamera.release();
            mCamera = null;
        }
    }
    public void setCamera(Camera camera) {
        if (mCamera == camera) { return; }
    }

    public static Camera getCameraInstance(){
        Camera c = null;
        try {
            c = Camera.open(); // attempt to get a Camera instance
        }
        catch (Exception e){
            // Camera is not available (in use or does not exist)
        }
        return c; // returns null if camera is unavailable
    }

    public void updateRgbBytes(final byte[] data, Camera camera) {
        imageConverter =
                new Runnable() {
                    @Override
                    public void run() {
                        ImageUtils.convertYUV420SPToARGB8888(
                                data,
                                previewWidth,
                                previewHeight,
                                rgbBytes);
                        image = Bitmap.createBitmap(previewWidth, previewHeight, Bitmap.Config.ARGB_8888);
                        image.setPixels(rgbBytes, 0, previewWidth, 0, 0, previewWidth, previewHeight);
//                        ImageUtils.saveBitmapForVideo(image);

                    }
                };
        rgbBytes = new int[previewWidth * previewHeight];
        imageConverter.run();
    }


    public void finish(View view){
        finish();
    }

    // detect face
    public boolean runDetection()
    {
        nhuman = 0;
        confidence = "Confidence:";
        boolean detectHuman = false;
        if (readyForNextImage == false){
            Log.i(TAG, "Skipping frame");
            return false;
        }
        croppedBitmap = Bitmap.createBitmap(cropSize, cropSize, Bitmap.Config.ARGB_8888);
        ++timestamp;
        long currTimestamp = timestamp;

        frameToCropTransform =
                ImageUtils.getTransformationMatrix(
                        previewWidth, previewHeight,
                        cropSize, cropSize,
                        0, MAINTAIN_ASPECT);
        cropToFrameTransform = new Matrix();
        frameToCropTransform.invert(cropToFrameTransform);

        // No mutex needed as this method is not reentrant.
        if (computingDetection) {
            //readyForNextImage(); // just add callback buffer and toggle an isprocessingframe
            readyForNextImage = false;
            return false;
        }

        // ready to compute detect face
        computingDetection = true;
        Log.i(TAG, "Preparing image " + currTimestamp + " for detection in bg thread.");

        image.setPixels(rgbBytes, 0, previewWidth, 0, 0, previewWidth, previewHeight);

        //readyForNextImage(); // just add callback buffer and toggle an isprocessingframe

        Canvas canvas = new Canvas(croppedBitmap);
        canvas.drawBitmap(image, frameToCropTransform, null);
        // For examining the actual TF input.
        if (SAVE_PREVIEW_BITMAP) {
            ImageUtils.saveBitmap(croppedBitmap);
        }

        Log.i(TAG, "Running detection on image " + System.nanoTime() / 1000);
        long startTime = SystemClock.uptimeMillis();
        List<Classifier.Recognition> results = detector.recognizeImage(croppedBitmap);
        lastProcessingTimeMs = SystemClock.uptimeMillis() - startTime;

        cropCopyBitmap = Bitmap.createBitmap(croppedBitmap);
        Canvas cropBitmapCanvas = new Canvas(cropCopyBitmap);
        Paint paint = new Paint();
        paint.setColor(Color.RED);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(2.0f);

        float minimumConfidence = MINIMUM_CONFIDENCE_TF_OD_API;

        List<Classifier.Recognition> mappedRecognitions =
                new LinkedList<Classifier.Recognition>();

        for ( Classifier.Recognition result : results) {
            RectF location = result.getLocation();
            if (result.getTitle().equals("person") && location != null && result.getConfidence() >= minimumConfidence) {
                cropBitmapCanvas.drawRect(location, paint);

                cropToFrameTransform.mapRect(location);

                result.setLocation(location);
                mappedRecognitions.add(result);
                App.getInstance().setFaceDetected(true);
                detectHuman = true;
                nhuman = nhuman + 1;
                confidence = confidence + " " + result.getConfidence().toString() + "%";
//				break; // added to improve performance
            }
        }
        Log.i("TF detection", confidence + ", " + String.valueOf(minimumConfidence));
        readyForNextImage = true;
        computingDetection = false;
//        image = null;
//        rgbBytes = null;
//        croppedBitmap = null;
        if (detectHuman == true) {
            //sendNotification();
            App app = App.getInstance();
            app.currentDetectPoint = new Point(app.getDisplayPosition(0),app.getDisplayPosition(1),app.getDisplayPosition(2));
//            cropCopyBitmap = null;
            return true;
        }
        return false;
    }


    private Future futures = new Future() {
        @Override
        public boolean cancel(boolean b) {
            return false;
        }

        @Override
        public boolean isCancelled() {
            return false;
        }

        @Override
        public boolean isDone() {
            return true;
        }

        @Override
        public Object get() throws ExecutionException, InterruptedException {
            return null;
        }

        @Override
        public Object get(long l, TimeUnit timeUnit) throws ExecutionException, InterruptedException, TimeoutException {
            return null;
        }
    };

    //get set


    public Bitmap getCroppedBitmap() {
        return croppedBitmap;
    }

    public void setCroppedBitmap(Bitmap croppedBitmap) {
        this.croppedBitmap = croppedBitmap;
    }

    public Bitmap getCropCopyBitmap() {
        return cropCopyBitmap;
    }

    public void setCropCopyBitmap(Bitmap cropCopyBitmap) {
        this.cropCopyBitmap = cropCopyBitmap;
    }

    public Matrix getFrameToCropTransform() {
        return frameToCropTransform;
    }

    public void setFrameToCropTransform(Matrix frameToCropTransform) {
        this.frameToCropTransform = frameToCropTransform;
    }

    public Matrix getCropToFrameTransform() {
        return cropToFrameTransform;
    }

    public void setCropToFrameTransform(Matrix cropToFrameTransform) {
        this.cropToFrameTransform = cropToFrameTransform;
    }

    public int getNhuman() {
        return nhuman;
    }

    public void setNhuman(int nhuman) {
        this.nhuman = nhuman;
    }

    public String getConfidence() {
        return confidence;
    }

    public void setConfidence(String confidence) {
        this.confidence = confidence;
    }

    public Camera getmCamera() {
        return mCamera;
    }

    public void setmCamera(Camera mCamera) {
        this.mCamera = mCamera;
    }

    //Singelton get Instance
    //Get app object
    public static CameraActivity getInstance() {
        if (cameraActivity == null) {
            synchronized (CameraActivity.class) {
                if (cameraActivity == null) {
                    cameraActivity = new CameraActivity();
                }
            }
        }
        return cameraActivity;
    }

    public int getImageVideoNumber() {
        return imageVideoNumber;
    }
}

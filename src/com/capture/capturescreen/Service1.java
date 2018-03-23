package com.capture.capturescreen;

import android.annotation.TargetApi;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.Image;
import android.media.ImageReader;
import android.media.SoundPool;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import android.widget.ImageButton;
import android.widget.LinearLayout;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import android.content.res.Configuration;

import android.app.Notification.BigPictureStyle;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.res.Resources;
import android.app.KeyguardManager;
import android.content.IntentFilter;
import android.media.MediaActionSound;
public class Service1 extends Service
{
    private LinearLayout mFloatLayout = null;
    private WindowManager.LayoutParams wmParams = null;
    private WindowManager mWindowManager = null;
    private LayoutInflater inflater = null;
    //private ImageButton mFloatView = null;
    private DragScaleView mFloatView = null;

    private static final String TAG = "MainActivity";

    private SimpleDateFormat dateFormat = null;
    private String strDate = null;
    private String pathImage = null;
    private String nameImage = null;

    private MediaProjection mMediaProjection = null;
    private VirtualDisplay mVirtualDisplay = null;

    public static int mResultCode = 0;
    public static Intent mResultData = null;
    public static MediaProjectionManager mMediaProjectionManager1 = null;

    private WindowManager mWindowManager1 = null;
    private int windowWidth = 0;
    private int windowHeight = 0;
    private ImageReader mImageReader = null;
    private DisplayMetrics metrics = null;
    private DisplayMetrics metricstemp = null;
    private int mScreenDensity = 0;

    private static final int INT1440 = 1440;
    private static final int INT720 = 720;
    private static  int HEIGHT = INT1440;
    private static  int WIDTH = INT720;
    private SoundPool soundPool;
    private AudioManager audioManager;
    int currentVolume = 0;
    int maxVolume = 0;
    float volume = 0.0f;

    private Service1 mService1;

    private  NotificationManager mNotificationManager;
    private  Notification.Builder mNotificationBuilder;
    private  BigPictureStyle mNotificationStyle;
    private final int mNotificationId =9999;
    private KeyguardManager mKeyguardManager;


    private MediaActionSound mCameraSound;

    private BootBroadcastReceiver mBootBroadcastReceiver= new BootBroadcastReceiver(){
        @Override
        public void onReceive(Context context, Intent intent) {
            super.onReceive(context, intent);
            //Log.i("yeming","onReceive=="+intent.getAction());
            if(mFloatView != null)
                mFloatView.invalidate();
        }
    };

    @Override
    public void onCreate()
    {
        // TODO Auto-generated method stub
        super.onCreate();
        mService1 = this;

        IntentFilter mIntentFilter = new IntentFilter(Intent.ACTION_SCREEN_ON);
        mIntentFilter.addAction(Intent.ACTION_SCREEN_OFF);
        mIntentFilter.addAction("com.capture.capturescreen.updateview");
        this.registerReceiver(mBootBroadcastReceiver,mIntentFilter);

        audioManager = (AudioManager) this.getSystemService(
                Context.AUDIO_SERVICE);
        currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_RING);
        maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_RING);
        volume = ((float)currentVolume)/maxVolume;

        // Setup the Camera shutter sound
        mCameraSound = new MediaActionSound();
        mCameraSound.load(MediaActionSound.SHUTTER_CLICK);


       /* SoundPool.Builder builder = new SoundPool.Builder();
        builder.setMaxStreams(2);//传入音频数量
        //AudioAttributes是一个封装音频各种属性的方法
        AudioAttributes.Builder attrBuilder = new AudioAttributes.Builder();
        attrBuilder.setLegacyStreamType(AudioManager.STREAM_MUSIC);//设置音频流的合适的属性
        builder.setAudioAttributes(attrBuilder.build());//加载一个AudioAttributes
        soundPool = builder.build();
        soundPool.load(this,R.raw.camera_click,1);*/


        //notifiaction
        mNotificationManager = (NotificationManager)this.getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationBuilder = new Notification.Builder(this.getApplication().getApplicationContext())
                .setTicker(this.getResources().getString(R.string.screenshot_saving_ticker))
                .setContentTitle(this.getResources().getString(R.string.screenshot_saving_title))
                .setContentText(this.getResources().getString(R.string.screenshot_saving_text))
                .setSmallIcon(R.drawable.stat_notify_image)
                .setWhen(System.currentTimeMillis());
                /*.setColor(r.getColor(com.android.internal.R.color.system_notification_accent_color));*/

        createFloatView();

        createVirtualEnvironment();




        mKeyguardManager = (KeyguardManager) this.getSystemService(Context.KEYGUARD_SERVICE);

    }

    @Override
    public IBinder onBind(Intent intent)
    {
        // TODO Auto-generated method stub
        return null;
    }

    public boolean isScreenChange() {
        Configuration mConfiguration = this.getResources().getConfiguration(); //获取设置的配置信息
        int ori = mConfiguration.orientation ; //获取屏幕方向
        if(ori == mConfiguration.ORIENTATION_LANDSCAPE){
            return true;
        }else if(ori == mConfiguration.ORIENTATION_PORTRAIT){
            return false;
        }
        return false;
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (isScreenChange()) {
            HEIGHT = INT720;
            WIDTH = INT1440;
            wmParams.gravity = Gravity.LEFT;
        } else {
            HEIGHT = INT1440;
            WIDTH = INT720;
            wmParams.gravity = Gravity.TOP;
        }
        //wmParams.x = 0;
        //wmParams.y = 0;

        // mWindowManager.updateViewLayout(mFloatLayout, wmParams);
        if (mFloatLayout != null) {
            mWindowManager.removeView(mFloatLayout);
        }
        stopVirtual();
        tearDownMediaProjection();
        createFloatView();
        createVirtualEnvironment();
    }
    

    private void createFloatView() {

        wmParams = new WindowManager.LayoutParams();
        mWindowManager = (WindowManager) getApplication().getSystemService(getApplication().WINDOW_SERVICE);
        wmParams.format = PixelFormat.RGBA_8888;


        /*wmParams.type = WindowManager.LayoutParams.TYPE_SYSTEM_ERROR;
        wmParams.flags =  WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                | WindowManager.LayoutParams.FLAG_FULLSCREEN
                | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                | WindowManager.LayoutParams.FLAG_BLUR_BEHIND
               // | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
        ;*/
        wmParams.type = LayoutParams.TYPE_TOP_MOST;
        wmParams.flags = LayoutParams.FLAG_NOT_FOCUSABLE
                | LayoutParams.FLAG_FULLSCREEN | LayoutParams.FLAG_LAYOUT_IN_SCREEN
                | LayoutParams.FLAG_LAYOUT_NO_LIMITS;

        /*wmParams.type = LayoutParams.TYPE_TOAST;
        wmParams.flags =LayoutParams.FLAG_TRANSLUCENT_NAVIGATION
                |LayoutParams.FLAG_TRANSLUCENT_STATUS
                |LayoutParams.FLAG_FULLSCREEN
                |LayoutParams.FLAG_LAYOUT_IN_SCREEN
                |LayoutParams.FLAG_SPLIT_TOUCH
                |LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH
                |LayoutParams.FLAG_LOCAL_FOCUS_MODE;*/
        if (isScreenChange()) {
            HEIGHT = INT720;
            WIDTH = INT1440;
            wmParams.gravity = Gravity.LEFT;
        } else {
            HEIGHT = INT1440;
            WIDTH = INT720;
            wmParams.gravity = Gravity.TOP;
        }

        //wmParams.gravity = Gravity.TOP;
        wmParams.x = 0;
        wmParams.y = 0;

        wmParams.width = WIDTH;//WindowManager.LayoutParams.MATCH_PARENT;
        wmParams.height = HEIGHT;//WindowManager.LayoutParams.MATCH_PARENT;
        inflater = LayoutInflater.from(getApplication());
        mFloatLayout = (LinearLayout) inflater.inflate(R.layout.float_layout, null);
        mFloatLayout.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                return false;
            }
        });
        //mFloatLayout.getdo
        mWindowManager.addView(mFloatLayout, wmParams);
        //mFloatView = (ImageButton)mFloatLayout.findViewById(R.id.float_id);
        mFloatView = (DragScaleView) mFloatLayout.findViewById(R.id.float_id);
        if(mFloatView.getFunctionFlag()==1){
            mFloatView.setBackgroundColor(Color.parseColor("#00000000"));
        }else{
            mFloatView.setBackgroundColor(Color.parseColor("#88888888"));
        }
        mFloatView.setService(this);

        metricstemp = new DisplayMetrics();
        mFloatLayout.measure(View.MeasureSpec.makeMeasureSpec(0,
                View.MeasureSpec.UNSPECIFIED), View.MeasureSpec
                .makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
        mWindowManager.getDefaultDisplay().getMetrics(metricstemp);
        Log.i(TAG, "mFloatLayout.getMeasuredWidth()" + mFloatLayout.getMeasuredWidth() + "," + mFloatLayout.getMeasuredHeight()
                + "metricstemp==" + metricstemp.widthPixels + "," + metricstemp.heightPixels);

        /*mFloatView.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                // TODO Auto-generated method stub

                wmParams.x = (int) event.getRawX() - mFloatView.getMeasuredWidth() / 2;
                wmParams.y = (int) event.getRawY() - mFloatView.getMeasuredHeight() / 2 - 25;

                mWindowManager.updateViewLayout(mFloatLayout, wmParams);
                return false;
            }
        });*/

        mFloatView.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                // hide the button
                /*mFloatView.setVisibility(View.INVISIBLE);

                Handler handler1 = new Handler();
                handler1.postDelayed(new Runnable() {
                    public void run() {
                        //start virtual
                        startVirtual();
                        Handler handler2 = new Handler();
                        handler2.postDelayed(new Runnable() {
                            public void run() {
                                //capture the screen
                                startCapture();
                                Handler handler3 = new Handler();
                                handler3.postDelayed(new Runnable() {
                                    public void run() {
                                        mFloatView.setVisibility(View.VISIBLE);
                                        //stopVirtual();
                                    }
                                }, 500);
                            }
                        }, 500);
                    }
                }, 500);*/
            }
        });

        Log.i(TAG, "created the float sphere view");
    }

    public void updateWindow(int w,int h){
        wmParams.width = w;
        wmParams.height = h;
        mWindowManager.updateViewLayout(mFloatLayout, wmParams);
    }
    private boolean isTouchPointInView(View view, int x, int y) {
        if (view == null) {
            return false;
        }
        int[] location = new int[2];
        view.getLocationOnScreen(location);
        int left = location[0];
        int top = location[1];
        int right = left + view.getMeasuredWidth();
        int bottom = top + view.getMeasuredHeight();
        //view.isClickable() &&
        if (y >= top && y <= bottom && x >= left
                && x <= right) {
            return true;
        }
        return false;
    }
    private void createVirtualEnvironment(){
        dateFormat = new SimpleDateFormat("yyyy_MM_dd_hh_mm_ss");
        strDate = dateFormat.format(new java.util.Date());
        pathImage = Environment.getExternalStorageDirectory().getPath()+"/Pictures/Screenshots/";
        nameImage = pathImage+strDate+".png";
        File fileImage = new File(pathImage);
        try {
            if(!fileImage.exists()){
                fileImage.mkdirs();
                Log.i(TAG, "image file created");
            }
        }catch (Exception e){

        }

        mMediaProjectionManager1 = (MediaProjectionManager)getApplication().getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        mWindowManager1 = (WindowManager)getApplication().getSystemService(Context.WINDOW_SERVICE);
        windowWidth = WIDTH;//mWindowManager1.getDefaultDisplay().getWidth();
        windowHeight = HEIGHT;//mWindowManager1.getDefaultDisplay().getHeight();
        metrics = new DisplayMetrics();
        mWindowManager1.getDefaultDisplay().getMetrics(metrics);
        mScreenDensity = metrics.densityDpi;
        mImageReader = ImageReader.newInstance(windowWidth, windowHeight, 0x1, 2); //ImageFormat.RGB_565
        Log.i(TAG, "windowWidth=="+windowWidth+";;windowHeight=="+windowHeight+";;metrics"+metrics.widthPixels+";;"+metrics.heightPixels+";;mScreenDensity=="+mScreenDensity);
        Log.i(TAG, "prepared the virtual environment");
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public void startVirtual(){
        if (mMediaProjection != null) {
            Log.i(TAG, "want to display virtual");
            virtualDisplay();
        } else {
            Log.i(TAG, "start screen capture intent");
            Log.i(TAG, "want to build mediaprojection and display virtual");
            setUpMediaProjection();
            virtualDisplay();
        }
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public void setUpMediaProjection(){
        mResultData = ((ShotApplication)getApplication()).getIntent();
        mResultCode = ((ShotApplication)getApplication()).getResult();
        mMediaProjectionManager1 = ((ShotApplication)getApplication()).getMediaProjectionManager();
        mMediaProjection = mMediaProjectionManager1.getMediaProjection(mResultCode, mResultData);
        Log.i(TAG, "mMediaProjection defined");
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void virtualDisplay(){
        mVirtualDisplay = mMediaProjection.createVirtualDisplay("screen-mirror",
                windowWidth, windowHeight, mScreenDensity, DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                mImageReader.getSurface(), null, null);
        Log.i(TAG, "virtual displayed");
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void startCapture() {
        if (mFloatView == null) {
            return;
        }
        if (mFloatView.getRengleRect().right > WIDTH) {
            return;
        } else if (mFloatView.getRengleRect().bottom > HEIGHT) {
            return;
        } else if (mFloatView.getGraphicPath().getRight() > WIDTH) {
            return;
        } else if (mFloatView.getGraphicPath().getBottom() > HEIGHT) {
            return;
        }

        /*if (audioManager != null && audioManager.getRingerMode() != AudioManager.RINGER_MODE_SILENT
                && audioManager.getRingerMode() != AudioManager.RINGER_MODE_VIBRATE) {
            soundPool.play(1, volume, volume, 0, 0, 1);
        }*/
        mCameraSound.play(MediaActionSound.SHUTTER_CLICK);

        strDate = dateFormat.format(new java.util.Date());
        nameImage = pathImage + strDate + ".png";

        Image image = mImageReader.acquireLatestImage();

        if (image==null){
            virtualDisplay();
            image = mImageReader.acquireLatestImage();
        }
        if (image==null){
            return;
        }

        int width = image.getWidth();
        int height = image.getHeight();
        final Image.Plane[] planes = image.getPlanes();
        final ByteBuffer buffer = planes[0].getBuffer();
        int pixelStride = planes[0].getPixelStride();
        int rowStride = planes[0].getRowStride();
        int rowPadding = rowStride - pixelStride * width;
        Log.i(TAG, "clipSelectRect image.getWidth()==" + image.getWidth() + "," + image.getHeight() + ";;pixelStride=" + pixelStride + ";;rowStride==" + rowStride + ";;rowPadding=" + rowPadding);
        Bitmap bitmap = Bitmap.createBitmap(width + rowPadding / pixelStride, height, Bitmap.Config.ARGB_8888);
        bitmap.copyPixelsFromBuffer(buffer);
        bitmap = Bitmap.createBitmap(bitmap, 0, 0, width, height);
        //Bitmap rectBitmap = clipRectangle(bitmap,mFloatView.getRengleRect());
        //Bitmap OvalBitmap =clipOval(rectBitmap,mFloatView.getRengleRect());
        //Bitmap selectRectBitmap =clipSelectRect(bitmap,mFloatView.getGraphicPath());
        if (mFloatView.getFunctionFlag() == 1) {
            Log.i(TAG, "getRengleRect image.getWidth()==" + mFloatView.getRengleRect());
            Bitmap rectBitmap = clipRectangle(bitmap, mFloatView.getRengleRect());
            bitmap.recycle();
            bitmap = rectBitmap;
        } else if (mFloatView.getFunctionFlag() == 2) {
            Bitmap selectRectBitmap = clipSelectRect(bitmap, mFloatView.getGraphicPath());
            bitmap.recycle();
            bitmap = selectRectBitmap;
        } else if (mFloatView.getFunctionFlag() == 3) {
            Bitmap rectBitmap = clipRectangle(bitmap, mFloatView.getRengleRect());
            Bitmap OvalBitmap = clipOval(rectBitmap, mFloatView.getRengleRect());
            bitmap.recycle();
            rectBitmap.recycle();
            bitmap = OvalBitmap;
        } else {

        }

        image.close();
        Log.i(TAG, "image data captured");

        if (bitmap != null) {
            try {
                File fileImage = new File(nameImage);
                if (!fileImage.exists()) {
                    fileImage.createNewFile();
                    Log.i(TAG, "image file created");
                }
                FileOutputStream out = new FileOutputStream(fileImage);
                Uri contentUri = null;
                if (out != null) {
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
                    out.flush();
                    out.close();
                    Intent media = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                    contentUri = Uri.fromFile(fileImage);
                    media.setData(contentUri);
                    this.sendBroadcast(media);
                    Log.i(TAG, "screen image saved");
                }

                if (mFloatView.getSharefunctionFlag() != 0) {
                    Intent intent = new Intent(Intent.ACTION_SEND);
                    intent.setType("image/png");
                    intent.putExtra(Intent.EXTRA_STREAM, contentUri);
                    intent.putExtra(Intent.EXTRA_SUBJECT, "Share");
                    //intent.putExtra(Intent.EXTRA_TEXT, "I have successfully share my message through my app");
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    //startActivity(Intent.createChooser(intent, "share"));
                    this.getApplication().startActivity(intent);
                }


                sendNotification(bitmap,contentUri);
                bitmap.recycle();

            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }


    }


    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void tearDownMediaProjection() {
        if (mMediaProjection != null) {
            mMediaProjection.stop();
            mMediaProjection = null;
        }
        Log.i(TAG,"mMediaProjection undefined");
    }

    private void stopVirtual() {
        if (mVirtualDisplay == null) {
            return;
        }
        mVirtualDisplay.release();
        mVirtualDisplay = null;
        Log.i(TAG,"virtual display stopped");
    }

    @Override
    public void onDestroy()
    {
        // to remove mFloatLayout from windowManager
        super.onDestroy();
        this.unregisterReceiver(mBootBroadcastReceiver);
        if(mFloatLayout != null)
        {
            mWindowManager.removeView(mFloatLayout);
        }
        /*if(soundPool != null){
            soundPool.release();
        }*/
        if(mCameraSound != null){
            mCameraSound.release();
        }
        tearDownMediaProjection();
        mFloatView.setService(null);
        Log.i(TAG, "application destroy");
    }

    public void dubbleTabCapture(){

        mFloatView.setVisibility(View.INVISIBLE);

        Handler handler1 = new Handler();
        handler1.postDelayed(new Runnable() {
            public void run() {
                //start virtual
                startVirtual();
                Handler handler2 = new Handler();
                handler2.postDelayed(new Runnable() {
                    public void run() {
                        //capture the screen
                        startCapture();
                        Handler handler3 = new Handler();
                        handler3.postDelayed(new Runnable() {
                            public void run() {
                                //mFloatView.setVisibility(View.VISIBLE);
                                stopVirtual();
                                mService1.stopSelf();
                            }
                        }, 50);
                    }
                }, 50);
            }
        }, 50);
    }


    public Bitmap clipRectangle(Bitmap bitmap,Rect mRect) {
        if (mRect == null) {
            return null;
        }
        if (mRect.left < 0)
            mRect.left = 0;
        if (mRect.right < 0)
            mRect.right = 0;
        if (mRect.top < 0)
            mRect.top = 0;
        if (mRect.bottom < 0)
            mRect.bottom = 0;
        int cut_width = Math.abs(mRect.left - mRect.right);
        int cut_height = Math.abs(mRect.top - mRect.bottom);
        if (cut_width > 0 && cut_height > 0) {
            Bitmap cutBitmap = Bitmap.createBitmap(bitmap, mRect.left, mRect.top, cut_width, cut_height);
            return cutBitmap;
        }
        return null;
    }

    public Bitmap clipOval(Bitmap clipbitmap,Rect mRect) {
        if (mRect == null) {
            return null;
        }
        if (mRect.left < 0)
            mRect.left = 0;
        if (mRect.right < 0)
            mRect.right = 0;
        if (mRect.top < 0)
            mRect.top = 0;
        if (mRect.bottom < 0)
            mRect.bottom = 0;
        int cut_width = Math.abs(mRect.left - mRect.right);
        int cut_height = Math.abs(mRect.top - mRect.bottom);
        if (cut_width <= 0 || cut_height <= 0) {
            return null;
        }
        Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setStyle(Paint.Style.FILL_AND_STROKE);
        paint.setColor(Color.WHITE);
        Bitmap temp = Bitmap.createBitmap(cut_width, cut_height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(temp);
        canvas.drawOval(0,0,cut_width,cut_height,paint);
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        // 关键代码，关于Xfermode和SRC_IN请自行查阅
        canvas.drawBitmap(clipbitmap, 0 , 0, paint);
        //saveCutBitmap(temp);
        return temp;
    }


    public Bitmap clipSelectRect(Bitmap bitmap,GraphicPath mGraphicPath) {
        Rect mRect = new Rect(mGraphicPath.getLeft(), mGraphicPath.getTop(), mGraphicPath.getRight(), mGraphicPath.getBottom());
        if (mRect.left < 0) mRect.left = 0;
        if (mRect.right < 0) mRect.right = 0;
        if (mRect.top < 0) mRect.top = 0;
        if (mRect.bottom < 0) mRect.bottom = 0;
        int cut_width = Math.abs(mRect.left - mRect.right);
        int cut_height = Math.abs(mRect.top - mRect.bottom);
        Log.i(TAG, "clipSelectRect　mRect==" + mRect + ";;;w==" + bitmap.getWidth() + ";;;h===" + bitmap.getHeight());
        if (cut_width > 0 && cut_height > 0) {
            Bitmap cutBitmap = Bitmap.createBitmap(bitmap, mRect.left, mRect.top, cut_width, cut_height);
            //Log.d("wnaglei","clipSelectRect  weight: "+cutBitmap.getWidth()+" height: "+cutBitmap.getHeight());
            //上面是将全屏截图的结果先裁剪成需要的大小，下面是裁剪成曲线图形区域
            Paint paint = new Paint();
            paint.setAntiAlias(true);
            paint.setStyle(Paint.Style.FILL_AND_STROKE);
            paint.setColor(Color.WHITE);
            Bitmap temp = Bitmap.createBitmap(cut_width, cut_height, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(temp);
            Path path = new Path();
            Log.d("yeming", "size2===" + mGraphicPath.size());
            if (mGraphicPath.size() > 1) {
                path.moveTo((float) ((mGraphicPath.pathX.get(0) - mRect.left)), (float) ((mGraphicPath.pathY.get(0) - mRect.top)));
                for (int i = 1; i < mGraphicPath.size(); i++) {
                    path.lineTo((mGraphicPath.pathX.get(i) - mRect.left), (mGraphicPath.pathY.get(i) - mRect.top));
                }
            } else {
                return null;
            }
            canvas.drawPath(path, paint);
            paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN)); // 关键代码，关于Xfermode和SRC_IN请自行查阅
            canvas.drawBitmap(cutBitmap, 0, 0, paint);
            cutBitmap.recycle();
            return temp;

        }
        return null;
    }

    private void sendNotification(Bitmap preview,Uri uri){
        Resources r = this.getResources();

        // Create the intent to show the screenshot in gallery
        Intent launchIntent = new Intent(Intent.ACTION_VIEW);
        launchIntent.setDataAndType(uri, "image/png");
        launchIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);


        mNotificationStyle = new Notification.BigPictureStyle()
                .bigPicture(preview);
        mNotificationBuilder.setStyle(mNotificationStyle);


        mNotificationBuilder
                .setContentTitle(r.getString(R.string.screenshot_saved_title))
                .setContentText(r.getString(R.string.screenshot_saved_text))
                .setContentIntent(PendingIntent.getActivity(this, 0, launchIntent, 0))
                .setWhen(System.currentTimeMillis())
                .setAutoCancel(true);


        Intent sharingIntent = new Intent(Intent.ACTION_SEND);
        sharingIntent.setType("image/png");
        sharingIntent.putExtra(Intent.EXTRA_STREAM, uri);
        sharingIntent.putExtra(Intent.EXTRA_SUBJECT, nameImage);

        Intent chooserIntent = Intent.createChooser(sharingIntent, null);
        chooserIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK
                | Intent.FLAG_ACTIVITY_NEW_TASK);
        mNotificationBuilder.addAction(R.drawable.ic_menu_share,
                r.getString(com.android.internal.R.string.share),
                PendingIntent.getActivity(this.getApplication().getApplicationContext(), 0, chooserIntent,
                        PendingIntent.FLAG_CANCEL_CURRENT));

        Notification n = mNotificationBuilder.build();
        n.flags &= ~Notification.FLAG_NO_CLEAR;
        mNotificationManager.cancel(mNotificationId);
        mNotificationManager.notify(mNotificationId, n);
    }

    public boolean isKeyGuard(){
        Log.d("yeming", "isKeyGuard==" + mKeyguardManager.inKeyguardRestrictedInputMode());
        return mKeyguardManager.inKeyguardRestrictedInputMode();
    }
    /*public boolean isNavigationBarShow(){
  if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
    Display display = getWindowManager().getDefaultDisplay();
    Point size = new Point();
    Point realSize = new Point();
    display.getSize(size);
    display.getRealSize(realSize);
    return realSize.y!=size.y;
  }else {
    boolean menu = ViewConfiguration.get(this).hasPermanentMenuKey();
    boolean back = KeyCharacterMap.deviceHasKey(KeyEvent.KEYCODE_BACK);
    if(menu || back) {
      return false;
    }else {
      return true;
    }
  }
}
public static int getNavigationBarHeight(Activity activity) {
  if (!isNavigationBarShow(activity)){
    return 0;
  }
  Resources resources = activity.getResources();
  int resourceId = resources.getIdentifier("navigation_bar_height",
      "dimen", "android");
  //获取NavigationBar的高度
  int height = resources.getDimensionPixelSize(resourceId);
  return height;
}
public static int getSceenHeight(Activity activity) {
  return activity.getWindowManager().getDefaultDisplay().getHeight()+getNavigationBarHeight(activity);
}*/
}

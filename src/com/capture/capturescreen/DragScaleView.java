package com.capture.capturescreen;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.content.res.Configuration;

/**
 * Created by yeming on 17-8-8.
 */



public class DragScaleView extends View implements View.OnTouchListener {
    protected int screenWidth;
    protected int screenHeight;
    protected int lastX;
    protected int lastY;
    private int oriLeft;
    private int oriRight;
    private int oriTop;
    private int oriBottom;
    private int dragDirection;
    private static final int TOP = 0x15;
    private static final int LEFT = 0x16;
    private static final int BOTTOM = 0x17;
    private static final int RIGHT = 0x18;
    private static final int LEFT_TOP = 0x11;
    private static final int RIGHT_TOP = 0x12;
    private static final int LEFT_BOTTOM = 0x13;
    private static final int RIGHT_BOTTOM = 0x14;
    private static final int CENTER = 0x19;
    private static final int INVALID = 0x20;
    private int offset = 0;//20;
    private static final int MININ_W_h = 120;
    private static final int TORERANCE = 40;//TORERANCE
    protected Paint paint = new Paint();
    private static final String TAG = "DragScaleView";
    private long lasttime = 0;
    private Service1 service = null;
    //private Rect rengleRect = null;
    private GraphicPath mGraphicPath;
    private Bitmap confirmBitmap, cancelBitmap,shareBitmap;
    private static final int DEFAULT_CONFIRM_BUTTON_RES = R.mipmap.ic_done_white_36dp;
    private static final int DEFAULT_CANCEL_BUTTON_RES = R.mipmap.ic_close_capture;
    private static final int DEFAULT_SHARE_BUTTON_RES = R.mipmap.ic_done_white_fx;
    private int confirmButtonRes = DEFAULT_CONFIRM_BUTTON_RES;
    private int cancelButtonRes = DEFAULT_CANCEL_BUTTON_RES;
    private int shareButtonRes = DEFAULT_SHARE_BUTTON_RES;

    private Rect markedArea;
    private Rect confirmArea,cancelArea,shareArea;
    private static final int BUTTON_EXTRA_WIDTH = 4;
    private int mActionGap = 15;

    private int downX,downY;
    private int startX,startY;
    private int endX,endY;
    private Paint mBitPaint;

    private boolean isValid=false;
    private boolean isUp=false;
    private boolean isMoveMode=false;
    private boolean isAdjustMode=false;
    private boolean isButtonClicked=false;
    public static int functionFlag = 2;//1,矩形　　2,自由截屏　3,椭圆，
    public static int sharefunctionFlag = 0;
    private Context mContext;

    private Rect rengleRect = new Rect();
    private boolean pinterDown = false;


    private int x;
    private int y;
    private int j;
    private static final int RADIUS = 15;
    private static final int LITTLE_RECTANGLE_W = 10;
    private static final int LITTLE_RECTANGLE_L = 28;

    public boolean isScreenChange(Context context) {
        Configuration mConfiguration = context.getResources().getConfiguration(); //获取设置的配置信息
        int ori = mConfiguration.orientation ; //获取屏幕方向
        if(ori == mConfiguration.ORIENTATION_LANDSCAPE){
            return true;
        }else if(ori == mConfiguration.ORIENTATION_PORTRAIT){

            return false;
        }
        return false;
    }
    protected void initScreenW_H() {
        functionFlag = ShotApplication.capturemode;
        screenHeight = 1440;//getResources().getDisplayMetrics().heightPixels - 0;//TORERANCE;
        screenWidth = getResources().getDisplayMetrics().widthPixels;
        if (isScreenChange(mContext)) {
            screenHeight = 720;//getResources().getDisplayMetrics().heightPixels - 0;//TORERANCE;
            screenWidth = 1440;//getResources().getDisplayMetrics().widthPixels;

            rengleRect.left = screenWidth/2-(200)/2;
            rengleRect.top = screenHeight/2-(200)/2;
            rengleRect.right = screenWidth/2+(200)/2;
            rengleRect.bottom = screenHeight/2+(200)/2;
        } else {
            screenHeight = 1440;//getResources().getDisplayMetrics().heightPixels - 0;//TORERANCE;
            screenWidth = 720;//getResources().getDisplayMetrics().widthPixels;

            rengleRect.left = screenWidth/2-(200)/2;
            rengleRect.top = screenHeight/2-(200)/2;
            rengleRect.right = screenWidth/2+(200)/2;
            rengleRect.bottom = screenHeight/2+(200)/2;
        }
        Log.d("yeming1","rengleRect1="+rengleRect);
        //rengleRect = new Rect();
        mGraphicPath = new GraphicPath();

        markedArea = new Rect();
        confirmArea = new Rect();
        cancelArea = new Rect();
        shareArea = new Rect();

        mBitPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mBitPaint.setFilterBitmap(true);
        mBitPaint.setDither(true);
    }

    private void init(Context context,AttributeSet attrs){
        if (attrs!=null) {
            TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.MarkSizeView);
            confirmButtonRes =typedArray.getResourceId(R.styleable.MarkSizeView_confirmButtonRes,DEFAULT_CONFIRM_BUTTON_RES);
            cancelButtonRes=typedArray.getResourceId(R.styleable.MarkSizeView_cancleButtonRes,DEFAULT_CANCEL_BUTTON_RES);
            shareButtonRes=typedArray.getResourceId(R.styleable.MarkSizeView_cancleButtonRes,DEFAULT_SHARE_BUTTON_RES);
        }
        confirmBitmap = BitmapFactory.decodeResource(getResources(), confirmButtonRes);
        cancelBitmap = BitmapFactory.decodeResource(getResources(), cancelButtonRes);
        shareBitmap = BitmapFactory.decodeResource(getResources(), shareButtonRes);
    }

    public DragScaleView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mContext = context;
        setOnTouchListener(this);
        init(context, attrs);
        initScreenW_H();
    }

    public DragScaleView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        setOnTouchListener(this);
        init(context, attrs);
        initScreenW_H();
    }

    public DragScaleView(Context context) {
        super(context);
        mContext = context;
        setOnTouchListener(this);
        initScreenW_H();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if(pinterDown){
            return;
        }
        super.onDraw(canvas);
        if(functionFlag == 1) {
            //paint.setColor(Color.TRANSPARENT);
            paint.setColor(Color.parseColor("#ff72c4fe"));
            paint.setStrokeWidth(4.0f);
            paint.setStyle(Paint.Style.STROKE);

            //paint.setStyle(Paint.Style.FILL_AND_STROKE);
            paint.setXfermode(null);
            paint.setAntiAlias(true);

            if (isUp) {

                //canvas.drawColor(Color.BLACK,PorterDuff.Mode.SRC);//绘制透黑色

                //paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
                //paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC));
                //canvas.drawRect(rengleRect.left+4, rengleRect.top+4, rengleRect.right-4, rengleRect.bottom-4, paint);

                paint.setColor(Color.parseColor("#ff72c4fe"));
                paint.setStrokeWidth(4.0f);
                paint.setStyle(Paint.Style.STROKE);
                paint.setAntiAlias(true);
                //paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC));
                canvas.drawRect(rengleRect,paint);
            } else {
                //paint.
                /*paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC));
                canvas.drawRect(rengleRect.left+4, rengleRect.top+4, rengleRect.right-4, rengleRect.bottom-4, paint);*/

                paint.setColor(Color.parseColor("#ff72c4fe"));
                paint.setStrokeWidth(4.0f);
                paint.setStyle(Paint.Style.STROKE);
                paint.setAntiAlias(true);
                //paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC));
                canvas.drawRect(rengleRect,paint);

            }



            paint.setStrokeWidth(1.0f);
            for(j=1;j<3;j++){
                //竖线
                x=rengleRect.left+j*(rengleRect.right-rengleRect.left)/3;
                //y=rengleRect.top+j*(rengleRect.bottom-rengleRect.top)/3;
                canvas.drawLine(x,rengleRect.top,x,rengleRect.bottom,paint);

                //x=rengleRect.right+j*(rengleRect.bottom-rengleRect.top)/3;
                y=rengleRect.top+j*(rengleRect.bottom-rengleRect.top)/3;
                canvas.drawLine(rengleRect.left,y,rengleRect.right,y,paint);

            }

            paint.setColor(Color.parseColor("#ff72c4fe"));
            paint.setStrokeWidth(1.0f);
            paint.setStyle(Paint.Style.FILL_AND_STROKE);
            paint.setAntiAlias(true);
            //paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC));
            if(true){
                //竖圆
                canvas.drawCircle(rengleRect.left,rengleRect.top,RADIUS,paint);
                canvas.drawCircle(rengleRect.left,rengleRect.bottom,RADIUS,paint);
                canvas.drawCircle(rengleRect.right,rengleRect.top,RADIUS,paint);
                canvas.drawCircle(rengleRect.right,rengleRect.bottom,RADIUS,paint);

            }

            //画矩形
            paint.setColor(Color.parseColor("#ff72c4fe"));
            paint.setStrokeWidth(1.0f);
            paint.setStyle(Paint.Style.FILL_AND_STROKE);
            canvas.drawRect((rengleRect.right+rengleRect.left)/2 -LITTLE_RECTANGLE_L/2,
                    rengleRect.top-LITTLE_RECTANGLE_W/2,
                    (rengleRect.right+rengleRect.left)/2 +LITTLE_RECTANGLE_L/2,
                    rengleRect.top+LITTLE_RECTANGLE_W/2,
                    paint);

            canvas.drawRect(rengleRect.right-LITTLE_RECTANGLE_W/2,
                    (rengleRect.bottom+rengleRect.top)/2 -LITTLE_RECTANGLE_L/2,
                    rengleRect.right+LITTLE_RECTANGLE_W/2,
                    (rengleRect.bottom+rengleRect.top)/2 +LITTLE_RECTANGLE_L/2,
                    paint);

            canvas.drawRect((rengleRect.right+rengleRect.left)/2 -LITTLE_RECTANGLE_L/2,
                    rengleRect.bottom-LITTLE_RECTANGLE_W/2,
                    (rengleRect.right+rengleRect.left)/2 +LITTLE_RECTANGLE_L/2,
                    rengleRect.bottom+LITTLE_RECTANGLE_W/2,
                    paint);

            canvas.drawRect(rengleRect.left-LITTLE_RECTANGLE_W/2,
                    (rengleRect.bottom+rengleRect.top)/2 -LITTLE_RECTANGLE_L/2,
                    rengleRect.left+LITTLE_RECTANGLE_W/2,
                    (rengleRect.bottom+rengleRect.top)/2 +LITTLE_RECTANGLE_L/2,
                    paint);


            //draw button
            if (/*isValid&&*/isUp) {
                //mBitPaint.setXfermode(null);
                canvas.drawBitmap(confirmBitmap, null, confirmArea, mBitPaint);
                canvas.drawBitmap(cancelBitmap, null, cancelArea, mBitPaint);
                /*if(!service.isKeyGuard()){
                canvas.drawBitmap(shareBitmap, null, shareArea, mBitPaint);
                }*/

                if(service.isKeyGuard()){
                   // mBitPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
                    //canvas.drawBitmap(shareBitmap, null, shareArea, mBitPaint);
                }else{
                    //mBitPaint.setXfermode(null);
                    canvas.drawBitmap(shareBitmap, null, shareArea, mBitPaint);
                }
            }
        } else if(functionFlag == 3){
            RectF dst = new RectF(offset, offset, (getWidth() - offset), (getHeight()
                    - offset));//2a=100-30,2b=310-260
            paint.setColor(Color.GREEN);
            canvas.drawOval(dst, paint);
        }else if(functionFlag == 2) {
            if (pinterDown){
                return;
            }
            paint.setColor(Color.parseColor("#ff72c4fe"));
            paint.setStyle(Paint.Style.FILL_AND_STROKE);
            paint.setStrokeWidth(4.0f);
            paint.setAntiAlias(false);
            //Log.d("yeming", "size===" + mGraphicPath.size());
            if (isUp) {
                Path path = new Path();
                if (mGraphicPath.size() > 1) {
                    path.moveTo(mGraphicPath.pathX.get(0), mGraphicPath.pathY.get(0));
                    for (int i = 1; i < mGraphicPath.size(); i++) {
                        path.lineTo(mGraphicPath.pathX.get(i), mGraphicPath.pathY.get(i));
                    }
                } else {
                    return;
                }
                canvas.drawColor(Color.BLACK, PorterDuff.Mode.SRC);//绘制透黑色

                paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
                canvas.drawPath(path, paint);
            } else {
                //paint.
                paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC));
                if (mGraphicPath.size() > 1) {
                    for (int i = 1; i < mGraphicPath.size(); i++) {
                        canvas.drawLine(mGraphicPath.pathX.get(i - 1), mGraphicPath.pathY.get(i - 1), mGraphicPath.pathX.get(i), mGraphicPath.pathY.get(i), paint);
                    }
                }
            }

            //draw button
            if (/*isValid&&*/isUp) {
                //mBitPaint.setXfermode(null);
                canvas.drawBitmap(confirmBitmap, null, confirmArea, mBitPaint);
                canvas.drawBitmap(cancelBitmap, null, cancelArea, mBitPaint);
                if(service.isKeyGuard()){
                    //mBitPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
                    //anvas.drawBitmap(shareBitmap, null, shareArea, mBitPaint);
                }else{
                    //mBitPaint.setXfermode(null);
                    canvas.drawBitmap(shareBitmap, null, shareArea, mBitPaint);
                }
            }
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        int action = event.getAction();
        int x= (int) event.getRawX();
        int y= (int) event.getRawY();
        switch (action){
            case MotionEvent.ACTION_DOWN:
                isUp = false;
                //Log.d(TAG,"lasttime"+lasttime+";;;System.currentTimeMillis()="+System.currentTimeMillis());
                if(functionFlag == 1) {
                    if (isAreaContainPoint(confirmArea,x, y)) {
                        Log.i(TAG, "getRengleRect2 image.getWidth()=="+rengleRect);
                        isButtonClicked = true;
                        dubbleTabCapture();
                        Log.i(TAG, "getRengleRect3 image.getWidth()=="+rengleRect);
                        break;
                    } else if (isAreaContainPoint(cancelArea,x, y)) {
                        isButtonClicked = true;
                        if (service != null) {
                            service.stopSelf();
                        }
                        break;
                    }else if(isAreaContainPoint(shareArea,x, y)&& !service.isKeyGuard()){
                        sharefunctionFlag =1;
                        isButtonClicked = true;
                        dubbleTabCapture();
                        break;
                    }else{
                        sharefunctionFlag = 0;
                        isButtonClicked = false;
                    }

                    /*if (System.currentTimeMillis() - lasttime < 500) {
                        lasttime = System.currentTimeMillis();
                        dubbleTabCapture();
                        return false;
                    }
                    lasttime = System.currentTimeMillis();*/

                    //Log.d(TAG,"screenWidth=="+screenWidth+";;;screenHeight=="+screenHeight);
                    /*oriLeft = v.getLeft();
                    oriRight = v.getRight();
                    oriTop = v.getTop();
                    oriBottom = v.getBottom();*/
                    lastY = (int) event.getRawY();
                    lastX = (int) event.getRawX();
                    /*dragDirection = getDirection(v, (int) event.getX(),
                            (int) event.getY());*/
                    //Log.d("yeming1","rengleRect3="+rengleRect);
                    dragDirection = getDirection(rengleRect, (int) event.getRawX(),
                            (int) event.getRawY());

                }else if(functionFlag == 2) {
                    downY = (int) event.getRawY();
                    downX = (int) event.getRawX();
                    pinterDown = true;
                    //Log.d("yeming","pinterDown="+pinterDown);
                    if (isAreaContainPoint(confirmArea,x, y)) {
                        isButtonClicked = true;
                        dubbleTabCapture();
                    } else if (isAreaContainPoint(cancelArea,x, y)) {
                        isButtonClicked = true;
                        //isValid = true;
                        if (true) {
                            //isValid = false;
                            startX = startY = endX = endY = 0;
                        }
                        mGraphicPath.clear();
                        if(service != null){
                            service.stopSelf();
                        }
                    }else if(isAreaContainPoint(shareArea,x, y)&& !service.isKeyGuard()){
                        sharefunctionFlag =1;
                        isButtonClicked = true;
                        dubbleTabCapture();

                    } else {
                        sharefunctionFlag = 0;
                        isButtonClicked = false;
                        isMoveMode = false;
                        startX = (int) event.getX();
                        startY = (int) event.getY();
                        endX = startX;
                        endY = startY;
                        //mGraphicPath.clear();
                        //mGraphicPath.addPath(x,y);

                    }
                }
                break;

            case MotionEvent.ACTION_MOVE:
                //Log.d(TAG,"MotionEvent.ACTION_MOVE functionFlag="+functionFlag);
                if(functionFlag == 2) {
                    if((int)Math.abs(Math.sqrt( ((x-downX)*(x-downX) +(y-downY)*(y-downY)) ))>10 ){
                        if(pinterDown){
                            mGraphicPath.clear();
                            pinterDown = false;
                        }

                        //Log.d("yeming","pinterDown2="+pinterDown+";;;x="+x+";;y="+y+";;downx="+downX+";;;downy="+downY+";;;;;;;;;"+(int)Math.abs(Math.sqrt( ((x-downX)*(x-downX) +(y-downY)*(y-downY)) )));
                    }
                    //Log.d("yeming","pinterDown2="+pinterDown);
                    if(!isButtonClicked){
                        if(pinterDown){

                        }else {
                            mGraphicPath.addPath(x, y);
                        }
                    }
                }
                break;

            case MotionEvent.ACTION_UP:
                isUp = true;
                if(functionFlag == 2) {
                    //Log.d(TAG,"MotionEvent.ACTION_UP isButtonClicked="+isButtonClicked);

                    if (isButtonClicked) {
                        pinterDown = false;
                        break;
                    }else{
                        if(pinterDown){
                            pinterDown = false;
                        }else{
                            mGraphicPath.addPath(x,y);
                        }
                    }
                    //Log.d(TAG,"MotionEvent.ACTION_UP22");
                    startX = mGraphicPath.getLeft();
                    startY = mGraphicPath.getTop();
                    endX = mGraphicPath.getRight();
                    endY = mGraphicPath.getBottom();

                    /*if ((endX-startX)*(endY-startY)>200){
                        isValid=true;
                    }*/
                    //markedArea.set(startX,startY,endX,endY);
                    if (endY < getHeight() - confirmBitmap.getHeight() * 3){
                        //显示在选区的下面
                        cancelArea.set(endX - confirmBitmap.getWidth()/* - mActionGap*/,
                                endY + mActionGap,
                                endX/* - mActionGap*/,
                                endY + confirmBitmap.getHeight() + mActionGap);
                        confirmArea.set(endX - 2 * confirmBitmap.getWidth() - mActionGap * 1,
                                endY + mActionGap,
                                endX - confirmBitmap.getWidth() - mActionGap * 1,
                                endY + confirmBitmap.getHeight() + mActionGap);
                        shareArea.set(endX - 3 * confirmBitmap.getWidth() - mActionGap * 2,
                                endY + mActionGap,
                                endX - 2*confirmBitmap.getWidth() - mActionGap * 2,
                                endY + confirmBitmap.getHeight() + mActionGap);
                    } else
                    if (startY > confirmBitmap.getHeight() * 3) {
                        //显示在选区的上面
                        cancelArea.set(endX - confirmBitmap.getWidth()/* - mActionGap*/,
                                startY - confirmBitmap.getHeight() - mActionGap,
                                endX/* - mActionGap*/,
                                startY - mActionGap);
                        confirmArea.set(endX - 2 * confirmBitmap.getWidth() - mActionGap * 1,
                                startY - confirmBitmap.getHeight() - mActionGap,
                                endX - confirmBitmap.getWidth() - mActionGap * 1,
                                startY - mActionGap);
                        shareArea.set(endX - 3 * confirmBitmap.getWidth() - mActionGap * 2,
                                startY - confirmBitmap.getHeight() - mActionGap,
                                endX - 2*confirmBitmap.getWidth() - mActionGap * 2,
                                startY - mActionGap);
                    }else
    //                    if (markedArea.width() > confirmBitmap.getWidth() * 3 + mActionGap * 3 && markedArea.height() > confirmBitmap.getHeight() * 5)
                    {
                        //显示在选区的内底部
                        cancelArea.set(endX - confirmBitmap.getWidth()/* - mActionGap*/,
                                endY - confirmBitmap.getHeight() - mActionGap,
                                endX/* - mActionGap*/,
                                endY - mActionGap);
                        confirmArea.set(endX - 2 * confirmBitmap.getWidth() - mActionGap * 1,
                                endY - confirmBitmap.getHeight() - mActionGap,
                                endX - confirmBitmap.getWidth() - mActionGap * 1,
                                endY - mActionGap);
                        shareArea.set(endX - 3 * confirmBitmap.getWidth() - mActionGap * 2,
                                endY - confirmBitmap.getHeight() - mActionGap,
                                endX - 2*confirmBitmap.getWidth() - mActionGap * 2,
                                endY - mActionGap);
                    }

                    if (shareArea.left<0){
                        int cancelAreaLeftMargin=Math.abs(shareArea.left)+mActionGap;
                        cancelArea.left = cancelArea.left+cancelAreaLeftMargin;
                        cancelArea.right = cancelArea.right+cancelAreaLeftMargin;
                        confirmArea.left = confirmArea.left+cancelAreaLeftMargin;
                        confirmArea.right = confirmArea.right+cancelAreaLeftMargin;
                        shareArea.left = shareArea.left+cancelAreaLeftMargin;
                        shareArea.right = shareArea.right+cancelAreaLeftMargin;
                    }

                }else if(functionFlag == 1){
                    if (isButtonClicked) {
                        //mGraphicPath.clear();
                        break;
                    }
                    if (rengleRect.bottom < getHeight() - confirmBitmap.getHeight() * 3){
                        //显示在选区的下面
                        cancelArea.set(rengleRect.right - confirmBitmap.getWidth() /*- mActionGap*/,
                                rengleRect.bottom + mActionGap,
                                rengleRect.right/* - mActionGap*/,
                                rengleRect.bottom + confirmBitmap.getHeight() + mActionGap);

                        confirmArea.set(rengleRect.right - 2 * confirmBitmap.getWidth() - mActionGap * 1,
                                rengleRect.bottom + mActionGap,
                                rengleRect.right - confirmBitmap.getWidth() - mActionGap * 1,
                                rengleRect.bottom + confirmBitmap.getHeight() + mActionGap);

                        shareArea.set(rengleRect.right - 3 * confirmBitmap.getWidth() - mActionGap * 2,
                                rengleRect.bottom + mActionGap,
                                rengleRect.right - 2*confirmBitmap.getWidth() - mActionGap * 2,
                                rengleRect.bottom + confirmBitmap.getHeight() + mActionGap);
                    } else
                    if (rengleRect.top > confirmBitmap.getHeight() * 3) {
                        //显示在选区的上面
                        cancelArea.set(rengleRect.right - confirmBitmap.getWidth() /*- mActionGap*/,
                                rengleRect.top - confirmBitmap.getHeight() - mActionGap,
                                rengleRect.right/* - mActionGap*/,
                                rengleRect.top - mActionGap);

                        confirmArea.set(rengleRect.right - 2 * confirmBitmap.getWidth() - mActionGap * 1,
                                rengleRect.top - confirmBitmap.getHeight() - mActionGap,
                                rengleRect.right - confirmBitmap.getWidth() - mActionGap * 1,
                                rengleRect.top - mActionGap);

                        shareArea.set(rengleRect.right - 3 * confirmBitmap.getWidth() - mActionGap * 2,
                                rengleRect.top - confirmBitmap.getHeight() - mActionGap,
                                rengleRect.right - 2*confirmBitmap.getWidth() - mActionGap * 2,
                                rengleRect.top - mActionGap);
                    }else
                    //                     if (markedArea.width() > confirmBitmap.getWidth() * 3 + mActionGap * 3 && markedArea.height() > confirmBitmap.getHeight() * 5)
                    {
                        //显示在选区的内底部
                        cancelArea.set(rengleRect.right - confirmBitmap.getWidth()/* - mActionGap*/,
                                rengleRect.bottom - confirmBitmap.getHeight() - mActionGap,
                                rengleRect.right/* - mActionGap*/,
                                rengleRect.bottom - mActionGap);
                        confirmArea.set(rengleRect.right - 2 * confirmBitmap.getWidth() - mActionGap * 1,
                                rengleRect.bottom - confirmBitmap.getHeight() - mActionGap,
                                rengleRect.right - confirmBitmap.getWidth() - mActionGap * 1,
                                rengleRect.bottom - mActionGap);
                        shareArea.set(rengleRect.right - 3 * confirmBitmap.getWidth() - mActionGap * 2,
                                rengleRect.bottom - confirmBitmap.getHeight() - mActionGap,
                                rengleRect.right - 2*confirmBitmap.getWidth() - mActionGap * 2,
                                rengleRect.bottom - mActionGap);
                    }

                    if (shareArea.left<0){
                        int cancelAreaLeftMargin=Math.abs(shareArea.left)+mActionGap;
                        cancelArea.left = cancelArea.left+cancelAreaLeftMargin;
                        cancelArea.right = cancelArea.right+cancelAreaLeftMargin;
                        confirmArea.left = confirmArea.left+cancelAreaLeftMargin;
                        confirmArea.right = confirmArea.right+cancelAreaLeftMargin;
                        shareArea.left = shareArea.left+cancelAreaLeftMargin;
                        shareArea.right = shareArea.right+cancelAreaLeftMargin;
                    }
                }
                break;


            case MotionEvent.ACTION_CANCEL:
                    isUp = true;
                break;
        }
        // 处理拖动事件
        if(functionFlag == 1) {
            delDrag(v, event, action);
        }
        //invalidate();
        if(pinterDown){

        }else{
            postInvalidate();
        }
        return false;
    }


    protected void delDrag(View v, MotionEvent event, int action) {
        switch (action) {
            case MotionEvent.ACTION_MOVE:
                int dx = (int) event.getRawX() - lastX;
                int dy = (int) event.getRawY() - lastY;
                switch (dragDirection) {
                    case LEFT: // 左边缘
                        left(v,rengleRect, dx);
                        break;
                    case RIGHT: // 右边缘
                        right(v, rengleRect,dx);
                        break;
                    case BOTTOM: // 下边缘
                        bottom(v, rengleRect,dy);
                        break;
                    case TOP: // 上边缘
                        top(v,rengleRect, dy);
                        break;
                    case CENTER: // 点击中心-->>移动
                        if(functionFlag == 1) {
                        center(v,rengleRect, dx, dy);
                        }
                        break;
                    case LEFT_BOTTOM: // 左下
                        left(v, rengleRect,dx);
                        bottom(v, rengleRect,dy);
                        break;
                    case LEFT_TOP: // 左上
                        left(v,rengleRect, dx);
                        top(v,rengleRect, dy);
                        break;
                    case RIGHT_BOTTOM: // 右下
                        right(v, rengleRect,dx);
                        bottom(v, rengleRect,dy);
                        break;
                    case RIGHT_TOP: // 右上
                        right(v, rengleRect,dx);
                        top(v,rengleRect, dy);
                        break;
                }
                /*if (dragDirection != CENTER) {
                    Log.d(TAG,"delDrag"+oriLeft+","+oriTop+","+oriRight+","+oriBottom);
                    v.layout(oriLeft, oriTop, oriRight, oriBottom);
                }*/
                //Log.d("yeming1","rengleRect2="+rengleRect);
                lastX = (int) event.getRawX();
                lastY = (int) event.getRawY();
                break;
            case MotionEvent.ACTION_UP:
                dragDirection = 0;
                break;
        }
    }
    protected void delDrag(View v, Rect mRect, MotionEvent event, int action) {
        switch (action) {
            case MotionEvent.ACTION_MOVE:
                int dx = (int) event.getRawX() - lastX;
                int dy = (int) event.getRawY() - lastY;
                switch (dragDirection) {
                    case LEFT: // 左边缘
                        left(v, rengleRect,dx);
                        break;
                    case RIGHT: // 右边缘
                        right(v,rengleRect, dx);
                        break;
                    case BOTTOM: // 下边缘
                        bottom(v,rengleRect, dy);
                        break;
                    case TOP: // 上边缘
                        top(v,rengleRect, dy);
                        break;
                    case CENTER: // 点击中心-->>移动
                        if(functionFlag == 1) {
                            center(v,rengleRect, dx, dy);
                        }
                        break;
                    case LEFT_BOTTOM: // 左下
                        left(v, rengleRect,dx);
                        bottom(v,rengleRect, dy);
                        break;
                    case LEFT_TOP: // 左上
                        left(v,rengleRect, dx);
                        top(v, rengleRect,dy);
                        break;
                    case RIGHT_BOTTOM: // 右下
                        right(v,rengleRect, dx);
                        bottom(v,rengleRect, dy);
                        break;
                    case RIGHT_TOP: // 右上
                        right(v, rengleRect,dx);
                        top(v, rengleRect,dy);
                        break;
                }
                /*if (dragDirection != CENTER) {
                    Log.d(TAG,"delDrag"+oriLeft+","+oriTop+","+oriRight+","+oriBottom);
                    v.layout(oriLeft, oriTop, oriRight, oriBottom);
                }*/
                if(dragDirection == INVALID){
                    return;
                }
                lastX = (int) event.getRawX();
                lastY = (int) event.getRawY();
                break;
            case MotionEvent.ACTION_UP:
                dragDirection = 0;
                break;
        }
    }

    private void center(View v, Rect mRect,int dx, int dy) {
        /*int left = v.getLeft() + dx;
        int top = v.getTop() + dy;
        int right = v.getRight() + dx;
        int bottom = v.getBottom() + dy;
        if (left < -offset) {
            left = -offset;
            right = left + v.getWidth();
        }
        if (right > screenWidth + offset) {
            right = screenWidth + offset;
            left = right - v.getWidth();
        }
        if (top < -offset) {
            top = -offset;
            bottom = top + v.getHeight();
        }
        if (bottom > screenHeight + offset) {
            bottom = screenHeight + offset;
            top = bottom - v.getHeight();
        }
        Log.d(TAG,"center"+left+","+top+","+right+","+bottom);
        v.layout(left, top, right, bottom);*/

        int left = mRect.left + dx;
        int top = mRect.top + dy;
        int right = mRect.right + dx;
        int bottom = mRect.bottom + dy;
        if (left < -offset) {
            left = -offset;
            right = left + (mRect.right-mRect.left);
        }
        if (right > screenWidth + offset) {
            right = screenWidth + offset;
            left = right - (mRect.right-mRect.left);
        }
        if (top < -offset) {
            top = -offset;
            bottom = top + (mRect.bottom-mRect.top);
        }
        if (bottom > screenHeight + offset) {
            bottom = screenHeight + offset;
            top = bottom - (mRect.bottom-mRect.top);
        }
        Log.d(TAG,"center"+left+","+top+","+right+","+bottom);
        rengleRect.left = left;
        rengleRect.top = top;
        rengleRect.right = right;
        rengleRect.bottom = bottom;



    }


    private void top(View v, Rect mRect,int dy) {
        /*oriTop += dy;
        if (oriTop < -offset) {
            oriTop = -offset;
        }
        if (oriBottom - oriTop - 2 * offset < MININ_W_h) {
            oriTop = oriBottom - 2 * offset - MININ_W_h;
        }*/
        Log.d(TAG,"rengleRect top()== "+rengleRect);
        mRect.top += dy;
        if (mRect.top < -offset) {
            mRect.top = -offset;
            //rengleRect.top = mRect.top;
        }
        if (mRect.bottom - mRect.top - 2 * offset < MININ_W_h) {
            mRect.top = mRect.bottom - 2 * offset - MININ_W_h;
            //rengleRect.top = mRect.top;
        }
        Log.d(TAG,"rengleRect2 top()== "+rengleRect);
    }


    private void bottom(View v, Rect mRect,  int dy) {
        /*oriBottom += dy;
        if (oriBottom > screenHeight + offset) {
            oriBottom = screenHeight + offset;
        }
        if (oriBottom - oriTop - 2 * offset < MININ_W_h) {
            oriBottom = MININ_W_h + oriTop + 2 * offset;
        }*/
        mRect.bottom += dy;
        if (mRect.bottom > screenHeight + offset) {
            mRect.bottom = screenHeight + offset;
        }
        if (mRect.bottom - mRect.top - 2 * offset < MININ_W_h) {
            mRect.bottom = MININ_W_h + mRect.top + 2 * offset;
        }
    }


    private void right(View v,Rect mRect, int dx) {
        /*oriRight += dx;
        if (oriRight > screenWidth + offset) {
            oriRight = screenWidth + offset;
        }
        if (oriRight - oriLeft - 2 * offset < MININ_W_h) {
            oriRight = oriLeft + 2 * offset + MININ_W_h;
        }*/

        mRect.right += dx;
        if (mRect.right > screenWidth + offset) {
            mRect.right = screenWidth + offset;
        }
        if (mRect.right - mRect.left - 2 * offset < MININ_W_h) {
            mRect.right = mRect.left + 2 * offset + MININ_W_h;
        }
    }


    private void left(View v, Rect mRect, int dx) {
        /*oriLeft += dx;
        if (oriLeft < -offset) {
            oriLeft = -offset;
        }
        if (oriRight - oriLeft - 2 * offset < MININ_W_h) {
            oriLeft = oriRight - 2 * offset - MININ_W_h;
        }*/

        mRect.left += dx;
        if (mRect.left < -offset) {
            mRect.left = -offset;
        }
        if (mRect.right - mRect.left - 2 * offset < MININ_W_h) {
            mRect.left = mRect.right - 2 * offset - MININ_W_h;
        }
    }


    protected int getDirection(View v, int x, int y) {
        int left = v.getLeft();
        int right = v.getRight();
        int bottom = v.getBottom();
        int top = v.getTop();
        if (x < TORERANCE && y < TORERANCE) {
            return LEFT_TOP;
        }
        if (y < TORERANCE && right - left - x < TORERANCE) {
            return RIGHT_TOP;
        }
        if (x < TORERANCE && bottom - top - y < TORERANCE) {
            return LEFT_BOTTOM;
        }
        if (right - left - x < TORERANCE && bottom - top - y < TORERANCE) {
            return RIGHT_BOTTOM;
        }
        if (x < TORERANCE) {
            return LEFT;
        }
        if (y < TORERANCE) {
            return TOP;
        }
        if (right - left - x < TORERANCE) {
            return RIGHT;
        }
        if (bottom - top - y < TORERANCE) {
            return BOTTOM;
        }
        return CENTER;
    }
    protected int getDirection(Rect mRect, int x, int y) {
        int left = mRect.left;
        int right = mRect.right;
        int bottom = mRect.bottom;
        int top = mRect.top;
        //Log.d(TAG,"getDirection mRect="+mRect.toString()+";;x=="+x+";;;y=="+y);
        if ((x>(left-TORERANCE) && x < (left+TORERANCE)) && (y>(top-TORERANCE) && y < (top+TORERANCE))) {
            Log.d(TAG,"getDirection1");
            return LEFT_TOP;
        }
        if ((y>(top-TORERANCE) && y < (top+TORERANCE)) && (x<(right+TORERANCE) && x> (right-TORERANCE))) {
            Log.d(TAG,"getDirection2");
            return RIGHT_TOP;
        }
        if ((x>(left-TORERANCE)&& x<(left+TORERANCE)) && (y>(bottom-TORERANCE) && y<(bottom+TORERANCE))) {
            Log.d(TAG,"getDirection3");
            return LEFT_BOTTOM;
        }
        if ((x>(right-TORERANCE) && x<(right+TORERANCE))&&(y>(bottom-TORERANCE) && y<(bottom+TORERANCE))) {
            Log.d(TAG,"getDirection4");
            return RIGHT_BOTTOM;
        }
        if ((x>(left-TORERANCE) && x < (left+TORERANCE))&&(y>(top+TORERANCE) && y<(bottom-TORERANCE))) {
            Log.d(TAG,"getDirection5");
            return LEFT;
        }
        if ((y>(top-TORERANCE) && y < (top+TORERANCE)) && (x>(left+TORERANCE) && x<(right-TORERANCE)) ) {
            Log.d(TAG,"getDirectiontop");
            return TOP;
        }
        if ((x>(right-TORERANCE)&& x<(right+TORERANCE))&&(y>(top+TORERANCE) && y<(bottom-TORERANCE))) {
            Log.d(TAG,"getDirection6");
            return RIGHT;
        }
        if ((y>(bottom-TORERANCE) && y<(bottom+TORERANCE))&&(x>(left+TORERANCE) && x<(right-TORERANCE))) {
            Log.d(TAG,"getDirection7");
            return BOTTOM;
        }

        if(x>(left+TORERANCE) && y> (top+TORERANCE) && x<(right-TORERANCE) && y<(bottom-TORERANCE)) {
            Log.d(TAG,"getDirection1");
            return CENTER;
        }

        return INVALID;
    }

    public int getCutWidth() {
        return getWidth() - 2 * offset;
    }


    public int getCutHeight() {
        return getHeight() - 2 * offset;
    }

    private void dubbleTabCapture(){
        if(service != null){
            service.dubbleTabCapture();
        }
    }
    public void setService(Service1 s1){
        service = s1;

    }

    public Rect getRengleRect(){
        /*rengleRect.left = oriLeft;
        rengleRect.top = oriTop;
        rengleRect.right = oriRight;
        rengleRect.bottom = oriBottom;*/
        Log.d(TAG,"getRengleRect"+rengleRect.left+","+rengleRect.top+","+rengleRect.right+","+rengleRect.bottom);
        return rengleRect;
    }
    public GraphicPath getGraphicPath(){
        return mGraphicPath;
    }
    public int getFunctionFlag(){
        return functionFlag;
    }
    public int getSharefunctionFlag(){
        return sharefunctionFlag;
    }


    private boolean isAreaContainPoint(Rect area,int x,int y){
        Rect newArea=new Rect(area.left-BUTTON_EXTRA_WIDTH,area.top-BUTTON_EXTRA_WIDTH,area.right+BUTTON_EXTRA_WIDTH,area.bottom+BUTTON_EXTRA_WIDTH);
        if (newArea.contains(x,y)){
            return true;
        }
        return false;
    }

}

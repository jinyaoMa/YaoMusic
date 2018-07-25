package ca.mohawk.ma.yaomusic.ui;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Connection;
import org.jsoup.Jsoup;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import ca.mohawk.ma.yaomusic.R;


public class RoundAngleImageView extends android.support.v7.widget.AppCompatImageView {

    private Paint paint;
    private int roundWidth = 50;
    private int roundHeight = 50;
    private Paint paint2;

    public static final int GET_DATA_SUCCESS = 1;
    public static final int NETWORK_ERROR = 2;
    public static final int SERVER_ERROR = 3;

    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what){
                case GET_DATA_SUCCESS:
                    Bitmap bitmap = (Bitmap) msg.obj;
                    setImageBitmap(bitmap);
                    break;
                case NETWORK_ERROR:
                    Log.d("RoundAngleImageView", "Network Error");
                    break;
                case SERVER_ERROR:
                    Log.d("RoundAngleImageView", "Server Error");
                    break;
            }
        }
    };

    public RoundAngleImageView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context, attrs);
    }

    public RoundAngleImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public RoundAngleImageView(Context context) {
        super(context);
        init(context, null);
    }

    public void setImageURL(final String path) {
        new Thread() {
            @Override
            public void run() {
                try {
                    URL url;

                    if (path.startsWith("kuwo:music:")) {
                        String kuwoMusicCoverBaseUrl = "http://img1.kwcdn.kuwo.cn/star/albumcover/";
                        Connection conn = Jsoup.connect("http://mobile.kuwo.cn/mpage/html5/songinfoandlrc").maxBodySize(0).ignoreContentType(true);
                        conn.data("mid", path.replace("kuwo:music:", ""));
                        String temp= conn.get().body().text();
                        String artpic = new JSONObject(temp).optJSONObject("songinfo").optString("artpic");
                        url = new URL(kuwoMusicCoverBaseUrl + artpic);
                    } else {
                        url = new URL(path);
                    }

                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                    connection.setRequestMethod("GET");
                    connection.setConnectTimeout(10000);
                    int code = connection.getResponseCode();
                    if (code == 200) {
                        InputStream inputStream = connection.getInputStream();
                        Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                        Message msg = Message.obtain();
                        msg.obj = bitmap;
                        msg.what = GET_DATA_SUCCESS;
                        handler.sendMessage(msg);
                        inputStream.close();
                    }else {
                        handler.sendEmptyMessage(SERVER_ERROR);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    handler.sendEmptyMessage(NETWORK_ERROR);
                }
            }
        }.start();
    }

    private void init(Context context, AttributeSet attrs) {

        if (attrs != null) {
            @SuppressLint("Recycle") TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.RoundAngleImageView);
            roundWidth = a.getDimensionPixelSize(R.styleable.RoundAngleImageView_roundWidth, roundWidth);
            roundHeight = a.getDimensionPixelSize(R.styleable.RoundAngleImageView_roundHeight, roundHeight);
        } else {
            float density = context.getResources().getDisplayMetrics().density;
            roundWidth = (int) (roundWidth * density);
            roundHeight = (int) (roundHeight * density);
        }

        paint = new Paint();
        paint.setColor(Color.WHITE);
        paint.setAntiAlias(true);
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_OUT));

        paint2 = new Paint();
        paint2.setXfermode(null);
    }

    @Override
    public void draw(Canvas canvas) {
        Bitmap bitmap = Bitmap.createBitmap(getWidth(), getHeight(), Config.ARGB_8888);
        Canvas canvas2 = new Canvas(bitmap);
        super.draw(canvas2);
        drawLeftUp(canvas2);
        drawLeftDown(canvas2);
        drawRightUp(canvas2);
        drawRightDown(canvas2);
        canvas.drawBitmap(bitmap, 0, 0, paint2);
        bitmap.recycle();
    }

    private void drawLeftUp(Canvas canvas) {
        Path path = new Path();
        path.moveTo(0, roundHeight);
        path.lineTo(0, 0);
        path.lineTo(roundWidth, 0);
        path.arcTo(new RectF(0, 0, roundWidth * 2, roundHeight * 2), -90, -90);
        path.close();
        canvas.drawPath(path, paint);
    }

    private void drawLeftDown(Canvas canvas) {
        Path path = new Path();
        path.moveTo(0, getHeight() - roundHeight);
        path.lineTo(0, getHeight());
        path.lineTo(roundWidth, getHeight());
        path.arcTo(new RectF(0, getHeight() - roundHeight * 2, roundWidth * 2, getHeight()), 90, 90);
        path.close();
        canvas.drawPath(path, paint);
    }

    private void drawRightDown(Canvas canvas) {
        Path path = new Path();
        path.moveTo(getWidth() - roundWidth, getHeight());
        path.lineTo(getWidth(), getHeight());
        path.lineTo(getWidth(), getHeight() - roundHeight);
        path.arcTo(new RectF(getWidth() - roundWidth * 2, getHeight() - roundHeight * 2, getWidth(), getHeight()), -0, 90);
        path.close();
        canvas.drawPath(path, paint);
    }

    private void drawRightUp(Canvas canvas) {
        Path path = new Path();
        path.moveTo(getWidth(), roundHeight);
        path.lineTo(getWidth(), 0);
        path.lineTo(getWidth() - roundWidth, 0);
        path.arcTo(new RectF(getWidth() - roundWidth * 2, 0, getWidth(), roundHeight * 2), -90, 90);
        path.close();
        canvas.drawPath(path, paint);
    }

}
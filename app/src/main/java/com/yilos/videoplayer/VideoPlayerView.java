package com.yilos.videoplayer;

import android.content.Context;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.webkit.URLUtil;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by yilos on 2015-11-18.
 */
public class VideoPlayerView extends RelativeLayout implements
        SurfaceHolder.Callback, MediaPlayer.OnCompletionListener, MediaPlayer.OnErrorListener,
        MediaPlayer.OnPreparedListener, MediaPlayer.OnSeekCompleteListener, MediaPlayer.OnInfoListener,
        MediaPlayer.OnVideoSizeChangedListener, View.OnClickListener {

    /**
     * 枚举视频地址的类型
     */
    public enum URITYPE {
        LOCAL, NETWORK, ONLINE;
    }

    private static final String TAG = "VideoPlayerView";
    /**
     * 上下文
     */
    private Context context;
    /**
     * 显示动画的View
     */
    private SurfaceView surfaceView;
    /**
     * 视频播放器
     */
    private MediaPlayer player;
    /**
     * 控制图层的Layout
     */
    private LinearLayout contraLayout;
    /**
     * 进度条
     */
    private SeekBar seekBar;
    /**
     * 播放时间
     */
    private TextView timeTextView;
    /**
     * 播放按钮
     */
    private ImageView playImageView;
    /**
     * 停止按钮
     */
    private ImageView stopImageView;
    /**
     * 重播按钮
     */
    private ImageView resetImageView;
    /**
     * 全屏按钮
     */
    private ImageView fullSrceenImageView;
    /**
     * 播放动画View的控制器
     */
    private SurfaceHolder surfaceHolder;
    /**
     * 播放出错后显示的图层
     */
    private FrameLayout playLayout;
    /**
     * 加载时显示的图层
     */
    private FrameLayout proLayout;
    /**
     * 播放出错后，重试按钮
     */
    private ImageView retryButton;
    /**
     * 播放进度值
     */
    private static int progressValue = 0;
    /**
     * 视频总长度
     */
    private static int videoLength;

    /**
     * 标识是否全屏
     */
    private boolean isFullScreen = false;
    /**
     * 改变全屏的监听
     */
    private OnChangeScreenListener onChangeScreenListener;

    /**
     * 标识是否正在播放
     */
    private boolean isPlaying = false;
    /**
     * uri地址
     */
    private String uriAddress;
    /**
     * 常量 定时隐藏控制图层
     */
    private static final int HIDE_CONTRE = 0;
    /**
     * 常量 定时获取播放进度
     */
    private static final int GET_PROGRESS = 1;
    /**
     * 定时器 定时获取进度
     */
    private Timer timer;
    /**
     * 任务 定时执行
     */
    private TimerTask task;

    /**
     * 视频地址类型
     */
    private URITYPE TYPE;

    public VideoPlayerView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.context = context;
        initView();
    }

    public VideoPlayerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.context = context;
        initView();
    }

    public VideoPlayerView(Context context) {
        super(context);
        this.context = context;
        initView();
    }

    /**
     * 初始化控件元素
     */
    private void initView() {
        initPlayer();
        View view = LayoutInflater.from(context).inflate(
                R.layout.video_player_layout, null);
        playLayout = (FrameLayout) view
                .findViewById(R.id.video_player_view_play_layout);
        proLayout = (FrameLayout) view
                .findViewById(R.id.video_player_view_pro_layout);
        retryButton = (ImageView) view.findViewById(R.id.video_player_view_retry);
        surfaceView = (SurfaceView) view
                .findViewById(R.id.video_player_view_surface);
        contraLayout = (LinearLayout) view
                .findViewById(R.id.video_player_view_control_layout);
        playImageView = (ImageView) view
                .findViewById(R.id.video_player_view_play);
        resetImageView = (ImageView) view
                .findViewById(R.id.video_player_view_reset);
        stopImageView = (ImageView) view
                .findViewById(R.id.video_player_view_stop);
        seekBar = (SeekBar) view.findViewById(R.id.video_player_view_seekbar);
        timeTextView = (TextView) view.findViewById(R.id.video_player_view_time);
        fullSrceenImageView = (ImageView) view
                .findViewById(R.id.video_player_view_fullscreen);
        surfaceHolder = surfaceView.getHolder();
        surfaceHolder.addCallback(this);
        addView(view);

        playImageView.setOnClickListener(this);
        stopImageView.setOnClickListener(this);
        resetImageView.setOnClickListener(this);
        fullSrceenImageView.setOnClickListener(this);
        surfaceView.setOnClickListener(this);
        retryButton.setOnClickListener(this);
        seekBar.setEnabled(false);
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // TODO Auto-generated method stub

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                // TODO Auto-generated method stub

            }

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress,
                                          boolean fromUser) {
                // TODO Auto-generated method stub
                if (fromUser) {
                    progressValue = progress;
                    Toast.makeText(context, "progressValue " + progress, Toast.LENGTH_SHORT)
                            .show();
                    player.seekTo(progress);
                }
            }
        });
    }

    /**
     * 开始定时器
     */
    private void startTimer() {
        if (timer == null) {
            timer = new Timer();
        }
        if (task == null) {
            task = new TimerTask() {
                @Override
                public void run() {
                    Message message = new Message();
                    message.what = GET_PROGRESS;
                    handler.sendMessage(message);
                }
            };
        }
        timer.schedule(task, 500, 500);
    }

    /**
     * 关闭定时器
     */
    private void stopTimer() {
        if (timer != null) {
            timer.cancel();
            timer.purge();
            timer = null;
        }
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    /**
     * 异步回调函数
     */
    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case HIDE_CONTRE:
                    contraLayout.setVisibility(View.GONE);
                    break;
                case GET_PROGRESS:
                    progressValue = player.getCurrentPosition();
                    seekBar.setProgress(progressValue);
                    timeTextView.setText(buildTimeMillis2MinutesSecond(progressValue) + "/" + buildTimeMillis2MinutesSecond(seekBar.getMax()));
                    break;
            }
        }
    };

    /**
     * 初始化mediaplayer
     */
    private void initPlayer() {
        if (player == null) {
            player = new MediaPlayer();
        }
        player.reset();
        player.setOnErrorListener(this);
        player.setOnCompletionListener(this);
        player.setOnInfoListener(this);
        player.setOnPreparedListener(this);
        player.setOnSeekCompleteListener(this);
        player.setOnVideoSizeChangedListener(this);
    }

    @Override
    public void onClick(View v) {
        // TODO Auto-generated method stub
        switch (v.getId()) {
            case R.id.video_player_view_retry:
                showProgressBar();
                playImageView.setImageResource(R.drawable.pause);
                playImageView.setEnabled(true);
                resetImageView.setEnabled(true);
                stopImageView.setEnabled(true);
                isPlaying = true;
                playVideo();
                break;
            case R.id.video_player_view_play:
                if (isPlaying) {
                    isPlaying = false;
                    playImageView.setImageResource(R.drawable.play);
                    stopTimer();
                    pause();
                } else {
                    isPlaying = true;
                    playImageView.setImageResource(R.drawable.pause);
                    if (progressValue > 0) {
                        startTimer();
                        player.seekTo(progressValue);
                        player.start();
                    } else {
                        playVideo();
                    }
                }
                break;
            case R.id.video_player_view_reset:
                isPlaying = true;
                reset();
                break;
            case R.id.video_player_view_stop:
                stopTimer();
                stop();
                break;
            case R.id.video_player_view_fullscreen:
                if (isFullScreen) {
                    isFullScreen = false;
                    shrinkScreen();
                } else {
                    isFullScreen = true;
                    fullScreen();
                }
                break;
            case R.id.video_player_view_surface:
                contraLayout.setVisibility(View.VISIBLE);
                handler.removeMessages(HIDE_CONTRE);
                handler.sendEmptyMessageDelayed(HIDE_CONTRE, 3000);
                break;
        }
    }

    /**
     * 全屏显示
     */
    private void fullScreen() {
//        if (onChangeScreenListener != null)
//            onChangeScreenListener.fullScreen();
//        ((SubscriptionDetailActivity) context)
//                .setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
    }

    /**
     * 全屏情况下返回
     */
    private void shrinkScreen() {
//        ((SubscriptionDetailActivity) context)
//                .setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
//        if (onChangeScreenListener != null)
//            onChangeScreenListener.shrinkScreen();
    }

    /**
     * 全屏或者缩放的监听
     */
    public interface OnChangeScreenListener {
        public void fullScreen();

        public void shrinkScreen();
    }

    /**
     * 暂停播放
     */
    private void pause() {
        progressValue = player.getCurrentPosition();
        player.pause();
    }

    /**
     * 停止播放
     */
    private void stop() {
        progressValue = 0;
        if (player.isPlaying()) {
            player.seekTo(0);
            player.pause();
        } else {
            seekBar.setProgress(0);
            player.pause();
        }
    }

    /**
     * 重播
     */
    private void reset() {
        progressValue = 0;
        if (player.isPlaying()) {
            player.seekTo(0);
            player.start();
        } else {
            playVideo();
        }
    }

    /**
     * 播放视频
     */
    public void playVideo() {
        if (this.uriAddress == null || uriAddress.equals("")) {
            Log.e(TAG, "播放地址为空~");
            return;
        }
        switch (TYPE) {
            case ONLINE:
                playOnline();
                break;
            case LOCAL:
                playLocation();
                break;
            case NETWORK:
                playNetWork();
                break;
        }

    }

    /**
     * 播放HTTP视频
     */
    private void playNetWork() {
        try {
            Log.d(TAG, "playNetWork 播放网络视频");
            initPlayer();
            player.setDataSource(context, Uri.parse(uriAddress));
            player.setDisplay(surfaceHolder);
        } catch (IllegalArgumentException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (SecurityException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IllegalStateException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        player.prepareAsync();
    }

    /**
     * 播放本地视频
     */
    private void playLocation() {
        try {
            Log.d(TAG, "playLocation 播放本地视频");
            initPlayer();
            player.setDataSource(uriAddress);
            player.setDisplay(surfaceHolder);
        } catch (IllegalArgumentException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (SecurityException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IllegalStateException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        player.prepareAsync();
    }

    /**
     * 播放在线视频 rtsp协议
     */
    private void playOnline() {
        try {
            Log.d(TAG, "playOnline 播放在线视频");
            initPlayer();
            player.setDataSource(context, Uri.parse(uriAddress));
            player.setDisplay(surfaceHolder);
        } catch (IllegalArgumentException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (SecurityException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IllegalStateException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        player.prepareAsync();

    }

    @Override
    public void onSeekComplete(MediaPlayer mp) {
        // TODO Auto-generated method stub
        Log.v(TAG, "onSeekComplete");
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        // TODO Auto-generated method stub
        Log.d(TAG, "onPrepared 预备完成~准备播放");
        Toast.makeText(context, "预备完成~准备播放", Toast.LENGTH_SHORT).show();
        cancleProgressBar();
        // 当prepare完成后，该方法触发，在这里我们播放视频

        // 首先取得video的宽和高
        if (isPlaying) {
            mp.start();
            startTimer();
        }
        mp.seekTo(progressValue);
        seekBar.setEnabled(true);
        videoLength = player.getDuration();
        seekBar.setMax(videoLength);
        seekBar.setProgress(progressValue);
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        // TODO Auto-generated method stub
        stopTimer();
        showErrorLayout();
        isPlaying = false;
        playImageView.setImageResource(R.drawable.play);
        playImageView.setEnabled(false);
        resetImageView.setEnabled(false);
        stopImageView.setEnabled(false);
        Log.d(TAG, "onError 视频播放错误~");
        Toast.makeText(context, "视频播放错误~", Toast.LENGTH_SHORT).show();
        switch (what) {
            case MediaPlayer.MEDIA_ERROR_SERVER_DIED:
                Log.v(TAG, "MEDIA_ERROR_SERVER_DIED");
                break;
            case MediaPlayer.MEDIA_ERROR_UNKNOWN:
                Log.v(TAG, "MEDIA_ERROR_UNKNOWN");
                break;
            default:
                break;
        }
        return true;
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        // TODO Auto-generated method stub
        isPlaying = false;
        playImageView.setImageResource(R.drawable.play);
        stopTimer();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width,
                               int height) {
        // TODO Auto-generated method stub
        Log.v(TAG, "surfaceChanged");
        player.setDisplay(holder);
//        playVideo();
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        // TODO Auto-generated method stub
        // 当SurfaceView中的Surface被创建的时候被调用
        // 在这里我们指定MediaPlayer在当前的Surface中进行播放
        Log.v(TAG, "surfaceCreated");
        // player.setDisplay(holder);
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        // TODO Auto-generated method stub
        Log.v(TAG, "surfaceDestroyed");
        if (player.isPlaying()) {
            isPlaying = true;
            progressValue = player.getCurrentPosition();
            player.stop();
            stopTimer();
        } else {
            player.stop();
        }
    }

    @Override
    public void onVideoSizeChanged(MediaPlayer mp, int width, int height) {
        // TODO Auto-generated method stub
        Log.v(TAG, "onVideoSizeChanged");
    }

    @Override
    public boolean onInfo(MediaPlayer mp, int what, int extra) {
        Log.v(TAG, "onInfo");
        // TODO Auto-generated method stub
        // 当一些特定信息出现或者警告时触发
        switch (what) {
            case MediaPlayer.MEDIA_INFO_BUFFERING_START:
                showProgressBar();
                break;
            case MediaPlayer.MEDIA_INFO_BUFFERING_END:
                cancleProgressBar();
                break;
            case MediaPlayer.MEDIA_INFO_BAD_INTERLEAVING:
                Log.d(TAG, "MEDIA_INFO_BAD_INTERLEAVING");
                break;
            case MediaPlayer.MEDIA_INFO_METADATA_UPDATE:
                break;
            case MediaPlayer.MEDIA_INFO_VIDEO_TRACK_LAGGING:
                break;
            case MediaPlayer.MEDIA_INFO_NOT_SEEKABLE:
                break;
        }
        return false;
    }

    /**
     * @return 返回视频地址
     */
    public String getUriAddress() {
        return uriAddress;
    }

    /**
     * 设置视频地址
     *
     * @param uriAddress 视频地址
     */
    public void setUriAddress(String uriAddress) {
        this.uriAddress = uriAddress;
        if (URLUtil.isNetworkUrl(uriAddress)) {
            TYPE = URITYPE.NETWORK;
        } else if (uriAddress.contains("rtsp://")) {
            TYPE = URITYPE.ONLINE;
        } else {
            TYPE = URITYPE.LOCAL;
        }
    }

    /**
     * 取消加载图层
     */
    private void cancleProgressBar() {
        Log.v(TAG, "cancleProgressBar");
        proLayout.setVisibility(View.GONE);
        surfaceView.setEnabled(true);
        playLayout.setVisibility(View.GONE);
    }

    /**
     * 显示加载图层
     */
    private void showProgressBar() {
        Log.v(TAG, "showProgressBar");
        proLayout.setVisibility(View.VISIBLE);
        surfaceView.setEnabled(false);
        playLayout.setVisibility(View.GONE);
    }

    /**
     * 显示加载视频错误图层
     */
    private void showErrorLayout() {
        Log.v(TAG, "showErrorLayout");
        playLayout.setVisibility(View.VISIBLE);
        surfaceView.setEnabled(false);
        proLayout.setVisibility(View.GONE);
    }

    private String buildTimeMillis2MinutesSecond(long timeMillis) {
        int second = (int) timeMillis / 1000;
        return new StringBuilder().append(second / 60).append(":").append(second % 60).toString();
    }

    /**
     * 设置全屏监听
     *
     * @param onChangeScreenListener 全屏监听
     */
    public void setOnChangeScreenListener(
            OnChangeScreenListener onChangeScreenListener) {
        this.onChangeScreenListener = onChangeScreenListener;
    }

}
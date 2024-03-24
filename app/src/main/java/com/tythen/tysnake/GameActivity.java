package com.tythen.tysnake;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.media.SoundPool;
import android.os.Bundle;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

import static com.tythen.tysnake.Constant.*;

public class GameActivity extends AppCompatActivity implements SurfaceHolder.Callback {

    private String direction = "right";
    private int foodX = 0, foodY = 0;
    private int score = 0;
    private TextView tv_score;
    private List<SnakePoint> snakePoints = new ArrayList();
    private SurfaceView surfaceView;
    private SurfaceHolder surfaceHolder;
    private Timer timer;
    private Canvas canvas = null;
    private Paint pointColor = null;
    private boolean gameOver;
    private SoundPool soundPool;
    private MediaPlayer mediaPlayer;
    private int eatingSoundId;
    private int deadSoundId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);
        surfaceView = findViewById(R.id.sv_game);
        surfaceView.getHolder().addCallback(this);
        tv_score = findViewById(R.id.tv_score);

        Button btn_up = findViewById(R.id.btn_up);
        Button btn_right = findViewById(R.id.btn_right);
        Button btn_left = findViewById(R.id.btn_left);
        Button btn_down = findViewById(R.id.btn_down);

        btn_up.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!direction.equals("down")) {
                    direction = "up";
                }
            }
        });
        btn_right.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!direction.equals("left")) {
                    direction = "right";
                }
            }
        });
        btn_left.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!direction.equals("right")) {
                    direction = "left";
                }
            }
        });
        btn_down.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!direction.equals("up")) {
                    direction = "down";
                }
            }
        });




        mediaPlayer = MediaPlayer.create(this, R.raw.bgm);
        mediaPlayer.setLooping(true);

        AudioAttributes audioAttributes = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();

        soundPool = new SoundPool.Builder()
                .setAudioAttributes(audioAttributes)
                .build();

        eatingSoundId = soundPool.load(this, R.raw.eating, 1);
        deadSoundId = soundPool.load(this,R.raw.dead,1);
    }
    @Override
    public void onResume() {
        super.onResume();
    }
    @Override
    protected void onPause() {
        super.onPause();
        if (mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mediaPlayer.release();
        soundPool.release();
    }
    @Override
    public void surfaceCreated(@NonNull SurfaceHolder surfaceHolder) {
        this.surfaceHolder = surfaceHolder;
        init();
    }

    @Override
    public void surfaceChanged(@NonNull SurfaceHolder surfaceHolder, int i, int i1, int i2) {

    }

    @Override
    public void surfaceDestroyed(@NonNull SurfaceHolder surfaceHolder) {

    }


    private void init() {
        snakePoints.clear();
        score = 0;
        tv_score.setText("0");
        direction = "right";
        int startX = 3 * pointSize;
        for (int i = 0; i < defaultTablePoints; i++) {
            SnakePoint snakePoint = new SnakePoint(startX, pointSize);
            snakePoints.add(snakePoint);
            startX -= 2 * pointSize;
        }
        addPoint();
        moveSnake();
        mediaPlayer.start();
    }


    private void addPoint() {
        int newFoodX = new Random().nextInt((surfaceView.getWidth() - 2 * pointSize) / pointSize);//左右两边各自留一个半径，然后除以半径得到一共有多少个点，然后随机生成一个点
        int newFoodY = new Random().nextInt((surfaceView.getHeight() - 2 * pointSize) / pointSize);
        if (newFoodX % 2 != 0) {
            newFoodX++;
        }
        if (newFoodY % 2 != 0) {
            newFoodY++;
        }
        foodX = (newFoodX * pointSize) + pointSize;
        foodY = (newFoodY * pointSize) + pointSize;
    }

    private void moveSnake() {
        timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {

                int headPositionX = snakePoints.get(0).getPositionX();
                int headPositionY = snakePoints.get(0).getPositionY();


                if (foodX == headPositionX && foodY == headPositionY) {
                    growSnake();
                    addPoint();
                    soundPool.play(eatingSoundId, 1.0f, 1.0f, 1, 0, 1.0f);
                }

                switch (direction) {
                    case "right":
                        snakePoints.get(0).setPositionX(headPositionX + pointSize * 2);
                        snakePoints.get(0).setPositionY(headPositionY);
                        break;
                    case "left":
                        snakePoints.get(0).setPositionX(headPositionX - pointSize * 2);
                        snakePoints.get(0).setPositionY(headPositionY);
                        break;
                    case "up":
                        snakePoints.get(0).setPositionX(headPositionX);
                        snakePoints.get(0).setPositionY(headPositionY - pointSize * 2);
                        break;
                    case "down":
                        snakePoints.get(0).setPositionX(headPositionX);
                        snakePoints.get(0).setPositionY(headPositionY + pointSize * 2);
                        break;
                }

                if (checkGameOver(headPositionX, headPositionY)) {
                    timer.purge();
                    timer.cancel();

                    soundPool.play(deadSoundId,1,1,1,0,1);
                    mediaPlayer.pause();

                    AlertDialog.Builder builder = new AlertDialog.Builder(GameActivity.this);
                    builder.setTitle("Joc încheiat");
                    builder.setCancelable(false);
                    builder.setMessage("Scorul tau este:" + score);
                    saveScore();
                    builder.setNegativeButton("Întoarcere", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            startActivity(new Intent(GameActivity.this,MainActivity.class));
                        }
                    });
                    builder.setPositiveButton("Repornire", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            init();
                        }
                    });

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            builder.show();
                        }
                    });
                    return;
                }
                else
                {

                    canvas = surfaceHolder.lockCanvas();
                    canvas.drawColor(Color.WHITE, PorterDuff.Mode.CLEAR);


                    int X = snakePoints.get(0).getPositionX();
                    int Y = snakePoints.get(0).getPositionY();
                    canvas.drawCircle(X, Y, pointSize, createPointColor());


                    canvas.drawCircle(foodX, foodY, pointSize, createPointColor());
                    for (int i = 1; i < snakePoints.size(); i++) {

                        int tempX = snakePoints.get(i).getPositionX();
                        int tempY = snakePoints.get(i).getPositionY();

                        snakePoints.get(i).setPositionX(headPositionX);
                        snakePoints.get(i).setPositionY(headPositionY);

                        canvas.drawCircle(snakePoints.get(i).getPositionX(),
                                snakePoints.get(i).getPositionY(),
                                pointSize,
                                createPointColor());

                        headPositionX = tempX;
                        headPositionY = tempY;
                    }
                }

                surfaceHolder.unlockCanvasAndPost(canvas);
            }
        }, 1000 - snakeMovingSpeed, 1000 - snakeMovingSpeed);

    }

    private void saveScore() {
        SharedPreferences shared = getSharedPreferences("score", Context.MODE_PRIVATE);
        int total = shared.getInt("total",0);
        ++total;
        SharedPreferences.Editor editor = shared.edit();
        Date date = new Date(System.currentTimeMillis());
        String nowDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(date);

        editor.putString(String.valueOf(total+"date"),nowDate);
        editor.putInt(String.valueOf(total+"score"),score);
        editor.putInt("total",total);
        editor.apply();
    }

    private void growSnake() {

        snakePoints.add(new SnakePoint(0, 0));

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                score++;
                tv_score.setText(String.valueOf(score));
            }
        });
    }

    private boolean checkGameOver(int headPositionX, int headPositionY) {
        boolean gameOver = false;
        if (snakePoints.get(0).getPositionX() < 0 ||
                snakePoints.get(0).getPositionX() > surfaceView.getWidth() ||
                snakePoints.get(0).getPositionY() < 0 ||
                snakePoints.get(0).getPositionY() > surfaceView.getHeight()) {
            gameOver = true;
        }
            for (int i = 1; i < snakePoints.size(); i++) {
                if (snakePoints.get(i).getPositionX() == headPositionX && snakePoints.get(i).getPositionY() == headPositionY) {
                    gameOver = true;
                }
            }

        return gameOver;
    }

    @SuppressLint("ResourceAsColor")
    private Paint createPointColor() {
        if (pointColor == null) {
            pointColor = new Paint();
            pointColor.setColor(Color.GREEN);
            pointColor.setStyle(Paint.Style.FILL);
            pointColor.setAntiAlias(true);
        }
        return pointColor;
    }
}
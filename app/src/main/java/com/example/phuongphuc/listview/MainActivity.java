package com.example.phuongphuc.listview;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.RunnableFuture;
import java.util.concurrent.TimeUnit;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.LogRecord;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private ListView lvItems;
    private ToggleButton btn_record;


    //File
    protected File Directory;
    private final static String DEFAULT_INITIAL_DIRECTORY = "/";
    private String directory;

    public String voiceStoragePath;

    //recorder
    MediaRecorder mediaRecorder;
    public boolean isRunning = false;
    private TextView tv_time;
    private long stTime;
    private long duration;
    Handler myHandler;


    //media player
    private Button b1,b2,b3,b4;
    private TextView tx1,tx2,tx3;
    private MediaPlayer mediaPlayer;
    private double startTime = 0;
    private double finalTime = 0;

    public Handler handler;
    private int forwardTime = 5000;
    private int backwardTime = 5000;
    private SeekBar seekBar;

    private static int oneTimeOnly = 0;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);



        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{
                    Manifest.permission.RECORD_AUDIO
                }, 10);

            }
        }



        File mydir = this.getDir("AudioFiles", Context.MODE_PRIVATE); //Creating an internal dir;
        if (!mydir.exists())
        {
            mydir.mkdirs();
        }



        btn_record = (ToggleButton) findViewById(R.id.btn_record);
        tv_time = (TextView)findViewById(R.id.tv_time);
        lvItems = (ListView) findViewById(R.id.lv_items);


        //media player
        b1 = (Button) findViewById(R.id.button1);
        b2 = (Button) findViewById(R.id.button2);
        b3 = (Button)findViewById(R.id.button3);
        b4 = (Button)findViewById(R.id.button4);

        tx1 = (TextView)findViewById(R.id.textView1);
        tx2 = (TextView)findViewById(R.id.textView2);
        tx3 = (TextView)findViewById(R.id.textView3);
        tx3.setText("");

        directory = this.getFilesDir().toString() + "/AudioFiles";

        b3.setEnabled(false);
        b1.setEnabled(false);
        b2.setEnabled(false);
        b4.setEnabled(false);
        refreshList();


        btn_record.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked){
                    if(mediaPlayer != null){

                        mediaPlayer.stop();
//                        mediaPlayer.release();
//                        mediaPlayer = null;

                    }

                    btn_record.setText("Stop");
                    stTime = System.currentTimeMillis();
                    isRunning = true;

                    setFileName();
                    initializeMediaRecord();

                    startAudioRecording();
                    updateTime();

                }else {
                    btn_record.setText("Record");
                    stopAudioRecording();
                    isRunning = false;
                    refreshList();
                }
            }
        });


        //time

        handler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                //Nhận nhãn của Button được gửi về từ tiến trình con
                String timetv = msg.obj.toString();
                //Khởi tạo 1 Button

                //Thiết lập text cho Button
                tv_time.setText(timetv);
            }
        };



//        mediaPlayer = MediaPlayer.create(this, R.raw.test);


        seekBar = (SeekBar)findViewById(R.id.seekBar);
        seekBar.setClickable(true);
        b2.setEnabled(false);

        b3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(getApplicationContext(), "Playing sound", Toast.LENGTH_SHORT).show();
                mediaPlayer.start();
                finalTime = mediaPlayer.getDuration();

                Log.e("thoi gian cua tap tin", Double.toString(finalTime));
                startTime = mediaPlayer.getCurrentPosition();
                seekBar.setMax((int) finalTime);

                if (oneTimeOnly == 0){
                    seekBar.setMax((int) finalTime);
                    oneTimeOnly = 1;
                }

                tx2.setText(String.format("%d min: %d: sec", TimeUnit.MILLISECONDS.toMinutes((long) finalTime),
                        TimeUnit.MILLISECONDS.toSeconds((long) finalTime -
                        TimeUnit.MILLISECONDS.toSeconds(TimeUnit.MILLISECONDS.toMinutes((long) finalTime)))));


                tx1.setText(String.format("%d min: %d sec",
                        TimeUnit.MILLISECONDS.toMinutes((long) startTime),
                        TimeUnit.MILLISECONDS.toSeconds((long) startTime) -
                                TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes((long)
                                        startTime)))
                );

                seekBar.setProgress((int)startTime);
                seekBar.postDelayed(UpdateSongTime, 100);
                b2.setEnabled(true);
                b3.setEnabled(false);

            }
        });


        b2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(getApplicationContext(), "Pausing sound",Toast.LENGTH_SHORT).show();
                mediaPlayer.pause();
                b2.setEnabled(false);
                b3.setEnabled(true);
            }
        });

        b4.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int temp = (int)startTime;

                if((temp+forwardTime)<=finalTime){
                    startTime = startTime + forwardTime;
                    mediaPlayer.seekTo((int) startTime);
                    Toast.makeText(getApplicationContext(),"You have Jumped forward 5 seconds",Toast.LENGTH_SHORT).show();
                }else{
                    Toast.makeText(getApplicationContext(),"Cannot jump forward 5 seconds",Toast.LENGTH_SHORT).show();
                }
            }
        });


        b1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int temp = (int)startTime;

                if((temp-backwardTime)>0){
                    startTime = startTime - backwardTime;
                    mediaPlayer.seekTo((int) startTime);
                    Toast.makeText(getApplicationContext(),"You have Jumped backward 5 seconds",Toast.LENGTH_SHORT).show();
                }else{
                    Toast.makeText(getApplicationContext(),"Cannot jump backward 5 seconds",Toast.LENGTH_SHORT).show();
                }
            }
        });

    }

    private Runnable UpdateSongTime = new Runnable() {
        public void run() {
            startTime = mediaPlayer.getCurrentPosition();
            tx1.setText(String.format("%d min: %d sec",
                    TimeUnit.MILLISECONDS.toMinutes((long) startTime),
                    TimeUnit.MILLISECONDS.toSeconds((long) startTime) -
                            TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.
                                    toMinutes((long) startTime)))
            );
            seekBar.setProgress((int)startTime);
            seekBar.postDelayed(this, 100);

            if (!mediaPlayer.isPlaying()){
                b2.setEnabled(false);
                b3.setEnabled(true);
//                Log.e("time", Integer.toString((int)finalTime));
            }
            //Log.e("progress", Integer.toString(seekBar.getProgress()));

        }
    };


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode){
            case 10: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    setFileName();
                    initializeMediaRecord();
                }
            }
        }
    }

    public File[] getFiles(String DirectoryPath) {
        File f = new File(DirectoryPath);
        f.mkdirs();
        File[] file = f.listFiles();
        return file;
    }

    public ArrayList<String> getFileNames(File[] file){
        ArrayList<String> arrayFiles = new ArrayList<String>();
        if (file.length == 0)
            return null;
        else {
            for (int i=0; i<file.length; i++)
                arrayFiles.add(file[i].getName());
        }

        return arrayFiles;
    }

    public void setAdapter(final List<String> list){
        //Create Adapter with data and row
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, list);

        //set Adapter for ListView
        lvItems.setAdapter(adapter);

        lvItems.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Toast.makeText(MainActivity.this, list.get(position), Toast.LENGTH_SHORT).show();
                tx3.setText(list.get(position).toString());
                String source = MainActivity.this.getFilesDir().toString() + "/AudioFiles/" + list.get(position).toString();
                mediaPlayer = new MediaPlayer();
                try {
                    mediaPlayer.setDataSource(source);
                    mediaPlayer.prepare();
                    b3.setEnabled(true);
                    b1.setEnabled(true);
                    b4.setEnabled(true);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });

        lvItems.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                Toast.makeText(MainActivity.this, list.get(position), Toast.LENGTH_SHORT).show();
                return false;
            }
        });
    }





    private void initializeMediaRecord(){
        mediaRecorder = new MediaRecorder();
        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.AAC_ADTS);
        mediaRecorder.setAudioEncoder(MediaRecorder.OutputFormat.AMR_NB);
        mediaRecorder.setOutputFile(voiceStoragePath);
    }

    private void startAudioRecording(){
        try {
            mediaRecorder.prepare();
            mediaRecorder.start();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void stopAudioRecording(){
        if(mediaRecorder != null){
            mediaRecorder.stop();
            mediaRecorder.release();
            mediaRecorder = null;
        }
    }


    private void refreshList(){
        //get directory
        File[] files = getFiles(directory);

        final List<String> listFiles = getFileNames(files);

        //
        if(listFiles != null){
            setAdapter(listFiles);
        }
        else{

            final List<String> noItem = Arrays.asList("no Audio Files");
            b3.setEnabled(false);
            b1.setEnabled(false);
            b2.setEnabled(false);
            b4.setEnabled(false);
            setAdapter(noItem);
        }
    }


//    buttonPlayLastRecordAudio.setOnClickListener(new View.OnClickListener() {
//        @Override
//        public void onClick(View view) throws IllegalArgumentException,
//                SecurityException, IllegalStateException {
//
//            buttonStop.setEnabled(false);
//            buttonStart.setEnabled(false);
//            buttonStopPlayingRecording.setEnabled(true);
//
//            mediaPlayer = new MediaPlayer();
//            try {
//                mediaPlayer.setDataSource(AudioSavePathInDevice);
//                mediaPlayer.prepare();
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//
//            mediaPlayer.start();
//            Toast.makeText(MainActivity.this, "Recording Playing",
//                    Toast.LENGTH_LONG).show();
//        }
//    });

    private void updateTime() {

//        final int numberButton = Integer.parseInt(edtNumberButton.getText().toString());
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {

                while (isRunning){
                    Message msg = handler.obtainMessage();

                    long time = System.currentTimeMillis() - stTime;
                    String txtime = String.format("%d min: %d sec", TimeUnit.MILLISECONDS.toMinutes((long) time),
                            TimeUnit.MILLISECONDS.toSeconds((long) time -
                                    TimeUnit.MILLISECONDS.toSeconds(TimeUnit.MILLISECONDS.toMinutes((long) time))));
                    msg.obj = txtime;

                    handler.sendMessage(msg);
//                }

                }
            }
        });
        //start thread
        thread.start();
    }


    public void setFileName(){
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");
        String date = df.format(Calendar.getInstance().getTime());

        voiceStoragePath = this.getFilesDir().toString() + "/AudioFiles/" + date + ".aac";
    }

}

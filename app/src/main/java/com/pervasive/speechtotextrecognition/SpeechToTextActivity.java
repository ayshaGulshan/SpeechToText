package com.pervasive.speechtotextrecognition;

import static android.Manifest.permission.RECORD_AUDIO;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

public class SpeechToTextActivity extends AppCompatActivity {


    /**
     * EditText for displaying the converted text.
     */
    EditText mConvertedText;

    /**
     * Textview for displaying the status of activity.
     */
    TextView mStatusText;

    /**
     * MediaPlayer for playing the saved text
     */
    private MediaPlayer mPlayer;

    /**
     * Variable to store recognizer intent object
     */
    Intent mSpeechRecognizerIntent;

    /**
     * Variable to store constraintLayout for icons
     */
    ConstraintLayout mStartLayout,mPlayLayout,mStopPlayLayout;

    @SuppressLint({"MissingInflatedId", "WrongViewCast"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mConvertedText = findViewById(R.id.text_converted);
        mStartLayout = findViewById(R.id.start_record_layout);
        mPlayLayout = findViewById(R.id.play_record_layout);
        mStopPlayLayout = findViewById(R.id.stop_play_layout);
        mStatusText = findViewById(R.id.status_text);
        mStopPlayLayout.setBackgroundColor(getResources().getColor(R.color.grey_disabled));
        mPlayLayout.setBackgroundColor(getResources().getColor(R.color.grey_disabled));
        mSpeechRecognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        mSpeechRecognizerIntent.putExtra("android.speech.extra.GET_AUDIO_FORMAT", "audio/AMR");
        mSpeechRecognizerIntent.putExtra("android.speech.extra.GET_AUDIO", true);
        mStatusText.setText("Tap start record");

    }

    /**
     * Method called to play the audio file
     * @param view
     */
    public void playOnClick(View view){
        mStartLayout.setEnabled(false);
        mPlayLayout.setEnabled(false);
        mStopPlayLayout.setEnabled(true);
        mStopPlayLayout.setBackgroundColor(getResources().getColor(R.color.PeachPuff));
        mStartLayout.setBackgroundColor(getResources().getColor(R.color.grey_disabled));
        mPlayLayout.setBackgroundColor(getResources().getColor(R.color.grey_disabled));
        mPlayer = new MediaPlayer();
        try {
            mPlayer.setDataSource(getAudioFileRecordingPath());
            mPlayer.prepare();
            mPlayer.start();
        } catch (IOException e) {
            Log.e("TAG", "IO exception");
        }

        mStatusText.setText("Playing recorded audio");

    }


    /**
     * Method called to start  speech record and recognition
     * @param view
     */
    public void startRecordOnClick(View view){
        if (checkPermissions()) {
            mStatusText.setText("Recording started");
            startActivityForResult(mSpeechRecognizerIntent, 0);
        } else{
            requestPermissions();
        }
    }

    /**
     * Override method to get the result of start activity
     * @param requestCode
     * @param resultCode
     * @param data
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 0 && resultCode == RESULT_OK) {
            Bundle audioBundle = data.getExtras();
            ArrayList<String> text = audioBundle.getStringArrayList(RecognizerIntent.EXTRA_RESULTS);
            mConvertedText.setText(text.get(0));
            writeAudioFileToMemory(data);
            writeTextToMemory(text.get(0));
            mStartLayout.setEnabled(true);
            mPlayLayout.setEnabled(true);
            mStopPlayLayout.setEnabled(false);
            mStopPlayLayout.setBackgroundColor(getResources().getColor(R.color.grey_disabled));
            mStartLayout.setBackgroundColor(getResources().getColor(R.color.PeachPuff));
            mPlayLayout.setBackgroundColor(getResources().getColor(R.color.PeachPuff));
            mStatusText.setText("Recording complete");
        }
    }

    /**
     * Method to stop playing the speech
     * @param view
     */
    public void stopPlayOnClick(View view){
        mStartLayout.setEnabled(true);
        mPlayLayout.setEnabled(true);
        mStopPlayLayout.setEnabled(false);
        mStopPlayLayout.setBackgroundColor(getResources().getColor(R.color.grey_disabled));
        mStartLayout.setBackgroundColor(getResources().getColor(R.color.PeachPuff));
        mPlayLayout.setBackgroundColor(getResources().getColor(R.color.PeachPuff));
        mStatusText.setText("Recorded audio play stopped");
        mPlayer.stop();
        mPlayer.release();
    }

    /**
     * Method to check the permissions
     * @return  permission status
     */
    public boolean checkPermissions(){
        int writePermission = ContextCompat.checkSelfPermission(getApplicationContext(),WRITE_EXTERNAL_STORAGE);
        int recordPermission = ContextCompat.checkSelfPermission(getApplicationContext(),RECORD_AUDIO );
        return writePermission == PackageManager.PERMISSION_GRANTED && recordPermission== PackageManager.PERMISSION_GRANTED;
    }

    /**
     * Method for request permissions for write and record audio
     */
    public void requestPermissions() {
        ActivityCompat.requestPermissions(SpeechToTextActivity.this,new String[]{RECORD_AUDIO,WRITE_EXTERNAL_STORAGE},
                1);
    }

    /**
     * Method to write the audio file to memory.
     * @param data
     */
    public void writeAudioFileToMemory(Intent data) {
        Uri audioU = data.getData();
        ContentResolver contentResolver = getContentResolver();
        try {
            InputStream fileInputStream = contentResolver.openInputStream(audioU);
            FileOutputStream audioOutputStream = new FileOutputStream(getAudioFileRecordingPath());
            byte[] buffer = new byte[99999];
            int read;
            while ((read = fileInputStream.read(buffer)) != -1) {
                audioOutputStream.write(buffer, 0, read);
            }
            fileInputStream.close();
            audioOutputStream.flush();
            audioOutputStream.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Method to save the converted text to memory
     * @param convertedText
     */
    public void writeTextToMemory(String convertedText) {
        try {
            FileOutputStream txtOutputStream = null;
            txtOutputStream = new FileOutputStream(getTextStoragePath());
            txtOutputStream.write(convertedText.getBytes());
            Toast.makeText(this,
                    "text saved to" + getTextStoragePath(), Toast.LENGTH_LONG).show();
            txtOutputStream.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Method to get file recording path
     * @return filepath
     */
    private String getAudioFileRecordingPath() {
        ContextWrapper contextWrapper = new ContextWrapper(getApplicationContext());
        File musicDirectory = contextWrapper.getExternalFilesDir(Environment.DIRECTORY_MUSIC);
        File file = new File(musicDirectory, "audio_record" + ".amr");
        return file.getPath();
    }

    /**
     * Method to get the path for storing the converted text file.
     * @return filepath
     */
    private String getTextStoragePath() {
        ContextWrapper contextWrapper = new ContextWrapper(getApplicationContext());
        File docDirectory = contextWrapper.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);
        File file = new File(docDirectory, "audio_text" + ".txt");
        return file.getPath();
    }

    /**
     * Method to get the permission granted
     * @param requestCode The request code passed in {@link #(
     * android.app.Activity, String[], int)}
     * @param permissions The requested permissions. Never null.
     * @param grantResults The grant results for the corresponding permissions
     *     which is either {@link android.content.pm.PackageManager#PERMISSION_GRANTED}
     *     or {@link android.content.pm.PackageManager#PERMISSION_DENIED}. Never null.
     *
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode== 1 && grantResults.length > 0) {
            boolean permissionToRecord = grantResults[0] == PackageManager.PERMISSION_GRANTED;
            boolean permissionToStore = grantResults[1] == PackageManager.PERMISSION_GRANTED;
            if (permissionToRecord && permissionToStore) {
                Toast.makeText(getApplicationContext(), "Permission Granted", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(getApplicationContext(), "Permission Denied", Toast.LENGTH_LONG).show();
            }
        }
    }

}
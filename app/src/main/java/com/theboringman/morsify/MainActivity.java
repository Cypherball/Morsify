package com.theboringman.morsify;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraManager;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Bundle;
import android.os.Vibrator;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.util.HashMap;

public class MainActivity extends AppCompatActivity {

    private String[] ALPHA = {"a","b","c","d","e","f","g","h","i","j","k","l","m","n","o","p","q","r","s","t","u","v","w","x","y","z",
            "0","1","2","3","4","5","6","7","8","9",
            ".",",","?","'","!","/","(",")","&",":",";","=","+","-","_","\"","$","@"};
    private String[] MORSE = {"·−","−···","−·−·","−··","·","··−·","−−·","····","··","·−−−","−·−","·−··","−−","−·","−−−","·−−·","−−·−","·−·","···","−","··−","···−","·−−","−··−","−·−−","−−··","−−−−−","·−−−−","··−−−","···−−","····−","·····","−····","−−···","−−−··","−−−−·","·−·−·−","−−··−−","··−−··","·−−−−·","−·−·−−","−··−·","−·−−·","−·−−·−","·−···","−−−···","−·−·−·","−···−","·−·−·","−····−","··−−·−","·−··−·","···−··−","·−−·−·"};
    private HashMap<String,String> alphaToMorse = new HashMap<>();
    private int frequency = 600;

    private Toolbar mToolbar;
    private TextInputEditText alphaInput;
    private EditText frequencyInput;
    private TextView morseField, copy;
    private MaterialButton convertButton;
    private SeekBar seekBar;
    private TextView[] speedValues;

    private static String TAG = "MainActivity";

    private static final int CAMERA_REQUEST = 123;
    boolean hasCameraFlash = false;
    boolean vibrateChecked, flashChecked, soundChecked;

    float feedbackSpeed = 1.0f;

    private boolean stopThread = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        createHashMap();
        initUI();

        convertButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(convertButton.getText() == "Stop Feedback"){
                    stopThread = true;
                    convertButton.setText("Convert");
                } else{
                    convertToMorse();
                }
            }
        });

        copy.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String morseText = morseField.getText().toString();
                if(!morseText.isEmpty()){
                    ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                    ClipData clip = ClipData.newPlainText("Copied Morse Text", morseField.getText().toString());
                    try{
                        clipboard.setPrimaryClip(clip);
                        Toast.makeText(MainActivity.this, "Copied!", Toast.LENGTH_SHORT).show();
                    } catch (Exception e){e.printStackTrace();}
                }
            }
        });

        frequencyInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                String frequencyString = frequencyInput.getText().toString().trim();
                if(!frequencyString.isEmpty()) {
                    if (Integer.parseInt(frequencyString) < 45000 && Integer.parseInt(frequencyString) > 0) {
                        frequency = Integer.parseInt(frequencyString);
                    }
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                resetSelectedSpeedValue(speedValues);
                setSeekBarSelectedValue(speedValues);
                setFeedbackSpeed();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
    }

    private void initUI() {
        alphaInput = findViewById(R.id.alpha_input);  //alphanumeric text input
        morseField = findViewById(R.id.morse_text);  //text view to display morse code
        convertButton = findViewById(R.id.button_convertToMorse);  //button to convert to morse or stop feedback thread
        seekBar = findViewById(R.id.speed_seekBar);  //feedback speed control seekbar
        frequencyInput = findViewById(R.id.frequencyInput);
        copy = findViewById(R.id.copy); //used as a copy button to copy morse text to clipboard
        mToolbar = findViewById(R.id.home_toolbar);
        setSupportActionBar(mToolbar);

        morseField.setMovementMethod(new ScrollingMovementMethod());
        alphaInput.requestFocus();

        //seekbar  text view values indicating discrete intervals
        TextView speedValue0 = findViewById(R.id.speed0);
        TextView speedValue1 = findViewById(R.id.speed1);
        TextView speedValue2 = findViewById(R.id.speed2);
        TextView speedValue3 = findViewById(R.id.speed3);
        TextView speedValue4 = findViewById(R.id.speed4);
        TextView speedValue5 = findViewById(R.id.speed5);
        TextView speedValue6 = findViewById(R.id.speed6);
        speedValues = new TextView[]{speedValue0,speedValue1,speedValue2,speedValue3,speedValue4,speedValue5,speedValue6};

        //initial feedback speed seekbar setup
        seekBar.setProgress(1);
        resetSelectedSpeedValue(speedValues);
        setSeekBarSelectedValue(speedValues);
        setFeedbackSpeed();

        frequencyInput.setText("600");

        //get camera permission for enabling flash feedback
        ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.CAMERA}, CAMERA_REQUEST);
        hasCameraFlash = getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH);

        //initialize feedback options checkboxes
        CheckBox soundCheckbox = findViewById(R.id.soundCheckbox);
        CheckBox flashCheckbox = findViewById(R.id.flashCheckbox);
        CheckBox vibrateCheckbox = findViewById(R.id.vibrateCheckbox);

        soundChecked = soundCheckbox.isChecked();
        flashChecked = flashCheckbox.isChecked();
        vibrateChecked = vibrateCheckbox.isChecked();
    }

    private void setFeedbackSpeed() {   //determine feedback speed through seekbar value
        switch (seekBar.getProgress()){
            case 0:
                feedbackSpeed = 0.25f;
                break;
            case 1:
                feedbackSpeed = 0.5f;
                break;
            case 2:
                feedbackSpeed = 0.75f;
                break;
            case 3:
                feedbackSpeed = 1.0f;
                break;
            case 4:
                feedbackSpeed = 1.25f;
                break;
            case 5:
                feedbackSpeed = 1.5f;
                break;
            case 6:
                feedbackSpeed = 1.75f;
                break;
            default:
                feedbackSpeed = 1.0f;
                seekBar.setProgress(3);
                break;
        }
    }

    private void setSeekBarSelectedValue(TextView[] values) {
        int progress = seekBar.getProgress();
        if(progress > -1 && progress < 7){
            values[progress].setTextSize(18);
            values[progress].setTextColor(getColor(R.color.colorText));
        } else{
            Toast.makeText(this, "Error setting progress value.", Toast.LENGTH_SHORT).show();
            Log.d(TAG, "setSeekBarSelectedValue: Progress value exceeds or is below defined progress range");
        }
    }

    private void resetSelectedSpeedValue(TextView[] values) {
        for(int i =0; i<values.length; i++){
            values[i].setTextSize(15);
            values[i].setTextColor(Color.GRAY);
        }
    }

    private void convertToMorse() {
        if(alphaInput.getText().toString().trim().isEmpty()){   //Condition for empty textbox
            Toast.makeText(this,"Please enter alphanumeric text!",Toast.LENGTH_SHORT).show();
            return;
        }
        char[] alphaText = alphaInput.getText().toString().trim().toCharArray();    //get and convert alphanum input of textbox to char array
        StringBuilder morseText = new StringBuilder();  //string builder for converting to morse text inside for loop
        for(int i=0;i<alphaText.length;i++){
            if(alphaText[i] == ' '){
                morseText.append("     ");
            } else {
                String morseEquivalant = alphaToMorse.get(Character.toString(alphaText[i]).toLowerCase());
                if(morseEquivalant!=null) {
                    morseText.append(morseEquivalant + " ");   //get morse code from hash map
                } else {
                    Toast.makeText(this, "Please enter valid symbols only.", Toast.LENGTH_SHORT).show();
                }
            }
        }
        morseField.setText(morseText.toString());
        if(soundChecked || vibrateChecked || flashChecked){
            deviceFeedbackThread feedbackThread = new deviceFeedbackThread(morseText.toString());
            new Thread(feedbackThread).start();
        }

    }

    private void createHashMap() {
        for(int i=0;i<ALPHA.length && i<MORSE.length;i++){
            alphaToMorse.put(ALPHA[i],MORSE[i]);
            Log.d(TAG, "createHashMap: " + ALPHA[i] + ", " + MORSE[i]);
        }
    }

    private void flashLightOn() {
        CameraManager cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            String cameraId = cameraManager.getCameraIdList()[0];
            cameraManager.setTorchMode(cameraId, true);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void flashLightOff() {
        CameraManager cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            String cameraId = cameraManager.getCameraIdList()[0];
            cameraManager.setTorchMode(cameraId, false);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private class deviceFeedbackThread implements Runnable{
        String morseText;
        float oneTimeUnit = 100.0f;

        //coded according to International Morse Code Standards
        float dotStandardTime = oneTimeUnit;
        float dashStandardTime = oneTimeUnit * 3;
        float symbolSpaceStandardWait = oneTimeUnit;
        float letterSpaceStandardWait = oneTimeUnit*3;
        float wordSpaceStandardWait = oneTimeUnit*7;

        int dotFeedbackTime;
        int dashFeedbackTime;
        int symbolSpaceSleepTime;
        int letterSpaceSleepTime;
        int wordSpaceSleepTime;

        deviceFeedbackThread(String morseText){
            this.morseText = morseText;
        }

        @Override
        public void run() {
            stopThread = false;
            startFeedback();
        }

        private void startFeedback() {
            Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
            boolean feedbackContinuationCondition = !stopThread && (flashChecked || soundChecked || vibrateChecked);
            convertButton.setText("Stop Feedback"); //set the button text to STOP

            for(int i=0; i<morseText.length() && feedbackContinuationCondition;i++){
                //Adjust speed of feedback
                dotFeedbackTime = (int) (dotStandardTime/feedbackSpeed);
                dashFeedbackTime = (int) (dashStandardTime/feedbackSpeed);
                symbolSpaceSleepTime = (int) (symbolSpaceStandardWait/feedbackSpeed);
                letterSpaceSleepTime = (int) (letterSpaceStandardWait/feedbackSpeed);
                wordSpaceSleepTime = (int) ((wordSpaceStandardWait/feedbackSpeed)/5);

                //Feedback conditions
                if(morseText.toCharArray()[i]=='·'){
                    generateFeedbacks(dotFeedbackTime,symbolSpaceSleepTime);
                } else if(morseText.toCharArray()[i]=='−'){
                    generateFeedbacks(dashFeedbackTime,symbolSpaceSleepTime);
                } else if(morseText.toCharArray()[i]==' '){
                    try {
                        if(morseText.toCharArray()[i+1]!=' '){
                            Thread.sleep(letterSpaceSleepTime);
                        }
                        else{
                            Thread.sleep(wordSpaceSleepTime);
                        }
                    } catch (Exception e){
                        Log.e(TAG, "convertToMorse: " + e);
                    }
                } else{
                    try {
                        Thread.sleep(letterSpaceSleepTime);
                        Log.d(TAG, "FeedbackThread: Invalid AlphaNumeric Symbol");
                    } catch (Exception e){
                        Log.e(TAG, "convertToMorse: " + e);
                    }
                }
                feedbackContinuationCondition = !stopThread && (flashChecked || soundChecked || vibrateChecked);
            }
            convertButton.setText("Convert");
            Thread.currentThread().interrupt();
        }

        private void generateFeedbacks(int feedbackTime, int sleepTime){
            Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
            try {
                if(vibrateChecked) {v.vibrate(feedbackTime);}
                if(hasCameraFlash && flashChecked){
                    flashLightOn();
                }
                Thread toneThread;
                PlayTone playTone = new PlayTone();
                boolean soundOn = false;
                if(soundChecked){
                    toneThread = new Thread(playTone);
                    toneThread.start();
                    soundOn = true;
                }
                Thread.sleep(feedbackTime);
                if(soundOn)
                    playTone.stopTune();
                if(hasCameraFlash)
                    flashLightOff();
                Thread.sleep(sleepTime);
            } catch (Exception e){
                Log.e(TAG, "convertToMorse: " + e);
            }
        }

        private void stop(){
            stopThread = true;
        }
    }

    public class PlayTone implements Runnable{
        private boolean isRunning;
        private int sampleRate = 44100;
        private double toneFreq = frequency;
        AudioTrack audioTrack;

        @Override
        public void run() {
            isRunning = true;
            int buffsize = AudioTrack.getMinBufferSize(sampleRate,
                    AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_16BIT);
            // create an audiotrack object
            audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC,
                    sampleRate, AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT, buffsize,
                    AudioTrack.MODE_STREAM);

            short samples[] = new short[buffsize];
            int amp = 10000;
            double twopi = 8.*Math.atan(1.);
            double ph = 0.0;

            // start audio
            audioTrack.play();

            // synthesis loop
            while(isRunning){
                double fr = toneFreq;
                for(int i=0; i < buffsize; i++){
                    samples[i] = (short) (amp*Math.sin(ph));
                    ph += twopi*fr/ sampleRate;
                }
                audioTrack.write(samples, 0, buffsize);
            }

        }

        public double getToneFreq() {
            return toneFreq;
        }

        public void setToneFreq(double toneFreq) {
            this.toneFreq = toneFreq;
        }

        public boolean isRunning() {
            return isRunning;
        }

        private void stopTune() {
            isRunning = false;
            audioTrack.stop();
            audioTrack.release();
            Thread.currentThread().interrupt();
        }
    }


    public void checkboxClicked(View view){
        boolean checked = ((CheckBox) view).isChecked();
        switch(view.getId()){
            case R.id.vibrateCheckbox:
                if(checked)
                    vibrateChecked = true;
                else
                    vibrateChecked = false;
                break;
            case R.id.soundCheckbox:
                if(checked)
                    soundChecked = true;
                else
                    soundChecked = false;
                break;
            case R.id.flashCheckbox:
                if(checked)
                    flashChecked = true;
                else {
                    flashLightOff();
                    flashChecked = false;
                }
                break;
            default:
                Log.d(TAG, "checkboxClicked: Invalid checkbox ID");
                Toast.makeText(this, "Checkbox Error!", Toast.LENGTH_SHORT).show();
                break;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case CAMERA_REQUEST:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    hasCameraFlash = getPackageManager().
                            hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH);
                    if(!hasCameraFlash){
                        Toast.makeText(this,"Error! Camera Flash may not be available.",Toast.LENGTH_LONG).show();
                    }
                } else {
                    Toast.makeText(MainActivity.this, "Permission Denied for the Camera", Toast.LENGTH_SHORT).show();
                }
                break;
        }
    }
}
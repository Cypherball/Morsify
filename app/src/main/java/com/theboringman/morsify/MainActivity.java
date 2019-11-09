package com.theboringman.morsify;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraManager;
import android.os.Bundle;
import android.os.Vibrator;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.util.HashMap;

import static java.lang.Thread.sleep;

public class MainActivity extends AppCompatActivity {

    private String[] ALPHA = {"a","b","c","d","e","f","g","h","i","j","k","l","m","n","o","p","q","r","s","t","u","v","w","x","y","z",
            "0","1","2","3","4","5","6","7","8","9",
            ".",",","?","'","!","/","(",")","&",":",";","=","+","-","_","\"","$","@"};
    private String[] MORSE = {"·−","−···","−·−·","−··","·","··−·","−−·","····","··","·−−−","−·−","·−··","−−","−·","−−−","·−−·","−−·−","·−·","···","−","··−","···−","·−−","−··−","−·−−","−−··","−−−−−","·−−−−","··−−−","···−−","····−","·····","−····","−−···","−−−··","−−−−·","·−·−·−","−−··−−","··−−··","·−−−−·","−·−·−−","−··−·","−·−−·","−·−−·−","·−···","−−−···","−·−·−·","−···−","·−·−·","−····−","··−−·−","·−··−·","···−··−","·−−·−·"};
    private HashMap<String,String> alphaToMorse = new HashMap<>();

    private TextInputEditText alphaInput;
    private TextView morseField;
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
        resetSelectedSpeedValue(speedValues);
        setSeekBarSelectedValue(speedValues);
        setFeedbackSpeed();

        //get camera permission for enabling flash feedback
        ActivityCompat.requestPermissions(MainActivity.this,
                new String[]{Manifest.permission.CAMERA}, CAMERA_REQUEST);
        hasCameraFlash = getPackageManager().
                hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH);

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
                feedbackSpeed = 1.75f;
                break;
            case 6:
                feedbackSpeed = 2.0f;
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
            values[progress].setTextSize(20);
        } else{
            Toast.makeText(this, "Error setting progress value.", Toast.LENGTH_SHORT).show();
            Log.d(TAG, "setSeekBarSelectedValue: Progress value exceeds or is below defined progress range");
        }
    }

    private void resetSelectedSpeedValue(TextView[] values) {
        for(int i =0; i<values.length; i++){
            values[i].setTextSize(15);
        }
    }

    private void convertToMorse() {
        if(alphaInput.getText().toString().trim().isEmpty()){   //Condition for empty textbox
            Toast.makeText(this,"Please enter alphanumeric text!",Toast.LENGTH_SHORT);
            return;
        }
        char[] alphaText = alphaInput.getText().toString().trim().toCharArray();    //get and convert alphanum input of textbox to char array
        StringBuilder morseText = new StringBuilder();  //string builder for converting to morse text inside for loop
        for(int i=0;i<alphaText.length;i++){
            if(alphaText[i] == ' '){
                morseText.append("     ");
            } else {
                morseText.append(alphaToMorse.get(Character.toString(alphaText[i]).toLowerCase()) + " ");   //get morse code from hash map
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
        }
    }

    private void flashLightOff() {
        CameraManager cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            String cameraId = cameraManager.getCameraIdList()[0];
            cameraManager.setTorchMode(cameraId, false);
        } catch (CameraAccessException e) {
        }
    }

    private class deviceFeedbackThread implements Runnable{
        String morseText;
        float oneTimeUnit = 50.0f;

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
                dotFeedbackTime = (int) (dotStandardTime/feedbackSpeed);
                dashFeedbackTime = (int) (dashStandardTime/feedbackSpeed);
                symbolSpaceSleepTime = (int) (symbolSpaceStandardWait/feedbackSpeed);
                letterSpaceSleepTime = (int) (letterSpaceStandardWait/feedbackSpeed);
                wordSpaceSleepTime = (int) ((wordSpaceStandardWait/feedbackSpeed)/5);

                if(morseText.toCharArray()[i]=='·'){
                    try {
                        if(vibrateChecked) {v.vibrate(dotFeedbackTime);}
                        if(hasCameraFlash && flashChecked){
                            flashLightOn();
                        }
                        Thread.sleep(dotFeedbackTime);
                        if(hasCameraFlash){
                            flashLightOff();
                        }
                        Thread.sleep(letterSpaceSleepTime);
                    } catch (Exception e){
                        Log.e(TAG, "convertToMorse: " + e);
                    }
                } else if(morseText.toCharArray()[i]=='−'){
                    try {
                        if(vibrateChecked) v.vibrate(dashFeedbackTime);
                        if(hasCameraFlash && flashChecked){
                            flashLightOn();
                        }
                        Thread.sleep(dashFeedbackTime);
                        if(hasCameraFlash){
                            flashLightOff();
                        }
                        Thread.sleep(letterSpaceSleepTime);
                    } catch (Exception e){
                        Log.e(TAG, "convertToMorse: " + e);
                    }
                } else if(morseText.toCharArray()[i]==' '){
                    try {
                        if(morseText.toCharArray()[i]!=' '){
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
            if(!feedbackContinuationCondition){
                convertButton.setText("Convert");
            }
        }

        private void stop(){
            stopThread = true;
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

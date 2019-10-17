package com.example.cs3235;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.SystemClock;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    // Global variables
    private static final String refresh_rate = "100hz";
    private static final String alphabet = "z";
    private static final String phone = "blue_huawei";

    // Used for logging on logcat
    private static final String TAG = "MainActivity";

    // Try to write to a file
    FileWriter writer;
    String fileDir;

    // Variables for Sensors
    private TextView x_accel, y_accel, z_accel;
    private SensorManager sensorManager;
    private Sensor accelerometer;
    private Sensor gyroscope;
    private SensorEventListener sensorEventListener;

    // Variables for XML elements UI display
    private Button startBtn;
    private Spinner spinner;
    private boolean isStartPressed;
    private boolean isSetPressed;
    private EditText mEditTextInput;
    private Button mButtonSet;
    public CustomEditText mKeyboard;

    private TextView mTextViewCountDown;
    private CountDownTimer mCountDownTimer;
    private boolean mTimerRunning;
    private long mStartTimeInMillis;
    private long mTimeLeftInMillis;

    // Firebase storage
    private StorageReference mStorageRef;
    private Uri filePath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mKeyboard = new CustomEditText(getApplicationContext());

        x_accel = findViewById(R.id.xAccel);
        y_accel = findViewById(R.id.yAccel);
        z_accel = findViewById(R.id.zAccel);
        mTextViewCountDown = findViewById(R.id.text_view_countdown);
        mEditTextInput = findViewById(R.id.mEditTimer);
        mButtonSet = findViewById(R.id.setBtn);
        spinner = findViewById(R.id.refresh_spinner);
        startBtn = findViewById(R.id.startBtn);
        mKeyboard = findViewById(R.id.keyboard);

        // Firebase storage
        mStorageRef = FirebaseStorage.getInstance("gs://cs3235-92947.appspot.com").getReference();

        // Prevent unnecessary tapping of EditText field before setting timer which causes app to crash
        mKeyboard.setVisibility(View.INVISIBLE);

        mButtonSet.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                String input = mEditTextInput.getText().toString();
                if (input.length() == 0) {
                    Toast.makeText(MainActivity.this, "Please enter a value", Toast.LENGTH_SHORT).show();
                    return;
                }

                long millisInput = Long.parseLong(input) * 1000;
                if (millisInput == 0) {
                    Toast.makeText(MainActivity.this, "Please enter positive number", Toast.LENGTH_SHORT).show();
                    return;
                }
                isSetPressed = true;
                setTimer(millisInput);
                mEditTextInput.setText("");
                closeKeyboard();


            }
        });

        // Initializing gyroscope components
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        try {
            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        } catch (NullPointerException npe) {
            Toast toast = Toast.makeText(this, "This device does not support Gyroscope", Toast.LENGTH_SHORT);
            toast.show();
            finish();
        }

        sensorEventListener = new SensorEventListener() {
            // Declaring variables to store sensor values to be input to writer library
            @Override
            public void onSensorChanged(SensorEvent event) {
                float accelX_value, accelY_value, accelZ_value;
                float gyroX_value, gyroY_value, gyroZ_value;
                Sensor sensor = event.sensor;
                if (isStartPressed && mTimerRunning && isSetPressed) {
                    if (sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
                        accelX_value = event.values[0];
                        accelY_value = event.values[1];
                        accelZ_value = event.values[2];
                        try {
                            writer.write(String.format(Locale.getDefault(), "%s, %f, %f, %f, %s\n", SystemClock.elapsedRealtimeNanos(),
                                    accelX_value, accelY_value, accelZ_value, "accel"));
                            writer.flush();
                        } catch (IOException io) {
                            Log.d(TAG, "Input output exception!" + io);
                        }
                    }
                    else if (sensor.getType() == Sensor.TYPE_GYROSCOPE) {
                        gyroX_value = event.values[0];
                        gyroY_value = event.values[1];
                        gyroZ_value = event.values[2];
                        try {
                            writer.write(String.format(Locale.getDefault(), "%s, %f, %f, %f, %s\n", SystemClock.elapsedRealtimeNanos(),
                                    gyroX_value, gyroY_value, gyroZ_value, "gyro"));
                            writer.flush();
                        } catch (IOException io) {
                            Log.d(TAG, "Input output exception!" + io);
                        }
                    }
                }
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {

            }
        };

        // Setting up for buttons
        startBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!(isStartPressed && mTimerRunning && isSetPressed)) {
                    Log.d(TAG, "Writing to " + getStorageDir());
                    // Creating date format
                    DateFormat simple = new SimpleDateFormat("ddMMyyyy_HHmmss");
                    Date currDate = new Date(System.currentTimeMillis());
                    try {
                        fileDir = getStorageDir() + "/accel_" + refresh_rate + "_" + alphabet + "_" + simple.format(currDate) + ".csv";
                        Log.d(TAG, "This is the filedir to be saved: " + fileDir);
                        writer = new FileWriter(new File(fileDir));
                        Log.d(TAG, "Successfully created writer");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    isStartPressed = true;
                    startTimer();
                    mButtonSet.setEnabled(false);
                    startBtn.setEnabled(false);
                    mKeyboard.setVisibility(View.VISIBLE);
                    mKeyboard.requestFocus();
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.showSoftInput(mKeyboard, InputMethodManager.SHOW_IMPLICIT);
                }

            }
        });

        mKeyboard.setOnKeyListener(new View.OnKeyListener() {
                                       @Override
                                       public boolean onKey(View v, int keyCode, KeyEvent event) {
                                           if(event.getAction() == KeyEvent.ACTION_DOWN) {
                                               Log.d(TAG, "Tapped");
                                               try {
                                                   writer.write(String.format(Locale.getDefault(), "%s, %f, %f, %f, %s\n", SystemClock.elapsedRealtimeNanos(),
                                                           -3.0, -3.0, -3.0, "Tapped"));
                                                   writer.flush();
                                               } catch (IOException io) {
                                                   Log.d(TAG, "Input output exception!" + io);
                                               }
                                               return false;
                                           }
                                           else if(event.getAction() == KeyEvent.ACTION_UP) {
                                               Log.d(TAG, "Released");
                                               try {
                                                   writer.write(String.format(Locale.getDefault(), "%s, %f, %f, %f, %s\n", SystemClock.elapsedRealtimeNanos(),
                                                           -3.0, -3.0, -3.0, "Released"));
                                                   writer.flush();
                                               } catch (IOException io) {
                                                   Log.d(TAG, "Input output exception!" + io);
                                               }
                                               return false;
                                           }
                                           return false;
                                       }
                                   });

        // Setting up for spinner
        ArrayAdapter < CharSequence > adapter = ArrayAdapter.createFromResource(MainActivity.this, R.array.refresh_rate, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String option = parent.getItemAtPosition(position).toString();
                // Unregistering the listener before setting a new refresh rate
                sensorManager.unregisterListener(sensorEventListener);

                // Todo: Add customized frequency based on requirements
                switch (option) {
                    case "UI": // 16.667 Hz
                        sensorManager.registerListener(sensorEventListener, accelerometer, SensorManager.SENSOR_DELAY_UI);
                        sensorManager.registerListener(sensorEventListener, gyroscope, SensorManager.SENSOR_DELAY_UI);
                        break;
                    case "Normal": // 5Hz
                        sensorManager.registerListener(sensorEventListener, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
                        sensorManager.registerListener(sensorEventListener, gyroscope, SensorManager.SENSOR_DELAY_NORMAL);
                        break;
                    case "Game": // 50hz
                        sensorManager.registerListener(sensorEventListener, accelerometer, SensorManager.SENSOR_DELAY_GAME);
                        sensorManager.registerListener(sensorEventListener, gyroscope, SensorManager.SENSOR_DELAY_GAME);
                        break;
                    case "Fastest": // 0 Microseconds delay
                        sensorManager.registerListener(sensorEventListener, accelerometer, 10000);
                        sensorManager.registerListener(sensorEventListener, gyroscope, 10000);
                        break;
                }

            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Do nothing
            }
        });

    }

    private void resetTimer() {
        mTimeLeftInMillis = mStartTimeInMillis;
        updateCountDownText();
    }

    private void setTimer(long milliseconds) {
        mStartTimeInMillis = milliseconds;
        resetTimer();
    }
    private void startTimer() {
        mCountDownTimer = new CountDownTimer(mTimeLeftInMillis, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                mTimeLeftInMillis = millisUntilFinished;
                updateCountDownText();
            }

            @Override
            public void onFinish() {
                Toast.makeText(MainActivity.this, "Completed data collection", Toast.LENGTH_SHORT).show();
                mTimerRunning = false;
                isSetPressed = false;
                isStartPressed = false;
                mButtonSet.setEnabled(true);
                startBtn.setEnabled(true);
                closeKeyboard();
                mKeyboard.setText("");
                try {
                    writer.close();
                } catch (IOException io) {
                    Log.e(TAG, "Error in IO when closing writer");
                }
                Uri file = Uri.fromFile(new File(fileDir));
                StorageReference csvRef = mStorageRef.child(phone + "/" + refresh_rate + "/" + file.getLastPathSegment());
                final ProgressDialog progressDialog = new ProgressDialog(MainActivity.this);
                progressDialog.setTitle("Progress...");

                csvRef.putFile(file)
                        .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                            @Override
                            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                                Toast.makeText(MainActivity.this, "Successful", Toast.LENGTH_SHORT).show();
                                progressDialog.dismiss();
                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception exception) {
                                // Handle unsuccessful uploads
                                Toast.makeText(MainActivity.this, "Not Successful", Toast.LENGTH_SHORT).show();
                            }
                        })
                        .addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
                            @Override
                            public void onProgress(UploadTask.TaskSnapshot taskSnapshot) {
                                //calculating progress percentage
                                double progress = (100.0 * taskSnapshot.getBytesTransferred()) / taskSnapshot.getTotalByteCount();

                                //displaying percentage in progress dialog
                                progressDialog.setMessage("Uploaded " + (int)(progress) + "%...");
                                progressDialog.show();

                            }
                        });
            }
        }.start();

        mTimerRunning = true;
    }

    private void updateCountDownText() {
        int minutes = (int) (mTimeLeftInMillis / 1000) / 60;
        int seconds = (int) (mTimeLeftInMillis / 1000) % 60;

        String timeLeftFormatted = String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds);

        mTextViewCountDown.setText(timeLeftFormatted);
    }
    private void closeKeyboard() {
        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    // DELAY refers to the refresh rate
    // Default = UI setting
    @Override
    protected void onResume() {
        super.onResume();
        sensorManager.registerListener(sensorEventListener, accelerometer, SensorManager.SENSOR_DELAY_GAME);
        sensorManager.registerListener(sensorEventListener, gyroscope, SensorManager.SENSOR_DELAY_GAME);
    }

    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(sensorEventListener);
    }

    private String getStorageDir() {
        return this.getExternalFilesDir(null).getAbsolutePath();
    }

}

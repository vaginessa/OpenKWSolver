package com.dotwee.openkwsolver;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.concurrent.ExecutionException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends Activity {

    public String regex = "^[0-9]+ .+";
    String kwCoreurl = "http://www.9kw.eu:80/index.cgi";
    String actionCaptchanewok = "?action=usercaptchanew";
    String actionSource = "&source=androidopenkws";
    String actionConfirm = ""; // &confirm=1
    String actionNocaptcha = "&nocaptcha=1";
    String actionAnswer = "?action=usercaptchacorrect";
    String actionShow = "?action=usercaptchashow";
    String actionSkipcaptcha = "?action=usercaptchaskip";
    String actionServercheck = "?action=userservercheck";
    String actionBalance = "?action=usercaptchaguthaben";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final Button buttonPull = (Button) findViewById(R.id.buttonPull);
        final Button buttonBalance = (Button) findViewById(R.id.buttonBalance);
        final ImageView ImageViewCaptcha = (ImageView) findViewById(R.id.imageViewCaptcha);
        final EditText EditTextCaptchaAnswer = (EditText) findViewById(R.id.editTextAnswer);
        EditTextCaptchaAnswer.setMaxWidth(EditTextCaptchaAnswer.getWidth());

        buttonBalance.setEnabled(false);

        File dir = getFilesDir();
        File file = new File(dir, "apikey.txt");
        if (file.exists()) {
            balanceThread();
            buttonBalance.setVisibility(View.VISIBLE);
        }

        if (isNetworkAvailable()) {
            servercheckThread();
        }

        buttonPull.setText(getString(R.string.start));
        buttonPull.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i("OnClickPull", "Click recognized");
                if (isNetworkAvailable()) {
                    final String CaptchaID = requestCaptchaID();
                    if (!CaptchaID.matches(regex)) {

                        final ProgressBar ProgressBar = (ProgressBar) findViewById(R.id.progressBar);

                        boolean b = pullCaptchaPicture(CaptchaID);

                        if (b) {
                            buttonPull.performClick();
                        }

                        final int[] i = {0};
                        final CountDownTimer CountDownTimer;
                        CountDownTimer = new CountDownTimer(30000, 1000) {

                            @Override
                            public void onTick(long millisUntilFinished) {
                                i[0]++;
                                ProgressBar.setProgress(i[0]);
                            }

                            @Override
                            public void onFinish() {

                            }
                        };

                        if (b) {
                            buttonPull.performClick();
                            Log.i("OnClickPull", "Timer started");
                        }

                        if (!b) {
                            CountDownTimer.start();
                        }

                        Button buttonSend = (Button) findViewById(R.id.buttonSend);
                        buttonSend.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                String CaptchaAnswer = EditTextCaptchaAnswer.getText().toString();
                                sendCaptchaAnswer(CaptchaAnswer, CaptchaID);

                                CountDownTimer.cancel();
                                Log.i("OnClickSend", "Timer killed");
                                ProgressBar.setProgress(0);

                                ImageViewCaptcha.setImageDrawable(null);
                                EditTextCaptchaAnswer.setText(null);

                                TextView TextViewCurrent;
                                TextViewCurrent = (TextView) findViewById(R.id.textViewCurrent);
                                TextViewCurrent.setText(null);

                                buttonPull.setEnabled(true);

                                Toast.makeText(getApplicationContext(), getString(R.string.next_captcha_arrives_soon), Toast.LENGTH_SHORT).show();
                                Handler autoPull = new Handler();
                                autoPull.postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        Log.i("OnClickPull", "Auto-pull next Captcha");
                                        buttonPull.performClick();
                                    }
                                }, 3000); // three sec delay
                            }
                        });

                        Button buttonSkip = (Button) findViewById(R.id.buttonSkip);
                        buttonSkip.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                Log.i("OnClickSkip", "Click recognized");
                                EditText EditTextCaptchaAnswer = (EditText) findViewById(R.id.editTextAnswer);
                                EditTextCaptchaAnswer.setText(null);

                                skipCaptcha(CaptchaID);

                                TextView TextViewCurrent = (TextView) findViewById(R.id.textViewCurrent);
                                TextViewCurrent.setText(null);
                                
                                CountDownTimer.cancel();
                                ProgressBar.setProgress(0);

                                ImageView ImageView = (ImageView) findViewById(R.id.imageViewCaptcha);
                                ImageView.setImageDrawable(null);

                                buttonPull.setEnabled(true);

                                Toast.makeText(getApplicationContext(), getString(R.string.next_captcha_arrives_soon), Toast.LENGTH_SHORT).show();
                                Handler autoPull = new Handler();
                                autoPull.postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        buttonPull.performClick();
                                        Log.i("OnClickSkip", "Auto-pull next Captcha");
                                    }
                                }, 3000); // three sec delay
                            }
                        });

                    } else Log.i("OnClickPull", "Error with ID: " + CaptchaID);
                } else {
                    DialogNetwork();
                }
            }
        });

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        Log.i("onCreateOptionsMenu", "Return: " + true);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case R.id.action_enter_api:
                DialogAPI();
                return true;
            case R.id.action_stop:
                finish();
                System.exit(0);
                return true;
            case R.id.action_debug:
                String sType = "debug";
                if (item.isChecked()) {
                    item.setChecked(false);
                    writeState(sType, false);
                } else {
                    item.setChecked(true);
                    writeState(sType, true);
                }
                return true;
            case R.id.action_selfonly:
                String aType = "selfonly";
                if (item.isChecked()) {
                    item.setChecked(false);
                    writeState(aType, false);
                } else {
                    item.setChecked(true);
                    writeState(aType, true);
                }
                return true;
            case R.id.action_loop:
                String lType = "loop";
                if (item.isChecked()) {
                    item.setChecked(false);
                    writeState(lType, false);
                } else {
                    item.setChecked(true);
                    writeState(lType, true);
                }
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    // Request CaptchaID
    public String requestCaptchaID() {
        String CaptchaURL = (kwCoreurl + actionCaptchanewok + pullKey() + actionSource + actionConfirm + actionNocaptcha + readState("selfonly") + readState("debug"));
        Log.i("requestCaptchaID", "URL: " + CaptchaURL);
        String CaptchaID = null;

        try {
            CaptchaID = new DownloadContentTask().execute(CaptchaURL).get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
        if (CaptchaID == "") {
            Log.i("requestCaptchaID", "CaptchaID is empty");
        } else Log.i("requestCaptchaID", "Received ID: " + CaptchaID);

        TextView TextViewCurrent = (TextView) findViewById(R.id.textViewCurrent);
        TextViewCurrent.setText(CaptchaID);
        
        return CaptchaID;
    }

    // Send Captcha answer
    public void sendCaptchaAnswer(String CaptchaAnswer, String CaptchaID) {

        Log.i("sendCaptchaAnswer", "Received answer: " + CaptchaAnswer);
        Log.i("sendCaptchaAnswer", "Received ID: " + CaptchaID);

        String CaptchaURL = (kwCoreurl + actionAnswer + actionSource + readState("debug") + "&antwort=" + CaptchaAnswer + "&id=" + CaptchaID + pullKey());
        // remove Spaces
        CaptchaURL = CaptchaURL.replaceAll(" ", "%20");
        Log.i("sendCaptchaAnswer", "URL: " + CaptchaURL);

        String Status = null;

        try {
            Status = new DownloadContentTask().execute(CaptchaURL).get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }

        Log.i("sendCaptchaAnswer", "Code: " + Status);

    }

    // Pull Captcha picture and display it
    public boolean pullCaptchaPicture(String CaptchaID) {
        String CaptchaPictureURL = (kwCoreurl + actionShow + actionSource + readState("debug") + "&id=" + CaptchaID + pullKey());
        Log.i("pullCaptchaPicture", "URL: " + CaptchaPictureURL);
        ImageView ImageV = (ImageView) findViewById(R.id.imageViewCaptcha);
        new DownloadImageTask(ImageV).execute(CaptchaPictureURL);

        if (CaptchaID != "") {
            return false;
        } else return true; // true =

    }

    // Skip Captcha
    public void skipCaptcha(String CaptchaID) {
        String CaptchaSkipURL = (kwCoreurl + actionSkipcaptcha + "&id=" + CaptchaID + pullKey() + actionSource + readState("debug"));
        Log.i("skipCaptcha", "URL: " + CaptchaSkipURL);
        String Code = null;

        try {
            Code = new DownloadContentTask().execute(CaptchaSkipURL).get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
        Log.i("skipCaptcha", "Result: " + Code);
    }

    // Dialog for API key
    public void DialogAPI() {
        AlertDialog.Builder AskDialog = new AlertDialog.Builder(this);

        AskDialog.setTitle("API-Key");
        AskDialog.setMessage(getString(R.string.enter_captcha_here));

        final String filename = "apikey.txt";
        final EditText input_key = new EditText(this);

        AskDialog.setView(input_key);
        AskDialog.setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

                OutputStreamWriter save = null;
                try {
                    save = new OutputStreamWriter(openFileOutput(filename, MODE_APPEND));
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
                String apikey = input_key.getText().toString();
                try {
                    assert save != null;
                    save.write(apikey);
                    Log.i("DialogAPI", "Saving API-Key successful");
                } catch (IOException e) {
                    e.printStackTrace();
                }
                try {
                    save.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                Toast.makeText(getApplicationContext(), getString(R.string.apikey_now_saved), Toast.LENGTH_SHORT).show();


            }
        });

        AskDialog.setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Log.i("DialogAPI", "Canceled Dialog");
            }
        });
        AskDialog.show();
    }

    // Read API-Key from Dialog
    private String pullKey() {
        String read = null;

        try {

            InputStream inputStream = openFileInput("apikey.txt");

            if (inputStream != null) {
                InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
                BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
                String receiveString;
                StringBuilder stringBuilder = new StringBuilder();

                while ((receiveString = bufferedReader.readLine()) != null) {
                    stringBuilder.append(receiveString);
                }

                inputStream.close();
                read = ("&apikey=" + stringBuilder.toString());
                Log.i("pullKey", "Readed key: " + stringBuilder.toString());
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        Log.i("pullKey", "Return: " + read);
        return read;

    }

    // Write states (Debug and self-only)
    public void writeState(String Type, Boolean State) {
        String FILENAME = (Type + ".txt");

        File dir = getFilesDir();
        File file = new File(dir, FILENAME);
        boolean deleted = file.delete();

        if (deleted) {
            Log.i("writeState", "File deleted: " + FILENAME);
        } else Log.i("writeState", "Not deleted: " + FILENAME);

        OutputStreamWriter save = null;
        try {
            save = new OutputStreamWriter(openFileOutput(FILENAME, MODE_APPEND));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        try {
            assert save != null;
            save.write(String.valueOf(State));
        } catch (IOException e) {
            e.printStackTrace();
            Log.i("writeState", "Couldn't write " + Type);
            Log.i("writeState", "Error " + e);
        }
        try {
            save.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Read written states
    public String readState(String Type) {
        String FILENAME = (Type + ".txt");
        String out = "";
        String ret = "";

        try {
            InputStream inputStream = openFileInput(FILENAME);

            if (inputStream != null) {
                InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
                BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
                String receiveString;
                StringBuilder stringBuilder = new StringBuilder();

                while ((receiveString = bufferedReader.readLine()) != null)
                    stringBuilder.append(receiveString);

                inputStream.close();
                ret = stringBuilder.toString();
                Log.i("readState", "File-Output: " + ret);
            }
        } catch (IOException ignored) {
        }

        if (Type == "debug") if (ret == "true") out = "&debug=1";

        if (Type == "selfonly") if (ret == "true") out = "&selfonly=1";
        else out = "";

        Log.i("readState", "After Check: " + out);
        return out;
    }

    // Check if network is available
    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    // Notify the user about now working network
    public void DialogNetwork() {
        AlertDialog.Builder Dialog = new AlertDialog.Builder(this);

        Dialog.setTitle("No network available");
        Dialog.setMessage("Please connect to the internet!");

        Dialog.setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

            }
        });

        Dialog.setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

            }
        });
        Dialog.show();
    }

    // BalanceThread: Update the balance every 5 seconds
    public void balanceThread() {
        final Thread BalanceUpdate;
        BalanceUpdate = new Thread() {

            @Override
            public void run() {
                while (!isInterrupted()) {
                    try {
                        Thread.sleep(5000); // 5000ms = 5s
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Button buttonBalance = (Button) findViewById(R.id.buttonBalance);
                            Log.i("balanceThread", "Called.");
                            String tBalance = null;

                            try {
                                tBalance = new DownloadContentTask().execute(kwCoreurl + actionBalance + actionSource + pullKey()).get();
                            } catch (InterruptedException | ExecutionException e) {
                                e.printStackTrace();
                            }

                            buttonBalance.setText(tBalance);
                            
                        }
                    });
                }

            }
        };

        // check if thread isn't already running.
        if (BalanceUpdate.isAlive()) {
            BalanceUpdate.stop();
            Log.i("balanceThread", "stopped");
        }

        // if not, start it
        else {
            BalanceUpdate.start();
            Log.i("balanceThread", "started");
        }
    }

    // ServercheckThread: Update the server-stats every 5 seconds
    public void servercheckThread() {
        final Thread ServercheckUpdate;
        ServercheckUpdate = new Thread() {

            @Override
            public void run() {
                while (!isInterrupted()) {
                    try {
                        Thread.sleep(5000); // 5000ms = 5s
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Log.i("servercheckThread", "Called.");
                            Pattern pQueue = Pattern.compile("queue=(\\d+)");
                            Pattern pWorker = Pattern.compile("worker=(\\d+)");
                            String tServercheck = null;

                            try {
                                tServercheck = new DownloadContentTask().execute(kwCoreurl + actionServercheck + actionSource).get();
                            } catch (InterruptedException | ExecutionException e) {
                                e.printStackTrace();
                            }

                            Matcher mQueue = pQueue.matcher(tServercheck);
                            if (mQueue.find()) {
                                TextView TextViewQueue = (TextView) findViewById(R.id.textViewQueue);
                                TextViewQueue.setText(null);
                                TextViewQueue.setText(getString(R.string.captchas_in_queue) + mQueue.group(1));
                                Log.i("pullStatus", "Queue: " + mQueue.group(1));
                            }

                            Matcher mWorker = pWorker.matcher(tServercheck);
                            if (mWorker.find()) {
                                TextView TextViewWorker = (TextView) findViewById(R.id.textViewWorker);
                                TextViewWorker.setText(null);
                                TextViewWorker.setText(getString(R.string.workers) + mWorker.group(1));
                                Log.i("pullStatus", "Worker: " + mWorker.group(1));
                            }
                        }
                    });
                }

            }
        };

        // check if thread isn't already running.
        if (ServercheckUpdate.isAlive()) {
            ServercheckUpdate.stop();
            Log.i("servercheckThread", "stopped");
        }

        // if not, start it
        else {
            ServercheckUpdate.start();
            Log.i("servercheckThread", "started");
        }
    }
}
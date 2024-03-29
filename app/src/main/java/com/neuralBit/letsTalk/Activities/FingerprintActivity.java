package com.neuralBit.letsTalk.Activities;

import android.content.DialogInterface;
import android.content.Intent;
import android.hardware.biometrics.BiometricPrompt;
import android.os.Build;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.util.Log;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.campaign.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class FingerprintActivity extends AppCompatActivity {
    private BiometricPrompt.AuthenticationCallback authenticationCallback;
    private CancellationSignal cancellationSignal;
    public static final String TAG = "FingerprintActivity";
    @RequiresApi(api = Build.VERSION_CODES.P)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fingerprint);
        String activityName=getIntent().getStringExtra("ActivityName");
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                ImageView fingerprint = findViewById(R.id.imageView);
                authenticationCallback = new BiometricPrompt.AuthenticationCallback() {
                    @Override
                    public void onAuthenticationError(int errorCode, CharSequence errString) {
                        super.onAuthenticationError(errorCode, errString);
                        fingerprint.setColorFilter(ContextCompat.getColor(getApplicationContext(), R.color.red), android.graphics.PorterDuff.Mode.SRC_IN);

                    }

                    @Override
                    public void onAuthenticationSucceeded(BiometricPrompt.AuthenticationResult result) {
                        super.onAuthenticationSucceeded(result);
                        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                        Class<?> cls;
                        if (user != null) {
                            fingerprint.setColorFilter(ContextCompat.getColor(getApplicationContext(), R.color.teal_200), android.graphics.PorterDuff.Mode.SRC_IN);
                            try {
                                if(activityName!=null) {
                                     cls= Class.forName(activityName);
                                }else{
                                    cls=MainActivity.class;
                                }
                                Intent mainIntent = new Intent(FingerprintActivity.this, cls);
                                startActivity(mainIntent);
                                finish();

                            } catch (ClassNotFoundException e) {
                                e.printStackTrace();
                            }


                        }else{
                            finishAndRemoveTask();
                        }


                    }
                };


            }

            BiometricPrompt biometricPrompt = new BiometricPrompt.Builder(this)
                    .setTitle("Let's talk")
                    .setSubtitle("Authentication is required")
                    .setDescription("Fingerprint Authentication")
                    .setNegativeButton("Cancel", getMainExecutor(), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {

                        }
                    }).build();
            biometricPrompt.authenticate(getCancellationSignal(), getMainExecutor(), authenticationCallback);


    }
    private CancellationSignal getCancellationSignal(){
        cancellationSignal=new CancellationSignal();
        cancellationSignal.setOnCancelListener(new CancellationSignal.OnCancelListener() {
            @Override
            public void onCancel() {
                makeToast("Authentication cancelled");
            }
        });
        return cancellationSignal;
    }

    private void makeToast(String text){
        Toast.makeText(getApplicationContext(), text, Toast.LENGTH_SHORT);
    }


}
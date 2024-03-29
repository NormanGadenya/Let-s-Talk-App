package com.neuralBit.letsTalk.Activities;

import static android.view.View.GONE;
import static com.neuralBit.letsTalk.Common.Tools.CAMERA_REQUEST;
import static com.neuralBit.letsTalk.Common.Tools.CONTACTS_REQUEST;
import static com.neuralBit.letsTalk.Common.Tools.GALLERY_REQUEST;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.example.campaign.R;
import com.neuralBit.letsTalk.Common.Tools;
import com.neuralBit.letsTalk.Model.userModel;
import com.neuralBit.letsTalk.Services.ProfileUploadService;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.zolad.zoominimageview.ZoomInImageView;

import java.io.ByteArrayOutputStream;


public class RegistrationActivity extends AppCompatActivity {
    private Button submit_button;
    private EditText userNameTV;
    private FirebaseDatabase database;
    private FloatingActionButton selProfilePic,gallery_button,camera_button,remove_button;
    private String userId;
    private String phoneNumber;
    private ConstraintLayout wrapper;
    public static final String TAG="Registration";
    private Uri selected;
    private ProgressBar progressBar;
    private ZoomInImageView profilePic;
    private BottomSheetBehavior bottomSheet;
    private boolean clickedDone=false;
    private String date;
    private String time;
    private Tools tools;
    private FirebaseUser user;
    private String preferredLang;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registration_activity);
        database = FirebaseDatabase.getInstance();
        user= FirebaseAuth.getInstance().getCurrentUser();
        InitializeControllers();
        bottomSheet= BottomSheetBehavior.from(wrapper);
        SharedPreferences sharedPreferences = getSharedPreferences("Settings", MODE_PRIVATE);
        SharedPreferences.Editor editor= sharedPreferences.edit();
        bottomSheet.setState(BottomSheetBehavior.STATE_HIDDEN);
        Spinner languageSpinner= findViewById(R.id.languages);
        ArrayAdapter<CharSequence> langAdapter = ArrayAdapter.createFromResource(this,R.array.languages,android.R.layout.simple_spinner_item);
        langAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        languageSpinner.setAdapter(langAdapter);
        languageSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                preferredLang =parent.getItemAtPosition(position).toString();
                TextView tv = (TextView) view;
                if(tv!=null){
                    tv.setTextColor(getResources().getColor(R.color.teal_200));
                }
                editor.putString("preferredLang",preferredLang);
                editor.apply();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
        selProfilePic.setOnClickListener(v -> {
            closeKeyboard();
            wrapper.setVisibility(View.VISIBLE);
            showLayoutActions();
        });
        submit_button.setOnClickListener(v -> {
            String userName = userNameTV.getText().toString();
            progressBar.setVisibility(View.VISIBLE);
            if(userName.isEmpty() || preferredLang==null){
                Toast.makeText(RegistrationActivity.this, "Please fill in the fields", Toast.LENGTH_SHORT).show();
            }else{
                if (user!=null ) {
                    if(!clickedDone){
                        userId = user.getUid();
                        phoneNumber = user.getPhoneNumber();
                        editor.putBoolean("useTranslator",false);
                        editor.apply();
                        updateUserDetails(userName,phoneNumber,userId);
                        if(selected!=null){
                            editor.putBoolean("useTranslator",false);
                            editor.apply();
                            Intent i= new Intent (getApplicationContext(), ProfileUploadService.class);
                            i.putExtra("userId",userId);
                            i.setData(selected);
                            startService(i);
                        }
                    }
                    Intent chatList=new Intent(RegistrationActivity.this, MainActivity.class);
                    startActivity(chatList);
                    finish();
                    userNameTV.setEnabled(false);

                }
            }
        });
        userNameTV.setOnKeyListener((v, keyCode, event) -> {
            clickedDone=false;
            if ((event.getAction() == KeyEvent.ACTION_DOWN) &&
                    (keyCode == KeyEvent.KEYCODE_ENTER)) {
                if (user!=null && preferredLang!=null) {
                    progressBar.setVisibility(View.VISIBLE);
                    String userName=userNameTV.getText().toString();
                    userId=user.getUid();
                    phoneNumber=user.getPhoneNumber();
                    updateUserDetails(userName,phoneNumber,userId);
                    clickedDone=true;
                    if(selected!=null){
                        editor.putBoolean("useTranslator",false);
                        Intent i= new Intent (getApplicationContext(), ProfileUploadService.class);
                        i.putExtra("userId",userId);
                        i.setData(selected);
                        startService(i);

                    }
                    editor.putBoolean("useTranslator",false);
                    editor.apply();
                    Intent chatList=new Intent(RegistrationActivity.this, MainActivity.class);
                    startActivity(chatList);

                }

                return true;
            }
            return false;
        });
        gallery_button.setOnClickListener(v -> {
            getImageFromGallery();
        });
        camera_button.setOnClickListener(v -> {
            getImageFromCamera();
        });
        remove_button.setOnClickListener(v -> {
            removeImage();
        });
    }

    private void removeImage() {
        selected=null;
        profilePic.setImageResource(R.drawable.person);
        wrapper.setVisibility(GONE);
        userNameTV.setEnabled(true);
    }

    private void getImageFromCamera() {
        if (ContextCompat.checkSelfPermission(RegistrationActivity.this,
                Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            startActivityForResult(cameraIntent,  CAMERA_REQUEST);
            userNameTV.setEnabled(false);
        } else {
            requestCameraPermission();
        }
    }

    private void getImageFromGallery() {
        if (ContextCompat.checkSelfPermission(RegistrationActivity.this,
                Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(intent,GALLERY_REQUEST);
                userNameTV.setEnabled(false);
        } else {
            requestStoragePermission();
        }
    }

    private void showLayoutActions(){
        bottomSheet.setState(BottomSheetBehavior.STATE_EXPANDED);
        bottomSheet.setPeekHeight(32);
        bottomSheet.addBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {
                if(newState== BottomSheetBehavior.STATE_COLLAPSED){

                }
            }

            @Override
            public void onSlide(@NonNull View bottomSheet, float slideOffset) {
                Log.d(TAG, "onSlide: "+slideOffset);
            }
        });

    }
    private void updateUserDetails(String userName, String phoneNumber, String userId) {
            userModel userModel =new userModel();
            userModel.setUserName(userName);
            userModel.setPhoneNumber(phoneNumber);
            userModel.setLastSeenDate(date);
            userModel.setLastSeenTime(time);
            userModel.setOnline(true);
            userModel.setShowLastSeen(true);
            userModel.setShowOnlineState(true);
            userModel.setPreferredLang(preferredLang);
            DatabaseReference myRef = database.getReference();
            myRef.child("UserDetails").child(userId).setValue(userModel);
    }

    private void InitializeControllers() {
        userNameTV=findViewById(R.id.editTextPersonName);
        submit_button=findViewById(R.id.RegistrationButton);
        selProfilePic=findViewById(R.id.selProfilePic);
        gallery_button=findViewById(R.id.gallery_button);
        remove_button=findViewById(R.id.remove_button);
        camera_button=findViewById(R.id.camera_button);
        wrapper=findViewById(R.id.layout_actions);
        profilePic=findViewById(R.id.image_profile);
        progressBar=findViewById(R.id.progressBar1);
        profilePic.setClipToOutline(true);
        tools =new Tools();
        time=tools.getTime();
        date=tools.getDate();


    }

    private Uri getImageUri( Bitmap photo) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        photo.compress(Bitmap.CompressFormat.JPEG, 512, bytes);
        String path = MediaStore.Images.Media.insertImage(this.getContentResolver(), photo, "Title", null);
        return Uri.parse(path);
    }

    private void requestStoragePermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                Manifest.permission.READ_EXTERNAL_STORAGE)) {
            new AlertDialog.Builder(this)
                    .setTitle("Permission needed")
                    .setMessage("This permission is needed because we need to access your storage")
                    .setPositiveButton("ok", (dialog, which) -> ActivityCompat.requestPermissions(RegistrationActivity.this,
                            new String[] {Manifest.permission.READ_EXTERNAL_STORAGE}, GALLERY_REQUEST))
                    .setNegativeButton("cancel", (dialog, which) -> dialog.dismiss())
                    .create().show();
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[] {Manifest.permission.READ_EXTERNAL_STORAGE}, GALLERY_REQUEST);
        }
    }

    private void requestCameraPermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                Manifest.permission.CAMERA)) {
            new AlertDialog.Builder(this)
                    .setTitle("Permission needed")
                    .setMessage("This permission is needed because we need to access your camera")
                    .setPositiveButton("ok", (dialog, which) -> ActivityCompat.requestPermissions(RegistrationActivity.this,
                            new String[] {Manifest.permission.CAMERA}, CAMERA_REQUEST))
                    .setNegativeButton("cancel", (dialog, which) -> dialog.dismiss())
                    .create().show();
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[] {Manifest.permission.CAMERA}, CAMERA_REQUEST);
        }
    }



    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == GALLERY_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(intent, GALLERY_REQUEST);
            } else {
                Toast.makeText(this, "Permission DENIED", Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == CAMERA_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                startActivityForResult(cameraIntent, CAMERA_REQUEST);
            } else {
                Toast.makeText(this, "Permission DENIED", Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == CONTACTS_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Intent chatList = new Intent(RegistrationActivity.this, MainActivity.class);
                startActivity(chatList);
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode==GALLERY_REQUEST && resultCode== Activity.RESULT_OK && data!=null){
            selected=data.getData();
            try{
                Bitmap bitmap=MediaStore.Images.Media.getBitmap(getContentResolver(),selected);
                profilePic.setImageBitmap(bitmap);
                wrapper.setVisibility(GONE);
                userNameTV.setEnabled(true);

            }catch(Exception e){
                Log.d("error",e.getMessage());
                userNameTV.setEnabled(true);

            }
        }
        else if(requestCode ==CAMERA_REQUEST && resultCode== Activity.RESULT_OK && data!=null){
            try{
                Bitmap bitmap=(Bitmap) data.getExtras().get("data");

                selected=getImageUri(bitmap);
                profilePic.setImageBitmap(bitmap);
                wrapper.setVisibility(GONE);
                userNameTV.setEnabled(true);

            }catch(Exception e){
                Log.d("error",e.getMessage());

            }

        }
        else{
            userNameTV.setEnabled(true);
        }

    }
    private void closeKeyboard(){
        View view=this.getCurrentFocus();
        if (view!=null){
            InputMethodManager im=(InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
            im.hideSoftInputFromWindow(view.getWindowToken(),0);
        }
    }

    @Override
    public void onBackPressed() {

     wrapper.setVisibility(GONE);
     userNameTV.setEnabled(true);
    }

}
package com.neuralBit.letsTalk.Activities;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static com.neuralBit.letsTalk.Common.Tools.GALLERY_REQUEST;

import android.Manifest;
import android.app.Activity;
import androidx.appcompat.app.AlertDialog;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.hardware.fingerprint.FingerprintManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.ResultReceiver;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.palette.graphics.Palette;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestBuilder;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.example.campaign.R;
import com.neuralBit.letsTalk.Common.Tools;
import com.neuralBit.letsTalk.Model.UserViewModel;
import com.neuralBit.letsTalk.Model.messageListModel;
import com.neuralBit.letsTalk.Services.downloadLanguage;
import com.neuralBit.letsTalk.Services.updateStatusService;
import com.neuralBit.letsTalk.adapter.LangSpinnerAdapter;
import com.neuralBit.letsTalk.adapter.MessageSettingsAdapter;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.jgabrielfreitas.core.BlurImageView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import jp.wasabeef.glide.transformations.BlurTransformation;

public class SettingsActivity extends AppCompatActivity{
    private final List<messageListModel> messageList = new ArrayList<>();
    private Uri selected;
    private BlurImageView imageView;
    private SeekBar seekBar;
    private ProgressBar progressBar;
    private int seekBarProgress = 1;
    private boolean changed = false;
    private String chatWallpaperUrI;
    private final FirebaseDatabase database = FirebaseDatabase.getInstance();
    private final FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
    private SharedPreferences sharedPreferences;
    private boolean showOnline, showLastSeen,setFingerprint,dynamicChatBubbles,useTranslator;
    public static final String TAG="SettingsActivity";
    private SharedPreferences.Editor editor ;
    private Tools tools = new Tools();
    private FloatingActionButton restoreButton;
    private MessageSettingsAdapter messageSettingsAdapter;
    private int chatBubbleColor,chatTextColor, chatReadColor;
    private Boolean fpSupport=false;
    private Spinner spinner;
    private String fpTimeOut;
    private Button downloadFr,downloadDe,downloadEs,downloadEn,downloadSw;
    private ProgressBar FrProgress,DeProgress,EsProgress,EnProgress,SwProgress;
    private Boolean frAvail= false;
    private Boolean deAvail=false;
    private Boolean esAvail =false;
    private Boolean enAvail = false;
    private Boolean swAvail=false;




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_settings);
        sharedPreferences = getSharedPreferences("Settings", MODE_PRIVATE);
        Toolbar toolbar = findViewById(R.id.toolbar);
        imageView = findViewById(R.id.imageView);
        restoreButton = findViewById(R.id.wallPaperRestore);
        imageView.setClipToOutline(true);
        imageView.setBackgroundResource(R.drawable.card_background3);
        seekBar = findViewById(R.id.seekBar);
        tools.context =getApplicationContext();
        Button logout = findViewById(R.id.logout);

        UserViewModel userViewModel = new ViewModelProvider(this).get(UserViewModel.class);
        CheckBox onlineStatus = findViewById(R.id.onlineStatus);
        CheckBox lastSeenStatus = findViewById(R.id.lastSeenStatus);
        CheckBox fingerprintStatus = findViewById(R.id.fingerprint);
        CheckBox chatBubblesState = findViewById(R.id.chatBubbles);
        CheckBox translateState = findViewById(R.id.translateText);
        downloadEn =findViewById(R.id.downloadEnglish);
        downloadFr =findViewById(R.id.downloadFrench);
        downloadDe = findViewById(R.id.downloadGerman);
        downloadEs = findViewById(R.id.downloadSpanish);
        downloadSw = findViewById(R.id.downloadKiswahili);

        FrProgress =findViewById(R.id.dFrProgress);
        DeProgress =findViewById(R.id.DeProgress);
        EnProgress =findViewById(R.id.dEnProgress);
        EsProgress =findViewById(R.id.dEsProgress);
        SwProgress =findViewById(R.id.dSwProgress);


        spinner =findViewById(R.id.fpTimeOutS);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,R.array.fpTimeOutS,android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                fpTimeOut =parent.getItemAtPosition(position).toString();
                TextView tv = (TextView) view;
                if(tv!=null){
                    tv.setTextColor(getResources().getColor(R.color.lightSteelBlue));
                }


            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        if(tools.checkBiometricSupport()){
            fpSupport =true;
        }
        userViewModel.initFUserInfo();
        progressBar = findViewById(R.id.progressBarChatWallpaper);
        setSupportActionBar(toolbar);
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
        editor= sharedPreferences.edit();
        showLastSeen = sharedPreferences.getBoolean("showLastSeen", true);
        showOnline = sharedPreferences.getBoolean("showOnline", true);
        useTranslator =sharedPreferences.getBoolean("useTranslator",false);
        String fpTimeOut= sharedPreferences.getString("fpTimeOut",null);
        String preferredLang = sharedPreferences.getString("preferredLang",null);
        frAvail = sharedPreferences.getBoolean("frAvail",false);
        enAvail = sharedPreferences.getBoolean("enAvail",false);
        esAvail = sharedPreferences.getBoolean("esAvail",false);
        swAvail = sharedPreferences.getBoolean("swAvail",false);
        deAvail = sharedPreferences.getBoolean("deAvail",false);
        ResultReceiver myResultReceiver = new SettingsActivity.MyReceiver(null);

        downloadSw.setOnClickListener(I->{
            SwProgress.setVisibility(VISIBLE);
            if(preferredLang != null){
                Intent i = new Intent(SettingsActivity.this, downloadLanguage.class);
                i.putExtra("otherUserLang","Swahili");
                i.putExtra("preferredLang",preferredLang);
                i.putExtra("receiver", myResultReceiver);
                startService(i);
            }

        });
        downloadEs.setOnClickListener(I->{
            if(preferredLang!=null){
                EsProgress.setVisibility(VISIBLE);
                Intent i = new Intent(SettingsActivity.this, downloadLanguage.class);
                i.putExtra("otherUserLang","Spanish");
                i.putExtra("preferredLang",preferredLang);
                i.putExtra("receiver", myResultReceiver);

                startService(i);
            }

        });
        downloadEn.setOnClickListener(I->{
            if(preferredLang != null){
                EnProgress.setVisibility(VISIBLE);
                Intent i = new Intent(SettingsActivity.this, downloadLanguage.class);
                i.putExtra("otherUserLang","English");
                i.putExtra("preferredLang",preferredLang);
                i.putExtra("receiver", myResultReceiver);

                startService(i);
            }
        });
        downloadDe.setOnClickListener(I->{
            if(preferredLang != null){
                DeProgress.setVisibility(VISIBLE);
                Intent i = new Intent(SettingsActivity.this, downloadLanguage.class);
                i.putExtra("otherUserLang","German");
                i.putExtra("preferredLang",preferredLang);
                i.putExtra("receiver", myResultReceiver);

                startService(i);
            }

        });
        downloadFr.setOnClickListener(I->{
            if(preferredLang != null){
                FrProgress.setVisibility(VISIBLE);
                Intent i = new Intent(SettingsActivity.this, downloadLanguage.class);
                i.putExtra("otherUserLang","French");
                i.putExtra("preferredLang",preferredLang);
                i.putExtra("receiver", myResultReceiver);

                startService(i);
            }

        });

        TextView tv = findViewById(R.id.textView20);

        if(fpTimeOut!=null){
            int spinnerPosition= adapter.getPosition(fpTimeOut);
            spinner.setSelection(spinnerPosition);
        }




        if(fpSupport && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P){
            setFingerprint = sharedPreferences.getBoolean("setFingerprint",false);
        }else{
            spinner.setVisibility(GONE);
            tv.setVisibility(GONE);
            findViewById(R.id.textView13).setVisibility(GONE);
            findViewById(R.id.textView19).setVisibility(GONE);
            fingerprintStatus.setVisibility(GONE);
        }
        dynamicChatBubbles= sharedPreferences.getBoolean("useDynamicBubbles",false);
        try{
            getOpacity();
            getCurrentWallpaper();
        }catch(Exception e){
            Log.e(TAG, "onCreate: ",e );
        }
        onlineStatus.setChecked(showOnline);
        lastSeenStatus.setChecked(showLastSeen);
        fingerprintStatus.setChecked(setFingerprint);
        chatBubblesState.setChecked(dynamicChatBubbles);
        translateState.setChecked(useTranslator);
        if(setFingerprint){
            tv.setVisibility(VISIBLE);
            spinner.setVisibility(VISIBLE);
        }else{
            tv.setVisibility(GONE);
            spinner.setVisibility(GONE);
        }


        RecyclerView recyclerView = findViewById(R.id.recycler_view_wall);
        FloatingActionButton editWallpaper = findViewById(R.id.editWallpaper);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        setUpRecyclerView(recyclerView);


        editWallpaper.setOnClickListener(v -> {
            if (ContextCompat.checkSelfPermission(getApplicationContext(),

                    Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {

                Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(intent, GALLERY_REQUEST);
            } else {
                requestStoragePermission();
            }
        });
        onlineStatus.setOnCheckedChangeListener((buttonView, isChecked) -> showOnline = isChecked);
        chatBubblesState.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if(isChecked){
                if(chatWallpaperUrI!=null){
                    getViewColor(Uri.parse(chatWallpaperUrI));

                }else{
                    getViewColor(null);

                }

            }else{
                messageSettingsAdapter.viewBackColor=getColor(R.color.cream);
                messageSettingsAdapter.notifyDataSetChanged();
            }

            dynamicChatBubbles =isChecked;});
        lastSeenStatus.setOnCheckedChangeListener((buttonView, isChecked) -> showLastSeen = isChecked);
        fingerprintStatus.setOnCheckedChangeListener((buttonView, isChecked) ->{
            setFingerprint = isChecked;
            if(isChecked){
                tv.setVisibility(VISIBLE);
                spinner.setVisibility(VISIBLE);
            }else{
                tv.setVisibility(GONE);
                spinner.setVisibility(GONE);
            }
        } );
        translateState.setOnCheckedChangeListener((buttonView,isChecked) ->{
            useTranslator =isChecked;
            if(useTranslator){
                switch (preferredLang) {
                    case "English": {
                        downloadEs.setVisibility(VISIBLE);
                        downloadSw.setVisibility(VISIBLE);
                        downloadFr.setVisibility(VISIBLE);
                        downloadDe.setVisibility(VISIBLE);
                    }
                    case "Spanish": {
                        downloadEn.setVisibility(VISIBLE);
                        downloadSw.setVisibility(VISIBLE);
                        downloadFr.setVisibility(VISIBLE);
                        downloadDe.setVisibility(VISIBLE);
                    }
                    case "French": {
                        downloadEs.setVisibility(VISIBLE);
                        downloadSw.setVisibility(VISIBLE);
                        downloadEn.setVisibility(VISIBLE);
                        downloadDe.setVisibility(VISIBLE);
                    }
                    case "German": {
                        downloadEs.setVisibility(VISIBLE);
                        downloadSw.setVisibility(VISIBLE);
                        downloadFr.setVisibility(VISIBLE);
                        downloadEn.setVisibility(VISIBLE);
                    }
                    case "Swahili": {
                        downloadEs.setVisibility(VISIBLE);
                        downloadEn.setVisibility(VISIBLE);
                        downloadFr.setVisibility(VISIBLE);
                        downloadDe.setVisibility(VISIBLE);
                    }
                }
            }
        });
        logout.setOnClickListener(v-> new AlertDialog.Builder(this)
                .setTitle("Log out")
                .setMessage("Are you sure you want to sign out")
                .setPositiveButton("Yes", (dialog, which) -> signOut())
                .setNegativeButton("No", (dialog, which) -> dialog.dismiss())
                .create().show());


        restoreButton.setOnClickListener(I->{
           editor.remove("chatWallpaper");
           imageView.setImageResource(R.drawable.def_wallpaper);
           messageSettingsAdapter.viewBackColor=getColor(R.color.cream);
           messageSettingsAdapter.notifyDataSetChanged();


        });
        FirebaseAuth mAuth =FirebaseAuth.getInstance();

        mAuth.addAuthStateListener(i->{
            if(i.getCurrentUser()==null){
                finishAndRemoveTask();
            }
        });
    }

    private void setUpRecyclerView(RecyclerView recyclerView) {
        messageList.add(new messageListModel(null, "123", "17-04-2020", "02:00", "", "", "TEXT", ""));
        messageList.add(new messageListModel(null, firebaseUser.getUid(), "17-04-2020", "02:00", "", "", "TEXT", ""));
        messageList.add(new messageListModel(null, "firebaseUser.getUid()", "17-04-2020", "02:00", "", "", "TEXT", ""));
        messageList.add(new messageListModel(null, firebaseUser.getUid(), "17-04-2020", "02:00", "", "", "TEXT", ""));
        messageList.add(new messageListModel(null, "firebaseUser.getUid()", "17-04-2020", "02:00", "", "", "TEXT", ""));
        messageList.add(new messageListModel(null, firebaseUser.getUid(), "17-04-2020", "02:00", "", "", "TEXT", ""));
        messageSettingsAdapter = new MessageSettingsAdapter(messageList, getApplicationContext());
        recyclerView.setAdapter(messageSettingsAdapter);
    }

    public void createPaletteAsync(Bitmap bitmap) {

        if (bitmap!=null){
            Palette.from(bitmap).generate(new Palette.PaletteAsyncListener() {

                public void onGenerated(Palette p) {
                    chatBubbleColor = p.getMutedColor(getResources().getColor(R.color.cream));
                    messageSettingsAdapter.viewBackColor =chatBubbleColor;
                    messageSettingsAdapter.notifyDataSetChanged();
                    Palette.Swatch mutedSwatch = p.getMutedSwatch();
                    Palette.Swatch vibrantSwatch = p.getVibrantSwatch();
                    if( vibrantSwatch != null && mutedSwatch !=null){
                        chatTextColor= mutedSwatch.getTitleTextColor();
                        chatReadColor= vibrantSwatch.getTitleTextColor();
                    }
                }
            });

        }

    }


    private void signOut() {
        Intent i=new Intent(SettingsActivity.this, updateStatusService.class);
        stopService(i);
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        assert user != null;
        DatabaseReference userDetailRef=database.getReference().child("UserDetails").child(user.getUid());
        Map<String ,Object> lastSeenStatus=new HashMap<>();

        lastSeenStatus.put("lastSeenDate",tools.getDate());
        lastSeenStatus.put("lastSeenTime",tools.getTime());
        lastSeenStatus.put("online",false);
        userDetailRef.onDisconnect().updateChildren(lastSeenStatus).addOnSuccessListener(I -> FirebaseAuth.getInstance().signOut());



    }


    private void getCurrentWallpaper() {
        chatWallpaperUrI = sharedPreferences.getString("chatWallpaper", null);

        int blur = sharedPreferences.getInt("chatBlur", 0);

        if (chatWallpaperUrI != null) {
            restoreButton.setVisibility(VISIBLE);
            if(dynamicChatBubbles){
                getViewColor(Uri.parse(chatWallpaperUrI));
            }
            RequestBuilder<Drawable> requestBuilder = Glide.with(getApplicationContext()).load(chatWallpaperUrI);
            if( blur!=0){
                requestBuilder =requestBuilder.transform(new BlurTransformation(blur));

            }

            requestBuilder.addListener(new RequestListener<Drawable>() {
                @Override
                public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                    progressBar.setVisibility(GONE);
                    Log.e(TAG, "onLoadFailed: ", e);
                    return false;
                }

                @Override
                public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                    progressBar.setVisibility(GONE);
                    seekBar.setProgress(blur * 4);
                    return false;
                }
            }).into(imageView);

        } else {
            imageView.setImageResource(R.drawable.def_wallpaper);
            if(dynamicChatBubbles){
                Log.d(TAG, "getCurrentWallpaper: ");
                getViewColor(null);
            }
        }


    }

    private void getOpacity() {
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                changed = true;
                if (chatWallpaperUrI != null || selected == null) {
                    seekBarProgress = progress / 4;
                    try {
                        if (seekBarProgress != 1) {
                            imageView.setBlur(seekBarProgress);
                        }
                    } catch (Exception e) {
                        e.getStackTrace();
                    }
                }

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

    }

    @Override
    protected void onStop() {
        setSettings(firebaseUser.getUid());
        super.onStop();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        Intent intent = new Intent(SettingsActivity.this, MainActivity.class);
        startActivity(intent);
    }

    private void requestStoragePermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                Manifest.permission.READ_EXTERNAL_STORAGE)) {
            new AlertDialog.Builder(this)
                    .setTitle("Permission needed")
                    .setMessage("This permission is needed because we need to access your storage")
                    .setPositiveButton("ok", (dialog, which) -> ActivityCompat.requestPermissions(SettingsActivity.this,
                            new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, GALLERY_REQUEST))
                    .setNegativeButton("cancel", (dialog, which) -> dialog.dismiss())
                    .create().show();
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, GALLERY_REQUEST);
        }
    }

    private void getViewColor(Uri wallpaper){
        new Thread(new Runnable() {

            @Override
            public void run() {
                try {
                    Bitmap bitmap;
                    if(wallpaper!=null ){

                        bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), wallpaper);

                    }else{
                        tools.context =getApplicationContext();
                        bitmap = tools.getBitmap(R.drawable.def_wallpaper);
                    }
                    createPaletteAsync(bitmap);



                } catch (Exception e) {
                    e.printStackTrace();
                }

            }
        }).start();
    }

    private void setSettings(String userId) {
        DatabaseReference myRef = database.getReference().child("UserDetails").child(userId);
        progressBar.setVisibility(VISIBLE);
        Map<String, Object> userDetail = new HashMap<>();

        userDetail.put("showLastSeenState", showLastSeen);
        userDetail.put("showOnlineState", showOnline);
        myRef.updateChildren(userDetail);
        editor.putBoolean("showOnline", showOnline);
        editor.putBoolean("showLastSeen", showLastSeen);
        editor.putBoolean("setFingerprint",setFingerprint);
        editor.putBoolean("useDynamicBubbles", dynamicChatBubbles);
        editor.putInt("chatBubbleColor",chatBubbleColor);
        editor.putInt("chatTextColor", chatTextColor);
        editor.putInt("chatReadColor", chatReadColor);
        editor.putString("fpTimeOut",fpTimeOut);
        editor.putBoolean("useTranslator",useTranslator);
        if (selected != null) {

            editor.putString("chatWallpaper", selected.toString());
            editor.putInt("chatBlur", seekBarProgress);

        } else {


            if (changed) {
                progressBar.setVisibility(GONE);
                editor.putInt("chatBlur", seekBarProgress);
            }

        }
        editor.apply();

        progressBar.setVisibility(GONE);
    }

    @SuppressWarnings("deprecation")
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
        }

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == GALLERY_REQUEST && resultCode == Activity.RESULT_OK && data != null) {
            selected = data.getData();
            chatWallpaperUrI =selected.toString();
            restoreButton.setVisibility(VISIBLE);
            try {
                progressBar.setVisibility(VISIBLE);
                Glide.with(getApplicationContext())
                        .load(selected)
                        .addListener(new RequestListener<Drawable>() {
                            @Override
                            public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                                progressBar.setVisibility(GONE);
                                return false;
                            }

                            @Override
                            public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                                progressBar.setVisibility(GONE);
                                return false;
                            }
                        })
                        .diskCacheStrategy(DiskCacheStrategy.NONE)
                        .skipMemoryCache(true)
                        .into(imageView)
                ;
                if(dynamicChatBubbles){
                    getViewColor(selected);
                }

                getOpacity();


            } catch (Exception e) {
                Log.d("error", e.getMessage());

            }

        }

    }

    public class MyReceiver extends ResultReceiver {
        public MyReceiver(Handler handler) {
            super(handler);
        }

        @Override
        protected void onReceiveResult(int resultCode, Bundle resultData) {
            super.onReceiveResult(resultCode, resultData);

            switch (resultCode){
                case 100: {
                    enAvail =true;
                    if(editor != null){
                        editor.putBoolean("enAvail",true);
                        EnProgress.setVisibility(GONE);
                        editor.apply();
                    }
                }

                case 200: {
                    esAvail =true;
                    if(editor != null){
                        editor.putBoolean("esAvail",true);
                        EnProgress.setVisibility(GONE);

                        editor.apply();

                    }
                }

                case 300: {
                    deAvail= true;
                    if(editor != null){
                        editor.putBoolean("deAvail",true);
                        EnProgress.setVisibility(GONE);

                        editor.apply();

                    }
                }

                case 400: {
                    swAvail =true;
                    if(editor != null){
                        editor.putBoolean("swAvail",true);
                        EnProgress.setVisibility(GONE);

                        editor.apply();

                    }
                }

                case 500: {
                    frAvail = true;
                    if(editor != null){
                        editor.putBoolean("frAvail",true);
                        EnProgress.setVisibility(GONE);
                        editor.apply();
                    }
                }

            }

        }
    }



}
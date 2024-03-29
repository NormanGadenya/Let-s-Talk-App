package com.neuralBit.letsTalk.Activities;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static com.neuralBit.letsTalk.Common.Tools.AUDIOREQUEST;
import static com.neuralBit.letsTalk.Common.Tools.IMAGEREQUEST;
import static com.neuralBit.letsTalk.Common.Tools.VIDEOREQUEST;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.os.ResultReceiver;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestBuilder;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.example.campaign.R;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.ml.common.modeldownload.FirebaseModelDownloadConditions;
import com.google.firebase.ml.naturallanguage.FirebaseNaturalLanguage;
import com.google.firebase.ml.naturallanguage.translate.FirebaseTranslator;
import com.google.firebase.ml.naturallanguage.translate.FirebaseTranslatorOptions;
import com.mikhaellopez.circularimageview.CircularImageView;
import com.neuralBit.letsTalk.Common.ServiceCheck;
import com.neuralBit.letsTalk.Common.Tools;
import com.neuralBit.letsTalk.Interfaces.APIService;
import com.neuralBit.letsTalk.Interfaces.RecyclerViewInterface;
import com.neuralBit.letsTalk.Model.MessageViewModel;
import com.neuralBit.letsTalk.Model.UserViewModel;
import com.neuralBit.letsTalk.Model.messageListModel;
import com.neuralBit.letsTalk.Model.userModel;
import com.neuralBit.letsTalk.Notifications.Client;
import com.neuralBit.letsTalk.Notifications.Data;
import com.neuralBit.letsTalk.Notifications.MyResponse;
import com.neuralBit.letsTalk.Notifications.Sender;
import com.neuralBit.letsTalk.Notifications.Token;
import com.neuralBit.letsTalk.Services.AudioUploadService;
import com.neuralBit.letsTalk.Services.ImageUploadService;
import com.neuralBit.letsTalk.Services.VideoUploadService;
import com.neuralBit.letsTalk.Services.updateStatusService;
import com.neuralBit.letsTalk.adapter.messageListAdapter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import hani.momanii.supernova_emoji_library.Actions.EmojIconActions;
import hani.momanii.supernova_emoji_library.Helper.EmojiconEditText;
import jp.wasabeef.glide.transformations.BlurTransformation;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ChatActivity extends AppCompatActivity implements RecyclerViewInterface {
    public static final String TAG="Chat Activity";
    private String otherUserId;
    private String profileUrI;
    private String otherUserName;
    private FirebaseDatabase database;
    private ArrayList<messageListModel> messageList = new ArrayList<>();
    private RecyclerView recyclerView ;
    private ImageButton sendButton;
    private ImageButton attachButton;
    private TextView userName,msgGroupDate;
    private EmojiconEditText newMessage;
    private CircularImageView profilePic;
    private FirebaseUser fUser;
    private ProgressBar progressBar;
    private messageListAdapter messageListAdapter;
    private ImageView backgroundImageView,onlineStatusView;
    private SharedPreferences settingsSharedPreferences;
    private LinearLayoutManager layoutManager;
    boolean notify=false;
    private FloatingActionButton imageButton,videoButton,audioButton;
    private MessageViewModel messageViewModel;
    private UserViewModel userViewModel;
    private View layoutActions,textArea;
    private APIService apiService;
    private MediaPlayer mediaPlayer;
    private String uploadImageTId,uploadVideoTId,uploadAudioTId;
    private int uploadImageTP,uploadVideoTP,uploadAudioTP;
    private final Map<String ,Integer> uploadImageData=new HashMap<>();
    private final Map<String ,Integer> uploadVideoData=new HashMap<>();
    private final Map<String ,Integer> uploadAudioData=new HashMap<>();
    private String date,time;
    private Tools tools;
    private String fPhoneNumber;
    private String chatWallpaperUrI;
    private ImageButton scrollButton;
    private TextView status;
    private Boolean dynamicChatBubbles;
    private CountDownTimer ct;
    private String otherUserPref;
    private FirebaseTranslator Translator;
    private Boolean useTranslator;

    @SuppressWarnings("deprecation")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);
        fUser = FirebaseAuth.getInstance().getCurrentUser();
        tools = new Tools();
        date=tools.getDate();
        time=tools.getTime();
        scrollButton=findViewById(R.id.scrollToBottom);
        tools.context=getApplicationContext();
        InitialiseControllers();

        chatWallpaperUrI=settingsSharedPreferences.getString("chatWallpaper",null);
        dynamicChatBubbles =settingsSharedPreferences.getBoolean("useDynamicBubbles",false);
        int chatBubbleColor =settingsSharedPreferences.getInt("chatBubbleColor",0);
        int chatTextColor =settingsSharedPreferences.getInt("chatTextColor",0);
        int chatReadColor =settingsSharedPreferences.getInt("chatReadColor",0);
        String fUserPrefLang = settingsSharedPreferences.getString("preferredLang",null);
        useTranslator = settingsSharedPreferences.getBoolean("useTranslator",false);
        loadUserDetails();
        setTypingStatus();

        FirebaseTranslatorOptions options =
                new FirebaseTranslatorOptions.Builder()
                        // below line we are specifying our source language.
                        .setSourceLanguage(tools.convertLangName(fUserPrefLang))
                        // in below line we are displaying our target language.
                        .setTargetLanguage(tools.convertLangName(otherUserPref))
                        // after that we are building our options.
                        .build();
        Translator = FirebaseNaturalLanguage.getInstance().getTranslator(options);
        layoutManager= new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false);
        recyclerView.setLayoutManager(layoutManager);
        messageViewModel.initChats(otherUserId);

        serviceCheck();
        messageList=messageViewModel.getMessages().getValue();
        loadAdapter(chatBubbleColor,chatTextColor,chatReadColor);

        imageButton.setOnClickListener(I->{
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            startActivityForResult(intent,IMAGEREQUEST);
        });

        videoButton.setOnClickListener(I->{
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Video.Media.EXTERNAL_CONTENT_URI);
            startActivityForResult(intent,VIDEOREQUEST);
        });

        audioButton.setOnClickListener(I->{
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Audio.Media.EXTERNAL_CONTENT_URI);
            startActivityForResult(intent,AUDIOREQUEST);

        });
        newMessage.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if(count > 0){
                    attachButton.setVisibility(GONE);
                }else{
                    attachButton.setVisibility(VISIBLE);

                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        getCurrentWallpaper(chatWallpaperUrI);
        try{
            Handler handler = new Handler(Looper.getMainLooper());
            handler.post(this::getMessages);

        }catch (Exception e){
            Log.d("Error" ,e.getLocalizedMessage());
        }
        String imageUrI=getIntent().getStringExtra("imageUrI");
        String caption=getIntent().getStringExtra("caption");
        String videoUrI=getIntent().getStringExtra("videoUrI");
        if(imageUrI!=null){
            if(tools.checkInternetConnection()){
                if(useTranslator) {
                    FirebaseModelDownloadConditions conditions = new FirebaseModelDownloadConditions.Builder().requireWifi().build();
                    Translator.downloadModelIfNeeded(conditions).addOnSuccessListener(I -> {
                        Translator.translate(caption).addOnSuccessListener(new OnSuccessListener<String>() {
                            @Override
                            public void onSuccess(String s) {
                                uploadImage(imageUrI,s);
                            }
                        }).addOnFailureListener(P->{
                            uploadImage(imageUrI,caption);
                        });
                    });
                }else{
                    uploadImage(imageUrI,caption);
                }

            }else{
                Toast.makeText(this,"No internet connection",Toast.LENGTH_SHORT).show();
            }

        }else if(videoUrI!=null){
            if(tools.checkInternetConnection()){
                if(useTranslator) {
                    FirebaseModelDownloadConditions conditions = new FirebaseModelDownloadConditions.Builder().requireWifi().build();
                    Translator.downloadModelIfNeeded(conditions).addOnSuccessListener(I -> {
                        Translator.translate(caption).addOnSuccessListener(new OnSuccessListener<String>() {
                            @Override
                            public void onSuccess(String s) {
                                uploadVideo(videoUrI,s);
                            }
                        }).addOnFailureListener(P->{
                            uploadVideo(videoUrI,caption);
                        });
                    });
                }else{
                    uploadVideo(videoUrI,caption);
                }
            }else{
                Toast.makeText(this,"No internet connection",Toast.LENGTH_SHORT).show();
            }

        }
        sendButton.setOnClickListener(view -> {
            if(tools.checkInternetConnection()){
                try{

                    uploadTextMessage();


                }catch (Exception e){
                    e.fillInStackTrace();
                }
            }else{
                Toast.makeText(this,"No internet connection",Toast.LENGTH_SHORT).show();
            }
        });



        attachButton.setOnClickListener(view ->{
            textArea.setVisibility(GONE);
            layoutActions.setVisibility(VISIBLE);
            layoutActions.animate().alpha(1.0f).setDuration(1000);
        });


    }

    private void loadAdapter(int chatBubbleColor,int chatTextColor, int chatReadColor){
        if(messageList!=null){
            messageListAdapter=new messageListAdapter();
            messageListAdapter.setMContext(ChatActivity.this);
            messageListAdapter.setMessageList(messageList);
            messageListAdapter.setActivity(this);
            messageListAdapter.setRecyclerViewInterface(this);
            messageListAdapter.setOtherUserId(otherUserId);
            messageListAdapter.uploadImageTask=uploadImageData;
            messageListAdapter.uploadVideoTask=uploadVideoData;
            messageListAdapter.chatWallpaperUri =chatWallpaperUrI;
            messageListAdapter.useTranslator=useTranslator;
            if(dynamicChatBubbles){
                messageListAdapter.viewTextColor=chatTextColor;
                messageListAdapter.checkedColor = chatReadColor;
                messageListAdapter.viewBackColor =chatBubbleColor;
            }

            recyclerView.setAdapter(messageListAdapter);
        }

    }

    private void loadUserDetails(){
        if(otherUserId!=null){
            getOtherUserDetails(otherUserId);
            saveSharedPreferenceData();

        }else{
            loadSharedPreferenceData();
        }

    }

    private void uploadVideo(String videoUrI,String caption){
        Intent intent =new Intent(this, VideoUploadService.class);
        ResultReceiver myResultReceiver=new MyReceiver(null);
        intent.putExtra("fPhoneNumber",fPhoneNumber)
                .putExtra("uri",videoUrI)
                .putExtra("userId", fUser.getUid())
                .putExtra("otherUserId",otherUserId)
                .putExtra("caption",caption)
                .putExtra("receiver",myResultReceiver);
        startService(intent);
    }

    private void uploadImage(String imageUrI,String caption){
        Intent intent =new Intent(this, ImageUploadService.class);
        ResultReceiver myResultReceiver=new MyReceiver(null);

        intent.putExtra("fPhoneNumber",fPhoneNumber)
                .putExtra("uri",imageUrI)
                .putExtra("userId", fUser.getUid())
                .putExtra("otherUserId",otherUserId)
                .putExtra("caption",caption)
                .putExtra("receiver",myResultReceiver);

        startService(intent);
    }

    private void uploadTextMessage() throws Exception {
        DatabaseReference otherUserBranch=database.getReference().child("chats").child(otherUserId).child(fUser.getUid());
        DatabaseReference fUserBranch=database.getReference().child("chats").child(fUser.getUid()).child(otherUserId).push();
        DatabaseReference fLMBranch=database.getReference().child("lastMessage").child(fUser.getUid()).child(otherUserId);
        DatabaseReference otherLMBranch=database.getReference().child("lastMessage").child(otherUserId).child(fUser.getUid());
        date=tools.getDate();
        time=tools.getTime();
        String message = newMessage.getText().toString();

        //noinspection deprecation
        updateToken(FirebaseInstanceId.getInstance().getToken());
        apiService= Client.getClient("https://fcm.googleapis.com").create(APIService.class);
        if(useTranslator){
            if(!message.trim().isEmpty()){
                String formattedDate = date;
                String formattedTime=time;
                messageListModel m=new messageListModel();
                m.setText(message);
                m.setText(tools.encryptText(message));
                m.setReceiver(otherUserId);
                m.setDate(formattedDate);
                m.setTime(formattedTime);
                m.setType("TEXT");
                newMessage.setText("");
                fLMBranch.setValue(m);
                fUserBranch.setValue(m);
                String messageKey= fUserBranch.getKey();
                FirebaseModelDownloadConditions conditions = new FirebaseModelDownloadConditions.Builder().requireWifi().build();
                Translator.downloadModelIfNeeded(conditions).addOnSuccessListener(I->{
                    Translator.translate(message).addOnSuccessListener(new OnSuccessListener<String>() {
                        @Override
                        public void onSuccess(String s) {
                            try {
                                String text= tools.encryptText(s);
                                m.setTranslatedText(text);
                                otherLMBranch.setValue(m);
                                assert messageKey != null;
                                otherUserBranch.child(messageKey).setValue(m);
                                notify=true;

                                sendNotification(otherUserId,fPhoneNumber, text);

                            } catch (Exception e) {
                                e.printStackTrace();
                            }




                        }
                    }).addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            e.printStackTrace();
                        }
                    });
                }).addOnFailureListener(I->{
                    try{
                        String text = tools.encryptText(message);
                        m.setTranslatedText(text);
                        otherLMBranch.setValue(m);
                        assert messageKey != null;
                        otherUserBranch.child(messageKey).setValue(m);
                        notify=true;

                        sendNotification(otherUserId,fPhoneNumber, text);

                    }catch(Exception e){
                        e.printStackTrace();
                    }

                    Toast.makeText(this, "Failed to download model", Toast.LENGTH_SHORT).show();
                });

            }
        }else{
            if(!message.trim().isEmpty()){
                String formattedDate = date;
                String formattedTime=time;
                messageListModel m=new messageListModel();
                String text = tools.encryptText(message);
                m.setText(text);
                m.setReceiver(otherUserId);
                m.setDate(formattedDate);
                m.setTime(formattedTime);
                m.setType("TEXT");
                fLMBranch.setValue(m);
                fUserBranch.setValue(m);
                String messageKey= fUserBranch.getKey();
                otherLMBranch.setValue(m);
                assert messageKey != null;
                otherUserBranch.child(messageKey).setValue(m);
                notify=true;

                sendNotification(otherUserId,fPhoneNumber, text);

                newMessage.setText("");
            }

        }

    }



    private void serviceCheck(){

        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);

        ServiceCheck serviceCheck=new ServiceCheck(updateStatusService.class,this,manager);
        serviceCheck.checkServiceRunning();
    }

    private void sendNotification(String otherUserId, String fPhoneNumber, String message) {
        DatabaseReference tokens=database.getReference("Tokens");
        Query query=tokens.orderByKey().equalTo(otherUserId);
        query.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for(DataSnapshot dataSnapshot:snapshot.getChildren()){
                    Token token=dataSnapshot.getValue(Token.class);
                    Data data=new Data(fUser.getUid(),R.mipmap.small_icon_round,message,fPhoneNumber,otherUserId,"New message");
                    assert token != null;
                    Sender sender = new Sender(data,token.getToken());
                    apiService.sendNotification(sender)
                            .enqueue(new Callback<MyResponse>(){
                                @Override
                                public void onResponse(@NonNull Call<MyResponse> call, Response<MyResponse> response) {

                                }

                                @Override
                                public void onFailure(@NonNull Call<MyResponse> call, Throwable t) {
                                    Log.e(TAG, "onFailure: ",t.fillInStackTrace() );
                                }
                            });

                    notify=false;
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private HashMap<String,Object> updateTypingStatus(int count,String otherUserId){
        HashMap<String,Object> typingStatus= new HashMap<>();
        if(count>=1){
            typingStatus.put("Typing", otherUserId);
        }else{
            typingStatus.put("Typing","none");
        }
        return typingStatus;
    }

    private void setTypingStatus(){
        DatabaseReference typingRef=database.getReference().child("UserDetails").child(fUser.getUid());
        newMessage.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                typingRef.updateChildren(updateTypingStatus(count,otherUserId));
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                typingRef.updateChildren(updateTypingStatus(count,otherUserId));
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
    }



    private void getCurrentWallpaper(String chatWallpaperUrI){

        int blur=settingsSharedPreferences.getInt("chatBlur",0);

        progressBar.setVisibility(VISIBLE);
        if (chatWallpaperUrI!=null){
            RequestBuilder requestBuilder= Glide.with(getApplicationContext()).load(chatWallpaperUrI);

            if(blur!=0) {
                requestBuilder= (RequestBuilder) requestBuilder.transform(new BlurTransformation(blur));
            }
            requestBuilder.addListener(new RequestListener<Drawable>() {
                @Override
                public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                    progressBar.setVisibility(GONE);
                    Log.e(TAG, "onLoadFailed: ",e );
                    return false;
                }

                @Override
                public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                    progressBar.setVisibility(GONE);


                    return false;
                }
            }).into(backgroundImageView);
        }else{
            backgroundImageView.setImageResource(R.drawable.def_wallpaper);
        }

   }



    private void statusCheck(userModel user){

            if(user.getOnline() && user.getShowOnlineState()){
                    onlineStatusView.setVisibility(VISIBLE);
                }else{
                onlineStatusView.setVisibility(GONE);
            }
            if(user.getTyping()!=null && user.getTyping().equals(fUser.getUid())){
                status.setVisibility(VISIBLE);
                status.setText(R.string.typing);
                status.setTextColor(getResources().getColor(R.color.lightSteelBlue));

            }else{


                    String state;

                    if(user.getShowOnlineState()){
                        status.setTextColor(getResources().getColor(R.color.eggshell));
                        if(user.getOnline() ){
                            status.setVisibility(VISIBLE);
                            state ="Online";
                            onlineStatusView.setVisibility(VISIBLE);
                            status.setSelected(false);
                            status.setText(state);

                        }else{
                            if(user.getShowLastSeenState()){
                                onlineStatusView.setVisibility(GONE);
                                status.setVisibility(VISIBLE);

                                String lastSeenDate=user.getLastSeenDate();
                                String lastSeenTime=user.getLastSeenTime();
                                if (lastSeenDate.equals(date)){
                                    state = "Last seen today at "+ lastSeenTime;
                                }else{
                                    state = "Last seen on " +lastSeenDate +" at "+ lastSeenTime;
                                }
                                status.setText(state);

                            }else{
                                status.setVisibility(GONE);
                            }


                        }




                }else{

                    status.setVisibility(GONE);
                    onlineStatusView.setVisibility(GONE);
                }
            }


    }

    private void loadSharedPreferenceData() {
        SharedPreferences sharedPreferences=getSharedPreferences("sharedPreferences",MODE_PRIVATE);
        otherUserId=sharedPreferences.getString("otherUserId",null);
        profileUrI=sharedPreferences.getString("profileUrI",null);
        otherUserName=sharedPreferences.getString("otherUserName",null);
        otherUserPref= sharedPreferences.getString("ouprefLang",null);
        getOtherUserDetails(otherUserId);

    }

    private void saveSharedPreferenceData() {
        SharedPreferences sharedPreferences =getSharedPreferences("sharedPreferences",MODE_PRIVATE);
        SharedPreferences.Editor editor=sharedPreferences.edit();
        editor.putString("otherUserId",otherUserId);
        editor.putString("profileUrI",profileUrI);
        editor.putString("otherUserName",otherUserName);
        editor.putString("ouprefLang",otherUserPref);


        editor.apply();
    }

    private void updateToken(String token) {
        DatabaseReference reference= FirebaseDatabase.getInstance().getReference("Tokens");
        Token token1 =new Token(token);
        reference.child(fUser.getUid()).setValue(token1);
    }

    private void InitialiseControllers() {
        ActionBar actionBar = getSupportActionBar();
        assert actionBar != null;
        actionBar.setTitle("");
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setDisplayShowCustomEnabled(true);
        LayoutInflater LayoutInflater=(LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        @SuppressLint("InflateParams") View actionBarView=LayoutInflater.inflate(R.layout.chat_custom_bar,null);
        actionBar.setCustomView(actionBarView);
        sendButton=findViewById(R.id.sendButton);
        status=findViewById(R.id.status);
        attachButton= findViewById(R.id.attachButton);
        profilePic=findViewById(R.id.image_profile);
        newMessage=findViewById(R.id.message_container);
        settingsSharedPreferences=getSharedPreferences("Settings",MODE_PRIVATE);
        progressBar=findViewById(R.id.progressBar2);
        backgroundImageView=findViewById(R.id.backgroundView);
        msgGroupDate=findViewById(R.id.msgGroupDateTop);
        onlineStatusView=findViewById(R.id.onlineStatusView);
        otherUserId=getIntent().getStringExtra("userId");
        otherUserName=getIntent().getStringExtra("userName");
        otherUserPref=getIntent().getStringExtra("preferredLang");

        fPhoneNumber= fUser.getPhoneNumber();
        userName=findViewById(R.id.userName);
        recyclerView=findViewById(R.id.recyclerView1);
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {

            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                if(messageList.size()>0){
                    int firstElementPosition = layoutManager.findFirstVisibleItemPosition();
                    int lastElementPosition = layoutManager.findLastCompletelyVisibleItemPosition();
                    try{

                        if(messageList.get(firstElementPosition).getDate().equals(new Tools().getDate())){
                            msgGroupDate.setVisibility(GONE);
                        }else{
                            msgGroupDate.setVisibility(VISIBLE);
                            msgGroupDate.setText(formatDate(messageList.get(Math.abs(firstElementPosition)).getDate()));
                        }
                        if (messageList.size()> lastElementPosition+1){
                            scrollButton.setVisibility(VISIBLE);
                        }else{
                            scrollButton.setVisibility(GONE);
                        }


                    }catch(Exception e){
                        Log.e(TAG, "onScrolled: ",e.fillInStackTrace());
                    }
                }
            }
        });
        scrollButton.setOnClickListener(I-> recyclerView.scrollToPosition(messageList.size()-1));

        database = FirebaseDatabase.getInstance();

        ImageButton emojiButton = findViewById(R.id.emoji_button);
        layoutActions=findViewById(R.id.layout_actions);
        imageButton=findViewById(R.id.att_image);
        videoButton=findViewById(R.id.att_vid);
        audioButton=findViewById(R.id.att_audio);
        textArea=findViewById(R.id.constraint_layout2);
        messageViewModel = new ViewModelProvider(this).get(MessageViewModel.class);
        userViewModel=new ViewModelProvider(this).get(UserViewModel.class);
        EmojIconActions emojiIcon=new EmojIconActions(getApplicationContext(),textArea,newMessage, emojiButton,"#495C66","#DCE1E2","#0B1830");
        emojiIcon.setIconsIds(R.drawable.ic_action_keyboard,R.drawable.smiley);
        emojiIcon.ShowEmojIcon();


    }

    private String formatDate(String date){
        String newDate;
        String month=null;
        switch(date.substring(3,5)){
            case "01":
                month="JAN";
                break;
            case "02":
                month="FEB";
                break;
            case "03":
                month="MAR";
                break;
            case "04":
                month="APR";
                break;
            case "05":
                month="MAY";
                break;
            case "06":
                month="JUNE";
                break;
            case "07":
                month="JULY";
                break;
            case "08":
                month="AUG";
                break;
            case "09":
                month="SEPT";
                break;
            case "10":
                month="OCT";
                break;
            case "11":
                month="NOV";
                break;
            case "12":
                month="DEC";
                break;

        }
        newDate=date.substring(0,2)+"-"+ month+ "-"+date.substring(6,10);
        return newDate;
    }

    private void getOtherUserDetails(String otherUserId) {
        userViewModel.initOtherUserInfo(otherUserId);
        userViewModel.getOtherUserInfo().observe(this,otherUserInfo ->{
            profileUrI=otherUserInfo.getProfileUrI();
            userName.setText(otherUserName);
            statusCheck(otherUserInfo);

            messageListAdapter.otherUserLang = otherUserInfo.getPreferredLang();
            if(profileUrI!=null){
                try {
                    Glide.with(getApplicationContext()).load(profileUrI).listener(new RequestListener<Drawable>() {
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
                    }).into(profilePic);
                } catch (Exception e) {
                    e.printStackTrace();
                }

            }else{
                progressBar.setVisibility(GONE);
                profilePic.setImageResource(R.drawable.ic_male_avatar_svgrepo_com);

            }
            profilePic.setOnClickListener(v -> {
                Intent intent =new Intent(getApplicationContext(), OtherUserActivity.class);
                intent.putExtra("otherUserId",otherUserId)
                        .putExtra("otherUserName",otherUserName)
                        .putExtra("otherUserLang",otherUserPref);
                startActivity(intent);
            });

        });

    }

    private void getMessages(){
        messageViewModel.getMessages().observe(this, messageListLive -> {
            messageList=messageListLive;
            messageListAdapter.notifyDataSetChanged();
            if (messageList.size() >= 1) {
                recyclerView.scrollToPosition(messageList.size()-1);
            }

        });
    }

    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(resultCode== Activity.RESULT_OK && data!=null) {
            Uri selected = data.getData();
            try{
                if(tools.isFileLessThan2MB(selected)) {
                    if (requestCode == IMAGEREQUEST) {
                        Intent intent = new Intent(getApplicationContext(), SendImage.class)
                                .putExtra("imageUrI", selected.toString())
                                .putExtra("otherUserId", otherUserId)
                                .putExtra("otherUserName", otherUserName);
                        startActivity(intent);
                    } else if (requestCode == VIDEOREQUEST) {
                        Intent intent = new Intent(getApplicationContext(), SendVideo.class)
                                .putExtra("videoUrI", selected.toString())
                                .putExtra("otherUserId", otherUserId)
                                .putExtra("otherUserName", otherUserName);
                        startActivity(intent);
                    } else if (requestCode == AUDIOREQUEST) {
                        Intent intent = new Intent(this, AudioUploadService.class);
                        String uri = selected.toString();
                        ResultReceiver myResultReceiver = new MyReceiver(null);
                        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
                        retriever.setDataSource(getApplicationContext(), selected);
                        String duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
                        intent.putExtra("fPhoneNumber", fPhoneNumber)
                                .putExtra("uri", uri)
                                .putExtra("receiver", myResultReceiver)
                                .putExtra("userId", fUser.getUid())
                                .putExtra("otherUserId", otherUserId)
                                .putExtra("audioDuration", duration);

                        startService(intent);
                    }
                }else{
                    Toast.makeText(this,"file size too large",Toast.LENGTH_SHORT).show();
                    textArea.setVisibility(View.GONE);
                }
            }catch(Exception e){
                Log.e(TAG, "onActivityResult:",e.fillInStackTrace());
            }
        }
    }

    @Override
    protected void onPause() {

        saveSharedPreferenceData();
       fpCountDown();
        super.onPause();
        releaseMediaPlayer();
    }

    private void fpCountDown(){
        if(settingsSharedPreferences.getBoolean("setFingerprint",false)){
            tools.context=getApplicationContext();
            ct=tools.setUpFPTime();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
            if(tools.fpTimeout){
                if(settingsSharedPreferences.getBoolean("setFingerprint",false)){
                    Intent intent = new Intent(ChatActivity.this,FingerprintActivity.class);
                    intent.putExtra("ActivityName",getClass().getCanonicalName());
                    startActivity(intent);
                }
            }else{
                if(ct!=null){
                    ct.cancel();
                }

            }

    }



    private void releaseMediaPlayer() {
        try {
            if (mediaPlayer != null) {
                if (mediaPlayer.isPlaying())
                    mediaPlayer.stop();
                mediaPlayer.release();
                mediaPlayer = null;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.chat_menu,menu);
        MenuItem profileDetails = menu.findItem(R.id.profileButton);
        MenuItem settings = menu.findItem(R.id.settingsButton);
        profileDetails.setOnMenuItemClickListener(item -> {
            Intent intent = new Intent(ChatActivity.this , UserProfileActivity.class)
                    .putExtra("userName",otherUserName);
            startActivity(intent);

            return false;
        });
        settings.setOnMenuItemClickListener(item -> {
            Intent intent = new Intent(ChatActivity.this , SettingsActivity.class);
            startActivity(intent);
            return false;
        });
        return true;

    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(item.getItemId()== android.R.id.home){
            onBackPressed();
        }

        return true;
    }



    @Override
    public void onBackPressed() {

        if(layoutActions.getVisibility()==VISIBLE){
            layoutActions.setVisibility(GONE);
            textArea.setVisibility(VISIBLE);
        }else{
            Intent mainIntent = new Intent(ChatActivity.this , MainActivity.class);
            mainIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(mainIntent);
            overridePendingTransition(android.R.anim.slide_in_left,android.R.anim.slide_out_right);
            finish();
        }

    }

    @Override
    public void onItemClick(int position) {
        Intent i= new Intent();

        String imageUrI=messageList.get(position).getImageUrI();
        String videoUrI=messageList.get(position).getVideoUrI();
        String caption =messageList.get(position).getText();

        if(imageUrI!=null){
            i=new Intent(this,ViewImageActivity.class);
            i.putExtra("imageUrI",imageUrI);
        }
        else if (videoUrI!=null){
            i=new Intent(this,ViewVideoActivity.class);
            i.putExtra("videoUrI",videoUrI);
        }
        i.putExtra("otherUserName",otherUserName);
        i.putExtra("caption",caption);

        if(messageList.get(position).getReceiver().equals(fUser.getUid())){
            i.putExtra("direction","from");
        }else{
            i.putExtra("direction","to");
        }
        if(messageList.get(position).getImageUrI()!=null || messageList.get(position).getVideoUrI()!=null && otherUserName!=null){
            startActivity(i);
        }
    }

    @Override
    public void onLongItemClick(int position) {}

    @Override
    public void getMediaPlayer(MediaPlayer mp) {
        mediaPlayer=mp;

    }

    public class MyReceiver extends ResultReceiver{
        public MyReceiver(Handler handler) {
            super(handler);
        }

        @Override
        protected void onReceiveResult(int resultCode, Bundle resultData) {
            super.onReceiveResult(resultCode, resultData);

                switch (resultCode){
                    case 100:
                        uploadImageTId=resultData.getString("uploadImageTId");
                        uploadImageTP=resultData.getInt("uploadImagePercentage");
                        uploadImageData.remove(null);
                        uploadImageData.put(uploadImageTId,uploadImageTP);
                        messageListAdapter.notifyDataSetChanged();

                        break;
                    case 200:
                        uploadVideoTId=resultData.getString("uploadVideoTId");
                        uploadVideoTP=resultData.getInt("uploadVideoPercentage");
                        uploadVideoData.remove(null);
                        uploadVideoData.put(uploadVideoTId,uploadVideoTP);
                        messageListAdapter.notifyDataSetChanged();
                        break;
                    case 300:
                        uploadAudioTId=resultData.getString("uploadAudioTId");
                        uploadAudioTP=resultData.getInt("uploadAudioPercentage");
                        uploadAudioData.remove(null);
                        uploadAudioData.put(uploadAudioTId,uploadAudioTP);
                        messageListAdapter.notifyDataSetChanged();
                }

        }
    }

    public Map<String,Integer> getUploadImageTaskData(){

        uploadImageData.put(uploadImageTId,uploadImageTP);

        return uploadImageData;
    }
    public Map<String,Integer> getUploadVideoTaskData(){

        uploadVideoData.put(uploadVideoTId,uploadVideoTP);

        return uploadVideoData;
    }
    public Map<String,Integer> getUploadAudioTaskData(){

        uploadAudioData.put(uploadAudioTId,uploadAudioTP);

        return uploadAudioData;
    }




}
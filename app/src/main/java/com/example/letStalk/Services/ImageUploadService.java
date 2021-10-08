package com.example.letStalk.Services;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.os.ResultReceiver;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.letStalk.Common.Tools;
import com.example.letStalk.Interfaces.APIService;
import com.example.letStalk.Model.messageListModel;
import com.example.letStalk.Notifications.Client;
import com.example.letStalk.Notifications.Data;
import com.example.letStalk.Notifications.Sender;
import com.example.letStalk.Notifications.Token;
import com.example.campaign.R;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.UnrecoverableEntryException;
import java.security.cert.CertificateException;
import java.security.interfaces.RSAPublicKey;
import java.util.List;

import static com.example.letStalk.Common.Tools.ALIAS;
import static com.example.letStalk.Common.Tools.getMimeType;

public class ImageUploadService extends Service {
    private FirebaseDatabase database;
    private StorageReference mStorageReference;
    boolean notify=false;

    private String fPhoneNumber;
    private String userId;
    private ResultReceiver myResultReceiver;
    private final Bundle bundle = new Bundle();
    private APIService apiService;
    private Tools tools;
    private String date;
    private String time;
    public static final String TAG="MAGEUPLOAD";
    private String otherUserPK;
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {

        return null;
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        database = FirebaseDatabase.getInstance();
        tools=new Tools();
        date= tools.getDate();
        time=tools.getTime();
        FirebaseStorage firebaseStorage = FirebaseStorage.getInstance();
        mStorageReference= firebaseStorage.getReference();
        myResultReceiver =  intent.getParcelableExtra("receiver");
        fPhoneNumber=intent.getStringExtra("fPhoneNumber");
        userId=intent.getStringExtra("userId");
        String otherUserId = intent.getStringExtra("otherUserId");
        String caption =intent.getStringExtra("caption");
        String uriString=intent.getStringExtra("uri");
        otherUserPK =intent.getStringExtra("otherUserPk");
        Log.d(TAG, "onStartCommand: "+ otherUserPK);
        Uri uri = Uri.parse(uriString);
        uploadImage(userId, otherUserId, uri,getApplicationContext(),caption);
        return START_NOT_STICKY;
    }

    private void uploadImage(String userId, String otherUserId, Uri uri, Context context, String caption){

        if(uri!=null){
            PublicKey fUserPublicKey;
            updateToken(FirebaseInstanceId.getInstance().getToken());
            apiService= Client.getClient("https://fcm.googleapis.com").create(APIService.class);


            DatabaseReference otherUserRef= database.getReference().child("chats").child(otherUserId).child(userId);
            DatabaseReference fUserChatRef= database.getReference().child("chats").child(userId).child(otherUserId).push();
            messageListModel fUserMessage=new messageListModel();
            fUserMessage.setTime(time);
            fUserMessage.setDate(date);
            fUserMessage.setType("IMAGE");
            try {
                KeyStore keyStore=KeyStore.getInstance("AndroidKeyStore");
                keyStore.load(null);
                KeyStore.PrivateKeyEntry privateKeyEntry = (KeyStore.PrivateKeyEntry)keyStore.getEntry(ALIAS, null);
                fUserPublicKey = (RSAPublicKey) privateKeyEntry.getCertificate().getPublicKey();
                fUserMessage.setImageUrI(tools.encrypt(String.valueOf(uri),fUserPublicKey));
                if(!caption.isEmpty()){
                    fUserMessage.setText(tools.encrypt(caption,fUserPublicKey));
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
            fUserMessage.setReceiver(otherUserId);
            fUserChatRef.setValue(fUserMessage);
            String messageKey=fUserChatRef.getKey();
            StorageReference fileReference;
            fileReference = mStorageReference.child(System.currentTimeMillis()
                    + getMimeType(context,uri));

            fileReference.putFile(uri).addOnProgressListener(taskSnapshot -> {

                @SuppressWarnings("IntegerDivisionInFloatingPointContext") double progress = 100.0 * (taskSnapshot.getBytesTransferred() / taskSnapshot.getTotalByteCount());
                int currentProgress = (int) progress;
                currentProgress=(int)((float)(-0.25*currentProgress)+25);
                if(currentProgress==0){
                    currentProgress=1;
                }
                bundle.putInt("uploadImagePercentage",currentProgress);
                bundle.putString("uploadImageTId",messageKey);

                myResultReceiver.send(100,bundle);
            }).addOnPausedListener(taskSnapshot -> System.out.println("Upload is paused")).continueWithTask(task -> {
                if (!task.isSuccessful()) {
                    throw task.getException();
                }
                return fileReference.getDownloadUrl();
            }).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    Uri downloadUri = task.getResult();
                    messageListModel messageOtherUser=new messageListModel();
                    messageOtherUser.setTime(time);
                    messageOtherUser.setDate(date);
                    messageOtherUser.setType("IMAGE");
                    messageOtherUser.setReceiver(otherUserId);
                    try {
                        messageOtherUser.setImageUrI(tools.encrypt(downloadUri.toString(), tools.initPublic(otherUserPK)));
                        if(!caption.isEmpty()){
                            messageOtherUser.setText(tools.encrypt(caption,tools.initPublic(otherUserPK)));
                        }

                    } catch (Exception e) {

                        Log.e("ImageUpload", "uploadImage: ", e);
                    }
                    otherUserRef.child(messageKey).setValue(messageOtherUser);
                    notify=true;
                    if(notify){
                        sendNotification(otherUserId,fPhoneNumber);
                    }
                }
            });
        }

    }


    private void updateToken(String token) {
        DatabaseReference reference= FirebaseDatabase.getInstance().getReference("Tokens");
        Token token1 =new Token(token);
        reference.child(userId).setValue(token1);
    }


    private void sendNotification(String otherUserId, String fPhoneNumber) {
        DatabaseReference tokens=database.getReference("Tokens");
        Query query=tokens.orderByKey().equalTo(otherUserId);
        query.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for(DataSnapshot dataSnapshot:snapshot.getChildren()){
                    Token token=dataSnapshot.getValue(Token.class);
                    Data data=new Data(userId, R.mipmap.ic_launcher2, "IMAGE",fPhoneNumber,otherUserId,"New message");
                    Sender sender = new Sender(data,token.getToken());
                    apiService.sendNotification(sender);
                    notify=false;
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }





}
package com.example.campaign.Activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.provider.ContactsContract;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Toast;

import com.example.campaign.Model.chatListModel;
import com.example.campaign.Model.userModel;
import com.example.campaign.R;
import com.example.campaign.adapter.chatListAdapter;
import com.example.campaign.adapter.userListAdapter;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class userListActivity extends AppCompatActivity {
    private List<String> chatListId;
    private RecyclerView recyclerView;

    private FirebaseUser user;
    private FirebaseDatabase database;
    private List<userModel> list;
    private com.example.campaign.adapter.userListAdapter userListAdapter;
    private Handler handler;
    private String lastMessage,time,date,userName,profileUrI;
    private String TAG ="userAct";
    private Context context;
    public  Map<String, String> namePhoneMap ;
    private int CONTACTS_REQUEST=110;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_list);
        InitializeControllers();
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setHasFixedSize(true);
        database = FirebaseDatabase.getInstance();
        userListAdapter=new userListAdapter(list,userListActivity.this);
        recyclerView.setAdapter(userListAdapter);

    }

    @Override
    protected void onStart() {
        super.onStart();
        if (ContextCompat.checkSelfPermission(context,
                Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) {
            loadUsers(getPhoneNumbers());

        } else {
            requestContactsPermission();
        }

    }

    private void InitializeControllers() {
        user= FirebaseAuth.getInstance().getCurrentUser();
        recyclerView=findViewById(R.id.recyclerViewUserList);
        list=new ArrayList<>();
        context=getApplicationContext();
        namePhoneMap= new HashMap<String, String>();
        ActionBar actionBar=getSupportActionBar();
        actionBar.setTitle("Select Contact");
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setDisplayShowCustomEnabled(true);

    }

    private void loadUsers(List<String> contacts){
        DatabaseReference userDetails=database.getReference().child("UserDetails");
        userDetails.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List <String> phoneNumbersList=new ArrayList<>();
                list.clear();
                for (DataSnapshot dataSnapshot:snapshot.getChildren()){

                    userModel userListObj=new userModel();
                    String id=dataSnapshot.getKey();
                    userModel users=dataSnapshot.getValue(userModel.class);
                    String phoneNumber=user.getPhoneNumber();
                    phoneNumbersList.add(user.getPhoneNumber());

                    int i= Collections.frequency(phoneNumbersList,users.getPhoneNumber());
                    if (contacts!=null && !users.getPhoneNumber().equals(phoneNumber)) {
                        if (i <=1 && contacts.contains(phoneNumber)){
                            userListObj.setUserName(users.getUserName());
                            userListObj.setPhoneNumber(users.getPhoneNumber());
                            userListObj.setProfileUrI(users.getProfileUrI());
                            userListObj.setUserId(id);
                            list.add(userListObj);
                            userListAdapter.notifyDataSetChanged();

                        }
                    }
                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    public boolean isAlphanumeric2(String str) {
        for (int i=0; i<str.length(); i++) {
            char c = str.charAt(i);
            if (c < 0x30 || (c >= 0x3a && c <= 0x40) || (c > 0x5a && c <= 0x60) || c > 0x7a)
                return false;
        }
        return true;
    }
    private List<String> getPhoneNumbers() {
        List<String> phoneNumbers=new ArrayList<>();
        Cursor phones = context.getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null, null, null, null);

        // Loop Through All The Numbers
        while (phones.moveToNext()) {

            String name = phones.getString(phones.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME));
            String phoneNumber = phones.getString(phones.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));

            // Cleanup the phone number
            phoneNumber = phoneNumber.replaceAll("[()\\s-]+", "");

            // Enter Into Hash Map
            namePhoneMap.put(phoneNumber, name);

        }
        for (Map.Entry<String, String> entry : namePhoneMap.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            if (key.contains("+")){
                phoneNumbers.add(key);
            }else{
                if(isAlphanumeric2(key)){
                    Long i=Long.parseLong(key);
                    String j="+256"+i;
                    phoneNumbers.add(j);
                }
            }
        }
        phones.close();
        return phoneNumbers;
    }

    private void requestContactsPermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                Manifest.permission.READ_CONTACTS)) {
            new AlertDialog.Builder(getApplicationContext())
                    .setTitle("Permission needed")
                    .setMessage("This permission is needed because we require access to your contacts")
                    .setPositiveButton("ok", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            ActivityCompat.requestPermissions(userListActivity.this,
                                    new String[] {Manifest.permission.READ_CONTACTS}, CONTACTS_REQUEST);
                        }
                    })
                    .setNegativeButton("cancel", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    })
                    .create().show();
        } else {
            ActivityCompat.requestPermissions(userListActivity.this,
                    new String[] {Manifest.permission.READ_CONTACTS}, CONTACTS_REQUEST);
        }
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == CONTACTS_REQUEST)  {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                loadUsers(getPhoneNumbers());

            } else {
                Toast.makeText(getApplicationContext(), "Permission DENIED", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:

                onBackPressed();
                break;
        }
        return true;
    }

    @Override
    public void onBackPressed() {
        Intent mainIntent = new Intent(userListActivity.this , chatListActivity.class);
        startActivity(mainIntent);
        finish();
    }
}
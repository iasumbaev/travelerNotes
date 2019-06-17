package com.example.ivan.travelernotes.Add;

import android.content.Intent;
import android.graphics.Bitmap;
import android.media.Image;
import android.os.Build;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.ivan.travelernotes.R;
import com.example.ivan.travelernotes.Utils.FirebaseMethods;
import com.example.ivan.travelernotes.Utils.UniversalImageLoader;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

public class NextActivity extends AppCompatActivity {

    private static final String TAG = "NextActivity";

    //firebase
    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener mAuthStateListner;
    private FirebaseDatabase mFirebaseDatabase;
    private DatabaseReference myRef;
    private FirebaseMethods mFirebaseMethods;

    //widgets
    private EditText mCaption;
    private EditText mCountry;
    private LinearLayout imagesWrapper;

    //vars
    private String mAppend = "file:/";
    private int imageCount = 0;
    private String imgUrl;
    private Bitmap bitmap;
    private Intent intent;
    private ArrayList<String> imgUrlsList;
    private ArrayList<String> captionList;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_next);
        mFirebaseMethods = new FirebaseMethods(NextActivity.this);
        //mCaption = (EditText) findViewById(R.id.caption);
        mCountry = (EditText) findViewById(R.id.country);
        imagesWrapper = (LinearLayout) findViewById(R.id.imagesViewWrapper);

        captionList = new ArrayList<String>();



        setFirebaseAuth();

        ImageView backArrow = (ImageView) findViewById(R.id.ivBackArrow);
        backArrow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "onClick: closing the activity");
                finish();
            }
        });

        TextView share = (TextView) findViewById(R.id.tvShare);
        share.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "onClick: navigating to the final share screen.");
                //upload the image to firebase
                Toast.makeText(NextActivity.this, "Подождите... Идёт загрузка фотографий", Toast.LENGTH_SHORT).show();

                //get caption from each photo
                int countChild = imagesWrapper.getChildCount();
                for (int i = 0; i < countChild; i++) {
                    LinearLayout ll = (LinearLayout) imagesWrapper.getChildAt(i);
                    EditText et = (EditText) ll.getChildAt(1);
                    Log.d(TAG, "onClick: " + String.valueOf(et.getText()));
                    captionList.add(String.valueOf(et.getText()));
                }

//                String caption = mCaption.getText().toString();
                String title = mCountry.getText().toString();
                if(title.equals("")){
                    Toast.makeText(NextActivity.this, "Добавьте название статьи", Toast.LENGTH_SHORT).show();
                }
                else {
                    if (intent.hasExtra(getString(R.string.selected_images))) {
                        imgUrlsList = intent.getStringArrayListExtra(getString(R.string.selected_images));
                        mFirebaseMethods.uploadNewPost(captionList, title, imageCount, imgUrlsList);
                    }
                }

               /* if (intent.hasExtra(getString(R.string.selected_image))) {
                    imgUrl = intent.getStringExtra(getString(R.string.selected_image));
                    mFirebaseMethods.uploadNewPhoto(getString(R.string.new_photo), caption, imageCount, imgUrl, null);
                } else if (intent.hasExtra(getString(R.string.selected_bitmap))) {
                    bitmap = intent.getParcelableExtra(getString(R.string.selected_bitmap));
                    mFirebaseMethods.uploadNewPhoto(getString(R.string.new_photo), caption, imageCount, null, bitmap);
                } */

            }
        });

        setImage();
    }

    /**
     * gets the image url from the incoming intent and displays a chosen image
     */
    private void setImage() {
        intent = getIntent();
        //ImageView image = (ImageView) findViewById(R.id.imageShare);

       /* if (intent.hasExtra(getString(R.string.selected_image))) {
            imgUrl = intent.getStringExtra(getString(R.string.selected_image));
            UniversalImageLoader.setImage(imgUrl, image, null, mAppend);
        } else if (intent.hasExtra(getString(R.string.selected_bitmap))) {
            bitmap = intent.getParcelableExtra(getString(R.string.selected_bitmap));
            Log.d(TAG, "setImage: got new bitmap");
            image.setImageBitmap(bitmap);
        }*/

        if(intent.hasExtra(getString(R.string.selected_images))) {
            imgUrlsList = intent.getStringArrayListExtra(getString(R.string.selected_images));
            for(int i = 0; i < imgUrlsList.size(); i++) {
                ImageView iv = new ImageView(getApplicationContext());
                RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(200, 200);

                LinearLayout.LayoutParams lpLL = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                lpLL.bottomMargin = 20;

                LinearLayout tempLL = new LinearLayout(getApplicationContext());

                tempLL.setOrientation(LinearLayout.HORIZONTAL);
                tempLL.setLayoutParams(lpLL);
                EditText et = new EditText(getApplicationContext());
                et.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
                et.setHint("Введите описание фотографии");
                iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
                iv.setLayoutParams(lp);
                tempLL.addView(iv);
                tempLL.addView(et);
                imagesWrapper.addView(tempLL);
                UniversalImageLoader.setImage(imgUrlsList.get(i), iv, null, mAppend);
            }

        }



    }

    /*----------------------------------- Firebase -------------------------------*/

    private void setFirebaseAuth() {
        Log.d(TAG, "setFirebaseAuth: setting up firebase auth");
        mAuth = FirebaseAuth.getInstance();
        mFirebaseDatabase = FirebaseDatabase.getInstance();
        myRef = mFirebaseDatabase.getReference();
        Log.d(TAG, "onDataChange: image count: " + imageCount);

        mAuthStateListner = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser currentUser = mAuth.getCurrentUser();

                if (currentUser != null) {
                    Log.d(TAG, "onStart: signed_in: " + currentUser.getUid());
                } else {

                    Log.d(TAG, "onStart: signed_out");
                }
            }
        };

        myRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                imageCount = mFirebaseMethods.getPostCount(dataSnapshot);
                Log.d(TAG, "onDataChange: image count: " + imageCount);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        mAuth.addAuthStateListener(mAuthStateListner);
    }

    @Override
    public void onStop() {
        super.onStop();
        if (mAuthStateListner != null) {
            mAuth.removeAuthStateListener(mAuthStateListner);
        }
    }

}

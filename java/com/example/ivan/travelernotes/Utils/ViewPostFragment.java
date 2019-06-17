package com.example.ivan.travelernotes.Utils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.util.TypedValue;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import com.example.ivan.travelernotes.R;
import com.example.ivan.travelernotes.SearchArticles.SearchArticlesActivity;
import com.example.ivan.travelernotes.models.Comment;
import com.example.ivan.travelernotes.models.Like;
import com.example.ivan.travelernotes.models.Photo;
import com.example.ivan.travelernotes.models.Post;
import com.example.ivan.travelernotes.models.User;
import com.example.ivan.travelernotes.models.UserAccountSettings;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.ittianyu.bottomnavigationviewex.BottomNavigationViewEx;

import java.lang.reflect.TypeVariable;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

public class ViewPostFragment extends Fragment {

    private static final String TAG = "ViewPostFragment";

    public interface OnCommentThreadSelectedListener {
        void onCommentThreadSelectedListener(Post post);
    }

    OnCommentThreadSelectedListener mOnCommentThreadSelectedListener;

    public ViewPostFragment() {
        super();
        setArguments(new Bundle());
    }

    //firebase
    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener mAuthStateListner;
    private FirebaseDatabase mFirebaseDatabase;
    private DatabaseReference myRef;
    private FirebaseMethods mFirebaseMethods;

    //widgets
    private SquareImageView mPostImage;
    private BottomNavigationViewEx bottomNavigationView;
    private TextView mBackLabel, mCaption, mUsername, mTimestamp, mLikes, mComments, mTitle;
    private ImageView mBackArrow, mEllipses, mHeartRed, mHeartWhite, mProfileImage, mComment;
    TableLayout tlImages;
    private LinearLayout ll;

    //vars
    private Photo mPhoto;
    private Post mPost;
    private int mActivityNumber = 0;
    private String photoUsername;
    private String photoUrl;
    private UserAccountSettings mUserAccountSettings;
    private GestureDetector mGestureDetector;
    private Heart mHeart;
    private Boolean mLikedByCurrentUser;
    private StringBuilder mUsers;
    private String mLikesString = "";
    private User mCurrentUser;
    private Context context;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_view_post, container, false);
        bottomNavigationView = (BottomNavigationViewEx) view.findViewById(R.id.bottomNavViewBar);
        mBackArrow = (ImageView) view.findViewById(R.id.backArrow);
        mUsername = (TextView) view.findViewById(R.id.username);
        mTimestamp = (TextView) view.findViewById(R.id.image_time_posted);
        mHeartRed = (ImageView) view.findViewById(R.id.image_heart_red);
        mHeartWhite = (ImageView) view.findViewById(R.id.image_heart);
        mProfileImage = (ImageView) view.findViewById(R.id.profile_photo);
        mLikes = (TextView) view.findViewById(R.id.image_likes);
        mComment = (ImageView) view.findViewById(R.id.speech_bubble);
        mComments = (TextView) view.findViewById(R.id.image_comments_link);
        mTitle = (TextView) view.findViewById(R.id.title);
        ll = (LinearLayout) view.findViewById(R.id.postWrapper);

        mHeart = new Heart(mHeartWhite, mHeartRed);
        mGestureDetector = new GestureDetector(getActivity(), new GestureListener());

        context = getContext();
        if(getActivity() == null) {
            context = (SearchArticlesActivity.getContext());
        }


        setFirebaseAuth();
        setupBottomNavigationView();
        //setupWidgets();

        return view;
    }

    @SuppressLint("ResourceAsColor")
    private void init() {
        try {
            // mPhoto = getPhotoFromBundle();
//mPhoto = getPhotoFromBundle();
            int imgCount = getPostFromBundle().getImage_path().size();
            Log.d(TAG, "init: imgCount = " + imgCount);
            int counterImgs = 0;
            int amountRows = imgCount / 3;
            if (imgCount % 3 != 0) {
                amountRows++;
            }
            int imageWidth = getResources().getDisplayMetrics().widthPixels / 3;

            /*for(int i = 0; i < amountRows  && counterImgs < imgCount; i++) {

                TableRow tr = new TableRow(getActivity());
               // tr.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, TableRow.LayoutParams.WRAP_CONTENT));

                TableLayout.LayoutParams layoutParams = new TableLayout.LayoutParams();


                for(int j = 0; j < 3 && counterImgs < imgCount; j++) {

                    SquareImageView view = new SquareImageView(getActivity());
                    view.setMaxWidth(imageWidth);
                    tr.addView(view);

                    view.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            if(view.getMinimumWidth() != getResources().getDisplayMetrics().widthPixels) {
                                view.setMinimumWidth(getResources().getDisplayMetrics().widthPixels);
                            } else {
                                view.setMinimumWidth(getResources().getDisplayMetrics().widthPixels / 3);
                            }
                        }
                    });

                    UniversalImageLoader.setImage(getPostFromBundle().getImage_path().get(counterImgs), view, null, "");
                    Log.d(TAG, "init: image_path: " + getPostFromBundle().getImage_path().get(counterImgs++));
                }
                tlImages.addView(tr);

            }*/

           /* for(int i = 0; i < imgCount; i++) {

                TableRow tr = new TableRow(getActivity());
                tr.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, TableRow.LayoutParams.WRAP_CONTENT));
                SquareImageView view = new SquareImageView(getActivity());
                view.setScaleType(ImageView.ScaleType.CENTER_CROP);
               *//* ViewGroup.LayoutParams lp = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
                view.setLayoutParams(lp);*//*
                tr.addView(view);
                UniversalImageLoader.setImage(getPostFromBundle().getImage_path().get(i), view, null, "");
                tlImages.addView(tr);
            }*/
            for (int i = 0; i < imgCount; i++) {
                SquareImageView image = new SquareImageView(getActivity());
                image.setScaleType(ImageView.ScaleType.CENTER_CROP);
                UniversalImageLoader.setImage(getPostFromBundle().getImage_path().get(i), image, null, "");
                ll.addView(image);
                TextView caption = new TextView(getActivity());
                //caption.setText(getPostFromBundle().getCaption().get(i));
                caption.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
                caption.setPadding(0,10,0,10);
                //caption.setTextAlignment();
                caption.setTextColor(Color.parseColor("#000000"));
                ll.addView(caption);

                Log.d(TAG, "init: setting photos " /*+ getPostFromBundle().getCaption().get(i)*/);

            }

            mActivityNumber = getActivityNumberFromBundle();
            String post_id = getPostFromBundle().getPost_id();

            Query query = FirebaseDatabase.getInstance().getReference()
                    .child(context.getString(R.string.dbname_posts))
                    .orderByChild(context.getString(R.string.field_post_id))
                    .equalTo(post_id);
            query.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    for (DataSnapshot singleSnapshot : dataSnapshot.getChildren()) {
                        Post newPost = new Post();
                        Map<String, Object> objectMap = (HashMap<String, Object>) singleSnapshot.getValue();

                        //TODO: caption to array list
                        //newPost.setCaption(objectMap.get(context.getString(R.string.field_caption)).toString());
                        newPost.setTags(objectMap.get(context.getString(R.string.filed_tags)).toString());
                        newPost.setUser_id(objectMap.get(context.getString(R.string.field_user_id)).toString());
                        newPost.setData_created(objectMap.get(context.getString(R.string.field_data_created)).toString());
                        newPost.setPost_id(objectMap.get(context.getString(R.string.field_post_id)).toString());
                        newPost.setCountry(objectMap.get(context.getString(R.string.field_country)).toString());

                        ArrayList<String> imageUrls = new ArrayList<String>();
                        for (DataSnapshot dSnapshot : singleSnapshot
                                .child(context.getString(R.string.filed_image_path)).getChildren()) {
                            imageUrls.add(dSnapshot.getValue(String.class));
                            Log.d(TAG, "onDataChange: found img_path: " + imageUrls.get(imageUrls.size() - 1));
                        }

                        newPost.setImage_path(imageUrls);

                        ArrayList<String> captions = new ArrayList<String>();
                        for (DataSnapshot dSnapshot : singleSnapshot
                                .child(context.getString(R.string.field_caption)).getChildren()) {
                            captions.add(dSnapshot.getValue(String.class));
                            Log.d(TAG, "onDataChange: found caption: " + captions.get(captions.size() - 1));
                        }

                        newPost.setCaption(captions);

                        ArrayList<String> photoIds = new ArrayList<String>();
                        for (DataSnapshot dSnapshot : singleSnapshot
                                .child(context.getString(R.string.field_photo_id)).getChildren()) {
                            photoIds.add((String) dataSnapshot.getValue());
                            Log.d(TAG, "onDataChange: found photo_id: " + photoIds.get(imageUrls.size() - 1));
                        }

                        newPost.setPhoto_ids(photoIds);

                        List<Comment> commentsList = new ArrayList<Comment>();
                        for (DataSnapshot dSnapshot : singleSnapshot
                                .child(context.getString(R.string.field_comments)).getChildren()) {
                            Comment comment = new Comment();
                            comment.setUser_id(dSnapshot.getValue(Comment.class).getUser_id());
                            comment.setComment(dSnapshot.getValue(Comment.class).getComment());
                            comment.setDate_created(dSnapshot.getValue(Comment.class).getDate_created());
                            commentsList.add(comment);
                        }
                        newPost.setComments(commentsList);

                        mPost = newPost;

                        getCurrentUser();
                        getPostDetails();
                        //getLikesString();

                    }

                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    Log.d(TAG, "onCancelled: query cancelled.");
                }
            });

        } catch (NullPointerException e) {
            Log.e(TAG, "onCreateView: NullPointerException: " + e.getMessage());
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (isAdded()) {
            init();
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            mOnCommentThreadSelectedListener = (OnCommentThreadSelectedListener) getActivity();
        } catch (ClassCastException e) {
            Log.e(TAG, "onAttachFragment: ClassCastException" + e.getMessage());
        }
    }

    private void getLikesString() {

        Log.d(TAG, "getLikesString: getting likes string");
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference();
        Query query = reference
                .child(context.getString(R.string.dbname_posts))
                .child(mPost.getPost_id())
                .child(context.getString(R.string.field_likes));

        final Context finalContext = context;
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                mUsers = new StringBuilder();
                for (DataSnapshot singleSnapshot : dataSnapshot.getChildren()) {
                    DatabaseReference reference = FirebaseDatabase.getInstance().getReference();
                    Query query = reference
                            .child(finalContext.getString(R.string.dbname_users))
                            .orderByChild(finalContext.getString(R.string.field_user_id))
                            .equalTo(singleSnapshot.getValue(Like.class).getUser_id());

                    query.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            for (DataSnapshot singleSnapshot : dataSnapshot.getChildren()) {
                                Log.d(TAG, "onDataChange: found like: " + singleSnapshot.getValue(User.class).getUsername());

                                mUsers.append(singleSnapshot.getValue(User.class).getUsername());
                                mUsers.append(",");
                            }

                            String[] splitUsers = mUsers.toString().split(",");

                            if (mUsers.toString().contains(mCurrentUser.getUsername() + ",")) {
                                mLikedByCurrentUser = true;
                            } else {
                                mLikedByCurrentUser = false;
                            }

                            int length = splitUsers.length;
                            if (length == 1) {
                                mLikesString = "Понравилось " + splitUsers[0];
                            } else if (length == 2) {
                                mLikesString = "Понравилось " + splitUsers[0]
                                        + " и " + splitUsers[1];
                            } else if (length == 3) {
                                mLikesString = "Понравилось " + splitUsers[0]
                                        + ", " + splitUsers[1]
                                        + " и " + splitUsers[2];
                            } else if (length == 4) {
                                mLikesString = "Понравилось " + splitUsers[0]
                                        + ", " + splitUsers[1]
                                        + ", " + splitUsers[2]
                                        + " и " + splitUsers[3];
                            } else if (length > 4) {
                                mLikesString = "Понравилось " + splitUsers[0]
                                        + ", " + splitUsers[1]
                                        + ", " + splitUsers[2]
                                        + " и ещё " + (splitUsers.length - 3) + " пользователям";
                            }
                            setupWidgets();
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {

                        }
                    });
                }
                if (!dataSnapshot.exists()) {
                    mLikesString = "";
                    mLikedByCurrentUser = false;
                    setupWidgets();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void getCurrentUser() {
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference();
        Query query = reference
                .child(context.getString(R.string.dbname_users))
                .orderByChild(context.getString(R.string.field_user_id))
                .equalTo(FirebaseAuth.getInstance().getCurrentUser().getUid());
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot singleSnaphot : dataSnapshot.getChildren()) {
                    mCurrentUser = singleSnaphot.getValue(User.class);
                }
                getLikesString();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.d(TAG, "onCancelled: query cancelled.");
            }
        });
    }


    public class GestureListener extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onDown(MotionEvent e) {
            return true;
        }

        @Override
        public boolean onDoubleTap(MotionEvent e) {

            DatabaseReference reference = FirebaseDatabase.getInstance().getReference();
            Query query = reference
                    .child(context.getString(R.string.dbname_posts))
                    .child(mPost.getPost_id())
                    .child(context.getString(R.string.field_likes));

            query.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    for (DataSnapshot singleSnapshot : dataSnapshot.getChildren()) {

                        String keyID = singleSnapshot.getKey();

                        //case1: Then user already liked the photo
                        if (mLikedByCurrentUser &&
                                singleSnapshot.getValue(Like.class).getUser_id()
                                        .equals(FirebaseAuth.getInstance().getCurrentUser().getUid())) {
                            myRef.child(context.getString(R.string.dbname_posts))
                                    .child(mPost.getPost_id())
                                    .child(context.getString(R.string.field_likes))
                                    .child(keyID)
                                    .removeValue();

                            myRef.child(context.getString(R.string.dbname_user_posts))
                                    .child(FirebaseAuth.getInstance().getCurrentUser().getUid())
                                    .child(mPost.getPost_id())
                                    .child(context.getString(R.string.field_likes))
                                    .child(keyID)
                                    .removeValue();

                            mHeart.toggleLike();
                            getLikesString();
                        }
                        //case2: The user has not liked photo
                        else if (!mLikedByCurrentUser) {
                            //add new like
                            addNewLike();
                            break;
                        }
                    }
                    if (!dataSnapshot.exists()) {
                        addNewLike();
                        //add new like
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                }
            });

            return true;
        }
    }

    private void addNewLike() {
        Log.d(TAG, "addNewLike: adding new like");

        String newLikeID = myRef.push().getKey();
        Like like = new Like();
        like.setUser_id(FirebaseAuth.getInstance().getCurrentUser().getUid());


        myRef.child(context.getString(R.string.dbname_posts))
                .child(mPost.getPost_id())
                .child(context.getString(R.string.field_likes))
                .child(newLikeID)
                .setValue(like);

        myRef.child(context.getString(R.string.dbname_user_posts))
                .child(mPost.getUser_id())
                .child(mPost.getPost_id())
                .child(context.getString(R.string.field_likes))
                .child(newLikeID)
                .setValue(like);

        mHeart.toggleLike();
        getLikesString();
    }

    private void getPostDetails() {
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference();
        Query query = reference
                .child(context.getString(R.string.dbname_users_account_settings))
                .orderByChild(context.getString(R.string.field_user_id))
                .equalTo(mPost.getUser_id());
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot singleSnaphot : dataSnapshot.getChildren()) {
                    mUserAccountSettings = singleSnaphot.getValue(UserAccountSettings.class);
                }
                // setupWidgets();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.d(TAG, "onCancelled: query cancelled.");
            }
        });
    }

    private void setupWidgets() {
        mTitle.setText(mPost.getCountry());
        String timestampDiff = getTimestampDifference();
        if (!timestampDiff.equals("0")) {
            mTimestamp.setText(timestampDiff + " ДНЕЙ НАЗАД");
        } else {
            mTimestamp.setText("СЕГОДНЯ");
        }
/*
        UniversalImageLoader.setImage(mUserAccountSettings.getProfile_photo(), mProfileImage, null, "");
        mUsername.setText(mUserAccountSettings.getUsername());*/
        mLikes.setText(mLikesString);
        //TODO: caption to array list
        for (int i = 1, j=0; i < ll.getChildCount(); i += 2, j++) {
            TextView caption = (TextView) ll.getChildAt(i);
            caption.setText(mPost.getCaption().get(j));
        }
        //mCaption.setText(mPost.getCaption());
        mTitle.setText(mPost.getCountry());

        if (mPost.getComments().size() > 0) {
            mComments.setText("Посмтореть все " + mPost.getComments().size() + " комментариев");
        } else {
            mComments.setText("");
        }

        mComments.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "onClick: navigating to comments thread");
                mOnCommentThreadSelectedListener.onCommentThreadSelectedListener(mPost);
            }
        });

        mBackArrow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "onClick: navigating back");
                getActivity().getSupportFragmentManager().popBackStack();
            }
        });

        mComment.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "onClick: navigating back");
                mOnCommentThreadSelectedListener.onCommentThreadSelectedListener(mPost);
            }
        });

        if (mLikedByCurrentUser) {
            mHeartWhite.setVisibility(View.GONE);
            mHeartRed.setVisibility(View.VISIBLE);
            mHeartRed.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View view, MotionEvent motionEvent) {
                    return mGestureDetector.onTouchEvent(motionEvent);
                }
            });
        } else {
            mHeartWhite.setVisibility(View.VISIBLE);
            mHeartRed.setVisibility(View.GONE);
            mHeartWhite.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View view, MotionEvent motionEvent) {
                    return mGestureDetector.onTouchEvent(motionEvent);
                }
            });
        }

    }


    /**
     * Returns a string representing the number of days ago the post was made
     *
     * @return
     */
    private String getTimestampDifference() {
        String difference = "";
        Calendar c = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.CANADA);
        sdf.setTimeZone(TimeZone.getTimeZone("Canada/Pacific"));
        Date today = c.getTime();
        sdf.format(today);
        Date timestamp;
        final String photoTimestamp = mPost.getData_created();
        try {
            timestamp = sdf.parse(photoTimestamp);
            difference = String.valueOf(Math.round(((today.getTime() - timestamp.getTime()) / 1000 / 60 / 60 / 24)));
        } catch (ParseException e) {
            Log.e(TAG, "getTimestampDifference: ParseException" + e.getMessage());
            difference = "0";
        }

        return difference;
    }

    /**
     * retrieve the activity number from incoming bundle from profileActivivty interface
     *
     * @return
     */
    private int getActivityNumberFromBundle() {
        Log.d(TAG, "getActivityNumberFromBundle: arguments: " + getArguments());

        Bundle bundle = this.getArguments();

        if (bundle != null) {
            return bundle.getInt(context.getString(R.string.activity_number));
        } else {
            return 0;
        }
    }

    /**
     * retrieve the photo from incoming bundle from profileActivivty interface
     *
     * @return
     */
    private Photo getPhotoFromBundle() {
        Log.d(TAG, "getPhotoFromBundle: arguments: " + getArguments());

        Bundle bundle = this.getArguments();

        if (bundle != null) {
            return bundle.getParcelable(context.getString(R.string.photo));
        } else {
            return null;
        }
    }

    /**
     * retrieve the post from incoming bundle from profileActivivty interface
     *
     * @return
     */
    private Post getPostFromBundle() {
        Log.d(TAG, "getPostFromBundle: arguments: " + getArguments());

        Bundle bundle = this.getArguments();

        if (bundle != null) {
            return bundle.getParcelable(context.getString(R.string.post));
        } else {
            return null;
        }
    }

    /**
     * BottomNavigationView setup
     */
    private void setupBottomNavigationView() {
        Log.d(TAG, "setupBottomNavigationView: setting up BottomNavigationView");
        BottomNavigationViewHelper.setupBottomNavigationView(bottomNavigationView);
        BottomNavigationViewHelper.enableNavigation(getActivity(), getActivity(), bottomNavigationView);
        Menu menu = bottomNavigationView.getMenu();
        MenuItem menuItem = menu.getItem(mActivityNumber);
        menuItem.setChecked(true);
    }

    /*----------------------------------- Firebase -------------------------------*/

    private void setFirebaseAuth() {
        Log.d(TAG, "setFirebaseAuth: setting up firebase auth");
        mAuth = FirebaseAuth.getInstance();
        mFirebaseDatabase = FirebaseDatabase.getInstance();
        myRef = mFirebaseDatabase.getReference();

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

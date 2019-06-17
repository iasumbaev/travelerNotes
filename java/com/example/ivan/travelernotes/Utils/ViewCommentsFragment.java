package com.example.ivan.travelernotes.Utils;

import android.content.Context;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.ivan.travelernotes.Feed.FeedActivity;
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
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.ittianyu.bottomnavigationviewex.BottomNavigationViewEx;

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

public class ViewCommentsFragment extends Fragment {

    private static final String TAG = "ViewCommentsFragment";
    Context context;

    public ViewCommentsFragment() {
        super();
        setArguments(new Bundle());
    }

    //firebase
    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener mAuthStateListner;
    private FirebaseDatabase mFirebaseDatabase;
    private DatabaseReference myRef;

    //widgets
    private ImageView mBackArrow, mCheckMark;
    private EditText mComment;
    private ListView mListView;


    //vars
    private Post mPost;
    private ArrayList<Comment> mComments;
    private Context mContext;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_view_comments, container, false);
        mBackArrow = (ImageView) view.findViewById(R.id.backArrow);
        mCheckMark = (ImageView) view.findViewById(R.id.ivPostComment);
        mComment = (EditText) view.findViewById(R.id.comment);
        mListView = (ListView) view.findViewById(R.id.listView);
        mComments = new ArrayList<>();
        mContext = getActivity();

        try {
            mPost = getPostFromBundle();

        } catch (NullPointerException e) {
            Log.e(TAG, "onCreateView: NullPointerException: " + e.getMessage());
        }

        setFirebaseAuth();
        return view;
    }

    private void setupWidgets() {

        CommentListAdapter adapter = new CommentListAdapter(mContext,
                R.layout.layout_comment, mComments);
        mListView.setAdapter(adapter);

        mCheckMark.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!mComment.getText().toString().equals("")) {
                    Log.d(TAG, "onClick: attempting to submit new comment.");
                    addNewComment(mComment.getText().toString());

                    mComment.setText("");
                    closeKeyboard();
                } else {
                    Toast.makeText(getActivity(), "Вы не можете оставить пустой комментарий", Toast.LENGTH_SHORT).show();
                }
            }
        });

        mBackArrow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "onClick: navigating back");
                if(getCallingActivivityFromBundle()!= null) {
                    if (getCallingActivivityFromBundle().equals(getString(R.string.feed_activity))) {
                        getActivity().getSupportFragmentManager().popBackStack();
                        ((FeedActivity) getActivity()).showLayout();
                    } else {
                        getActivity().getSupportFragmentManager().popBackStack();
                    }
                } else {
                    ((SearchArticlesActivity) getActivity()).getSupportFragmentManager().popBackStack();
                }
            }
        });
    }

    private void closeKeyboard() {
        View view = getActivity().getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }

    }


    private void addNewComment(String newComment) {
        Log.d(TAG, "addNewComment: adding new comment: " + newComment);

        String commentID = myRef.push().getKey();

        Comment comment = new Comment();
        comment.setComment(newComment);
        comment.setDate_created(getTimestamp());
        comment.setUser_id(FirebaseAuth.getInstance().getCurrentUser().getUid());

        //insert into photos node
        myRef.child(mContext.getString(R.string.dbname_posts))
                .child(mPost.getPost_id())
                .child(getString(R.string.field_comments))
                .child(commentID)
                .setValue(comment);


        //insert into user_photos node
        myRef.child(mContext.getString(R.string.dbname_user_posts))
                .child(mPost.getUser_id())
                .child(mPost.getPost_id())
                .child(getString(R.string.field_comments))
                .child(commentID)
                .setValue(comment);
    }

    private String getTimestamp() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.CANADA);
        sdf.setTimeZone(TimeZone.getTimeZone("Canada/Pacific"));
        return sdf.format(new Date());
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
            return bundle.getParcelable(getString(R.string.post));
        } else {
            return null;
        }
    }

    /**
     * retrieve the photo from incoming bundle from profileActivivty interface
     *
     * @return
     */
    private String getCallingActivivityFromBundle() {
        Log.d(TAG, "getPhotoFromBundle: arguments: " + getArguments());

        Bundle bundle = this.getArguments();

        if (bundle != null) {
            return bundle.getString(getString(R.string.feed_activity));
        } else {
            return null;
        }
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


        if (mPost.getComments().size() == 0) {
            mComments.clear();

            Comment firstComment = new Comment();
            //TODO: caption to array list
            //firstComment.setComment(mPost.getCaption());
            firstComment.setUser_id(mPost.getUser_id());
            firstComment.setDate_created(mPost.getData_created());
            mComments.add(firstComment);
            mPost.setComments(mComments);

            setupWidgets();

        }



        if(getActivity() == null) {
            mContext = (SearchArticlesActivity.getContext());
        }

        myRef.child(mContext.getString(R.string.dbname_posts))
                .child(mPost.getPost_id())
                .child(mContext.getString(R.string.field_comments))
                .addChildEventListener(new ChildEventListener() {
                    @Override
                    public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
                        Query query = myRef
                                .child(mContext.getString(R.string.dbname_posts))
                                .orderByChild(mContext.getString(R.string.field_post_id))
                                .equalTo(mPost.getPost_id());
                        query.addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                for (DataSnapshot singleSnaphot : dataSnapshot.getChildren()) {

                                    Post post = new Post();
                                    Map<String, Object> objectMap = (HashMap<String, Object>) singleSnaphot.getValue();

                                    //TODO: caption to array list
                                    //post.setCaption(objectMap.get(mContext.getString(R.string.field_caption)).toString());
                                    post.setTags(objectMap.get(mContext.getString(R.string.filed_tags)).toString());
                                    post.setPost_id(objectMap.get(mContext.getString(R.string.field_post_id)).toString());
                                    post.setUser_id(objectMap.get(mContext.getString(R.string.field_user_id)).toString());
                                    post.setData_created(objectMap.get(mContext.getString(R.string.field_data_created)).toString());
                                    post.setCountry(objectMap.get(mContext.getString(R.string.field_country)).toString());


                                    ArrayList<String> imageUrls = new ArrayList<String>();
                                    for (DataSnapshot dSnapshot : dataSnapshot
                                            .child(mContext.getString(R.string.filed_image_path)).getChildren()) {
                                        imageUrls.add(dSnapshot.getValue(String.class));
                                        Log.d(TAG, "onDataChange: found img_path: " + imageUrls.get(imageUrls.size() - 1));
                                    }

                                    post.setImage_path(imageUrls);

                                    ArrayList<String> photoIds = new ArrayList<String>();
                                    for (DataSnapshot dSnapshot : dataSnapshot
                                            .child(mContext.getString(R.string.field_photo_id)).getChildren()) {
                                        photoIds.add((String) dataSnapshot.getValue());
                                        Log.d(TAG, "onDataChange: found photo_id: " + photoIds.get(imageUrls.size() - 1));
                                    }

                                    post.setPhoto_ids(photoIds);


                                    mComments.clear();

                                    Comment firstComment = new Comment();
                                    //TODO: caption to array list
                                    //firstComment.setComment(mPost.getCaption());
                                    firstComment.setUser_id(mPost.getUser_id());
                                    firstComment.setDate_created(mPost.getData_created());

                                    mComments.add(firstComment);

                                    for (DataSnapshot dSnapshot : singleSnaphot
                                            .child(mContext.getString(R.string.field_comments)).getChildren()) {
                                        Comment comment = new Comment();
                                        comment.setUser_id(dSnapshot.getValue(Comment.class).getUser_id());
                                        comment.setComment(dSnapshot.getValue(Comment.class).getComment());
                                        comment.setDate_created(dSnapshot.getValue(Comment.class).getDate_created());
                                        mComments.add(comment);
                                    }

                                    post.setComments(mComments);

                                    mPost = post;

                                    setupWidgets();

//                    List<Like> likesList = new ArrayList<Like>();
//                    for(DataSnapshot dSnapshot: singleSnaphot
//                            .child(getString(R.string.field_likes)).getChildren()) {
//                        Like like = new Like();
//                        like.setUser_id(dSnapshot.getValue(Like.class).getUser_id());
//                        likesList.add(like);
//                    }
//
                                }


                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError databaseError) {
                                Log.d(TAG, "onCancelled: query cancelled.");
                            }
                        });
                    }

                    @Override
                    public void onChildChanged(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

                    }

                    @Override
                    public void onChildRemoved(@NonNull DataSnapshot dataSnapshot) {

                    }

                    @Override
                    public void onChildMoved(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

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

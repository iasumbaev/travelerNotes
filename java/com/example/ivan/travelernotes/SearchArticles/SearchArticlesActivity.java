package com.example.ivan.travelernotes.SearchArticles;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import com.example.ivan.travelernotes.Profile.ProfileActivity;
import com.example.ivan.travelernotes.R;
import com.example.ivan.travelernotes.Utils.BottomNavigationViewHelper;
import com.example.ivan.travelernotes.Utils.PostListAdapter;
import com.example.ivan.travelernotes.Utils.UserListAdapter;
import com.example.ivan.travelernotes.Utils.ViewCommentsFragment;
import com.example.ivan.travelernotes.Utils.ViewPostFragment;
import com.example.ivan.travelernotes.models.Comment;
import com.example.ivan.travelernotes.models.Post;
import com.example.ivan.travelernotes.models.User;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.ittianyu.bottomnavigationviewex.BottomNavigationViewEx;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

//TODO: elastic search для поиска

public class SearchArticlesActivity extends AppCompatActivity implements
        ViewPostFragment.OnCommentThreadSelectedListener {
    private static final String TAG = "SearchArticlesActivity";

    private static final int ACTIVITY_NUM = 3;
    private static Context myContext;

    public Context mContext = SearchArticlesActivity.this;


    public static Context getContext() {
        return myContext;
    }

    @Override
    public void onCommentThreadSelectedListener(Post post) {
        Log.d(TAG, "OnCommentThreadSelectedListener: selected a comment thread");

        /*TextView title = findViewById(R.id.search_tv);
        title.setVisibility(View.GONE);
        ListView lv = findViewById(R.id.listView);
        lv.setVisibility(View.GONE);*/

        ViewCommentsFragment fragment = new ViewCommentsFragment();
        Bundle args = new Bundle();
        args.putParcelable(getString(R.string.post), post);
        fragment.setArguments(args);

        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.container, fragment);
        transaction.addToBackStack(getString(R.string.view_comments_fragment));
        transaction.commit();
    }

    public void onPostThreadSelectedListener(Post post) {
        Log.d(TAG, "onPostThreadSelectedListener: selected a comment thread");
        hideSoftKeyboard();
        ViewPostFragment fragment = new ViewPostFragment();
        Bundle args = new Bundle();
        args.putParcelable(getString(R.string.post), post);
        fragment.setArguments(args);

        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.container, fragment);
        transaction.addToBackStack(getString(R.string.view_post_fragment));
        transaction.commit();
    }

    //widgets
    private EditText mSearchParam;
    private ListView mListView;

    //vars
    private List<Post> mPostList;
    private PostListAdapter mAdapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        myContext = getContext();
        setContentView(R.layout.activity_search_articles);
        mSearchParam = (EditText) findViewById(R.id.search);
        mSearchParam.setVisibility(View.GONE);
        mListView = (ListView) findViewById(R.id.listView);
        Log.d(TAG, "onCreate: started");
        TextView title = findViewById(R.id.search_tv);
        title.setVisibility(View.VISIBLE);
        ListView lv = findViewById(R.id.listView);
        lv.setVisibility(View.VISIBLE);

        hideSoftKeyboard();
        setupBottomNavigationView();
        //  initTextListener();
        mPostList = new ArrayList<>();
        searchForMatch();

    }

    private void initTextListener() {
        Log.d(TAG, "initTextListener: initializing");

        mPostList = new ArrayList<>();

        mSearchParam.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                String text = mSearchParam.getText().toString();
                Log.d(TAG, "afterTextChanged: " + text);
                //searchForMatch(text);
            }
        });
    }

    private void searchForMatch() {
        //Log.d(TAG, "searchForMatch: searching for a match: " + keyword);
        mPostList.clear();
        //update the users list view
        /*if (keyword.length() == 0) {

        } else {*/
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference();
        Query query = reference.child(getString(R.string.dbname_posts))
                .orderByChild(getString(R.string.field_country));
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot singleSnapshot : dataSnapshot.getChildren()) {
                    Post newPost = new Post();
                    Map<String, Object> objectMap = (HashMap<String, Object>) singleSnapshot.getValue();

                    newPost.setTags(objectMap.get(getString(R.string.filed_tags)).toString());
                    newPost.setUser_id(objectMap.get(getString(R.string.field_user_id)).toString());
                    newPost.setData_created(objectMap.get(getString(R.string.field_data_created)).toString());
                    newPost.setPost_id(objectMap.get(getString(R.string.field_post_id)).toString());
                    newPost.setCountry(objectMap.get(getString(R.string.field_country)).toString());

                    ArrayList<String> imageUrls = new ArrayList<String>();
                    for (DataSnapshot dSnapshot : singleSnapshot
                            .child(getString(R.string.filed_image_path)).getChildren()) {
                        imageUrls.add(dSnapshot.getValue(String.class));
                        Log.d(TAG, "onDataChange: found img_path: " + imageUrls.get(imageUrls.size() - 1));
                    }

                    newPost.setImage_path(imageUrls);

                    ArrayList<String> captions = new ArrayList<String>();
                    for (DataSnapshot dSnapshot : singleSnapshot
                            .child(getString(R.string.field_caption)).getChildren()) {
                        captions.add(dSnapshot.getValue(String.class));
                        Log.d(TAG, "onDataChange: found caption: " + captions.get(captions.size() - 1));
                    }

                    newPost.setCaption(captions);

                    ArrayList<String> photoIds = new ArrayList<String>();
                    for (DataSnapshot dSnapshot : singleSnapshot
                            .child(getString(R.string.field_photo_id)).getChildren()) {
                        photoIds.add((String) dataSnapshot.getValue());
                        Log.d(TAG, "onDataChange: found photo_id: " + photoIds.get(imageUrls.size() - 1));
                    }

                    newPost.setPhoto_ids(photoIds);

                    List<Comment> commentsList = new ArrayList<Comment>();
                    for (DataSnapshot dSnapshot : singleSnapshot
                            .child(getString(R.string.field_comments)).getChildren()) {
                        Comment comment = new Comment();
                        comment.setUser_id(dSnapshot.getValue(Comment.class).getUser_id());
                        comment.setComment(dSnapshot.getValue(Comment.class).getComment());
                        comment.setDate_created(dSnapshot.getValue(Comment.class).getDate_created());
                        commentsList.add(comment);
                    }
                    newPost.setComments(commentsList);


                    mPostList.add(newPost);
                    //update the users list view
                    updatePostsList();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
        //}
    }

    private void updatePostsList() {
        Log.d(TAG, "updateUsersList: updating users list");

        mAdapter = new PostListAdapter(SearchArticlesActivity.this, R.layout.layout_post_list_item, mPostList);
        mListView.setAdapter(mAdapter);


        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                Log.d(TAG, "onClick: navigating to post");
                onPostThreadSelectedListener(mPostList.get(i));
            }
        });
    }


    private void hideSoftKeyboard() {
        if (getCurrentFocus() != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
        }
    }

    /**
     * BottomNavigationView setup
     */
    private void setupBottomNavigationView() {
        Log.d(TAG, "setupBottomNavigationView: setting up BottomNavigationView");
        BottomNavigationViewEx bottomNavigationViewEx = (BottomNavigationViewEx) findViewById(R.id.bottomNavViewBar);
        BottomNavigationViewHelper.setupBottomNavigationView(bottomNavigationViewEx);
        BottomNavigationViewHelper.enableNavigation(mContext, this, bottomNavigationViewEx);
        Menu menu = bottomNavigationViewEx.getMenu();
        MenuItem menuItem = menu.getItem(ACTIVITY_NUM);
        menuItem.setChecked(true);
    }

}

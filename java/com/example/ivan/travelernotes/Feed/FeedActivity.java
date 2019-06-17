package com.example.ivan.travelernotes.Feed;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.design.widget.TabLayout;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;

import com.example.ivan.travelernotes.Login.LoginActivity;
import com.example.ivan.travelernotes.R;
import com.example.ivan.travelernotes.Utils.BottomNavigationViewHelper;
import com.example.ivan.travelernotes.Utils.FeedListAdapter;
import com.example.ivan.travelernotes.Utils.SectionsPagerAdapter;
import com.example.ivan.travelernotes.Utils.UniversalImageLoader;
import com.example.ivan.travelernotes.Utils.ViewCommentsFragment;
import com.example.ivan.travelernotes.models.Photo;
import com.example.ivan.travelernotes.models.Post;
import com.example.ivan.travelernotes.models.UserAccountSettings;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.ittianyu.bottomnavigationviewex.BottomNavigationViewEx;
import com.nostra13.universalimageloader.core.ImageLoader;

public class FeedActivity extends AppCompatActivity implements
        FeedListAdapter.OnLoadMoreItemsListener {

    @Override
    public void onLoadMoreItems() {
        Log.d(TAG, "onLoadMoreItems: displaying more photos");
        FeedFragment fragment = (FeedFragment) getSupportFragmentManager()
                .findFragmentByTag("android:switcher" + R.id.viewpager_container + ":" + mViewPager.getCurrentItem());
        if (fragment != null) {
            fragment.displayMorePhotos();
        }
    }

    private static final String TAG = "FeedActivity";

    private static final int ACTIVITY_NUM = 0;
    private static final int FEED_FRAGMENT = 1;

    private Context mContext = FeedActivity.this;

    //firebase
    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener mAuthStateListner;

    //widgets
    private ViewPager mViewPager;
    private FrameLayout mFrameLayout;
    private RelativeLayout mRelativeLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_feed);
        Log.d(TAG, "onCreate: starting");

        mViewPager = (ViewPager) findViewById(R.id.viewpager_container);
        mFrameLayout = (FrameLayout) findViewById(R.id.container);
        mRelativeLayout = (RelativeLayout) findViewById(R.id.relLayoutParent);


        if(setFirebaseAuth()) {
            initImageLoader();
            setupBottomNavigationView();
            setupViewPager();
        }


    }

    public void onCommentThreadSelected(Post post, String callingActivivty) {
        Log.d(TAG, "onCommentThreadSelected: selected a comment thread");

        ViewCommentsFragment fragment = new ViewCommentsFragment();
        Bundle args = new Bundle();
        args.putParcelable(getString(R.string.post), post);
        args.putString(getString(R.string.feed_activity), getString(R.string.feed_activity));
        fragment.setArguments(args);

        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.container, fragment);
        transaction.addToBackStack(getString(R.string.view_comments_fragment));
        transaction.commit();
    }

    public void hideLayout() {
        Log.d(TAG, "hideLayout: hiding layout");
        mRelativeLayout.setVisibility(View.GONE);
        mFrameLayout.setVisibility(View.VISIBLE);
    }

    public void showLayout() {
        Log.d(TAG, "hideLayout: showing layout");
        mRelativeLayout.setVisibility(View.VISIBLE);
        mFrameLayout.setVisibility(View.GONE);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        if (mFrameLayout.getVisibility() == View.VISIBLE) {
            showLayout();
        }
    }

    private void initImageLoader() {
        UniversalImageLoader universalImageLoader = new UniversalImageLoader(mContext);
        ImageLoader.getInstance().init(universalImageLoader.getConfig());
    }

    /**
     * Responsible for adding 3 tabs: Camera, Home, Messages
     */
    private void setupViewPager() {
        SectionsPagerAdapter adapter = new SectionsPagerAdapter(getSupportFragmentManager());

//        adapter.addFragment(new CameraFragment()); //index 0

        adapter.addFragment(new FeedFragment()); //index 1

//        adapter.addFragment(new MessagesFragment()); //index 2

        mViewPager.setAdapter(adapter);

        TabLayout tabLayout = (TabLayout) findViewById(R.id.tabs);
        tabLayout.setupWithViewPager(mViewPager);

//        tabLayout.getTabAt(0).setIcon(R.drawable.ic_camera);
        tabLayout.getTabAt(0).setText("Лента");
//        tabLayout.getTabAt(2).setIcon(R.drawable.ic_arrow);
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


    /*----------------------------------- Firebase -------------------------------*/

    /**
     * checks to see if the @param 'user' is logged in
     *
     * @param user
     */
    private void checkCurrentUser(FirebaseUser user) {
        Log.d(TAG, "checkCurrentUser: checking if user is logged in.");
        if (user == null) {
            Intent intent = new Intent(mContext, LoginActivity.class);
            startActivity(intent);
        }
    }

    private boolean setFirebaseAuth() {
        Log.d(TAG, "setFirebaseAuth: setting up firebase auth");
        mAuth = FirebaseAuth.getInstance();

        mAuthStateListner = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser currentUser = mAuth.getCurrentUser();

                //check if user is logged in
                checkCurrentUser(currentUser);

                if (currentUser != null) {
                    Log.d(TAG, "onStart: signed_in: " + currentUser.getUid());
                } else {

                    Log.d(TAG, "onStart: signed_out");
                }
            }
        };

        return mAuth.getCurrentUser() != null;
    }

    @Override
    public void onStart() {
        super.onStart();
        mAuth.addAuthStateListener(mAuthStateListner);
        mViewPager.setCurrentItem(FEED_FRAGMENT);
        checkCurrentUser(mAuth.getCurrentUser());
    }

    @Override
    public void onStop() {
        super.onStop();
        if (mAuthStateListner != null) {
            mAuth.removeAuthStateListener(mAuthStateListner);
        }
    }



}

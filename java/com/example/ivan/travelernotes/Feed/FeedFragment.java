package com.example.ivan.travelernotes.Feed;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import com.example.ivan.travelernotes.R;
import com.example.ivan.travelernotes.Utils.FeedListAdapter;
import com.example.ivan.travelernotes.models.Comment;
import com.example.ivan.travelernotes.models.Like;
import com.example.ivan.travelernotes.models.Photo;
import com.example.ivan.travelernotes.models.Post;
import com.example.ivan.travelernotes.models.UserAccountSettings;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FeedFragment extends Fragment {

    private static final String TAG = "FeedFragment";

    //vars
    private ArrayList<Post> mPosts;
    private ArrayList<Post> mPaginatedPosts;
    private ArrayList<String> mFollowing;
    private ListView mListView;
    private FeedListAdapter mAdapter;
    private int mResults;


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_feed, container, false);
        mListView = (ListView) view.findViewById(R.id.listView);
        mFollowing = new ArrayList<>();
        mPosts = new ArrayList<>();

        getFollowing();

        return view;
    }

    private void getFollowing() {
        Log.d(TAG, "getFollowing: searching for following");

        DatabaseReference reference = FirebaseDatabase.getInstance().getReference();
        Query query = reference
                .child(getString(R.string.dbname_following))
                .child(FirebaseAuth.getInstance().getCurrentUser().getUid());
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot singleSnapshot : dataSnapshot.getChildren()) {
                    Log.d(TAG, "onDataChange: found user: "
                            + singleSnapshot.child(getString(R.string.field_user_id)).getValue());

                    mFollowing.add(singleSnapshot.child(getString(R.string.field_user_id)).getValue().toString());
                }

                mFollowing.add(FirebaseAuth.getInstance().getCurrentUser().getUid());

                //get the photos
                getPosts();

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void getPosts() {
        Log.d(TAG, "getPhotos: getting photos");
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference();
        for (int i = 0; i < mFollowing.size(); i++) {
            final int count = i;
            Query query = reference
                    .child(getString(R.string.dbname_user_posts))
                    .child(mFollowing.get(i))
                    .orderByChild(getString(R.string.field_user_id))
                    .equalTo(mFollowing.get(i));
            query.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    for (DataSnapshot singleSnapshot : dataSnapshot.getChildren()) {
                        Post post = new Post();
                        Map<String, Object> objectMap = (HashMap<String, Object>) singleSnapshot.getValue();

                        post.setTags(objectMap.get(getString(R.string.filed_tags)).toString());
                        post.setUser_id(objectMap.get(getString(R.string.field_user_id)).toString());
                        post.setData_created(objectMap.get(getString(R.string.field_data_created)).toString());
                        post.setPost_id(objectMap.get(getString(R.string.field_post_id)).toString());
                        post.setCountry(objectMap.get(getString(R.string.field_country)).toString());

                        ArrayList<String> imageUrls = new ArrayList<String>();
                        for (DataSnapshot dSnapshot : singleSnapshot
                                .child(getString(R.string.filed_image_path)).getChildren()) {
                            imageUrls.add(dSnapshot.getValue(String.class));
                            Log.d(TAG, "onDataChange: found img_path: " + imageUrls.get(imageUrls.size() - 1));
                        }

                        post.setImage_path(imageUrls);

                        ArrayList<String> captions = new ArrayList<String>();
                        for (DataSnapshot dSnapshot : singleSnapshot
                                .child(getString(R.string.field_caption)).getChildren()) {
                            captions.add(dSnapshot.getValue(String.class));
                            Log.d(TAG, "onDataChange: found caption: " + captions.get(captions.size() - 1));
                        }

                        post.setCaption(captions);
                        ArrayList<String> photoIds = new ArrayList<String>();
                        for (DataSnapshot dSnapshot : singleSnapshot
                                .child(getString(R.string.field_photo_id)).getChildren()) {
                            photoIds.add((String) dataSnapshot.getValue());
                            Log.d(TAG, "onDataChange: found photo_id: " + photoIds.get(imageUrls.size() - 1));
                        }

                        post.setPhoto_ids(photoIds);

                        List<Comment> commentsList = new ArrayList<Comment>();
                        for (DataSnapshot dSnapshot : singleSnapshot
                                .child(getString(R.string.field_comments)).getChildren()){
                            Comment comment = new Comment();
                            comment.setUser_id(dSnapshot.getValue(Comment.class).getUser_id());
                            comment.setComment(dSnapshot.getValue(Comment.class).getComment());
                            comment.setDate_created(dSnapshot.getValue(Comment.class).getDate_created());
                            commentsList.add(comment);
                        }
                        post.setComments(commentsList);

                        mPosts.add(post);
                    }

                    if (count >= mFollowing.size() - 1) {
                        //display our photos
                        displaysPhotos();
                    }

                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                }
            });

        }

    }

    private void displaysPhotos() {
        mPaginatedPosts = new ArrayList<>();
        if (mPosts != null) {
            try {

                Collections.sort(mPosts, new Comparator<Post>() {
                    @Override
                    public int compare(Post photo, Post t1) {
                        return t1.getData_created().compareTo(photo.getData_created());
                    }
                });

                int iterations = mPosts.size();

                if (iterations > 10) {
                    iterations = 10;
                }

                mResults = 10;

                for (int i = 0; i < iterations; i++) {
                    mPaginatedPosts.add(mPosts.get(i));
                }

                mAdapter = new FeedListAdapter(getActivity(), R.layout.layout_feed_list_item, mPaginatedPosts);
                mListView.setAdapter(mAdapter);

            } catch (NullPointerException e) {
                Log.e(TAG, "displaysPhotos: NullPointerException" + e.getMessage());
            } catch (IndexOutOfBoundsException e) {
                Log.e(TAG, "displaysPhotos: IndexOutOfBoundsException" + e.getMessage());
            }
        }
    }

    public void displayMorePhotos() {
        Log.d(TAG, "displayMorePhotos: displaying more photos");

        try {
            if(mPosts.size() > mResults && mPosts.size() > 0) {
                int iterations;

                if(mPosts.size() > (mResults + 10)) {
                    Log.d(TAG, "displayMorePhotos: there are greater than 10 more photos");
                    iterations = 10;
                } else {
                    Log.d(TAG, "displayMorePhotos: there is less than 10 more photos");
                    iterations = mPosts.size() - 1;
                }

                //add the new photos to the paginated results
                for (int i = mResults; i < mResults + iterations; i++) {
                    mPaginatedPosts.add(mPosts.get(i));
                }
                mResults += iterations;
                mAdapter.notifyDataSetChanged();
            }

        } catch (NullPointerException e) {
            Log.e(TAG, "displaysPhotos: NullPointerException" + e.getMessage());
        } catch (IndexOutOfBoundsException e) {
            Log.e(TAG, "displaysPhotos: IndexOutOfBoundsException" + e.getMessage());
        }
    }

}

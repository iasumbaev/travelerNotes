package com.example.ivan.travelernotes.Utils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.util.TypedValue;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import com.example.ivan.travelernotes.Feed.FeedActivity;
import com.example.ivan.travelernotes.Profile.ProfileActivity;
import com.example.ivan.travelernotes.R;
import com.example.ivan.travelernotes.models.Comment;
import com.example.ivan.travelernotes.models.Like;
import com.example.ivan.travelernotes.models.Photo;
import com.example.ivan.travelernotes.models.Post;
import com.example.ivan.travelernotes.models.User;
import com.example.ivan.travelernotes.models.UserAccountSettings;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.nostra13.universalimageloader.core.ImageLoader;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import de.hdodenhof.circleimageview.CircleImageView;

public class FeedListAdapter extends ArrayAdapter<Post> {

    public interface OnLoadMoreItemsListener {
        void onLoadMoreItems();
    }

    OnLoadMoreItemsListener mOnLoadMoreItemsListener;

    private static final String TAG = "FeedListAdapter";

    private LayoutInflater mInflater;
    private int mLayoutResource;
    private Context mContext;
    private DatabaseReference mReference;
    private String currentUsername;

    public FeedListAdapter(@NonNull Context context, int resource, @NonNull List<Post> objects) {
        super(context, resource, objects);
        mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mLayoutResource = resource;
        this.mContext = context;
        mReference = FirebaseDatabase.getInstance().getReference();
    }

    static class ViewHolder {
        CircleImageView mProfileImage;
        String likesString;
        TextView username, timeDelta, likes, comments, title;
        SquareImageView image;
        ImageView heartRed, heartWhite, comment;

        UserAccountSettings settings = new UserAccountSettings();
        User user = new User();
        StringBuilder users;
        String mLikesString;
        boolean likedByCurrentUser;
        Heart heart;
        GestureDetector detector;
        Post post;
        TableLayout tlImages;
        LinearLayout ll;
    }

    @SuppressLint("ResourceAsColor")
    @NonNull
    @Override
    public View getView(final int position, @Nullable View convertView, @NonNull ViewGroup parent) {

        final ViewHolder holder;

        // if (convertView == null) {
        convertView = mInflater.inflate(mLayoutResource, parent, false);
        holder = new ViewHolder();

        holder.username = (TextView) convertView.findViewById(R.id.username);
        holder.heartRed = (ImageView) convertView.findViewById(R.id.image_heart_red);
        holder.heartWhite = (ImageView) convertView.findViewById(R.id.image_heart);
        holder.comment = (ImageView) convertView.findViewById(R.id.speech_bubble);
        holder.likes = (TextView) convertView.findViewById(R.id.image_likes);
        holder.comments = (TextView) convertView.findViewById(R.id.image_comments_link);
        holder.timeDelta = (TextView) convertView.findViewById(R.id.image_time_posted);
        holder.mProfileImage = (CircleImageView) convertView.findViewById(R.id.profile_photo);
        holder.heart = new Heart(holder.heartWhite, holder.heartRed);
        holder.post = getItem(position);
        holder.detector = new GestureDetector(mContext, new GestureListener(holder));
        holder.users = new StringBuilder();
        holder.ll = (LinearLayout) convertView.findViewById(R.id.postWrapper);
        holder.title = (TextView) convertView.findViewById(R.id.title);

        convertView.setTag(holder);
        /*} else {
            Log.d(TAG, "getView: wtf???");
            holder = (ViewHolder) convertView.getTag();
        }*/

        //get the current users username (need for checking likes string)
        getCurrentUsername();

        Log.d(TAG, "getView: " + position + ": " + holder.post.getCountry());

        //set the caption
        //TODO: caption to array list
        // holder.caption.setText(getItem(position).getCaption());

        //get likes string
        getLikesString(holder);


        holder.title.setText(getItem(position).getCountry());

        //set the comment
        List<Comment> comments = getItem(position).getComments();
        holder.comments.setText("Показать все " + comments.size() + " комментариев");
        holder.comments.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "onClick: loading comment thread for " + getItem(position).getPost_id());
                ((FeedActivity) mContext).onCommentThreadSelected(getItem(position),
                        mContext.getString(R.string.feed_activity));

                ((FeedActivity) mContext).hideLayout();
            }
        });

        //set time it was posted
        String timeStampDifference = getTimestampDifference(getItem(position));
        if (!timeStampDifference.equals("0")) {
            holder.timeDelta.setText(timeStampDifference + " ДНЕЙ НАЗАД");
        } else {
            holder.timeDelta.setText("СЕГОДНЯ");
        }

        //set the profile image
        final ImageLoader imageLoader = ImageLoader.getInstance();
        int imgCount = getItem(position).getImage_path().size();
/*
        for (int i = 0; i < imgCount; i++) {

            TableRow tr = new TableRow(mContext);
            tr.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, TableRow.LayoutParams.WRAP_CONTENT));
            SquareImageView view = new SquareImageView(mContext);
            view.setScaleType(ImageView.ScaleType.CENTER_CROP);
               */
/* ViewGroup.LayoutParams lp = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
                view.setLayoutParams(lp);*//*

            tr.addView(view);
            UniversalImageLoader.setImage(getItem(position).getImage_path().get(i), view, null, "");
            holder.tlImages.addView(tr);
        }
*/
        Log.d(TAG, "getView: imgCount: " + imgCount);
        for (int i = 0; i < imgCount; i++) {
            SquareImageView image = new SquareImageView(getContext());
            image.setScaleType(ImageView.ScaleType.CENTER_CROP);
            UniversalImageLoader.setImage(getItem(position).getImage_path().get(i), image, null, "");
            holder.ll.addView(image);
            TextView caption = new TextView(getContext());
            caption.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
            caption.setPadding(0,10,0,10);
            caption.setTextColor(Color.parseColor("#000000"));

            caption.setText(getItem(position).getCaption().get(i));
            holder.ll.addView(caption);
            Log.d(TAG, "getView: setting caption: " + getItem(position).getCaption().get(i));
        }

        //get the profile image and username
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference();
        Query query = reference
                .child(mContext.getString(R.string.dbname_users_account_settings))
                .orderByChild(mContext.getString(R.string.field_user_id))
                .equalTo(getItem(position).getUser_id());
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot singleSnapshot : dataSnapshot.getChildren()) {
                    //currentUsername = singleSnapshot.getValue(UserAccountSettings.class).getUsername();
                    Log.d(TAG, "onDataChange: found user: " +
                            singleSnapshot.getValue(UserAccountSettings.class).getUsername());

                    holder.username.setText(singleSnapshot.getValue(UserAccountSettings.class).getUsername());
                    holder.username.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            Log.d(TAG, "onClick: navigating to profile of: " +
                                    holder.user.getUsername());

                            Intent intent = new Intent(mContext, ProfileActivity.class);
                            intent.putExtra(mContext.getString(R.string.calling_activity),
                                    mContext.getString(R.string.feed_activity));
                            intent.putExtra(mContext.getString(R.string.intent_user), holder.user);
                            mContext.startActivity(intent);
                        }
                    });

                    imageLoader.displayImage(singleSnapshot.getValue(UserAccountSettings.class).getProfile_photo(), holder.mProfileImage);
                    holder.mProfileImage.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            Log.d(TAG, "onClick: navigating to profile of: " +
                                    holder.user.getUsername());

                            Intent intent = new Intent(mContext, ProfileActivity.class);
                            intent.putExtra(mContext.getString(R.string.calling_activity),
                                    mContext.getString(R.string.feed_activity));
                            intent.putExtra(mContext.getString(R.string.intent_user), holder.user);
                            mContext.startActivity(intent);
                        }
                    });

                    holder.settings = singleSnapshot.getValue(UserAccountSettings.class);
                    holder.comment.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            ((FeedActivity) mContext).onCommentThreadSelected(getItem(position),
                                    mContext.getString(R.string.feed_activity));

                            ((FeedActivity) mContext).hideLayout();
                        }
                    });
                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });


        //get the user object
        Query userQuery = mReference
                .child(mContext.getString(R.string.dbname_users))
                .orderByChild(mContext.getString(R.string.field_user_id))
                .equalTo(getItem(position).getUser_id());
        userQuery.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot singleSnapshot : dataSnapshot.getChildren()) {
                    Log.d(TAG, "onDataChange: found user: " +
                            singleSnapshot.getValue(User.class).getUsername());

                    holder.user = singleSnapshot.getValue(User.class);
                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        if (reachedEndOfList(position)) {
            loadMoreData();
        }

        return convertView;
    }

    private boolean reachedEndOfList(int position) {
        return position == getCount() - 1;
    }

    private void loadMoreData() {
        try {
            mOnLoadMoreItemsListener = (OnLoadMoreItemsListener) getContext();
        } catch (ClassCastException e) {
            Log.e(TAG, "loadMoreData: ClassCastException" + e.getMessage());
        }

        try {
            mOnLoadMoreItemsListener.onLoadMoreItems();
        } catch (NullPointerException e) {
            Log.e(TAG, "loadMoreData: NullPointerException" + e.getMessage());
        }
    }

    public class GestureListener extends GestureDetector.SimpleOnGestureListener {

        ViewHolder mHolder;

        public GestureListener(ViewHolder holder) {
            mHolder = holder;
        }

        @Override
        public boolean onDown(MotionEvent e) {
            return true;
        }

        @Override
        public boolean onDoubleTap(MotionEvent e) {

            DatabaseReference reference = FirebaseDatabase.getInstance().getReference();
            Query query = reference
                    .child(mContext.getString(R.string.dbname_posts))
                    .child(mHolder.post.getPost_id())
                    .child(mContext.getString(R.string.field_likes));

            query.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    for (DataSnapshot singleSnapshot : dataSnapshot.getChildren()) {

                        String keyID = singleSnapshot.getKey();

                        //case1: Then user already liked the photo
                        if (mHolder.likedByCurrentUser &&
                                singleSnapshot.getValue(Like.class).getUser_id()
                                        .equals(FirebaseAuth.getInstance().getCurrentUser().getUid())) {
                            mReference.child(mContext.getString(R.string.dbname_posts))
                                    .child(mHolder.post.getPost_id())
                                    .child(mContext.getString(R.string.field_likes))
                                    .child(keyID)
                                    .removeValue();

                            mReference.child(mContext.getString(R.string.dbname_user_posts))
                                    .child(FirebaseAuth.getInstance().getCurrentUser().getUid())
                                    .child(mHolder.post.getPost_id())
                                    .child(mContext.getString(R.string.field_likes))
                                    .child(keyID)
                                    .removeValue();

                            mHolder.heart.toggleLike();
                            getLikesString(mHolder);
                        }
                        //case2: The user has not liked photo
                        else if (!mHolder.likedByCurrentUser) {
                            //add new like
                            addNewLike(mHolder);
                            break;
                        }
                    }
                    if (!dataSnapshot.exists()) {
                        addNewLike(mHolder);
                        //add new like
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                }
            });

            return true;
        }

        private void addNewLike(final ViewHolder holder) {
            Log.d(TAG, "addNewLike: adding new like");

            String newLikeID = mReference.push().getKey();
            Like like = new Like();
            like.setUser_id(FirebaseAuth.getInstance().getCurrentUser().getUid());


            mReference.child(mContext.getString(R.string.dbname_posts))
                    .child(holder.post.getPost_id())
                    .child(mContext.getString(R.string.field_likes))
                    .child(newLikeID)
                    .setValue(like);

            mReference.child(mContext.getString(R.string.dbname_user_posts))
                    .child(holder.post.getUser_id())
                    .child(holder.post.getPost_id())
                    .child(mContext.getString(R.string.field_likes))
                    .child(newLikeID)
                    .setValue(like);

            holder.heart.toggleLike();
            getLikesString(holder);
        }

    }

    private void getCurrentUsername() {
        Log.d(TAG, "getCurrentUsername: retreiving user account setting");
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference();
        Query query = reference
                .child(mContext.getString(R.string.dbname_users))
                .orderByChild(mContext.getString(R.string.field_user_id))
                .equalTo(FirebaseAuth.getInstance().getCurrentUser().getUid());
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot singleSnapshot : dataSnapshot.getChildren()) {
                    currentUsername = singleSnapshot.getValue(UserAccountSettings.class).getUsername();
                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void getLikesString(final ViewHolder holder) {
        Log.d(TAG, "getLikesString: getting likes string");
        try {
            DatabaseReference reference = FirebaseDatabase.getInstance().getReference();
            Query query = reference
                    .child(mContext.getString(R.string.dbname_posts))
                    .child(holder.post.getPost_id())
                    .child(mContext.getString(R.string.field_likes));

            query.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    holder.users = new StringBuilder();
                    for (DataSnapshot singleSnapshot : dataSnapshot.getChildren()) {
                        DatabaseReference reference = FirebaseDatabase.getInstance().getReference();
                        Query query = reference
                                .child(mContext.getString(R.string.dbname_users))
                                .orderByChild(mContext.getString(R.string.field_user_id))
                                .equalTo(singleSnapshot.getValue(Like.class).getUser_id());

                        query.addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                for (DataSnapshot singleSnapshot : dataSnapshot.getChildren()) {
                                    Log.d(TAG, "onDataChange: found like: " + singleSnapshot.getValue(User.class).getUsername());

                                    holder.users.append(singleSnapshot.getValue(User.class).getUsername());
                                    holder.users.append(",");
                                }

                                String[] splitUsers = holder.users.toString().split(",");
                                Log.d(TAG, "onDataChange: users string: " + splitUsers.toString());


                                if (holder.users.toString().contains(currentUsername + ",")) {
                                    holder.likedByCurrentUser = true;
                                } else {
                                    holder.likedByCurrentUser = false;
                                }

                                int length = splitUsers.length;

                                if (length == 0) {
                                    Log.d(TAG, "onDataChange: !!!!!");
                                    holder.likesString = "";
                                }

                                if (length == 1) {
                                    holder.likesString = "Понравилось " + splitUsers[0];
                                } else if (length == 2) {
                                    holder.likesString = "Понравилось " + splitUsers[0]
                                            + " и " + splitUsers[1];
                                } else if (length == 3) {
                                    holder.likesString = "Понравилось " + splitUsers[0]
                                            + ", " + splitUsers[1]
                                            + " и " + splitUsers[2];
                                } else if (length == 4) {
                                    holder.likesString = "Понравилось " + splitUsers[0]
                                            + ", " + splitUsers[1]
                                            + ", " + splitUsers[2]
                                            + " и " + splitUsers[3];
                                } else if (length > 4) {
                                    holder.likesString = "Понравилось " + splitUsers[0]
                                            + ", " + splitUsers[1]
                                            + ", " + splitUsers[2]
                                            + " и ещё " + (splitUsers.length - 3) + " пользователям";
                                }
                                Log.d(TAG, "onDataChange: likes string: " + holder.likesString);
//                                Log.d(TAG, "onDataChange: photo caption: " + holder.caption.getText().toString());
                                //setup likes string
                                setupLikesString(holder, holder.likesString);
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError databaseError) {

                            }
                        });
                    }
                    if (!dataSnapshot.exists()) {
                        holder.likesString = "";
                        holder.likedByCurrentUser = false;
                        //setup likes string
                        setupLikesString(holder, holder.likesString);
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                }
            });
        } catch (NullPointerException e) {
            Log.e(TAG, "getLikesString: NullPointerException: " + e.getMessage());
            holder.likesString = "";
            holder.likedByCurrentUser = false;
            //setup likes string
            setupLikesString(holder, holder.likesString);
        }
    }

    private void setupLikesString(final ViewHolder holder, String likesString) {
        Log.d(TAG, "setupLikesString: likes string: " + holder.likesString);

        if (holder.likedByCurrentUser) {
            Log.d(TAG, "setupLikesString: photo is liked by current user");
            holder.heartWhite.setVisibility(View.GONE);
            holder.heartRed.setVisibility(View.VISIBLE);
            holder.heartRed.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View view, MotionEvent motionEvent) {
                    return holder.detector.onTouchEvent(motionEvent);
                }
            });
        } else {
            Log.d(TAG, "setupLikesString: photo is not liked by current user");
            holder.heartWhite.setVisibility(View.VISIBLE);
            holder.heartRed.setVisibility(View.GONE);
            holder.heartWhite.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View view, MotionEvent motionEvent) {
                    return holder.detector.onTouchEvent(motionEvent);
                }
            });
        }
        holder.likes.setText(likesString);
    }

    /**
     * Returns a string representing the number of days ago the post was made
     *
     * @return
     */
    private String getTimestampDifference(Post post) {
        String difference = "";
        Calendar c = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.CANADA);
        sdf.setTimeZone(TimeZone.getTimeZone("Canada/Pacific"));
        Date today = c.getTime();
        sdf.format(today);
        Date timestamp;
        final String photoTimestamp = post.getData_created();
        try {
            timestamp = sdf.parse(photoTimestamp);
            difference = String.valueOf(Math.round(((today.getTime() - timestamp.getTime()) / 1000 / 60 / 60 / 24)));
        } catch (ParseException e) {
            Log.e(TAG, "getTimestampDifference: ParseException" + e.getMessage());
            difference = "0";
        }

        return difference;
    }

}

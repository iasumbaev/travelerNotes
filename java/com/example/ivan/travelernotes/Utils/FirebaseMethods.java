package com.example.ivan.travelernotes.Utils;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.util.Log;
import android.widget.Toast;

import com.example.ivan.travelernotes.Feed.FeedActivity;
import com.example.ivan.travelernotes.Login.RegisterActivity;
import com.example.ivan.travelernotes.Profile.AccountSettingsActivity;
import com.example.ivan.travelernotes.R;
import com.example.ivan.travelernotes.models.Photo;
import com.example.ivan.travelernotes.models.Post;
import com.example.ivan.travelernotes.models.User;
import com.example.ivan.travelernotes.models.UserAccountSettings;
import com.example.ivan.travelernotes.models.UserSettings;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageMetadata;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class FirebaseMethods {
    private static final String TAG = "FirebaseMethods";

    //firebase
    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener mAuthStateListner;
    private FirebaseDatabase mFirebaseDatabase;
    private DatabaseReference myRef;
    private StorageReference mStorageReference;
    private String userID;

    //vars
    private Context mContext;
    private double mPhotoUploadProgress = 0;

    public FirebaseMethods(Context context) {
        mAuth = FirebaseAuth.getInstance();
        mFirebaseDatabase = FirebaseDatabase.getInstance();
        myRef = mFirebaseDatabase.getReference();
        mStorageReference = FirebaseStorage.getInstance().getReference();
        mContext = context;


        if (mAuth.getCurrentUser() != null) {
            userID = mAuth.getCurrentUser().getUid();
        }
    }

    public void uploadNewPost(final ArrayList<String> captions, final String title, int count, final ArrayList<String> imgUrl) {
        Log.d(TAG, "uploadNewPost: attempting to upload new post");

        final int[] photosUploaded = {0};

        FilePath filePath = new FilePath();
        String user_id = FirebaseAuth.getInstance().getCurrentUser().getUid();
        int countPhotos = 0;
        final Post post = new Post();
        final ArrayList<StorageReference> storageReference = new ArrayList<>();
        ArrayList<UploadTask> uploadTask = new ArrayList<>();
        //convert image url to bitmap
        ArrayList<Bitmap> bm = new ArrayList<>();
        for (int i = 0; i < imgUrl.size(); i++) {
            storageReference.add(mStorageReference
                    .child(filePath.FIREBASE_POSTS_STORAGE + "/" + user_id + "/posts" + (count + 1) + "/photos" + (countPhotos + 1)));
            countPhotos++;
            bm.add(ImageManager.getBitmap(imgUrl.get(i)));

            byte[] bytes = ImageManager.getBytesFromBitmap(bm.get(i), 80);

            uploadTask.add(storageReference.get(i).putBytes(bytes));


            final int finalI = i;
            Task<Uri> uriTask = uploadTask.get(i).continueWithTask(new Continuation<UploadTask.TaskSnapshot, Task<Uri>>() {
                @Override
                public Task<Uri> then(@NonNull Task<UploadTask.TaskSnapshot> task) throws Exception {
                    if (!task.isSuccessful()) {
                        throw task.getException();
                    }

                    return storageReference.get(finalI).getDownloadUrl();
                }
            }).addOnCompleteListener(new OnCompleteListener<Uri>() {
                @Override
                public void onComplete(@NonNull Task<Uri> task) {
                    if (task.isSuccessful()) {
                        Uri downloadUri = task.getResult();
                        String downloadURL = downloadUri.toString();

                        Toast.makeText(mContext, "Фото загружено успешно", Toast.LENGTH_SHORT).show();

                        //add new photo to 'photos' node and 'user_photos' node
                        addPhotoToPost(post, downloadURL);
                        photosUploaded[0]++;

                        Log.d(TAG, "onComplete: imgs: " + post.getImage_path().toString());

                        if (photosUploaded[0] == imgUrl.size()) {
                            addPostToDatabase(post, captions, title);
                            //navigate to the main feed so the user can see their photo
                            Intent intent = new Intent(mContext, FeedActivity.class);
                            mContext.startActivity(intent);
                        }


                    }
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Log.d(TAG, "onFailure: Photo upload failed");
                    Toast.makeText(mContext, "Ошибка загрузки фотографии", Toast.LENGTH_SHORT).show();
                    photosUploaded[0]++;
                }
            });
        }



    }

    public void addPhotoToPost(Post post, String url) {
        Log.d(TAG, "addPhotoToDatabase: adding photo to database");

        String newPostsKey = myRef.child(mContext.getString(R.string.dbname_posts)).push().getKey();

        post.addImage(url);
        post.addPhotoId(newPostsKey);

    }


    public void uploadNewPhoto(String photoType, final String caption, final int count, final String imgUrl,
                               Bitmap bm) {
        Log.d(TAG, "uploadNewPhoto: attempting to upload new photo");

        FilePath filePath = new FilePath();
        //case1) new photo
        if (photoType.equals(mContext.getString(R.string.new_photo))) {
            Log.d(TAG, "uploadNewPhoto: uploading NEW photo");

            String user_id = FirebaseAuth.getInstance().getCurrentUser().getUid();

            final StorageReference storageReference = mStorageReference
                    .child(filePath.FIREBASE_IMAGE_STORAGE + "/" + user_id + "/photo" + (count + 1));

            //conver image url to bitmap
            if (bm == null) {
                bm = ImageManager.getBitmap(imgUrl);
            }

            byte[] bytes = ImageManager.getBytesFromBitmap(bm, 100);

            UploadTask uploadTask = null;
            uploadTask = storageReference.putBytes(bytes);


            Task<Uri> uriTask = uploadTask.continueWithTask(new Continuation<UploadTask.TaskSnapshot, Task<Uri>>() {
                @Override
                public Task<Uri> then(@NonNull Task<UploadTask.TaskSnapshot> task) throws Exception {
                    if (!task.isSuccessful()) {
                        throw task.getException();
                    }

                    return storageReference.getDownloadUrl();
                }
            }).addOnCompleteListener(new OnCompleteListener<Uri>() {
                @Override
                public void onComplete(@NonNull Task<Uri> task) {
                    if (task.isSuccessful()) {
                        Uri downloadUri = task.getResult();
                        String downloadURL = downloadUri.toString();


                        Toast.makeText(mContext, "photo upload success", Toast.LENGTH_SHORT).show();

                        //add new photo to 'photos' node and 'user_photos' node
                        addPhotoToDatabase(caption, downloadURL);

                        //navigate to the main feed so the user can see their photo
                        Intent intent = new Intent(mContext, FeedActivity.class);
                        mContext.startActivity(intent);
                    }
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Log.d(TAG, "onFailure: Photo upload failed");
                    Toast.makeText(mContext, "Photo upload failed", Toast.LENGTH_SHORT).show();
                }
            });


        }
        //case new profile photo
        else if (photoType.equals(mContext.getString(R.string.profile_photo))) {
            Log.d(TAG, "uploadNewPhoto: uploading PROFILE photo");


            String user_id = FirebaseAuth.getInstance().getCurrentUser().getUid();

            final StorageReference storageReference = mStorageReference
                    .child(filePath.FIREBASE_IMAGE_STORAGE + "/" + user_id + "/profile_photo");

            //conver image url to bitmap
            if (bm == null) {
                bm = ImageManager.getBitmap(imgUrl);
            }
            byte[] bytes = ImageManager.getBytesFromBitmap(bm, 100);

            UploadTask uploadTask = null;
            uploadTask = storageReference.putBytes(bytes);

            Task<Uri> uriTask = uploadTask.continueWithTask(new Continuation<UploadTask.TaskSnapshot, Task<Uri>>() {
                @Override
                public Task<Uri> then(@NonNull Task<UploadTask.TaskSnapshot> task) throws Exception {
                    if (!task.isSuccessful()) {
                        throw task.getException();
                    }

                    return storageReference.getDownloadUrl();
                }
            }).addOnCompleteListener(new OnCompleteListener<Uri>() {
                @Override
                public void onComplete(@NonNull Task<Uri> task) {
                    if (task.isSuccessful()) {
                        Uri downloadUri = task.getResult();
                        String downloadURL = downloadUri.toString();

                        Toast.makeText(mContext, "photo upload success", Toast.LENGTH_SHORT).show();

                        //insert into 'user_account_settings' node
                        setProfilePhoto(downloadURL);

                        ((AccountSettingsActivity) mContext).setupViewPager(
                                ((AccountSettingsActivity) mContext).pagerAdapter
                                        .getFragmentNumber(mContext.getString(R.string.edit_profile_fragment))
                        );
                    }
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Log.d(TAG, "onFailure: Photo upload failed");
                    Toast.makeText(mContext, "Photo upload failed", Toast.LENGTH_SHORT).show();
                }
            });


        }

    }

    private void setProfilePhoto(String url) {
        Log.d(TAG, "setProfilePhoto: setting new profile image: " + url);

        myRef.child(mContext.getString(R.string.dbname_users_account_settings))
                .child(FirebaseAuth.getInstance().getCurrentUser().getUid())
                .child(mContext.getString(R.string.profile_photo))
                .setValue(url);
    }


    private String getTimestamp() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.CANADA);
        sdf.setTimeZone(TimeZone.getTimeZone("Canada/Pacific"));
        return sdf.format(new Date());
    }

    private void addPostToDatabase(Post post, ArrayList<String> captions, String title) {
        Log.d(TAG, "addPhotoToDatabase: addingPhotoToDatabase: adding photo to database");
        Log.d(TAG, "addPostToDatabase: imgs: " + post.getImage_path().toString());
        String tags = "";
        String newPostsKey = myRef.child(mContext.getString(R.string.dbname_posts)).push().getKey();

        post.setPost_id(newPostsKey);
        post.setCaption(captions);
        post.setData_created(getTimestamp());
        post.setTags(tags);
        post.setUser_id(FirebaseAuth.getInstance().getCurrentUser().getUid());
        post.setCountry(title);

        //insert into database
        myRef.child(mContext.getString(R.string.dbname_user_posts)).
                child(FirebaseAuth.getInstance().getCurrentUser().getUid())
                .child(newPostsKey).setValue(post);

        myRef.child(mContext.getString(R.string.dbname_posts)).child(newPostsKey).setValue(post);

    }

    private void addPhotoToDatabase(String caption, String url) {
        Log.d(TAG, "addPhotoToDatabase: addingPhotoToDatabase: adding photo to database");

        String tags = StringManipulation.getTags(caption);
        String newPhototKey = myRef.child(mContext.getString(R.string.dbname_photos)).push().getKey();
        Photo photo = new Photo();
        photo.setCaption(caption);
        photo.setData_created(getTimestamp());
        photo.setImage_path(url);
        photo.setTags(tags);
        photo.setUser_id(FirebaseAuth.getInstance().getCurrentUser().getUid());
        photo.setPhoto_id(newPhototKey);

        //insert into database
        myRef.child(mContext.getString(R.string.dbname_user_photos)).
                child(FirebaseAuth.getInstance().getCurrentUser()
                        .getUid()).child(newPhototKey).setValue(photo);
        myRef.child(mContext.getString(R.string.dbname_photos)).child(newPhototKey).setValue(photo);

    }

    public int getPostCount(DataSnapshot dataSnapshot) {
        int count = 0;
        for (DataSnapshot ds : dataSnapshot
                .child(mContext.getString(R.string.dbname_user_posts))
                .child(FirebaseAuth.getInstance().getCurrentUser().getUid())
                .getChildren()) {
            count++;
        }
        return count;
    }

    /**
     * Update user_account_settings' node for current user
     *
     * @param displayName
     * @param website
     * @param description
     * @param phoneNumber
     */
    public void updateUserAccountSettings(String displayName, String website, String description, long phoneNumber) {
        Log.d(TAG, "updateUserAccountSettings: updating user account settings");

        if (displayName != null) {
            myRef.child(mContext.getString(R.string.dbname_users_account_settings))
                    .child(userID)
                    .child(mContext.getString(R.string.field_display_name))
                    .setValue(displayName);
        }

        if (website != null) {
            myRef.child(mContext.getString(R.string.dbname_users_account_settings))
                    .child(userID)
                    .child(mContext.getString(R.string.field_website))
                    .setValue(website);
        }
        if (description != null) {
            myRef.child(mContext.getString(R.string.dbname_users_account_settings))
                    .child(userID)
                    .child(mContext.getString(R.string.field_description))
                    .setValue(description);
        }
        if (phoneNumber != 0) {
            myRef.child(mContext.getString(R.string.dbname_users))
                    .child(userID)
                    .child(mContext.getString(R.string.field_phone_number))
                    .setValue(phoneNumber);
        }
    }

    /**
     * Update the username in the 'users' node and 'user_account_settings' node
     *
     * @param username
     */
    public void updateUsername(String username) {
        Log.d(TAG, "updateUsername: updating username to: " + username);
        myRef.child(mContext.getString(R.string.dbname_users))
                .child(userID)
                .child(mContext.getString(R.string.field_username))
                .setValue(username);

        myRef.child(mContext.getString(R.string.dbname_users_account_settings))
                .child(userID)
                .child(mContext.getString(R.string.field_username))
                .setValue(username);
    }


    /**
     * Update the email in the 'users' node
     *
     * @param email
     */
    public void updateEmail(String email) {
        Log.d(TAG, "updateEmail: updating email to: " + email);
        myRef.child(mContext.getString(R.string.dbname_users))
                .child(userID)
                .child(mContext.getString(R.string.field_email))
                .setValue(email);

    }

   /* public boolean checkIfUsernameExists(String username, DataSnapshot dataSnapshot) {
        Log.d(TAG, "checkIfUsernameExists: checking if " + username + " already exists.");
        User user = new User();
        for (DataSnapshot ds : dataSnapshot.child(userID).getChildren()) {
            Log.d(TAG, "checkIfUsernameExists: datashapshot:" + ds);

            user.setUsername(ds.getValue(User.class).getUsername());
            Log.d(TAG, "checkIfUsernameExists: username: " + user.getUsername());

            if (StringManipulation.expandUsername(user.getUsername()).equals(username)) {
                Log.d(TAG, "checkIfUsernameExists: FOUND A MATCH: " + user.getUsername());
                return true;
            }
        }
        return false;
    }*/


    /**
     * Register a new email and password to Firebase Authentication
     *
     * @param email
     * @param password
     * @param username
     */
    public void registerNewEmail(final String email, String password, final String username) {
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information
                            Log.d(TAG, "createUserWithEmail:success");
                            //send verification email
                            sendVerificationEmail();

                            userID = mAuth.getCurrentUser().getUid();
                            Log.d(TAG, "onComplete: Authstate changed: " + userID);
                        } else {
                            // If sign in fails, display a message to the user.
                            Log.w(TAG, "createUserWithEmail:failure", task.getException());
                            Toast.makeText(mContext, "Authentication failed.",
                                    Toast.LENGTH_SHORT).show();
                            // updateUI(null);
                        }

                        // ...
                    }
                });
    }

    public void sendVerificationEmail() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user != null) {
            user.sendEmailVerification().addOnCompleteListener(new OnCompleteListener<Void>() {
                @Override
                public void onComplete(@NonNull Task<Void> task) {
                    if (task.isSuccessful()) {
                        Toast.makeText(mContext, "Пожалуйста, перейдите по ссылке в письме, отправленного вам на почту", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(mContext, "Ошибка подтверждения почты", Toast.LENGTH_SHORT).show();
                        ;
                    }
                }
            });
        }
    }


    /**
     * Add information to the users nodes
     * Add information to the user_account_settings node
     *
     * @param email
     * @param username
     * @param description
     * @param website
     * @param profile_photo
     */
    public void addNewUser(String email, String username, String description, String
            website, String profile_photo) {
        User user = new User(userID, 1, email, StringManipulation.condenseUsername(username));

        myRef.child(mContext.getString(R.string.dbname_users)).child(userID).setValue(user);

        UserAccountSettings settings = new UserAccountSettings(
                description,
                username,
                0,
                0,
                0,
                profile_photo,
                StringManipulation.condenseUsername(username),
                website,
                userID

        );
        myRef.child(mContext.getString(R.string.dbname_users_account_settings)).child(userID).setValue(settings);
    }

    /**
     * Retrieves the account settings for the user currently logged in
     * Database: user_account_settings node
     *
     * @param dataSnapshot
     * @return
     */
    public UserSettings getUserSettings(DataSnapshot dataSnapshot) {
        Log.d(TAG, "getUserAccountSettings: retrieving user account settings from firebase");

        UserAccountSettings settings = new UserAccountSettings();
        User user = new User();
        for (DataSnapshot ds : dataSnapshot.getChildren()) {

            //user_account_settings nide
            if (ds.getKey().equals(mContext.getString(R.string.dbname_users_account_settings))) {
                Log.d(TAG, "getUserAccountSettings: datasnapshot: " + ds);


                try {
                    settings.setDisplay_name(
                            ds.child(userID)
                                    .getValue(UserAccountSettings.class)
                                    .getDisplay_name()

                    );

                    settings.setUsername(
                            ds.child(userID)
                                    .getValue(UserAccountSettings.class)
                                    .getUsername()

                    );

                    settings.setWebsite(
                            ds.child(userID)
                                    .getValue(UserAccountSettings.class)
                                    .getWebsite()

                    );

                    settings.setDescription(
                            ds.child(userID)
                                    .getValue(UserAccountSettings.class)
                                    .getDescription()

                    );

                    settings.setProfile_photo(
                            ds.child(userID)
                                    .getValue(UserAccountSettings.class)
                                    .getProfile_photo()

                    );

                    settings.setPosts(
                            ds.child(userID)
                                    .getValue(UserAccountSettings.class)
                                    .getPosts()

                    );

                    settings.setFollowing(
                            ds.child(userID)
                                    .getValue(UserAccountSettings.class)
                                    .getFollowing()

                    );

                    settings.setFollowers(
                            ds.child(userID)
                                    .getValue(UserAccountSettings.class)
                                    .getFollowers()

                    );
                    Log.d(TAG, "getUserAccountSettings: retrieved user_account_settings information: " + settings.toString());
                } catch (NullPointerException e) {
                    Log.e(TAG, "getUserAccountSettings: NullPointerException: " + e.getMessage());
                }

            }
            // user node
            if (ds.getKey().equals(mContext.getString(R.string.dbname_users))) {
                Log.d(TAG, "getUserAccountSettings: datasnapshot: " + ds);

                user.setUsername(
                        ds.child(userID)
                                .getValue(User.class)
                                .getUsername()
                );

                user.setEmail(
                        ds.child(userID)
                                .getValue(User.class)
                                .getEmail()
                );

                user.setPhone_number(
                        ds.child(userID)
                                .getValue(User.class)
                                .getPhone_number()
                );

                user.setUser_id(
                        ds.child(userID)
                                .getValue(User.class)
                                .getUser_id()
                );

                Log.d(TAG, "getUserAccountSettings: retrieved user information: " + user.toString());

            }

        }

        return new UserSettings(user, settings);

    }

}

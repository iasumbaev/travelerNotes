package com.example.ivan.travelernotes.models;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

public class Post implements Parcelable {
    private static final String TAG = "Post";
    private ArrayList<String> caption;
    private String data_created;
    private ArrayList<String> image_path;
    private ArrayList<String> photo_ids;
    private String user_id;
    private String tags;
    private List<Like> likes;
    private List<Comment> comments;
    private String country;
    private String post_id;

    public Post() {
        image_path = new ArrayList<>();
        photo_ids = new ArrayList<>();
    }

    public Post(ArrayList<String> caption, String data_created, ArrayList<String> image_path, ArrayList<String> photo_ids, String user_id, String tags, List<Like> likes, List<Comment> comments, String country, String post_id) {
        this.caption = caption;
        this.data_created = data_created;
        this.image_path = image_path;
        this.photo_ids = photo_ids;
        this.user_id = user_id;
        this.tags = tags;
        this.likes = likes;
        this.comments = comments;
        this.country = country;
        this.post_id = post_id;
    }

    protected Post(Parcel in) {
        caption = in.createStringArrayList();
        data_created = in.readString();
        image_path = in.createStringArrayList();
        photo_ids = in.createStringArrayList();
        user_id = in.readString();
        tags = in.readString();
        country = in.readString();
        post_id = in.readString();
    }

    public static final Creator<Post> CREATOR = new Creator<Post>() {
        @Override
        public Post createFromParcel(Parcel in) {
            return new Post(in);
        }

        @Override
        public Post[] newArray(int size) {
            return new Post[size];
        }
    };

    public ArrayList<String> getCaption() {
        return caption;
    }

    public void setCaption(ArrayList<String> caption) {
        this.caption = caption;
    }

    public String getData_created() {
        return data_created;
    }

    public void setData_created(String data_created) {
        this.data_created = data_created;
    }

    public ArrayList<String> getImage_path() {
        return image_path;
    }

    public void setImage_path(ArrayList<String> image_path) {
        this.image_path = image_path;
    }

    public ArrayList<String> getPhoto_ids() {
        return photo_ids;
    }

    public void setPhoto_ids(ArrayList<String> photo_ids) {
        this.photo_ids = photo_ids;
    }

    public String getUser_id() {
        return user_id;
    }

    public void setUser_id(String user_id) {
        this.user_id = user_id;
    }

    public String getTags() {
        return tags;
    }

    public void setTags(String tags) {
        this.tags = tags;
    }

    public List<Like> getLikes() {
        return likes;
    }

    public void setLikes(List<Like> likes) {
        this.likes = likes;
    }

    public List<Comment> getComments() {
        return comments;
    }

    public void setComments(List<Comment> comments) {
        this.comments = comments;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getPost_id() {
        return post_id;
    }

    public void setPost_id(String post_id) {
        this.post_id = post_id;
    }

    public void addImage(String imagePath) {
        Log.d(TAG, "addImage: " + imagePath);
        image_path.add(imagePath);
    }

    public void addPhotoId(String id) {
        photo_ids.add(id);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeStringList(caption);
        parcel.writeString(data_created);
        parcel.writeStringList(image_path);
        parcel.writeStringList(photo_ids);
        parcel.writeString(user_id);
        parcel.writeString(tags);
        parcel.writeString(country);
        parcel.writeString(post_id);
    }

    @Override
    public String toString() {
        return "Post{" +
                "caption=" + caption +
                ", data_created='" + data_created + '\'' +
                ", image_path=" + image_path +
                ", photo_ids=" + photo_ids +
                ", user_id='" + user_id + '\'' +
                ", tags='" + tags + '\'' +
                ", likes=" + likes +
                ", comments=" + comments +
                ", country='" + country + '\'' +
                ", post_id='" + post_id + '\'' +
                '}';
    }
}

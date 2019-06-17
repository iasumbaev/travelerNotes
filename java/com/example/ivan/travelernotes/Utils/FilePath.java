package com.example.ivan.travelernotes.Utils;

import android.os.Environment;

public class FilePath {

    //"storage/emulated/0"
    public  String ROOT_DIR  = Environment.getExternalStorageDirectory().getPath();

    public String PICTURES = ROOT_DIR + "/Pictures";
    public String CAMERA = ROOT_DIR + "/DCIM/Camera";
    public String POSTS = ROOT_DIR + "/DCIM/Camera";

    public String FIREBASE_IMAGE_STORAGE = "photos/users/";
    public String FIREBASE_POSTS_STORAGE = "posts/users/";
}

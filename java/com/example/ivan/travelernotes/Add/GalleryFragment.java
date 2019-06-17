package com.example.ivan.travelernotes.Add;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.example.ivan.travelernotes.Profile.AccountSettingsActivity;
import com.example.ivan.travelernotes.R;
import com.example.ivan.travelernotes.Utils.FilePath;
import com.example.ivan.travelernotes.Utils.FileSearch;
import com.example.ivan.travelernotes.Utils.GridImageAdapter;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.assist.FailReason;
import com.nostra13.universalimageloader.core.listener.ImageLoadingListener;

import java.util.ArrayList;

public class GalleryFragment extends Fragment {

    private static final String TAG = "GalleryFragment";

    //constants
    private static final int NUM_GRID_COLUMNS = 3;

    //widgets
    private GridView gridView;
    //    private ImageView galleryImage;
    private Spinner directorySpinner;
    private LinearLayout imagesWrapper;

    //vars
    private ArrayList<String> directories;
    private String mAppend = "file:/";
    private String mSelectedImage;
    private ArrayList<String> imagesList;


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_gallery, container, false);
//        galleryImage = (ImageView) view.findViewById(R.id.galleryImageView);
        gridView = (GridView) view.findViewById(R.id.gridView);

        imagesList = new ArrayList<String>();
        directorySpinner = (Spinner) view.findViewById(R.id.spinnerDirectory);
        directories = new ArrayList<>();
        imagesWrapper = (LinearLayout) view.findViewById(R.id.galleryImageViewWrapper);
        Log.d(TAG, "onCreateView: started.");

        ImageView shareClose = (ImageView) view.findViewById(R.id.ivCloseShare);
        shareClose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "onClick: closing the gallery fragment");
                getActivity().finish();
            }
        });

        TextView nextScreen = (TextView) view.findViewById(R.id.tvNext);
        nextScreen.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "onClick: navigating to the final share screen.");

                if (isRootTask()) {
                    if (imagesWrapper.getChildCount() == 0) {
                        Toast.makeText(getActivity(), "Добавьте хотя бы одну фотографию", Toast.LENGTH_SHORT).show();
                    } else {
                        Intent intent = new Intent(getActivity(), NextActivity.class);
                        intent.putExtra(getString(R.string.selected_image), mSelectedImage);
                        intent.putExtra(getString(R.string.selected_images), imagesList);
                        startActivity(intent);
                    }
                } else {
                    Intent intent = new Intent(getActivity(), AccountSettingsActivity.class);
                    intent.putExtra(getString(R.string.selected_image), mSelectedImage);
                    intent.putExtra(getString(R.string.return_to_fragment), getString(R.string.edit_profile_fragment));
                    startActivity(intent);
                    getActivity().finish();
                }
            }
        });

        init();

        return view;
    }

    private boolean isRootTask() {
        if (((AddActivity) getActivity()).getTask() == 0) {
            return true;
        } else {
            return false;
        }
    }

    private void init() {
        FilePath filePath = new FilePath();

        //check for other folders "/storage/emulated/0/pictures"
        if (FileSearch.getDirectoryPath(filePath.PICTURES) != null) {
            directories = FileSearch.getDirectoryPath(filePath.PICTURES);
        }

        //Убрал, ибо так не все папки отображаются
        //Если понадобится, то в строке с объявлением адаптера(чуть ниже) нужно заменить directories на directoryNames

        /*ArrayList<String> directoryNames = new ArrayList<>();
        for(int i = 0; i < directories.size(); i++) {
            int index = directories.get(i).lastIndexOf("/");
            String string = directories.get(i).substring(index);
            directoryNames.add(string);
        }*/

        directories.add(filePath.CAMERA);


        if (FileSearch.getDirectoryPath(filePath.POSTS) != null) {
            directories.addAll(FileSearch.getDirectoryPath(filePath.POSTS));
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(getActivity(),
                android.R.layout.simple_spinner_item, directories);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        directorySpinner.setAdapter(adapter);

        directorySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                Log.d(TAG, "onItemSelected: selected: " + directories.get(i));

                //setup our image grid for the directory chosen
                setupGridView(directories.get(i));
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });


    }

    private void setupGridView(String selectedDirectory) {
        Log.d(TAG, "setupGridView: directory chosen: " + selectedDirectory);
        final ArrayList<String> imgURLs = FileSearch.getFilePath(selectedDirectory);

        //set the grid columns width
        int gridWidth = getResources().getDisplayMetrics().widthPixels;
        final int imageWidth = gridWidth / NUM_GRID_COLUMNS;
        gridView.setColumnWidth(imageWidth);

        //use grid adapter to adapter the images to gridview
        GridImageAdapter adapter = new GridImageAdapter(getActivity(), R.layout.layout_grid_imageview, mAppend, imgURLs);
        gridView.setAdapter(adapter);

        //TODO: вынести layoutparams выше
        //TODO: установить gravity для iv

        imagesWrapper.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, getResources().getDisplayMetrics().widthPixels / 10));
        //set the first image to be displayed when the activity fragment view is inflated
        if (imgURLs.size() > 0) {
            ImageView iv = new ImageView(getActivity());
            RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(gridWidth / 10, 200);
            iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
            iv.setLayoutParams(lp);
            // imagesWrapper.addView(iv);
            //setImage(imgURLs.get(0), iv, mAppend);
            //mSelectedImage = imgURLs.get(0);
            //imagesList.add(mSelectedImage);
            iv.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    LinearLayout parent = (LinearLayout) view.getParent();

                    for (int i = 0; i < parent.getChildCount(); i++) {
                        if (parent.getChildAt(i).equals(view)) {
                            parent.removeView(view);
                            imagesList.remove(i);
                        }
                    }


                }
            });
        }

        gridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                Log.d(TAG, "onItemClick: selected an image: " + imgURLs.get(i));

                if (imagesList.size() >= 10) {
                    Toast.makeText(getActivity(), "Достигнуто максимальное количество фотографий", Toast.LENGTH_SHORT).show();
                } else {
                    ImageView iv = new ImageView(getActivity());
                    RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(getResources().getDisplayMetrics().widthPixels / 10, getResources().getDisplayMetrics().widthPixels / 10);
                    iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
                    iv.setLayoutParams(lp);
                    imagesWrapper.addView(iv);
                    setImage(imgURLs.get(i), iv, mAppend);
                    mSelectedImage = imgURLs.get(i);
                    imagesList.add(mSelectedImage);
                    iv.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            LinearLayout parent = (LinearLayout) view.getParent();

                            for (int i = 0; i < parent.getChildCount(); i++) {
                                if (parent.getChildAt(i).equals(view)) {
                                    parent.removeView(view);
                                    imagesList.remove(i);
                                }
                            }
                        }
                    });
                }

            }
        });

    }

    private void setImage(String imgURL, ImageView image, String append) {
        Log.d(TAG, "setImage: setting image");

        ImageLoader imageLoader = ImageLoader.getInstance();

        imageLoader.displayImage(append + imgURL, image, new ImageLoadingListener() {
            @Override
            public void onLoadingStarted(String imageUri, View view) {
            }

            @Override
            public void onLoadingFailed(String imageUri, View view, FailReason failReason) {

            }

            @Override
            public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage) {

            }

            @Override
            public void onLoadingCancelled(String imageUri, View view) {

            }
        });
    }

}

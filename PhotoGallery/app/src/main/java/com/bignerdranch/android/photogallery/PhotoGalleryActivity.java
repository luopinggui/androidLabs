package com.bignerdranch.android.photogallery;

import androidx.fragment.app.Fragment;

public class PhotoGalleryActivity extends SingleFragmentActivity {
    @Override
    protected Fragment createFragment() {   //将PhotoFragment托管到PhotoActivity上
        return PhotoGalleryFragment.newInstance();
    }
}

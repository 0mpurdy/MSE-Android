package mse.mse_android.Views;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.ArrayList;

import mse.mse_android.R;
import mse.mse_android.data.author.Author;
import mse.mse_android.helpers.FileHelper;
import mse.mse_android.helpers.ReaderCreator;

public class MainActivity extends Activity {

    private String[] mAuthorList;

    private ArrayList<String> groupItem;
    private ArrayList<Object> childItem;

    SearchFragment searchFragment;

    public MainActivity() {
    }

//    private static class XAPKFile {
//        public final boolean mIsMain;
//        public final int mFileVersion;
//        public final long mFileSize;
//
//        XAPKFile(boolean isMain, int fileVersion, long fileSize) {
//            mIsMain = isMain;
//            mFileVersion = fileVersion;
//            mFileSize = fileSize;
//        }
//    }
//
//    private static final XAPKFile[] xAPKS = {
//            new XAPKFile(
//                    true, // true signifies a main file
//                    2, // the version of the APK that the file was uploaded
//                    // against
//                    139402018L // the length of the zipfile in bytes right click on you expansion file and get the size in bytes, size must be same as zip size
//            ),
//
//    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // request no title
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);

        // add the activity to the reader creator
        ReaderCreator.mActivity = this;

        searchFragment = new SearchFragment();

        // add the sprint 1 fragment
        if (savedInstanceState == null) {
            getFragmentManager().beginTransaction()
                    .add(R.id.content_frame, searchFragment, "fragment_game").commit();
        }

    }



    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if ((keyCode == KeyEvent.KEYCODE_BACK) && searchFragment.goBack()) {
            Log.d(this.getClass().getName(), "back button pressed");
            return true;
        } else {
            return super.onKeyDown(keyCode, event);
        }
    }
}

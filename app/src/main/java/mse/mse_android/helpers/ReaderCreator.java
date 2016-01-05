package mse.mse_android.helpers;

import android.app.Activity;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;

import mse.mse_android.common.Config;

/**
 * Created by Michael Purdy on 04/01/2016.
 *
 * This creates buffered readers for files in android
 */
public class ReaderCreator {

    public static Activity mActivity;
    public static Config mCfg;

    public static BufferedReader getBufferedReader(String path, boolean asset) throws IOException {
        if (asset) {
            return new BufferedReader(new InputStreamReader(mActivity.getAssets().open(path)));
        } else {
            return new BufferedReader(new FileReader(mActivity.getFilesDir() + File.separator + path));
        }
    }

    public static String getResultsFileLocation() {
        return mCfg.getResDir() + mCfg.getResultsFile();
    }
}

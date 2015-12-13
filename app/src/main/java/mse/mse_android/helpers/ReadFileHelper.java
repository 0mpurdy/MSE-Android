package mse.mse_android.helpers;

import android.app.Activity;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;

import mse.mse_android.data.Author;

/**
 * Created by mj_pu_000 on 05/12/2015.
 */
public class ReadFileHelper extends FileHelper {

    BufferedReader br;
    ArrayList<String> errors;

    String currentLine;

    public ReadFileHelper(Activity activity, Author author, int volNum, boolean asset) {
        this(activity, getHtmlFilePath(author, volNum), asset);
    }

    public ReadFileHelper(Activity activity, String filename, boolean asset) {
        super(activity, filename);
        this.errors = new ArrayList<>();

        try {
            br = new BufferedReader(new InputStreamReader(mActivity.getAssets().open(filename)));
        } catch (IOException e) {
            errors.add("Could not open " + filename);
        }
    }

    public String readLine() throws IOException {
        currentLine = br.readLine();
        return currentLine;
    }

    public void readLines(int count) throws IOException {
        for (int i = 0; i < count; i++) readLine();
    }

    public String getCurrentLine() {
        return currentLine;
    }

    @Override
    public void close() {
        if (br != null) {
            try {
                br.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}

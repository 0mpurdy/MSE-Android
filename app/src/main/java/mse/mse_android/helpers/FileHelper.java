package mse.mse_android.helpers;

import android.app.Activity;

import java.io.File;

import mse.mse_android.data.Author;
import mse.mse_android.data.BibleBook;
import mse.mse_android.data.HymnBook;

/**
 * Created by mj_pu_000 on 05/12/2015.
 */
public abstract class FileHelper {

    Activity mActivity;
    File file;

    public FileHelper(Activity activity, String filename){
        this.mActivity = activity;
        this.file = new File(filename);
    }

    public static String getHtmlFilePath(Author author, int volNum) {
        String filename = "";
        if (author.equals(Author.BIBLE)) {
            filename += author.getTargetPath(BibleBook.values()[volNum - 1].getName() + ".htm");
        } else if (author.equals(Author.HYMNS)) {
            filename += author.getTargetPath(HymnBook.values()[volNum - 1].getOutputFilename());
        } else {
            filename += author.getVolumePath(volNum);
        }

        return filename;
    }

    public abstract void close();

    public String getFilePath() {
        return file.getAbsolutePath();
    }
}

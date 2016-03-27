package mse.mse_android.helpers;

import android.app.Activity;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.InputStreamReader;

import mse.mse_android.data.Author;
import mse.mse_android.data.BibleBook;
import mse.mse_android.data.HymnBook;

/**
 * @author Michael Purdy
 *         Helps to generate links and folder paths
 */
public abstract class FileHelper {

    Activity mActivity;
    File file;

    private static final String TARGET_FOLDER = "files/target_a";

    public FileHelper(Activity activity, String filename) {
        this.mActivity = activity;
        this.file = new File(filename);
    }

    public static String getHtmlFileName(Author author, int volNum) {
        String filename = "";
        if (author.equals(Author.BIBLE)) {
            filename += getTargetPath(author, BibleBook.values()[volNum - 1].getBookFileName());
        } else if (author.equals(Author.HYMNS)) {
            filename += getTargetPath(author, HymnBook.values()[volNum - 1].getOutputFilename());
        } else {
            filename += getTargetVolumePath(author, volNum);
        }

        return filename;
    }

    public abstract void close();

    public String getFilePath() {
        return file.getAbsolutePath();
    }

    // region target paths

    public static String getTargetPath(Author author, String filename) {
        return TARGET_FOLDER + File.separator + author.getPath() + filename;
    }

    public static String getTargetVolumePath(Author author, int volumeNumber) {
        return getTargetPath(author, author.getVolumeName(volumeNumber));
    }

    public static String getIndexTargetPath(Author author) {
        return getTargetPath(author, author.getIndexFileName());
    }


    public static String getHtmlLink(Author author, String filename) {
        return "../../../" + TARGET_FOLDER + "/" + author.getPath() + filename;
    }

    // endregion
}

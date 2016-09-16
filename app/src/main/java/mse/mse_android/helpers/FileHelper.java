package mse.mse_android.helpers;

import android.app.Activity;

import java.io.File;

import mse.mse_android.common.config.Config;
import mse.mse_android.data.author.Author;
import mse.mse_android.data.author.BibleBook;
import mse.mse_android.data.author.HymnBook;

/**
 * @author Michael Purdy
 *         Helps to generate links and folder paths
 */
public abstract class FileHelper {

    public static String getHtmlFileName(Config cfg, Author author, int volNum) {
        String filename; //= cfg.getResDir();

        switch (author) {
            case BIBLE:
                filename = author.getTargetPath(BibleBook.values()[volNum - 1].getTargetName());
                break;
            case HYMNS:
                filename = author.getTargetPath(HymnBook.values()[volNum - 1].getTargetFilename());
                break;
            default:
                filename = author.getTargetVolumePath(volNum);

        }
        return filename;
    }

    public static String getTextFileName(Config cfg, Author author, int volNum) {
        String filename = cfg.getResDir();

        switch (author) {
            case BIBLE:
                filename += author.getSourcePath(BibleBook.values()[volNum - 1].getSourceName());
                break;
            case HYMNS:
                filename += author.getSourcePath(HymnBook.values()[volNum - 1].getSourceFilename());
                break;
            default:
                filename += author.getSourceVolumePath(volNum);

        }
        return filename;
    }

}

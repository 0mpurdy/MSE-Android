package mse.mse_android.data;

import java.io.File;

import mse.mse_android.data.author.Author;
import mse.mse_android.helpers.FileHelper;

/**
 * Created by Michael Purdy
 */
public enum PreparePlatform {

    PC("PC", ".." + File.separator + "MSE-Res-Lite" + File.separator + "res", "target", "../../mseStyle.css", "", false),
    ANDROID("Android", "", "files/target", "../../mseStyle.css", "mse:", false);

    private String name;
    private String res;
    private String targetFolder;
    private String stylesLink;
    private String linkPrefix;
    private boolean fullLink;

    PreparePlatform(String name, String res, String targetFolder, String stylesLink, String linkPrefix, boolean fullLink) {
        this.name = name;
        this.res = res;
        this.targetFolder = targetFolder;
        this.stylesLink = stylesLink;
        this.linkPrefix = linkPrefix;
        this.fullLink = fullLink;
    }

    public String getName() {
        return name;
    }

    public String getResDir() {
        return res;
    }

    public String getSourcePath() {
        return ".." + File.separator + "MSE-Res-Lite" + File.separator + "res" + File.separator + "source" + File.separator;
    }

    public String getTargetPath(boolean asset) {
        if (asset) return targetFolder;
        return res + File.separator + targetFolder;
    }

//    public String getTargetFolder() {
//        return targetFolder;
//    }

    public String getStylesLink() {
        return stylesLink;
    }
}

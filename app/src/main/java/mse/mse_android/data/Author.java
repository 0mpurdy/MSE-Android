package mse.mse_android.data;

import java.io.File;

public enum Author {

    BIBLE(0, "Bible", "Bible", "bible", 66, false, false),
    HYMNS(1, "Hymns", "Hymns", "hymns", 5, false, false),
    TUNES(2, "Tunes", "Hymn Tunes", "tunes", 100, false, false),
    JND(3, "JND", "J.N.Darby", "jnd", 52, true, true),
    JBS(4, "JBS", "J.B.Stoney",  "jbs", 17, true, false),
    CHM(5, "CHM", "C.H.Mackintosh", "chm", 18, true, false),
    FER(6, "FER", "F.E.Raven", "fer", 21, true, true),
    CAC(7, "CAC", "C.A.Coates", "cac", 37, true, true),
    JT(8, "JT", "J.Taylor Snr", "jt", 103, true, false),
    GRC(9, "GRC", "G.R.Cowell", "grc", 88, true, false),
    AJG(10, "AJG", "A.J.Gardiner", "ajg", 11, true, false),
    SMC(11, "SMC", "S.McCallum", "smc", 10, true, false),
    WJH(12, "WJH", "W.J.House", "wjh", 23, true, true),
    Misc(13, "Misc", "Various Authors", "misc", 26, true, false);

    private final int index;
    private final String code;
    private final String name;
    private final String folder;
    private final int numVols;
    private final boolean isMinistry;
    private final boolean searchable;

    Author(int index, String code, String name, String folder, int numVols, boolean isMinistry, boolean searchable) {
        this.index = index;
        this.code = code;
        this.name = name;
        this.folder = folder;
        this.numVols = numVols;
        this.isMinistry = isMinistry;
        this.searchable = searchable;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public String getTargetPath(String filename) {
        return "target" + File.separator + folder + File.separator + filename;
    }

    public String getVolumePath(int volumeNumber) {
        return getTargetPath(code + volumeNumber + ".htm");
    }

    public String getContentsName() {
        return code + "-Contents.htm";
    }

    public String getIndexFilePath() {
        return getTargetPath("index-" + getCode() + ".idx");
    }

    public boolean isMinistry() {
        return isMinistry;
    }

    public boolean isSearchable() {
        return searchable;
    }

    public int getNumVols() {
        return numVols;
    }

    public int getIndex() {
        return index;
    }
}

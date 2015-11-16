/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mse.mse_android.common;

import android.app.Activity;
import android.os.Parcel;
import android.os.Parcelable;

import mse.mse_android.data.Author;
import mse.mse_android.search.SearchScope;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;

/**
 *
 * @author michael
 */
public class Config {

    // the number of times a word has to appear before it is too frequent
    public final int TOO_FREQUENT = 10000;

    private String configFilePath = "Config.txt";

    private Logger logger;

    private Activity context;

    private String mseVersion;
    //    private String defaultBrowser;
    private String resDir;
    private String resultsFileName;
    private String searchString;
    private SearchScope searchScope;
    private HashMap<String, Boolean> selectedAuthors;
    private boolean setup;
    private boolean debugOn;

    public Config(Logger logger, Activity context) {

        this.logger = logger;
        this.context = context;

        configFilePath = context.getFilesDir() + File.separator + configFilePath;

        File configFile = new File(configFilePath);
        if (!configFile.exists()) {
            logger.log(LogLevel.LOW, "No config file found - setting defaults");
            setDefaults();
            save();
            return;
        } else {

            // for now always use defaults
            setDefaults();
            save();
            return;
        }
//
//        BufferedReader br = null;
//        try {
//
//            br = new BufferedReader(new FileReader(configFilePath));
//
//            mseVersion = getNextOption(br, "mseVersion");
//            resDir = getNextOption(br, "mseVersion");
//            resultsFileName = getNextOption(br, "resultsFileName");
//            searchString = getNextOption(br, "searchString");
//            synopsis = getNextBooleanOption(br, "synopsis");
//            beep = getNextBooleanOption(br, "beep");
//            splashWindow = getNextBooleanOption(br, "splashWindow");
//            autoLoad = getNextBooleanOption(br, "autoLoad");
//            fullScan = getNextBooleanOption(br, "fullScan");
//            setup = getNextBooleanOption(br, "setup");
//            debugOn = getNextBooleanOption(br, "debugOn");
//
//            // skip selected authors line
//            br.readLine();
//
//            selectedAuthors = new HashMap<>();
//
//            // for each searchable author
//            for (Author nextAuthor : Author.values()) {
//                if (nextAuthor.isSearchable()) {
//                    String[] splitLine = br.readLine().split(":");
//                    selectedAuthors.put(splitLine[0], Boolean.parseBoolean(splitLine[1]));
//                }
//            }
//
//
//        } catch (IOException | ArrayIndexOutOfBoundsException ex) {
//            logger.log(LogLevel.LOW, "Error reading config - setting defaults");
//            setDefaults();
//        }  finally {
//            if (br != null) try {
//                br.close();
//            } catch (IOException e) {
//                logger.log(LogLevel.HIGH, "Could not close config file");
//            }
//        }

    }

    private String getNextOption(BufferedReader br, String optionName) throws IOException {
        String option = "";
        try {
            option = br.readLine().split(":")[1];
        } catch (ArrayIndexOutOfBoundsException ex) {
            logger.log(LogLevel.DEBUG, "No value found for config option " + optionName);
        }
        return option;
    }

    private boolean getNextBooleanOption(BufferedReader br, String optionName) throws IOException {
        return Boolean.getBoolean(getNextOption(br, optionName));
    }

    private void setDefaults() {

        mseVersion = "3.0.0";
        resDir = File.separator;
//        defaultBrowser = "/usr/bin/firefox";
//        defaultBrowser = "C:\\Program Files (x86)\\Mozilla Firefox\\firefox.exe";
        resultsFileName = "SearchResults.htm";
        searchString = "";
        searchScope = SearchScope.SENTENCE;

        // set the selected books to be searched to only the bible
        selectedAuthors = new HashMap<>();
        for (Author nextAuthor : Author.values()) {
            if (nextAuthor.isSearchable()) {
                selectedAuthors.put(nextAuthor.getCode(), false);
            }
        }
        selectedAuthors.put(Author.BIBLE.getCode(), true);

        setup = false;
        debugOn = false;

    }

    public void save() {
        if (!setup) {

            File configFile = new File(configFilePath);

            BufferedWriter bw = null;
            try {

                bw = new BufferedWriter(new FileWriter(configFile));

                writeOption(bw,"mseVersion",mseVersion);
                writeOption(bw,"resDir",resDir);
                writeOption(bw,"resultsFileName",resultsFileName);
                writeOption(bw,"searchString",searchString);
                writeOption(bw,"setup",setup);
                writeOption(bw,"debugOn",debugOn);

                bw.write(" --- Selected Authors --- ");
                bw.newLine();

                for (String nextAuthorCode : selectedAuthors.keySet()) {
                    writeOption(bw, nextAuthorCode, selectedAuthors.get(nextAuthorCode).toString());
                }

//                changed to remove dependecy on gson
//                Gson gson = new GsonBuilder().setPrettyPrinting().create();
//                String json = gson.toJson(this);
//                File f = new File("config.txt");
//                PrintWriter pw = new PrintWriter(f);
//                pw.write(json);
//                pw.close();

                logger.log(LogLevel.DEBUG, "Config saved: " + configFile.getCanonicalPath());

            } catch (IOException ioe) {
                logger.log(LogLevel.LOW, "Could not write config" + ioe.getMessage());
            } finally {
                if (bw != null) try {
                    bw.close();
                } catch (IOException e) {
                    logger.log(LogLevel.HIGH, e.getMessage());
                }
            }
        }
    }

    public void toggleAuthorSelected(String authorCode) {
        setSelectedAuthor(authorCode, !isAuthorSelected(authorCode));
    }

    private void writeOption(BufferedWriter bw, String optionName, Object option) throws IOException {
        bw.write(optionName + ":" + option);
        bw.newLine();
    }

    private void writeOption(BufferedWriter bw, String optionName, String optionValue) throws IOException {
        bw.write(optionName + ":" + optionValue);
        bw.newLine();
    }

    public void setSetup(boolean setupCheck) {
        setup = setupCheck;
    }

    public boolean isSettingUp() {
        return setup;
    }

    public String getMseVersion() {
        return mseVersion;
    }

    public String getResDir() {
        return context.getFilesDir() + resDir;
    }

    public String getResultsFileName() {
        return "target" + File.separator + "results" + File.separator + resultsFileName;
    }

    public void setResultsFileName(String resultsFileName) {
        this.resultsFileName = resultsFileName;
    }

    public String getSearchString() {
        return searchString;
    }

    public void setSearchString(String searchString) {
        this.searchString = searchString;
    }

    public SearchScope getSearchScope() {
        return searchScope;
    }

    public void setSearchScope(SearchScope searchScope) {
        this.searchScope = searchScope;
    }

    public ArrayList<Author> getSelectedAuthors() {
        ArrayList<Author> selectedAuthors = new ArrayList<>();
        for (Author nextAuthor : Author.values()) {
            if (nextAuthor.isSearchable() && isAuthorSelected(nextAuthor.getCode())) selectedAuthors.add(nextAuthor);
        }
        return selectedAuthors;
    }

    public void setSelectedAuthors(HashMap<String, Boolean> selectedAuthors) {
        this.selectedAuthors = selectedAuthors;
    }

    public void setSelectedAuthor(String authorCode, boolean isSelected) {
        selectedAuthors.put(authorCode, isSelected);
    }

    public boolean isAuthorSelected(String authorCode) {
        return selectedAuthors.get(authorCode);
    }

    public boolean isAnyAuthorSelected() {
        boolean check = false;
        for (Author nextAuthor : Author.values()) {
            if (nextAuthor != Author.TUNES) {
                if (isAuthorSelected(nextAuthor.getCode())) {
                    check = true;
                }
            }
        }
        return check;
    }

    public void refresh() {
        setDefaults();
        save();
    }
}

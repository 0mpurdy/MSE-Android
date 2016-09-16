/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mse.mse_android.common.config;

import android.app.Activity;

import mse.mse_android.common.log.LogLevel;
import mse.mse_android.common.log.Logger;
import mse.mse_android.data.author.Author;
import mse.mse_android.data.search.SearchType;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * @author Michael Purdy
 */
public class Config {

    // the number of times a word has to appear before it is too frequent
    public final int TOO_FREQUENT = 10000;

    private final String contextDir;
    private final String configFilePath;

    private Logger logger;

    private static final String mseVersion = "3.0.2";

    private String resDir;
    private String resultsFileName;
    private String searchString;
    private SearchType searchType;
    private HashMap<String, Boolean> selectedAuthors;
    private boolean setup;

    public Config(Logger logger, String contextDir, String path, boolean useDefaults) {

        this.logger = logger;
        this.contextDir = contextDir;
        this.configFilePath = path;

        if (useDefaults) {
            setDefaults();
        } else {
            File configFile = new File(configFilePath);
            if (!configFile.exists()) {
                logger.log(LogLevel.LOW, "No config file found - setting defaults");
                setDefaults();
                save();
                return;
            }


            BufferedReader br = null;
            try {

                br = new BufferedReader(new FileReader(configFilePath));

                // skip mse Version
                br.readLine();
                resDir = getNextOption(br, "resDir");
                resultsFileName = getNextOption(br, "resultsFileName");
                searchString = getNextOption(br, "searchString");
                searchType = SearchType.fromString(getNextOption(br, "searchType"));
                if (searchType == null) searchType = SearchType.MATCH;

                // skip selected authors line
                br.readLine();

                selectedAuthors = new HashMap<>();

                // for each searchable author
                for (Author nextAuthor : Author.values()) {
                    if (nextAuthor.isSearchable()) {
                        String[] splitLine = br.readLine().split(":");
                        selectedAuthors.put(splitLine[0], Boolean.parseBoolean(splitLine[1]));
                    }
                }

            } catch (IOException | ArrayIndexOutOfBoundsException | NullPointerException ex) {
                logger.log(LogLevel.LOW, "Error reading config - setting defaults");
                setDefaults();
            } finally {
                if (br != null) try {
                    br.close();
                } catch (IOException e) {
                    logger.log(LogLevel.HIGH, "Could not close config file");
                }
            }
        }

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

        resDir = File.separator;
        resultsFileName = "SearchResults.htm";
        searchString = "";
        searchType = SearchType.MATCH;

        // set the selected books to be searched to only the bible
        selectedAuthors = new HashMap<>();
        for (Author nextAuthor : Author.values()) {
            if (nextAuthor.isSearchable()) {
                selectedAuthors.put(nextAuthor.getCode(), false);
            }
        }
        selectedAuthors.put(Author.BIBLE.getCode(), true);
        setup = false;

    }

    public void save() {
        if (!setup) {

            File configFile = new File(configFilePath);

            BufferedWriter bw = null;
            try {

                bw = new BufferedWriter(new FileWriter(configFile));

                writeOption(bw, "mseVersion", mseVersion);
                writeOption(bw, "resDir", resDir);
                writeOption(bw, "resultsFileName", resultsFileName);
                writeOption(bw, "searchString", searchString);
                writeOption(bw, "searchType", searchType.getMenuName());

                bw.write(" --- Selected Authors --- ");
                bw.newLine();

                for (String nextAuthorCode : selectedAuthors.keySet()) {
                    writeOption(bw, nextAuthorCode, selectedAuthors.get(nextAuthorCode).toString());
                }

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

    public String getMseVersion() {
        return mseVersion;
    }

    public String getResDir() {
        return contextDir + File.separator + resDir;
    }

    public String getResultsFile() {
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

    public SearchType getSearchType() {
        return searchType;
    }

    public void setSearchType(SearchType searchType) {
        this.searchType = searchType;
    }

    public HashMap<String, Boolean> getSelectedAuthors() {
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

    public void setSetup(boolean setupCheck) {
        setup = setupCheck;
    }

    public boolean isSettingUp() {
        return setup;
    }

    public void refresh() {
        setDefaults();
        save();
    }

    public String getPrevSearchesFile() {
        return getResDir() + File.separator + "target" + File.separator + "results" + File.separator + "PreviousSearches.html";
    }

    public String getSearchEngineHelpPage() {
        return getResDir() + File.separator + "other" + File.separator + "Help.html";
    }

    public String getSupportPage() {
        return getResDir() + File.separator + "other" + File.separator + "Contact.html";
    }

    public String getAboutPage() {
        return getResDir() + File.separator + "other" + File.separator + "AboutMSE.html";
    }

    public String getLabourersPage() {
        return getResDir() + File.separator + "other" + File.separator + "Labourers.html";
    }

}
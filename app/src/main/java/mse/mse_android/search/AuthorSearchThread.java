/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mse.mse_android.search;

// gui

import android.app.Activity;
import android.util.Log;

// java
import java.io.*;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;

// mse
import mse.mse_android.common.*;
import mse.mse_android.data.*;
import mse.mse_android.helpers.ReadFileHelper;


/**
 * @author michael
 */
public class AuthorSearchThread extends SingleSearchThread {

    Activity mActivity;

    private Config cfg;
    private ArrayList<LogRow> searchLog;

    AuthorSearchCache asc;

    private ArrayList<String> authorResults;

    private AtomicInteger progress;

    public AuthorSearchThread(Activity activity, Config cfg, AuthorSearchCache asc, AtomicInteger progress) {
        this.mActivity = activity;
        this.cfg = cfg;
        this.searchLog = new ArrayList<>();
        this.authorResults = new ArrayList<>();
        this.progress = progress;

        this.asc = asc;
    }

    @Override
    public void run() {

        searchAuthor(authorResults, asc);
        authorResults.add("Number of results for " + asc.getAuthorName() + ": " + asc.numAuthorResults);

    }

    private void searchAuthor(ArrayList<String> resultText, AuthorSearchCache asc) {

        searchLog.add(new LogRow(LogLevel.DEBUG, "\tSearching: " + asc.author.getName() + " for \"" + asc.getSearchString() + "\""));

        asc.setSearchWords();
        addAuthorHeader(resultText);

        // print the title of the author search results and search words
        searchLog.add(new LogRow(LogLevel.TRACE, "\tSearch strings: " + asc.printableSearchWords()));

        /* Search all the words to make sure that all the search tokens are in the author's
         * index. log any words that are too frequent, find the least frequent token and
         * record the number of infrequent words
         */
        if (asc.getWildSearch()) {
            asc.setSearchTokens(asc.getSearchWords());
        } else {
            asc.setSearchTokens(tokenizeArray(asc.getSearchWords(), asc.author.getCode(), 0, 0));
        }
        searchLog.add(new LogRow(LogLevel.TRACE, "\tSearch tokens: " + asc.printableSearchTokens()));

        int errorNum = asc.setLeastFrequentToken();

        if ((asc.getLeastFrequentToken() != null) && (errorNum % 4 < 2)) {
            // at least one searchable token and all tokens in author index

            asc.setReferencesToSearch();
            asc.refineReferences();

            // should be at least two references (volume and page to start)
            if (asc.referencesToSearch.length > 1) {
                // process each page that contains a match
                asc.refIndex = 0;
                asc.getNextPage();
                boolean volumeSuccess = true;

                // for each reference
                while (asc.volNum != 0 && volumeSuccess) {

                    if (asc.author.equals(Author.HYMNS)) addHymnBookTitle(resultText);

                    volumeSuccess = searchSingleVolume(resultText, asc);

                } // end one frequent token and all tokens found

            } // end had references to search
            else {
                searchLog.add(new LogRow(LogLevel.LOW, "No overlap in references for " + asc.author.getCode()));
            }

        } else {
            resultText.add("\t<p>");
            if (errorNum % 4 > 1) {
                resultText.add("\t\tNot all words were found in index.<br>");
                searchLog.add(new LogRow(LogLevel.LOW, "\tTokens in " + asc.author.getCode() + " not all found: " + asc.printableSearchTokens()));
            }
            if (errorNum >= 4) {
                resultText.add("\t\tSome words appeared too frequently: " + asc.getTooFrequentTokens());
                searchLog.add(new LogRow(LogLevel.LOW, "\tToo frequent tokens in " + asc.author.getCode() + ": " + asc.getTooFrequentTokens()));
            }
            resultText.add("\t</p>");
        }

    }

    private void addHymnBookTitle(ArrayList<String> resultText) {
        resultText.add(String.format("\t\t<p class=\"%s\">\n\t\t\t<a href=\"%s\">%s</a>",
                "results-hymnbook-name",
                "file:///android_asset/" + asc.author.getTargetPath(getVolumeName()),
                HymnBook.values()[asc.volNum - 1].getName()));
    }

    private void addAuthorHeader(ArrayList<String> resultText) {
        resultText.add("\n\t<hr>\n\t<h1>Results of search through " + asc.author.getName() + "</h1>");
        resultText.add("\n\t<p>\n\t\tSearched: " + asc.printableSearchWords() + "\n\t</p>");
    }

    private boolean searchSingleVolume(ArrayList<String> resultText, AuthorSearchCache asc) {
        // for each volume

        searchLog.add(new LogRow(LogLevel.TRACE, "\tVol: " + asc.volNum));

        int cPageNum;
        final int cVolNum = asc.volNum;

        // read the file
        ReadFileHelper rf = new ReadFileHelper(mActivity, asc.author, asc.volNum, true);
        try {

            // update progress
            progress.addAndGet(1000 / asc.author.getNumVols());

            // line should not be null when first entering loop
            asc.line = rf.readLine();

            while (asc.volNum == cVolNum) {
                // while still in the same volume
                // loop through references

                // skip to next page and get the last line of the previous page
                cPageNum = findNextPage(asc, rf);

                // if the page number is 0 log the error and break out
                if (cPageNum == 0) {
                    searchLog.add(new LogRow(LogLevel.HIGH, "Could not find reference " + getShortReadableReference()));
                    return false;
                }

                asc.currentSectionHeader = getFirstSectionHeader(rf);

                searchSinglePage(resultText, rf, asc);

                asc.line = asc.currentSectionHeader;

                // get the next reference
                asc.getNextPage();

                // if the current page is not the page before the next reference clear
                // previous line
                if (cPageNum != asc.pageNum - 1) {
                    asc.prevLine = "";
                }

            } // finished references in volume

        } catch (IOException ioe) {
            searchLog.add(new LogRow(LogLevel.HIGH, "Couldn't read " + rf.getFilePath() + " " + asc.volNum + ":" + asc.pageNum));
            Log.d("[BAD     ]", ioe.getMessage());
            ioe.printStackTrace();
            return false;
        } finally {
            rf.close();
        }

        return true;

    }

    private void searchSinglePage(ArrayList<String> resultText, ReadFileHelper rf, AuthorSearchCache asc) throws IOException {

        boolean foundToken = false;

        asc.setVerseNum(0);

        // while still on the same page (class != page-number)
        while (isNextSectionSearchable(asc.currentSectionHeader)) {

            asc.line = getNextSection(rf);

            ArrayList<String> stringsToSearch = new ArrayList<>();

            // get the searchLine based on the search scope
            if ((asc.getSearchScope() == SearchScope.SENTENCE) || asc.getSearchScope() == SearchScope.CLAUSE) {
                stringsToSearch = convertLineIntoSentences(asc.line, getTrailingIncompleteSentence(asc.prevLine));
            } else {
                stringsToSearch.add(asc.line);
            }

            asc.incrementVerseNum();

            // search the scope
            foundToken = searchScope(resultText, stringsToSearch, asc, foundToken);

            if (asc.author.equals(Author.BIBLE)) {
                resultText = asc.finishSearchingSingleBibleScope(removeHtml(asc.line), resultText, foundToken);
                if (asc.getSearchScope() == SearchScope.CLAUSE) {
                    foundToken = false;
                }
            }

            // set the current line as the previous line if it is searchable
            if (isNextSectionSearchable(asc.currentSectionHeader)) {
                asc.prevLine = asc.line;
            } else {
                asc.prevLine = "";
            }

            asc.currentSectionHeader = getNextSectionHeader(rf);
        }

        if (!foundToken) {
            if (asc.getInfrequentTokens().size() > 1) {
                searchLog.add(new LogRow(LogLevel.DEBUG, "Did not find token " + getShortReadableReference()));
            } else {
                searchLog.add(new LogRow(LogLevel.LOW, "Did not find token " + getShortReadableReference()));
            }
        }
    }

    private boolean isNextSectionSearchable(String sectionHeader) {

        if (asc.author.isMinistry()) {
            return sectionHeader.contains("class=\"heading\"") || sectionHeader.contains("class=\"paragraph\"") || sectionHeader.contains("class=\"footnote\"");
        } else if (asc.author.equals(Author.BIBLE)) {
            return sectionHeader.contains("<tr");
        } else if (asc.author.equals(Author.HYMNS)) {
            return sectionHeader.contains("class=\"verse-number\"");
        } else {
            return false;
        }
    }

    private boolean searchScope(ArrayList<String> resultText, ArrayList<String> stringsToSearch, AuthorSearchCache asc, boolean foundToken) {

        for (String scope : stringsToSearch) {

            // if the current scope contains all search terms mark them and print it out (or one if it is a wildcard search)
            SearchScope tempSearchScope = asc.getSearchScope();
            boolean validSentence = (tempSearchScope.equals(SearchScope.SENTENCE));
            if (validSentence && checkSentenceSearch(scope, asc) ||
                    asc.getSearchScope().equals(SearchScope.CLAUSE) && clauseSearch(tokenizeLine(scope, asc), asc.getSearchTokens())) {

                foundToken = true;

                String markedLine = markLine(new StringBuilder(scope), asc.getSearchWords());

                // close any opened blockquote tags
                if (markedLine.contains("<blockquote>")) markedLine += "</blockquote>";

                addResultText(resultText, markedLine);

                asc.incrementResults();
            }
        }

        return foundToken;
    }


    private void addResultText(ArrayList<String> resultText, String markedLine) {

        if (asc.author.isMinistry()) {
            resultText.add("\t<p>");
            resultText.add("\t\t<a href=\"file:///android_asset/" + asc.author.getTargetPath(getVolumeName() + "#" + asc.pageNum) + "\"> "
                    + getReadableReference() + "</a> ");
            resultText.add(markedLine);
            resultText.add("\t</p>");
        } else if (asc.author.equals(Author.BIBLE)) {

            if (!asc.isWrittenBibleSearchTableHeader()) {
                resultText.add("\t<p><a href=\"file:///android_asset/" + asc.author.getTargetPath(getVolumeName() + "#" + asc.pageNum + ":" + asc.getVerseNum()) + "\"> "
                        + getReadableReference() + "</a></p>");
                resultText.add("\t<table class=\"bible-searchResult\">");
                resultText.add("\t\t<tr>");
                asc.setWrittenBibleSearchTableHeader(true);
                if (!asc.isSearchingDarby()) {
                    resultText.add("\t\t\t<td class=\"mse-half\">" + asc.previousDarbyLine + "</td>");
                }
            }

            if (asc.isSearchingDarby()) {

                resultText.add("\t\t\t<td class=\"mse-half\">" + markedLine + "</td>");

            } else {

                resultText.add("\t\t\t<td class=\"mse-half\">" + markedLine + "</td>");

            }

        } else if (asc.author.equals(Author.HYMNS)) {

            resultText.add("\t<p>");
            resultText.add("\t\t<a href=\"file:///android_asset/" + asc.author.getTargetPath(getVolumeName() + "#" + asc.pageNum) + "\"> "
                    + getReadableReference() + "</a> ");
            resultText.add("<blockquote>" + markedLine + "</blockquote>");
            resultText.add("\t</p>");
        }

    }

    private String getReadableReference() {
        if (asc.author.isMinistry()) {
            return asc.author.getCode() + " volume " + asc.volNum + " page " + asc.pageNum;
        } else if (asc.author.equals(Author.BIBLE)) {
            return BibleBook.values()[asc.volNum - 1].getName() + " chapter " + asc.pageNum + ":" + asc.getVerseNum();
        } else if (asc.author.equals(Author.HYMNS)) {
            return Integer.toString(asc.pageNum);
        }

        return "";
    }

    private String getVolumeName() {
        if (asc.author.isMinistry()) {
            return asc.author.getCode() + asc.volNum + ".htm";
        } else if (asc.author.equals(Author.BIBLE)) {
            return BibleBook.values()[asc.volNum - 1].getName() + ".htm";
        } else if (asc.author.equals(Author.HYMNS)) {
            return HymnBook.values()[asc.volNum - 1].getOutputFilename();
        } else {
            return "";
        }
    }

    private String getFirstSectionHeader(ReadFileHelper rf) throws IOException {
        if (asc.author.isMinistry()) {
            // skip a line
            rf.readLine();
            return rf.readLine();
        } else if (asc.author.equals(Author.BIBLE)) {

            // skip synopsis
            rf.readLines(6);

            return rf.readLine();

        } else if (asc.author.equals(Author.HYMNS)) {

            // skip author and metre
            rf.readLines(7);

            return rf.readLine();

        }

        return null;

    }

    private String getNextSectionHeader(ReadFileHelper rf) throws IOException {
        // returns the line that contains the class of the next section

        if (asc.author.isMinistry()) {
            // skip a line
            rf.readLine();
            return rf.readLine();
        } else if (asc.author.equals(Author.BIBLE)) {

            // if still in same section do not change section header
            if (!asc.isSearchingDarby()) return asc.currentSectionHeader;

            // skip a line
            rf.readLine();

            return rf.readLine();

        } else if (asc.author.equals(Author.HYMNS)) {

            // debug counter for number of times called
            int i = 0;
            while (!rf.readLine().contains("class")) {
                i++;
            }

            return rf.getCurrentLine();

        }

        return null;

    }

    private String getNextSection(ReadFileHelper rf) throws IOException {

        if (asc.author.isMinistry()) {
            return rf.readLine();
        } else if (asc.author.equals(Author.BIBLE)) {
            String line = "";
            if (asc.isSearchingDarby()) {

                // skip verse number
                rf.readLine();

                // read jnd
                line = rf.readLine();

            } else {
                line = rf.readLine();
            }
            return line;
        } else if (asc.author.equals(Author.HYMNS)) {

            // skip verse anchor tag
            rf.readLines(3);

            String output = "";

            if (asc.volNum == 2 && asc.pageNum == 456) {
                Log.d("this", "that");
            }

            try {
                while (!rf.readLine().contains("</td>")) {
                    output += rf.getCurrentLine();
                }
            } catch (NullPointerException npe) {
                Log.d("[BAD   ]", getShortReadableReference());
            }

            return output;

        }

        return null;
    }

    // check sentence search
    private boolean checkSentenceSearch(String scope, AuthorSearchCache asc) {
        return wordSearch(tokenizeLine(scope, asc), asc.getSearchTokens(), asc.getWildSearch());
    }

    boolean foundCurrentSearchToken;

    private boolean wordSearch(String[] currentLineTokens, String[] searchTokens, boolean wildSearch) {

        for (String nextSearchToken : searchTokens) {
            foundCurrentSearchToken = false;
            for (String nextLineToken : currentLineTokens) {
                if (nextSearchToken.equals(nextLineToken)) {
                    foundCurrentSearchToken = true;
                    if (wildSearch) return true;
                }
            }
            if (!foundCurrentSearchToken && !asc.getWildSearch()) return false;
        }

        // if it reaches this point as a wild search then no tokens were found
        if (wildSearch) return false;
        return true;
    }

    private boolean clauseSearch(String[] currentLineTokens, String[] searchTokens) {

        boolean toggle = false;

        // read through the clause finding each word in order
        // return false if reached the end without finding every word

        // true if the next word should be next word in the search tokens
        boolean currentWordIsSearchToken;

        // position of the next token to find in the search tokens array
        int j = 0;

        for (int i = 0; i < currentLineTokens.length; i++) {
            currentWordIsSearchToken = false;
            if (currentLineTokens[i].equalsIgnoreCase(searchTokens[j])) {
                j++;
                currentWordIsSearchToken = true;
            }

            if (j > 0) {

                // if all words found in order return true
                if (j == searchTokens.length) return true;

                // if current word wasn't a search token reset j
                if (!currentWordIsSearchToken) j = 0;
            }

        }

        return false;

    }

    private int findNextPage(AuthorSearchCache asc, ReadFileHelper rf) throws IOException {
        // set the prevLine in asc to the last line of the previous page
        // return the page number of the next page
        // asc.Line is the page number and br is at the line of the current page number

        if (asc.author.equals(Author.BIBLE)) {
            return findNextBiblePage(asc, rf);
        } else if (asc.author.equals(Author.HYMNS)) {
            return findNextHymnPage(asc, rf);
        } else {
            return findNextMinistryPage(asc, rf);
        }

    }

    private int findNextHymnPage(AuthorSearchCache asc, ReadFileHelper rf) throws IOException {

        int cPageNum = 0;
        boolean foundPage = false;

        // clear the previous line
        asc.prevLine = "";

        // read until page number = page ref
        // or if page number is page before next reference get the last line
        while (!foundPage) {

            if (asc.line != null) {

                if (asc.line.contains("<td class=\"hymn-number\">")) {
                    asc.line = rf.readLine();

                    try {

                        // get the current page's number
                        cPageNum = getPageNumber(asc.line, "=", ">");

                        if (asc.pageNum == cPageNum) {
                            return cPageNum;
                        }

                    } catch (NumberFormatException nfe) {
                        searchLog.add(new LogRow(LogLevel.HIGH, "Error formatting page number in search: " + asc.author.getCode() + " " + asc.volNum + ":" + cPageNum));
                        return 0;
                    }
                }
            } else {
                searchLog.add(new LogRow(LogLevel.HIGH, "NULL line when reading " + getShortReadableReference()));
                break;
            }
            asc.line = rf.readLine();

        } // found start of page

        // shouldn't reach here
        return 0;
    }

    private int getPageNumber(String line, String splitStart, String splitEnd) {
        int start = line.indexOf(splitStart) + splitStart.length();
        int end = line.indexOf(splitEnd, start);
        return Integer.parseInt(line.substring(start, end));
    }

    private int findNextBiblePage(AuthorSearchCache asc, ReadFileHelper rf) throws IOException {

        int cPageNum = 0;
        boolean foundPage = false;

        // clear the previous line
        asc.prevLine = "";

        // read until page number = page ref
        // or if page number is page before next reference get the last line
        while (!foundPage) {

            if (asc.line != null) {

                if (asc.line.contains("<td colspan=\"3\" class=\"chapterTitle\">")) {

                    try {

                        // get the current page's number
                        int startIndex = asc.line.indexOf("name=") + 5;
                        int endIndex = asc.line.indexOf('>', startIndex);
                        String cPageNumStr = asc.line.substring(startIndex, endIndex);
                        cPageNum = Integer.parseInt(cPageNumStr);

                        // if it is the previous page
                        if (asc.pageNum == cPageNum + 1) {
                            asc.prevLine = getLastLineOfBiblePage(rf);

                            // skip close and open of table and open of row
                            rf.readLines(3);

                            // set found page get page number
                            asc.line = rf.readLine();

                            startIndex = asc.line.indexOf("name=") + 5;
                            endIndex = asc.line.indexOf('>', startIndex);
                            cPageNumStr = asc.line.substring(startIndex, endIndex);
                            cPageNum = Integer.parseInt(cPageNumStr);

                            if (asc.pageNum == cPageNum) {
                                return cPageNum;
                            } else {
                                // error next page not after previous page
                                searchLog.add(new LogRow(LogLevel.LOW, "Couldn't find search page: " + asc.author.getCode() + " " + asc.volNum + ":" + asc.pageNum));
                                return 0;
                            }
                        } else if (asc.pageNum == cPageNum) {
                            return cPageNum;
                        }
                    } catch (NumberFormatException nfe) {
                        searchLog.add(new LogRow(LogLevel.HIGH, "Error formatting page number in search: " + asc.author.getCode() + " " + asc.volNum + ":" + cPageNum));
                        return 0;
                    }
                }
            } else {
                searchLog.add(new LogRow(LogLevel.HIGH, "NULL line when reading " + getShortReadableReference()));
                break;
            }
            asc.line = rf.readLine();

        } // found start of page

        // shouldn't reach here
        return 0;
    }

    private String getLastLineOfBiblePage(ReadFileHelper rf) throws IOException {
        // returns the last darby line of the bible and moves
        // the buffered reader pointer to point at the end of the chapter
        // (empty line before </table>)

        // skip synopsis
        rf.readLines(6);

        // while still on the page get the darby line
        String darbyLine = "";
        while ((rf.readLine()).contains("<tr")) {
            // skip verse number
            rf.readLine();

            // set the line
            darbyLine = rf.readLine();

            // skip kjv and end of row tag
            rf.readLines(2);
        }

        return darbyLine;
    }

    private int findNextMinistryPage(AuthorSearchCache asc, ReadFileHelper rf) throws IOException {

        int cPageNum = 0;
        boolean foundPage = false;

        // clear the previous line
        asc.prevLine = "";

        // read until page number = page ref
        // or if page number is page before next reference get the last line
        while (!foundPage) {

            if (asc.line != null) {

                if (asc.line.contains("class=\"page-number\"")) {
                    asc.line = rf.readLine();

                    try {

                        // get the current page's number
                        cPageNum = Integer.parseInt(asc.line.substring(asc.line.indexOf("=") + 1, asc.line.indexOf('>')));

                        // if it is the previous page
                        if (asc.pageNum == cPageNum + 1) {
                            asc.prevLine = getLastLineOfPage(rf);

                            // set found page get page number
                            asc.line = rf.readLine();

                            // skip any footnotes
                            while (asc.line.contains("class=\"footnote\"")) {
                                rf.readLines(2);
                                asc.line = rf.readLine();
                            }

                            cPageNum = Integer.parseInt(asc.line.substring(asc.line.indexOf("=") + 1, asc.line.indexOf('>')));

                            if (asc.pageNum == cPageNum) {
                                return cPageNum;
                            } else {
                                // error next page not after previous page
                                searchLog.add(new LogRow(LogLevel.LOW, "Couldn't find search page: " + asc.author.getCode() + " " + asc.volNum + ":" + asc.pageNum));
                                return 0;
                            }
                        } else if (asc.pageNum == cPageNum) {
                            return cPageNum;
                        }
                    } catch (NumberFormatException nfe) {
                        searchLog.add(new LogRow(LogLevel.HIGH, "Error formatting page number in search: " + asc.author.getCode() + " " + asc.volNum + ":" + cPageNum));
                        return 0;
                    }
                }
            } else {
                searchLog.add(new LogRow(LogLevel.HIGH, "NULL line when reading " + asc.author.getCode() + " vol " + asc.volNum + " page " + asc.pageNum));
                break;
            }
            asc.line = rf.readLine();

        } // found start of page

        // shouldn't reach here
        return 0;
    }

    private String getLastLineOfPage(ReadFileHelper rf) throws IOException {

        // for each paragraph in the page
        // skip a line and check if the next line is a heading or paragraph
        rf.readLine();

        String tempLine = rf.readLine();
        String lastLine = "";

        // while not the last line
        while (tempLine.contains("class=\"heading\"") || tempLine.contains("class=\"paragraph\"") || tempLine.contains("class=\"footnote\"")) {

            // read the line of text
            lastLine = rf.readLine();

            // skip the </div> tag line
            rf.readLine();

            // read the next line class
            tempLine = rf.readLine();
        }

        return lastLine;

    }

    int startOfLastSentencePos;

    private String getTrailingIncompleteSentence(String line) {

        if (asc.author.equals(Author.BIBLE)) return line;

        if (line.isEmpty()) return "";

        startOfLastSentencePos = line.lastIndexOf("<a name=");

        // no sentence break in line
        if (startOfLastSentencePos < 0) return line;

        startOfLastSentencePos = line.indexOf('>', startOfLastSentencePos) + 1;
        startOfLastSentencePos = line.indexOf('>', startOfLastSentencePos) + 1;

        if (startOfLastSentencePos < line.length()) return line.substring(startOfLastSentencePos);
        return "";
    }

    ArrayList<String> sentences = new ArrayList<>();
    int startOfSentencePos;
    int endOfSentencePos;

    private ArrayList<String> convertLineIntoSentences(String line, String trailingIncompleteSentence) {

        sentences.clear();
        startOfSentencePos = 0;
        endOfSentencePos = 0;

        if (!line.contains("<a name=")) {
            // if there are no fullstops return the whole line
            sentences.add(line);
            return sentences;
        }

        while (endOfSentencePos >= 0 && endOfSentencePos < line.length()) {

            endOfSentencePos = line.indexOf("<a name=", startOfSentencePos) - 1;

            // if there are no fullstops return the whole line
            if (endOfSentencePos < 0) {
                sentences.add(line.substring(startOfSentencePos, line.length()));
                return sentences;
            }

            sentences.add(line.substring(startOfSentencePos, endOfSentencePos));

            // skip the a tags
            startOfSentencePos = line.indexOf('>', endOfSentencePos) + 1;
            startOfSentencePos = line.indexOf('>', startOfSentencePos) + 1;
            endOfSentencePos = startOfSentencePos;

        }

        if (sentences.size() > 0)
            sentences.set(0, trailingIncompleteSentence + " " + sentences.get(0));

        return sentences;
    }

    StringBuilder lineBuilder = new StringBuilder();
    int startHtml;
    int endHtml;

    private String[] tokenizeLine(String line, AuthorSearchCache asc) {

        // if the line contains html - remove the html tag
        while (line.contains("<")) {

            // set the lineBuilder to the string passed in
            lineBuilder.setLength(0);
            lineBuilder.append(line);
            startHtml = 0;
            while ((startHtml < lineBuilder.length()) && (lineBuilder.charAt(startHtml) != '<')) {
                startHtml++;
            }
            endHtml = startHtml + 1;
            while ((endHtml < lineBuilder.length()) && (lineBuilder.charAt(endHtml) != '>')) {
                endHtml++;
            }
            if ((startHtml < lineBuilder.length()) && (endHtml <= lineBuilder.length()))
                lineBuilder.replace(startHtml, endHtml + 1, "");

            line = lineBuilder.toString();
        }

        // split the line into tokens (words) by non-word characters
        return tokenizeArray(line.split("[\\W]"), asc.getAuthorCode(), asc.volNum, asc.pageNum);
    }

    String[] newTokensArray;
    ArrayList<String> newTokens = new ArrayList<>();

    private String[] tokenizeArray(String[] tokens, String authorCode, int volNum, int pageNum) {

        newTokens.clear();

        // make each token into a word that can be searched
        for (String token : tokens) {
            token = token.toUpperCase();
            if (!isAlpha(token)) {
                token = processString(token);
            }
            if (!isAlpha(token)) {
                searchLog.add(new LogRow(LogLevel.HIGH, "Error processing token " + authorCode + " " + volNum + ":" + pageNum + ": " + token));
                token = "";
            }
            newTokens.add(token);
        } // end for each token

        newTokensArray = new String[newTokens.size()];
        newTokensArray = newTokens.toArray(newTokensArray);

        return newTokensArray;
    }

    private boolean isAlpha(String token) {
        char[] chars = token.toCharArray();

        for (char c : chars) {
            if (!Character.isLetter(c)) {
                return false;
            }
        }
        return true;
    }

    private String processString(String token) {
        for (char c : token.toCharArray()) {
            if (!Character.isLetter(c)) {
                token = token.replace(Character.toString(c), "");
            }
        }

        return token;
    }

    private String getBasicWords(String strIn, boolean dropPunctuation, boolean dropTableTags) {
        String inString = strIn;
        String outString = "";
        char currentChar;
        int currentPosition = 0;
        int numSpaces = 0;
        int numCols = 0;
        int lengthOfInString = inString.length();

        //remove html tags & page numbers
        while (currentPosition < lengthOfInString) {
            currentChar = inString.charAt(currentPosition);
            if ((currentChar == 13) || (currentChar == 10)) {
                //ignore carriage returns & line feeds

            } else if (currentChar == '<') {

                // get the position of an html starting tag
                int keepCurrPos = currentPosition;

                // find the positon of an html end tag
                while ((currentPosition < inString.length()) && (inString.charAt(currentPosition) != '>')) {
                    currentPosition++;
                }

                // TODO -I find out why you do this
                // if you haven't reached the end of the string to remove tags from and you don't have to drop table tags
                if ((currentPosition < inString.length()) && (!dropTableTags)) {

                    // get the contents of the tag
                    String htmlTag = inString.substring(keepCurrPos, currentPosition);

                    if ((htmlTag.equals("</td")) && (numCols < 2)) {
                        outString += "</td><td>";
                        numCols++;
                    } else if (htmlTag.equals("</tr")) {
                        outString += ("</td></tr><tr><td>");
                        numCols = 0;
                    }
                }

                // TODO -I find out what this does
                if ((inString.length() - currentPosition) > 10) {//possibly new page
                    String strNextBit = inString.toString().substring(currentPosition + 1, currentPosition + 7);
                    if (strNextBit.equals("[Page ")) {//throw away word 'page'
                        while ((currentPosition < inString.length()) && (inString.charAt(currentPosition) != ']')) {
                            currentPosition++;
                        }
                    }
                }

                // TODO -I find out why
                // if there are no spaces, add one on the end
                if (numSpaces == 0) {
                    outString += " ";
                    numSpaces++;
                }

                // TODO -I could this be else if
            } else {
                if (dropPunctuation) {

                    // if punctuation is to be dropped
                    if (Character.isLetter(currentChar) || Character.isDigit(currentChar)) {

                        // if the character is a letter or a digit
                        // add the current character the the output
                        outString += currentChar;
                        numSpaces = 0;

                    } else if (currentChar == '-' || currentChar == '\'') {
                        if ((Character.isLetter(outString.charAt(outString.length() - 1))) && (numSpaces == 0)) {
                            // if the last character in the output so far is a letter and the number of spaces is zero
                            outString += currentChar;
                        } else if (numSpaces == 0) {
                            outString += " ";
                            numSpaces++;
                        }
                    } else if (numSpaces == 0) {
                        outString += " ";
                        numSpaces++;
                    }
                } else {
                    // if punctation is kept just add the current character to the output
                    outString += currentChar;
                    numSpaces = 0;
                }
            }
            currentPosition++;
        }
        return outString;
    }

    int charPos = 0;
    int startOfWord = 0;
    int endOfWord = 0;
    String currentCapitalisation;

    private String markLine(StringBuilder line, String[] words) {
        // highlight all the search words in the line with an html <mark/> tag

        // remove any html already in the line
        charPos = 0;

        if (!asc.author.equals(Author.HYMNS)) line = removeHtml(line);

        for (String word : words) {

            charPos = 0;

            // while there are still more words matching the current word in the line
            // and the char position hasn't exceeded the line
            while (charPos < line.length() && ((startOfWord = line.toString().toLowerCase().indexOf(word.toLowerCase(), charPos)) != -1)) {

                endOfWord = startOfWord + word.length();
                charPos = endOfWord;

                // if the word has a letter before it or after it then skip it
                if (startOfWord >= 0 && Character.isLetter(line.charAt(startOfWord - 1))) continue;
                if (endOfWord < line.length() && Character.isLetter(line.charAt(endOfWord)))
                    continue;

                // otherwise mark the word
                currentCapitalisation = line.substring(startOfWord, endOfWord);
                line.replace(startOfWord, endOfWord, "<mark>" + currentCapitalisation + "</mark>");

                // set the char position to after the word
                charPos = endOfWord + 13;
            }
        }

        return line.toString();
    }


    private String removeHtml(String line) {
        return removeHtml(new StringBuilder(line)).toString();
    }

    private StringBuilder removeHtml(StringBuilder line) {
        int charPos = -1;

        while (++charPos < line.length()) {
            if (line.charAt(charPos) == '<') {
                int tempCharIndex = charPos + 1;
                while (tempCharIndex < line.length() - 1 && line.charAt(tempCharIndex) != '>')
                    tempCharIndex++;
                tempCharIndex++;
                line.replace(charPos, tempCharIndex, "");
            }
        }

        return line;
    }

    public String getShortReadableReference() {
        if (asc.author.isMinistry()) {
            return asc.author.getCode() + "vol " + asc.volNum + ":" + asc.pageNum;
        } else if (asc.author.equals(Author.BIBLE)) {
            return BibleBook.values()[asc.volNum - 1].getName() + " " + asc.pageNum + ":" + asc.getVerseNum();
        } else if (asc.author.equals(Author.HYMNS)) {
            return HymnBook.values()[asc.volNum -1].getName() + " " + asc.pageNum + ":" + asc.getVerseNum();
        }
        return "Can't get short readable reference";
    }

    @Override
    public ArrayList<LogRow> getLog() {
        return searchLog;
    }

    @Override
    public ArrayList<String> getResults() {
        return authorResults;
    }

    @Override
    int getNumberOfResults() {
        return asc.numAuthorResults;
    }

}

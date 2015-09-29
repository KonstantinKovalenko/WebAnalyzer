package webanalyzer;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.*;

/**
 *
 * @author Admin
 */
public class WebAnalyzer {

    private URL url;
    private boolean URLisValid = true;
    private HttpURLConnection conn;
    private BufferedReader in;
    private String webPageURL;
    private String pageCodeString;
    private static String phraseMatch;
    private Set<String> tempInnerLinks = new HashSet();
    private static volatile Set<String> haveToVisit = new HashSet();
    private static volatile Set<String> visitedInnerLinks = new HashSet();
    private int pageSymbolCounter = 0;
    private int pageMatchesCounter = 0;
    private ExecutorService exec = Executors.newCachedThreadPool();

    public WebAnalyzer(String webPageURL) {
        this.webPageURL = webPageURL;
    }

    public WebAnalyzer(String webPageURL, String phraseMatch) {
        this.webPageURL = webPageURL;
        this.phraseMatch = phraseMatch;
    }

    /**
     * Returns true if the Web Page was sucesfully scan. If URL is valid, method
     * scans a Web Page, fills the queue visit the following pages, saves data
     * on hard drive.
     *
     * @return boolean result of web page scan.
     *
     * @throws NullPointerException if {@code webPageURL} is null.
     */
    boolean scanWebPage() {
        try {
            if (webPageURL == null) {
                throw new NullPointerException();
            }
            url = new URL(webPageURL);
            if (visitedInnerLinks.isEmpty()) {
                visitedInnerLinks.add(getWebPageURL());
            }
        } catch (MalformedURLException e) {
            System.err.println(e);
            URLisValid = false;
            return false;
        }
        if (URLisValid) {
            try {
                conn = (HttpURLConnection) url.openConnection();
                in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                final String HREF = "href=:";
                while ((pageCodeString = in.readLine()) != null) {
                    pageSymbolCounter += pageCodeString.length();
                    if (phraseMatch != null) {
                        String clonePageCodeString = pageCodeString;
                        while (clonePageCodeString.contains(phraseMatch)) {
                            pageMatchesCounter++;
                            clonePageCodeString = clonePageCodeString.substring(clonePageCodeString.indexOf(phraseMatch) + phraseMatch.length());
                        }
                    }
                    while (fullCheckPCS(pageCodeString)) {
                        addToTempSet(pageCodeString, tempInnerLinks, webPageURL);
                        pageCodeString = pageCodeString.substring(pageCodeString.indexOf("href") + HREF.length());
                    }
                }
                fillVisited();
                saveData(webPageURL);
            } catch (IOException e) {
                System.err.println(e);
            }
        }
        return true;
    }

    private synchronized void fillVisited() {
        for (String s : tempInnerLinks) {
            if (!visitedInnerLinks.contains(s)) {
                haveToVisit.add(s);
            }
        }
    }

    private synchronized List<String> getNotVisitedElements() {
        ArrayList<String> result = new ArrayList();
        for (String s : haveToVisit) {
            if (!visitedInnerLinks.contains(s)) {
                result.add(s);
                visitedInnerLinks.add(s);
            }
        }
        haveToVisit.clear();
        return result;
    }

    /**
     * Method scans Web Site creating a separated thread for each page. Received
     * data saves in path: "c\WebAnalyzer\pageName\".
     *
     */
    public void scanWebSite() {
        scanWebPage();
        while (!haveToVisit.isEmpty()) {
            for (String s : getNotVisitedElements()) {
                WebAnalyzer wa = new WebAnalyzer(s);
                Thread t = new Thread() {
                    @Override
                    public void run() {
                        wa.scanWebPage();
                    }
                };
                exec.execute(t);
            }
            try {

                TimeUnit.SECONDS.sleep(2);
            } catch (InterruptedException e) {
                System.err.println(e);
            }
        }
    }

    /**
     * Method returns matches in the incoming String by pattern
     *
     * @param stringToCheck is a String to be checked
     * @param pattern is a String pattern
     */
    private boolean checkPageCodeString(String stringToCheck, String pattern) {
        Pattern myPattern = Pattern.compile(pattern);
        Matcher myMatcher = myPattern.matcher(stringToCheck);
        return myMatcher.matches();
    }

    private boolean fullCheckPCS(String stringToCheck) {
        return (checkPageCodeString(stringToCheck, ".*href=\".*" + returnCleanURL(webPageURL) + ".*\\.html.*")
                || checkPageCodeString(stringToCheck, ".*href=\"\\/?\\w*\\.html.*"))
                || (checkPageCodeString(stringToCheck, ".*href=\".*" + returnCleanURL(webPageURL) + ".*\\.php.*")
                || checkPageCodeString(stringToCheck, ".*href=\"\\/?\\w*\\.php.*"))
                || (checkPageCodeString(stringToCheck, ".*href=\".*" + returnCleanURL(webPageURL) + ".*\\.xml.*")
                || checkPageCodeString(stringToCheck, ".*href=\"\\/?\\w*\\.xml.*"));
    }

    private boolean simpleCheckPCS(String stringToCheck) {
        return checkPageCodeString(stringToCheck, ".*\\.html.*")
                || checkPageCodeString(stringToCheck, ".*\\.php.*")
                || checkPageCodeString(stringToCheck, ".*\\.xml.*");
    }

    /**
     * Returns cutted string contains only Web Page name and its domain or
     * domains.
     *
     * @param inputString is a setted URL in constructor.
     *
     * @return main page URL as a String
     *
     * @throws IllegalArgumentException if URL is not valid (means setted URL in
     * constructor is not valid)
     */
    String returnCleanURL(String inputString) {
        if (URLisValid) {
            String resultString = inputString;
            final String HTTP = "http://";
            final String HTTPS = "https://";
            final int indOfHttp = resultString.indexOf("http");
            final int indOfHttps = resultString.indexOf("https");
            final int indOfSlash = resultString.indexOf("/", (indOfHttp + HTTP.length()));
            final int indOfSlashHttps = resultString.indexOf("/", (indOfHttp + HTTPS.length()));
            if (indOfSlashHttps != -1) {
                if (indOfHttps != -1) {
                    resultString = resultString.substring(indOfHttps + HTTPS.length(), indOfSlashHttps);
                    return resultString;
                } else {
                    resultString = resultString.substring(indOfHttp + HTTP.length(), indOfSlash);
                    return resultString;
                }
            } else {
                if (indOfHttps != -1) {
                    resultString = resultString.substring(indOfHttps + HTTPS.length());
                    return resultString;
                } else {
                    resultString = resultString.substring(indOfHttp + HTTP.length());
                    return resultString;
                }
            }
        } else {
            throw new IllegalArgumentException();
        }
    }

    private void addToTempSet(String inputString, Collection inputCollection, String inputWebPageURL) {
        String resultString = inputString;
        if (checkPageCodeString(resultString, ".*href=\".*" + returnCleanURL(inputWebPageURL) + ".*\\.html.*")
                || checkPageCodeString(resultString, ".*href=\".*" + returnCleanURL(inputWebPageURL) + ".*\\.php.*")
                || checkPageCodeString(resultString, ".*href=\".*" + returnCleanURL(inputWebPageURL) + ".*\\.xml.*")) {
            addSimpleLink(resultString, inputCollection);
        } else if (checkPageCodeString(resultString, ".*href=\".*\\.html.*")
                || checkPageCodeString(resultString, ".*href=\".*\\.php.*")
                || checkPageCodeString(resultString, ".*href=\".*\\.xml.*")) {
            addComplicatedLink(resultString, inputCollection, inputWebPageURL);
        }
    }

    /**
     * The method adds to the collection String URL (an internal link on the
     * page) after processing the original line of code page.
     *
     * @param inputString is an original line of code page.
     * @param inputCollection is a collection where the element is added.
     *
     */
    void addSimpleLink(String inputString, Collection inputCollection) {
        final String HREF = "href=:";
        final int indOfHrefHttp = inputString.indexOf("href=\"http");
        final int indOfQuotes = inputString.indexOf("\"", (indOfHrefHttp + HREF.length()));
        if (indOfQuotes != -1) {
            inputString = inputString.substring((indOfHrefHttp + HREF.length()), indOfQuotes);
            if (simpleCheckPCS(inputString)) {
                inputCollection.add(inputString);
            }
        }
    }

    /**
     * The method adds to the collection String URL (an internal link on the
     * page) after processing the original line of code page.
     *
     * @param inputString is an original line of code page.
     * @param inputCollection is a collection where the element is added.
     * @param inputWebPageURL is a URL of this page, required for processing.
     *
     */
    void addComplicatedLink(String inputString, Collection inputCollection, String inputWebPageURL) {
        final String HREF = "href=:";
        final int indOfHref = inputString.indexOf("href=\"");
        final int indOfQuotes = inputString.indexOf("\"", (indOfHref + HREF.length()));
        if (indOfQuotes != -1) {
            inputString = inputString.substring((indOfHref + HREF.length()), indOfQuotes);
            if (simpleCheckPCS(inputString)) {
                if (inputWebPageURL.equals("http://" + returnCleanURL(inputWebPageURL))
                        || inputWebPageURL.equals("https://" + returnCleanURL(inputWebPageURL))) {
                    if (inputString.charAt(0) != '/') {
                        inputCollection.add(inputWebPageURL + "/" + inputString);
                    } else {
                        inputCollection.add(inputWebPageURL + inputString);
                    }
                } else if (inputWebPageURL.equals("http://" + returnCleanURL(inputWebPageURL) + "/")
                        || inputWebPageURL.equals("https://" + returnCleanURL(inputWebPageURL) + "/")) {
                    if (inputString.charAt(0) != '/') {
                        inputCollection.add(inputWebPageURL + inputString);
                    } else {
                        if (inputWebPageURL.equals("http://" + returnCleanURL(inputWebPageURL) + "/")) {
                            inputCollection.add("http://" + returnCleanURL(inputWebPageURL) + inputString);
                        } else {
                            inputCollection.add("https://" + returnCleanURL(inputWebPageURL) + inputString);
                        }
                    }
                } else {
                    String handleWPU = inputWebPageURL;
                    final int lastIndOfSlash = handleWPU.lastIndexOf("/");
                    if (inputString.charAt(0) != '/') {
                        handleWPU = handleWPU.substring(0, lastIndOfSlash + 1);
                        inputCollection.add(handleWPU + inputString);
                    } else {
                        handleWPU = handleWPU.substring(0, lastIndOfSlash);
                        inputCollection.add(handleWPU + inputString);
                    }
                }
            }
        }
    }

    String alignFolderName(String inputString) {
        final String HTTP = "http://";
        final String HTTPS = "https://";
        final int indOfHttp = inputString.indexOf("http");
        final int indOfHttps = inputString.indexOf("https");
        if (indOfHttps != -1) {
            inputString = inputString.substring(indOfHttps + HTTPS.length());
        } else {
            inputString = inputString.substring(indOfHttp + HTTP.length());
        }
        inputString = inputString.replaceAll("\\W", "-");
        return inputString;
    }

    boolean saveData(String inputStringWebPageURL) {
        if (inputStringWebPageURL == null) {
            throw new NullPointerException();
        }
        if (URLisValid) {
            String folderName = inputStringWebPageURL;
            folderName = alignFolderName(folderName);
            File f = new File("c:\\WebAnalyzer\\" + folderName);
            f.mkdirs();
            try {
                PrintWriter outPageName = new PrintWriter(new BufferedWriter(new FileWriter("c:\\WebAnalyzer\\" + folderName + "\\Page URL.txt")));
                PrintWriter outSymbolCounter = new PrintWriter(new BufferedWriter(new FileWriter("c:\\WebAnalyzer\\" + folderName + "\\Page symbol counter.txt")));
                PrintWriter outMatchesCounter = new PrintWriter(new BufferedWriter(new FileWriter("c:\\WebAnalyzer\\" + folderName + "\\Page matches counter.txt")));
                try {
                    outPageName.println(inputStringWebPageURL);
                    outSymbolCounter.println(pageSymbolCounter + " symbols in code this page");
                    if (phraseMatch != null) {
                        outMatchesCounter.println(pageMatchesCounter + " instances of the phrase \"" + phraseMatch + "\" in code this page");
                    } else {
                        outMatchesCounter.println("No phrase to seek matches");
                    }
                } finally {
                    outPageName.close();
                    outSymbolCounter.close();
                    outMatchesCounter.close();
                    return true;
                }
            } catch (IOException e) {
                System.err.println(e);
                return false;
            }
        }
        return false;
    }

    /**
     * @return the webPageURL
     */
    public String getWebPageURL() {
        return webPageURL;
    }
}

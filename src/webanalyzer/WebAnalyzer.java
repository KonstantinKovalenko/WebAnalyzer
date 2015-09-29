package webanalyzer;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.*;

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
    private static Site site = new Site();
    private final String HTTP = "http://";
    private final String HTTPS = "https://";
    private final String HREF = "href=\"";
    private final String SLASH = "/";
    private final String QUOTES = "\"";

    public WebAnalyzer(String webPageURL) {
        this.webPageURL = webPageURL;
    }

    public WebAnalyzer(String webPageURL, String phraseMatch) {
        this.webPageURL = webPageURL;
        this.phraseMatch = phraseMatch;
    }

    boolean scanWebPage() {
        try {
            if (webPageURL == null) {
                throw new NullPointerException();
            }
            url = new URL(webPageURL);
            if (visitedInnerLinks.isEmpty()) {
                visitedInnerLinks.add(webPageURL);
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
                        pageCodeString = pageCodeString.substring(pageCodeString.indexOf(HREF) + HREF.length());
                    }
                }
                fillVisited();
                savePageDataToSiteDB(site, webPageURL, pageSymbolCounter, pageMatchesCounter, phraseMatch);
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
        exec.shutdown();
    }

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

    String returnCleanURL(String inputString) {
        if (URLisValid) {
            String resultString = inputString;
            final int indOfHttp = resultString.indexOf(HTTP);
            final int indOfHttps = resultString.indexOf(HTTPS);
            final int indOfSlash = resultString.indexOf(SLASH, (indOfHttp + HTTP.length()));
            final int indOfSlashHttps = resultString.indexOf(SLASH, (indOfHttps + HTTPS.length()));
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

    void addSimpleLink(String inputString, Collection inputCollection) {
        final int indOfHrefHttp = inputString.indexOf("href=\"http");
        final int indOfQuotes = inputString.indexOf(QUOTES, (indOfHrefHttp + HREF.length()));
        if (indOfQuotes != -1) {
            inputString = inputString.substring((indOfHrefHttp + HREF.length()), indOfQuotes);
            if (simpleCheckPCS(inputString)) {
                inputCollection.add(inputString);
            }
        }
    }

    void addComplicatedLink(String inputString, Collection inputCollection, String inputWebPageURL) {
        final char CHAR_SLASH = '/';
        final int indOfHref = inputString.indexOf(HREF);
        final int indOfQuotes = inputString.indexOf(QUOTES, (indOfHref + HREF.length()));
        if (indOfQuotes == -1) {
            return;
        }
        inputString = inputString.substring((indOfHref + HREF.length()), indOfQuotes);
        if (!simpleCheckPCS(inputString)) {
            return;
        }
        if (inputWebPageURL.equals(HTTP + returnCleanURL(inputWebPageURL))
                || inputWebPageURL.equals(HTTPS + returnCleanURL(inputWebPageURL))) {
            if (inputString.charAt(0) != CHAR_SLASH) {
                inputCollection.add(inputWebPageURL + SLASH + inputString);
            } else {
                inputCollection.add(inputWebPageURL + inputString);
            }
        } else if (inputWebPageURL.equals(HTTP + returnCleanURL(inputWebPageURL) + SLASH)
                || inputWebPageURL.equals(HTTPS + returnCleanURL(inputWebPageURL) + SLASH)) {
            if (inputString.charAt(0) != CHAR_SLASH) {
                inputCollection.add(inputWebPageURL + inputString);
            } else {
                if (inputWebPageURL.equals(HTTP + returnCleanURL(inputWebPageURL) + SLASH)) {
                    inputCollection.add(HTTP + returnCleanURL(inputWebPageURL) + inputString);
                } else {
                    inputCollection.add(HTTPS + returnCleanURL(inputWebPageURL) + inputString);
                }
            }
        } else {
            String handleWPU = inputWebPageURL;
            final int lastIndOfSlash = handleWPU.lastIndexOf(SLASH);
            if (inputString.charAt(0) != CHAR_SLASH) {
                handleWPU = handleWPU.substring(0, lastIndOfSlash + 1);
                inputCollection.add(handleWPU + inputString);
            } else {
                handleWPU = handleWPU.substring(0, lastIndOfSlash);
                inputCollection.add(handleWPU + inputString);
            }
        }
    }

    private void savePageDataToSiteDB(Site incSite, String incWebPageURL, int incPageSymbolCounter, int incPageMatchesCounter, String incPhraseMatch) {
        if (incPhraseMatch != null) {
            incSite.add(new Page(incWebPageURL, incPageSymbolCounter, incPageMatchesCounter, incPhraseMatch));
        } else {
            incSite.add(new Page(incWebPageURL, incPageSymbolCounter, incPageMatchesCounter));
        }
    }

    /**
     *
     * @param path is a path to save data on users PC.
     * <p>
     * For example, if user have to save data in folder "Result"     <pre>
     * "d:\Work\Progs\Result"
     * </pre> path must be:
     * <pre>
     * "d:\\Work\\Progs\\Result"</pre>
     */
    public void saveDataToHDD(String path) {
        SaveSiteData ssd = new SaveSiteData(site);
        ssd.saveSiteDataToHDD(path);
    }

    public String getWebPageURL() {
        return webPageURL;
    }
}

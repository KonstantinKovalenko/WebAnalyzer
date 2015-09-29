package webanalyzer;

import java.util.*;
import org.junit.*;
import static org.junit.Assert.*;

public class WebAnalyzerTest {

    @Test
    public void testScanWebPageTrue() {
        WebAnalyzer wa = new WebAnalyzer("http://www.beluys.com/html_basics/html_page.html", "html");
        assertTrue(wa.scanWebPage());
    }

    @Test
    public void testScanWebPageFalse() {
        WebAnalyzer wa = new WebAnalyzer("www.beluys.com/html_basics/html_page.html", "html");
        assertFalse(wa.scanWebPage());
    }

    @Test(expected = NullPointerException.class)
    public void testScanWebPageNull() {
        WebAnalyzer wa = new WebAnalyzer(null, "html");
        wa.scanWebPage();
    }

    @Test
    public void testScanWebSite() {
        WebAnalyzer wa = new WebAnalyzer("http://lostfilm.tv", "html");
        wa.scanWebSite();
    }

    @Test
    public void testReturnCleanURLHTTP() {
        WebAnalyzer wa = new WebAnalyzer("http://www.beluys.com/html_basics/html_page.html");
        String expected = "www.beluys.com";
        assertEquals(expected, wa.returnCleanURL("http://www.beluys.com/html_basics/html_page.html"));
    }

    @Test
    public void testReturnCleanURLHTTPS() {
        WebAnalyzer wa = new WebAnalyzer("https://www.beluys.ru/html_basics/html_page.html");
        String expected = "www.beluys.ru";
        assertEquals(expected, wa.returnCleanURL(wa.getWebPageURL()));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testReturnCleanURLIllegalArgumentEx() {
        WebAnalyzer wa = new WebAnalyzer("www.beluys.com/html_basics/html_page.html", "html");
        wa.scanWebPage();
        wa.returnCleanURL(wa.getWebPageURL());
    }

    @Test
    public void testAddSimpleLink() {
        WebAnalyzer wa = new WebAnalyzer("anyString");
        Collection testCollection = new LinkedList();
        wa.addSimpleLink("<a href:\"http?^&*$#@!any text after http and between the quotation marks\"", testCollection);
        String expected = "http?^&*$#@!any text after http and between the quotation marks";
        for (Object obj : testCollection) {
            assertEquals(expected, obj);
        }
    }

    @Test
    public void testAddComplicatedLinkWithoutSlashes() {
        WebAnalyzer wa = new WebAnalyzer("anyString");
        Collection testCollection = new LinkedList();
        wa.addComplicatedLink("<a href:\"nullsite.html\"", testCollection, "https://www.beluys.ru");
        String expected = "https://www.beluys.ru/nullsite.html";
        for (Object obj : testCollection) {
            assertEquals(expected, obj);
        }
    }

    @Test
    public void testAddComplicatedLinkWithoutSlashInURL() {
        WebAnalyzer wa = new WebAnalyzer("anyString");
        Collection testCollection = new LinkedList();
        wa.addComplicatedLink("<a href:\"/nullsite.php\"", testCollection, "https://www.beluys.ru");
        String expected = "https://www.beluys.ru/nullsite.php";
        for (Object obj : testCollection) {
            assertEquals(expected, obj);
        }
    }

    @Test
    public void testAddComplicatedLinkWithoutSlashInLink() {
        WebAnalyzer wa = new WebAnalyzer("anyString");
        Collection testCollection = new LinkedList();
        wa.addComplicatedLink("<a href:\"nullsite.xml123anytext\"", testCollection, "http://www.beluys.ru/");
        String expected = "http://www.beluys.ru/nullsite.xml123anytext";
        for (Object obj : testCollection) {
            assertEquals(expected, obj);
        }
    }

    @Test
    public void testAlignFolderName() {
        WebAnalyzer wa = new WebAnalyzer("anyString");
        String tested = wa.alignFolderName("http://lostfilm.tv/news.php?act=full&type=1&id=3979");
        String expected = "lostfilm-tv-news-php-act-full-type-1-id-3979";
        assertEquals(expected, tested);
    }

    @Test(expected = NullPointerException.class)
    public void testSaveDataNullPointerEx() {
        WebAnalyzer wa = new WebAnalyzer("anyString");
        wa.saveData(null);
    }

    @Test
    public void testSaveDataTrue() {
        WebAnalyzer wa = new WebAnalyzer("anyString");
        assertTrue(wa.saveData("http://+anyString&with?complicated^%$symbols"));
    }

    @Test
    public void testSaveDataFalse() {
        WebAnalyzer wa = new WebAnalyzer("www.beluys.com/html_basics/html_page.html", "html");
        wa.scanWebPage();
        assertFalse(wa.saveData(wa.getWebPageURL()));
    }
}

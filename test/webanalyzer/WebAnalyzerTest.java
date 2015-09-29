package webanalyzer;

import java.io.File;
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
    public void testReturnCleanURLHTTP() {
        WebAnalyzer wa = new WebAnalyzer("http://www.beluys.com/html_basics/html_page.html");
        String expected = "www.beluys.com";
        assertEquals(expected, wa.returnCleanURL(wa.getWebPageURL()));
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
    public void testSaveDataToHDD() {
        WebAnalyzer wa = new WebAnalyzer("http://www.beluys.com/html_basics/html_page.html");
        wa.scanWebSite();
        wa.saveDataToHDD("d:\\123asd");
        File f = new File("d:\\123asd\\www-beluys-com-html_basics-html_page-html");
        assertTrue(f.isDirectory());
    }
}

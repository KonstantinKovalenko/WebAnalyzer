package webanalyzer;

import java.util.*;

public class Site {

    private final List<Page> siteDB;

    Site() {
        siteDB = new ArrayList();
    }

    void add(Page page) {
        siteDB.add(page);
    }

    void clear() {
        siteDB.clear();
    }

    int size() {
        return siteDB.size();
    }

    List getSiteDataBase() {
        return siteDB;
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();
        for (Page page : siteDB) {
            result.append(page.toString() + "\n\n");
        }
        return result.toString();
    }
}

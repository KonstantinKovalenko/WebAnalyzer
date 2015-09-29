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

    /**
     * Compares the specified object with this DataBase for equality. Returns
     * <tt>true</tt> if and only if the specified object is an Arraylist, both
     * lists have the same size, and all corresponding pairs of elements in the
     * two lists are <i>equal</i>. (Two elements <tt>e1</tt> and
     * <tt>e2</tt> are <i>equal</i> if <tt>(e1==null ? e2==null :
     * e1.equals(e2))</tt>.) In other words, two lists are defined to be equal
     * if they contain the same elements in the same order. This definition
     * ensures that the equals method works properly across different
     * implementations of the <tt>List</tt> interface.
     *
     * @param o the object to be compared for equality with this list
     * @return <tt>true</tt> if the specified object is equal to this list
     */
    @Override
    public boolean equals(Object o) {
        if (o.getClass() != siteDB.getClass()) {
            return false;
        }
        ArrayList aList = (ArrayList) o;
        Object[] incArray = aList.toArray();
        Object[] myArray = siteDB.toArray();
        if (myArray.length != incArray.length) {
            return false;
        }
        for (int i = 0; i < myArray.length; i++) {
            if (myArray[i].hashCode() != incArray[i].hashCode()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 37 * hash + Objects.hashCode(this.siteDB);
        return hash;
    }
}

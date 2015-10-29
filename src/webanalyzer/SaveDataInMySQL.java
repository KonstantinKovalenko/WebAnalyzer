package webanalyzer;

import java.sql.*;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;

public class SaveDataInMySQL implements SaveSiteData {

    private final String mainPage;
    private static ExceptionHandler exceptionHandler;

    SaveDataInMySQL(String mainPage) {
        this.mainPage = mainPage;
        createExceptionHandler();
    }

    private void createExceptionHandler() {
        WebSiteAnalyzer wsa = new WebSiteAnalyzer("");
        exceptionHandler = wsa.getExceptionHandler();
    }

    @Override
    public void saveData(Site site) {
        addDataToMainTable();
        addDataToSecondaryTable(site);
    }

    private void addDataToMainTable() {
        Connection con = null;
        final Calendar c = new GregorianCalendar();
        final String scanDate = Integer.toString(c.get(Calendar.YEAR))
                + Integer.toString(c.get(Calendar.MONTH) + 1)
                + Integer.toString(c.get(Calendar.DAY_OF_MONTH));
        final String sSQL = "insert into sites (MainPage,ScanDate) values ('" + mainPage + "'," + Integer.parseInt(scanDate) + ")";
        try {
            con = openConnection();
            Statement statement = con.createStatement();
            statement.execute(sSQL);
        } catch (SQLException e) {
            exceptionHandler.handleException(e);
        } finally {
            closeConnection(con);
        }
    }

    private void addDataToSecondaryTable(Site site) {
        Connection con = null;
        List<Page> siteDB = site.getSiteDataBase();
        try {
            con = openConnection();
            for (Page page : siteDB) {
                String sSQL = "insert into data (ID_Sites, PageURL, PhraseMatch,MatchesCounter,SymbolCounter) values "
                        + "((select max(id) from sites),"
                        + "'" + page.getPageName() + "',"
                        + "'" + page.getPhraseMatch() + "',"
                        + page.getPageMatchesCounter() + ","
                        + page.getPageSymbolCounter() + ")";
                Statement statement = con.createStatement();
                statement.execute(sSQL);
            }
        } catch (SQLException e) {
            exceptionHandler.handleException(e);
        } finally {
            closeConnection(con);
        }
    }

    private Connection openConnection() throws SQLException {
        final String host = "localhost";
        final String port = "3306";
        final String dbName = "WebSiteAnalyzerDB";
        final String userName = "root";
        final String password = "gosuprotoss";
        final String dbURL = "jdbc:mysql://" + host + ":" + port + "/" + dbName;
        return DriverManager.getConnection(dbURL, userName, password);
    }

    private void closeConnection(Connection con) {
        try {
            con.close();
        } catch (SQLException e) {
            System.err.println("Cannot close connection: " + e.getMessage());
        }
    }

    public static void main(String[] args) throws SQLException {
        SaveDataInMySQL sss = new SaveDataInMySQL("asdsdfasdf");
        sss.addDataToMainTable();

    }
}

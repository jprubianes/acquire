package tests;

import com.friskysoft.framework.Browser;
import org.openqa.selenium.remote.BrowserType;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;

public class BaseTestScript {

    protected static Browser driver;
    protected static String user = "cajiwihe@dropjar.com";
    protected static String password = "Hereisanew1!";
    protected static String noDepartmentsTitle = "No Departments...Yet";

    @BeforeClass
    public void setup() {
        driver = Browser.newLocalDriver(BrowserType.CHROME).moveToCenter();
    }

    @AfterClass
    public void teardown() {

        // Save a screenshot before shutdown
        driver.takeScreenshot();

        // Shutdown webdriver
        driver.destroy();
    }
}

package tests;

import com.friskysoft.framework.Browser;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.remote.BrowserType;
import org.testng.Assert;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;
import pages.Departments;
import pages.LoginPage;

import java.time.Instant;
import java.util.List;

public class PartnerTest extends BaseTestScript {

    Departments departments = new Departments();
    LoginPage loginPage = new LoginPage();

    @Test
    public void addDepartmentForNewUsers() {
        String departmentName = "Test" + Instant.now().toEpochMilli();
        login();
        navigateToDepartmentSettings();
        driver.takeScreenshot(true);

        //validate that message for a new user without departments is display
        departments.emptyDepartmentTitle.waitToBePresent().assertTextContainsString(noDepartmentsTitle);

        //add department process and validate is being created and added to the department table
        departments.createNewDepartmentEmptyData.waitToBeClickable().click();
        addDepartmentModal(departmentName);
        departments.departmentNameTableColumn.waitToBePresent().assertTextContainsString(departmentName);
    }

    @Test(dependsOnMethods = "addDepartmentForNewUsers" )
    public void addDepartmentForExistingData() {
        driver = Browser.newLocalDriver(BrowserType.CHROME).moveToCenter();
        String departmentName = "Test" + Instant.now().toEpochMilli();
        login();
        navigateToDepartmentSettings();

        //add department process and validate is being created and added to the department table
        departments.addDepartmentButtonExistingData.waitToBeClickable().click();
        addDepartmentModal(departmentName);
        List<WebElement> webElements = departments.departmentNameTableColumn.getWebElements();
        String expectedName =  webElements.get(0).getText();
        Assert.assertEquals(expectedName, departmentName); //validate that the department was created correctly and added to the table
        Assert.assertEquals(webElements.size(), 2); //validate that there are 2 Departments now
        deleteDepartmentFromTable(1);
        deleteDepartmentFromTable(0);
    }

    //for now can be used to delete one item at the time in case an specific item wants to be deleted
    private void deleteDepartmentFromTable(int index) {
        List<WebElement> webElements = departments.popoverMenuDepartmentOption.getWebElements();
        webElements.get(index).click();
        departments.popoverDelete.waitToBeClickable().click();
        departments.deleteButtonConfirmation.waitToBeClickable().click();
    }

    private void addDepartmentModal(String departmentName) {
        departments.inputDepartmentNameText.waitToBePresent().sendKeys(departmentName);
        departments.saveButton.waitToBeClickable().click();

    }


    public void login() {
        //login into the acquire app
        driver.open("https://accounts.uat.env.acquire.io/login?endpoint=account&return=&account=lo4h4a");
        driver.takeScreenshot();
        loginPage.emailTextBox.waitToBePresent().sendKeys(user);
        loginPage.passwordTextBox.waitToBePresent().sendKeys(password);
        loginPage.loginButton.waitToBeClickable().click();
        Browser.sleep(3000);

    }

    private void navigateToDepartmentSettings() {
        //Navigate to department Settings
        departments.settings.waitToBeClickable().click();
        departments.accountSettings.waitToBeClickable().click();
        departments.departments.waitToBeClickable().click();
    }

}

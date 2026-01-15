import org.openqa.selenium.By;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.firefox.FirefoxDriver;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

import org.apache.commons.io.FileUtils;

public class CasDriver {

    WebDriver webDriver;
    int screenshotCounter = 0;
    boolean doTakeSnapshots = false;
    protected HashMap<String, HashMap<String, String>> connectionParametersByProfile;

    public CasDriver(WebDriver driver) {
        this.webDriver = driver;
    }

    public CasDriver(WebDriver driver, HashMap<String, HashMap<String, String>> connectionParametersByProfile) {
        this.webDriver = driver;
        this.connectionParametersByProfile = connectionParametersByProfile;
    }

    public WebDriver getWebDriver() {
        return webDriver;
    }

    public void fillFormAndSubmit(String login, String password) {
        webDriver.findElement(By.id("username")).sendKeys(login);
        webDriver.findElement(By.id("password")).sendKeys(password);

        // Va dépendre de l'environnement
        webDriver.findElement(By.cssSelector("[type='submit']")).click();
        // alternative possible (selon environnement)
        // this.driver.findElement(By.id("submitBtn")).click();

        takeFullPageScreenshot("fillFormAndSubmit");
    }

    public void fillFormAndSubmitUsingProfile(String profileId) {
        HashMap<String, String> connectionParameter = connectionParametersByProfile.get(profileId);
        fillFormAndSubmit(connectionParameter.get("login"), connectionParameter.get("password"));
    }

    public void takeFullPageScreenshot(String title) {
        if (!doTakeSnapshots) {
            return;
        }

        screenshotCounter++;
        File srcFile = ((FirefoxDriver)webDriver).getFullPageScreenshotAs(OutputType.FILE);
        try {
            FileUtils.copyFile(srcFile, new File("/tmp/screenshots.cas."+screenshotCounter+(null!=title?"."+title:"")+".png"));
        } catch (IOException e) {
            System.out.println(e);
        }
    }
}

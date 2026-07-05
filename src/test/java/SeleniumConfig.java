import java.util.concurrent.TimeUnit;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.firefox.FirefoxProfile;

public class SeleniumConfig {

    private WebDriver driver;

    public SeleniumConfig() {
        FirefoxOptions browserOptions = new FirefoxOptions();

        FirefoxProfile profile = new FirefoxProfile();
        profile.setPreference("network.proxy.type", 0);
        browserOptions.setProfile(profile);

        browserOptions.addArguments("--width=1920");
        browserOptions.addArguments("--height=1080");

        driver = new FirefoxDriver(browserOptions);
        driver.manage().timeouts().implicitlyWait(5, TimeUnit.SECONDS);
    }

    public WebDriver getDriver()
    {
        return driver;
    }

    static {
        System.setProperty("webdriver.gecko.driver", "/usr/local/geckodriver-0.36.0/geckodriver");
    }
}
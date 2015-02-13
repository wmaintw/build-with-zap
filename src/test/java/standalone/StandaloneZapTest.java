package standalone;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openqa.selenium.Proxy;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.remote.CapabilityType;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.zaproxy.clientapi.core.ClientApi;
import zap.ZapUtils;

import java.io.IOException;

import static java.lang.String.format;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.openqa.selenium.By.*;

public class StandaloneZapTest {

    public static final String WEB_APP = "http://localhost:8080/WebGoat";
    public static final String HTTP_PROXY = "localhost:7070";
    public static final String API_KEY = "123456";
    public static final String ZAP_INSTALL_PATH = "/Users/wma/Downloads/security/zap-for-linux/ZAP_2.3.1/zap.sh";
    public static final String ZAP_HOST = "localhost";
    public static final int ZAP_PORT = 7070;
    private WebDriver driver;
    private ClientApi zapClient;

    @Before
    public void setUp() throws Exception {
        launchZapServer();
        zapClient = new ClientApi(ZAP_HOST, ZAP_PORT);

        driver = setupDriverProxy();

        waitUntilZapStarted(60);
    }

    @Test
    public void shouldDisplaySearchResult() throws Exception {
        driver.get(WEB_APP);
        driver.findElement(name("username")).sendKeys("guest");
        driver.findElement(name("password")).sendKeys("guest");
        driver.findElement(name("loginForm")).submit();

        waitUntilLoggedIn(5);

        Thread.sleep(3 * 1000);

        int numberOfAlerts = ZapUtils.getInteger(zapClient.core.numberOfAlerts(WEB_APP));
        assertThat(numberOfAlerts, is(0));
    }

    @After
    public void tearDown() throws Exception {
        driver.quit();
        zapClient.core.shutdown(API_KEY);
    }

    private Process launchZapServer() throws IOException {
        ProcessBuilder builder = new ProcessBuilder(ZAP_INSTALL_PATH, "-config", format("api.key=%s", API_KEY));
        return builder.start();
    }

    private WebDriver setupDriverProxy() {
        Proxy proxy = new Proxy();
        proxy.setHttpProxy(HTTP_PROXY).setFtpProxy(HTTP_PROXY).setSocksProxy(HTTP_PROXY);
        DesiredCapabilities capabilities = new DesiredCapabilities();
        capabilities.setCapability(CapabilityType.PROXY, proxy);

        return driver = new FirefoxDriver(capabilities);
    }

    private void waitUntilZapStarted(int timeOutInSeconds) {
        new WebDriverWait(driver, timeOutInSeconds).until((WebDriver input) -> {
            input.get("http://zap/");
            return input.getTitle().equalsIgnoreCase("ZAP API UI");
        });
    }

    private void waitUntilLoggedIn(int timeOutInSeconds) {
        new WebDriverWait(driver, timeOutInSeconds).until(new ExpectedCondition<Boolean>() {
            @Override
            public Boolean apply(WebDriver input) {
                return input.getTitle().equalsIgnoreCase("WebGoat");
            }
        });
    }
}

# BUILD WITH ZAP

The idea is to integrate ZAP Porxy into CI server, let it scan or analysis security vulnerabilities while running regular functional test.

### Quick Start

You just need 2 steps to benefit from ZAP Proxy.

**step 1, Setup Proxy Setting for WebDriver**

```java
public WebDriver setupDriverProxy() {
    Proxy proxy = new Proxy();
    proxy.setHttpProxy(HTTP_PROXY)
        .setFtpProxy(HTTP_PROXY)
        .setSocksProxy(HTTP_PROXY);
    DesiredCapabilities capabilities = new DesiredCapabilities();
    capabilities.setCapability(CapabilityType.PROXY, proxy);

    return driver = new FirefoxDriver(capabilities);
}
```

**step 2 Run Gradle Task**

```sh
$ gradle buildWithZap stopZap
```

The `buildWithZap` task will launch ZAP proxy first and then run functional tests, after that a report will be generated. At the end, ZAP Proxy will be shutdown by task `stopZap`.

### Report
When functional tests finished, a report will be generated under **zap-report** folder. There are 2 report files:

* zap-alerts-{timestamp}.html
* zap-alerts-{timestamp}.json
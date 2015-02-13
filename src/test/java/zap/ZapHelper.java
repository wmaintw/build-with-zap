package zap;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.tools.generic.EscapeTool;
import org.zaproxy.clientapi.core.Alert;
import org.zaproxy.clientapi.core.Alert.Risk;
import org.zaproxy.clientapi.core.ClientApi;
import org.zaproxy.clientapi.core.ClientApiException;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.*;

import static java.lang.Integer.valueOf;
import static java.lang.String.format;
import static org.apache.commons.io.FileUtils.deleteQuietly;
import static org.apache.commons.io.FileUtils.writeStringToFile;
import static org.zaproxy.clientapi.core.Alert.Risk.*;

public class ZapHelper {
    public static final String ZAP_ALERTS_DATA_FILE = "zap-alerts-%s.json";
    public static final String ZAP_ALERTS_REPORT_FILE = "zap-alerts-%s.html";
    public static final String ZAP_ALERT_REPORT_TEMPLATE = "alertReportTemplate.vm";
    private static ClientApi zapClient;

    public static void main(String[] args) throws IOException, ClientApiException, URISyntaxException {

        String action = args[0];

        switch (action) {
            case "start":
                String zapInstallPath = args[1];
                String apiKey = args[2];

                launchZapServer(zapInstallPath, apiKey);
                break;
            case "stop":
                stopZapServer(args[1], valueOf(args[2]), args[3]);
                break;
            case "report":
                generateReport(args[1], valueOf(args[2]), args[3]);
                break;
            default:
                System.out.println("can't understand the action: " + action);
        }
    }

    private static void generateReport(String host, int port, String website) throws ClientApiException, IOException {
        zapClient = new ClientApi(host, port);
        int numberOfAlerts = ZapUtils.getInteger(zapClient.core.numberOfAlerts(""));
        List<Alert> zapAlerts = zapClient.getAlerts(website, 0, numberOfAlerts);
        Date reportDate = new Date();

        File zapReportDir = getZapReportDir();
        generateZapAlertsDataFile(zapReportDir, reportDate, zapAlerts);
        generateZapAlertsReportFile(zapReportDir, reportDate, zapAlerts, website);

        promptReportFileLocation(reportDate, zapReportDir);
    }

    private static void promptReportFileLocation(Date reportDate, File zapReportDir) {
        String zapReportFilename = format(ZAP_ALERTS_REPORT_FILE, reportDate.getTime());
        System.out.println(format("ZAP report generated at: %s/%s", zapReportDir, zapReportFilename));
    }

    private static File getZapReportDir() {
        String currentWorkingDir = Paths.get("").toAbsolutePath().toString();
        return new File(format("%s/zap-reports", currentWorkingDir));
    }

    private static void generateZapAlertsDataFile(File zapReportDir, Date reportDate, List<Alert> allAlerts) throws IOException, ClientApiException {
        File zapAlertsData = new File(zapReportDir, format(ZAP_ALERTS_DATA_FILE, reportDate.getTime()));
        deleteQuietly(zapAlertsData);
        writeStringToFile(zapAlertsData, getAlertsInJson(allAlerts));
    }

    private static void generateZapAlertsReportFile(File zapReportDir, Date reportDate, List<Alert> alerts, String website) throws IOException {
        File zapAlertsReport = new File(zapReportDir, format(ZAP_ALERTS_REPORT_FILE, reportDate.getTime()));
        deleteQuietly(zapAlertsReport);
        writeStringToFile(zapAlertsReport, buildReportContent(alerts, website, reportDate));
    }

    private static String getAlertsInJson(List<Alert> allAlerts) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.writeValueAsString(allAlerts);
    }

    private static String buildReportContent(List<Alert> alerts, String website, Date reportDate) {
        VelocityContext context = prepareReportContext(alerts, website, reportDate);

        StringWriter writer = new StringWriter();

        Template reportTemplate = getVelocityTemplate();
        reportTemplate.merge(context, writer);

        return writer.toString();
    }

    private static VelocityContext prepareReportContext(List<Alert> alerts, String website, Date reportDate) {
        VelocityContext context = new VelocityContext();
        EscapeTool escapeTool = new EscapeTool();
        context.put("esc", escapeTool);
        context.put("targetWebsite", website);
        context.put("zapRunDate", reportDate);
        context.put("numberOfAlerts", alerts.size());

        HashMap<Risk, List<Alert>> alertsBySeverity = groupAlertsBySeverity(alerts);
        context.put("numberOfHighAlerts", alertsBySeverity.get(High).size());
        context.put("numberOfMediumAlerts", alertsBySeverity.get(Medium).size());
        context.put("numberOfLowAlerts", alertsBySeverity.get(Low).size());
        context.put("numberOfInformationalAlerts", alertsBySeverity.get(Informational).size());

        ArrayList orderedAlerts = new ArrayList();
        orderedAlerts.addAll(alertsBySeverity.get(High));
        orderedAlerts.addAll(alertsBySeverity.get(Medium));
        orderedAlerts.addAll(alertsBySeverity.get(Low));
        orderedAlerts.addAll(alertsBySeverity.get(Informational));
        context.put("alerts", orderedAlerts);

        return context;
    }

    private static HashMap<Risk, List<Alert>> groupAlertsBySeverity(List<Alert> alerts) {
        HashMap<Risk, List<Alert>> alertsBySeverity = new HashMap<>();
        alertsBySeverity.put(High, new ArrayList<>());
        alertsBySeverity.put(Medium, new ArrayList<>());
        alertsBySeverity.put(Low, new ArrayList<>());
        alertsBySeverity.put(Informational, new ArrayList<>());

        for (Alert alert : alerts) {
            alertsBySeverity.get(alert.getRisk()).add(alert);
        }

        return alertsBySeverity;
    }

    private static Template getVelocityTemplate() {
        Properties properties = new Properties();
        properties.setProperty("resource.loader", "class");
        properties.setProperty("class.resource.loader.class", "org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader");
        VelocityEngine engine = new VelocityEngine(properties);

        return engine.getTemplate(ZAP_ALERT_REPORT_TEMPLATE);
    }

    private static void stopZapServer(String host, int port, String apiKey) throws ClientApiException {
        zapClient = new ClientApi(host, port);
        zapClient.core.shutdown(apiKey);
        System.out.println("shutdown ZAP single sent");
    }

    private static void launchZapServer(String zapInstallPath, String apiKey) throws IOException {
        ProcessBuilder builder = new ProcessBuilder(zapInstallPath, "-config", format("api.key=%s", apiKey));
        builder.start();
    }
}

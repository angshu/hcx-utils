package in.projecteka.data;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Application {
    private static final List<String> supportedTypes = Arrays.asList("PR", "DR");
    private static final Map<String, DocumentGenerator> generators = new HashMap<>() {{
        put("PR", new PrescriptionGenerator());
        put("DR", new DiagnosticReportGenerator());
    }};

    public static void main(String[] args) throws Exception {
        Path location = getOutFileLocation(checkRequired("out"));
        String type = getDocumentType(checkRequired("type"));
        if (Utils.isBlank(type)) {
            System.out.println("Type is blank. Can not generate document");
            return;
        }
        String patientName = getPatientName(checkRequired("name"));
        Date fromDate = getFromDate(checkRequired("fromDate"));
        int number = getNumerOfInstances(checkRequired("number"));
        DocumentGenerator documentGenerator = generators.get(type);
        documentGenerator.init();
        try {
            documentGenerator.execute(patientName, fromDate, number, location);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static String getPatientName(String name) {
        if (Utils.isBlank(name)) {
            System.out.println("name not provided. Defaulting to navjot");
            return "navjot";
        }
        return name;
    }

    private static Path getOutFileLocation(String out) {
        if (Utils.isBlank(out)) {
            System.out.println("out path not provided. Defaulting to /tmp");
            return Paths.get("/tmp/test");
        }
        return Paths.get(out);
    }

    private static String getDocumentType(String type) {
        if (!Utils.isBlank(type)) {
            if (supportedTypes.contains(type.toUpperCase())) {
                return type.toUpperCase();
            }
        }
        System.out.println("Please provide Type, possible values: PR, DR");
        return null;
    }

    private static int getNumerOfInstances(String number) {
        if (Utils.isBlank(number)) {
            System.out.println("number is blank. Defaulting to 1.");
            return 1;
        }
        return Integer.parseInt(number);
    }

    private static Date getFromDate(String fromDate) throws ParseException {
        if (Utils.isBlank(fromDate)) {
            System.out.println("fromDate is blank. Defaulting to today's date.");
            return new Date();
        }
        Date date = new SimpleDateFormat("yyyy-MM-dd").parse(fromDate);
        return date;
    }

    private static String checkRequired(String name) throws Exception {
        String property = System.getProperty(name);
        if (Utils.isBlank(property)) {
            System.out.println(String.format("Required property [%s] not set.", name));
        }
        return property;
    }
}

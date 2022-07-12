package in.projecteka.utils;

import in.projecteka.utils.data.DiagnosticReportGenerator;
import in.projecteka.utils.data.DischargeSummaryGenerator;
import in.projecteka.utils.common.DocumentGenerator;
import in.projecteka.utils.data.HealthDocumentRecordGenerator;
import in.projecteka.utils.data.ImmunizationGenerator;
import in.projecteka.utils.data.OPConsultationGenerator;
import in.projecteka.utils.data.PrescriptionGenerator;
import in.projecteka.utils.common.Utils;
import in.projecteka.utils.data.WellnessRecordGenerator;
import in.projecteka.utils.hcx.HcxCoverageEligibilityRequestGenerator;
import in.projecteka.utils.hcx.HcxCoverageEligibilityResponseGenerator;
import in.projecteka.utils.hcx.HcxValueSet;

import java.nio.file.Paths;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class Application {
    private static final Map<String, DocumentGenerator> generators = new HashMap<>() {{
        put("PR", new PrescriptionGenerator());
        put("DR", new DiagnosticReportGenerator());
        put("OP", new OPConsultationGenerator());
        put("DS", new DischargeSummaryGenerator());
        put("IR", new ImmunizationGenerator());
        put("HD", new HealthDocumentRecordGenerator());
        put("WR", new WellnessRecordGenerator());
        put("CEREQ", new HcxCoverageEligibilityRequestGenerator());
        put("CERES", new HcxCoverageEligibilityResponseGenerator());
        put("VS", new HcxValueSet());
    }};
    private static final List<String> supportedTypes = Arrays.asList("PR", "DR", "OP", "DS", "IR", "HD", "WR", "CEREQ", "VS");

    public static void main(String[] args) throws Exception {
        String type = getDocumentType(checkRequired("type"));
        if (Utils.isBlank(type)) {
            System.out.println("Please provide Type, possible values: PR, DR, OP, DS, IR, HD, WR, CEREQ");
            return;
        }
        DocRequest request =
                DocRequest.builder()
                        .type(type)
                        .patientId(checkOptional("id").orElse(null))
                        .patientName(checkOptional("name").orElseGet(Application::defaultPatient))
                        .provName(checkOptional("hip").orElseGet(Application::defaultHip))
                        .fromDate(getFromDate(checkOptional("fromDate")))
                        .number(Integer.valueOf(checkOptional("number").orElseGet(Application::defaultInstanceNumber)))
                        .outPath(Paths.get(checkOptional("out").orElseGet(Application::defaultOutputLocation)))
                        .csvPath(Paths.get(checkRequired("csv").orElseThrow(() -> new RuntimeException("Can not run generator"))))
                        .build();
        DocumentGenerator documentGenerator = generators.get(type);
        documentGenerator.init();
        try {
            documentGenerator.execute(request);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    private static String defaultHip() {
        System.out.println("Defaulting Provider *hip* to max");
        return "max";
    }

    private static String defaultPatient() {
        System.out.println("Defaulting Patient *name* to navjot");
        return "navjot";
    }

    private static String defaultOutputLocation() {
        System.out.println("Defaulting path *out* to /tmp");
        return "/tmp";
    }

    private static String defaultInstanceNumber() {
        System.out.println("Defaulting instances *number* to 1");
        return "1";
    }

    private static String getDocumentType(Optional<String> type) {
        return supportedTypes.contains(type.get().toUpperCase()) ? type.get().toUpperCase() : null;
    }

    private static Date getFromDate(Optional<String> fromDate) throws ParseException {
        if (fromDate.isEmpty()) {
            System.out.println("*fromDate* is blank. Defaulting to today's date.");
            return new Date();
        }

        if (Utils.isBlank(fromDate.get())) {
            System.out.println("*fromDate* is blank. Defaulting to today's date.");
            return new Date();
        }
        Date date = new SimpleDateFormat("yyyy-MM-dd").parse(fromDate.get());
        return date;
    }

    private static Optional<String> checkRequired(String name) throws Exception {
        String property = System.getProperty(name);
        if (Utils.isBlank(property)) {
            System.out.println(String.format("Required property [%s] not set.", name));
        }
        return Optional.ofNullable(property);
    }

    private static Optional<String> checkOptional(String name) throws Exception {
        return Optional.ofNullable(System.getProperty(name));
    }
}

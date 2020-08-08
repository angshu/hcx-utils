package in.projecteka.utils.data.model;

import in.projecteka.utils.data.Utils;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.HashMap;
import java.util.Map;

@Data
@AllArgsConstructor
@Builder
public class SimpleCondition {
    private String text;
    private String code;
    private String severity;
    private String severityCode;
    private String category;
    private String categoryCode;
    private String clinicalStatus;



    private static Map<String, SimpleCondition> tests = new HashMap<>() {{
        put("1", new SimpleCondition("Fever", "386661006", "Mild", "", "problem list", "problem-list-item", "active"));
        put("2", new SimpleCondition("Toothache", "", "Severe", "24484000", "problem list", "problem-list-item", "active"));
        put("3", new SimpleCondition("Dry cough", "", "Mild", "", "problem list", "problem-list-item", "active"));
        put("4", new SimpleCondition("Migrane", "", "Mild to moderate", "371923003", "Encounter Diagnosis", "encounter-diagnosis", "recurrence"));
        put("5", new SimpleCondition("Burn of ear", "39065001", "Severe", "24484000", "Encounter Diagnosis", "encounter-diagnosis", "active"));
        put("6", new SimpleCondition("Asthma", "", "Mild", "", "problem list", "problem-list-item", "active"));
        put("7", new SimpleCondition("Bacterial infectious disease", "87628006", "", "", "Encounter Diagnosis", "encounter-diagnosis", "active"));
        put("8", new SimpleCondition("Retropharyngeal abscess", "18099001", "Mild to moderate", "371923003", "Encounter Diagnosis", "encounter-diagnosis", "relapse"));
    }};

    private static Map<String, SimpleCondition> diagnosesForHospitalization = new HashMap<>() {{
        put("1", new SimpleCondition("Bacterial infection due to Bacillus", "128944008", "Mild", "", "Encounter Diagnosis", "encounter-diagnosis", "recurrence"));
        put("2", new SimpleCondition("Acute gastritis", "25458004", "Severe", "24484000",  "Encounter Diagnosis", "encounter-diagnosis", "recurrence"));
        put("3", new SimpleCondition("Fracture of ankle", "16114001", "Severe", "24484000",  "Encounter Diagnosis", "encounter-diagnosis", "recurrence"));
    }};

    public static SimpleCondition getRandomCondition() {
        int index = Utils.randomInt(1, tests.size());
        return tests.get(String.valueOf(index));
    }

    public static SimpleCondition getRandomComplaint() {
        int index = Utils.randomInt(1, 4);
        return tests.get(String.valueOf(index));
    }

    public static SimpleCondition getRandomHospitalizationReason() {
        return diagnosesForHospitalization.get(String.valueOf(Utils.randomInt(1, diagnosesForHospitalization.size())));
    }
}

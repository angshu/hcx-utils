package in.projecteka.data.model;

import in.projecteka.data.Utils;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.HashMap;
import java.util.Map;

@Data
@AllArgsConstructor
public class DiagnosticTest {
    private String abbr;
    private String display;
    private String code;

    private static Map<String, DiagnosticTest> tests = new HashMap<>() {{
        put("1", new DiagnosticTest("CBC", "Complete Blood Count", "58410-2"));
        put("2", new DiagnosticTest("Hemoglobin", "Hemoglobin [Mass/volume] in Blood", "718-7"));
        put("3", new DiagnosticTest("WBC", "Leukocytes [#/volume] in Blood by Automated count", "6690-2"));
    }};

    public static DiagnosticTest getRandomTest() {
        int index = Utils.randomInt(1, tests.size());
        return tests.get(String.valueOf(index));
    }

}

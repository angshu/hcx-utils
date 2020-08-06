package in.projecteka.utils.data.model;

import in.projecteka.utils.data.Utils;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.HashMap;
import java.util.Map;

@Data
@AllArgsConstructor
public class SimpleDiagnosticTest {
    private String abbr;
    private String display;
    private String code;

    private static Map<String, SimpleDiagnosticTest> tests = new HashMap<>() {{
        put("1", new SimpleDiagnosticTest("CBC", "Complete Blood Count", "58410-2"));
        put("2", new SimpleDiagnosticTest("Hemoglobin", "Hemoglobin [Mass/volume] in Blood", "718-7"));
        put("3", new SimpleDiagnosticTest("WBC", "Leukocytes [#/volume] in Blood by Automated count", "6690-2"));
    }};

    public static SimpleDiagnosticTest getRandomTest() {
        int index = Utils.randomInt(1, tests.size());
        return tests.get(String.valueOf(index));
    }

}

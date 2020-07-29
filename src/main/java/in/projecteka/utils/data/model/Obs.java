package in.projecteka.utils.data.model;

import in.projecteka.utils.data.Utils;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.HashMap;
import java.util.Map;

@Data
@AllArgsConstructor
public class Obs {
    private static final String cholesterol = "{\n" +
            "  \"resourceType\": \"Observation\",\n" +
            "  \"id\": \"cholesterol\",\n" +
            "  \"status\": \"final\",\n" +
            "  \"code\": {\n" +
            "    \"text\": \"Cholesterol\"\n" +
            "  },\n" +
            "  \"valueString\": \"6.3mmol/L\"\n" +
            "}";
    private static final String triglyceride = "{\n" +
            "  \"resourceType\": \"Observation\",\n" +
            "  \"id\": \"triglyceride\",\n" +
            "  \"status\": \"final\",\n" +
            "  \"code\": {\n" +
            "    \"text\": \"Triglyceride\"\n" +
            "  },\n" +
            "  \"valueString\": \"1.3 mmol/L\"\n" +
            "}";
    private static final String temperature = "{\n" +
            "  \"resourceType\": \"Observation\",\n" +
            "  \"id\": \"2IPDObs1\",\n" +
            "  \"status\": \"final\",\n" +
            "  \"code\": {\n" +
            "    \"text\": \"Temperature\"\n" +
            "  },\n" +
            "  \"valueQuantity\": {\n" +
            "    \"value\": 99.5,\n" +
            "    \"unit\": \"C\"\n" +
            "  }\n" +
            "}";

    private static Map<String, String> obsList = new HashMap<>() {{
       put("1", cholesterol);
       put("2", triglyceride);
       put("3", temperature);
    }};

    public static String getObservationResString() {
        int index = Utils.randomInt(1, obsList.size());
        return obsList.get(String.valueOf(index));
    }
}

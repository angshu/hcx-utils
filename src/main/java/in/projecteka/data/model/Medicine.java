package in.projecteka.data.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class Medicine {
    String code;
    String name;
    String condition;
    String instruction;
    String notes;

    public static Medicine parse(String details) {
        String[] parts = details.split(",");
        String notes = null;
        if (parts.length == 5) {
            notes = parts[4].trim();
        }
        return new Medicine(parts[0].trim(), parts[1].trim(), parts[2].trim(), parts[3].trim(), notes);
    }
}

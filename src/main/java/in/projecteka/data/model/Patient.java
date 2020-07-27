package in.projecteka.data.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class Patient {
    String name;
    String gender;
    String hid;
    String cmId;
    String hospitalId;

    public static Patient parse(String details) {
        String[] parts = details.split(",");
        return new Patient(parts[0].trim(), parts[1].trim(), parts[2].trim(), parts[3].trim(), parts[4].trim());
    }
}

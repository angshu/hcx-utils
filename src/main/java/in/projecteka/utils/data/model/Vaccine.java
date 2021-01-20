package in.projecteka.utils.data.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class Vaccine {
    String code;
    String name;

    public static Vaccine parse(String details) {
        String[] parts = details.split(",");
        return new Vaccine(parts[0].trim(), parts[1].trim());
    }

}

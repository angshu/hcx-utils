package in.projecteka.data.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.hl7.fhir.r4.model.Practitioner;

@Data
@AllArgsConstructor
public class Doctor {
    String name;
    String docId;
    String prefix;
    String suffix;

    public static Doctor parse(String details) {
        String[] parts = details.split(",");
        return new Doctor(parts[0].trim(), parts[1].trim(), parts[2].trim(), parts[3].trim());
    }

    public static String getDisplay(Practitioner author) {
        return String.format("%s %s", author.getNameFirstRep().getPrefixAsSingleString(), author.getNameFirstRep().getText());
    }
}

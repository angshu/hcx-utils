package in.projecteka.data;

import java.nio.file.Path;
import java.util.Date;

public interface DocumentGenerator {
    void init() throws Exception;
    void execute(String patientName, Date fromDate, int number, Path location) throws Exception;
}

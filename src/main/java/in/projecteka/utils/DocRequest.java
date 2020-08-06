package in.projecteka.utils;

import lombok.Builder;
import lombok.Getter;

import java.nio.file.Path;
import java.util.Date;

@Getter
@Builder
public class DocRequest {
    private String patientName;
    private String patientId;
    private Date fromDate;
    private int number;
    private String hipPrefix;
    private Path outPath;
}

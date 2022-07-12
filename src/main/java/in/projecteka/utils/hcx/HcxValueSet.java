package in.projecteka.utils.hcx;

import ca.uhn.fhir.context.FhirContext;
import com.opencsv.CSVParser;
import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import in.projecteka.utils.DocRequest;
import in.projecteka.utils.common.DocumentGenerator;
import in.projecteka.utils.common.Utils;
import org.hl7.fhir.r4.model.Enumerations;
import org.hl7.fhir.r4.model.ValueSet;

import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;

public class HcxValueSet implements DocumentGenerator {
    @Override
    public void init() throws Exception {

    }

    @Override
    public void execute(DocRequest docRequest) throws Exception {
        LocalDateTime dateTime = docRequest.getFromDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
        System.out.println("CSV file:" + docRequest.getCsvPath());
        if (!Files.exists(docRequest.getCsvPath().toAbsolutePath())) {
            throw new RuntimeException("ValueSet source CSV File does not exist");
        }
        if (!Files.isRegularFile(docRequest.getCsvPath().toAbsolutePath())) {
            throw new RuntimeException("ValueSet source CSV File does not exist");
        }

        String valueSetName = Utils.removeFileExtension(docRequest.getCsvPath().getFileName().toString(), true);
        ValueSet valueSet = new ValueSet();
        valueSet.setVersion("1.0");
        valueSet.setName(valueSetName);
        valueSet.setStatus(Enumerations.PublicationStatus.DRAFT);
        //Date today = Date.from(dateTime.atZone(ZoneId.systemDefault()).toInstant());
        Date today = Date.from(dateTime.toLocalDate().atStartOfDay(ZoneId.systemDefault()).toInstant());
        valueSet.setDate(today);
        valueSet.setPublisher("HCX");
        valueSet.setPublisher("HCX");
        valueSet.setDescription(String.format("%s - Edit Description", valueSetName));

        Reader reader = Files.newBufferedReader(docRequest.getCsvPath());
        CSVParser parser = new CSVParserBuilder()
                .withSeparator(',')
                .withIgnoreQuotations(true)
                .build();
        CSVReader csvReader = new CSVReaderBuilder(reader)
                .withSkipLines(0)
                .withCSVParser(parser)
                .build();
        List<String[]> lines = csvReader.readAll();
        //line 2-3 are headers
        List<String[]> headers = lines.subList(1, 4);
        //lines starting from 6 are codes
        List<String[]> codeList = lines.subList(5, lines.size());

        if (headers.size() >= 1) {
            if (headers.get(0).length > 0) {
                String name = readLineValue(headers.get(0), "Name");
                if (!"".equals(name)) {
                    valueSetName = name;
                }
            }
        }

        if (headers.size() >= 2) {
            if (headers.get(1).length > 0) {
                String description = readLineValue(headers.get(1), "Description");
                if (!"".equals(description)) {
                    valueSet.setDescription(description);
                }
            }
        }

        valueSet.setName(valueSetName);
        valueSet.setUrl("http://hcxprotocol.io/ValueSet/" + valueSetName);

        if (headers.size() >= 3) {
            if (headers.get(2).length > 0) {
                String url = readLineValue(headers.get(2), "Url");
                if (!"".equals(url)) {
                    valueSet.setUrl(url);
                }
            }
        }

        ValueSet.ValueSetExpansionComponent expansion = new ValueSet.ValueSetExpansionComponent();
        valueSet.setExpansion(expansion);

        for (String[] codes : codeList) {
            if (codes.length > 1) {
                ValueSet.ValueSetExpansionContainsComponent expandComponent = expansion.addContains();
                expandComponent.setCode(codes[0]);
                expandComponent.setDisplay(codes[1]);
            }
        }

        FhirContext fhirContext = FhirContext.forR4();
        String encodedString = fhirContext.newJsonParser().encodeResourceToString(valueSet);

        String fileName = String.format("hcx-vs-%s-%s.json",valueSetName, Utils.formatDate(today, "yyyyMMdd"));
        Path path = Paths.get(docRequest.getOutPath().toString(), fileName);
        System.out.println("Saving ValueSet to file:" + path.toString());
        Utils.saveToFile(path, encodedString);
    }

    private String readLineValue(String[] parts, String key) {
        if (parts[0].equalsIgnoreCase(key)) {
            return parts.length > 1 ? parts[1] : "";
        }
        return "";
    }

}

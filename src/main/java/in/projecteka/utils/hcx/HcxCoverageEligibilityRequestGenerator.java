package in.projecteka.utils.hcx;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import in.projecteka.utils.DocRequest;
import in.projecteka.utils.common.DocumentGenerator;
import in.projecteka.utils.data.FHIRUtils;
import in.projecteka.utils.data.Utils;
import lombok.SneakyThrows;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Composition;
import org.hl7.fhir.r4.model.CoverageEligibilityRequest;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Organization;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.ResourceType;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import java.util.stream.Collectors;

public class HcxCoverageEligibilityRequestGenerator implements DocumentGenerator {
    private Properties doctors;
    private Properties patients;

    @Override
    public void init() throws Exception {
        doctors = Utils.loadFromFile("/practitioners.properties");
        patients = Utils.loadFromFile("/patients.properties");
    }

    @Override
    public void execute(DocRequest docRequest) throws Exception {
        FhirContext fhirContext = FhirContext.forR4();
        LocalDateTime dateTime = docRequest.getFromDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
        for (int i = 0; i < docRequest.getNumber(); i++) {
            Date date = Utils.getNextDate(dateTime, i);
            Bundle bundle = createCoverageEligbilityRequestBundle(
                    fhirContext.newJsonParser(),
                    date,
                    docRequest.getPatientId(),
                    docRequest.getPatientName(),
                    docRequest.getProvName());
            String encodedString = fhirContext.newJsonParser().encodeResourceToString(bundle);
            List<Bundle.BundleEntryComponent> patientEntries =
                    bundle.getEntry().stream()
                            .filter(e -> e.getResource().getResourceType().equals(ResourceType.Patient))
                            .collect(Collectors.toList());
            Bundle.BundleEntryComponent patientEntry = patientEntries.get(0);
            String fileName = String.format("%s%sCoverageEligibilityRequestDoc%s.json",
                    docRequest.getProvName().toUpperCase(),
                    patientEntry.getResource().getId(),
                    Utils.formatDate(date, "yyyyMMdd"));
            Path path = Paths.get(docRequest.getOutPath().toString(), fileName);
            System.out.println("Saving CoverageEligibilityRequestDoc to file:" + path.toString());
            Utils.saveToFile(path, encodedString);
        }

    }

    @SneakyThrows
    private Bundle createCoverageEligbilityRequestBundle(IParser fhirParser, Date date, String patientId, String patientName, String hipPrefix) {
        //Create bundle and other resources like patinet and provider organization
        Bundle bundle = FHIRUtils.createBundle(date, hipPrefix);
        Patient patientResource = FHIRUtils.getPatientResource(patientName, patientId, patients);
        patientResource.addIdentifier(createInsurerIdentifier("http://gicofIndia.com", "BEN-101"));

        Reference patientRef = FHIRUtils.getReferenceToPatient(patientResource);
        //Practitioner author = FHIRUtils.createAuthor(hipPrefix, doctors);
        Organization author = fhirParser.parseResource(Organization.class, FHIRUtils.loadOrganization(hipPrefix));


        Composition composition = new Composition();
        composition.setId(UUID.randomUUID().toString());
        composition.setDate(bundle.getTimestamp());
        composition.setIdentifier(FHIRUtils.getIdentifier(composition.getId(), hipPrefix, "hcx-documents")); //TODO IG
        composition.setStatus(Composition.CompositionStatus.FINAL);
        composition.setType(HcxFhirUtils.getCompositionType()); //TODO IG
        composition.setTitle("Coverage Eligibility Request");
        composition.addAuthor().setResource(author);

        FHIRUtils.addToBundleEntry(bundle, composition, false);
        FHIRUtils.addToBundleEntry(bundle, author, false);
        FHIRUtils.addToBundleEntry(bundle, patientResource, false);

        composition.setSubject(patientRef);

        Composition.SectionComponent section = composition.addSection();
        section.setTitle("# Eligibility Request");
        section.setCode(HcxFhirUtils.getCoverageEligibilityResourceType()); //TODO IG - Same as Composition.type?
        CoverageEligibilityRequest cer = new CoverageEligibilityRequest();
        cer.setId(UUID.randomUUID().toString());
        cer.setStatus(CoverageEligibilityRequest.EligibilityRequestStatus.ACTIVE);
        section.getEntry().add(FHIRUtils.getReferenceToResource(cer));
        FHIRUtils.addToBundleEntry(bundle, cer, false);


        //report.setSubject(FHIRUtils.getReferenceToPatient(patientResource));

//        report.setIssued(date);
//        if (randomBool()) {
//            report.setEffective(FHIRUtils.getDateTimeType(date));
//        }
//
//        Organization organization = fhirParser.parseResource(Organization.class, FHIRUtils.loadOrganization(hipPrefix));
//        FHIRUtils.addToBundleEntry(bundle, organization, true);
//        report.setPerformer(Collections.singletonList(FHIRUtils.getReferenceToResource(organization)));
//
//        Practitioner interpreter = author;
//        if (randomBool()) {
//            interpreter = FHIRUtils.createAuthor(hipPrefix, doctors);
//            Practitioner doctor = (Practitioner) FHIRUtils.findResourceInBundleById(bundle, ResourceType.Practitioner, interpreter.getId());
//            if (doctor == null) {
//                FHIRUtils.addToBundleEntry(bundle, interpreter, false);
//            } else {
//                interpreter = doctor;
//            }
//        }
//        report.setResultsInterpreter(Collections.singletonList(FHIRUtils.getReferenceToResource(interpreter)));
//        report.setCode(FHIRUtils.getDiagnosticTestCode(SimpleDiagnosticTest.getRandomTest()));
//
//        if (randomBool()) {
//            report.setConclusion("Refer to Doctor. To be correlated with further study.");
//        }
//
//
//        if (randomBool()) {
//            DocumentReference docReference = FHIRUtils.getReportAsDocReference(author, "Surgical Pathology Report");
//            FHIRUtils.addToBundleEntry(bundle, docReference, false);
//            section.getEntry().add(FHIRUtils.getReferenceToResource(docReference));
//        }

        return bundle;
    }

    private Identifier createInsurerIdentifier(String insurerDomain, String value) {
        Identifier identifier = new Identifier();
        identifier.setSystem(String.format("%s/beneficiaries", insurerDomain));
        identifier.setValue(value);
        identifier.setType(new CodeableConcept(new Coding("http://terminology.hl7.org/CodeSystem/v2-0203", "SN","Subscriber Number")));
        return identifier;
    }
}

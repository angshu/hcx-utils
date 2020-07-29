package in.projecteka.utils.data;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import in.projecteka.utils.data.model.DiagnosticTest;
import in.projecteka.utils.data.model.Doctor;
import in.projecteka.utils.data.model.Obs;
import org.hl7.fhir.r4.model.Attachment;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Composition;
import org.hl7.fhir.r4.model.DiagnosticReport;
import org.hl7.fhir.r4.model.DocumentReference;
import org.hl7.fhir.r4.model.Encounter;
import org.hl7.fhir.r4.model.Enumerations;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Organization;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Practitioner;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.ResourceType;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import java.util.stream.Collectors;

import static in.projecteka.utils.data.Constants.EKA_LOINC_SYSTEM;
import static in.projecteka.utils.data.Utils.randomBool;

public class DiagnosticReportGenerator implements DocumentGenerator {
    private Properties doctors;
    private Properties patients;

    @Override
    public void init() throws Exception {
        doctors = Utils.loadFromFile("/practitioners.properties");
        patients = Utils.loadFromFile("/patients.properties");
    }

    @Override
    public void execute(String patientName, Date fromDate, int number, Path location, String hipPrefix) throws Exception {
        FhirContext fhirContext = FhirContext.forR4();
        LocalDateTime dateTime = fromDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
        for (int i = 0; i < number; i++) {
            Date date = Utils.getNextDate(dateTime, i);
            Bundle bundle = createDiagnosticReportBundle(date, patientName, hipPrefix, fhirContext.newJsonParser());
            String encodedString = fhirContext.newJsonParser().encodeResourceToString(bundle);
            List<Bundle.BundleEntryComponent> patientEntries =
                    bundle.getEntry().stream()
                            .filter(e -> e.getResource().getResourceType().equals(ResourceType.Patient))
                            .collect(Collectors.toList());
            Bundle.BundleEntryComponent patientEntry = patientEntries.get(0);
            String fileName = String.format("%s%sDiagnosticReportDoc%s.json",
                    hipPrefix.toUpperCase(),
                    patientEntry.getResource().getId(),
                    Utils.formatDate(date, "yyyyMMdd"));
            Path path = Paths.get(location.toString(), fileName);
            System.out.println("Saving DiagnosticReport to file:" + path.toString());
            Utils.saveToFile(path, encodedString);
            //System.out.println(encodedString);
        }
    }

    private Bundle createDiagnosticReportBundle(Date date, String patientName, String hipPrefix, IParser parser) throws Exception {
        Bundle bundle = FHIRUtils.createBundle(date, hipPrefix);
        Patient patientResource = FHIRUtils.getPatientResource(patientName, patients);
        Reference patientRef = new Reference();
        patientRef.setResource(patientResource);
        Composition reportDoc = new Composition();
        reportDoc.setId(UUID.randomUUID().toString());
        reportDoc.setDate(bundle.getTimestamp());
        reportDoc.setIdentifier(FHIRUtils.getIdentifier(reportDoc.getId(), hipPrefix, "document"));
        reportDoc.setStatus(Composition.CompositionStatus.FINAL);
        reportDoc.setType(FHIRUtils.getDiagnosticReportType());
        reportDoc.setTitle("Diagnostic Report Document");
        FHIRUtils.addToBundleEntry(bundle, reportDoc, false);
        Practitioner author = FHIRUtils.createAuthor(hipPrefix, doctors);
        FHIRUtils.addToBundleEntry(bundle, author, false);
        reportDoc.addAuthor().setResource(author);
        if (randomBool()) {
            reportDoc.getAuthor().get(0).setDisplay(Doctor.getDisplay(author));
        }
        FHIRUtils.addToBundleEntry(bundle, patientResource, false);
        reportDoc.setSubject(FHIRUtils.getReferenceToPatient(patientResource));
        Composition.SectionComponent section = reportDoc.addSection();
        section.setTitle("# Diagnostic Report");
        section.setCode(FHIRUtils.getDiagnosticReportType());

        Encounter encounter = FHIRUtils.createEncounter("Outpatient visit", "AMB", reportDoc.getDate());
        encounter.setSubject(FHIRUtils.getReferenceToPatient(patientResource));
        FHIRUtils.addToBundleEntry(bundle, encounter, false);
        Reference referenceToResource = FHIRUtils.getReferenceToResource(encounter);
        reportDoc.setEncounter(referenceToResource);

        DiagnosticReport report = new DiagnosticReport();
        report.setId(UUID.randomUUID().toString());
        report.setStatus(DiagnosticReport.DiagnosticReportStatus.FINAL);
        section.getEntry().add(FHIRUtils.getReferenceToResource(report));
        FHIRUtils.addToBundleEntry(bundle, report, false);
        report.setSubject(FHIRUtils.getReferenceToPatient(patientResource));
        if (randomBool()) {
            report.setEncounter(referenceToResource);
        }

        report.setIssued(date);
        if (randomBool()) {
            report.setEffective(FHIRUtils.getDateTimeType(date));
        }

        Organization organization = parser.parseResource(Organization.class, FHIRUtils.loadOrganization(hipPrefix));
        FHIRUtils.addToBundleEntry(bundle, organization, true);
        report.setPerformer(Collections.singletonList(FHIRUtils.getReferenceToResource(organization)));

        Practitioner interpreter = author;
        if (randomBool()) {
            interpreter = FHIRUtils.createAuthor(hipPrefix, doctors);
            FHIRUtils.addToBundleEntry(bundle, interpreter, false);
        }
        report.setResultsInterpreter(Collections.singletonList(FHIRUtils.getReferenceToResource(interpreter)));
        report.setCode(getDiagnosticTestCode(DiagnosticTest.getRandomTest()));

        if (randomBool()) {
            report.setConclusion("Refer to Doctor. To be correlated with further study.");
        }

        if (randomBool()) {
            //presented form
            report.getPresentedForm().add(getSurgicalReportAsAttachment());
            if (randomBool()) {
                addObservvationsToBundle(parser, bundle, report);
            }
        } else {
            addObservvationsToBundle(parser, bundle, report);
        }

        if (randomBool()) {
            DocumentReference docReference = getReportAsDocReference(author);
            FHIRUtils.addToBundleEntry(bundle, docReference, false);
            section.getEntry().add(FHIRUtils.getReferenceToResource(docReference));
        }

        return bundle;
    }

    private void addObservvationsToBundle(IParser parser, Bundle bundle, DiagnosticReport report) {
        Observation observation = parser.parseResource(Observation.class, Obs.getObservationResString());
        observation.setId(UUID.randomUUID().toString());
        FHIRUtils.addToBundleEntry(bundle, observation, true);
        report.addResult(FHIRUtils.getReferenceToResource(observation));
    }

    private DocumentReference getReportAsDocReference(Practitioner author) throws IOException {
        DocumentReference documentReference = new DocumentReference();
        documentReference.setStatus(Enumerations.DocumentReferenceStatus.CURRENT);
        documentReference.setId(UUID.randomUUID().toString());
        documentReference.setType(FHIRUtils.getDiagnosticReportType());
        CodeableConcept concept = new CodeableConcept();
        Coding coding = concept.addCoding();
        coding.setSystem(EKA_LOINC_SYSTEM);
        coding.setCode("30954-2");
        coding.setDisplay("Surgical Pathology Report");
        documentReference.setType(concept);
        documentReference.setAuthor(Collections.singletonList(FHIRUtils.getReferenceToResource(author)));
        DocumentReference.DocumentReferenceContentComponent content = documentReference.addContent();
        content.setAttachment(getSurgicalReportAsAttachment());
        return  documentReference;
    }

    private Attachment getSurgicalReportAsAttachment() throws IOException {
        Attachment attachment = new Attachment();
        attachment.setTitle("Surgical Pathology Report");
        attachment.setContentType("application/pdf");
        attachment.setData(Utils.readFileContent("/sample-prescription-base64.txt"));
        return attachment;
    }


    private CodeableConcept getDiagnosticTestCode(DiagnosticTest randomTest) {
        CodeableConcept concept = new CodeableConcept();
        if (randomBool()) {
            concept.setText(randomTest.getDisplay());
        }
        Coding coding = concept.addCoding();
        coding.setCode(randomTest.getCode());
        coding.setSystem(EKA_LOINC_SYSTEM);
        coding.setDisplay(randomTest.getDisplay());
        return concept;
    }

}

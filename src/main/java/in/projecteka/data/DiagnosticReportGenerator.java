package in.projecteka.data;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import in.projecteka.data.model.DiagnosticTest;
import in.projecteka.data.model.Doctor;
import in.projecteka.data.model.Obs;
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

import static in.projecteka.data.Constants.EKA_LOINC_SYSTEM;
import static in.projecteka.data.FHIRUtils.addToBundleEntry;
import static in.projecteka.data.FHIRUtils.createAuthor;
import static in.projecteka.data.FHIRUtils.createBundle;
import static in.projecteka.data.FHIRUtils.createEncounter;
import static in.projecteka.data.FHIRUtils.getDateTimeType;
import static in.projecteka.data.FHIRUtils.getDiagnosticReportType;
import static in.projecteka.data.FHIRUtils.getIdentifier;
import static in.projecteka.data.FHIRUtils.getPatientResource;
import static in.projecteka.data.FHIRUtils.getReferenceToPatient;
import static in.projecteka.data.FHIRUtils.getReferenceToResource;
import static in.projecteka.data.FHIRUtils.loadOrganization;
import static in.projecteka.data.Utils.randomBool;

public class DiagnosticReportGenerator implements DocumentGenerator {
    private Properties doctors;
    private Properties patients;

    @Override
    public void init() throws Exception {
        doctors = Utils.loadFromFile("/practitioners.properties");
        patients = Utils.loadFromFile("/patients.properties");
    }

    @Override
    public void execute(String patientName, Date fromDate, int number, Path location) throws Exception {
        FhirContext fhirContext = FhirContext.forR4();
        LocalDateTime dateTime = fromDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
        for (int i = 0; i < number; i++) {
            Date date = Utils.getNextDate(dateTime, i);
            Bundle bundle = createDiagnosticReportBundle(date, patientName, "max", fhirContext.newJsonParser());
            String encodedString = fhirContext.newJsonParser().encodeResourceToString(bundle);
            List<Bundle.BundleEntryComponent> patientEntries =
                    bundle.getEntry().stream()
                            .filter(e -> e.getResource().getResourceType().equals(ResourceType.Patient))
                            .collect(Collectors.toList());
            Bundle.BundleEntryComponent patientEntry = patientEntries.get(0);
            String fileName = String.format("%sDiagnosticReportDoc%s.json",
                    patientEntry.getResource().getId(), Utils.formatDate(date, "yyyyMMdd"));
            Path path = Paths.get(location.toString(), fileName);
            System.out.println("Saving DiagnosticReport to file:" + path.toString());
            Utils.saveToFile(path, encodedString);
            //System.out.println(encodedString);
        }
    }

    private Bundle createDiagnosticReportBundle(Date date, String patientName, String hipPrefix, IParser parser) throws Exception {
        Bundle bundle = createBundle(date, hipPrefix);
        Patient patientResource = getPatientResource(patientName, patients);
        Reference patientRef = new Reference();
        patientRef.setResource(patientResource);
        Composition reportDoc = new Composition();
        reportDoc.setId(UUID.randomUUID().toString());
        reportDoc.setDate(bundle.getTimestamp());
        reportDoc.setIdentifier(getIdentifier(reportDoc.getId(), hipPrefix, "document"));
        reportDoc.setStatus(Composition.CompositionStatus.FINAL);
        reportDoc.setType(getDiagnosticReportType());
        reportDoc.setTitle("Diagnostic Report Document");
        addToBundleEntry(bundle, reportDoc, false);
        Practitioner author = createAuthor(hipPrefix, doctors);
        addToBundleEntry(bundle, author, false);
        reportDoc.addAuthor().setResource(author);
        if (randomBool()) {
            reportDoc.getAuthor().get(0).setDisplay(Doctor.getDisplay(author));
        }
        addToBundleEntry(bundle, patientResource, false);
        reportDoc.setSubject(getReferenceToPatient(patientResource));
        Composition.SectionComponent section = reportDoc.addSection();
        section.setTitle("# Diagnostic Report");
        section.setCode(getDiagnosticReportType());

        Encounter encounter = createEncounter("Outpatient visit", "AMB", reportDoc.getDate());
        encounter.setSubject(getReferenceToPatient(patientResource));
        addToBundleEntry(bundle, encounter, false);
        Reference referenceToResource = getReferenceToResource(encounter);
        reportDoc.setEncounter(referenceToResource);

        DiagnosticReport report = new DiagnosticReport();
        report.setId(UUID.randomUUID().toString());
        report.setStatus(DiagnosticReport.DiagnosticReportStatus.FINAL);
        section.getEntry().add(getReferenceToResource(report));
        addToBundleEntry(bundle, report, false);
        report.setSubject(getReferenceToPatient(patientResource));
        if (randomBool()) {
            report.setEncounter(referenceToResource);
        }

        report.setIssued(date);
        if (randomBool()) {
            report.setEffective(getDateTimeType(date));
        }

        Organization organization = parser.parseResource(Organization.class, loadOrganization(hipPrefix));
        addToBundleEntry(bundle, organization, true);
        report.setPerformer(Collections.singletonList(getReferenceToResource(organization)));

        Practitioner interpreter = author;
        if (randomBool()) {
            interpreter = createAuthor(hipPrefix, doctors);
            addToBundleEntry(bundle, interpreter, false);
        }
        report.setResultsInterpreter(Collections.singletonList(getReferenceToResource(interpreter)));
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
            addToBundleEntry(bundle, docReference, false);
            section.getEntry().add(getReferenceToResource(docReference));
        }

        return bundle;
    }

    private void addObservvationsToBundle(IParser parser, Bundle bundle, DiagnosticReport report) {
        Observation observation = parser.parseResource(Observation.class, Obs.getObservationResString());
        observation.setId(UUID.randomUUID().toString());
        addToBundleEntry(bundle, observation, true);
        report.addResult(getReferenceToResource(observation));
    }

    private DocumentReference getReportAsDocReference(Practitioner author) throws IOException {
        DocumentReference documentReference = new DocumentReference();
        documentReference.setStatus(Enumerations.DocumentReferenceStatus.CURRENT);
        documentReference.setId(UUID.randomUUID().toString());
        documentReference.setType(getDiagnosticReportType());
        CodeableConcept concept = new CodeableConcept();
        Coding coding = concept.addCoding();
        coding.setSystem(EKA_LOINC_SYSTEM);
        coding.setCode("30954-2");
        coding.setDisplay("Surgical Pathology Report");
        documentReference.setType(concept);
        documentReference.setAuthor(Collections.singletonList(getReferenceToResource(author)));
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

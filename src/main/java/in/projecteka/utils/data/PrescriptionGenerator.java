package in.projecteka.utils.data;

import ca.uhn.fhir.context.FhirContext;
import in.projecteka.utils.DocRequest;
import in.projecteka.utils.common.DocumentGenerator;
import in.projecteka.utils.data.model.Doctor;
import in.projecteka.utils.data.model.Medicine;
import org.hl7.fhir.r4.model.Binary;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Composition;
import org.hl7.fhir.r4.model.Condition;
import org.hl7.fhir.r4.model.Encounter;
import org.hl7.fhir.r4.model.Medication;
import org.hl7.fhir.r4.model.MedicationRequest;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Practitioner;
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

public class PrescriptionGenerator implements DocumentGenerator {
    private Properties medicationProps;
    private Properties doctors;
    private Properties patients;

    public void init() throws Exception {
        medicationProps = Utils.loadFromFile("/medications.properties");
        doctors = Utils.loadFromFile("/practitioners.properties");
        patients = Utils.loadFromFile("/patients.properties");
    }

    public void execute(DocRequest request) throws Exception {
        FhirContext fhirContext = FhirContext.forR4();
        LocalDateTime dateTime = request.getFromDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
        for (int i = 0; i < request.getNumber(); i++) {
            Date date = Utils.getNextDate(dateTime, i);
            Bundle bundle = createPrescriptionBundle(date, request.getPatientName(), request.getProvName(), request.getPatientId());
            String encodedString = fhirContext.newJsonParser().encodeResourceToString(bundle);
            List<Bundle.BundleEntryComponent> patientEntries =
                    bundle.getEntry().stream()
                            .filter(e -> e.getResource().getResourceType().equals(ResourceType.Patient))
                            .collect(Collectors.toList());
            Bundle.BundleEntryComponent patientEntry = patientEntries.get(0);
            String fileName = String.format("%s%sPrescriptionDoc%s.json",
                    request.getProvName().toUpperCase(),
                    patientEntry.getResource().getId(),
                    Utils.formatDate(date, "yyyyMMdd"));
            Path path = Paths.get(request.getOutPath().toString(), fileName);
            System.out.println("Saving Prescription to file:" + path.toString());
            Utils.saveToFile(path, encodedString);
            //System.out.println(encodedString);
        }
    }

    private Bundle createPrescriptionBundle(Date date, String patientName, String hipPrefix, String patientId) throws Exception {
        Bundle bundle = FHIRUtils.createBundle(date, hipPrefix);
        Patient patientResource = FHIRUtils.getPatientResource(patientName, patientId, patients);
        Reference patientRef = createPatientReference(patientResource);

        Composition prescriptionDoc = new Composition();
        prescriptionDoc.setId(UUID.randomUUID().toString());
        prescriptionDoc.setDate(bundle.getTimestamp());
        prescriptionDoc.setIdentifier(FHIRUtils.getIdentifier(prescriptionDoc.getId(), hipPrefix, "document"));
        prescriptionDoc.setStatus(Composition.CompositionStatus.FINAL);
        CodeableConcept prescriptionType = FHIRUtils.getPrescriptionType();
        prescriptionDoc.setType(prescriptionType);
        prescriptionDoc.setTitle("Prescription");
        FHIRUtils.addToBundleEntry(bundle, prescriptionDoc, false);

        Practitioner author = FHIRUtils.createAuthor(hipPrefix, doctors);
        FHIRUtils.addToBundleEntry(bundle, author, false);
        prescriptionDoc.addAuthor().setResource(author);
        if (Utils.randomBool()) {
            prescriptionDoc.getAuthor().get(0).setDisplay(Doctor.getDisplay(author));
        }
        FHIRUtils.addToBundleEntry(bundle, patientResource, false);
        prescriptionDoc.setSubject(FHIRUtils.getReferenceToPatient(patientResource));

        if (Utils.randomBool()) {
            //add encounter
            Encounter encounter = FHIRUtils.createEncounter("Outpatient visit", "AMB", prescriptionDoc.getDate());
            encounter.setSubject(patientRef);
            FHIRUtils.addToBundleEntry(bundle, encounter, false);
            prescriptionDoc.setEncounter(FHIRUtils.getReferenceToResource(encounter));
        }

        Composition.SectionComponent section = prescriptionDoc.addSection();
        section.setTitle("OPD Prescription");
        section.setCode(prescriptionType);

        int numberOfMeds = Utils.randomInt(1, 3);
        for (int i = 0; i < numberOfMeds; i++) {
            int medIndex = Utils.randomInt(1, 10);
            Medicine med = Medicine.parse((String) medicationProps.get(String.valueOf(medIndex)));
            Condition condition = FHIRUtils.getCondition(med.getCondition());
            Medication medication = FHIRUtils.getMedication(med);
            if (condition != null) {
                condition.setSubject(patientRef);
                FHIRUtils.addToBundleEntry(bundle, condition, false);
            }
            var useMedicationCodeableConcept = Utils.randomBool();
            if (!useMedicationCodeableConcept) {
                FHIRUtils.addToBundleEntry(bundle, medication, false);
            }
            MedicationRequest medReq = FHIRUtils.createMedicationRequest(author, prescriptionDoc.getDate(), med, medication, condition, useMedicationCodeableConcept);
            medReq.setSubject(patientRef);
            FHIRUtils.addToBundleEntry(bundle, medReq, false);
            section.getEntry().add(FHIRUtils.getReferenceToResource(medReq));
        }

        if (Utils.randomInt(1,10) % 3 == 0) {
            System.out.println("Including a binary resource");
            Binary binary = new Binary();
            binary.setId(UUID.randomUUID().toString());
            binary.setContentType("application/pdf");
            binary.setData(Utils.readFileContent("/sample-prescription-base64.txt"));
            FHIRUtils.addToBundleEntry(bundle, binary, false);
            section.getEntry().add(FHIRUtils.getReferenceToResource(binary));
        }
        return bundle;
    }

    private Reference createPatientReference(Patient patientResource) {
        Reference patientRef = new Reference();
        patientRef.setResource(patientResource);
        return patientRef;
    }

}

package in.projecteka.utils.data;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import in.projecteka.utils.DocRequest;
import in.projecteka.utils.data.model.Doctor;
import in.projecteka.utils.data.model.Medicine;
import in.projecteka.utils.data.model.Obs;
import in.projecteka.utils.data.model.SimpleAllergy;
import in.projecteka.utils.data.model.SimpleCondition;
import in.projecteka.utils.data.model.SimpleDiagnosticTest;
import lombok.SneakyThrows;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.AllergyIntolerance;
import org.hl7.fhir.r4.model.Annotation;
import org.hl7.fhir.r4.model.Appointment;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.CarePlan;
import org.hl7.fhir.r4.model.Composition;
import org.hl7.fhir.r4.model.Condition;
import org.hl7.fhir.r4.model.DateTimeType;
import org.hl7.fhir.r4.model.DiagnosticReport;
import org.hl7.fhir.r4.model.DocumentReference;
import org.hl7.fhir.r4.model.Encounter;
import org.hl7.fhir.r4.model.Medication;
import org.hl7.fhir.r4.model.MedicationRequest;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Organization;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Period;
import org.hl7.fhir.r4.model.Practitioner;
import org.hl7.fhir.r4.model.Procedure;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.ResourceType;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import java.util.stream.Collectors;

import static in.projecteka.utils.data.Constants.EKA_SCT_SYSTEM;
import static in.projecteka.utils.data.Constants.FHIR_CONDITION_CATEGORY_SYSTEM;
import static in.projecteka.utils.data.Constants.FHIR_CONDITION_CLINICAL_STATUS_SYSTEM;
import static in.projecteka.utils.data.FHIRUtils.getDateTimeType;
import static in.projecteka.utils.data.FHIRUtils.getDiagnosticTestCode;
import static in.projecteka.utils.data.FHIRUtils.getMedication;
import static in.projecteka.utils.data.FHIRUtils.getReportAsDocReference;
import static in.projecteka.utils.data.FHIRUtils.getSurgicalReportAsAttachment;
import static in.projecteka.utils.data.Utils.getPastDate;
import static in.projecteka.utils.data.Utils.randomBool;

public class OPConsultationGenerator implements  DocumentGenerator {
    private Properties doctors;
    private Properties patients;
    private Properties medicationProps;

    @Override
    public void init() throws Exception {
        doctors = Utils.loadFromFile("/practitioners.properties");
        patients = Utils.loadFromFile("/patients.properties");
        medicationProps = Utils.loadFromFile("/medications.properties");
    }

    @Override
    public void execute(DocRequest request) throws Exception {
        FhirContext fhirContext = FhirContext.forR4();
        LocalDateTime dateTime = request.getFromDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
        for (int i = 0; i < request.getNumber(); i++) {
            Date date = Utils.getNextDate(dateTime, i);
            Bundle bundle = createOPConsultationBundle(date, request.getPatientName(), request.getHipPrefix(), fhirContext.newJsonParser(), request.getPatientId());
            String encodedString = fhirContext.newJsonParser().encodeResourceToString(bundle);
            List<Bundle.BundleEntryComponent> patientEntries =
                    bundle.getEntry().stream()
                            .filter(e -> e.getResource().getResourceType().equals(ResourceType.Patient))
                            .collect(Collectors.toList());
            Bundle.BundleEntryComponent patientEntry = patientEntries.get(0);
            String fileName = String.format("%s%sOPConsultationDoc%s.json",
                    request.getHipPrefix().toUpperCase(),
                    patientEntry.getResource().getId(),
                    Utils.formatDate(date, "yyyyMMdd"));
            Path path = Paths.get(request.getOutPath().toString(), fileName);
            System.out.println("Saving OP Consultation to file:" + path.toString());
            Utils.saveToFile(path, encodedString);
            //System.out.println(encodedString);
        }
    }

    @SneakyThrows
    private Bundle createOPConsultationBundle(Date date, String patientName, String hipPrefix, IParser jsonParser, String patientId) {
        Bundle bundle = FHIRUtils.createBundle(date, hipPrefix);

        Composition opDoc = new Composition();
        opDoc.setId(UUID.randomUUID().toString());
        opDoc.setDate(bundle.getTimestamp());
        opDoc.setIdentifier(FHIRUtils.getIdentifier(opDoc.getId(), hipPrefix, "document"));
        opDoc.setStatus(Composition.CompositionStatus.FINAL);
        opDoc.setType(FHIRUtils.getOPConsultationType());
        opDoc.setTitle("OP Consultation Document");
        FHIRUtils.addToBundleEntry(bundle, opDoc, false);
        Practitioner author = FHIRUtils.createAuthor(hipPrefix, doctors);
        FHIRUtils.addToBundleEntry(bundle, author, false);
        opDoc.addAuthor().setResource(author);
        if (randomBool()) {
            opDoc.getAuthor().get(0).setDisplay(Doctor.getDisplay(author));
        }

        if (randomBool()) {
            Organization organization = jsonParser.parseResource(Organization.class, FHIRUtils.loadOrganization(hipPrefix));
            FHIRUtils.addToBundleEntry(bundle, organization, true);
            opDoc.setCustodian(FHIRUtils.getReferenceToResource(organization));
        }

        Patient patientResource = FHIRUtils.getPatientResource(patientName, patientId, patients);
        FHIRUtils.addToBundleEntry(bundle, patientResource, false);
        opDoc.setSubject(FHIRUtils.getReferenceToPatient(patientResource));

        Encounter encounter = FHIRUtils.createEncounter("Outpatient visit", "AMB", opDoc.getDate());
        encounter.setSubject(FHIRUtils.getReferenceToPatient(patientResource));
        FHIRUtils.addToBundleEntry(bundle, encounter, false);
        Reference referenceToResource = FHIRUtils.getReferenceToResource(encounter);
        opDoc.setEncounter(referenceToResource);


        createChiefComplaintsSection(bundle, opDoc, patientResource);
        createAllergiesSection(bundle, opDoc, patientResource, jsonParser);
        createMedicalHistorySection(bundle, opDoc, patientResource); //TODO
        createSymptomSection(bundle, opDoc, patientResource); //TODO
        createObservationSection(bundle, opDoc, patientResource, jsonParser);
        createInvestigationSection(bundle, opDoc, patientResource); //TODO
        createPrescriptionSection(bundle, opDoc, patientResource);
        createDocumentsSection(bundle, opDoc, patientResource, hipPrefix);
        createProcedureSection(bundle, opDoc, patientResource, hipPrefix);
        createDiagnosticReportSection(bundle, opDoc, patientResource, jsonParser, hipPrefix);
        createPlanSection(bundle, opDoc, patientResource);
        createFollowupSection(bundle, opDoc, patientResource, hipPrefix);
        return bundle;
    }

    private void createProcedureSection(Bundle bundle, Composition composition, Patient patient, String hipPrefix) {
        Composition.SectionComponent section = composition.addSection();
        section.setTitle("Procedures");
        section.setCode(FHIRUtils.getProcedureSectionCode());
        Procedure procedure = new Procedure();
        procedure.setId(UUID.randomUUID().toString());
        procedure.setStatus(Procedure.ProcedureStatus.COMPLETED);
        if (randomBool()) {
            procedure.setCode(FHIRUtils.getCodeableConcept(
                    "90105005",
                    "Biopsy of soft tissue of forearm (Procedure)",
                    "Biopsy of suspected melanoma L) arm"));
        } else {
            procedure.setCode(FHIRUtils.getCodeableConcept(
                    "232717009",
                    "Coronary artery bypass grafting",
                    ""));
        }
        procedure.setSubject(FHIRUtils.getReferenceToResource(patient));
        DateTimeType dateTimeType = new DateTimeType();
        dateTimeType.setValue(Utils.getFutureTime(composition.getDate(), 60));
        procedure.setPerformed(dateTimeType);
        procedure.setAsserter(composition.getAuthorFirstRep());
        Procedure.ProcedurePerformerComponent performer = procedure.addPerformer();
        //performer.setActor()

        if (randomBool()) {
            Practitioner surgeon = FHIRUtils.createAuthor(hipPrefix, doctors);
            Resource existingResourceInBundle = FHIRUtils.findResourceInBundleById(bundle, ResourceType.Practitioner, surgeon.getId());
            if (existingResourceInBundle != null) {
                performer.setActor(FHIRUtils.getReferenceToResource(existingResourceInBundle));
            } else {
                FHIRUtils.addToBundleEntry(bundle, surgeon, true);
                performer.setActor(FHIRUtils.getReferenceToResource(surgeon));
            }
        }
        procedure.setComplication(Collections.singletonList(FHIRUtils.getCodeableConcept("131148009", "Bleeding", null)));
        FHIRUtils.addToBundleEntry(bundle, procedure, true);
        section.getEntry().add(FHIRUtils.getReferenceToResource(procedure));
    }

    @SneakyThrows
    private void createFollowupSection(Bundle bundle, Composition composition, Patient patient, String hipPrefix) {
        Composition.SectionComponent section = composition.addSection();
        section.setTitle("Follow up");
        section.setCode(FHIRUtils.getFollowupSectionCode());
        Reference docRef = composition.getAuthor().get(0);
        Appointment app = FHIRUtils.createAppointment(docRef, Utils.getFutureDate(composition.getDate(), 7));
        if (randomBool()) {
            Practitioner anotherDoc = FHIRUtils.createAuthor(hipPrefix, doctors);
            var prevDoc = ((Practitioner) docRef.getResource());
            if (!isSamePractitioner(anotherDoc, prevDoc)) {
                FHIRUtils.addToBundleEntry(bundle, anotherDoc, true);
                Appointment.AppointmentParticipantComponent participant = app.addParticipant();
                participant.setActor(FHIRUtils.getReferenceToResource(anotherDoc));
                if (app.getStatus().equals(Appointment.AppointmentStatus.BOOKED)) {
                    participant.setStatus(Appointment.ParticipationStatus.ACCEPTED);
                } else {
                    participant.setStatus(Appointment.ParticipationStatus.TENTATIVE);
                }
            }
        }

        FHIRUtils.addToBundleEntry(bundle, app, true);
        section.getEntry().add(FHIRUtils.getReferenceToResource(app));
    }

    private boolean isSamePractitioner(Practitioner old, Practitioner newDoc) {
        return old.getId().equals(newDoc.getId());
    }

    @SneakyThrows
    private void createPlanSection(Bundle bundle, Composition composition, Patient patient) {
        Composition.SectionComponent section = composition.addSection();
        section.setTitle("Care Plan");
        section.setCode(FHIRUtils.getCarePlanSectionType());
        CarePlan plan = new CarePlan();
        plan.setId(UUID.randomUUID().toString());
        if (randomBool()) {
            plan.setStatus(CarePlan.CarePlanStatus.ACTIVE);
            plan.setIntent(CarePlan.CarePlanIntent.PLAN);
            plan.setTitle("Active Plan for next 2 months");
        } else {
            plan.setStatus(CarePlan.CarePlanStatus.DRAFT);
            plan.setIntent(CarePlan.CarePlanIntent.PROPOSAL);
            plan.setTitle("Tentative Plan for next 2 months");
        }
        plan.setSubject(composition.getSubject());
        plan.setAuthor(composition.getAuthorFirstRep());
        plan.setDescription("Actively monitor progress. Review every week to start with. Medications to be revised after 2 weeks.");

        Period period = new Period();
        period.setStart(composition.getDate());
        period.setEnd(Utils.getFutureDate(composition.getDate(), 60));
        plan.setPeriod(period);

        Annotation annotation1 = new Annotation();
        annotation1.setText("Actively monitor progress.");
        Annotation annotation2 = new Annotation();
        annotation2.setText("Review every week to start with. Medications to be revised after 2 weeks.");
        plan.setNote(Arrays.asList(annotation1, annotation2));

        FHIRUtils.addToBundleEntry(bundle, plan, false);
        section.getEntry().add(FHIRUtils.getReferenceToResource(plan));
    }

    @SneakyThrows
    private void createDiagnosticReportSection(Bundle bundle, Composition composition, Patient patient, IParser jsonParser, String hipPrefix) {
        if (randomBool()) return; //dont need diagnosticReport always

        Composition.SectionComponent section = composition.addSection();
        section.setTitle("Diagnostic Reports");
        section.setCode(FHIRUtils.getDiagnosticReportType());

        DiagnosticReport report = new DiagnosticReport();
        report.setId(UUID.randomUUID().toString());
        report.setStatus(DiagnosticReport.DiagnosticReportStatus.FINAL);
        section.getEntry().add(FHIRUtils.getReferenceToResource(report));
        FHIRUtils.addToBundleEntry(bundle, report, false);
        report.setSubject(FHIRUtils.getReferenceToPatient(patient));

        Reference encounterRef = composition.getEncounter();
        if (randomBool()) {
            Encounter encounter = FHIRUtils.createEncounter("Outpatient visit", "AMB", composition.getDate());
            encounter.setSubject(FHIRUtils.getReferenceToPatient(patient));
            FHIRUtils.addToBundleEntry(bundle, encounter, false);
            encounterRef = FHIRUtils.getReferenceToResource(encounter);
        }

        report.setIssued(composition.getDate());
        if (randomBool()) {
            report.setEffective(FHIRUtils.getDateTimeType(composition.getDate()));
        }

        Reference organizationRef = composition.getCustodian();
        if (organizationRef == null) {
            //TODO load a different organization
            Organization organization = jsonParser.parseResource(Organization.class, FHIRUtils.loadOrganization(hipPrefix));
            FHIRUtils.addToBundleEntry(bundle, organization, true);
            organizationRef = FHIRUtils.getReferenceToResource(organization);
        }
        report.setPerformer(Collections.singletonList(organizationRef));

        Practitioner interpreter = FHIRUtils.createAuthor(hipPrefix, doctors);
        var docAuthor = ((Practitioner) composition.getAuthor().get(0).getResource());
        if (!isSamePractitioner(interpreter, docAuthor)) {
            FHIRUtils.addToBundleEntry(bundle, interpreter, false);
        }
        report.setResultsInterpreter(Collections.singletonList(FHIRUtils.getReferenceToResource(interpreter)));
        report.setCode(getDiagnosticTestCode(SimpleDiagnosticTest.getRandomTest()));

        if (randomBool()) {
            report.setConclusion("Refer to Doctor. To be correlated with further study.");
        }

        if (randomBool()) {
            //presented form
            report.getPresentedForm().add(getSurgicalReportAsAttachment());
            if (randomBool()) {
                addObservvationsToBundle(jsonParser, bundle, report);
            }
        } else {
            addObservvationsToBundle(jsonParser, bundle, report);
        }

        if (randomBool()) {
            DocumentReference docReference = getReportAsDocReference(interpreter);
            FHIRUtils.addToBundleEntry(bundle, docReference, false);
            section.getEntry().add(FHIRUtils.getReferenceToResource(docReference));
        }
    }

    @SneakyThrows
    private void createDocumentsSection(Bundle bundle, Composition composition, Patient patient, String hipPrefix) {
        Composition.SectionComponent section = composition.addSection();
        section.setTitle("Clinical consultation");
        section.setCode(FHIRUtils.getDocumentReferenceSectionType());

        Practitioner docAuthor = (Practitioner) composition.getAuthorFirstRep().getResource();
        if (randomBool()) {
            Practitioner anotherDoc = FHIRUtils.createAuthor(hipPrefix, doctors);
            if (!isSamePractitioner(anotherDoc, docAuthor)) {
                FHIRUtils.addToBundleEntry(bundle, anotherDoc, false);
                docAuthor = anotherDoc;
            }
        }
        DocumentReference docReference = FHIRUtils.getReportAsDocReference(docAuthor);
        FHIRUtils.addToBundleEntry(bundle, docReference, false);
        section.getEntry().add(FHIRUtils.getReferenceToResource(docReference));
    }

    @SneakyThrows
    private void createPrescriptionSection(Bundle bundle, Composition composition, Patient patient) {
        int numberOfMeds = Utils.randomInt(1, 3);
        Composition.SectionComponent section = composition.addSection();
        section.setTitle("Medications");
        section.setCode(FHIRUtils.getPrescriptionSectionType());

        Reference referenceToPatient = FHIRUtils.getReferenceToPatient(patient);
        for (int i = 0; i < numberOfMeds; i++) {
            int medIndex = Utils.randomInt(1, 10);
            Medicine med = Medicine.parse((String) medicationProps.get(String.valueOf(medIndex)));
            Condition condition = FHIRUtils.getCondition(med.getCondition());
            Medication medication = getMedication(med);
            if (condition != null) {
                condition.setSubject(referenceToPatient);
                FHIRUtils.addToBundleEntry(bundle, condition, false);
            }
            var useMedicationCodeableConcept = Utils.randomBool();
            if (!useMedicationCodeableConcept) {
                FHIRUtils.addToBundleEntry(bundle, medication, false);
            }
            MedicationRequest medReq = FHIRUtils.createMedicationRequest(
                    getPractitioner(composition),
                    composition.getDate(),
                    med,
                    medication,
                    condition,
                    useMedicationCodeableConcept);
            medReq.setSubject(referenceToPatient);
            FHIRUtils.addToBundleEntry(bundle, medReq, false);
            section.getEntry().add(FHIRUtils.getReferenceToResource(medReq));
        }
    }

    private Practitioner getPractitioner(Composition composition) {
        IBaseResource resource = composition.getAuthor().get(0).getResource();
        return (Practitioner) resource;
    }

    @SneakyThrows
    private void createInvestigationSection(Bundle bundle, Composition composition, Patient patient) {
        return;
    }

    @SneakyThrows
    private void createObservationSection(Bundle bundle, Composition composition, Patient patient, IParser parser) {
        Composition.SectionComponent section = composition.addSection();
        section.setTitle("Physical Examination");
        section.setCode(FHIRUtils.getPhysicalExaminationSectionCode());
        int numOfObs = Utils.randomInt(1,3);
        for (int i = 0; i < numOfObs; i++) {
            Observation observation = parser.parseResource(Observation.class, Obs.getPhysicalObsResString());
            observation.setId(UUID.randomUUID().toString());
            FHIRUtils.addToBundleEntry(bundle, observation, true);
            section.getEntry().add(FHIRUtils.getReferenceToResource(observation));
        }
    }

    @SneakyThrows
    private void createSymptomSection(Bundle bundle, Composition composition, Patient patient) {
        return;
    }

    @SneakyThrows
    private void createAllergiesSection(Bundle bundle, Composition composition, Patient patient, IParser parser) {
        Composition.SectionComponent section = composition.addSection();
        section.setTitle("Allergy Section");
        section.setCode(FHIRUtils.getAllergySectionType());
        AllergyIntolerance foodAllergy = SimpleAllergy.getFoodAllergy(parser, composition.getSubject(), composition.getAuthorFirstRep());
        AllergyIntolerance medicationAllergy = SimpleAllergy.getMedicationAllergy(parser, composition.getSubject(), composition.getAuthorFirstRep());
        FHIRUtils.addToBundleEntry(bundle, foodAllergy, true);
        FHIRUtils.addToBundleEntry(bundle, medicationAllergy, true);
        section.getEntry().add(FHIRUtils.getReferenceToResource(foodAllergy));
        section.getEntry().add(FHIRUtils.getReferenceToResource(medicationAllergy));
    }

    @SneakyThrows
    private void createMedicalHistorySection(Bundle bundle, Composition composition, Patient patient) {
        return;
    }

    @SneakyThrows
    private void createChiefComplaintsSection(Bundle bundle, Composition composition, Patient patient) {
        int numberOfComplaints = Utils.randomInt(1, 3);
        Composition.SectionComponent section = composition.addSection();
        section.setTitle("Chief Complaints");
        section.setCode(FHIRUtils.getChiefComplaintSectionType());

        Reference referenceToPatient = FHIRUtils.getReferenceToPatient(patient);
        for (int i = 0; i < numberOfComplaints; i++) {
            Condition condition = createCondition(SimpleCondition.getRandomComplaint(), composition.getDate());
            condition.setSubject(referenceToPatient);
            FHIRUtils.addToBundleEntry(bundle, condition, false);
            section.getEntry().add(FHIRUtils.getReferenceToResource(condition));
        }
    }

    private Condition createCondition(SimpleCondition randomComplaint, Date date) {
        Condition condition = new Condition();
        condition.setId(UUID.randomUUID().toString());
        if (randomBool()) {
            condition.setClinicalStatus(
                    FHIRUtils.conceptWith(
                            randomComplaint.getClinicalStatus(),
                            randomComplaint.getClinicalStatus(),
                            FHIR_CONDITION_CLINICAL_STATUS_SYSTEM));
        }
        condition.setCode(FHIRUtils.conceptWith(randomComplaint.getText(), randomComplaint.getCode(), EKA_SCT_SYSTEM));
        condition.setCategory(Collections.singletonList(FHIRUtils.conceptWith(randomComplaint.getCategory(),
                randomComplaint.getCategoryCode(), FHIR_CONDITION_CATEGORY_SYSTEM)));
        condition.setSeverity(FHIRUtils.conceptWith(randomComplaint.getSeverity(), randomComplaint.getSeverityCode(), EKA_SCT_SYSTEM));
        if (randomBool()) {
            condition.setRecordedDate(date);
        }
        if (randomBool()) {
            Date onsetDate = getPastDate(date, 30);
            if (randomBool()) {
                condition.setOnset(getDateTimeType(onsetDate));
            } else {
                Period period = FHIRUtils.getPeriod(onsetDate, null);
                condition.setOnset(period);
            }
        }
        return condition;
    }

    private void addObservvationsToBundle(IParser parser, Bundle bundle, DiagnosticReport report) {
        Observation observation = parser.parseResource(Observation.class, Obs.getObservationResString());
        observation.setId(UUID.randomUUID().toString());
        FHIRUtils.addToBundleEntry(bundle, observation, true);
        report.addResult(FHIRUtils.getReferenceToResource(observation));
    }
}

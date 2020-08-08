package in.projecteka.utils.data;

import in.projecteka.utils.data.model.SimpleCondition;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Composition;
import org.hl7.fhir.r4.model.Condition;
import org.hl7.fhir.r4.model.DateTimeType;
import org.hl7.fhir.r4.model.Encounter;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Type;

import java.time.LocalDateTime;
import java.util.Date;

import static in.projecteka.utils.data.FHIRUtils.createEncounterDiagnosis;

public class DischargeSummaryGenerator extends OPConsultationGenerator {


    @Override
    protected String getDocBasicName() {
        return "DischargeSummaryDoc";
    }

    @Override
    protected Date getCompositionDate(LocalDateTime dateTime, int docIndex) {
        return Utils.getFutureDate(dateTime, docIndex*10);
    }

    @Override
    protected CodeableConcept getDocumentType() {
        return FHIRUtils.getDischargeSummaryType();
    }

    @Override
    protected Encounter getCompositionEncounter(Composition composition, Bundle bundle, Reference patientRef) {
        Encounter encounter = FHIRUtils.createEncounter("Inpatient visit", "IMP",
                Utils.getPastDate(composition.getDate(), 4));
        encounter.setSubject(patientRef);
        encounter.getPeriod().setEnd(Utils.getFutureDate(encounter.getPeriod().getStart(), 4));
        Encounter.DiagnosisComponent diagnosis = encounter.addDiagnosis();
        Condition condition = createEncounterDiagnosis(SimpleCondition.getRandomHospitalizationReason(), composition.getDate());
        condition.setSubject(patientRef);
        FHIRUtils.addToBundleEntry(bundle, condition, false);
        diagnosis.setCondition(FHIRUtils.getReferenceToResource(condition));
        return encounter;
    }

    @Override
    protected Date getAppointmentDate(Composition composition) {
        return Utils.getFutureDate(composition.getDate(), 3);
    }

    @Override
    protected Date getMedicationDate(Composition composition) {
        if (Utils.randomBool()) {
            return Utils.getPastDate(composition.getDate(), 1);
        } else {
            return Utils.getPastDate(composition.getDate(), 2);
        }
    }

    @Override
    protected String getComplaintsSectionTitle() {
        return "Presenting Problems";
    }

    @Override
    protected Type getEffectiveObservationDate(Composition composition, int index) {
        Encounter encounter = (Encounter) composition.getEncounter().getResource();
        DateTimeType dateTimeType = new DateTimeType();
        dateTimeType.setValue(Utils.getFutureDate(encounter.getPeriod().getStart(), index));
        return dateTimeType;
    }

    @Override
    protected DateTimeType getProcedureDate(Composition composition) {
        Encounter encounter = (Encounter) composition.getEncounter().getResource();
        DateTimeType dateTimeType = new DateTimeType();
        dateTimeType.setValue(Utils.getPastDate(encounter.getPeriod().getEnd(), 2));
        return dateTimeType;
    }

    @Override
    protected String getCompositionDocumentTitle() {
        return "Discharge Summary Document";
    }

    @Override
    protected String getMedicationSectionsTitle() {
        return "Prescribed medications during Admission";
    }
}

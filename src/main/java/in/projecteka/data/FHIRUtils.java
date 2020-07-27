package in.projecteka.data;

import in.projecteka.data.model.Doctor;
import lombok.SneakyThrows;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.DateTimeType;
import org.hl7.fhir.r4.model.Encounter;
import org.hl7.fhir.r4.model.Enumerations;
import org.hl7.fhir.r4.model.HumanName;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Meta;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Period;
import org.hl7.fhir.r4.model.Practitioner;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.StringType;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.Properties;
import java.util.UUID;

public class FHIRUtils {
    static Enumerations.AdministrativeGender getGender(String gender) {
        return Enumerations.AdministrativeGender.fromCode(gender);
    }

    static HumanName getHumanName(String name, String prefix, String suffix) {
        HumanName humanName = new HumanName();
        humanName.setText(name);
        if (!Utils.isBlank(prefix)) {
            humanName.setPrefix(Collections.singletonList(new StringType(prefix)));
        }
        if (!Utils.isBlank(suffix)) {
            humanName.setSuffix(Collections.singletonList(new StringType(suffix)));
        }
        return humanName;
    }

    static Identifier getIdentifier(String id, String domain, String resType) {
        Identifier identifier = new Identifier();
        identifier.setSystem(getHospitalSystemForType(domain, resType));
        identifier.setValue(id);
        return identifier;
    }

    private static String getHospitalSystemForType(String hospitalDomain, String type) {
        return String.format(Constants.HOSPITAL_SYSTEM, hospitalDomain, type);
    }

    static Bundle createBundle(Date forDate, String hipDomain) {
        Bundle bundle = new Bundle();
        bundle.setId(UUID.randomUUID().toString());
        bundle.setTimestamp(forDate);
        bundle.setIdentifier(getIdentifier(bundle.getId(), hipDomain, "bundle"));
        Meta bundleMeta = Utils.getMeta(forDate);
        bundle.setMeta(bundleMeta);
        bundle.setType(Bundle.BundleType.DOCUMENT);
        return bundle;
    }

    static Practitioner createAuthor(String hipPrefix, Properties doctors) {
        String details = (String) doctors.get(String.valueOf(Utils.randomInt(1, 3)));
        Doctor doc = Doctor.parse(details);
        Practitioner practitioner = new Practitioner();
        practitioner.setId(hipPrefix.toUpperCase() + doc.getDocId());
        practitioner.setIdentifier(Arrays.asList(getIdentifier(practitioner.getId(), "mciindia", "doctor")));
        practitioner.getName().add(getHumanName(doc.getName(), doc.getPrefix(), doc.getSuffix()));
        return practitioner;
    }

    static Patient getPatientResource(String name, Properties patients) throws Exception {
        Object patientDetail = patients.get(name);
        if (patientDetail == null) {
            throw new Exception("Can not identify patient with name: " + name);
        }
        in.projecteka.data.model.Patient patient = in.projecteka.data.model.Patient.parse((String) patientDetail);
        Patient patientResource = new Patient();
        patientResource.setId(patient.getHid());
        patientResource.setName(Collections.singletonList(getHumanName(patient.getName(), null, null)));
        patientResource.setGender(getGender(patient.getGender()));
        return patientResource;
    }

    static Encounter createEncounter(String display, String encClass, Date date) {
        Encounter encounter = new Encounter();
        Period period = new Period();
        period.setStart(date);
        encounter.setPeriod(period);
        encounter.setStatus(Encounter.EncounterStatus.FINISHED);
        Coding coding = new Coding();
        coding.setSystem("http://terminology.hl7.org/CodeSystem/v3-ActCode");
        coding.setCode(encClass);
        coding.setDisplay(display);
        encounter.setClass_(coding);
        encounter.setId(UUID.randomUUID().toString());
        return encounter;
    }

    static CodeableConcept getDiagnosticReportType() {
        CodeableConcept type = new CodeableConcept();
        Coding coding = type.addCoding();
        coding.setSystem(Constants.EKA_SCT_SYSTEM);
        coding.setCode("721981007");
        coding.setDisplay("Diagnostic Report");
        return type;
    }

    static CodeableConcept getPrescriptionType() {
        CodeableConcept type = new CodeableConcept();
        Coding coding = type.addCoding();
        coding.setSystem(Constants.EKA_SCT_SYSTEM);
        coding.setCode("440545006");
        coding.setDisplay("Prescription record");
        return type;
    }

    static void addToBundleEntry(Bundle bundle, Resource resource, boolean useIdPart) {
        String resourceType = resource.getResourceType().toString();
        String id = useIdPart ? resource.getIdElement().getIdPart() : resource.getId();
        bundle.addEntry()
                .setFullUrl(resourceType + "/" + id)
                .setResource(resource);
    }

    static Reference getReferenceToPatient(Patient patientResource) {
        Reference patientRef = new Reference();
        patientRef.setResource(patientResource);
        if (Utils.randomBool()) {
            patientRef.setDisplay(patientResource.getNameFirstRep().getNameAsSingleString());
        }
        return patientRef;
    }

    static Reference getReferenceToResource(Resource res) {
        Reference ref = new Reference();
        ref.setResource(res);
        return ref;
    }

    static DateTimeType getDateTimeType(Date date) {
        DateTimeType dateTimeType = new DateTimeType();
        dateTimeType.setValue(date);
        return dateTimeType;
    }

    @SneakyThrows
    public static String loadOrganization(String hipPrefix) {
        String fileName = "/orgs/" + hipPrefix + ".json";
        return new String(Utils.class.getResourceAsStream(fileName).readAllBytes(), StandardCharsets.UTF_8);
    }
}

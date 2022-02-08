package in.projecteka.utils.hcx;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import in.projecteka.utils.DocRequest;
import in.projecteka.utils.common.DocumentGenerator;
import in.projecteka.utils.common.FHIRUtils;
import in.projecteka.utils.common.Utils;
import lombok.SneakyThrows;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Composition;
import org.hl7.fhir.r4.model.Coverage;
import org.hl7.fhir.r4.model.CoverageEligibilityRequest;
import org.hl7.fhir.r4.model.Organization;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.ResourceType;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
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
        patientResource.addIdentifier(HcxFhirUtils. createInsurerIdentifier("http://gicofIndia.com", "BEN-101"));

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
        FHIRUtils.addToBundleEntry(bundle, author, true);
        FHIRUtils.addToBundleEntry(bundle, patientResource, false);

        composition.setSubject(patientRef);

        Composition.SectionComponent section = composition.addSection();
        section.setTitle("# Eligibility Request");
        section.setCode(HcxFhirUtils.getCoverageEligibilityResourceType()); //TODO IG - Same as Composition.type?
        CoverageEligibilityRequest cer = new CoverageEligibilityRequest();
        cer.setId(UUID.randomUUID().toString()); //request id

        section.getEntry().add(FHIRUtils.getReferenceToResource(cer));
        FHIRUtils.addToBundleEntry(bundle, cer, false);


        //load the eligibility request object
        cer.setStatus(CoverageEligibilityRequest.EligibilityRequestStatus.ACTIVE);
        cer.addPurpose(CoverageEligibilityRequest.EligibilityRequestPurpose.DISCOVERY);
        cer.addIdentifier(FHIRUtils.getIdentifier(cer.getId(), hipPrefix, "coverage-eligibility-request"));
        cer.setPatient(patientRef); //ref to patient resource

        //set service period
        Date start = Date.from(LocalDateTime.now().minusDays(1L).atZone(ZoneId.systemDefault()).toInstant());
        Date end = Date.from(LocalDateTime.now().plusDays(1L).atZone(ZoneId.systemDefault()).toInstant());
        cer.setServiced(FHIRUtils.newPeriod(start, end));

        cer.setCreated(new Date());
        cer.setProvider(FHIRUtils.getReferenceToResource(author));
        Organization insurer = fhirParser.parseResource(Organization.class, FHIRUtils.loadOrganization("gic"));
        FHIRUtils.addToBundleEntry(bundle, insurer, true);
        Reference insurerRef = FHIRUtils.getReferenceToResource(insurer);
        cer.setInsurer(insurerRef); //set insurer

        Coverage coverage = new Coverage();
        coverage.setId(UUID.randomUUID().toString());
        coverage.addIdentifier(FHIRUtils.getIdentifier("policy-"+patientResource.getId(), "gicofIndia", "policies"));
        coverage.setStatus(Coverage.CoverageStatus.ACTIVE);
        coverage.setSubscriber(patientRef); //if not the patient, then a different patient  resource should be created and embedded onto the bundle, with the right identifier
        coverage.setSubscriberId("SN-"+patientResource.getId()); //
        coverage.setBeneficiary(patientRef);
        coverage.setRelationship(HcxFhirUtils.getRelationship("self"));
        coverage.setPayor(Arrays.asList(insurerRef));
        FHIRUtils.addToBundleEntry(bundle, coverage, false);

        CoverageEligibilityRequest.InsuranceComponent insuranceComponent = cer.addInsurance();
        insuranceComponent.setFocal(true);
        insuranceComponent.setCoverage(FHIRUtils.getReferenceToResource(coverage));

        return bundle;
    }

}

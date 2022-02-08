package in.projecteka.utils.hcx;

import in.projecteka.utils.common.Constants;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.ResourceType;

import static in.projecteka.utils.hcx.Constants.SYS_HCX_DOCTYPE;
import static in.projecteka.utils.hcx.Constants.VS_FHIR_SUBSCRIBER_REL;

public class HcxFhirUtils {
    static CodeableConcept getCompositionType() {
        return new CodeableConcept(
                new Coding(SYS_HCX_DOCTYPE,
                        HcxDocumentType.CERequest.getCode(),
                        HcxDocumentType.CERequest.getDisplay()));
    }

    public static CodeableConcept getCoverageEligibilityResourceType() {
        return new CodeableConcept(
                new Coding(Constants.VS_FHIR_RES_TYPE,
                        ResourceType.CoverageEligibilityRequest.name(),
                        "Coverage Eligibility Request"));
    }

    public static Identifier createInsurerIdentifier(String insurerDomain, String value) {
        Identifier identifier = new Identifier();
        identifier.setSystem(String.format("%s/beneficiaries", insurerDomain));
        identifier.setValue(value);
        identifier.setType(new CodeableConcept(new Coding("http://terminology.hl7.org/CodeSystem/v2-0203", "SN","Subscriber Number")));
        return identifier;
    }

    static CodeableConcept getRelationship(String rel) {
        return new CodeableConcept(
                new Coding(VS_FHIR_SUBSCRIBER_REL, rel,""));
    }
}

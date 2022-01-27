package in.projecteka.utils.hcx;

import in.projecteka.utils.common.Constants;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.ResourceType;

import static in.projecteka.utils.hcx.Constants.SYS_HCX_DOCTYPE;

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
}

package in.projecteka.utils.hcx;

public enum HcxDocumentType {

    CERequest("HcxCoverageEligibilityRequest", "Coverage Eligibility Request Doc"),
    CEResponse("HcxCoverageEligibilityResponse", "Coverage Eligibility Response Doc");

    private final String code;
    private final String display;

    HcxDocumentType(String code, String display) {
        this.code = code;
        this.display = display;
    }

    public String getCode() {
        return this.code;
    }
    public String getDisplay() {
        return this.display;
    }
}

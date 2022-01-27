package in.projecteka.utils.common;

import in.projecteka.utils.DocRequest;

public interface DocumentGenerator {
    void init() throws Exception;
    void execute(DocRequest docRequest) throws Exception;
}

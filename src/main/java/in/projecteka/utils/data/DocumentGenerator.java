package in.projecteka.utils.data;

import in.projecteka.utils.DocRequest;

public interface DocumentGenerator {
    void init() throws Exception;
    void execute(DocRequest docRequest) throws Exception;
}

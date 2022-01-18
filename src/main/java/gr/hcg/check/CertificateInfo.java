package gr.hcg.check;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class CertificateInfo {

    public String issuerDN;
    public String subjectDN;

    public Date notValidBefore;
    public Date notValidAfter;

    public String signAlgorithm;
    public String serial;

    public Map<String, String> issuerOIDs = new HashMap<>();

    public Map<String, String> subjectOIDs = new HashMap<>();
}

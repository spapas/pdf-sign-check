package gr.hcg.check;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class PDFSignatureInfo {

    public Map<String, Object> entries = new HashMap<>();

    public String reason;
    public String name;
    public String subFilter;
    public String filter;
    public String contactInfo;
    public String location;

    public Date signDate;

    public boolean coversWholeDocument;
    public boolean isSelfSigned;

    public String signatureVerified;

    public CertificateInfo certificateInfo;

}

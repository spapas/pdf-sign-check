package gr.hcg.services;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.text.MessageFormat;
import java.util.UUID;

@Component
public class UploadDocumentService {

    @Value("${upload.directory}")
    private String uploadDir;

    private String getPathName(String year, String authority, String folder, String protocol, String uuid) {

        String[] params = new String[]{
                uploadDir,
                year,
                authority,
                folder

        };
        String path = MessageFormat.format("{0}/{1}/{2}/{3}/", params);
        new File(path).mkdirs();

        params = new String[]{
                uploadDir,
                year,
                authority,
                folder,
                protocol,
                uuid
        };

        return MessageFormat.format("{0}/{1}/{2}/{3}/{4}_{5}.pdf", params);

    }

    //@Transactional
    public String handleUpload(String year, String authority, String folder, String protocol, String uuid, byte [] bytes) throws IOException {
        String pathName = getPathName(year, authority, folder, protocol, uuid);

        BufferedOutputStream stream = new BufferedOutputStream(new FileOutputStream(pathName));
        FileCopyUtils.copy(new ByteArrayInputStream(bytes), stream);
        stream.close();

        return pathName;
    }

}

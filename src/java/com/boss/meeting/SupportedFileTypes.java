package com.boss.meeting;

import static com.boss.meeting.FileTypeConstants.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Administrator on 2014/12/12.
 */
public final class SupportedFileTypes {
    // Set as private to prevent instantiation
    private SupportedFileTypes() {}

    private static final List<String> SUPPORTED_FILE_LIST = new ArrayList<String>(15) {
        {
            // Add all the supported files
            add(XLS); add(XLSX);	add(DOC); add(DOCX); add(PPT); add(PPTX);
            add(ODT); add(RTF); add(TXT); add(ODS); add(ODP); add(PDF);
            add(JPG); add(JPEG); add(PNG);
        }
    };

    private static final List<String> OFFICE_FILE_LIST = new ArrayList<String>(11) {
        {
            // Add all Offile file types
            add(XLS); add(XLSX);	add(DOC); add(DOCX); add(PPT); add(PPTX);
            add(ODT); add(RTF); add(TXT); add(ODS); add(ODP);
        }
    };

    private static final List<String> IMAGE_FILE_LIST = new ArrayList<String>(3) {
        {
            // Add all image file types
            add(JPEG); add(JPG);	add(PNG);
        }
    };

    /*
     * Returns if the file with extension is supported.
     */
    public static boolean isFileSupported(String fileExtension) {
        return SUPPORTED_FILE_LIST.contains(fileExtension.toLowerCase());
    }

    /*
     * Returns if the office file is supported.
     */
    public static boolean isOfficeFile(String fileExtension) {
        return OFFICE_FILE_LIST.contains(fileExtension.toLowerCase());
    }

    public static boolean isPdfFile(String fileExtension) {
        return "pdf".equalsIgnoreCase(fileExtension);
    }

    /*
     * Returns if the iamge file is supported
     */
    public static boolean isImageFile(String fileExtension) {
        return IMAGE_FILE_LIST.contains(fileExtension.toLowerCase());
    }
}

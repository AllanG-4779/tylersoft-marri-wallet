package net.tylersoft.common.storage;

import java.util.Map;

public class FileExtensionExtractor {
    public static Map<String, String> getFileExtension(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return Map.of("extension", "", "contentType", "");
        }
        var extension = fileName.substring(fileName.lastIndexOf(".") + 1);
        var contentType = switch (extension.toLowerCase()) {
            case "jpg", "jpeg" -> "image/jpeg";
            case "png" -> "image/png";
            case "pdf" -> "application/pdf";
            case "doc", "docx" -> "application/msword";
            case "xls", "xlsx" -> "application/vnd.ms-excel";
            default -> "";
        };
        return Map.of("extension", extension, "contentType", contentType);

    }
}

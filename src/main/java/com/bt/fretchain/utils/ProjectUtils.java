package com.bt.fretchain.utils;

import org.springframework.web.multipart.MultipartFile;

public class ProjectUtils {

    public static String getFileExtension(String filename) {

        if (filename == null || filename.isEmpty()) {
            return ""; // Handle empty or null filename
        }

        int indexOfDot = filename.lastIndexOf('.');
        if (indexOfDot != -1) {
            return filename.substring(indexOfDot + 1);
        } else {
            return ""; // Handle filename without extension
        }
    }
    public static String getFolderName(String contentType) {
        if (contentType.startsWith("image/")) {
            return "images";
        } else if (contentType.startsWith("video/")) {
            return "videos";
        } else if (contentType.startsWith("audio/")) {
            return "audios";
        } else if (contentType.equals("application/pdf")) {
            return "pdfs";
        } else {
            return "documents";
        }
    }
    public static String getFileName(String filename) {
        if (filename == null || filename.isEmpty()) {
            return "";
        }

        int indexOfDot = filename.lastIndexOf('.');
        if (indexOfDot != -1) {
            return filename.substring(0, indexOfDot);
        } else {
            return filename; // Handle filename without extension
        }
    }
    public static long getCurrentTimeStamp() {
        return System.currentTimeMillis();
    }

    public static String determineFileType(MultipartFile file) {
        String contentType = file.getContentType();
        if (contentType != null) {
            // Check for image files
            if (contentType.startsWith("image/")) {
                return "images";
            }
            // Check for PDF files
            else if (contentType.equals("application/pdf")) {
                return "pdfs";
            }
            // Check for video files
            else if (contentType.startsWith("video/")) {
                return "videos";
            }
            // Check for document files (Word, Excel, PowerPoint, Text)
            else if (contentType.equals("application/msword") ||
                    contentType.equals("application/vnd.openxmlformats-officedocument.wordprocessingml.document") || // Word .docx
                    contentType.equals("application/vnd.ms-excel") ||
                    contentType.equals("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet") || // Excel .xlsx
                    contentType.equals("application/vnd.ms-powerpoint") ||
                    contentType.equals("application/vnd.openxmlformats-officedocument.presentationml.presentation") || // PowerPoint .pptx
                    contentType.equals("text/plain")) { // Text documents .txt
                return "documents";
            }
        }
        // Default case: return "others" for unknown file types
        return "others";
    }

    public static String generatePublicUrl(String bucketName,String region, String s3Key) {
        return "https://" + bucketName + ".s3." + region + ".amazonaws.com/" + s3Key;
    }
    private static void addIfNotEmpty(StringBuilder body, String label, String value) {
        if (value != null && !value.trim().isEmpty()) {
            body.append("<p><b>").append(label).append(":</b> ")
                    .append(value).append("</p>");
        }
    }



}

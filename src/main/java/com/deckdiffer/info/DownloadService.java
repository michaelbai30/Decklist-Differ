/**
 * DownloadService.java; Stores and retrieves generated text files for download.
 *
 * Goal:
 * - Store generated deck-difference text outputs
 * - Defines retrieval helpers used by DeckListDifferServer download routes
 * - Maintain an in-memory map of filename → file contents
 */

package com.deckdiffer.info;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class DownloadService {

    // Stores ALL downloadable text files generated during comparison
    private static final Map<String, String> generatedFiles = new HashMap<>();

    private DownloadService() {}

    /**
     * Saves or overwrites a text file entry.
     *
     * @param fileName name of the file including ".txt"
     * @param content full text contents of the file as string
     */
    public static void saveFile(String fileName, String content) {
        if (fileName == null || fileName.isEmpty()) return;
        if (content == null) content = "";
        generatedFiles.put(fileName, content);
    }

    /**
     * Retrieves the contents of a given file name
     *
     * @param fileName file name including ".txt"
     * @return full file text or null if missing
     */
    public static String getFile(String fileName) {
        return generatedFiles.get(fileName);
    }

    /**
     * Check whether downloadable file exists
     *
     * @param fileName file name including ".txt"
     * @return true if file is stored
     */
    public static boolean exists(String fileName) {
        return generatedFiles.containsKey(fileName);
    }
    
    public static Map<String, String> getAllFiles() {
        return Collections.unmodifiableMap(generatedFiles);
    }
}

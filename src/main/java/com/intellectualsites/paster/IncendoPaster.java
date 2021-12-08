/*
 *                      _____          _
 *                     |  __ \        | |
 *                     | |__) |_ _ ___| |_ ___ _ __
 *                     |  ___/ _` / __| __/ _ \ '__|
 *                     | |  | (_| \__ \ ||  __/ |
 *                     |_|   \__,_|___/\__\___|_|
 *
 *      Paste service used to submit data to the IncendoPasteViewer
 *                Copyright (C) 2021 IntellectualSites
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.intellectualsites.paster;

import com.google.common.base.Charsets;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class IncendoPaster {

    /**
     * Upload service URL
     */
    public static final String UPLOAD_PATH = "https://athion.net/ISPaster/paste/upload";
    /**
     * Valid paste applications
     */
    public static final Collection<String> VALID_APPLICATIONS =
            Arrays.asList("plotsquared", "fastasyncworldedit", "incendopermissions", "kvantum");

    private final Collection<PasteFile> files = new ArrayList<>();
    private final String pasteApplication;

    /**
     * Construct a new paster
     *
     * @param pasteApplication The application that is sending the paste
     */
    public IncendoPaster(final String pasteApplication) {
        if (pasteApplication == null || pasteApplication.isEmpty()) {
            throw new IllegalArgumentException("paste application cannot be null, nor empty");
        }
        if (!VALID_APPLICATIONS.contains(pasteApplication.toLowerCase(Locale.ROOT))) {
            throw new IllegalArgumentException(String.format("Unknown application name: %s", pasteApplication));
        }
        this.pasteApplication = pasteApplication;
    }

    /**
     * Automatically create a new debugpaste and upload it. Returns the URL.
     *
     * @param logFile    the log file
     * @param debugInfo  extra implementation-specific information
     * @param extraFiles extra files to be added to the paste
     * @return URL of the paste
     * @throws IOException if upload failed
     */
    public static String debugPaste(@Nonnull File logFile, @Nullable String debugInfo, @Nullable File... extraFiles) throws
            IOException {
        return debugPaste(logFile, debugInfo, extraFiles == null ? null : Arrays.asList(extraFiles));
    }

    /**
     * Automatically create a new debugpaste and upload it. Returns the URL.
     *
     * @param logFile    the log file
     * @param debugInfo  extra implementation-specific information
     * @param extraFiles extra files to be added to the paste
     * @return URL of the paste
     * @throws IOException if upload failed
     */
    public static String debugPaste(@Nonnull File logFile, @Nullable String debugInfo, @Nullable List<File> extraFiles)
            throws IOException {
        final IncendoPaster incendoPaster = new IncendoPaster("fastasyncworldedit");

        StringBuilder b = new StringBuilder();
        b.append(
                """
                        # Welcome to this paste
                        # It is meant to provide us at IntellectualSites with better information about your problem
                        """);
        b.append("\n# Server Information\n");
        b.append(debugInfo);
        b.append("\n# YAY! Now, let's see what we can find in your JVM\n");
        Runtime runtime = Runtime.getRuntime();
        RuntimeMXBean rb = ManagementFactory.getRuntimeMXBean();
        b.append("Uptime: ").append(TimeUnit.MINUTES.convert(rb.getUptime(), TimeUnit.MILLISECONDS)).append(" minutes")
                .append('\n');
        b.append("JVM Flags: ").append(rb.getInputArguments()).append('\n');
        b.append("Free Memory: ").append(runtime.freeMemory() / 1024 / 1024).append(" MB").append('\n');
        b.append("Max Memory: ").append(runtime.maxMemory() / 1024 / 1024).append(" MB").append('\n');
        b.append("Total Memory: ").append(runtime.totalMemory() / 1024 / 1024).append(" MB").append('\n');
        b.append("Available Processors: ").append(runtime.availableProcessors()).append('\n');
        b.append("Java Name: ").append(rb.getVmName()).append('\n');
        b.append("Java Version: '").append(System.getProperty("java.version")).append("'\n");
        b.append("Java Vendor: '").append(System.getProperty("java.vendor")).append("'\n");
        b.append("Operating System: '").append(System.getProperty("os.name")).append("'\n");
        b.append("OS Version: ").append(System.getProperty("os.version")).append('\n');
        b.append("OS Arch: ").append(System.getProperty("os.arch")).append('\n');
        b.append("# Okay :D Great. The paste has been created successfully.");
        incendoPaster.addFile(new IncendoPaster.PasteFile("information", b.toString()));

        try {
            final String file;
            if (Files.size(logFile.toPath()) > 14_000_000) {
                file = "latest.log is larger than 14 MB. Not uploading.";
            } else {
                file = readFile(logFile, true);
            }
            incendoPaster.addFile(new IncendoPaster.PasteFile("latest.log", file));
        } catch (IOException ignored) {
        }

        if (extraFiles != null) {
            for (File f : extraFiles) {
                incendoPaster.addFile(f);
            }
        }

        final String rawResponse;
        try {
            rawResponse = incendoPaster.upload();
        } catch (Throwable throwable) {
            throw new IOException(String.format("Failed to upload files: %s", throwable.getMessage()), throwable);
        }
        final JsonObject jsonObject = JsonParser.parseString(rawResponse).getAsJsonObject();

        if (jsonObject.has("created")) {
            final String pasteId = jsonObject.get("paste_id").getAsString();
            return String.format("https://athion.net/ISPaster/paste/view/%s", pasteId);
        } else {
            throw new IOException(String.format("Failed to upload files: %s", jsonObject.get("response").getAsString()));
        }
    }

    private static String readFile(final File file, boolean cleanIPs) throws IOException {
        final StringBuilder content = new StringBuilder();
        final List<String> lines = new ArrayList<>();
        try (final BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                lines.add(line);
            }
        }
        for (int i = Math.max(0, lines.size() - 1000); i < lines.size(); i++) {
            content.append(lines.get(i)).append("\n");
        }
        String contentStr = content.toString();
        if (cleanIPs) {
            contentStr = contentStr.replaceAll(
                    "\\b(1?[0-9]{1,2}|2[0-4][0-9]|25[0-5])\\.(1?[0-9]{1,2}|2[0-4][0-9]|25[0-5])\\.(1?[0-9]{1,2}|2[0-4][0-9]|25[0-5])\\.(1?[0-9]{1,2}|2[0-4][0-9]|25[0-5])\\b",
                    "*"
            );
        }
        return contentStr;
    }

    /**
     * Get an immutable collection containing all the files that have been added to this paster
     *
     * @return Unmodifiable collection
     */
    public final Collection<PasteFile> getFiles() {
        return Collections.unmodifiableCollection(this.files);
    }

    public void addFile(File file) throws IOException {
        addFile(file, null);
    }

    public void addFile(File file, @Nullable String name) throws IOException {
        String fileName = name != null ? name : file.getName();
        boolean cleanIPS = fileName.endsWith(".log") || fileName.endsWith(".txt") || !fileName.contains(".");
        addFile(new PasteFile(fileName, readFile(file, cleanIPS)));
    }

    /**
     * Add a file to the paster. Bypasses "checks" and IP cleansing.
     *
     * @param file File to paste
     */
    public void addFile(final PasteFile file) {
        if (file == null) {
            throw new IllegalArgumentException("File cannot be null");
        }
        // Check to see that no duplicate files are submitted
        for (final PasteFile pasteFile : this.files) {
            if (pasteFile.fileName.equalsIgnoreCase(file.getFileName())) {
                throw new IllegalArgumentException(String.format("Found duplicate file with name %s", file.getFileName()));
            }
        }
        this.files.add(file);
    }

    /**
     * Create a JSON string from the submitted information
     *
     * @return compiled JSON string
     */
    private String toJsonString() {
        final StringBuilder builder = new StringBuilder("{\n");
        builder.append("\"paste_application\": \"").append(this.pasteApplication).append("\",\n\"files\": \"");
        Iterator<PasteFile> fileIterator = this.files.iterator();
        while (fileIterator.hasNext()) {
            final PasteFile file = fileIterator.next();
            builder.append(file.getFileName());
            if (fileIterator.hasNext()) {
                builder.append(",");
            }
        }
        builder.append("\",\n");
        fileIterator = this.files.iterator();
        while (fileIterator.hasNext()) {
            final PasteFile file = fileIterator.next();
            builder.append("\"file-").append(file.getFileName()).append("\": \"")
                    .append(file.getContent().replaceAll("\"", "\\\\\"")).append("\"");
            if (fileIterator.hasNext()) {
                builder.append(",\n");
            }
        }
        builder.append("\n}");
        return builder.toString();
    }

    /**
     * Upload the paste and return the status message
     *
     * @return Status message
     * @throws Throwable any and all exceptions
     */
    public final String upload() throws Throwable {
        final URL url = new URL(UPLOAD_PATH);
        final URLConnection connection = url.openConnection();
        final HttpURLConnection httpURLConnection = (HttpURLConnection) connection;
        httpURLConnection.setRequestMethod("POST");
        httpURLConnection.setDoOutput(true);
        final byte[] content = toJsonString().getBytes(Charsets.UTF_8);
        httpURLConnection.setFixedLengthStreamingMode(content.length);
        httpURLConnection.setRequestProperty("Content-Type", "application/json");
        httpURLConnection.setRequestProperty("Accept", "*/*");
        httpURLConnection.connect();
        try (final OutputStream stream = httpURLConnection.getOutputStream()) {
            stream.write(content);
        }
        if (!httpURLConnection.getResponseMessage().contains("OK")) {
            throw new IllegalStateException(String
                    .format("Server returned status: %d %s", httpURLConnection.getResponseCode(),
                            httpURLConnection.getResponseMessage()
                    ));
        }
        final StringBuilder input = new StringBuilder();
        try (final BufferedReader inputStream = new BufferedReader(
                new InputStreamReader(httpURLConnection.getInputStream()))) {
            String line;
            while ((line = inputStream.readLine()) != null) {
                input.append(line).append("\n");
            }
        }
        return input.toString();
    }


    /**
     * Simple class that represents a paste file
     */
    public static class PasteFile {

        private final String fileName;
        private final String content;

        /**
         * Construct a new paste file
         *
         * @param fileName File name, cannot be empty, nor null
         * @param content  File content, cannot be empty, nor null
         */
        public PasteFile(final String fileName, final String content) {
            if (fileName == null || fileName.isEmpty()) {
                throw new IllegalArgumentException("file name cannot be null, nor empty");
            }
            if (content == null || content.isEmpty()) {
                throw new IllegalArgumentException("content cannot be null, nor empty");
            }
            this.fileName = fileName;
            this.content = content;
        }

        /**
         * Get the file name
         *
         * @return File name
         */
        public String getFileName() {
            return this.fileName;
        }

        /**
         * Get the file content as a single string
         *
         * @return File content
         */
        public String getContent() {
            return this.content;
        }

    }

}

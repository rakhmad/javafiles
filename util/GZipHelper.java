package com.msci.moslem.util;

import com.alutam.ziputils.ZipDecryptInputStream;
import com.alutam.ziputils.ZipEncryptOutputStream;
import net.lingala.zip4j.core.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.io.ZipOutputStream;
import net.lingala.zip4j.model.ZipParameters;
import net.lingala.zip4j.progress.ProgressMonitor;
import net.lingala.zip4j.util.Zip4jConstants;

import java.io.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;


/**
 * This class provides function related with compression.
 * We use Zip4J library (http://www.lingala.net/zip4j/)
 *
 * @author Rakhmad Azhari <r.azhari@samsung.com>
 * @version 0.0.4 - Adding compression method adapted from BlackBerry team.
 */
public class GZipHelper {

    private static GZipHelper instance;
    private String inputPath, outputPath;
    private boolean isEncrypted = false;
    private String password;

    private GZipHelper() {}

    public static GZipHelper getInstance() {
        if (instance == null) {
            return new GZipHelper();
        }
        return instance;
    }

    public void setInputPath(String inputPath) {
        this.inputPath = inputPath;
    }

    public void setOutputPath(String outputPath) {
        this.outputPath = outputPath;
    }

    public void setEncrypted(boolean encrypted) {
        isEncrypted = encrypted;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    /**
     * Create archive file using method provided by Zip4J.
     *
     * @throws IOException
     * @throws ZipException
     */
    public void compress() throws IOException, ZipException {

        // Define output file.
        ZipFile zipFile = new ZipFile(outputPath);

        /**
         * Parameters for creating compressed file
         */
        ZipParameters parameters = new ZipParameters();

        // Set compression method
        parameters.setCompressionMethod(Zip4jConstants.COMP_DEFLATE);
        // Set compression level, currently using the strongest.
        parameters.setCompressionLevel(Zip4jConstants.DEFLATE_LEVEL_NORMAL);
        parameters.setIncludeRootFolder(false);
        // Check whether user want to create a password-protected zip file
        if (isEncrypted) {
            parameters.setEncryptFiles(true);
            parameters.setEncryptionMethod(Zip4jConstants.ENC_METHOD_STANDARD);
            parameters.setPassword(password);
        }

        File input = new File(inputPath);

        // Is input a directory or a file?
        if (input.isDirectory()) {
            // Zip
            // zipFile.addFolder(input, parameters);
            zipFile.createZipFileFromFolder(input, parameters, false, 0);
        } else {
            // Zip
            // zipFile.addFile(input, parameters);
            zipFile.createZipFile(input, parameters);
        }

    }

    /**
     * Create archive file using ZipOutputStream.
     *
     * @throws ZipException
     * @throws IOException
     */
    public void compressWithOutputStream() throws ZipException, IOException {

        // Initialize ZipOutputStream
        ZipOutputStream zipOutputStream = new ZipOutputStream(new FileOutputStream(new File(outputPath)));

        InputStream inputStream = null;

        // ZipParameters
        ZipParameters parameters = new ZipParameters();
        parameters.setCompressionMethod(Zip4jConstants.COMP_DEFLATE);
        parameters.setCompressionLevel(Zip4jConstants.DEFLATE_LEVEL_NORMAL);

        if (isEncrypted) {
            parameters.setEncryptFiles(true);
            parameters.setEncryptionMethod(Zip4jConstants.ENC_METHOD_STANDARD);
            parameters.setPassword(password);
        }

        File input = new File(inputPath);
        if (input.isDirectory()) {
            File[] contents = input.listFiles();
            for (int i = 0; i < contents.length; i++) {
                addToZipOutputStream(contents[i], zipOutputStream, parameters);
            }
        } else {
            addToZipOutputStream(input, zipOutputStream, parameters);
        }

        zipOutputStream.finish();
    }

    /**
     * Archive file using ZipOutputStream, a more low-level approach, since the method provided is not
     * supported by all client.
     *
     * @param input        File Object to process.
     * @param outputStream ZipOutputStream.
     * @param parameters   Zip parameters used to create archive.
     * @throws IOException  No file/directory is found/already exists.
     * @throws ZipException Failed to create archive file.
     */
    public void addToZipOutputStream(File input, ZipOutputStream outputStream, ZipParameters parameters) throws IOException, ZipException {

        // Add input to zip file
        outputStream.putNextEntry(input, parameters);

        //Initialize input stream
        InputStream inputStream = new FileInputStream(input);
        byte[] readBuff = new byte[4096];
        int readLen;

        while ((readLen = inputStream.read(readBuff)) != -1) {
            outputStream.write(readBuff, 0, readLen);
        }

        outputStream.closeEntry();
        inputStream.close();
    }

    /**
     * Another approach proposed from BlackBerry team. Originally using ZipME as ZipInput/OutputStream.
     * Encryption handled by a library ziputils (https://bitbucket.org/matulic/ziputils/overview).
     * This library does not have maven repository, so it will be placed in MoslemDAO/lib. Set your
     * IDE accordingly.
     * <p/>
     * As a note, I am using java.util.ZipOutputStream as replacement. Although Zip4J also provides
     * its own implementation of ZipOutputStream.
     *
     * @throws IOException Fail to create zip files or can not find any file.
     */
    public void compressWithEncrypOutputStream() throws IOException {

        File input = new File(inputPath);
        File output = new File(outputPath);

        ZipEncryptOutputStream zeos = new ZipEncryptOutputStream(new FileOutputStream(output), password);
        java.util.zip.ZipOutputStream zos = new java.util.zip.ZipOutputStream(zeos);

        if (input.isDirectory()) {
            File[] files = input.listFiles();
            for (int i = 0; i < files.length; i++) {
                ZipEntry ze = new ZipEntry(files[i].getName());
                zos.putNextEntry(ze);
                InputStream is = new FileInputStream(files[i].getCanonicalPath());
                int b;
                while ((b = is.read()) != -1) {
                    zos.write(b);
                }
                zos.closeEntry();
            }
            zos.close();
        }
    }

    /**
     * Extracting password-protected file using method proposed by BlackBerry team.
     *
     * @throws IOException
     */
    public void extractWithDecryptOutputStream() throws IOException {

        File input = new File(inputPath);

        ZipDecryptInputStream zdis = new ZipDecryptInputStream(new FileInputStream(input), password);
        ZipInputStream zis = new ZipInputStream(zdis);

        ZipEntry ze;
        while ((ze = zis.getNextEntry()) != null) {
            FileOutputStream fos = new FileOutputStream(outputPath + ze.getName());
            int b;
            while ((b = zis.read()) != -1) {
                fos.write(b);
            }
            fos.close();
            zis.closeEntry();
        }
        zis.close();
    }

    /**
     * Extracting zipFile to outputPath defined.
     * For this method: inputPath is the compressed file, and outputPath is location to extract.
     * Please specify inputPath and outputPath using setter method provide.
     * <p/>
     * If you plan to extract password-protected, use setEncrypted to true and set the passwword via setPassword()
     * method.
     *
     * @throws IOException  If no file or directory found, or directory/file already exists.
     * @throws ZipException Failed to extract file.
     */
    public void extract() throws IOException, ZipException {

        ZipFile zipFile = new ZipFile(inputPath);
        if (isEncrypted) {
            zipFile.setPassword(password);
        }
        zipFile.extractAll(outputPath);

    }

    /**
     * Adding Progress information to running process.
     * Just for fun.
     *
     * @param zipFile Zip File to monitor
     */
    public void progress(ZipFile zipFile) {

        ProgressMonitor monitor = zipFile.getProgressMonitor();
        while (monitor.getState() == ProgressMonitor.STATE_BUSY) {
            System.out.println("Percent done: " + monitor.getPercentDone());
            System.out.println("File: " + monitor.getFileName());
            switch (monitor.getCurrentOperation()) {
                case ProgressMonitor.OPERATION_ADD:
                    System.out.println("Adding Files...");
                    break;
                case ProgressMonitor.OPERATION_EXTRACT:
                    System.out.println("Extracting Files..");
                    break;
            }
        }
        System.out.println("Result: " + monitor.getResult());
        if (monitor.getResult() == ProgressMonitor.RESULT_ERROR) {
            if (monitor.getException() != null) {
                monitor.getException().printStackTrace();
            } else {
                System.err.println("An error occured without any exception");
            }
        }

    }
}

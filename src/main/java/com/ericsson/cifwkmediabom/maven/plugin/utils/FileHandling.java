package com.ericsson.cifwkmediabom.maven.plugin.utils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Map.Entry;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.LineIterator;
import org.apache.maven.plugin.logging.Log;
import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;

public class FileHandling {
    public static boolean createMediaDirectory = true;
    private String versionInfo = "";
    private String header = "";
    private List<NameValuePair> versionInfoMap = new ArrayList<NameValuePair>();

    public static boolean createISODirectory(String directory, Log log){
        try {
            File file = new File(directory);
            createMediaDirectory = deleteDir(file, log);
            if (createMediaDirectory == false) {
                throw new MojoFailureException("Issue with Deleting Existing Directory and Contents for: "
                        + directory);
            }
            log.info("Creating local media directory: " + directory);
            file.mkdirs();
        } catch (Exception error) {
            log.error("Error: in creating local directory for Media Artifacts: "
                    + error);
            try {
                throw new MojoFailureException("Error: in creating local directory for Media Artifacts :"
                        + error);
            } catch (MojoFailureException error1) {
                log.error(error1);
                error1.printStackTrace();
                return false;
            }
        }
        return true;
    }

    public String getVersionInformation(MavenProject project, String mediaProduct, String mediaDrop, String restUrl, Log log) {
        versionInfoMap.add(new BasicNameValuePair("product", mediaProduct));
        versionInfoMap.add(new BasicNameValuePair("drop", mediaDrop));
        try {
            header = mediaProduct + " " + mediaDrop + " (ISO Version: " + project.getVersion() + ") ";
            versionInfo =  header + new GenericRestCalls().setUpGETRestCall(versionInfoMap, restUrl, log);
        } catch (Exception error) {
            log.error("Error in getting http response from generic rest call getAOMRstate: "
                    + error);
            try {
                throw new MojoFailureException(
                        "Error in getting http response from generic rest call getAOMRstate: "
                                + error);
            } catch (MojoFailureException error1) {
                log.error(error1);
                error1.printStackTrace();
            }
        }
        return versionInfo;
    }

    public static void writeVersionInformation(String content, String versionFile, Log log) {
        try {
            File vFile = new File(versionFile);
            if (vFile.isFile()) {
                log.info(versionFile
                        + " File already existings locally, deleting: "
                        + vFile);
                vFile.delete();
            }
            log.info("Creating version file: " + vFile);
            vFile.createNewFile();

            log.info("Writing Version Information to version file: " + vFile);
            FileWriter fw = new FileWriter(vFile.getAbsoluteFile());
            BufferedWriter bw = new BufferedWriter(fw);
            bw.write(content);
            bw.close();

        } catch (IOException error) {
            error.printStackTrace();
            try {
                throw new MojoFailureException("Error writing version information to file :"
                        + error);
            } catch (MojoFailureException error1) {
                log.error(error1);
                error1.printStackTrace();
            }
        }
    }

    public static boolean deleteDir(File dir, Log log) {
        if (dir.isDirectory()) {
            log.info("Directory Exists Deleting contents and Main Directory: "
                    + dir);
            String[] children = dir.list();
            for (int i = 0; i < children.length; i++) {
                boolean success = deleteDir(new File(dir, children[i]), log);
                if (!success) {
                    return false;
                }
            }
            dir.delete();
        }
        return true;
    }

    public static void writeDropContentsToLocalFile(String result, String contentsFile, Log log) {
        try {
            File contentfile = new File(contentsFile);
            if (contentfile.isFile()) {
                log.info(contentsFile
                        + " File already existings locally, deleting: "
                        + contentfile);
                contentfile.delete();
            }
            log.info("Creating Content local file: " + contentfile);
            contentfile.createNewFile();

            log.info("Writing Content to local file: " + contentfile);
            FileWriter fw = new FileWriter(contentfile.getAbsoluteFile());
            BufferedWriter bw = new BufferedWriter(fw);
            bw.write(result);
            bw.close();

        } catch (IOException error) {
            error.printStackTrace();
            try {
                throw new MojoFailureException("Error writing contents to File :"
                        + error);
            } catch (MojoFailureException error1) {
                log.error(error1);
                error1.printStackTrace();
            }
        }
    }

    public static String copyFindReplaceInFile(File fileName, String tempFileName, Map<String, String> details) {

        Path tempFilePath = null;
        File tempFile = null;
        try {
            tempFile = File.createTempFile(tempFileName, ".xml");
            tempFilePath = Paths.get(tempFile.getAbsolutePath());
        } catch (IOException e1) {
            e1.printStackTrace();
        }

        Charset charset = StandardCharsets.UTF_8;
        String content;

        try {
            copyContentsOfFile(fileName, tempFile);
            content = new String(Files.readAllBytes(tempFilePath), charset);
            Iterator<Entry<String, String>> iterator = details.entrySet().iterator();
            while (iterator.hasNext()) {
                Entry<String, String> next = iterator.next();
                Map.Entry<String, String> pairs = (Map.Entry<String, String>) next;
                content = content.replace(pairs.getKey().toString(), pairs.getValue().toString());
                iterator.remove();
                Files.write(tempFilePath, content.getBytes(charset));
            }
        } catch (Exception error) {
            error.printStackTrace();
        }
        return tempFile.getAbsolutePath();
    }

    public static void copyContentsOfFile(File fin, File dest) {

        FileInputStream fis = null;
        try {
            fis = new FileInputStream(fin);
        } catch (FileNotFoundException e1) {
            e1.printStackTrace();
        }
        BufferedReader in = new BufferedReader(new InputStreamReader(fis));

        FileWriter fstream = null;
        try {
            fstream = new FileWriter(dest.getAbsolutePath(), true);
        } catch (IOException e) {
            e.printStackTrace();
        }
        BufferedWriter out = new BufferedWriter(fstream);

        String aLine = null;
        try {
            while ((aLine = in.readLine()) != null) {
                out.write(aLine);
                out.newLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                in.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                out.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        try {
            in.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void replaceStringInFile(String filePath, String find, String replace){
    List<String> lines = new ArrayList<String>();
    String line = null;
    try {

        File file = new File(filePath);
        FileReader fileReader = new FileReader(file);
        BufferedReader bufferedReader = new BufferedReader(fileReader);
        LineIterator fileIterator = FileUtils.lineIterator(file, "UTF-8");

        while (fileIterator.hasNext()){
            line = fileIterator.nextLine();

            if (line.contains(find)){
                line = line.replaceAll(find, replace);
            }
            lines.add(line);
        }

        fileIterator.close();
        fileReader.close();
        bufferedReader.close();

        FileWriter fileWriter = new FileWriter(file);
        BufferedWriter out = new BufferedWriter(fileWriter);
        for(String s : lines){
             out.write(s);
             out.newLine();
        }
        out.flush();
        out.close();

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void createFileInDirectory(String fileName, Log log){

        Path path = Paths.get(fileName);
        String pathBaseDir = path.getParent().toString();
        Path basePath = Paths.get(pathBaseDir);
        if (Files.notExists(basePath)){
            File dir = new File(pathBaseDir);
            dir.mkdir();
        }
        if (Files.notExists(path)){
            File file = new File(fileName);
            try {
                file.createNewFile();
            } catch (IOException e) {
                log.info("Issue Creating Local Dependency List File: " + file);
                e.printStackTrace();
            }
        }
    }

    @SuppressWarnings("resource")
    public void writelinesToFile(String entry, String fileLocation, Log log) throws MojoExecutionException, MojoFailureException {
        File file = new File(fileLocation);
        try {
            Scanner scanner = new Scanner(file);
            boolean found = false;
            while (scanner.hasNextLine()) {
                String lineFromFile = scanner.nextLine();
                if (entry.equals(lineFromFile)) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                Writer writer = null;
                try {
                    writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(fileLocation, true), "utf-8"));
                    writer.write(entry + "\n");
                } catch (IOException ex) {
                    log.error(" ********** Issue Writing Line to File: "+ fileLocation + " ********** ");
                    ex.printStackTrace();
                } finally {
                    try {
                        writer.close();
                    } catch (IOException ex) {
                        log.error(" ********** Issue Closing File: "+ fileLocation + " ********** ");
                        ex.printStackTrace();
                    }
                }
            }
        } catch (FileNotFoundException fileNotFound) {
            log.error(" ********** Issue with scanning through file: " + fileNotFound + " ********** ");
        }
    }

    @SuppressWarnings("resource")
    public List<String> buildListFromFile(String Filename) throws IOException{
        List<String> localList = new ArrayList<String>(); 
        File filein = new File(Filename);
        BufferedReader reader = new BufferedReader(new FileReader(filein));
        String line;
        while ((line = reader.readLine()) != null) {
            if (!localList.contains(line)) {
                localList.add(line);
            }
        }
        return localList;
    }
}
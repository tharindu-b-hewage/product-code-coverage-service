/*
 *   Copyright (c) 2018, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *   WSO2 Inc. licenses this file to you under the Apache License,
 *   Version 2.0 (the "License"); you may not use this file except
 *   in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing,
 *   software distributed under the License is distributed on an
 *   "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *   KIND, either express or implied.  See the License for the
 *   specific language governing permissions and limitations
 *   under the License.
 */

package org.wso2.productcodecoverageservice.codecoverage.jenkinshandler;

import org.apache.log4j.Logger;
import org.apache.tomcat.util.http.fileupload.FileUtils;
import org.springframework.boot.system.ApplicationHome;
import org.wso2.productcodecoverageservice.Application;
import org.wso2.productcodecoverageservice.Constants.General;
import org.wso2.productcodecoverageservice.Constants.Jenkins;
import org.wso2.productcodecoverageservice.codecoverage.HTTPutils.FileDownloader;
import org.wso2.productcodecoverageservice.codecoverage.ziputils.Unzipper;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Properties;

/*
Jenkins server model to get all required execution data files and compiled class files for a product area
 */
public class JenkinsServer {

    private static final Logger log = Logger.getLogger(JenkinsServer.class);
    /*
    Temporary directory for store jacoco execution data files and compiled class files of a product area
     */
    private final Path temporaryProductAreaWorkspace;
    private String[] productAreaJenkinsJobs;
    private String jenkinsAuthString;
    private String jenkinsServerURL;

    public JenkinsServer() throws IOException {

        ApplicationHome home = new ApplicationHome(Application.class);

        Properties application = new Properties();
        try (FileInputStream propertiesStream = new FileInputStream(home.getDir() + File.separator + General.PROPERTIES_PATH)) {
            application.load(propertiesStream);

            this.jenkinsAuthString = application.getProperty(Jenkins.JENKINS_SERVER_BASE64_AUTH_STRING);
            this.jenkinsServerURL = application.getProperty(Jenkins.JENKINS_SERVER_URL);

            this.temporaryProductAreaWorkspace = Files.createDirectories(
                    Paths.get(home.getDir() + File.separator + Jenkins.WORKSPACE_DIRECTORY_PREFIX));
        }
    }

    /**
     * Store repositories of a product area for calculation purpose
     *
     * @param productAreaJenkinsJobs String list of repositories for a product area
     */
    public void setProductAreaJenkinsJobs(String[] productAreaJenkinsJobs) {

        this.productAreaJenkinsJobs = productAreaJenkinsJobs;
    }

    /**
     * Download A file from the url with basic auth. If the download process is interrupted, clear all remaining data
     */
    private void downloadFile(String URL, File saveFile, String basicAuthString) throws IOException {

        try {
            FileDownloader.downloadWithBasicAuth(URL, saveFile, basicAuthString);
        } catch (IOException e) {
            if (saveFile.exists()) FileUtils.forceDelete(saveFile);

            /* Throw the exception to stop downloading class files for this component */
            throw e;
        }
    }

    /**
     * Download jacoco source files and save them in the relevant locations
     *
     * @param jenkinsJob
     * @return location of the jacoco data file
     */
    private String downloadJacocoSources(String jenkinsJob) throws IOException {

        String jacocoSourcesFileRequestURL = this.jenkinsServerURL
                + General.URL_SEPARATOR
                + jenkinsJob
                + General.URL_SEPARATOR
                + Jenkins.LAST_SUCCESSFUL_BUILD
                + General.URL_SEPARATOR
                + Jenkins.JACOCO_RESOURCES_ZIP;

        String[] jenkinsJobSplit = jenkinsJob.split(General.URL_SEPARATOR);
        String jenkinsJobName = jenkinsJobSplit[jenkinsJobSplit.length - 1];
        String dataFileSavePath = this.temporaryProductAreaWorkspace.toAbsolutePath()
                + File.separator
                + jenkinsJobName
                + File.separator
                + Jenkins.JACOCO_SOURCES_FILE_ZIP;
        File dataFileLocation = new File(dataFileSavePath);

        /* Clear existing file */
        if (dataFileLocation.exists()) FileUtils.forceDelete(dataFileLocation);

        log.info("Downloading " + jacocoSourcesFileRequestURL);
        downloadFile(jacocoSourcesFileRequestURL, dataFileLocation, this.jenkinsAuthString);

        String unzippedFolderPath = dataFileSavePath.replace(File.separator + Jenkins.JACOCO_SOURCES_FILE_ZIP, "");
        File unzippedFolder = new File(unzippedFolderPath);
        if (!unzippedFolder.exists()) unzippedFolder.mkdirs();

        Unzipper.unzipFile(dataFileSavePath,
                unzippedFolder);
        /*
        If jacoco.exec file exists for the component, save the path for merging process
         */
        String unzippedJacocoExecFilePath = unzippedFolderPath
                + File.separator + Jenkins.EXTRACTED_JACOCO_FOLDER
                + File.separator + Jenkins.JACOCO_DATAFILE_NAME;
        if ((new File(unzippedJacocoExecFilePath)).exists()) {
            return unzippedJacocoExecFilePath;
        } else {
            log.warn("Could not find jacoco.exec file in " + unzippedJacocoExecFilePath + ". Skipped");
            return null;
        }

        //fileCopy(dataFileSavePath.replace(Jenkins.JACOCO_SOURCES_FILE_ZIP, Jenkins.JACOCO_DATAFILE_NAME), )
    }

    /**
     * Download all jacoco data files from the last successful build in Jenkins server
     */
    public ArrayList<String> downloadCoverageFiles() {

        ArrayList<String> jacocoDataFiles = new ArrayList<>();
        for (String eachJenkinsJob : this.productAreaJenkinsJobs) {

            try {
                String jacocoDataFile = downloadJacocoSources(eachJenkinsJob);
                if (jacocoDataFile != null) {
                    jacocoDataFiles.add(jacocoDataFile);
                }
            } catch (IOException e) {
                log.error("Error while downloading coverage files from jenkins. Skipping " + eachJenkinsJob);
            } catch (Exception e) {
                log.fatal("Server connection error. Skipping " + eachJenkinsJob);
            }
        }

        return jacocoDataFiles;
    }

    /**
     * Download a zip file containing compiled classes from the last successful build in Jenkins server
     *
     * @param jenkinsJob Name in the jenkins for a required repository
     */
    private void downloadCompiledClassesZip(String jenkinsJob) throws IOException {

        String compiledClassesZipRequestURL = this.jenkinsServerURL
                + General.URL_SEPARATOR
                + jenkinsJob
                + General.URL_SEPARATOR
                + Jenkins.LAST_SUCCESSFUL_BUILD
                + General.URL_SEPARATOR
                + Jenkins.CLASSES_ZIP;

        String[] jenkinsJobSplit = jenkinsJob.split(General.URL_SEPARATOR);
        String jenkinsJobName = jenkinsJobSplit[jenkinsJobSplit.length - 1];
        String dataFileSavePath = this.temporaryProductAreaWorkspace.toAbsolutePath()
                + File.separator
                + Jenkins.COMPILED_CLASSES_FOLDER
                + File.separator
                + jenkinsJobName
                + File.separator
                + Jenkins.COMPILED_CLASSES_FILE_NAME;
        File compiledClassesZip = new File(dataFileSavePath);

        /* Clear existing file */
        if (compiledClassesZip.exists()) FileUtils.forceDelete(compiledClassesZip);

        log.info("Downloading " + compiledClassesZipRequestURL);

        downloadFile(compiledClassesZipRequestURL, compiledClassesZip, this.jenkinsAuthString);
    }

    /**
     * Get the path to the folder containing jacoco report files and compiled class files
     *
     * @return Temporary folder to be used for downloading class, source and execution data file for the calculation
     */
    public Path getTemporaryProductAreaWorkspace() {

        return this.temporaryProductAreaWorkspace;
    }

    /**
     * Remove temporary workspace
     *
     * @throws IOException Unable to delete the temporary workspace
     */
    public void clearTemporaryData() throws IOException {

        FileUtils.deleteDirectory(this.temporaryProductAreaWorkspace.toFile());
    }
}

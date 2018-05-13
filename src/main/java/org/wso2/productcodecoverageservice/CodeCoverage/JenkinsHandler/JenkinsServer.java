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

package org.wso2.productcodecoverageservice.CodeCoverage.JenkinsHandler;

import org.apache.log4j.Logger;
import org.apache.tomcat.util.http.fileupload.FileUtils;
import org.springframework.boot.system.ApplicationHome;
import org.wso2.productcodecoverageservice.Application;
import org.wso2.productcodecoverageservice.CodeCoverage.HTTPUtils.FileDownloader;
import org.wso2.productcodecoverageservice.Constants.General;
import org.wso2.productcodecoverageservice.Constants.Jenkins;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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
        application.load(new FileInputStream(home.getDir() + File.separator + General.PROPERTIES_PATH));

        this.jenkinsAuthString = application.getProperty(Jenkins.JENKINS_SERVER_BASE64_AUTH_STRING);
        this.jenkinsServerURL = application.getProperty(Jenkins.JENKINS_SERVER_URL);

        this.temporaryProductAreaWorkspace = Files.createDirectories(
                Paths.get(home.getDir() + File.separator + Jenkins.WORKSPACE_DIRECTORY_PREFIX));
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
     * Download a given Jacoco data file from the last successful build in Jenkins server
     *
     * @param jenkinsJob Name in the jenkins for a required repository
     */
    private void downloadJacocoDataFile(String jenkinsJob) throws IOException {

        String jacocoDataFileRequestURL = this.jenkinsServerURL
                + General.URL_SEPERATOR
                + jenkinsJob
                + General.URL_SEPERATOR
                + Jenkins.LAST_SUCCESSFUL_BUILD
                + General.URL_SEPERATOR
                + Jenkins.JACOCO_DATA_FILE;

        String[] jenkinsJobSplit = jenkinsJob.split(General.URL_SEPERATOR);
        String jenkinsJobName = jenkinsJobSplit[jenkinsJobSplit.length - 1];
        String dataFileSavePath = this.temporaryProductAreaWorkspace.toAbsolutePath()
                + File.separator
                + Jenkins.JACOCO_DATAFILES_FOLDER
                + File.separator
                + jenkinsJobName
                + File.separator
                + Jenkins.JACOCO_DATAFILE_NAME;
        File dataFileLocation = new File(dataFileSavePath);

        /* Clear existing file */
        if (dataFileLocation.exists()) FileUtils.forceDelete(dataFileLocation);

        log.info("Downloading " + jacocoDataFileRequestURL);

        /* If an error occured during download process delete any existing downloaded data*/
        try {
            FileDownloader.downloadWithBasicAuth(jacocoDataFileRequestURL, dataFileLocation, this.jenkinsAuthString);
        } catch (IOException e) {
            if (dataFileLocation.exists()) FileUtils.forceDelete(dataFileLocation);

            /* Throw the exception to stop downloading class files for this component */
            throw e;
        }
    }

    /**
     * Download all jacoco data files from the last successful build in Jenkins server
     */
    public void downloadCoverageFiles() throws IOException {

        for (String eachJenkinsJob : this.productAreaJenkinsJobs) {

            try {
                downloadJacocoDataFile(eachJenkinsJob);
                downloadCompiledClassesZip(eachJenkinsJob);
            } catch (IOException e) {
                log.warn("Error while downloading coverage files from jenkins. Skipping " + eachJenkinsJob);
            }
        }
    }

    /**
     * Download a zip file containing compiled classes from the last successful build in Jenkins server
     *
     * @param jenkinsJob Name in the jenkins for a required repository
     */
    private void downloadCompiledClassesZip(String jenkinsJob) throws IOException {

        String compiledClassesZipRequestURL = this.jenkinsServerURL
                + General.URL_SEPERATOR
                + jenkinsJob
                + General.URL_SEPERATOR
                + Jenkins.LAST_SUCCESSFUL_BUILD
                + General.URL_SEPERATOR
                + Jenkins.CLASSES_ZIP;

        String[] jenkinsJobSplit = jenkinsJob.split(General.URL_SEPERATOR);
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

        /* If an error occured during download process delete any existing downloaded data*/
        try {
            FileDownloader.downloadWithBasicAuth(compiledClassesZipRequestURL, compiledClassesZip, this.jenkinsAuthString);
        } catch (IOException e) {
            if (compiledClassesZip.exists()) FileUtils.forceDelete(compiledClassesZip);
            throw e;
        }
    }

    /**
     * Get the path to the folder containing jacoco report files and compiled class files
     *
     * @return
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

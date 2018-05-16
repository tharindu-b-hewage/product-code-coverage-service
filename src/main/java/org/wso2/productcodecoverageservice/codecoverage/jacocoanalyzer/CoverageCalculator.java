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

package org.wso2.productcodecoverageservice.codecoverage.jacocoanalyzer;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.jacoco.core.analysis.Analyzer;
import org.jacoco.core.analysis.CoverageBuilder;
import org.jacoco.core.tools.ExecFileLoader;
import org.springframework.boot.system.ApplicationHome;
import org.wso2.productcodecoverageservice.Application;
import org.wso2.productcodecoverageservice.Constants.Coverage;
import org.wso2.productcodecoverageservice.Constants.General;
import org.wso2.productcodecoverageservice.Constants.Jenkins;
import org.wso2.productcodecoverageservice.codecoverage.CodeCoverageController;
import org.wso2.productcodecoverageservice.codecoverage.ziputils.Unzipper;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Iterator;

/*
 A Processor for jacoco and repository build files for code coverage
 */
public class CoverageCalculator {

    private static final Logger log = Logger.getLogger(CodeCoverageController.class);
    private final String jacocoDatafiles;
    private final String compiledClassesZipFiles;
    private final String sourcesZipFiles;
    private final ExecFileLoader dataFileLoader = new ExecFileLoader();
    private final String productID;

    public CoverageCalculator(Path coverageFiles, String productID) {

        this.jacocoDatafiles = coverageFiles.toString() + File.separator + Jenkins.JACOCO_DATAFILES_FOLDER;
        this.compiledClassesZipFiles = coverageFiles.toString() + File.separator + Jenkins.COMPILED_CLASSES_FOLDER;
        this.sourcesZipFiles = coverageFiles.toString() + File.separator + Jenkins.SOURCE_FILES_FOLDER;
        this.productID = productID;
    }

    /**
     * Get all the execution files belong to the product area jobs and create a single execution data file
     *
     * @throws IOException If execution data files cannot be found, loaded or the merged data file cannot be saved
     */
    public void mergeDataFiles() throws IOException {

        /* Get an iterator for all jacoco data files in the directory */
        String[] dataFileExtension = {Coverage.DATA_FILE_EXTENSION};
        Iterator<File> dataFiles = (FileUtils.listFiles(new File(this.jacocoDatafiles), dataFileExtension, true)).iterator();

        if (!dataFiles.hasNext()) throw new IOException("Zero jacoco data files available");

        while (dataFiles.hasNext()) {
            this.dataFileLoader.load(dataFiles.next());
        }

        String mergedDataFilePath = this.jacocoDatafiles + File.separator + General.STEP_BACK + Coverage.MERGED_JACOCO_DATA_FILE;
        this.dataFileLoader.save(new File(mergedDataFilePath), false);

    }

    /**
     * Get line coverage ratio and number of lines to cover for the product area component
     *
     * @param component Name of the component
     * @return A ComponentCoverage object containing line coverage ratio and number of lines to cover
     */
    private ComponentCoverage getComponentCoverageData(String component) throws IOException {

        File componentClassesZipFile = new File(
                this.compiledClassesZipFiles + File.separator + component + File.separator + Jenkins.COMPILED_CLASSES_FILE_NAME);
        File sourcesZipFile = new File(
                this.sourcesZipFiles + File.separator + component + File.separator + Jenkins.SOURCE_FILE_ZIP);
        if (!componentClassesZipFile.exists() || !sourcesZipFile.exists()) {
            throw new IOException("Classes or source files cannot be found");
        }
        File classExtractFolder = new File(
                this.compiledClassesZipFiles + File.separator + component + File.separator + Coverage.EXTRACTED_CLASS_FOLDER);
        File sourceExtractFolder = new File(
                this.sourcesZipFiles + File.separator + component + File.separator + Coverage.EXTRACTED_SOURCE_FOLDER);
        if (!classExtractFolder.exists()) classExtractFolder.mkdirs();
        if (!sourceExtractFolder.exists()) sourceExtractFolder.mkdirs();

        Unzipper.unzipFile(componentClassesZipFile.toString(), classExtractFolder);
        Unzipper.unzipFile(sourcesZipFile.toString(), sourceExtractFolder);

        CoverageBuilder coverageBuilder = new CoverageBuilder();
        Analyzer analyzer = new Analyzer(this.dataFileLoader.getExecutionDataStore(), coverageBuilder);

        /* Use org folder in the extracted folder as it contain the class files required*/
        analyzer.analyzeAll(new File(classExtractFolder.toString()
                + File.separator + Coverage.CLASSES
                + File.separator + Coverage.ORG
                + File.separator + Coverage.WSO2));
        /*
        Calculate and prepare output data
         */
        String lineCoverageRatio = Double.toString(coverageBuilder.getBundle(component).getLineCounter().getCoveredRatio());
        String linesToCover = Integer.toString(coverageBuilder.getBundle(component).getLineCounter().getTotalCount());

        return new ComponentCoverage(lineCoverageRatio, linesToCover);
    }

    /**
     * For each of the product component, generate html coverage reports
     */
    public void generateCoverageReports(String[] productAreaComponents) {

        for (String component : productAreaComponents) {

            String classExtractFolder = this.compiledClassesZipFiles + File.separator + component + File.separator + Coverage.EXTRACTED_CLASS_FOLDER;
            String sourceExtractFolder = this.sourcesZipFiles + File.separator + component + File.separator + Coverage.EXTRACTED_SOURCE_FOLDER;

            ApplicationHome home = new ApplicationHome(Application.class);
            String componentName = (new File(component)).getName();

            ReportGenerator report = new ReportGenerator();
            report.setExecFileLoader(this.dataFileLoader);
            report.setClassesDirectory(new File(classExtractFolder + File.separator + Coverage.CLASSES));
            report.setSourceDirectory(new File(sourceExtractFolder + File.separator + Coverage.SOURCES));
            report.setReportDirectory(new File(
                    home.getDir()
                            + File.separator + Coverage.COVERAGE_REPORTS_DIRECTORY
                            + File.separator + this.productID
                            + File.separator + componentName));

            try {
                report.createReport();
            } catch (IOException e) {
                log.warn("Error creating report for " + componentName + ". Cleaning generated files");
                try {
                    FileUtils.cleanDirectory(report.getReportDirectory());
                } catch (IOException f) {
                    log.warn("Error cleaning generated report files. Maybe files were not generated at all");
                }
            }
        }
    }

    /**
     * Get line coverage ratio and lines to cover count in each product component in the product area
     *
     * @return A HashMap containing coverage information for each of the product area component
     */
    public HashMap<String, ComponentCoverage> getProductCoverageData(String[] productAreaComponents) {

        HashMap<String, ComponentCoverage> productCoverageData = new HashMap<>();

        for (String eachComponent : productAreaComponents) {
            log.info("Calculating coverage data for " + eachComponent);
            try {
                ComponentCoverage componentCoverageData = getComponentCoverageData(eachComponent);
                /*
                As each component is in the format of 'folder_name/job_name', format it and use only the job name
                 */
                String[] eachComponentSplit = eachComponent.split(General.URL_SEPARATOR);
                String eachComponentJobName = eachComponentSplit[eachComponentSplit.length - 1];

                productCoverageData.put(eachComponentJobName, componentCoverageData);
            } catch (IOException e) {
                log.info("Skipping " + eachComponent + " due to coverage calculation error");
            }
        }

        return productCoverageData;
    }
}

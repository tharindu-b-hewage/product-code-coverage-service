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

package org.wso2.productcodecoverageservice.CodeCoverage.JacocoAnalyzer;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.jacoco.core.analysis.Analyzer;
import org.jacoco.core.analysis.CoverageBuilder;
import org.jacoco.core.tools.ExecFileLoader;
import org.wso2.productcodecoverageservice.CodeCoverage.CodeCoverageController;
import org.wso2.productcodecoverageservice.CodeCoverage.ZipUtils.Unzipper;
import org.wso2.productcodecoverageservice.Constants.Coverage;
import org.wso2.productcodecoverageservice.Constants.General;
import org.wso2.productcodecoverageservice.Constants.Jenkins;

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
    private final ExecFileLoader dataFileLoader = new ExecFileLoader();
    private String mergedDataFile = null;

    public CoverageCalculator(Path coverageFiles) {

        this.jacocoDatafiles = coverageFiles.toString() + File.separator + Jenkins.JACOCO_DATAFILES_FOLDER;
        this.compiledClassesZipFiles = coverageFiles.toString() + File.separator + Jenkins.COMPILED_CLASSES_FOLDER;
    }

    /**
     * Get all the execution files belong to the product area jobs and create a single execution data file
     *
     * @throws IOException
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

        this.mergedDataFile = mergedDataFilePath;
    }

    /**
     * Get line coverage ratio and number of lines to cover for the product area component
     *
     * @param component Name of the component
     * @return A hashmap containing line coverage ratio and number of lines to cover
     */
    private HashMap<String, String> getComponentCoverageData(String component) throws IOException {

        HashMap<String, String> coverageData = new HashMap<>();

        File componentClassesZipFile = new File(this.compiledClassesZipFiles + File.separator + component + File.separator + Jenkins.COMPILED_CLASSES_FILE_NAME);
        if (!componentClassesZipFile.exists()) {
            throw new IOException("Classes zip file not found");
        }
        File classExtractFolder = new File(this.compiledClassesZipFiles + File.separator + component + File.separator + Coverage.EXTRACTED_CLASS_FOLDER);
        if (!classExtractFolder.exists()) classExtractFolder.mkdirs();

        Unzipper.unzipFile(componentClassesZipFile.toString(), classExtractFolder);

        CoverageBuilder coverageBuilder = new CoverageBuilder();
        Analyzer analyzer = new Analyzer(this.dataFileLoader.getExecutionDataStore(), coverageBuilder);

        /* Use org folder in the extracted folder as it contain the class files required*/
        analyzer.analyzeAll(new File(classExtractFolder.toString() + File.separator + Coverage.ORG_FOLDER));

        String lineCoverageRatio = Double.toString(coverageBuilder.getBundle(component).getLineCounter().getCoveredRatio());
        String linesToCover = Integer.toString(coverageBuilder.getBundle(component).getLineCounter().getTotalCount());

        coverageData.put(Coverage.LINE_COVERAGE_RATIO, lineCoverageRatio);
        coverageData.put(Coverage.LINES_TO_COVER, linesToCover);

        return coverageData;
    }

    /**
     * Get line coverage ratio and lines to cover count in each product component in the product area
     *
     * @return
     */
    public HashMap<String, HashMap<String, String>> getProductCoverageData(String[] productAreaComponents) {

        HashMap<String, HashMap<String, String>> productCoverageData = new HashMap<>();

        for (String eachComponent : productAreaComponents) {
            log.info("Calculating coverage data for " + eachComponent);
            try {
                HashMap<String, String> componentCoverageData = getComponentCoverageData(eachComponent);
                productCoverageData.put(eachComponent, componentCoverageData);
            }
            catch (IOException e) {
                log.info("Skipping " + eachComponent + " due to coverage calculation error");
            }
        }

        return productCoverageData;
    }
}

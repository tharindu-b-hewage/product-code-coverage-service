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

import org.jacoco.core.analysis.Analyzer;
import org.jacoco.core.analysis.CoverageBuilder;
import org.jacoco.core.analysis.IBundleCoverage;
import org.jacoco.core.tools.ExecFileLoader;
import org.jacoco.report.DirectorySourceFileLocator;
import org.jacoco.report.FileMultiReportOutput;
import org.jacoco.report.IReportGroupVisitor;
import org.jacoco.report.IReportVisitor;
import org.jacoco.report.html.HTMLFormatter;
import org.wso2.productcodecoverageservice.Constants;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;

public class ReportGenerator {

    private File reportDirectory;
    private ExecFileLoader execFileLoader;
    private File sourceDirectory;
    private File classesDirectory;

    public static void main(String[] args) throws IOException{

        String classesPath = "/home/tharindu/Desktop/coverage-test/product_apim_2x/classes";
        String sourcesPath = "/home/tharindu/Desktop/coverage-test/product_apim_2x/sources";
        String executionPath = "/home/tharindu/Desktop/coverage-test/product_apim_2x/jacoco.exec";
        String reportPath = "/home/tharindu/Desktop/coverage-test/product_apim_2x/report";

        ExecFileLoader loader = new ExecFileLoader();
        loader.load(new File(executionPath));

        ReportGenerator report = new ReportGenerator();
        report.setReportDirectory(new File(reportPath));
        report.setSourceDirectory(new File(sourcesPath));
        report.setExecFileLoader(loader);
        report.setClassesDirectory(new File(classesPath));

        report.createReport();
    }

    public void setClassesDirectory(File classesDirectory) {

        this.classesDirectory = classesDirectory;
    }

    public void setExecFileLoader(ExecFileLoader execFileLoader) {

        this.execFileLoader = execFileLoader;
    }

    public void setReportDirectory(File reportDirectory) {

        this.reportDirectory = reportDirectory;
    }

    public void setSourceDirectory(File sourceDirectory) {

        this.sourceDirectory = sourceDirectory;
    }

    public File getReportDirectory() {

        return reportDirectory;
    }

    public void createReport()
            throws IOException {

        // Create a concrete report visitor based on some supplied
        // configuration. In this case we use the defaults
        final HTMLFormatter htmlFormatter = new HTMLFormatter();
        final IReportVisitor visitor = htmlFormatter
                .createVisitor(new FileMultiReportOutput(reportDirectory));

        // Initialize the report with all of the execution and session
        // information. At this point the report doesn't know about the
        // structure of the report being created
        visitor.visitInfo(execFileLoader.getSessionInfoStore().getInfos(),
                execFileLoader.getExecutionDataStore().getContents());

        // Find how many groups are needed to be created at the beginning stage
        // source directory = org.wso2.component_name.*
        String modulesPath = this.classesDirectory + File.separator + Constants.Coverage.ORG + File.separator + Constants.Coverage.WSO2;
        traverseAndGroupModules(modulesPath, visitor);

        visitor.visitEnd();
    }

    private void traverseAndGroupModules(String modulePath, IReportGroupVisitor visitor) throws IOException {

        String[] dataFileExtension = {Constants.Coverage.CLASS_FILE_EXTENSION};
        Iterator<File> classFiles = (org.apache.commons.io.FileUtils.listFiles(new File(modulePath), dataFileExtension, false)).iterator();
        File[] subdirectories = new File(modulePath).listFiles(File::isDirectory);

        if (!classFiles.hasNext() && subdirectories.length > 0) { // Create group
            IReportGroupVisitor groupVisitor = visitor.visitGroup(modulePath.replace(this.classesDirectory + File.separator, "").replace(File.separator, "."));

            for (File eachSubModule : subdirectories) {
                traverseAndGroupModules(eachSubModule.getAbsolutePath(), groupVisitor);
            }
        } else if (classFiles.hasNext()) {
            File module = new File(modulePath);
            IBundleCoverage bundleCoverage = getBundleCoverage(module);
            visitor.visitBundle(bundleCoverage, new DirectorySourceFileLocator(
                    this.sourceDirectory, "utf-8", 4));
        }
    }

    private IBundleCoverage getBundleCoverage(File classesDirectory) throws IOException {

        final CoverageBuilder coverageBuilder = new CoverageBuilder();
        final Analyzer analyzer = new Analyzer(
                execFileLoader.getExecutionDataStore(), coverageBuilder);

        analyzer.analyzeAll(classesDirectory);

        return coverageBuilder.getBundle(classesDirectory.getAbsolutePath().replace(this.classesDirectory.getAbsolutePath() + File.separator, "").replace(File.separator, "."));
    }
}
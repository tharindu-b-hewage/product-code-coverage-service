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

package org.wso2.productcodecoverageservice.sourcefilereport;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.system.ApplicationHome;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.wso2.productcodecoverageservice.Application;
import org.wso2.productcodecoverageservice.Constants;
import org.wso2.productcodecoverageservice.Constants.Report;
import org.wso2.productcodecoverageservice.codecoverage.CodeCoverageController;
import org.wso2.productcodecoverageservice.sourcefilereport.jsonobject.SourceFileJson;
import org.wso2.productcodecoverageservice.sourcefilereport.jsonobject.SourceFileJsonObject;
import org.wso2.productcodecoverageservice.sourcefilereport.jsonobject.SourceFileReport;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Properties;

@RestController
public class SourceFileReportController {

    private static final Logger log = Logger.getLogger(CodeCoverageController.class);
    @Autowired
    Environment environment;

    /**
     * Get download information of a hosted coverage report for a particular product
     *
     * @param sourceFileJsonObject Details of the classes which needs the source file details
     * @return A CoverageReport object with the hosted server information
     */
    @RequestMapping(value = {Report.GET_SOURCE_FILE_REPORT_REQUEST}, method = {RequestMethod.POST})
    public ArrayList<SourceFileReport> getComponentCoverageReport(@RequestBody SourceFileJsonObject sourceFileJsonObject) {

        log.info("Initializing source file analyzing process.");

        ApplicationHome home = new ApplicationHome(Application.class);

        String coverageReportFolderPath = home.getDir() + File.separator + Constants.Coverage.COVERAGE_REPORTS_DIRECTORY;

        HashMap<String, HashMap<String, String[]>> fileData = new HashMap<>();
        HashMap<String, String> classFilePaths = new HashMap<>();
        ArrayList<SourceFileReport> list = new ArrayList<>();

        Properties properties = new Properties();
        try {
            properties.load(new FileReader(home.getDir() + File.separator + Constants.General.PROPERTIES_PATH));
            String findbugsPath = properties.getProperty(Constants.General.FINDBUGS_FOLDER_PATH);
            log.info("Findbugs folder path is set to " + findbugsPath + ".");
            // process xml
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/nonvalidating/load-dtd-grammar", false);
            factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            DocumentBuilder documentBuilder = factory.newDocumentBuilder();
            for (SourceFileJson data : sourceFileJsonObject.getSourceFileJsons()) {
                String key = data.getPackageName() + File.separator + data.getClassName();
                if (!fileData.containsKey(key)) {
                    String componentName = data.getComponentName();
                    switch (componentName) {
                        case "carbon-apimgt":
                            componentName = "carbon-apimgt_6.x";
                            break;
                        case "product-apim":
                            componentName = "product-apim_2.x";
                            break;
                        case "analytics-apim":
                            componentName = "analytics-apim_2.x";
                            break;
                    }
                    String compiledClassesFolderPath = coverageReportFolderPath + File.separator + data.getProductId()
                            + File.separator + componentName + File.separator + Constants.Coverage.COMPILED_CLASS_FOLDER;
                    getClassFilesPaths(new File(compiledClassesFolderPath), compiledClassesFolderPath, classFilePaths);
                    String sourceXmlPath = coverageReportFolderPath + File.separator + data.getProductId()
                            + File.separator + componentName + File.separator + Constants.Coverage.XML_REPORT_FILE;
                    File sourceXml = new File(sourceXmlPath);
                    if (sourceXml.exists()) {
                        Document document = documentBuilder.parse(sourceXml);
                        NodeList packageNodes = document.getElementsByTagName(Constants.Coverage.PACKAGE);
                        for (int i = 0; i < packageNodes.getLength(); i++) {
                            Element packageElement = (Element) packageNodes.item(i);
                            NodeList sourceNodes = packageElement.getElementsByTagName(Constants.Coverage.SOURCE_FILE);
                            for (int j = 0; j < sourceNodes.getLength(); j++) {
                                Element sourceElement = (Element) sourceNodes.item(j);
                                NodeList counterNodes = sourceElement.getElementsByTagName(Constants.Coverage.COUNTER);
                                HashMap<String, String[]> sourceFileReport = new HashMap<>();
                                String sourceName = sourceElement.getAttribute(Constants.Coverage.NAME);
                                String className = sourceName.replace(".java", ".class");
                                for (int k = 0; k < counterNodes.getLength(); k++) {
                                    Element dataNode = (Element) counterNodes.item(k);
                                    String type = dataNode.getAttribute(Constants.Coverage.TYPE);
                                    String missed = dataNode.getAttribute(Constants.Coverage.MISSED);
                                    String covered = dataNode.getAttribute(Constants.Coverage.COVERED);
                                    String[] values = {missed, covered};
                                    sourceFileReport.put(type, values);
                                }
                                fileData.put(packageElement.getAttribute(Constants.Coverage.NAME) + File.separator +
                                        sourceName, sourceFileReport);
                            }
                        }
                    }
                }

                if (fileData.containsKey(key)) {
                    log.info("Retrieving coverage data for " + key + ".");
                    HashMap sourceData = fileData.get(key);
                    SourceFileReport.LineCoverageData lineCoverageReport = new SourceFileReport
                            .LineCoverageData("", "");
                    SourceFileReport.MethodCoverageData methodCoverageReport = new SourceFileReport
                            .MethodCoverageData("", "");
                    SourceFileReport.BranchCoverageData branchCoverageReport = new SourceFileReport
                            .BranchCoverageData("", "");
                    SourceFileReport.InstructionCoverageData instructionCoverageReport = new SourceFileReport
                            .InstructionCoverageData("", "");

                    if (sourceData.get(Constants.Coverage.LINE) != null) {
                        String[] lineCoverageData = (String[]) sourceData.get(Constants.Coverage.LINE);
                        lineCoverageReport = new SourceFileReport.LineCoverageData
                                (lineCoverageData[0], lineCoverageData[1]);
                    }
                    if (sourceData.get(Constants.Coverage.METHOD) != null) {
                        String[] methodCoverageData = (String[]) sourceData.get(Constants.Coverage.METHOD);
                        methodCoverageReport = new SourceFileReport.MethodCoverageData
                                (methodCoverageData[0], methodCoverageData[1]);
                    }
                    if (sourceData.get(Constants.Coverage.INSTRUCTION) != null) {
                        String[] instructionCoverageData = (String[]) sourceData.get(Constants.Coverage.INSTRUCTION);
                        instructionCoverageReport = new SourceFileReport.InstructionCoverageData
                                (instructionCoverageData[0], instructionCoverageData[1]);
                    }
                    if (sourceData.get(Constants.Coverage.BRANCH) != null) {
                        String[] branchCoverageData = (String[]) sourceData.get(Constants.Coverage.BRANCH);
                        branchCoverageReport = new SourceFileReport.BranchCoverageData
                                (branchCoverageData[0], branchCoverageData[1]);
                    }
                    ArrayList<String> issueList = getIssues(findbugsPath, classFilePaths.get(data.getClassName().replace(".java"
                            , ".class")));
                    log.info("Findbugs analysis succesful for " + data.getClassName() + ".");
                    list.add(new SourceFileReport(key, issueList, instructionCoverageReport,
                            branchCoverageReport, lineCoverageReport, methodCoverageReport));
                }
            }
        } catch (Exception ex) {
            log.error("Error occured while getting data for source files." + ex.getMessage());
        }
        return list;
    }

    /**
     * This method generate a string of findbugs analysis of class
     *
     * @param findBugsFolderPath findbugs folderpath
     * @param classFilePath      Path of the class file
     */
    public ArrayList<String> getIssues(String findBugsFolderPath, String classFilePath) {
        ArrayList<String> issueList = new ArrayList<>();
        try {
            String command = findBugsFolderPath + " analyze -low " + classFilePath;
            Process findbugsProcess = Runtime.getRuntime().exec(command);

            BufferedReader processOutput = new BufferedReader(new InputStreamReader(findbugsProcess.getInputStream()));
            String issue;
            while ((issue = processOutput.readLine()) != null) {
                issueList.add(issue);
            }
            return issueList;
        } catch (IOException ex) {
            log.error("Error occured while getting findbugs results." + ex.getMessage());
            return issueList;
        }
    }

    /**
     * This method collects the paths of class files
     *
     * @param sourceFile      path of the file to zip
     * @param sourceFileName  name of the file to zip
     * @param classFilesPaths HashMap to store paths
     */
    private static void getClassFilesPaths(File sourceFile, String sourceFileName, HashMap<String, String> classFilesPaths) {
        if (sourceFile.isDirectory()) {
            File[] subFiles = sourceFile.listFiles();
            for (File subFile : subFiles) {
                getClassFilesPaths(subFile, sourceFileName + File.separator + subFile.getName(), classFilesPaths);
            }
        } else {
            classFilesPaths.put(sourceFile.getName(), sourceFile.getAbsolutePath());
        }
    }

}


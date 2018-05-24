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

package org.wso2.productcodecoverageservice.coveragereport;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.system.ApplicationHome;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.wso2.productcodecoverageservice.Application;
import org.wso2.productcodecoverageservice.Constants;
import org.wso2.productcodecoverageservice.Constants.Report;
import org.wso2.productcodecoverageservice.codecoverage.CodeCoverageController;
import org.wso2.productcodecoverageservice.coveragereport.jsonobject.CoverageReport;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;

@RestController
public class CoverageReportController {

    private static final Logger log = Logger.getLogger(CodeCoverageController.class);
    @Autowired
    Environment environment;

    /**
     * Get download information of a hosted coverage report for a particular product
     *
     * @param productID     ID relevant to the product area
     * @param componentName Job name in the jenkins server relevant to the product area component
     * @param reportType    Either to get a html or xml report information
     * @return A CoverageReport object with the hosted server information
     * @throws IOException Unable to read the application.properties file from the classpath
     */
    @RequestMapping(value = {Report.GET_COVERAGE_REPORT_REQUEST}, method = {RequestMethod.GET})
    public CoverageReport getComponentCoverageReport(
            @RequestParam(value = Report.PRODUCT_ID) String productID,
            @RequestParam(value = Report.COMPONENT_NAME) String componentName,
            @RequestParam(value = Report.REPORT_TYPE) String reportType) throws IOException {

        ApplicationHome home = new ApplicationHome(Application.class);
        String coverageReportFolderPath = home.getDir() + File.separator + Constants.Coverage.COVERAGE_REPORTS_DIRECTORY;

        Properties properties = new Properties();
        properties.load(new FileReader(home.getDir() + File.separator + Constants.General.PROPERTIES_PATH));

        String reportFile;
        switch (reportType) {

            case Report.HTML:
                reportFile = Report.HTML_INDEX_FILE;
                break;
            case Report.XML:
                reportFile = Constants.Coverage.XML_REPORT_FILE;
                break;
            default:
                return new CoverageReport(null, componentName, "Invalid report type");
        }

        String coverageReportPath = coverageReportFolderPath
                + File.separator + productID
                + File.separator + componentName
                + File.separator + reportFile;
        File coverageReportFile = new File(coverageReportPath);

        if (!coverageReportFile.exists()) {
            return new CoverageReport(null, componentName, "Report missing");
        }

        String hostAddress = properties.getProperty(Report.REPORT_HOST_IP);
        String coverageReportURL = Constants.General.HTTPS
                + hostAddress + ":" + environment.getProperty("local.server.port")
                + Constants.General.URL_SEPARATOR + productID
                + Constants.General.URL_SEPARATOR + componentName
                + Constants.General.URL_SEPARATOR + reportFile;

        CoverageReport componentCoverageReport = new CoverageReport(coverageReportURL, componentName, "Success");
        log.info("Coverage report sent : " + componentName + " is " + componentCoverageReport.getReportPath());
        return componentCoverageReport;
    }
}

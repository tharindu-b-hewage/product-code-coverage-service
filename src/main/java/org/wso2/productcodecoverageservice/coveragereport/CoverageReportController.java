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
import java.net.InetAddress;
import java.net.UnknownHostException;

@RestController
public class CoverageReportController {

    @Autowired
    Environment environment;

    private static final Logger log = Logger.getLogger(CodeCoverageController.class);
    @RequestMapping(value = {Report.GET_COVERAGE_REPORT_REQUEST}, method = {RequestMethod.GET})
    public CoverageReport getComponentCoverageReport(
            @RequestParam(value = Report.PRODUCT_ID) String productID,
            @RequestParam(value = Report.COMPONENT_NAME) String componentName) {

        ApplicationHome home = new ApplicationHome(Application.class);
        String coverageReportFolderPath = home.getDir() + File.separator + Constants.Coverage.COVERAGE_REPORTS_DIRECTORY;

        String coverageReportPath = coverageReportFolderPath
                + File.separator + productID
                + File.separator + componentName
                + File.separator + Report.HTML_INDEX_FILE;
        File coverageReportFile = new File(coverageReportPath);

        CoverageReport componentCoverageReport = null;
        boolean reportPathSuccess = true;
        if (!coverageReportFile.exists()) {
            reportPathSuccess = false;
        }
        try {
            String coverageReportURL = Constants.General.HTTPS
                    + InetAddress.getLocalHost().getHostAddress() + ":" + environment.getProperty("local.server.port")
                    + File.separator + productID
                    + File.separator + componentName
                    + File.separator + Report.HTML_INDEX_FILE;
            componentCoverageReport = new CoverageReport(coverageReportURL, componentName, "Success");
            log.info("Coverage report path for " + componentName + " is " + componentCoverageReport.getReportPath());
        } catch (UnknownHostException e) {
            reportPathSuccess = false;
        }

        if (reportPathSuccess) {
            return componentCoverageReport;
        }

        log.warn("Coverage report for " + componentName + " is not found");
        return new CoverageReport(null, componentName, "Report missing");
    }
}

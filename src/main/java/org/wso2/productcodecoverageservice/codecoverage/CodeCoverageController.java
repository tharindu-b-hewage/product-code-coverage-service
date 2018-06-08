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

package org.wso2.productcodecoverageservice.codecoverage;

import org.apache.log4j.Logger;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.wso2.productcodecoverageservice.Constants.Coverage;
import org.wso2.productcodecoverageservice.Constants.Info;
import org.wso2.productcodecoverageservice.codecoverage.jacocoanalyzer.ComponentCoverage;
import org.wso2.productcodecoverageservice.codecoverage.jacocoanalyzer.CoverageCalculator;
import org.wso2.productcodecoverageservice.codecoverage.jenkinshandler.JenkinsServer;
import org.wso2.productcodecoverageservice.codecoverage.jsonobject.ProductArea;
import org.wso2.productcodecoverageservice.codecoverage.jsonobject.ProductAreaCodeCoverage;
import org.wso2.productcodecoverageservice.codecoverage.jsonobject.Products;
import org.wso2.productcodecoverageservice.codecoverage.jsonobject.ProductsCodeCoverage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

@RestController
public class CodeCoverageController {

    private static final Logger log = Logger.getLogger(CodeCoverageController.class);

    private static final String serviceInfo = Info.MESSAGE;

    @RequestMapping(value = {Coverage.POST_COVERAGE_REQUEST}, method = {RequestMethod.POST})
    public ProductsCodeCoverage getProductAreaInfo(@RequestBody Products products) {

        if (products != null) {

            ProductsCodeCoverage productsCodeCoverage = new ProductsCodeCoverage("Success");

            ArrayList<ProductAreaCodeCoverage> productAreaCodeCoverages = new ArrayList<>();
            for (ProductArea eachProductArea : products.getProductAreas()) {
                String productAreaID = null;
                try {
                    productAreaID = eachProductArea.getProductId();
                    log.info("Calculating coverage data for :- ProductID = " + productAreaID);
                    ProductAreaCodeCoverage productAreaCodeCoverage = getProductAreaCodeCoverage(eachProductArea);
                    productAreaCodeCoverages.add(productAreaCodeCoverage);
                } catch (Exception e) {
                    ProductAreaCodeCoverage badProductAreaCoverage;
                    if (eachProductArea == null) {
                        log.error("Invalid request data. Could not find productArea information");
                        badProductAreaCoverage = new ProductAreaCodeCoverage(
                                null,
                                null,
                                null,
                                null);
                    } else {
                        log.error("Error occured while calculating coverage for product area " + productAreaID);
                        badProductAreaCoverage = new ProductAreaCodeCoverage(
                                productAreaID,
                                null,
                                null,
                                null);
                    }
                    productAreaCodeCoverages.add(badProductAreaCoverage);
                }
            }

            if (productAreaCodeCoverages.size() != 0) {
                ProductAreaCodeCoverage[] productAreaCodeCoveragesList = productAreaCodeCoverages.toArray(new ProductAreaCodeCoverage[productAreaCodeCoverages.size()]);
                productsCodeCoverage.setProductAreas(productAreaCodeCoveragesList);
            } else {
                productsCodeCoverage.setProductAreas(null);
            }
            log.info("Code coverage calculation is successfully completed");
            return productsCodeCoverage;
        } else {
            log.error("Code coverage calculation is unsuccessful");
            return new ProductsCodeCoverage("Invalid request data");
        }
    }

    private ProductAreaCodeCoverage getProductAreaCodeCoverage(ProductArea productArea) throws IOException {

        HashMap<String, ComponentCoverage> productCodeCoverage;
        long overallLinesToCover = 0;
        long overallCoveredLines = 0;
        Double overallCoveredRatio;

        JenkinsServer jenkins = new JenkinsServer();
        try {
            jenkins.setProductAreaJenkinsJobs(productArea.getComponents());
            ArrayList<String> jacocoDataFiles = jenkins.downloadCoverageFiles();

            CoverageCalculator coverageCalculator = new CoverageCalculator(jenkins.getTemporaryProductAreaWorkspace(), productArea.getProductId());
            log.info("Merging retrieved jacoco data files");
            coverageCalculator.mergeDataFiles(jacocoDataFiles);
            productCodeCoverage = coverageCalculator.getProductCoverageData(productArea.getComponents());
            log.info("Generating coverage reports");
            coverageCalculator.generateCoverageReports(productArea.getComponents());

            /* Calculate overall code coverage value for the product area*/
            for (String productAreaComponent : productCodeCoverage.keySet()) {
                /*
                Skip the component if it's not relevant to the code coverage calculation
                 */
                ComponentCoverage componentCoverage = productCodeCoverage.get(productAreaComponent);

                long componentLinesToCover = Long.parseLong(componentCoverage.getComponentLinesToCover());
                Double componentCoveredRatio = Double.parseDouble(componentCoverage.getComponentLineCoveredRatio());
                double componentCoveredLines = componentLinesToCover * componentCoveredRatio;

                overallCoveredLines += (long) componentCoveredLines;
                overallLinesToCover += componentLinesToCover;
            }
            if (overallLinesToCover > 0) {
                overallCoveredRatio = (double) overallCoveredLines / (double) overallLinesToCover;
                log.info("Overall line coverage in ProductID=" + productArea.getProductId() + " is " + Double.toString(Math.round(overallCoveredRatio * 100)) + "%");
            } else {
                overallCoveredRatio = null;
            }
        } finally {
            jenkins.clearTemporaryData();
        }
        return new ProductAreaCodeCoverage(productArea.getProductId(), productCodeCoverage,
                Long.toString(overallLinesToCover), overallCoveredRatio != null ? Double.toString(overallCoveredRatio) : null);
    }
}

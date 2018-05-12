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

package org.wso2.productcodecoverageservice.CodeCoverage;

import org.apache.log4j.Logger;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.wso2.productcodecoverageservice.CodeCoverage.JSONObject.ProductArea;
import org.wso2.productcodecoverageservice.CodeCoverage.JSONObject.ProductAreaCodeCoverage;
import org.wso2.productcodecoverageservice.CodeCoverage.JSONObject.Products;
import org.wso2.productcodecoverageservice.CodeCoverage.JSONObject.ProductsCodeCoverage;
import org.wso2.productcodecoverageservice.CodeCoverage.JacocoAnalyzer.CoverageCalculator;
import org.wso2.productcodecoverageservice.CodeCoverage.JenkinsHandler.JenkinsServer;
import org.wso2.productcodecoverageservice.Constants.Coverage;
import org.wso2.productcodecoverageservice.Constants.General;
import org.wso2.productcodecoverageservice.Constants.Info;

import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

@RestController
public class CodeCoverageController {

    private static final Logger log = Logger.getLogger(CodeCoverageController.class);
    private static final String serviceInfo = Info.MESSAGE;
    private final AtomicLong counter = new AtomicLong();
    private AtomicBoolean serviceBusy = new AtomicBoolean();

    @RequestMapping(value = {General.POST_PRODUCT_COVERAGE_REQUEST}, method = {RequestMethod.POST})
    public ProductsCodeCoverage getProductAreaInfo(@RequestParam(name = "auth", required = true) String authString, @RequestBody Products products) throws IOException {

        if (!serviceBusy.get() && verifyAuthString(authString)) {

            this.serviceBusy.set(true);

            ProductsCodeCoverage productsCodeCoverage = new ProductsCodeCoverage("Success");
            int productAreaLinesToCover = 0;
            int productAreaUncoveredLines = 0;

            ArrayList<ProductAreaCodeCoverage> productAreaCodeCoverages = new ArrayList<>();
            for (ProductArea eachProductArea : products.getProducts()) {
                try {
                    ProductAreaCodeCoverage productAreaCodeCoverage = getProductAreaCodeCoverage(eachProductArea);
                    productAreaCodeCoverages.add(productAreaCodeCoverage);
                } catch (Exception e) {

                    /* Add null for products areas caused errors. This is temporary and should be fixed soon*/
                    productAreaCodeCoverages.add(null);
                }
            }

            ProductAreaCodeCoverage[] productAreaCodeCoveragesList = productAreaCodeCoverages.toArray(new ProductAreaCodeCoverage[productAreaCodeCoverages.size()]);
            productsCodeCoverage.setProductAreas(productAreaCodeCoveragesList);

            serviceBusy.set(false);
            return productsCodeCoverage;
        } else if (verifyAuthString(authString)) {
            return new ProductsCodeCoverage("service busy");
        } else {
            return new ProductsCodeCoverage("unauthorized");
        }
    }

    private boolean verifyAuthString(String authString) throws IOException {

        Properties propertiesFile = new Properties();
        propertiesFile.load(new FileReader(General.PROPERTIES_PATH));

        return authString.equals(propertiesFile.get(Coverage.TRUSTED_BASIC_AUTH_USER));
    }

    private ProductAreaCodeCoverage getProductAreaCodeCoverage(ProductArea productArea) throws IOException {

        HashMap<String, HashMap<String, String>> productCodeCoverage;
        JenkinsServer jenkins = new JenkinsServer();

        jenkins.setProductAreaJenkinsJobs(productArea.getProductComponents());
        log.info("Downloading coverage files from Jenkins server");
        jenkins.downloadCoverageFiles();

        CoverageCalculator coverageCalculator = new CoverageCalculator(jenkins.getTemporaryProductAreaWorkspace());
        log.info("Merging retrieved jacoco data files");
        coverageCalculator.mergeDataFiles();
        log.info("Calculating code coverage data for each component");
        productCodeCoverage = coverageCalculator.getProductCoverageData(productArea.getProductComponents());

        jenkins.clearTemporaryData();

        this.serviceBusy.set(false);
        return new ProductAreaCodeCoverage(counter.incrementAndGet(), productArea.getProductName(), productCodeCoverage);
    }
}

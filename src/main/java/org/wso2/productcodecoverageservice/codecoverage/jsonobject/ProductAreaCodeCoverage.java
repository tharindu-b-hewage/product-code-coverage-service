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

package org.wso2.productcodecoverageservice.codecoverage.jsonobject;

import java.util.HashMap;

public class ProductAreaCodeCoverage {

    private final String productId;
    private final String overallLinesToCover;
    private final String overallLineCoverageRatio;

    /*
    Resulting code coverage JSON. Example: {component_name: {coverage_ratio: XX, lines_to_cover: YY}, ..}
     */
    private final HashMap<String, HashMap<String, String>> componentCodeCoverage;

    public ProductAreaCodeCoverage(String productId, HashMap<String, HashMap<String, String>> componentCodeCoverage,
                                   String overallLinesToCover, String overallLineCoverageRatio) {

        this.productId = productId;
        this.componentCodeCoverage = componentCodeCoverage;
        this.overallLineCoverageRatio = overallLineCoverageRatio;
        this.overallLinesToCover = overallLinesToCover;
    }

    public HashMap<String, HashMap<String, String>> getComponentCodeCoverage() {

        return componentCodeCoverage;
    }

    public String getProductId() {

        return productId;
    }

    public String getOverallLineCoverageRatio() {

        return overallLineCoverageRatio;
    }

    public String getOverallLinesToCover() {

        return overallLinesToCover;
    }
}

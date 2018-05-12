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

package org.wso2.productcodecoverageservice.CodeCoverage.JSONObject;

import java.util.HashMap;

public class ProductAreaCodeCoverage {

    private final String productArea;
    private final long id;
    private String serviceBusyStatus;
    /*
    Resulting code coverage JSON. Example: {component_name: {coverage_ratio: XX, lines_to_cover: YY}, ..}
     */
    private final HashMap<String, HashMap<String, String>> productAreaCodeCoverage;

    public ProductAreaCodeCoverage(long id, String productArea, HashMap<String, HashMap<String, String>> productAreaCodeCoverage) {

        this.id = id;
        this.productArea = productArea;
        this.productAreaCodeCoverage = productAreaCodeCoverage;
        this.serviceBusyStatus = "service available";
    }

    public ProductAreaCodeCoverage(long id, String serviceBusyMessage) {

        this.id = id;
        this.productArea = null;
        this.productAreaCodeCoverage = null;
        this.serviceBusyStatus = serviceBusyMessage;
    }

    public long getId() {

        return id;
    }

    public HashMap<String, HashMap<String, String>> getProductAreaCodeCoverage() {

        return productAreaCodeCoverage;
    }

    public String getProductArea() {

        return productArea;
    }

    public String getServiceBusyStatus() {

        return serviceBusyStatus;
    }
}

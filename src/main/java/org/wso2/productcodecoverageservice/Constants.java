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

package org.wso2.productcodecoverageservice;

public class Constants {

    public static class Info {

        public static final String GET_REQUEST_INFO = "/product-coverage-service/info";
        public static final String MESSAGE = "wso2 product coverage service";
    }

    public static class Jenkins {

        public static final String WORKSPACE_DIRECTORY_PREFIX = "codeCoverageServiceTemp";
        public static final String JENKINS_SERVER_BASE64_AUTH_STRING = "jenkins.base64.encoded.basic.auth.string";
        public static final String LAST_SUCCESSFUL_BUILD = "lastSuccessfulBuild";
        public static final String JACOCO_DATA_FILE = "jacoco/jacoco.exec";
        public static final String CLASSES_ZIP = "projectFiles/classFiles.zip";
        public static final String SOURCES_ZIP = "projectFiles/sourceFiles.zip";
        public static final String JACOCO_DATAFILES_FOLDER = "jacocoDataFiles";
        public static final String COMPILED_CLASSES_FOLDER = "compiledClasses";
        public static final String JACOCO_DATAFILE_NAME = "jacoco.exec";
        public static final String COMPILED_CLASSES_FILE_NAME = "classes.zip";
        public static final String JENKINS_SERVER_URL = "jenkins.server.URL";
        public static final String SOURCE_FILES_FOLDER = "sourceFiles";
        public static final String SOURCE_FILE_ZIP = "sources.zip";
        public static final String JACOCO_SOURCES_FILE_ZIP = "jacoco_sources.zip";
        public static final String COVERAGE_SOURCES_TEMP_FOLDER = "sources_temp";
        public static final String JACOCO_RESOURCES_ZIP = "jacoco/resources.zip";
        public static final String EXTRACTED_JACOCO_FOLDER = "jacocoResources";
    }

    public static class Coverage {

        public static final String MERGED_JACOCO_DATA_FILE = "jacoco-merged.exec";
        public static final String DATA_FILE_EXTENSION = "exec";
        public static final String CLASS_FILE_EXTENSION = "class";
        public static final String EXTRACTED_CLASS_FOLDER = "extractedClasses";
        public static final String POST_COVERAGE_REQUEST = "/product-coverage-service/code-coverage";
        public static final String EXTRACTED_SOURCE_FOLDER = "extractedSources";
        public static final String COVERAGE_REPORTS_DIRECTORY = "static";
        public static final String CLASSES = "classes";
        public static final String SOURCES = "sources";
        public static final String ORG = "org";
        public static final String WSO2 = "wso2";
    }

    public static class Report {

        public static final String GET_COVERAGE_REPORT_REQUEST = "/product-coverage-service/coverage-report";
        public static final String PRODUCT_ID = "productID";
        public static final String COMPONENT_NAME = "componentName";
        public static final String HTML_INDEX_FILE = "index.html";
    }

    public static class General {

        public static final String PROPERTIES_PATH = "application.properties";
        public static final String URL_SEPARATOR = "/";
        public static final String STEP_BACK = "../";
        public static final String SKIPPED_COMPONENTS = "skipping.components";
        public static final String BASIC_AUTH_USER_NAME = "auth.user.name";
        public static final String BASIC_AUTH_USER_PASSWORD = "auth.user.password";
        public static final String BASIC_AUTH_ROLE = "USER";
        public static final String HTTP = "http://";
        public static final String HTTPS = "https://";
    }
}

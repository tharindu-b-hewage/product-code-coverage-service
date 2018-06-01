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

package org.wso2.productcodecoverageservice.sourcefilereport.jsonobject;

public class SourceFileReport {

    private final String sourceFile;
    private final String issues;
    private final InstructionCoverageData instructionCoverageData;
    private final BranchCoverageData branchCoverageData;
    private final LineCoverageData lineCoverageData;
    private final MethodCoverageData methodCoverageData;

    public SourceFileReport(String sourceFile, String issues, InstructionCoverageData instructionCoverageData,
                            BranchCoverageData branchCoverageData, LineCoverageData lineCoverageData,
                            MethodCoverageData methodCoverageData) {
        this.sourceFile = sourceFile;
        this.issues = issues;
        this.instructionCoverageData = instructionCoverageData;
        this.branchCoverageData = branchCoverageData;
        this.lineCoverageData = lineCoverageData;
        this.methodCoverageData = methodCoverageData;
    }

    public String getSourceFile() {

        return sourceFile;
    }

    public String getIssues() {

        return issues;
    }

    public InstructionCoverageData getInstructionCoverageData() {

        return instructionCoverageData;
    }

    public BranchCoverageData getBranchCoverageData() {
        return branchCoverageData;
    }

    public LineCoverageData getLineCoverageData() {
        return lineCoverageData;
    }

    public MethodCoverageData getMethodCoverageData() {
        return methodCoverageData;
    }

    public static class InstructionCoverageData {

        String missed;
        String covered;

        public InstructionCoverageData(String missed, String covered) {
            this.missed = missed;
            this.covered = covered;
        }

        public String getMissedInstructions() {
            return missed;
        }

        public String getCoveredInstructions() {
            return covered;
        }
    }

    public static class BranchCoverageData {

        String missed;
        String covered;

        public BranchCoverageData(String missed, String covered) {
            this.missed = missed;
            this.covered = covered;
        }

        public String getMissedBranches() {
            return missed;
        }

        public String getCoveredBranches() {
            return covered;
        }
    }

    public static class LineCoverageData {

        String missed;
        String covered;

        public LineCoverageData(String missed, String covered) {
            this.missed = missed;
            this.covered = covered;
        }

        public String getMissedLines() {
            return missed;
        }

        public String getCoveredLines() {
            return covered;
        }
    }

    public static class MethodCoverageData {

        String missedMethods;
        String coveredMethods;

        public MethodCoverageData(String missedMethods, String coveredMethods) {
            this.missedMethods = missedMethods;
            this.coveredMethods = coveredMethods;
        }

        public String getMissedMethods() {
            return missedMethods;
        }

        public String getCoveredMethods() {
            return coveredMethods;
        }

    }
}


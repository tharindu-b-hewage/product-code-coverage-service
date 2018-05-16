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

package org.wso2.productcodecoverageservice.codecoverage.HTTPutils;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class FileDownloader {

    /**
     * Download a file from a URL with basic authentication
     *
     * @param fileURL           Downloading file
     * @param fileSavePath      Path of the saved file
     * @param encodedAuthString Base 64 encoded authentication string
     * @throws IOException If the connection with url failed or failure to save the downloaded file
     */
    public static void downloadWithBasicAuth(String fileURL, File fileSavePath, String encodedAuthString) throws IOException {

        URL connectionUrl = new URL(fileURL);
        HttpURLConnection httpConnection = (HttpURLConnection) connectionUrl.openConnection();

        httpConnection.setRequestMethod("GET");
        httpConnection.setRequestProperty("Authorization", "Basic " + encodedAuthString);

        int responseCode = httpConnection.getResponseCode();

        if (responseCode == 200) {
            InputStream inputStream = httpConnection.getInputStream();

            // opens an output stream to save into file
            FileUtils.forceMkdirParent(fileSavePath);
            FileOutputStream outputStream = new FileOutputStream(fileSavePath);

            int bytesRead;
            byte[] buffer = new byte[4096];
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }

            outputStream.close();
            inputStream.close();
        } else {
            throw new IOException("File download failed");
        }
    }
}

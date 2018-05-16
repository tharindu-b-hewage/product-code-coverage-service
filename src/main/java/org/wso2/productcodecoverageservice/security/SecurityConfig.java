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

package org.wso2.productcodecoverageservice.security;

import org.springframework.boot.system.ApplicationHome;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.wso2.productcodecoverageservice.Application;
import org.wso2.productcodecoverageservice.Constants;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;

@Configuration
public class SecurityConfig extends WebSecurityConfigurerAdapter {

    private final Properties application = new Properties();

    public SecurityConfig() throws IOException {

        ApplicationHome home = new ApplicationHome(Application.class);
        this.application.load(new FileReader(home.getDir() + File.separator + Constants.General.PROPERTIES_PATH));
    }

    // Authentication : User --> Roles
    protected void configure(AuthenticationManagerBuilder auth) throws Exception {

        auth.inMemoryAuthentication().passwordEncoder(org.springframework.security.crypto.password.NoOpPasswordEncoder.getInstance())
                .withUser(application.getProperty(Constants.General.BASIC_AUTH_USER_NAME))
                .password(application.getProperty(Constants.General.BASIC_AUTH_USER_PASSWORD))
                .roles(Constants.General.BASIC_AUTH_ROLE);
    }

    // Authorization : Role -> Access
    protected void configure(HttpSecurity http) throws Exception {

        http.httpBasic().and().authorizeRequests().antMatchers(Constants.Coverage.POST_COVERAGE_REQUEST)
                .hasRole(Constants.General.BASIC_AUTH_ROLE).and().csrf().disable().headers().frameOptions().disable();

        http.httpBasic().and().authorizeRequests().antMatchers(Constants.Report.GET_COVERAGE_REPORT_REQUEST)
                .hasRole(Constants.General.BASIC_AUTH_ROLE).and().csrf().disable().headers().frameOptions().disable();
    }

}

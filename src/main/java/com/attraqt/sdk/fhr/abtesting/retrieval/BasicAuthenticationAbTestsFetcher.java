/*
 * FHR A/B Testing SDK. Copyright (C) 2020 Attraqt Limited
 *
 * This file is part of the FHR A/B Testing SDK.
 *
 * The FHR A/B Testing SDK is free software: you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * The FHR A/B Testing SDK is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with the FHR A/B Testing SDK.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.attraqt.sdk.fhr.abtesting.retrieval;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.core.GenericType;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.UriBuilder;

import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.ClientProperties;

import com.attraqt.sdk.fhr.abtesting.model.RunningAbTest;

import lombok.RequiredArgsConstructor;

/**
 * Handles fetching the A/B tests from FHR A/B Tests Service by using basic authentication.
 */
@RequiredArgsConstructor
public class BasicAuthenticationAbTestsFetcher implements RunningAbTestsFetcher {

    private static final String BASIC_AUTH_PREFIX = "Basic ";

    private final String username;
    private final String password;
    private final URI runningAbTestsPath;
    private final int connectionTimeoutInSeconds;
    private final int readTimeoutInSeconds;

    public List<RunningAbTest> getRunningAbTests() {

        ClientConfig configuration = new ClientConfig()
            .property(ClientProperties.CONNECT_TIMEOUT, (int) TimeUnit.SECONDS.toMillis(connectionTimeoutInSeconds))
            .property(ClientProperties.READ_TIMEOUT, (int) TimeUnit.SECONDS.toMillis(readTimeoutInSeconds));

        Client client = ClientBuilder.newClient(configuration);

        UriBuilder uriBuilder = UriBuilder.fromUri(runningAbTestsPath);

        String usernameAndPassword  = username + ":" + password;

        String authorizationHeaderValue =
            BASIC_AUTH_PREFIX + java.util.Base64.getEncoder().encodeToString(usernameAndPassword.getBytes());

        return new ArrayList<>(client.target(uriBuilder)
            .request(MediaType.APPLICATION_JSON)
            .header(HttpHeaders.AUTHORIZATION, authorizationHeaderValue)
            .get()
            .readEntity(new GenericType<List<RunningAbTest>>() {
            }));
    }
}

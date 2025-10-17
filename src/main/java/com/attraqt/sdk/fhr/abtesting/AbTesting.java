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

package com.attraqt.sdk.fhr.abtesting;

import java.net.URI;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import jakarta.ws.rs.core.UriBuilder;

import com.attraqt.sdk.fhr.abtesting.caching.AbTestsCache;
import com.attraqt.sdk.fhr.abtesting.caching.InMemoryAbTestsCache;
import com.attraqt.sdk.fhr.abtesting.model.RunningAbTest;
import com.attraqt.sdk.fhr.abtesting.model.RunningAbTest.RunningAbTestVariant;
import com.attraqt.sdk.fhr.abtesting.model.SchedulingOptions;
import com.attraqt.sdk.fhr.abtesting.retrieval.BasicAuthenticationAbTestsFetcher;
import com.attraqt.sdk.fhr.abtesting.retrieval.RunningAbTestsFetcher;
import com.attraqt.sdk.fhr.abtesting.scheduling.AbTestsRetrievalScheduler;
import com.google.common.base.Strings;
import com.google.common.hash.Hashing;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@Slf4j
public class AbTesting {

    private static final String FH_ABTESTS_PARAMETER = "fh_abtests";
    private static final int CACHE_RETRY_TIME_MINUTES = 1;

    public static final String DEFAULT_AB_TESTS_PATH = "/config/running/ab-tests";
    public static final int DEFAULT_CACHE_EXPIRATION_TIME_IN_MINUTES = 5;
    public static final int DEFAULT_CONNECTION_TIMEOUT_IN_SECONDS = 1;
    public static final int DEFAULT_READ_TIMEOUT_IN_SECONDS = 30;

    /**
     * Sets the A/B Tests Cache.
     * <p>
     * If it is not provided, {@link InMemoryAbTestsCache} will be used by default.
     *
     * @param abTestsCache An implementation of {@link AbTestsCache}
     */
    private AbTestsCache abTestsCache;
    private AbTestsRetrievalScheduler abTestsRetrievalScheduler;
    /**
     * Sets the runningAbTestsFetcher.
     * <p>
     * If it is not provided, {@link BasicAuthenticationAbTestsFetcher} will be used by default.
     *
     * @param runningAbTestsFetcher An implementation of {@link RunningAbTestsFetcher}
     */
    private RunningAbTestsFetcher runningAbTestsFetcher;
    /**
     * Sets the cacheExpireTimeMinutes.
     * <p>
     * If it is not provided {@value com.attraqt.sdk.fhr.abtesting.AbTesting#DEFAULT_CACHE_EXPIRATION_TIME_IN_MINUTES}
     * will be used by default.
     *
     * @param cacheExpireTimeMinutes An integer which indicates the cache expiration in minutes.
     */
    private Integer cacheExpireTimeMinutes;
    /**
     * Sets the abTestsServerUrl.
     * <p>
     * If it is not provided, {@link IllegalArgumentException} will be thrown.
     *
     * @param abTestsServerUrl The FHR A/B Tests Service Url
     */
    private String abTestsServerUrl;
    /**
     * Sets the abTestsPath.
     * <p>
     * If it is not provided, {@value com.attraqt.sdk.fhr.abtesting.AbTesting#DEFAULT_AB_TESTS_PATH} will be used by
     * default.
     *
     * @param abTestsPath The path of the endpoint which serves the A/B tests
     */
    private String abTestsPath;
    /**
     * Sets the username.
     * <p>
     * If username is not provided, {@link IllegalArgumentException} will be thrown.
     *
     * @param username The username
     */
    private String username;
    /**
     * Sets the password.
     * <p>
     * If password is not provided, {@link IllegalArgumentException} will be thrown.
     *
     * @param password The password
     */
    private String password;
    /**
     * Sets the connectionTimeoutInSeconds.
     * <p>
     * If it is not provided, {@value com.attraqt.sdk.fhr.abtesting.AbTesting#DEFAULT_CONNECTION_TIMEOUT_IN_SECONDS}
     * will be used by default.
     *
     * @param connectionTimeoutInSeconds An integer which indicates the timeout to connect to A/B tests server.
     */
    private Integer connectionTimeoutInSeconds;
    /**
     * Sets the readTimeoutInSeconds.
     * <p>
     * If it is not provided, {@value com.attraqt.sdk.fhr.abtesting.AbTesting#DEFAULT_READ_TIMEOUT_IN_SECONDS} will be
     * used by default.
     *
     * @param readTimeoutInSeconds An integer which indicates the timeout to read from A/B tests server.
     */
    private Integer readTimeoutInSeconds;
    @Getter
    private boolean isStarted;

    /**
     * Fetches A/B tests and applies variant selection algorithm. Then appends fh_abtests parameter to URI. If
     * fh_abtests parameter already exists in URI it will be overwritten.
     *
     * @param sessionId The sessionId
     * @param uri       The Fas request URI
     * @return URI updated with A/B tests parameter
     */
    public URI appendAbTestsParameter(String sessionId, URI uri) {
        String fhAbtests = getFhAbtests(sessionId, getQueryParamsFromURI(uri));
        UriBuilder builder = UriBuilder.fromUri(uri);
        if (!Strings.isNullOrEmpty(fhAbtests)) {
            builder.replaceQueryParam(FH_ABTESTS_PARAMETER, fhAbtests);
        }
        return builder.build();
    }

    /**
     * Fetches A/B tests and applies variant selection algorithm. Then put fh_abtests parameter into the parameters map.
     * If fh_abtests parameter already exists in parameters map it will be overwritten.
     *
     * @param sessionId      The sessionId
     * @param fhrQueryParams The query parameters map
     * @return Map updated with A/B tests parameter
     */
    public Map<String, List<String>> appendAbTestsParameter(String sessionId,
                                                            Map<String, List<String>> fhrQueryParams) {
        String fhAbtests = getFhAbtests(sessionId, fhrQueryParams);
        if (!Strings.isNullOrEmpty(fhAbtests)) {
            fhrQueryParams.put(FH_ABTESTS_PARAMETER, Collections.singletonList(fhAbtests));
        }
        return fhrQueryParams;
    }

    /**
     * Shuts the {@link AbTesting#abTestsRetrievalScheduler} down. The cache will no longer be updated until it is
     * started again.
     */
    public void shutdown() {
        abTestsRetrievalScheduler.shutdown();

        isStarted = false;

        log.info("Shutting down A/B tests retrieval.");
    }

    /**
     * Starts the {@link AbTesting#abTestsRetrievalScheduler} which fetches A/B tests and updates the cache
     * periodically.
     */
    public void start() {
        if (isStarted) {
            log.warn("AbTesting object has already been started. Please shut it down before trying to restart");
        } else {
            SchedulingOptions schedulingOptions =
                new SchedulingOptions(cacheExpireTimeMinutes, CACHE_RETRY_TIME_MINUTES, TimeUnit.MINUTES);

            abTestsRetrievalScheduler.start(abTestsCache, runningAbTestsFetcher, schedulingOptions);

            isStarted = true;
        }
    }

    private String getFhAbtests(String sessionId, Map<String, List<String>> fhrQueryParams) {
        if (!isStarted) {
            log.warn("appendAbTestsParameter method called on AbTesting object that is in not started state. Please " +
                "call abTesting.start() method before calling appendAbTestsParameter.");
        }
        // Try get from cache, otherwise get new ab tests from cru and put them into the cache
        List<RunningAbTest> abTests = abTestsCache.getAbTests();

        if (abTests != null && !abTests.isEmpty()) {
            // Remove the existent fh_abtests parameter to avoid duplication
            fhrQueryParams.remove(FH_ABTESTS_PARAMETER);
            abTests = abTests
                .stream()
                .filter(runningAbTest -> matchAbTestWithParams(runningAbTest, fhrQueryParams))
                .collect(Collectors.toList());

            if (!abTests.isEmpty()) {
                // Select ab test variants for session id and return
                return getAbTestsForSessionId(sessionId, abTests);
            }
        } else {
            log.debug("A/B tests cache is empty. No A/B tests to match on.");
        }

        return null;
    }

    private boolean matchAbTestWithParams(RunningAbTest runningAbTest, Map<String, List<String>> params) {
        for (Map.Entry<String, List<String>> entry : runningAbTest.getFilters().entrySet()) {
            if (params.containsKey(entry.getKey())) {
                for (String pattern : entry.getValue()) {
                    List<String> values = params.get(entry.getKey());
                    // If the filtering param occurring more than once or it's value doesn't match with the pattern
                    if (values.size() != 1 || !values.get(0).matches(pattern)) {
                        return false;
                    }
                }
            } else {
                return false;
            }
        }

        return true;
    }

    private String getAbTestsForSessionId(String sessionId, List<RunningAbTest> runningABTests) {
        ArrayList<String> selections = new ArrayList<>();
        for (RunningAbTest abTest : runningABTests) {
            String sessionTestSelection = String.format("%s#%s", sessionId, abTest.getId());

            long percentile =
                Hashing.murmur3_32().hashUnencodedChars(sessionTestSelection).padToLong() % 100;

            List<RunningAbTestVariant> variations =
                abTest.getVariations()
                    .stream()
                    .sorted(Comparator.comparing(RunningAbTestVariant::hashCode))
                    .collect(Collectors.toList());

            int acc = 0;

            for (RunningAbTestVariant variant : variations) {
                acc += variant.getWeight();
                if (acc > percentile) {
                    selections.add(abTest.getId() + ":" + variant.getId());
                    break;
                }
            }
        }
        return String.join(";", selections);
    }

    private Map<String, List<String>> getQueryParamsFromURI(URI uri) {
        String query = uri.getQuery();
        if (query == null) {
            return Collections.emptyMap();
        }
        return Arrays.stream(query.split("&")).map(item -> {
            int index = item.indexOf('=');
            return new AbstractMap.SimpleEntry<>(item.substring(0, index), item.substring(index + 1));
        }).collect(
            Collectors.groupingBy(Map.Entry::getKey, Collectors.mapping(Map.Entry::getValue, Collectors.toList())));
    }

    /**
     * The AbTesting builder
     */
    public static class AbTestingBuilder {

        /**
         * Builds an {@link AbTesting} object. Checks the provided parameters and puts the defaults if applicable.
         *
         * @return An {@link AbTesting} object
         * @throws IllegalArgumentException if the certain parameters are missing
         */
        public AbTesting build() {
            if (abTestsServerUrl == null) {
                throw new IllegalArgumentException("abTestsServerUrl required by AbTestingBuilder is not provided.");
            }

            cacheExpireTimeMinutes = validateCacheExpireTimeMinutes(cacheExpireTimeMinutes);

            if (abTestsPath == null) {
                abTestsPath = DEFAULT_AB_TESTS_PATH;
            }

            if (abTestsCache == null) {
                abTestsCache = new InMemoryAbTestsCache();
            }

            if (runningAbTestsFetcher == null) {
                if (username == null) {
                    throw new IllegalArgumentException(
                        String.format(
                            "username required by %s is not provided.",
                            AbTestingBuilder.class.getSimpleName()));
                }

                if (password == null) {
                    throw new IllegalArgumentException(
                        String.format("password required by %s is not provided.",
                            AbTestingBuilder.class.getSimpleName()));
                }

                if (connectionTimeoutInSeconds == null) {
                    connectionTimeoutInSeconds = DEFAULT_CONNECTION_TIMEOUT_IN_SECONDS;
                }

                if (readTimeoutInSeconds == null) {
                    readTimeoutInSeconds = DEFAULT_READ_TIMEOUT_IN_SECONDS;
                }

                URI runningAbTestsUri = UriBuilder.fromUri(abTestsServerUrl).path(abTestsPath).build();

                runningAbTestsFetcher =
                    new BasicAuthenticationAbTestsFetcher(
                        username,
                        password,
                        runningAbTestsUri,
                        connectionTimeoutInSeconds,
                        readTimeoutInSeconds);

            }

            abTestsRetrievalScheduler =
                new AbTestsRetrievalScheduler();

            return new AbTesting(
                abTestsCache,
                abTestsRetrievalScheduler,
                runningAbTestsFetcher,
                cacheExpireTimeMinutes,
                abTestsServerUrl,
                abTestsPath,
                username,
                null, // We do not store password as has already been consumed and to reduce exposure of it.
                connectionTimeoutInSeconds,
                readTimeoutInSeconds,
                false);
        }

        private AbTestingBuilder isStarted(boolean isStarted) {
            return this;
        }

        private AbTestingBuilder abTestsRetrievalScheduler(AbTestsRetrievalScheduler abTestsRetrievalScheduler) {
            return this;
        }

        private Integer validateCacheExpireTimeMinutes(Integer cacheExpireTimeMinutes) {
            if (cacheExpireTimeMinutes == null) {
                log.info("No cache expiry time specified for the A/B tests cache. Using default of {} minutes.",
                    DEFAULT_CACHE_EXPIRATION_TIME_IN_MINUTES);
                return DEFAULT_CACHE_EXPIRATION_TIME_IN_MINUTES;
            } else if (cacheExpireTimeMinutes < DEFAULT_CACHE_EXPIRATION_TIME_IN_MINUTES) {
                log.warn(
                    "The given expiration time for the A/B tests cache of {} minutes is less than default {} " +
                        "minutes. Using default.",
                    cacheExpireTimeMinutes, DEFAULT_CACHE_EXPIRATION_TIME_IN_MINUTES);
                return DEFAULT_CACHE_EXPIRATION_TIME_IN_MINUTES;
            }

            return cacheExpireTimeMinutes;
        }
    }
}


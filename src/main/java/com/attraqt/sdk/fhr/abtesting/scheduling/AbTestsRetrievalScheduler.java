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

package com.attraqt.sdk.fhr.abtesting.scheduling;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;

import com.attraqt.sdk.fhr.abtesting.caching.AbTestsCache;
import com.attraqt.sdk.fhr.abtesting.model.RunningAbTest;
import com.attraqt.sdk.fhr.abtesting.model.SchedulingOptions;
import com.attraqt.sdk.fhr.abtesting.retrieval.RunningAbTestsFetcher;

import lombok.extern.slf4j.Slf4j;

/**
 * Schedules a {@link ScheduledExecutorService} to fetch and cache the A/B tests periodically. It can be configured by
 * {@link SchedulingOptions}. If it fails it retries up to {@value AbTestsRetrievalScheduler#MAX_RETRY} times on each
 * schedule.
 */
@Slf4j
public class AbTestsRetrievalScheduler {

    private static final int MAX_RETRY = 3;
    private static final int INITIAL_ATTEMPT_DELAY = 0;
    private final ScheduledExecutorService ses = Executors.newScheduledThreadPool(1, new DaemonThreadFactory());

    /**
     * Shuts down the scheduled {@link ScheduledExecutorService}.
     */
    public void shutdown() {
        ses.shutdown();
    }

    /**
     * Schedules a {@link ScheduledExecutorService} at a fixed rate based on given schedulingOptions to retrieve and
     * cache A/B tests.
     *
     * @param abTestsCache          The A/B tests cache
     * @param runningAbTestsFetcher The runningAbTestsFetcher to fetch A/B tests from FHR A/B Tests Service
     * @param schedulingOptions     A {@link SchedulingOptions} to provide scheduling configurations
     */
    public void start(AbTestsCache abTestsCache, RunningAbTestsFetcher runningAbTestsFetcher,
                      SchedulingOptions schedulingOptions) {
        ses.scheduleAtFixedRate(
            () -> {
                try {
                    retrieveAndCacheAbTests(runningAbTestsFetcher, abTestsCache);
                } catch (Exception e) {
                    scheduleRetry(
                        runningAbTestsFetcher,
                        abTestsCache,
                        1,
                        schedulingOptions);
                }
            },
            INITIAL_ATTEMPT_DELAY,
            schedulingOptions.getCacheExpireTime(),
            schedulingOptions.getCacheTimeUnits());
    }

    private void scheduleRetry(RunningAbTestsFetcher runningAbTestsFetcher,
                               AbTestsCache abTestsCache,
                               int retryAttempt,
                               SchedulingOptions schedulingOptions) {
        log.warn("Error occurred while fetching A/B tests! Retrying.");
        ses.schedule(
            () -> {
                try {
                    retrieveAndCacheAbTests(runningAbTestsFetcher, abTestsCache);
                } catch (Exception exception) {
                    if (retryAttempt == MAX_RETRY) {
                        log.error("Max retries exceeded with fetching A/B tests. An outdated cache will be used.");
                        log.warn("Exception occurred on final attempt, attempt number {}, to get A/B tests:",
                            MAX_RETRY, exception);
                    } else {
                        log.debug("Exception occurred on attempt to get A/B tests:", exception);
                        scheduleRetry(
                            runningAbTestsFetcher,
                            abTestsCache,
                            retryAttempt + 1,
                            schedulingOptions);
                    }
                }
            },
            schedulingOptions.getCacheRetryTime(),
            schedulingOptions.getCacheTimeUnits());
    }

    private void retrieveAndCacheAbTests(RunningAbTestsFetcher runningAbTestsFetcher, AbTestsCache abTestsCache) {
        log.info("Trying to fetch A/B tests.");
        List<RunningAbTest> runningAbTests = runningAbTestsFetcher.getRunningAbTests();
        abTestsCache.cacheAbTests(runningAbTests);
        log.info("The A/B tests cache is successfully updated.");
    }

    private static class DaemonThreadFactory implements ThreadFactory {

        public Thread newThread(Runnable r) {
            Thread thread = new Thread(r);
            thread.setDaemon(true);
            return thread;
        }
    }

}

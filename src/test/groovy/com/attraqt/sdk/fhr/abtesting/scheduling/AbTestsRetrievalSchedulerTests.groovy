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

package com.attraqt.sdk.fhr.abtesting.scheduling

import java.util.concurrent.TimeUnit

import org.spf4j.log.Level
import org.spf4j.test.log.LogAssert
import org.spf4j.test.log.TestLoggers
import org.spf4j.test.matchers.LogMatchers

import com.attraqt.sdk.fhr.abtesting.caching.AbTestsCache
import com.attraqt.sdk.fhr.abtesting.model.RunningAbTest
import com.attraqt.sdk.fhr.abtesting.model.SchedulingOptions
import com.attraqt.sdk.fhr.abtesting.retrieval.RunningAbTestsFetcher

import spock.lang.Specification
import spock.util.concurrent.PollingConditions

class AbTestsRetrievalSchedulerTests extends Specification {
    private static int MAX_RETRIES = 3;

    def "initialize should schedule the fetcher to get A/B tests and then pass them to abTestsCache"() {
        given:
        def cacheAbTestsCalled = false

        AbTestsCache abTestsCache = Mock()
        abTestsCache.cacheAbTests() >> { cacheAbTestsCalled = true }

        RunningAbTestsFetcher runningAbTestsFetcher = Stub()
        List<RunningAbTest> runningAbTests = [new RunningAbTest(null, null, null)]
        runningAbTestsFetcher.getRunningAbTests() >> runningAbTests

        def conditions = new PollingConditions(timeout: 1, initialDelay: 0.1, factor: 0.1)

        AbTestsRetrievalScheduler scheduler = new AbTestsRetrievalScheduler()

        LogAssert successfulUpdateLogExpectation = TestLoggers.sys().expect(
                AbTestsRetrievalScheduler.class.getName(),
                Level.INFO,
                LogMatchers.hasMessage("The A/B tests cache is successfully updated."))

        when:
        scheduler.start(
                abTestsCache,
                runningAbTestsFetcher,
                new SchedulingOptions(1, 5, TimeUnit.SECONDS))

        conditions.eventually {
            cacheAbTestsCalled // Wait until abTestsCache.cacheAbTests has been called.
        }

        then:
        successfulUpdateLogExpectation.assertObservation()
        1 * abTestsCache.cacheAbTests(runningAbTests)

        cleanup:
        scheduler.shutdown()
    }

    def "initialize should schedule retries up to a maximum number"() {
        given:

        AbTestsCache abTestsCache = Mock()

        RunningAbTestsFetcher runningAbTestsFetcher = Mock()

        AbTestsRetrievalScheduler scheduler = new AbTestsRetrievalScheduler()

        LogAssert maxRetryLogExpectation = TestLoggers.sys().expect(
                AbTestsRetrievalScheduler.class.getName(),
                Level.ERROR,
                LogMatchers.hasMessage("Max retries exceeded with fetching A/B tests. An outdated cache will be " +
                        "used."))

        when:
        scheduler.start(
                abTestsCache,
                runningAbTestsFetcher,
                new SchedulingOptions(1000, 50, TimeUnit.MILLISECONDS))

        Thread.sleep(400)

        then:
        maxRetryLogExpectation.assertObservation()
        (MAX_RETRIES + 1) * runningAbTestsFetcher.getRunningAbTests() >> { throw new IllegalArgumentException() }
        0 * abTestsCache.cacheAbTests(_)

        cleanup:
        scheduler.shutdown()
    }


}

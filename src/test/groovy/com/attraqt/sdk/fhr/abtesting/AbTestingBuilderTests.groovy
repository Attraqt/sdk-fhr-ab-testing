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

package com.attraqt.sdk.fhr.abtesting

import java.util.concurrent.TimeUnit

import org.spf4j.log.Level
import org.spf4j.test.log.LogAssert
import org.spf4j.test.log.TestLoggers
import org.spf4j.test.matchers.LogMatchers

import com.attraqt.sdk.fhr.abtesting.caching.AbTestsCache

import com.attraqt.sdk.fhr.abtesting.model.SchedulingOptions
import com.attraqt.sdk.fhr.abtesting.retrieval.RunningAbTestsFetcher
import com.attraqt.sdk.fhr.abtesting.scheduling.AbTestsRetrievalScheduler

import spock.lang.Specification

class AbTestingBuilderTests extends Specification {
    private static Integer DEFAULT_CACHE_MINUTES = 5

    def "should throw an IllegalArgumentException if no abTestsServerUrl is provided"() {
        given:
        def builder = AbTesting.builder()

        when:
        builder.build()
        then:
        def exception = thrown IllegalArgumentException
        exception.message == "abTestsServerUrl required by AbTestingBuilder is not provided."
    }

    def "should throw an IllegalArgumentException if no username is provided with default A/B tests fetcher"() {
        given:
        def builder = AbTesting.builder().abTestsServerUrl("url")

        when:
        builder.build()

        then:
        def exception = thrown IllegalArgumentException
        exception.message == "username required by AbTestingBuilder is not provided."
    }

    def "should throw an IllegalArgumentException if no password is provided with default A/B tests fetcher"() {
        given:
        def builder = AbTesting.builder().abTestsServerUrl("url").username("username")

        when:
        builder.build()

        then:
        def exception = thrown IllegalArgumentException
        exception.message == "password required by AbTestingBuilder is not provided."
    }

    def "should pass the provided AbTestsCache to AbTestsRetrievalScheduler if provided"() {
        given:
        AbTestsRetrievalScheduler schedulerMock = Mock()
        AbTestsCache abTestsCache = Mock()

        AbTesting abTesting =
                AbTesting.builder()
                        .abTestsServerUrl("url")
                        .username("username")
                        .password("password")
                        .abTestsCache(abTestsCache)
                        .abTestsRetrievalScheduler(schedulerMock)
                        .build()

        abTesting.abTestsRetrievalScheduler = schedulerMock

        when:
        abTesting.start()
        abTesting.shutdown()

        then:
        1 * schedulerMock.start(abTestsCache,_, _)
    }

    def "should pass the provided RunningAbTestsFetcher to AbTestsRetrievalScheduler where provided"() {
        given:
        AbTestsRetrievalScheduler schedulerMock = Mock()
        RunningAbTestsFetcher runningAbTestsFetcher = Mock()

        AbTesting abTesting =
                AbTesting.builder()
                        .abTestsServerUrl("url")
                        .username("username")
                        .password("password")
                        .runningAbTestsFetcher(runningAbTestsFetcher)
                        .abTestsRetrievalScheduler(schedulerMock)
                        .build()

        abTesting.abTestsRetrievalScheduler = schedulerMock

        when:
        abTesting.start()
        abTesting.shutdown()

        then:
        1 * schedulerMock.start(_,runningAbTestsFetcher, _)
    }


    def "should enforce a minimum value when setting the abTestsRetrievalScheduler's cacheExpireTimeMinutes"() {
        given:
        AbTestsRetrievalScheduler schedulerMock = Mock()

        def builder =
                AbTesting.builder()
                        .abTestsServerUrl("url")
                        .username("username")
                        .password("password")
                        .cacheExpireTimeMinutes(specifiedMinutes)

        LogAssert belowMinimumLogMessageAssert =
                belowMinimumLogMessage ?
                        setupLogExpectation(Level.WARN, belowMinimum(specifiedMinutes)) :
                        setupNotLoggedExpectation(Level.WARN, belowMinimum(specifiedMinutes))

        LogAssert noMinimumLogMessageAssert =
                noMinimumLogMessage ?
                        setupLogExpectation(Level.INFO, noMinimumLogMessage()) :
                        setupNotLoggedExpectation(Level.INFO, noMinimumLogMessage())

        when:
        AbTesting abTesting = builder.build()
        abTesting.abTestsRetrievalScheduler = schedulerMock
        abTesting.start()
        abTesting.shutdown()

        then:
        belowMinimumLogMessageAssert.assertObservation()
        noMinimumLogMessageAssert.assertObservation()
        1 * schedulerMock.start(_, _, new SchedulingOptions(expectedMinutes, 1, TimeUnit.MINUTES))

        where:
        specifiedMinutes | expectedMinutes       | belowMinimumLogMessage   | noMinimumLogMessage
        -128             | DEFAULT_CACHE_MINUTES | true                     | false
        0                | DEFAULT_CACHE_MINUTES | true                     | false
        1                | DEFAULT_CACHE_MINUTES | true                     | false
        4                | DEFAULT_CACHE_MINUTES | true                     | false
        null             | DEFAULT_CACHE_MINUTES | false                    | true
        5                | 5                     | false                    | false
        6                | 6                     | false                    | false
        128              | 128                   | false                    | false
    }

    private static String belowMinimum(Integer specifiedMinutes) {
        return "The given expiration time for the A/B tests cache of $specifiedMinutes minutes is less than " +
                        "default $DEFAULT_CACHE_MINUTES minutes. Using default."

    }

    private static String noMinimumLogMessage() {
        return "No cache expiry time specified for the A/B tests cache. Using default of $DEFAULT_CACHE_MINUTES minutes."
    }

    private static LogAssert setupNotLoggedExpectation(Level logLevel, String message) {
        TestLoggers.sys().dontExpect(AbTesting.class.getName(), logLevel, LogMatchers.hasMessage(message))
    }

    private static LogAssert setupLogExpectation(Level logLevel, String message) {
        TestLoggers.sys().expect(AbTesting.class.getName(), logLevel, LogMatchers.hasMessage(message))
    }
}

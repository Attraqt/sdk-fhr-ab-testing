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

import com.attraqt.sdk.fhr.abtesting.caching.AbTestsCache

import com.attraqt.sdk.fhr.abtesting.model.RunningAbTest
import com.attraqt.sdk.fhr.abtesting.scheduling.AbTestsRetrievalScheduler

import spock.lang.Specification

class AbTestingTests extends Specification {
    def "should not change the provided parameters if no A/B tests are available in the cache (MAP)"() {
        given:
        AbTestsCache cache = Mock()
        AbTesting abTesting = createAbTestingWithCache(cache)
        abTesting.start()

        Map<String, List<String>> passedParams = ["param": ["value"]]

        when:
        def result = abTesting.appendAbTestsParameter("sessionId", passedParams)
        abTesting.shutdown()

        then:
        1 * cache.getAbTests() >> []
        result == passedParams
    }

    def "should not append the A/B tests if they are filtered out (MAP)"() {
        given:
        AbTestsCache cache = Mock()
        AbTesting abTesting = createAbTestingWithCache(cache)
        abTesting.start()

        Map<String, List<String>> passedParams = ["param": ["value"]]

        RunningAbTest runningAbTest =
                new RunningAbTest(
                        "id",
                        buildAbTestVariants(),
                        ["param": ["notValue"]]
                )

        when:
        def result = abTesting.appendAbTestsParameter("sessionId", passedParams)
        abTesting.shutdown()

        then:
        1 * cache.getAbTests() >> [runningAbTest]
        result == ["param": ["value"]]
    }

    def "should append the applicable A/B tests and variants if available (MAP)"() {
        given:
        AbTestsCache cache = Mock()
        AbTesting abTesting = createAbTestingWithCache(cache)
        abTesting.start()

        Map<String, List<String>> passedParams = ["param": ["value"]]

        RunningAbTest runningAbTest =
                new RunningAbTest(
                        "testId",
                        buildAbTestVariants(),
                        ["param": ["value"]]
                )

        when:
        def result = abTesting.appendAbTestsParameter("sessionId", passedParams)
        abTesting.shutdown()

        then:
        1 * cache.getAbTests() >> [runningAbTest]
        result == ["param": ["value"], "fh_abtests": ["testId:A"]]
    }

    def "should split the traffic within the tolerance of a specified percentage difference tolerance"() {
        given:

        RunningAbTest runningAbTest =
                new RunningAbTest(
                        "testId",
                        buildAbTestVariants(),
                        ["param": ["value"]]
                )

        AbTestsCache cache = Mock()
        cache.getAbTests() >> [runningAbTest]
        AbTesting abTesting = createAbTestingWithCache(cache)
        abTesting.start()

        Map<String, List<String>> passedParams = ["param": ["value"]]

        when:
        List<List<String>> results = []

        for (i in 0..<iterations) {
            UUID uuid = UUID.randomUUID()
            def result = abTesting.appendAbTestsParameter(uuid.toString(), passedParams)
            results.add(result["fh_abtests"])
        }

        def variantDistribution = results.countBy { s -> s }

        def percentageDifference =
                percentageDifference(variantDistribution[["testId:A"]], variantDistribution[["testId:B"]])

        abTesting.shutdown()

        then:
        percentageDifference < maximumPercentageDifference

        where:
        iterations | maximumPercentageDifference
        100000     | 2
        1000000    | 1
    }

    private static float percentageDifference(int a, int b) {
        return 100 * Math.abs((a - b) / ((a + b) / 2))
    }

    private static def buildAbTestVariants() {
        [new RunningAbTest.RunningAbTestVariant("A", 50),
         new RunningAbTest.RunningAbTestVariant("B", 50)]
    }

    private def createAbTestingWithCache(AbTestsCache abTestsCache) {
        AbTestsRetrievalScheduler retrievalScheduler = Mock() // Stop retrieval from being scheduled, as will be mocked

        return AbTesting.builder()
                .abTestsServerUrl("dummyUrl")
                .username("dummy username")
                .password("password")
                .abTestsRetrievalScheduler(retrievalScheduler)
                .abTestsCache(abTestsCache)
                .build()
    }
}

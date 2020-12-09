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

package com.attraqt.sdk.fhr.abtesting.model;


import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.EqualsAndHashCode;
import lombok.Getter;

@JsonIgnoreProperties(ignoreUnknown = true)
@EqualsAndHashCode
@Getter
public final class RunningAbTest {

    /**
     * Returns the id of the ab-test.
     *
     * @return The id of the ab-test.
     */
    private final String id;
    /**
     * Returns the list of ab-test cases.
     *
     * @return A list of {@link RunningAbTestVariant}
     */
    private final List<RunningAbTestVariant> variations;
    /**
     * Returns an ab-test filters map which are used to trigger A/B tests.
     *
     * @return An ab-test filters map.
     */
    private final Map<String, List<String>> filters;

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public RunningAbTest(@JsonProperty("id") String id,
                         @JsonProperty("variations") List<RunningAbTestVariant> variations,
                         @JsonProperty("filters") Map<String, List<String>> filters
    ) {
        this.id = id;
        this.variations = variations;
        this.filters = filters;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    @EqualsAndHashCode
    @Getter
    public static final class RunningAbTestVariant {

        /**
         * Returns the id of the ab-test case.
         *
         * @return The id of the ab-test case.
         */
        private final String id;
        /**
         * Returns the weight of the ab-test case which is used to allocate traffic between test cases.
         *
         * @return The weight.
         */
        private final Integer weight;

        @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
        public RunningAbTestVariant(@JsonProperty("id") String id, @JsonProperty("weight") Integer weight) {
            this.id = id;
            this.weight = weight;
        }
    }
}



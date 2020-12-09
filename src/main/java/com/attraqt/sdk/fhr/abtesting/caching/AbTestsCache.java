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

package com.attraqt.sdk.fhr.abtesting.caching;

import java.util.List;

import com.attraqt.sdk.fhr.abtesting.model.RunningAbTest;

/**
 * An interface to store/retrieve A/B tests to/from the cache.
 */
public interface AbTestsCache {

    /**
     * Updates the cache with given A/B tests.
     *
     * @param abTests The A/B tests to be cached.
     */
    void cacheAbTests(List<RunningAbTest> abTests);

    /**
     * Returns the A/B tests from the cache.
     * <p>
     * It returns null if nothing has been cached yet.
     * <p>
     * If FHR A/B Tests Service returns an empty list which means there is no active A/B tests it will return an empty
     * list.
     *
     * @return A list of {@link RunningAbTest}
     */
    List<RunningAbTest> getAbTests();
}

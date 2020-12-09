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
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

/**
 * An in memory implementation of {@link AbTestsCache}
 */
public class InMemoryAbTestsCache implements AbTestsCache {

    private static final String CACHE_KEY = "key";
    private final Cache<String, List<RunningAbTest>> cache;

    /**
     * Initializes the cache and constructs an {@link InMemoryAbTestsCache} object.
     */
    public InMemoryAbTestsCache() {
        cache = CacheBuilder.newBuilder()
            .initialCapacity(1)
            .build();
    }

    public void cacheAbTests(List<RunningAbTest> abTests) {
        cache.put(CACHE_KEY, abTests);
    }
    
    public List<RunningAbTest> getAbTests() {
        return cache.getIfPresent(CACHE_KEY);
    }
}

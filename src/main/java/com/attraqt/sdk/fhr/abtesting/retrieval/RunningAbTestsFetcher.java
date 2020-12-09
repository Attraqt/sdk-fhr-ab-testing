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

import java.util.List;

import com.attraqt.sdk.fhr.abtesting.model.RunningAbTest;

/**
 * An interface to fetch running A/B tests from FHR A/B Tests Service.
 */
public interface RunningAbTestsFetcher {

    /**
     * Fetches running A/B tests from FHR A/B Tests Service and returns.
     *
     * @return A list of {@link RunningAbTest}
     */
    List<RunningAbTest> getRunningAbTests();
}

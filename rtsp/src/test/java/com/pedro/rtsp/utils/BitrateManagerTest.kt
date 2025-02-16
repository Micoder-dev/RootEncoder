/*
 * Copyright (C) 2021 pedroSG94.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.pedro.rtsp.utils

import com.pedro.rtsp.MainDispatcherRule
import com.pedro.rtsp.Utils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.times
import org.mockito.kotlin.verify


/**
 * Created by pedro on 14/4/22.
 */
@RunWith(MockitoJUnitRunner::class)
class BitrateManagerTest {

  @get:Rule
  val mainDispatcherRule = MainDispatcherRule()
  @Mock
  private lateinit var connectCheckerRtsp: ConnectCheckerRtsp
  private val timeUtilsMocked = Mockito.mockStatic(TimeUtils::class.java)
  private var fakeTime = 7502849023L

  @Before
  fun setup() {
    timeUtilsMocked.`when`<Long>(TimeUtils::getCurrentTimeMillis).then { fakeTime }
  }

  @After
  fun teardown() {
    fakeTime = 7502849023L
  }

  @Test
  fun `WHEN set multiple values THEN return total of values each second`() = runTest {
    Utils.useStatics(listOf(timeUtilsMocked)) {
      val bitrateManager = BitrateManager(connectCheckerRtsp)
      val fakeValues = arrayOf(100L, 200L, 300L, 400L, 500L)
      var expectedResult = 0L
      fakeValues.forEach {
        bitrateManager.calculateBitrate(it)
        expectedResult += it
      }
      fakeTime += 1000
      val value = 100L
      bitrateManager.calculateBitrate(value)
      expectedResult += value
      val resultValue = argumentCaptor<Long>()
      verify(connectCheckerRtsp, times(1)).onNewBitrateRtsp(resultValue.capture())
      val marginError = 20
      assertTrue(expectedResult - marginError <= resultValue.firstValue && resultValue.firstValue <= expectedResult + marginError)
    }
  }
}
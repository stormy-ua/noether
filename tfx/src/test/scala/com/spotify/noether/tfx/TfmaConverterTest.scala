/*
 * Copyright 2018 Spotify AB.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.spotify.noether.tfx

import com.spotify.noether._
import org.scalactic.TolerantNumerics
import org.scalatest.{FlatSpec, Matchers}
import tensorflow_model_analysis.MetricsForSliceOuterClass.MetricsForSlice

class TfmaConverterTest extends FlatSpec with Matchers {

  "Stuff" should "work" in {
    val binAgg = BinaryConfusionMatrix().asTfmaProto

    val aucAgg = AUC(ROC).asTfmaProto
  }

  "ConfusionMatrix converter" should "work" in {
    val data = List(
      (0, 0),
      (0, 1),
      (0, 0),
      (1, 0),
      (1, 1),
      (1, 1),
      (1, 1)
    ).map { case (s, pred) => Prediction(pred, s) }

    val cmProto: MetricsForSlice = ConfusionMatrix(Seq(0, 1)).asTfmaProto(data)

    val cm = cmProto
      .getMetricsMap()
      .get("Noether_ConfusionMatrix")
      .getConfusionMatrixAtThresholds
      .getMatrices(0)

    assert(cm.getFalseNegatives === 1L)
    assert(cm.getFalsePositives === 1L)
    assert(cm.getTrueNegatives === 2L)
    assert(cm.getTruePositives === 3L)
  }

  "BinaryConfusionMatrix converter" should "work" in {
    val data = List(
      (false, 0.1),
      (false, 0.6),
      (false, 0.2),
      (true, 0.2),
      (true, 0.8),
      (true, 0.7),
      (true, 0.6)
    ).map { case (pred, s) => Prediction(pred, s) }

    val cmProto: MetricsForSlice = BinaryConfusionMatrix().asTfmaProto(data)

    val cm = cmProto
      .getMetricsMap()
      .get("Noether_ConfusionMatrix")
      .getConfusionMatrixAtThresholds
      .getMatrices(0)

    assert(cm.getThreshold === 0.5)
    assert(cm.getTruePositives === 3L)
    assert(cm.getFalseNegatives === 1L)
    assert(cm.getFalsePositives === 1L)
    assert(cm.getTrueNegatives === 2L)
  }

  "Error rate summary" should "work" in {
    implicit val doubleEq = TolerantNumerics.tolerantDoubleEquality(0.1)
    val classes = 10
    def s(idx: Int): List[Double] = 0.until(classes).map(i => if (i == idx) 1.0 else 0.0).toList

    val data =
      List((s(1), 1), (s(3), 1), (s(5), 5), (s(2), 3), (s(0), 0), (s(8), 1)).map {
        case (scores, label) => Prediction(label, scores)
      }

    val ersProto: MetricsForSlice = ErrorRateSummary.asTfmaProto(data)

    val ersV =
      ersProto.getMetricsMap.get("Noether_ErrorRateSummary").getDoubleValue.getValue
    assert(ersV === 0.5)
  }
}
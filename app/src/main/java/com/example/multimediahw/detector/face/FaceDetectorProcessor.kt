package com.example.multimediahw.detector.face

import android.content.Context
import android.util.Log
import android.widget.Toast
import com.google.android.gms.tasks.Task
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.google.mlkit.vision.face.FaceLandmark
import java.util.Locale
import com.example.multimediahw.detector.GraphicOverlay
import com.example.multimediahw.detector.VisionProcessorBase
import com.google.mlkit.vision.face.FaceDetector

/** Face Detector Demo.  */
class FaceDetectorProcessor(context: Context, detectorOptions: FaceDetectorOptions?) :
  VisionProcessorBase<List<Face>>(context) {

  private val detector: FaceDetector
  private val context: Context = context

  private val blinkCount: MutableMap<Int, Int> = mutableMapOf()
  private val startTime: MutableMap<Int, Long> = mutableMapOf()
  private val currentEyeState: MutableMap<Int, EyeStates> = mutableMapOf()

  init {
    val options = detectorOptions ?: FaceDetectorOptions.Builder()
      .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
      .enableTracking()
      .build()

    detector = FaceDetection.getClient(options)

    Log.v(MANUAL_TESTING_LOG, "Face detector options: $options")
  }

  override fun stop() {
    super.stop()
    detector.close()
  }

  override fun detectInImage(image: InputImage): Task<List<Face>> {
    return detector.process(image)
  }

  override fun onSuccess(faces: List<Face>, graphicOverlay: GraphicOverlay) {
    for (face in faces) {
      if (face.trackingId != null) {

        if (face.trackingId !in blinkCount.keys) {
          startTime[face.trackingId!!] = System.currentTimeMillis()
          blinkCount[face.trackingId!!] = 0
        }

        if (face.leftEyeOpenProbability == null || face.rightEyeOpenProbability == null) {
          currentEyeState[face.trackingId!!] = EyeStates.NO_INFO
        } else if (face.leftEyeOpenProbability!! > 0.4 && face.rightEyeOpenProbability!! > 0.4) {
          currentEyeState[face.trackingId!!] = EyeStates.OPEN
        } else if (face.leftEyeOpenProbability!! <= 0.4 && face.rightEyeOpenProbability!! > 0.4) {
          currentEyeState[face.trackingId!!] = EyeStates.LEFT_CLOSED
        } else if (face.leftEyeOpenProbability!! > 0.4 && face.rightEyeOpenProbability!! <= 0.4) {
          currentEyeState[face.trackingId!!] = EyeStates.RIGHT_CLOSED
        } else if (face.leftEyeOpenProbability!! <= 0.4 && face.rightEyeOpenProbability!! <= 0.4) {
          if (currentEyeState[face.trackingId!!] != EyeStates.CLOSED) {
            blinkCount[face.trackingId!!] = blinkCount[face.trackingId]!! + 1
            currentEyeState[face.trackingId!!] = EyeStates.CLOSED
          }
        } else {
          currentEyeState[face.trackingId!!] = EyeStates.NO_INFO
        }

        // Проверка на подделку
        if (blinkCount[face.trackingId!!]!! < 1 &&
          System.currentTimeMillis() - startTime[face.trackingId!!]!! > 4000) {
          // Обнаружена возможная подделка
          Toast.makeText(context, "Обнаружена возможная подделка с использованием фотографии!", Toast.LENGTH_LONG).show()
        }

        graphicOverlay.add(
          FaceGraphic(
            graphicOverlay, face,
            blinkCount[face.trackingId!!]!!, startTime[face.trackingId!!]!!
          )
        )
      }

      logExtrasForTesting(face)
    }
  }

  override fun onFailure(e: Exception) {
    Log.e(TAG, "Face detection failed $e")
  }

  companion object {
    private const val TAG = "FaceDetectorProcessor"
    private const val MANUAL_TESTING_LOG = "FaceDetectionDemo"

    private fun logExtrasForTesting(face: Face?) {
      if (face != null) {
        Log.v(
          MANUAL_TESTING_LOG,
          "face bounding box: " + face.boundingBox.flattenToString()
        )
        Log.v(
          MANUAL_TESTING_LOG,
          "face Euler Angle X: " + face.headEulerAngleX
        )
        Log.v(
          MANUAL_TESTING_LOG,
          "face Euler Angle Y: " + face.headEulerAngleY
        )
        Log.v(
          MANUAL_TESTING_LOG,
          "face Euler Angle Z: " + face.headEulerAngleZ
        )
        // All landmarks
        val landMarkTypes = intArrayOf(
          FaceLandmark.MOUTH_BOTTOM,
          FaceLandmark.MOUTH_RIGHT,
          FaceLandmark.MOUTH_LEFT,
          FaceLandmark.RIGHT_EYE,
          FaceLandmark.LEFT_EYE,
          FaceLandmark.RIGHT_EAR,
          FaceLandmark.LEFT_EAR,
          FaceLandmark.RIGHT_CHEEK,
          FaceLandmark.LEFT_CHEEK,
          FaceLandmark.NOSE_BASE
        )
        val landMarkTypesStrings = arrayOf(
          "MOUTH_BOTTOM",
          "MOUTH_RIGHT",
          "MOUTH_LEFT",
          "RIGHT_EYE",
          "LEFT_EYE",
          "RIGHT_EAR",
          "LEFT_EAR",
          "RIGHT_CHEEK",
          "LEFT_CHEEK",
          "NOSE_BASE"
        )
        for (i in landMarkTypes.indices) {
          val landmark = face.getLandmark(landMarkTypes[i])
          if (landmark == null) {
            Log.v(
              MANUAL_TESTING_LOG,
              "No landmark of type: " + landMarkTypesStrings[i] + " has been detected"
            )
          } else {
            val landmarkPosition = landmark.position
            val landmarkPositionStr =
              String.format(Locale.US, "x: %f , y: %f", landmarkPosition.x, landmarkPosition.y)
            Log.v(
              MANUAL_TESTING_LOG,
              "Position for face landmark: " +
                      landMarkTypesStrings[i] +
                      " is :" +
                      landmarkPositionStr
            )
          }
        }
        Log.v(
          MANUAL_TESTING_LOG,
          "face left eye open probability: " + face.leftEyeOpenProbability
        )
        Log.v(
          MANUAL_TESTING_LOG,
          "face right eye open probability: " + face.rightEyeOpenProbability
        )
        Log.v(
          MANUAL_TESTING_LOG,
          "face smiling probability: " + face.smilingProbability
        )
        Log.v(
          MANUAL_TESTING_LOG,
          "face tracking id: " + face.trackingId
        )
      }
    }

    enum class EyeStates {
      OPEN, CLOSED, LEFT_CLOSED, RIGHT_CLOSED, NO_INFO
    }
  }
}
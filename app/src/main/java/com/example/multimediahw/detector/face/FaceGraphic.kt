package com.example.multimediahw.detector.face
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import com.example.multimediahw.detector.GraphicOverlay
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceLandmark
import com.google.mlkit.vision.face.FaceLandmark.LandmarkType
import java.lang.Math.pow
import java.util.Locale
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.sqrt

class FaceGraphic(overlay: GraphicOverlay?, private val face: Face,
                  private val countBlinks: Int, private val startTime: Long)
  : GraphicOverlay.Graphic(overlay) {
  private val facePositionPaint: Paint
  private val numColors = COLORS.size
  private val idPaints = Array(numColors) { Paint() }
  private val boxPaints = Array(numColors) { Paint() }
  private val labelPaints = Array(numColors) { Paint() }

  init {
    val selectedColor = Color.WHITE
    facePositionPaint = Paint()
    facePositionPaint.color = selectedColor
    for (i in 0 until numColors) {
      idPaints[i] = Paint()
      idPaints[i].color = COLORS[i][0]
      idPaints[i].textSize = ID_TEXT_SIZE
      boxPaints[i] = Paint()
      boxPaints[i].color = COLORS[i][1]
      boxPaints[i].style = Paint.Style.STROKE
      boxPaints[i].strokeWidth = BOX_STROKE_WIDTH
      labelPaints[i] = Paint()
      labelPaints[i].color = COLORS[i][1]
      labelPaints[i].style = Paint.Style.FILL
    }
  }

  /** Draws the face annotations for position on the supplied canvas. */
  override fun draw(canvas: Canvas?) {
    // Draws a circle at the position of the detected face, with the face's track id below.

    // Draws a circle at the position of the detected face, with the face's track id below.
    val x = translateX(face.boundingBox.centerX().toFloat())
    val y = translateY(face.boundingBox.centerY().toFloat())
    canvas?.drawCircle(x, y, FACE_POSITION_RADIUS, facePositionPaint)

    // Calculate positions.
    val left = x - scale(face.boundingBox.width() / 2.0f)
    val top = y - scale(face.boundingBox.height() / 2.0f)
    val right = x + scale(face.boundingBox.width() / 2.0f)
    val bottom = y + scale(face.boundingBox.height() / 2.0f)
    val lineHeight = ID_TEXT_SIZE + BOX_STROKE_WIDTH
    var yLabelOffset: Float = if (face.trackingId == null) 0f else -lineHeight

    // Decide color based on face ID
    val colorID = if (face.trackingId == null) 0 else abs(face.trackingId!! % NUM_COLORS)

    // Calculate width and height of label box
    var textWidth = idPaints[colorID].measureText("ID: " + face.trackingId)
    if (face.smilingProbability != null) {
      yLabelOffset -= lineHeight
      textWidth =
        max(
          textWidth,
          idPaints[colorID].measureText(
            String.format(Locale.US, "Happiness: %.2f", face.smilingProbability)
          )
        )
    }
    if (face.leftEyeOpenProbability != null) {
      yLabelOffset -= lineHeight
      textWidth =
        max(
          textWidth,
          idPaints[colorID].measureText(
            String.format(Locale.US, "Left eye open: %.2f", face.leftEyeOpenProbability)
          )
        )
    }
    if (face.rightEyeOpenProbability != null) {
      yLabelOffset -= lineHeight
      textWidth =
        max(
          textWidth,
          idPaints[colorID].measureText(
            String.format(Locale.US, "Right eye open: %.2f", face.rightEyeOpenProbability)
          )
        )
    }

    yLabelOffset = yLabelOffset - 3 * lineHeight
    textWidth =
      Math.max(
        textWidth,
        idPaints[colorID].measureText(
          String.format(Locale.US, "EulerX: %.2f", face.headEulerAngleX)
        )
      )
    textWidth =
      Math.max(
        textWidth,
        idPaints[colorID].measureText(
          String.format(Locale.US, "EulerY: %.2f", face.headEulerAngleY)
        )
      )
    textWidth =
      Math.max(
        textWidth,
        idPaints[colorID].measureText(
          String.format(Locale.US, "EulerZ: %.2f", face.headEulerAngleZ)
        )
      )

    // Draw labels
    canvas?.drawRect(
      left - BOX_STROKE_WIDTH,
      top + yLabelOffset,
      left + textWidth + 2 * BOX_STROKE_WIDTH,
      top,
      labelPaints[colorID]
    )
    yLabelOffset += ID_TEXT_SIZE
    canvas?.drawRect(left, top, right, bottom, boxPaints[colorID])
    if (face.trackingId != null) {
      canvas?.drawText("ID: " + face.trackingId, left, top + yLabelOffset, idPaints[colorID])
      yLabelOffset += lineHeight
    }

    // Draws all face contours.
    for (contour in face.allContours) {
      for (point in contour.points) {
        canvas?.drawCircle(
          translateX(point.x),
          translateY(point.y),
          FACE_POSITION_RADIUS,
          facePositionPaint
        )
      }
    }

    // Draws smiling and left/right eye open probabilities.
    if (face.smilingProbability != null) {
      canvas?.drawText(
        "Smiling: " + String.format(Locale.US, "%.2f", face.smilingProbability),
        left,
        top + yLabelOffset,
        idPaints[colorID]
      )
      yLabelOffset += lineHeight
    }

    val leftEye = face.getLandmark(FaceLandmark.LEFT_EYE)
    if (face.leftEyeOpenProbability != null) {
      canvas?.drawText(
        "Left eye open: " + String.format(Locale.US, "%.2f", face.leftEyeOpenProbability),
        left,
        top + yLabelOffset,
        idPaints[colorID]
      )
      yLabelOffset += lineHeight
    }
    if (leftEye != null) {
      val leftEyeLeft =
        translateX(leftEye.position.x) - idPaints[colorID].measureText("Left Eye") / 2.0f
      canvas?.drawRect(
        leftEyeLeft - BOX_STROKE_WIDTH,
        translateY(leftEye.position.y) + ID_Y_OFFSET - ID_TEXT_SIZE,
        leftEyeLeft + idPaints[colorID].measureText("Left Eye") + BOX_STROKE_WIDTH,
        translateY(leftEye.position.y) + ID_Y_OFFSET + BOX_STROKE_WIDTH,
        labelPaints[colorID]
      )
      canvas?.drawText(
        "Left Eye",
        leftEyeLeft,
        translateY(leftEye.position.y) + ID_Y_OFFSET,
        idPaints[colorID]
      )
    }

    val rightEye = face.getLandmark(FaceLandmark.RIGHT_EYE)
    if (face.rightEyeOpenProbability != null) {
      canvas?.drawText(
        "Right eye open: " + String.format(Locale.US, "%.2f", face.rightEyeOpenProbability),
        left,
        top + yLabelOffset,
        idPaints[colorID]
      )
      yLabelOffset += lineHeight
    }
    if (rightEye != null) {
      val rightEyeLeft =
        translateX(rightEye.position.x) - idPaints[colorID].measureText("Right Eye") / 2.0f
      canvas?.drawRect(
        rightEyeLeft - BOX_STROKE_WIDTH,
        translateY(rightEye.position.y) + ID_Y_OFFSET - ID_TEXT_SIZE,
        rightEyeLeft + idPaints[colorID].measureText("Right Eye") + BOX_STROKE_WIDTH,
        translateY(rightEye.position.y) + ID_Y_OFFSET + BOX_STROKE_WIDTH,
        labelPaints[colorID]
      )
      canvas?.drawText(
        "Right Eye",
        rightEyeLeft,
        translateY(rightEye.position.y) + ID_Y_OFFSET,
        idPaints[colorID]
      )
    }

    canvas?.drawText("EulerX: " + face.headEulerAngleX, left, top + yLabelOffset, idPaints[colorID])
    yLabelOffset += lineHeight
    canvas?.drawText("EulerY: " + face.headEulerAngleY, left, top + yLabelOffset, idPaints[colorID])
    yLabelOffset += lineHeight
    canvas?.drawText("EulerZ: " + face.headEulerAngleZ, left, top + yLabelOffset, idPaints[colorID])

    // Draw facial landmarks
    if (canvas != null) {
      drawFaceLandmark(canvas, FaceLandmark.LEFT_EYE)
      drawFaceLandmark(canvas, FaceLandmark.RIGHT_EYE)
      drawFaceLandmark(canvas, FaceLandmark.LEFT_CHEEK)
      drawFaceLandmark(canvas, FaceLandmark.RIGHT_CHEEK)


      // Blink
      canvas.drawRect(
        left - BOX_STROKE_WIDTH,
        bottom,
        right + BOX_STROKE_WIDTH,
        bottom + 4 * lineHeight,
        labelPaints[colorID]
      )

      if (face.leftEyeOpenProbability == null || face.rightEyeOpenProbability == null) {
        canvas.drawText("Недостаточно информации", left, bottom + lineHeight, idPaints[colorID])
      } else if (face.leftEyeOpenProbability!! > 0.4 && face.rightEyeOpenProbability!! > 0.4) {
        canvas.drawText("Глаза открыты", left, bottom + lineHeight, idPaints[colorID])
      } else if (face.leftEyeOpenProbability!! <= 0.4 && face.rightEyeOpenProbability!! > 0.4) {
        canvas.drawText("Закрыт левый глаз", left, bottom + lineHeight, idPaints[colorID])
      } else if (face.leftEyeOpenProbability!! > 0.4 && face.rightEyeOpenProbability!! <= 0.4) {
        canvas.drawText("Закрыт правый глаз", left, bottom + lineHeight, idPaints[colorID])
      } else if (face.leftEyeOpenProbability!! <= 0.4 && face.rightEyeOpenProbability!! <= 0.4) {
        canvas.drawText("Закрыты оба глаза", left, bottom + lineHeight, idPaints[colorID])
      } else {
        canvas.drawText("Недостаточно информации", left, bottom + lineHeight, idPaints[colorID])
      }

      canvas.drawText("Всего морганий: $countBlinks", left,
        bottom + 2 * lineHeight, idPaints[colorID])
      canvas.drawText("Частота в минуту: " + "%.3f".format( countBlinks /
              ((System.currentTimeMillis() - startTime) / 60000.0)), left,
        bottom + 3 * lineHeight, idPaints[colorID])



      /*
     // Yawn

     val mouthBottom = face.getLandmark(FaceLandmark.MOUTH_BOTTOM)
     val mouthLeft = face.getLandmark(FaceLandmark.MOUTH_LEFT)
     val mouthRight = face.getLandmark(FaceLandmark.MOUTH_RIGHT)
     val nose = face.getLandmark(FaceLandmark.NOSE_BASE)

     if (mouthRight != null && mouthBottom != null && mouthLeft != null) {
       val proportion = nose?.let {
         ((nose.position.y - mouthBottom.position.y) /
                 (mouthLeft.position.x - mouthRight.position.x))
       }
       val a2 = (mouthRight.position.x - mouthBottom.position.x).pow(2) +
               (mouthRight.position.y - mouthBottom.position.y).pow(2)
       val b2 = ((mouthLeft.position.x - mouthBottom.position.x)).pow(2) +
               ((mouthLeft.position.y - mouthBottom.position.y)).pow(2)
       val c2 = (mouthLeft.position.x - mouthRight.position.x).pow(2) +
               (mouthLeft.position.y - mouthRight.position.y).pow(2)
       val angle = acos((a2 + b2 - c2) / (2 * sqrt(a2) * sqrt(b2)))*(180/Math.PI)

       val isYawning =  if (proportion != null && proportion > 0.9) "Зевает" else ("Не зевает")
       canvas.drawText("($proportion) $isYawning", left, bottom + lineHeight, idPaints[colorID])
       canvas.drawText("Проп-ция: ($proportion)", left, bottom + 2 * lineHeight, idPaints[colorID])
       canvas.drawText("Угол: ($angle)", left, bottom + 3 * lineHeight, idPaints[colorID])

     }

      */
    }
  }



  private fun drawFaceLandmark(canvas: Canvas, @LandmarkType landmarkType: Int) {
    val faceLandmark = face.getLandmark(landmarkType)
    if (faceLandmark != null) {
      canvas.drawCircle(
        translateX(faceLandmark.position.x),
        translateY(faceLandmark.position.y),
        FACE_POSITION_RADIUS,
        facePositionPaint
      )
    }
  }

  companion object {
    private const val FACE_POSITION_RADIUS = 8.0f
    private const val ID_TEXT_SIZE = 30.0f
    private const val ID_Y_OFFSET = 40.0f
    private const val BOX_STROKE_WIDTH = 5.0f
    private const val NUM_COLORS = 10
    private val COLORS =
      arrayOf(
        intArrayOf(Color.BLACK, Color.WHITE),
        intArrayOf(Color.WHITE, Color.MAGENTA),
        intArrayOf(Color.BLACK, Color.LTGRAY),
        intArrayOf(Color.WHITE, Color.RED),
        intArrayOf(Color.WHITE, Color.BLUE),
        intArrayOf(Color.WHITE, Color.DKGRAY),
        intArrayOf(Color.BLACK, Color.CYAN),
        intArrayOf(Color.BLACK, Color.YELLOW),
        intArrayOf(Color.WHITE, Color.BLACK),
        intArrayOf(Color.BLACK, Color.GREEN)
      )
  }
}

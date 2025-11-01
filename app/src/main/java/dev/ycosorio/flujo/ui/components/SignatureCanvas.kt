package dev.ycosorio.flujo.ui.components

import android.view.MotionEvent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
data class StrokePath(val path: Path, val color: Color = Color.Black, val strokeWidth: Float = 5f)


@Composable
fun SignatureCanvas(
    modifier: Modifier = Modifier,
    onPathDrawn: (StrokePath) -> Unit,
    paths: List<StrokePath>
) {
    var currentPath = remember { Path() }

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White)
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        event.changes.forEach { change ->
                            when {
                                change.pressed && !change.previousPressed -> {
                                    // ACTION_DOWN
                                    currentPath = Path()
                                    currentPath.moveTo(change.position.x, change.position.y)
                                }
                                change.pressed && change.previousPressed -> {
                                    // ACTION_MOVE
                                    currentPath.lineTo(change.position.x, change.position.y)
                                    onPathDrawn(StrokePath(path = currentPath))
                                }
                            }
                            change.consume()
                        }
                    }
                }
            }
    ) {
        paths.forEach { (path, color, strokeWidth) ->
            drawPath(
                path = path,
                color = color,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
            )
        }
    }
}
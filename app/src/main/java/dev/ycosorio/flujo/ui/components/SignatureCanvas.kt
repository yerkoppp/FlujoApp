package dev.ycosorio.flujo.ui.components

import android.view.MotionEvent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInteropFilter

data class StrokePath(val path: Path, val color: Color = Color.Black, val strokeWidth: Float = 5f)

@OptIn(ExperimentalComposeUiApi::class)
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
            .pointerInteropFilter {
                when (it.action) {
                    MotionEvent.ACTION_DOWN -> {
                        currentPath = Path()
                        currentPath.moveTo(it.x, it.y)
                        true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        currentPath.lineTo(it.x, it.y)
                        onPathDrawn(StrokePath(path = currentPath))
                        true
                    }
                    else -> false
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
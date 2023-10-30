package com.juraj.agsl

import android.graphics.BitmapFactory
import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.core.view.WindowCompat
import com.juraj.agsl.ui.theme.AGSLShadersTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val IMG_SHADER_SRC = """
    uniform float2 size;
    uniform float time;
    uniform shader composable;
    
    half4 main(float2 fragCoord) {
        float scale = 1 / size.x;
        float2 scaledCoord = fragCoord * scale;
        float2 center = size * 0.5 * scale;
        float dist = distance(scaledCoord, center);
        float2 dir = scaledCoord - center;
        float sin = sin(dist * 70 - time * 6.28);
        float2 offset = dir * sin;
        float2 textCoord = scaledCoord + offset / 30;
        return composable.eval(textCoord / scale);
    }
"""

private const val FRACTAL_SHADER_SRC = """
    uniform float2 size;
    uniform float time;
    uniform shader composable;
    
    float f(float3 p) {
        p.z -= time * 5.;
        float a = p.z * .1;
        p.xy *= mat2(cos(a), sin(a), -sin(a), cos(a));
        return .1 - length(cos(p.xy) + sin(p.yz));
    }
    
    half4 main(float2 fragcoord) { 
        float3 d = .5 - fragcoord.xy1 / size.y;
        float3 p=float3(0);
        for (int i = 0; i < 32; i++) {
          p += f(p) * d;
        }
        return ((sin(p) + float3(2, 5, 12)) / length(p)).xyz1;
    }
"""

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        val shader = RuntimeShader(IMG_SHADER_SRC)
//        val shader = RuntimeShader(FRACTAL_SHADER_SRC) // TODO: uncomment to see 2nd shader
        val photo = BitmapFactory.decodeResource(resources, R.drawable.butterfly)

        setContent {
            val scope = rememberCoroutineScope()
            val timeMs = remember { mutableStateOf(0f) }
            LaunchedEffect(Unit) {
                scope.launch {
                    while (true) {
                        timeMs.value = (System.currentTimeMillis() % 100_000L) / 1_000f
                        delay(10)
                    }
                }
            }

            AGSLShadersTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier
                        .fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Image(
                        bitmap = photo.asImageBitmap(),
                        modifier = Modifier
                            .onSizeChanged { size ->
                                shader.setFloatUniform(
                                    "size",
                                    size.width.toFloat(),
                                    size.height.toFloat()
                                )
                            }
                            .graphicsLayer {
                                clip = true
                                shader.setFloatUniform("time",timeMs.value)
                                renderEffect =
                                    RenderEffect
                                        .createRuntimeShaderEffect(shader, "composable")
                                        .asComposeRenderEffect()
                            },
                        contentScale = ContentScale.FillHeight,
                        contentDescription = null,
                    )
                }

            }
        }
    }
}

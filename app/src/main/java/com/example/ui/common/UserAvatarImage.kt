package com.example.ui.common

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.R
import java.io.File

/**
 * Componente unificado de Avatar de Usuario en toda la aplicación:
 * - Si el usuario seleccionó una foto de avatar personalizada (archivo local o URL remota de Supabase),
 *   se muestra su foto real recortada en círculo con escala de contenido perfecta.
 * - Si no tiene foto personalizada aún, muestra como fallback elegante el jugador predeterminado o
 *   las iniciales del nombre con fondo degradado moderno.
 */
@Composable
fun UserAvatarImage(
    avatarUrl: String?,
    displayName: String = "Jugador",
    fallbackDrawable: Int = R.drawable.avatarchico,
    size: Dp = 40.dp,
    borderBrush: Brush? = null,
    borderColor: Color? = null,
    borderWidth: Dp = 2.dp,
    modifier: Modifier = Modifier
) {
    val localFileBitmap = remember(avatarUrl) {
        if (!avatarUrl.isNullOrBlank() && !avatarUrl.startsWith("http://") && !avatarUrl.startsWith("https://")) {
            try {
                val file = File(avatarUrl)
                if (file.exists()) {
                    BitmapFactory.decodeFile(file.absolutePath)?.asImageBitmap()
                } else null
            } catch (_: Exception) {
                null
            }
        } else {
            null
        }
    }

    var rootModifier = modifier
        .size(size)
        .clip(CircleShape)

    if (borderBrush != null) {
        rootModifier = rootModifier.background(borderBrush).padding(borderWidth).clip(CircleShape)
    } else if (borderColor != null) {
        rootModifier = rootModifier.border(borderWidth, borderColor, CircleShape)
    }

    Box(
        modifier = rootModifier.background(Color(0xFFE2E8F0)),
        contentAlignment = Alignment.Center
    ) {
        when {
            // 1. Imagen desde archivo local cargado por el usuario
            localFileBitmap != null -> {
                Image(
                    bitmap = localFileBitmap,
                    contentDescription = "Avatar de $displayName",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
            // 2. Imagen remota desde Supabase Storage o URL web
            !avatarUrl.isNullOrBlank() && (avatarUrl.startsWith("http://") || avatarUrl.startsWith("https://")) -> {
                AsyncImage(
                    model = avatarUrl,
                    contentDescription = "Avatar de $displayName",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
            // 3. Fallback: Foto por defecto del jugador
            else -> {
                Image(
                    painter = painterResource(id = fallbackDrawable),
                    contentDescription = "Avatar de jugador",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

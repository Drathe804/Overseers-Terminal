// Create this in a file named AuthProvider.kt
import androidx.compose.runtime.staticCompositionLocalOf
import com.dravenmiller.overseersterminal.GoogleAuthBridge

val LocalAuthBridge = staticCompositionLocalOf<GoogleAuthBridge?> { null }

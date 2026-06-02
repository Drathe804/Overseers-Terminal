import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.dravenmiller.overseersterminal.components.PipText
import com.dravenmiller.overseersterminal.theme.ThemeController

@Composable
fun PipPicker(
    title: String,
    options: List<String>,
    selectedIndex: Int,
    themeController: ThemeController,
    onOptionSelected: (Int) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    // THE BUTTON
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        if (title.isNotEmpty()) {
            PipText(title, themeController, fontSize = 12.sp, modifier = Modifier.alpha(0.8f))
            Spacer(Modifier.height(4.dp))
        }
        PipText(
            text = "[ ${options[selectedIndex]} ]",
            themeController = themeController,
            fontSize = 20.sp,
            modifier = Modifier.clickable { expanded = true }
        )
    }

    // THE PIP-BOY DROPDOWN WHEEL
    if (expanded) {
        Dialog(onDismissRequest = { expanded = false }) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .heightIn(max = 300.dp) // Keeps it from getting too tall
                    .border(2.dp, themeController.activeColor)
                    .background(Color.Black)
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                LazyColumn(horizontalAlignment = Alignment.CenterHorizontally) {
                    items(options.size) { index ->
                        val isSelected = index == selectedIndex
                        PipText(
                            text = options[index],
                            themeController = themeController,
                            fontSize = if (isSelected) 24.sp else 16.sp,
                            textColorOverride = if (isSelected) Color.Black else null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(if (isSelected) themeController.activeColor else Color.Transparent)
                                .alpha(if (isSelected) 1.0f else 0.5f) // Dims unselected items!
                                .clickable {
                                    onOptionSelected(index)
                                    expanded = false
                                }
                                .padding(vertical = 12.dp)
                        )
                    }
                }
            }
        }
    }
}

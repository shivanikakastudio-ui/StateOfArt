package dev.abhinav.myapplication

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Card
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TriStateCheckbox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.tooling.preview.Preview
import dev.abhinav.myapplication.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Quiz(
                        title = "Who wants to be a millionaire?",
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
fun Quiz(title: String, modifier: Modifier = Modifier) {
    LazyColumn(modifier) {
        item { Text(title) }
        items(10) {
            var state by rememberSaveable { mutableStateOf(ToggleableState.Indeterminate) }
            Card {
                Text("How're you feeling?")
                TriStateCheckbox(
                    state,
                    {
                        state =
                            when (state) {
                                ToggleableState.On -> ToggleableState.Off
                                ToggleableState.Off -> ToggleableState.Indeterminate
                                ToggleableState.Indeterminate -> ToggleableState.On
                            }
                    })
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun QuizPreview() {
    MyApplicationTheme {
        Quiz("Android")
    }
}
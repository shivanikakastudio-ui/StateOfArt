package dev.abhinav.myapplication

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
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
        item{Text(title)}
        items(10) {
            Text("Question $it")
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
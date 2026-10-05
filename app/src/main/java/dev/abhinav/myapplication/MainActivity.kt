package dev.abhinav.myapplication

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.abhinav.myapplication.quiz.QuizUiState
import dev.abhinav.myapplication.quiz.QuizViewModel
import dev.abhinav.myapplication.quiz.data.Question
import dev.abhinav.myapplication.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    private val quizViewModel: QuizViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    val state by quizViewModel.state.collectAsState()
                    Quiz(
                        title = "Who wants to be a millionaire?",
                        state = state,
                        contentPadding = innerPadding,
                    )
                }
            }
        }
    }
}

@Composable
fun Quiz(
    title: String,
    state: QuizUiState,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(),
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.semantics { heading() },
                )
                Text(
                    text = "${state.questionCount ?: "…"} questions · API requests: ${state.apiRequests}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        items(state.questions, key = { it.id }) { question ->
            QuestionCard(question, Modifier.padding(horizontal = 16.dp))
        }
    }
}

@Composable
fun QuestionCard(question: Question, modifier: Modifier = Modifier) {
    var selected by rememberSaveable { mutableStateOf<Int?>(null) }
    Card(modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(question.text, style = MaterialTheme.typography.titleMedium)
            Column(Modifier.selectableGroup()) {
                question.options.forEachIndexed { index, option ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = selected == index,
                                onClick = { selected = index },
                                role = Role.RadioButton,
                            )
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = selected == index, onClick = null)
                        Text(option, modifier = Modifier.padding(start = 8.dp))
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun QuizPreview() {
    MyApplicationTheme {
        Quiz(
            title = "Who wants to be a millionaire?",
            state = QuizUiState(
                questions = listOf(
                    Question("q1", "How many bits are in a byte?", listOf("4", "8", "16", "32"), 1),
                    Question("q2", "Which planet has the most moons?", listOf("Earth", "Mars", "Saturn", "Venus"), 2),
                ),
                questionCount = 2,
                apiRequests = 1,
            ),
        )
    }
}

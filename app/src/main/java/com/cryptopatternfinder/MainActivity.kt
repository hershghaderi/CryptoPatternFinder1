package com.cryptopatternfinder

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.cryptopatternfinder.core.Observation
import com.cryptopatternfinder.core.PatternEngine
import com.cryptopatternfinder.data.Store
import com.cryptopatternfinder.ocr.OcrParser
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.time.LocalDateTime

class MainActivity : ComponentActivity() {
    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        setContent {
            App(Store(this))
        }
    }
}

@Composable
fun App(store: Store) {
    var tab by remember { mutableIntStateOf(0) }
    var exchange by remember { mutableStateOf("") }
    var selectedSymbol by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("اسکرین‌شات قیمت را انتخاب کن.") }

    val data = remember { mutableStateListOf<Observation>() }

    fun refresh() {
        data.clear()
        data.addAll(store.all())
    }

    LaunchedEffect(Unit) {
        refresh()
    }

    val symbols = data
        .map { it.symbol }
        .distinct()
        .sorted()

    val picker =
        rememberLauncherForActivityResult(
            ActivityResultContracts.GetContent()
        ) { uri: Uri? ->

            if (uri == null) return@rememberLauncherForActivityResult

            try {
                val image = InputImage.fromFilePath(
                    store.appContext,
                    uri
                )

                TextRecognition
                    .getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
                    .process(image)
                    .addOnSuccessListener { result ->

                        val rows = OcrParser.parse(
                            result.text,
                            exchange.ifBlank { "نامشخص" },
                            LocalDateTime.now()
                        )

                        rows.forEach { observation ->
                            store.insert(observation)
                        }

                        refresh()

                        message =
                            if (rows.isEmpty()) {
                                "ارزی از این اسکرین‌شات شناسایی نشد."
                            } else {
                                "${rows.size} ارز شناسایی و اطلاعات آن‌ها ذخیره شد."
                            }
                    }
                    .addOnFailureListener { error ->
                        message = "خطا در OCR: ${error.message}"
                    }

            } catch (e: Exception) {
                message = "خطا: ${e.message}"
            }
        }

    MaterialTheme {

        Scaffold(
            bottomBar = {

                NavigationBar {

                    val names = listOf(
                        "ثبت",
                        "الگوها",
                        "اخبار",
                        "تأثیر اخبار",
                        "تاریخچه"
                    )

                    names.forEachIndexed { index, name ->

                        NavigationBarItem(
                            selected = tab == index,
                            onClick = { tab = index },
                            icon = {},
                            label = { Text(name) }
                        )
                    }
                }
            }

        ) { padding ->

            Column(
                modifier = Modifier
                    .padding(padding)
                    .padding(16.dp)
                    .fillMaxSize()
            ) {

                Text(
                    "Crypto Pattern Finder",
                    style = MaterialTheme.typography.headlineSmall
                )

                Spacer(Modifier.height(16.dp))

                when (tab) {

                    0 -> RegistrationScreen(
                        exchange = exchange,
                        onExchangeChange = { exchange = it },
                        message = message,
                        onPickImage = {
                            picker.launch("image/*")
                        },
                        totalRecords = data.size
                    )

                    1 -> PatternScreen(data)

                    2 -> NewsScreen(
                        symbols = symbols,
                        selectedSymbol = selectedSymbol,
                        onSymbolSelected = {
                            selectedSymbol = it
                        }
                    )

                    3 -> NewsImpactScreen(
                        data = data,
                        symbols = symbols,
                        selectedSymbol = selectedSymbol,
                        onSymbolSelected = {
                            selectedSymbol = it
                        }
                    )

                    4 -> HistoryScreen(data)
                }
            }
        }
    }
}

@Composable
fun RegistrationScreen(
    exchange: String,
    onExchangeChange: (String) -> Unit,
    message: String,
    onPickImage: () -> Unit,
    totalRecords: Int
) {

    Text(
        "📥 ثبت اطلاعات",
        style = MaterialTheme.typography.titleLarge
    )

    Spacer(Modifier.height(12.dp))

    OutlinedTextField(
        value = exchange,
        onValueChange = onExchangeChange,
        label = { Text("نام صرافی") },
        modifier = Modifier.fillMaxWidth()
    )

    Spacer(Modifier.height(12.dp))

    Button(
        onClick = onPickImage,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text("📸 افزودن اسکرین‌شات")
    }

    Spacer(Modifier.height(12.dp))

    Text(message)

    Spacer(Modifier.height(16.dp))

    Text(
        "تمام اطلاعات استخراج‌شده دائمی ذخیره می‌شوند.",
        style = MaterialTheme.typography.bodyMedium
    )

    Spacer(Modifier.height(6.dp))

    Text(
        "تعداد رکوردهای ذخیره‌شده: $totalRecords"
    )

    Spacer(Modifier.height(12.dp))

    Text(
        "تعداد اسکرین‌شات محدود نیست؛ هر مقدار که برای ثبت وضعیت بازار لازم باشد می‌توانی وارد کنی."
    )
}

@Composable
fun PatternScreen(
    data: List<Observation>
) {

    val patterns = remember(data) {
        PatternEngine.recurringPatterns(data)
    }

    Text(
        "🧠 تحلیل الگوهای نوسان",
        style = MaterialTheme.typography.titleLarge
    )

    Spacer(Modifier.height(12.dp))

    if (patterns.isEmpty()) {

        Text(
            "هنوز داده تاریخی کافی برای کشف الگوی تکرارشونده وجود ندارد."
        )

    } else {

        LazyColumn {

            items(patterns) { pattern ->

                val hour = pattern.startMinute / 60
                val minute = pattern.startMinute % 60

                Text(
                    "${pattern.symbol} | " +
                    "${pattern.weekday} | " +
                    "%02d:%02d | ".format(hour, minute) +
                    "${pattern.dominantDirection} | " +
                    "${pattern.occurrences} بار | " +
                    "%.1f%%".format(pattern.consistencyPercent)
                )

                HorizontalDivider()

                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
fun NewsScreen(
    symbols: List<String>,
    selectedSymbol: String,
    onSymbolSelected: (String) -> Unit
) {

    Text(
        "📰 اخبار ارز",
        style = MaterialTheme.typography.titleLarge
    )

    Spacer(Modifier.height(12.dp))

    if (symbols.isEmpty()) {

        Text(
            "ابتدا حداقل یک ارز از طریق اسکرین‌شات ثبت کن."
        )

    } else {

        Text("ارز مورد نظر را انتخاب کن:")

        Spacer(Modifier.height(8.dp))

        symbols.forEach { symbol ->

            Button(
                onClick = {
                    onSymbolSelected(symbol)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    if (selectedSymbol == symbol) {
                        "✓ $symbol"
                    } else {
                        symbol
                    }
                )
            }

            Spacer(Modifier.height(6.dp))
        }

        if (selectedSymbol.isNotBlank()) {

            Spacer(Modifier.height(12.dp))

            Text(
                "اخبار مرتبط با $selectedSymbol"
            )

            Spacer(Modifier.height(8.dp))

            Text(
                "منابع خبری انتخاب‌شده در این بخش قرار خواهند گرفت."
            )
        }
    }
}

@Composable
fun NewsImpactScreen(
    data: List<Observation>,
    symbols: List<String>,
    selectedSymbol: String,
    onSymbolSelected: (String) -> Unit
) {

    Text(
        "📊 تأثیر اخبار بر نوسانات",
        style = MaterialTheme.typography.titleLarge
    )

    Spacer(Modifier.height(12.dp))

    if (symbols.isEmpty()) {

        Text(
            "برای تحلیل تأثیر اخبار، ابتدا داده قیمت ثبت کن."
        )

        return
    }

    Text("انتخاب ارز:")

    Spacer(Modifier.height(8.dp))

    symbols.forEach { symbol ->

        OutlinedButton(
            onClick = {
                onSymbolSelected(symbol)
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                if (selectedSymbol == symbol) {
                    "✓ $symbol"
                } else {
                    symbol
                }
            )
        }
    }

    Spacer(Modifier.height(12.dp))

    if (selectedSymbol.isBlank()) {

        Text(
            "یک ارز انتخاب کن تا تحلیل قیمت و اخبار آن در کنار هم نمایش داده شود."
        )

    } else {

        val rows = data.filter {
            it.symbol == selectedSymbol
        }

        Text(
            "تحلیل $selectedSymbol"
        )

        Spacer(Modifier.height(8.dp))

        Text(
            "تعداد داده‌های تاریخی: ${rows.size}"
        )

        Spacer(Modifier.height(8.dp))

        if (rows.size < 3) {

            Text(
                "برای تحلیل آماری دقیق‌تر، داده تاریخی بیشتری ثبت کن."
            )

        } else {

            val average =
                rows.map { it.changePercent }.average()

            Text(
                "میانگین تغییر ثبت‌شده: %.2f%%".format(average)
            )

            Spacer(Modifier.height(8.dp))

            Text(
                "در نسخه کامل، رویدادهای خبری مرتبط با $selectedSymbol با این نوسانات زمانی مقایسه می‌شوند."
            )
        }
    }
}

@Composable
fun HistoryScreen(
    data: List<Observation>
) {

    Text(
        "📚 تاریخچه کامل",
        style = MaterialTheme.typography.titleLarge
    )

    Spacer(Modifier.height(8.dp))

    Text(
        "تمام داده‌های قیمت بدون محدودیت زمانی نگهداری می‌شوند."
    )

    Spacer(Modifier.height(12.dp))

    LazyColumn {

        items(data) { observation ->

            Text(
                "${observation.symbol}  " +
                "${observation.changePercent}%  |  " +
                "${observation.exchange}  |  " +
                "${observation.observedAt}"
            )

            HorizontalDivider()

            Spacer(Modifier.height(6.dp))
        }
    }
}

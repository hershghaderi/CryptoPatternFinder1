package com.cryptopatternfinder

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp
import com.cryptopatternfinder.core.Observation
import com.cryptopatternfinder.data.Store
import com.cryptopatternfinder.ocr.OcrParser
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString

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
    var message by remember {
        mutableStateOf("نام صرافی را وارد کن و سپس اسکرین‌شات را اضافه کن.")
    }

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

            if (uri == null) {
                return@rememberLauncherForActivityResult
            }

            try {
                val image = InputImage.fromFilePath(
                    store.appContext,
                    uri
                val clipboardManager = LocalClipboardManager.current
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

                        if (rows.isEmpty()) {
                            message = """
OCR انجام شد.

متن خام تشخیص‌داده‌شده:
${result.text}

تعداد رکوردهای استخراج‌شده: ${rows.size}

صرافی:
${exchange.ifBlank { "نامشخص" }}
""".trimIndent()

                        rows.forEach { observation ->
                            store.insert(observation)
                        }

                        refresh()
                        message = "OCR:\n\n${result.text} متن"

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
                        "تحلیل",
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

                Spacer(
                    modifier = Modifier.height(16.dp)
                )

                when (tab) {

                    0 -> RegistrationScreen(
                        exchange = exchange,
                        onExchangeChange = {
                            exchange = it
                        },
                        message = message,
                        onPickImage = {
                            picker.launch("image/*")
                        },
                        totalRecords = data.size
                    )

                    1 -> AnalysisScreen(
                        data = data,
                        symbols = symbols,
                        selectedSymbol = selectedSymbol,
                        onSymbolSelected = {
                            selectedSymbol = it
                        }
                    )

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
        label = {
            Text("نام صرافی")
        },
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

Spacer(Modifier.height(8.dp))

Button(
    onClick = {
        clipboardManager.setText(
            AnnotatedString(message)
        )
    },
    modifier = Modifier.fillMaxWidth()
) {
    Text("📋 کپی متن OCR")
}

    Spacer(Modifier.height(16.dp))

    Card(
        modifier = Modifier.fillMaxWidth()
    ) {

        Column(
            modifier = Modifier.padding(16.dp)
        ) {

            Text(
                "وضعیت ذخیره‌سازی",
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(Modifier.height(8.dp))

            Text(
                "رکوردهای ذخیره‌شده: $totalRecords"
            )

            Spacer(Modifier.height(6.dp))

            Text(
                "هر اسکرین‌شات با نام صرافی و زمان ثبت می‌شود."
            )
        }
    }

    Spacer(Modifier.height(12.dp))

    Text(
        "برای تشخیص بهتر ارز، نمادهایی مانند BTC، ETH، XRP، SOL و سایر نمادهای معتبر از متن تصویر استخراج می‌شوند."
    )
}

@Composable
fun AnalysisScreen(
    data: List<Observation>,
    symbols: List<String>,
    selectedSymbol: String,
    onSymbolSelected: (String) -> Unit
) {

    Text(
        "📈 تحلیل و چارت",
        style = MaterialTheme.typography.titleLarge
    )

    Spacer(Modifier.height(12.dp))

    if (symbols.isEmpty()) {

        Text(
            "ابتدا چند اسکرین‌شات قیمت ثبت کن."
        )

        return
    }

    Text("انتخاب ارز:")

    Spacer(Modifier.height(8.dp))

    LazyColumn(
        modifier = Modifier.height(180.dp)
    ) {

        items(symbols) { symbol ->

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

            Spacer(Modifier.height(4.dp))
        }
    }

    Spacer(Modifier.height(12.dp))

    if (selectedSymbol.isBlank()) {

        Text(
            "یک ارز انتخاب کن تا اطلاعات، روند و چارت آن نمایش داده شود."
        )

    } else {

        val rows = data
            .filter {
                it.symbol == selectedSymbol
            }
            .sortedBy {
                it.observedAt
            }

        Card(
            modifier = Modifier.fillMaxWidth()
        ) {

            Column(
                modifier = Modifier.padding(16.dp)
            ) {

                Text(
                    selectedSymbol,
                    style = MaterialTheme.typography.headlineSmall
                )

                Spacer(Modifier.height(8.dp))

                Text(
                    "تعداد مشاهدات: ${rows.size}"
                )

                if (rows.isNotEmpty()) {

                    Spacer(Modifier.height(6.dp))

                    val latest = rows.last()

                    Text(
                        "آخرین تغییر: %.2f%%".format(
                            latest.changePercent
                        )
                    )

                    Spacer(Modifier.height(6.dp))

                    Text(
                        "صرافی: ${latest.exchange}"
                    )

                    Spacer(Modifier.height(6.dp))

                    Text(
                        "آخرین ثبت: ${formatDate(latest.observedAt)}"
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        PriceChart(rows)

        Spacer(Modifier.height(12.dp))

        if (rows.size >= 3) {

            val average =
                rows.map {
                    it.changePercent
                }.average()

            Text(
                "میانگین تغییر: %.2f%%".format(average)
            )

            Spacer(Modifier.height(6.dp))

            val positive =
                rows.count {
                    it.changePercent > 0
                }

            val negative =
                rows.count {
                    it.changePercent < 0
                }

            Text(
                "مثبت: $positive  |  منفی: $negative"
            )

        } else {

            Text(
                "برای تحلیل روند، داده تاریخی بیشتری ثبت کن."
            )
        }
    }
}

@Composable
fun PriceChart(
    rows: List<Observation>
) {

    Card(
        modifier = Modifier.fillMaxWidth()
    ) {

        Column(
            modifier = Modifier.padding(16.dp)
        ) {

            Text(
                "📊 نمودار تغییرات",
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(Modifier.height(12.dp))

            if (rows.isEmpty()) {

                Text("داده‌ای برای نمودار وجود ندارد.")

            } else {

                val chartRows = rows
                    .sortedBy { it.observedAt }
                    .takeLast(20)

                val values = chartRows.map {
                    it.changePercent
                }

                val minValue = values.minOrNull() ?: 0.0
                val maxValue = values.maxOrNull() ?: 0.0

                val range = (maxValue - minValue)
                    .coerceAtLeast(1.0)

                val outlineColor =
                    MaterialTheme.colorScheme.outline

                val primaryColor =
                    MaterialTheme.colorScheme.primary

                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                ) {

                    val width = size.width
                    val height = size.height

                    val zeroY =
                        height -
                            (((0.0 - minValue) / range) * height)
                                .toFloat()
                                .coerceIn(0f, height)

                    drawLine(
                        color = outlineColor,
                        start = Offset(0f, zeroY),
                        end = Offset(width, zeroY),
                        strokeWidth = 2f
                    )

                    if (chartRows.size >= 2) {

                        for (i in 0 until chartRows.lastIndex) {

                            val x1 =
                                i.toFloat() /
                                    chartRows.lastIndex
                                        .coerceAtLeast(1) *
                                    width

                            val x2 =
                                (i + 1).toFloat() /
                                    chartRows.lastIndex
                                        .coerceAtLeast(1) *
                                    width

                            val y1 =
                                height -
                                    (((values[i] - minValue) / range) *
                                        height)
                                        .toFloat()

                            val y2 =
                                height -
                                    (((values[i + 1] - minValue) / range) *
                                        height)
                                        .toFloat()

                            drawLine(
                                color = primaryColor,
                                start = Offset(x1, y1),
                                end = Offset(x2, y2),
                                strokeWidth = 5f
                            )
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {

                    Text(
                        "کمینه: %.2f%%".format(minValue)
                    )

                    Text(
                        "بیشینه: %.2f%%".format(maxValue)
                    )
                }

                Spacer(Modifier.height(8.dp))

                val latest = values.last()

                Text(
                    "آخرین مقدار: %.2f%%".format(latest)
                )

                Spacer(Modifier.height(4.dp))

                Text(
                    when {
                        latest > 0 -> "روند فعلی: صعودی ↗"
                        latest < 0 -> "روند فعلی: نزولی ↘"
                        else -> "روند فعلی: خنثی →"
                    }
                )
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
            "ابتدا ارزها را از اسکرین‌شات ثبت کن."
        )

        return
    }

    Text("ارز مورد نظر:")

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

        Spacer(Modifier.height(4.dp))
    }

    if (selectedSymbol.isNotBlank()) {

        Spacer(Modifier.height(12.dp))

        Card(
            modifier = Modifier.fillMaxWidth()
        ) {

            Column(
                modifier = Modifier.padding(16.dp)
            ) {

                Text(
                    "اخبار مرتبط با $selectedSymbol",
                    style = MaterialTheme.typography.titleMedium
                )

                Spacer(Modifier.height(8.dp))

                Text(
                    "برای دریافت اخبار واقعی، این بخش باید به منبع خبری یا API متصل شود."
                )

                Spacer(Modifier.height(8.dp))

                Text(
                    "فعلاً انتخاب ارز فعال است و نماد انتخاب‌شده آماده اتصال به اخبار است."
                )
            }
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
            "یک ارز انتخاب کن."
        )

    } else {

        val rows = data.filter {
            it.symbol == selectedSymbol
        }

        Card(
            modifier = Modifier.fillMaxWidth()
        ) {

            Column(
                modifier = Modifier.padding(16.dp)
            ) {

                Text(
                    "تحلیل $selectedSymbol",
                    style = MaterialTheme.typography.titleMedium
                )

                Spacer(Modifier.height(8.dp))

                Text(
                    "تعداد داده‌ها: ${rows.size}"
                )

                if (rows.isNotEmpty()) {

                    val average =
                        rows.map {
                            it.changePercent
                        }.average()

                    Spacer(Modifier.height(8.dp))

                    Text(
                        "میانگین نوسان: %.2f%%".format(
                            average
                        )
                    )

                    Spacer(Modifier.height(8.dp))

                    Text(
                        if (average > 0) {
                            "روند داده‌های ثبت‌شده بیشتر مثبت است."
                        } else if (average < 0) {
                            "روند داده‌های ثبت‌شده بیشتر منفی است."
                        } else {
                            "روند ثبت‌شده تقریباً خنثی است."
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun HistoryScreen(
    data: List<Observation>
) {

    Text(
        "📚 تاریخچه قیمت",
        style = MaterialTheme.typography.titleLarge
    )

    Spacer(Modifier.height(8.dp))

    Text(
        "داده‌ها بر اساس ارز و زمان ثبت نمایش داده می‌شوند."
    )

    Spacer(Modifier.height(12.dp))

    if (data.isEmpty()) {

        Text(
            "هنوز داده‌ای ثبت نشده است."
        )

        return
    }

    LazyColumn {

        items(
            data.sortedByDescending {
                it.observedAt
            }
        ) { observation ->

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            ) {

                Column(
                    modifier = Modifier.padding(12.dp)
                ) {

                    Text(
                        observation.symbol,
                        style = MaterialTheme.typography.titleMedium
                    )

                    Spacer(Modifier.height(4.dp))

                    Text(
                        "صرافی: ${observation.exchange}"
                    )

                    Text(
                        "تغییر: %.2f%%".format(
                            observation.changePercent
                        )
                    )

                    Text(
                        "تاریخ ثبت: ${formatDate(observation.observedAt)}"
                    )
                }
            }
        }
    }
}

private fun formatDate(
    date: LocalDateTime
): String {

    return date.format(
        DateTimeFormatter.ofPattern(
            "yyyy/MM/dd  HH:mm"
        )
    )
}

package com.cryptopatternfinder

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.unit.dp
import com.cryptopatternfinder.core.Observation
import com.cryptopatternfinder.core.PatternEngine
import com.cryptopatternfinder.data.Store
import com.cryptopatternfinder.ocr.OcrParser
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

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

    var exchange by remember {
        mutableStateOf("")
    }

    var selectedSymbol by remember {
        mutableStateOf("")
    }

    var message by remember {
        mutableStateOf(
            "نام صرافی را وارد کن و سپس هر تعداد اسکرین‌شات لازم داری اضافه کن."
        )
    }

    val data = remember {
        mutableStateListOf<Observation>()
    }

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

    val exchanges = data
        .map { it.exchange }
        .filter { it.isNotBlank() }
        .distinct()
        .sorted()

    val picker =
        rememberLauncherForActivityResult(
            ActivityResultContracts.GetContent()
        ) { uri: Uri? ->

            if (uri == null) {
                return@rememberLauncherForActivityResult
            }

            val selectedExchange =
                exchange.trim().ifBlank {
                    "نامشخص"
                }

            try {

                val image = InputImage.fromFilePath(
                    store.appContext,
                    uri
                )

                message = "در حال خواندن اسکرین‌شات..."

                TextRecognition
                    .getClient(
                        TextRecognizerOptions.DEFAULT_OPTIONS
                    )
                    .process(image)
                    .addOnSuccessListener { result ->

                        val rows =
                            OcrParser.parse(
                                text = result.text,
                                exchange = selectedExchange,
                                seen = LocalDateTime.now()
                            )

                        rows.forEach { observation ->
                            store.insert(observation)
                        }

                        refresh()

                        message =
                            if (rows.isEmpty()) {
                                "ارزی با اطمینان کافی از این تصویر شناسایی نشد. تصویر واضح‌تر انتخاب کن."
                            } else {
                                "${rows.size} ارز از این اسکرین‌شات استخراج و برای صرافی «$selectedExchange» ذخیره شد."
                            }
                    }
                    .addOnFailureListener { error ->

                        message =
                            "خطا در خواندن تصویر: ${error.message}"
                    }

            } catch (error: Exception) {

                message =
                    "خطا در باز کردن تصویر: ${error.message}"
            }
        }

    MaterialTheme {

        Scaffold(

            bottomBar = {

                NavigationBar {

                    val names =
                        listOf(
                            "ثبت",
                            "تحلیل",
                            "اخبار",
                            "تأثیر خبر",
                            "تاریخچه"
                        )

                    names.forEachIndexed { index, name ->

                        NavigationBarItem(
                            selected = tab == index,
                            onClick = {
                                tab = index
                            },
                            icon = {},
                            label = {
                                Text(name)
                            }
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
                    text = "Crypto Pattern Finder",
                    style = MaterialTheme.typography.headlineSmall
                )

                Spacer(
                    Modifier.height(16.dp)
                )

                when (tab) {

                    0 -> RegistrationScreen(
                        exchange = exchange,
                        exchanges = exchanges,
                        onExchangeChange = {
                            exchange = it
                        },
                        message = message,
                        onPickImage = {
                            picker.launch("image/*")
                        },
                        todayCount = store.countToday(),
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

                    4 -> HistoryScreen(
                        data = data
                    )
                }
            }
        }
    }
}

@Composable
fun RegistrationScreen(
    exchange: String,
    exchanges: List<String>,
    onExchangeChange: (String) -> Unit,
    message: String,
    onPickImage: () -> Unit,
    todayCount: Int,
    totalRecords: Int
) {

    Text(
        text = "📥 ثبت اطلاعات بازار",
        style = MaterialTheme.typography.titleLarge
    )

    Spacer(
        Modifier.height(12.dp)
    )

    OutlinedTextField(
        value = exchange,
        onValueChange = onExchangeChange,
        label = {
            Text("نام صرافی")
        },
        placeholder = {
            Text("مثلاً Binance")
        },
        modifier = Modifier.fillMaxWidth()
    )

    Spacer(
        Modifier.height(10.dp)
    )

    if (exchanges.isNotEmpty()) {

        Text(
            "صرافی‌های ثبت‌شده:"
        )

        Spacer(
            Modifier.height(6.dp)
        )

        LazyColumn(
            modifier = Modifier.height(100.dp)
        ) {

            items(exchanges) { item ->

                OutlinedButton(
                    onClick = {
                        onExchangeChange(item)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(item)
                }
            }
        }
    }

    Spacer(
        Modifier.height(10.dp)
    )

    Button(
        onClick = onPickImage,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            "📸 افزودن اسکرین‌شات"
        )
    }

    Spacer(
        Modifier.height(12.dp)
    )

    Card(
        modifier = Modifier.fillMaxWidth()
    ) {

        Column(
            modifier = Modifier.padding(12.dp)
        ) {

            Text(
                "وضعیت ثبت امروز",
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(
                Modifier.height(6.dp)
            )

            Text(
                "تعداد رکورد امروز: $todayCount"
            )

            Text(
                "کل رکوردهای تاریخی: $totalRecords"
            )
        }
    }

    Spacer(
        Modifier.height(12.dp)
    )

    Text(
        message
    )

    Spacer(
        Modifier.height(12.dp)
    )

    Text(
        "محدودیتی برای تعداد اسکرین‌شات وجود ندارد. هر اسکرین‌شات با تاریخ و ساعت خودش پردازش می‌شود و اطلاعات استخراج‌شده برای تحلیل‌های آینده باقی می‌ماند."
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
        text = "📈 تحلیل و نمودار نوسان",
        style = MaterialTheme.typography.titleLarge
    )

    Spacer(
        Modifier.height(10.dp)
    )

    if (symbols.isEmpty()) {

        Text(
            "هنوز داده‌ای برای تحلیل وجود ندارد."
        )

        return
    }

    Text(
        "ارز را انتخاب کن:"
    )

    Spacer(
        Modifier.height(6.dp)
    )

    LazyColumn(
        modifier = Modifier.height(140.dp)
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
        }
    }

    if (selectedSymbol.isBlank()) {

        Spacer(
            Modifier.height(12.dp)
        )

        Text(
            "یک ارز انتخاب کن تا داده‌های تاریخی آن نمایش داده شود."
        )

        return
    }

    val rows =
        data
            .filter {
                it.symbol == selectedSymbol
            }
            .sortedBy {
                it.observedAt
            }

    Spacer(
        Modifier.height(12.dp)
    )

    Text(
        "$selectedSymbol — ${rows.firstOrNull()?.name ?: selectedSymbol}",
        style = MaterialTheme.typography.titleMedium
    )

    Spacer(
        Modifier.height(8.dp)
    )

    if (rows.isEmpty()) {

        Text(
            "برای این ارز داده‌ای ثبت نشده است."
        )

        return
    }

    val average =
        rows
            .map { it.changePercent }
            .average()

    val maximum =
        rows.maxOf {
            it.changePercent
        }

    val minimum =
        rows.minOf {
            it.changePercent
        }

    val positive =
        rows.count {
            it.changePercent > 0
        }

    val negative =
        rows.count {
            it.changePercent < 0
        }

    Card(
        modifier = Modifier.fillMaxWidth()
    ) {

        Column(
            modifier = Modifier.padding(12.dp)
        ) {

            Text(
                "خلاصه آماری",
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(
                Modifier.height(6.dp)
            )

            Text(
                "تعداد مشاهدات: ${rows.size}"
            )

            Text(
                "میانگین تغییر: %.2f%%".format(average)
            )

            Text(
                "بیشترین تغییر: %.2f%%".format(maximum)
            )

            Text(
                "کمترین تغییر: %.2f%%".format(minimum)
            )

            Text(
                "صعودی: $positive | نزولی: $negative"
            )
        }
    }

    Spacer(
        Modifier.height(12.dp)
    )

    Text(
        "📊 نمودار متنی تغییرات"
    )

    Spacer(
        Modifier.height(6.dp)
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp)
    ) {

        items(rows) { row ->

            val sign =
                if (row.changePercent >= 0) {
                    "▲"
                } else {
                    "▼"
                }

            Text(
                "$sign  ${formatDate(row.observedAt)}   %.2f%%"
                    .format(row.changePercent)
            )

            HorizontalDivider()
        }
    }

    Spacer(
        Modifier.height(10.dp)
    )

    val patterns =
        PatternEngine.recurringPatterns(rows)

    if (patterns.isNotEmpty()) {

        Text(
            "🧠 الگوهای تکرارشونده",
            style = MaterialTheme.typography.titleMedium
        )

        Spacer(
            Modifier.height(6.dp)
        )

        patterns.take(5).forEach { pattern ->

            Text(
                "${pattern.weekday} | " +
                    "${pattern.dominantDirection} | " +
                    "${pattern.occurrences} مشاهده | " +
                    "%.1f%% سازگاری"
                        .format(pattern.consistencyPercent)
            )
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
        text = "📰 اخبار بر اساس ارز",
        style =

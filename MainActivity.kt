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

class MainActivity:ComponentActivity() {
    override fun onCreate(state:Bundle?) {
        super.onCreate(state)
        setContent { App(Store(this)) }
    }
}

@Composable
fun App(store:Store) {
    var tab by remember { mutableIntStateOf(0) }
    var exchange by remember { mutableStateOf("صرافی") }
    var message by remember { mutableStateOf("یک اسکرین‌شات وارد کن.") }
    val data=remember { mutableStateListOf<Observation>() }

    fun refresh(){ data.clear(); data.addAll(store.all()) }
    LaunchedEffect(Unit){ refresh() }

    val picker=rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri:Uri? ->
        if(uri==null)return@rememberLauncherForActivityResult
        try {
            val image=InputImage.fromFilePath(store.appContext,uri)
            TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
                .process(image)
                .addOnSuccessListener { result ->
                    val rows=OcrParser.parse(result.text,exchange,LocalDateTime.now())
                    rows.forEach { store.insert(it,uri.toString()) }
                    refresh()
                    message="${rows.size} ارز شناسایی و ذخیره شد."
                }
                .addOnFailureListener { error -> message="خطا در OCR: ${error.message}" }
        } catch(e:Exception){ message="خطا: ${e.message}" }
    }

    MaterialTheme {
        Scaffold(bottomBar={
            NavigationBar {
                listOf("ثبت","الگوها","اخبار","تاریخچه").forEachIndexed { i,n ->
                    NavigationBarItem(i==tab,{tab=i},{},label={Text(n)})
                }
            }
        }) { pad ->
            Column(Modifier.padding(pad).padding(16.dp).fillMaxSize()) {
                Text("Crypto Pattern Finder",style=MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.height(12.dp))
                when(tab) {
                    0 -> {
                        OutlinedTextField(
                            value = exchange,
                            onValueChange = { newValue: String -> exchange = newValue },
                            label = { Text(text = "نام صرافی") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(8.dp))
                        Button({picker.launch("image/*")}){Text("📸 افزودن اسکرین‌شات")}
                        Spacer(Modifier.height(8.dp)); Text(message)
                    }
                    1 -> PatternScreen(data)
                    2 -> {
                        Text("📰 اخبار ارز",style=MaterialTheme.typography.titleLarge)
                        Spacer(Modifier.height(8.dp))
                        Text("با انتخاب یک ارز، این بخش برای نمایش اخبار مرتبط آماده است.")
                    }
                    else -> {
                        Text("📊 تاریخچه: ${data.size} رکورد")
                        LazyColumn {
                            items(data){o -> Text("${o.symbol}  ${o.changePercent}% | ${o.exchange} | ${o.observedAt}"); HorizontalDivider()}
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PatternScreen(data:List<Observation>) {
    val patterns=remember(data.size){PatternEngine.recurringPatterns(data)}
    Text("🧠 کشف الگو",style=MaterialTheme.typography.titleLarge)
    Spacer(Modifier.height(8.dp))
    if(patterns.isEmpty()) Text("برای الگوی آماری حداقل ۳ مشاهده در زمان‌های مشابه لازم است.")
    else LazyColumn {
        items(patterns) { p ->
            val hh=p.startMinute/60; val mm=p.startMinute%60
            Text("${p.symbol} | ${p.weekday} | %02d:%02d | ${p.dominantDirection} | ${p.occurrences} بار | %.1f%%"
                .format(hh,mm,p.consistencyPercent))
            HorizontalDivider()
        }
    }
}

fun App(store: Store) {

    var tab by remember { mutableIntStateOf(0) }
    var exchange by remember { mutableStateOf("") }
    var selectedSymbol by remember { mutableStateOf("") }

    var message by remember {
        mutableStateOf(
            "نام صرافی را وارد کن و سپس اسکرین‌شات را انتخاب کن."
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

    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->

        if (uri == null) return@rememberLauncherForActivityResult

        if (exchange.isBlank()) {
            message = "اول نام صرافی را وارد کن."
            return@rememberLauncherForActivityResult
        }

        try {

            val image = InputImage.fromFilePath(
                store.appContext,
                uri
            )

            TextRecognition
                .getClient(
                    TextRecognizerOptions.DEFAULT_OPTIONS
                )
                .process(image)
                .addOnSuccessListener { result ->

                    val rows = OcrParser.parse(
                        result.text,
                        exchange.trim(),
                        LocalDateTime.now()
                    )

                    rows.forEach { observation ->
                        store.insert(observation)
                    }

                    refresh()

                    message =
                        if (rows.isEmpty()) {
                            "ارزی از تصویر شناسایی نشد."
                        } else {
                            "${rows.size} ارز از صرافی ${exchange.trim()} ذخیره شد."
                        }
                }
                .addOnFailureListener { error ->

                    message =
                        "خطا در OCR: ${error.message ?: "خطای نامشخص"}"
                }

        } catch (e: Exception) {

            message =
                "خطا در باز کردن تصویر: ${e.message ?: "خطای نامشخص"}"
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
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp)
            ) {

                Text(
                    "Crypto Pattern Finder",
                    style = MaterialTheme.typography.headlineSmall
                )

                Spacer(
                    Modifier.height(12.dp)
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

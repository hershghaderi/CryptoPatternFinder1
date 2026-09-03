2s
1s
0s
19s
31s
Run gradle assembleDebug
Starting a Gradle Daemon (subsequent builds will be faster)
> Task :app:preBuild UP-TO-DATE
> Task :app:preDebugBuild UP-TO-DATE
> Task :app:mergeDebugNativeDebugMetadata NO-SOURCE
> Task :app:checkKotlinGradlePluginConfigurationErrors SKIPPED
> Task :app:generateDebugResValues
> Task :app:checkDebugAarMetadata
> Task :app:mapDebugSourceSetPaths
> Task :app:generateDebugResources
> Task :app:packageDebugResources
> Task :app:mergeDebugResources
> Task :app:createDebugCompatibleScreenManifests
> Task :app:extractDeepLinksDebug
> Task :app:parseDebugLocalResources
> Task :app:processDebugMainManifest
> Task :app:processDebugManifest
> Task :app:javaPreCompileDebug
> Task :app:mergeDebugShaders
> Task :app:compileDebugShaders NO-SOURCE
> Task :app:generateDebugAssets UP-TO-DATE
> Task :app:mergeDebugAssets
> Task :app:desugarDebugFileDependencies
> Task :app:compressDebugAssets
> Task :app:processDebugManifestForPackage
> Task :app:checkDebugDuplicateClasses
> Task :app:mergeDebugJniLibFolders
> Task :app:mergeLibDexDebug
> Task :app:mergeDebugNativeLibs
> Task :app:processDebugResources
> Task :app:validateSigningDebug
> Task :app:writeDebugAppMetadata
> Task :app:writeDebugSigningConfigVersions

> Task :app:stripDebugDebugSymbols
Unable to strip the following libraries, packaging them as they are: libandroidx.graphics.path.so, libmlkit_google_ocr_pipeline.so.

> Task :app:mergeExtDexDebug

e: file:///home/runner/work/CryptoPatternFinder1/CryptoPatternFinder1/app/src/main/java/com/cryptopatternfinder/MainActivity.kt:250:26 Unresolved reference 'NewsImpactScreen'.
e: file:///home/runner/work/CryptoPatternFinder1/CryptoPatternFinder1/app/src/main/java/com/cryptopatternfinder/MainActivity.kt:255:46 Unresolved reference 'it'.
e: file:///home/runner/work/CryptoPatternFinder1/CryptoPatternFinder1/app/src/main/java/com/cryptopatternfinder/MainActivity.kt:259:26 Unresolved reference 'HistoryScreen'.
e: file:///home/runner/work/CryptoPatternFinder1/CryptoPatternFinder1/app/src/main/java/com/cryptopatternfinder/MainActivity.kt:587:27 Unresolved reference 'formatDate'.
e: file:///home/runner/work/CryptoPatternFinder1/CryptoPatternFinder1/app/src/main/java/com/cryptopatternfinder/MainActivity.kt:635:16 Syntax error: Expecting an expression.
> Task :app:compileDebugKotlin FAILED
gradle/actions: Writing build results to /home/runner/work/_temp/.gradle-actions/build-results/__run-1788452715335.json

28 actionable tasks: 28 executed
FAILURE: Build failed with an exception.

* What went wrong:
Execution failed for task ':app:compileDebugKotlin'.
> A failure occurred while executing org.jetbrains.kotlin.compilerRunner.GradleCompilerRunnerWithWorkers$GradleKotlinCompilerWorkAction
   > Compilation error. See log for more details

* Try:
> Run with --stacktrace option to get the stack trace.
> Run with --info or --debug option to get more log output.
> Run with --scan to get full insights.
> Get more help at https://help.gradle.org.

BUILD FAILED in 30s
Error: Process completed with exit code 1.

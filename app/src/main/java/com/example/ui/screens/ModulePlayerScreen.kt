package com.example.ui.screens

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.widget.MediaController
import android.widget.VideoView
import java.util.Locale
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.entity.ModuleEntity
import com.example.ui.theme.*
import com.example.ui.viewmodel.MainViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.ui.draw.alpha
import kotlin.math.sin

fun extractYouTubeVideoId(url: String): String? {
    if (url.isBlank()) return null
    val pattern = "(?<=watch\\?v=|/videos/|embed\\/|youtu.be\\/|\\/v\\/|\\/e\\/|watch\\?node_id=|&v=)[^#&?]*".toRegex()
    val match = pattern.find(url)
    return match?.value?.takeIf { it.length == 11 }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModulePlayerScreen(
    viewModel: MainViewModel,
    moduleId: Int,
    onBack: () -> Unit,
    onStartQuiz: (Int) -> Unit
) {
    val context = LocalContext.current
    val moduleState by viewModel.repository.getModuleById(moduleId)
        .collectAsStateWithLifecycle(initialValue = null)

    var isPlaying by remember { mutableStateOf(true) }
    var currentProgressSeconds by remember { mutableFloatStateOf(0f) }
    var speedMultiplier by remember { mutableFloatStateOf(1.0f) }
    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Transcript, 1 = Key Takeaways
    var playerEngine by remember { mutableStateOf("NATIVE_VIDEOVIEW") } // "NATIVE_VIDEOVIEW", "WEBVIEW", "VISUALIZER"
    var overrideVideoUrl by remember { mutableStateOf<String?>(null) }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var speechWebViewRef by remember { mutableStateOf<WebView?>(null) }
    var videoViewRef by remember { mutableStateOf<VideoView?>(null) }
    var isMuted by remember { mutableStateOf(false) }
    var ttsEngine by remember { mutableStateOf<TextToSpeech?>(null) }
    var isTtsSpeaking by remember { mutableStateOf(false) }
    var isTtsReady by remember { mutableStateOf(false) }
    var ttsSpeechRate by remember { mutableFloatStateOf(1.0f) }
    var pcmSpeechJob by remember { mutableStateOf<Job?>(null) }
    val coroutineScope = rememberCoroutineScope()

    val module = moduleState

    // Initialize TextToSpeech Engine for reliable voice narration
    DisposableEffect(context) {
        var tts: TextToSpeech? = null
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val res = tts?.setLanguage(Locale.US)
                if (res == TextToSpeech.LANG_MISSING_DATA || res == TextToSpeech.LANG_NOT_SUPPORTED) {
                    tts?.setLanguage(Locale.getDefault())
                }
                isTtsReady = true
            }
        }
        tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {}
            override fun onDone(utteranceId: String?) {}
            override fun onError(utteranceId: String?) {}
        })
        ttsEngine = tts

        onDispose {
            tts.stop()
            tts.shutdown()
        }
    }

    fun playPcmStartChime() {
        try {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
            if (maxVol > 0) {
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, maxVol, 0)
            }

            val sampleRate = 22050
            val numSamples = sampleRate / 4 // 0.25s chime
            val samples = ShortArray(numSamples)
            for (i in 0 until numSamples) {
                val angle = 2.0 * Math.PI * i / (sampleRate / 659.25) // E5 chime
                samples[i] = (Math.sin(angle) * 20000).toInt().toShort()
            }
            val audioTrack = AudioTrack(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build(),
                AudioFormat.Builder()
                    .setSampleRate(sampleRate)
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build(),
                numSamples * 2,
                AudioTrack.MODE_STATIC,
                AudioManager.AUDIO_SESSION_ID_GENERATE
            )
            audioTrack.write(samples, 0, numSamples)
            audioTrack.play()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun speakLessonVoice(text: String) {
        try {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
            if (maxVol > 0) {
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, maxVol, 0)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        if (isTtsSpeaking) {
            pcmSpeechJob?.cancel()
            pcmSpeechJob = null
            ttsEngine?.stop()
            speechWebViewRef?.evaluateJavascript("stopWebText();", null)
            webViewRef?.evaluateJavascript("stopWebText();", null)
            isTtsSpeaking = false
        } else {
            playPcmStartChime()
            isTtsSpeaking = true

            val quotedText = org.json.JSONObject.quote(text)
            speechWebViewRef?.evaluateJavascript("playToneSound(); speakWebText($quotedText, $ttsSpeechRate);", null)
            webViewRef?.evaluateJavascript("playToneSound(); speakWebText($quotedText, $ttsSpeechRate);", null)

            if (ttsEngine != null) {
                ttsEngine?.setSpeechRate(ttsSpeechRate)
                val params = Bundle().apply {
                    putInt(TextToSpeech.Engine.KEY_PARAM_STREAM, AudioManager.STREAM_MUSIC)
                    putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, 1.0f)
                }
                ttsEngine?.speak(text, TextToSpeech.QUEUE_FLUSH, params, "LessonVoice_${System.currentTimeMillis()}")
            }

            // High-fidelity PCM Speech Stream synthesis
            pcmSpeechJob?.cancel()
            pcmSpeechJob = coroutineScope.launch(Dispatchers.Default) {
                var track: AudioTrack? = null
                try {
                    val words = text.split(Regex("\\s+")).filter { it.isNotBlank() }
                    val sampleRate = 22050
                    val minBuf = AudioTrack.getMinBufferSize(
                        sampleRate,
                        AudioFormat.CHANNEL_OUT_MONO,
                        AudioFormat.ENCODING_PCM_16BIT
                    )
                    val audioTrack = AudioTrack(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                            .build(),
                        AudioFormat.Builder()
                            .setSampleRate(sampleRate)
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build(),
                        minBuf.coerceAtLeast(8192),
                        AudioTrack.MODE_STREAM,
                        AudioManager.AUDIO_SESSION_ID_GENERATE
                    )
                    track = audioTrack
                    audioTrack.play()

                    val basePitch = 165.0

                    for (i in words.indices) {
                        if (!isActive) break
                        val word = words[i]
                        val clean = word.lowercase().replace(Regex("[^a-z0-9]"), "")
                        if (clean.isBlank()) continue

                        val durationMs = ((85 + clean.length * 30) / ttsSpeechRate).toLong().coerceIn(110, 480)
                        val numSamples = (sampleRate * durationMs / 1000).toInt()
                        if (numSamples <= 0) continue

                        val samples = ShortArray(numSamples)
                        val isEnd = word.contains(".") || word.contains("!") || word.contains("?")
                        val pitch = basePitch + (if (isEnd) -15.0 else (i % 4) * 8.0 - 12.0)

                        for (s in 0 until numSamples) {
                            val t = s.toDouble() / sampleRate
                            val env = Math.sin(Math.PI * s / numSamples)
                            val f0 = pitch
                            val f1 = pitch * 2.1
                            val f2 = pitch * 3.1
                            val wave = Math.sin(2.0 * Math.PI * f0 * t) * 0.5 +
                                       Math.sin(2.0 * Math.PI * f1 * t) * 0.3 +
                                       Math.sin(2.0 * Math.PI * f2 * t) * 0.2
                            samples[s] = (wave * 22000 * env).toInt().coerceIn(-32000, 32000).toShort()
                        }
                        audioTrack.write(samples, 0, numSamples)

                        val pauseMs = if (isEnd) (250 / ttsSpeechRate).toLong() else (40 / ttsSpeechRate).toLong()
                        val pauseSamples = (sampleRate * pauseMs / 1000).toInt()
                        if (pauseSamples > 0) {
                            val silence = ShortArray(pauseSamples)
                            audioTrack.write(silence, 0, pauseSamples)
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    try {
                        track?.stop()
                        track?.release()
                    } catch (_: Exception) {}
                    withContext(Dispatchers.Main) {
                        isTtsSpeaking = false
                    }
                }
            }
        }
    }

    // Helper to trigger simultaneous video audio and voice narration
    fun playVideoAndVoiceTogether(fullLessonText: String) {
        isMuted = false
        isPlaying = true
        try {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, maxVol, 0)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        if (playerEngine == "WEBVIEW") {
            webViewRef?.evaluateJavascript("if (typeof enableAudio === 'function') enableAudio(); var v=document.getElementById('vid'); if(v){ v.muted=false; v.volume=1.0; v.play(); }", null)
        } else if (playerEngine == "NATIVE_VIDEOVIEW") {
            videoViewRef?.start()
        }

        speakLessonVoice(fullLessonText)
    }

    // Ensure audio focus and media stream volume are configured for video playback
    DisposableEffect(Unit) {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val currentVol = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        if (currentVol == 0 || currentVol < (maxVol * 0.5f)) {
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, (maxVol * 0.85f).toInt(), 0)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MOVIE)
                        .build()
                )
                .build()
            audioManager.requestAudioFocus(focusRequest)
            onDispose {
                audioManager.abandonAudioFocusRequest(focusRequest)
            }
        } else {
            @Suppress("DEPRECATION")
            audioManager.requestAudioFocus(null, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN)
            onDispose {
                @Suppress("DEPRECATION")
                audioManager.abandonAudioFocus(null)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(module?.title ?: "Module Lesson", style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("module_player_back_button")) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundWhite)
            )
        },
        bottomBar = {
            Surface(
                color = SurfaceWhite,
                shadowElevation = 0.dp,
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderLight)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = {
                            if (module != null) {
                                viewModel.markModuleCompleted(module.courseId, module.id)
                            }
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = PassGreen),
                        border = androidx.compose.foundation.BorderStroke(1.dp, PassGreen),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("mark_module_complete_button")
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Mark Watched", style = MaterialTheme.typography.labelMedium)
                    }

                    Button(
                        onClick = { onStartQuiz(moduleId) },
                        colors = ButtonDefaults.buttonColors(containerColor = ExecutiveBlue),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("start_quiz_button")
                    ) {
                        Text("Take Module Quiz", style = MaterialTheme.typography.labelLarge)
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(Icons.Default.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                    }
                }
            }
        },
        containerColor = BackgroundWhite
    ) { innerPadding ->
        if (module == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = ExecutiveBlue)
            }
        } else {
            val activeVideoUrl = overrideVideoUrl ?: if (module.videoUrl.isNotBlank()) module.videoUrl else "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4"
            val totalDuration = module.videoDurationSeconds.toFloat().coerceAtLeast(60f)
            val fullLessonSpeech = remember(module) {
                "${module.title}. ${module.summary}. Key Takeaways: ${module.keyTakeaways}. Lesson Transcript: ${module.transcript.ifBlank { module.summary }}"
            }

            // Timeline progression timer
            LaunchedEffect(isPlaying, speedMultiplier) {
                while (isPlaying) {
                    delay((1000 / speedMultiplier).toLong())
                    if (currentProgressSeconds < totalDuration) {
                        currentProgressSeconds += 1f
                    } else {
                        isPlaying = false
                        viewModel.markModuleCompleted(module.courseId, module.id)
                    }
                }
            }

            // Optional manual voice narration when requested by user

            // Hidden background speech WebView for guaranteed WebSpeech API execution across all player modes
            Box(modifier = Modifier.size(1.dp).alpha(0.01f)) {
                AndroidView(
                    factory = { ctx ->
                        WebView(ctx).apply {
                            settings.apply {
                                javaScriptEnabled = true
                                domStorageEnabled = true
                                mediaPlaybackRequiresUserGesture = false
                            }
                            webChromeClient = WebChromeClient()
                            webViewClient = WebViewClient()
                            val html = """
                                <!DOCTYPE html>
                                <html>
                                <head>
                                    <script>
                                        function speakWebText(text, rate) {
                                            try {
                                                if ('speechSynthesis' in window) {
                                                    window.speechSynthesis.cancel();
                                                    var u = new SpeechSynthesisUtterance(text);
                                                    u.rate = rate || 1.0;
                                                    u.pitch = 1.0;
                                                    u.volume = 1.0;
                                                    u.lang = 'en-US';
                                                    window.speechSynthesis.speak(u);
                                                    return true;
                                                }
                                            } catch(e){}
                                            return false;
                                        }
                                        function stopWebText() {
                                            try {
                                                if ('speechSynthesis' in window) {
                                                    window.speechSynthesis.cancel();
                                                }
                                            } catch(e){}
                                        }
                                        function playToneSound() {
                                            try {
                                                var AC = window.AudioContext || window.webkitAudioContext;
                                                if (AC) {
                                                    var ctx = new AC();
                                                    var osc = ctx.createOscillator();
                                                    var gain = ctx.createGain();
                                                    osc.type = 'sine';
                                                    osc.frequency.setValueAtTime(587.33, ctx.currentTime);
                                                    gain.gain.setValueAtTime(0.3, ctx.currentTime);
                                                    osc.connect(gain);
                                                    gain.connect(ctx.destination);
                                                    osc.start();
                                                    osc.stop(ctx.currentTime + 0.25);
                                                }
                                            } catch(e){}
                                        }
                                    </script>
                                </head>
                                <body></body>
                                </html>
                            """.trimIndent()
                            loadDataWithBaseURL("https://localhost", html, "text/html", "UTF-8", null)
                            speechWebViewRef = this
                        }
                    }
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                // Player Engine Selector Bar
                Surface(
                    color = Color(0xFF0F172A),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FilterChip(
                            selected = playerEngine == "WEBVIEW",
                            onClick = { playerEngine = "WEBVIEW" },
                            label = { Text("🌐 Web Player", style = MaterialTheme.typography.labelSmall) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = ExecutiveBlue,
                                selectedLabelColor = Color.White,
                                containerColor = Color.White.copy(alpha = 0.15f),
                                labelColor = Color.LightGray
                            )
                        )
                        FilterChip(
                            selected = playerEngine == "NATIVE_VIDEOVIEW",
                            onClick = { playerEngine = "NATIVE_VIDEOVIEW" },
                            label = { Text("🎥 Native Player", style = MaterialTheme.typography.labelSmall) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = ExecutiveBlue,
                                selectedLabelColor = Color.White,
                                containerColor = Color.White.copy(alpha = 0.15f),
                                labelColor = Color.LightGray
                            )
                        )
                        FilterChip(
                            selected = playerEngine == "VISUALIZER",
                            onClick = { playerEngine = "VISUALIZER" },
                            label = { Text("🎨 Visualizer", style = MaterialTheme.typography.labelSmall) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = ExecutiveBlue,
                                selectedLabelColor = Color.White,
                                containerColor = Color.White.copy(alpha = 0.15f),
                                labelColor = Color.LightGray
                            )
                        )
                        FilterChip(
                            selected = playerEngine == "TTS_VOICE",
                            onClick = {
                                playerEngine = "TTS_VOICE"
                                if (!isTtsSpeaking) {
                                    speakLessonVoice(fullLessonSpeech)
                                }
                            },
                            label = { Text("🗣️ Voice", style = MaterialTheme.typography.labelSmall) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = ExecutiveBlue,
                                selectedLabelColor = Color.White,
                                containerColor = Color.White.copy(alpha = 0.15f),
                                labelColor = Color.LightGray
                            )
                        )
                    }
                }

                // Video Player Container Frame (250dp height)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(250.dp)
                        .background(Color(0xFF0F172A)),
                    contentAlignment = Alignment.Center
                ) {
                    when (playerEngine) {
                        "TTS_VOICE" -> {
                            // Native High-Definition Android Text-To-Speech Narration Engine
                            val fullLessonSpeech = remember(module) {
                                "${module.title}. ${module.summary}. Key Takeaways: ${module.keyTakeaways}. Lesson Transcript: ${module.transcript.ifBlank { module.summary }}"
                            }

                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.verticalGradient(
                                            colors = listOf(Color(0xFF0F172A), Color(0xFF1E293B))
                                        )
                                    )
                                    .padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = ExecutiveBlue.copy(alpha = 0.25f),
                                    border = androidx.compose.foundation.BorderStroke(2.dp, ExecutiveBlue),
                                    modifier = Modifier.size(56.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = if (isTtsSpeaking) Icons.Default.VolumeUp else Icons.Default.RecordVoiceOver,
                                            contentDescription = "Voice Narration",
                                            tint = Color.White,
                                            modifier = Modifier.size(28.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Text(
                                    text = if (isTtsSpeaking) "🔊 Reading Lesson Aloud..." else "🗣️ Voice Narration Mode",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )

                                Text(
                                    text = module.title,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.LightGray,
                                    maxLines = 1
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(
                                        onClick = {
                                            speakLessonVoice(fullLessonSpeech)
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (isTtsSpeaking) Color.Red else ExecutiveBlue
                                        ),
                                        shape = RoundedCornerShape(20.dp),
                                        modifier = Modifier.testTag("tts_toggle_speech_button")
                                    ) {
                                        Icon(
                                            imageVector = if (isTtsSpeaking) Icons.Default.Stop else Icons.Default.PlayArrow,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(if (isTtsSpeaking) "Stop Voice" else "▶️ Speak Aloud", style = MaterialTheme.typography.labelMedium)
                                    }

                                    AssistChip(
                                        onClick = {
                                            ttsSpeechRate = when (ttsSpeechRate) {
                                                1.0f -> 1.25f
                                                1.25f -> 1.5f
                                                1.5f -> 0.8f
                                                else -> 1.0f
                                            }
                                            if (isTtsSpeaking) {
                                                speakLessonVoice(fullLessonSpeech)
                                                speakLessonVoice(fullLessonSpeech)
                                            }
                                        },
                                        label = { Text("Speed: ${ttsSpeechRate}x", color = Color.White) },
                                        colors = AssistChipDefaults.assistChipColors(containerColor = Color.White.copy(alpha = 0.15f))
                                    )
                                }
                            }
                        }

                        "WEBVIEW" -> {
                            // HTML5 / YouTube Embedded Video Player with auto audio enable on tap
                            val youtubeId = extractYouTubeVideoId(activeVideoUrl)
                            AndroidView(
                                factory = { ctx ->
                                    WebView(ctx).apply {
                                        setLayerType(android.view.View.LAYER_TYPE_SOFTWARE, null)
                                        settings.apply {
                                            javaScriptEnabled = true
                                            domStorageEnabled = true
                                            mediaPlaybackRequiresUserGesture = false
                                            useWideViewPort = true
                                            loadWithOverviewMode = true
                                            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                                            allowFileAccess = true
                                            allowContentAccess = true
                                            databaseEnabled = true
                                        }
                                        webChromeClient = WebChromeClient()
                                        webViewClient = WebViewClient()

                                        val htmlData = if (youtubeId != null) {
                                            """
                                                <!DOCTYPE html>
                                                <html>
                                                <head>
                                                    <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
                                                    <style>
                                                        * { margin:0; padding:0; box-sizing:border-box; }
                                                        body { background:#000; display:flex; justify-content:center; align-items:center; height:100vh; width:100vw; overflow:hidden; }
                                                        iframe { width:100%; height:100%; border:0; }
                                                        .unmute-btn { position:absolute; top:12px; left:50%; transform:translateX(-50%); z-index:100; background:#0052cc; color:#fff; padding:8px 16px; border-radius:20px; font-weight:bold; font-size:12px; cursor:pointer; border:1px solid #60a5fa; box-shadow:0 4px 12px rgba(0,0,0,0.5); }
                                                    </style>
                                                </head>
                                                <body>
                                                    <div id="unmuteBtn" class="unmute-btn" onclick="unmuteYt()">🔊 Tap Player for Full Sound</div>
                                                    <iframe id="yt" src="https://www.youtube-nocookie.com/embed/$youtubeId?autoplay=1&mute=0&controls=1&enablejsapi=1&origin=https://www.youtube.com" allow="autoplay; encrypted-media; picture-in-picture; accelerometer; gyroscope" allowfullscreen></iframe>
                                                    <script>
                                                        function unmuteYt() {
                                                            var iframe = document.getElementById('yt');
                                                            if (iframe) {
                                                                iframe.contentWindow.postMessage('{"event":"command","func":"unMute","args":""}', '*');
                                                                iframe.contentWindow.postMessage('{"event":"command","func":"playVideo","args":""}', '*');
                                                            }
                                                            var btn = document.getElementById('unmuteBtn');
                                                            if (btn) btn.style.display = 'none';
                                                        }
                                                        function speakWebText(text, rate) {
                                                            try {
                                                                if ('speechSynthesis' in window) {
                                                                    window.speechSynthesis.cancel();
                                                                    var u = new SpeechSynthesisUtterance(text);
                                                                    u.rate = rate || 1.0;
                                                                    u.pitch = 1.0;
                                                                    u.volume = 1.0;
                                                                    u.lang = 'en-US';
                                                                    window.speechSynthesis.speak(u);
                                                                    return true;
                                                                }
                                                            } catch(e){}
                                                            return false;
                                                        }
                                                        function stopWebText() {
                                                            try { if ('speechSynthesis' in window) window.speechSynthesis.cancel(); } catch(e){}
                                                        }
                                                        document.addEventListener('click', unmuteYt);
                                                        document.addEventListener('touchstart', unmuteYt);
                                                    </script>
                                                </body>
                                                </html>
                                            """.trimIndent()
                                        } else {
                                            """
                                                <!DOCTYPE html>
                                                <html>
                                                <head>
                                                    <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
                                                    <style>
                                                        * { margin:0; padding:0; box-sizing:border-box; }
                                                        body { background:#0f172a; display:flex; flex-direction:column; justify-content:center; align-items:center; height:100vh; width:100vw; overflow:hidden; font-family:-apple-system,BlinkMacSystemFont,"Segoe UI",Roboto,sans-serif; color:#fff; }
                                                        video { width:100%; height:100%; object-fit:contain; background:#000; }
                                                        .audio-banner { position:absolute; top:12px; left:50%; transform:translateX(-50%); z-index:100; background:#0052cc; color:#ffffff; padding:8px 16px; border-radius:20px; font-weight:bold; font-size:12px; cursor:pointer; box-shadow:0 4px 12px rgba(0,0,0,0.5); display:flex; align-items:center; gap:6px; border:1px solid #60a5fa; }
                                                    </style>
                                                </head>
                                                <body>
                                                    <div id="audioBanner" class="audio-banner" onclick="enableAudio()">🔊 Tap Player for Full Sound</div>
                                                    <video id="vid" controls autoplay playsinline loop preload="auto">
                                                        <source src="$activeVideoUrl" type="video/mp4">
                                                        <source src="https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4" type="video/mp4">
                                                        <source src="https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4" type="video/mp4">
                                                        Your browser does not support HTML5 video playback.
                                                    </video>
                                                    <script>
                                                        var v = document.getElementById('vid');
                                                        var banner = document.getElementById('audioBanner');
                                                        function enableAudio() {
                                                            if (v) {
                                                                v.muted = false;
                                                                v.volume = 1.0;
                                                                var p = v.play();
                                                                if (p && p.catch) {
                                                                    p.catch(function(e) {
                                                                        console.log(e);
                                                                    });
                                                                }
                                                            }
                                                            if (banner) banner.style.display = 'none';
                                                        }
                                                        function speakWebText(text, rate) {
                                                            try {
                                                                if ('speechSynthesis' in window) {
                                                                    window.speechSynthesis.cancel();
                                                                    var u = new SpeechSynthesisUtterance(text);
                                                                    u.rate = rate || 1.0;
                                                                    u.pitch = 1.0;
                                                                    u.volume = 1.0;
                                                                    u.lang = 'en-US';
                                                                    window.speechSynthesis.speak(u);
                                                                    return true;
                                                                }
                                                            } catch(e){}
                                                            return false;
                                                        }
                                                        function stopWebText() {
                                                            try { if ('speechSynthesis' in window) window.speechSynthesis.cancel(); } catch(e){}
                                                        }
                                                        document.addEventListener('click', enableAudio);
                                                        document.addEventListener('touchstart', enableAudio);
                                                        window.onload = function() {
                                                            if (v) {
                                                                v.muted = false;
                                                                v.volume = 1.0;
                                                                var promise = v.play();
                                                                if (promise !== undefined && promise.catch) {
                                                                    promise.catch(function(error) {
                                                                        console.log('Autoplay deferred until touch', error);
                                                                        if (banner) banner.style.display = 'flex';
                                                                    });
                                                                }
                                                            }
                                                        };
                                                    </script>
                                                </body>
                                                </html>
                                            """.trimIndent()
                                        }

                                        loadDataWithBaseURL("https://commondatastorage.googleapis.com", htmlData, "text/html", "UTF-8", null)
                                        webViewRef = this
                                    }
                                },
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        "NATIVE_VIDEOVIEW" -> {
                            // Native Android VideoView Player with full Audio Focus and MediaController
                            AndroidView(
                                factory = { ctx ->
                                    VideoView(ctx).apply {
                                        layoutParams = FrameLayout.LayoutParams(
                                            FrameLayout.LayoutParams.MATCH_PARENT,
                                            FrameLayout.LayoutParams.MATCH_PARENT
                                        )
                                        val mc = MediaController(ctx)
                                        mc.setAnchorView(this)
                                        setMediaController(mc)
                                        
                                        setOnErrorListener { _, _, _ ->
                                            try {
                                                val fallbackUri = Uri.parse("https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4")
                                                setVideoURI(fallbackUri)
                                                start()
                                            } catch (e: Exception) {
                                                e.printStackTrace()
                                            }
                                            true
                                        }

                                        val isWebUrl = activeVideoUrl.contains("youtube.com") || activeVideoUrl.contains("youtu.be")
                                        val cleanUri = Uri.parse(
                                            if (activeVideoUrl.endsWith(".mp4") || activeVideoUrl.endsWith(".m3u8") || (!isWebUrl && activeVideoUrl.startsWith("http"))) {
                                                activeVideoUrl
                                            } else {
                                                "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4"
                                            }
                                        )
                                        setVideoURI(cleanUri)
                                        
                                        setOnPreparedListener { mp ->
                                            mp.isLooping = true
                                            mp.setVolume(if (isMuted) 0f else 1.0f, if (isMuted) 0f else 1.0f)
                                            start()
                                            isPlaying = true
                                        }
                                        
                                        videoViewRef = this
                                    }
                                },
                                update = { vv ->
                                    if (isPlaying) {
                                        if (!vv.isPlaying) vv.start()
                                    } else {
                                        if (vv.isPlaying) vv.pause()
                                    }
                                },
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        else -> {
                            // Interactive Lesson Visualizer Player with background audio
                            AnimatedLessonPlayer(
                                module = module,
                                videoUrl = activeVideoUrl,
                                isPlaying = isPlaying,
                                isMuted = isMuted,
                                progressSeconds = currentProgressSeconds,
                                totalDurationSeconds = totalDuration
                            )
                        }
                    }

                    // Floating Top Control Bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.TopCenter)
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(Color.Black.copy(alpha = 0.85f), Color.Transparent)
                                )
                            )
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Video Player Badge & Voice Narration Button
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                color = Color.Black.copy(alpha = 0.65f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Icon(Icons.Default.PlayCircle, contentDescription = null, tint = PassGreen, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("HD Video Player", style = MaterialTheme.typography.labelSmall, color = Color.White)
                                }
                            }

                            Surface(
                                color = if (isTtsSpeaking) ExecutiveBlue else Color.Black.copy(alpha = 0.65f),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.clickable {
                                    speakLessonVoice(fullLessonSpeech)
                                }
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isTtsSpeaking) Icons.Default.VolumeUp else Icons.Default.RecordVoiceOver,
                                        contentDescription = "Voice Narration",
                                        tint = Color.White,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (isTtsSpeaking) "Voice Speaking..." else "🗣️ Lesson Voice",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White
                                    )
                                }
                            }
                        }

                        // Mute & Speed
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                color = if (isMuted) Color.Red.copy(alpha = 0.8f) else Color.Black.copy(alpha = 0.6f),
                                shape = CircleShape,
                                modifier = Modifier.clickable {
                                    isMuted = !isMuted
                                    if (playerEngine == "WEBVIEW") {
                                        webViewRef?.evaluateJavascript("var v=document.getElementById('vid'); if(v){ v.muted=${isMuted}; v.volume=${if (isMuted) 0 else 1}; }", null)
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = if (isMuted) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                                    contentDescription = "Audio Toggle",
                                    tint = Color.White,
                                    modifier = Modifier.padding(6.dp).size(16.dp)
                                )
                            }

                            Surface(
                                color = Color.Black.copy(alpha = 0.6f),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.clickable {
                                    speedMultiplier = when (speedMultiplier) {
                                        1.0f -> 1.25f
                                        1.25f -> 1.5f
                                        1.5f -> 2.0f
                                        else -> 1.0f
                                    }
                                }
                            ) {
                                Text(
                                    text = "${speedMultiplier}x",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    // Floating Bottom Controls for Visualizer mode
                    if (playerEngine == "VISUALIZER") {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.BottomCenter)
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f))
                                    )
                                )
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = { isPlaying = !isPlaying },
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(ExecutiveBlue)
                                        .testTag("play_pause_video_button")
                                ) {
                                    Icon(
                                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                        contentDescription = if (isPlaying) "Pause" else "Play",
                                        tint = Color.White,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }

                            Slider(
                                value = currentProgressSeconds.coerceIn(0f, totalDuration),
                                onValueChange = { currentProgressSeconds = it },
                                valueRange = 0f..totalDuration,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(20.dp),
                                colors = SliderDefaults.colors(
                                    thumbColor = ExecutiveBlue,
                                    activeTrackColor = ExecutiveBlue,
                                    inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                                )
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                val curMin = (currentProgressSeconds / 60).toInt()
                                val curSec = (currentProgressSeconds % 60).toInt()
                                val totMin = (totalDuration / 60).toInt()
                                val totSec = (totalDuration % 60).toInt()

                                Text(
                                    text = String.format("%02d:%02d / %02d:%02d", curMin, curSec, totMin, totSec),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White.copy(alpha = 0.9f)
                                )

                                Text(
                                    text = if (isPlaying) "● STREAMING" else "PAUSED",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isPlaying) PassGreen else Color.LightGray
                                )
                            }
                        }
                    }
                }

                // Dual Stream Audio Controller & Quick Video Switcher Bar
                Surface(
                    color = Color(0xFF1E293B),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                        // Master Dual Play Button
                        Button(
                            onClick = {
                                playVideoAndVoiceTogether(fullLessonSpeech)
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ExecutiveBlue
                            ),
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(42.dp)
                                .testTag("play_video_and_voice_together_button")
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(Icons.Default.RecordVoiceOver, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "🎬 + 🗣️ Play Video & Voice Together",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.VolumeUp, contentDescription = null, tint = ExecutiveBlueSoft, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Individual Stream Controls:", style = MaterialTheme.typography.labelMedium, color = Color.White)
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Button(
                                    onClick = {
                                        speakLessonVoice(fullLessonSpeech)
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isTtsSpeaking) Color.Red else Color.White.copy(alpha = 0.2f)
                                    ),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    modifier = Modifier.height(28.dp).testTag("read_lesson_voice_button")
                                ) {
                                    Icon(
                                        imageVector = if (isTtsSpeaking) Icons.Default.Stop else Icons.Default.Mic,
                                        contentDescription = null,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(if (isTtsSpeaking) "Stop Voice" else "🎙️ Voice Only", style = MaterialTheme.typography.labelSmall)
                                }

                                Button(
                                    onClick = {
                                        isMuted = false
                                        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
                                        val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                                        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, maxVol, 0)

                                        if (playerEngine == "WEBVIEW") {
                                            webViewRef?.evaluateJavascript("if (typeof enableAudio === 'function') enableAudio(); var v=document.getElementById('vid'); if(v){ v.muted=false; v.volume=1.0; v.play(); }", null)
                                        } else if (playerEngine == "NATIVE_VIDEOVIEW") {
                                            videoViewRef?.start()
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.2f)),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    modifier = Modifier.height(28.dp).testTag("enable_sound_button")
                                ) {
                                    Icon(Icons.Default.VolumeUp, contentDescription = null, modifier = Modifier.size(12.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Video Sound Only", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            AssistChip(
                                onClick = {
                                    overrideVideoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4"
                                },
                                label = { Text("BigBuckBunny MP4", style = MaterialTheme.typography.labelSmall) },
                                leadingIcon = { Icon(Icons.Default.PlayCircle, contentDescription = null, modifier = Modifier.size(14.dp)) }
                            )
                            AssistChip(
                                onClick = {
                                    overrideVideoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4"
                                },
                                label = { Text("ElephantsDream MP4", style = MaterialTheme.typography.labelSmall) },
                                leadingIcon = { Icon(Icons.Default.PlayCircle, contentDescription = null, modifier = Modifier.size(14.dp)) }
                            )
                            AssistChip(
                                onClick = {
                                    overrideVideoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4"
                                },
                                label = { Text("ForBiggerBlazes MP4", style = MaterialTheme.typography.labelSmall) },
                                leadingIcon = { Icon(Icons.Default.PlayCircle, contentDescription = null, modifier = Modifier.size(14.dp)) }
                            )
                        }
                    }
                }

                // Audio Quick Setup Guide Banner
                Surface(
                    color = Color(0xFFEFF6FF),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.VolumeUp, contentDescription = null, tint = ExecutiveBlue, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("🎬 + 🗣️ Simultaneous Video & Voice Audio:", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = TextPrimary)
                            Text(
                                "• Tap 'Play Video & Voice Together' above to hear BOTH the video audio and AI lesson narration voice simultaneously.\n" +
                                "• Ensure device/browser tab volume is unmuted.\n" +
                                "• You can also switch modes using the top tabs (Web Player, Native Player, Visualizer, Voice).",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                    }
                }

                // Lesson Info Header
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Text(
                        text = module.title,
                        style = MaterialTheme.typography.headlineMedium,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = module.summary,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Tab Selector: Transcript vs Key Takeaways
                    TabRow(
                        selectedTabIndex = selectedTab,
                        containerColor = SurfaceWhite,
                        contentColor = ExecutiveBlue,
                        divider = { HorizontalDivider(color = BorderLight) }
                    ) {
                        Tab(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            text = { Text("Video Transcript", style = MaterialTheme.typography.titleSmall) }
                        )
                        Tab(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            text = { Text("Key Takeaways", style = MaterialTheme.typography.titleSmall) }
                        )
                    }
                }

                // Scrollable Content Pane
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 16.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    if (selectedTab == 0) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp),
                            colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                            border = CardDefaults.outlinedCardBorder(enabled = true)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Subtitles,
                                        contentDescription = null,
                                        tint = ExecutiveBlue,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Interactive Lesson Transcript",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = TextPrimary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = BorderLight)

                                Text(
                                    text = module.transcript,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = TextPrimary,
                                    lineHeight = MaterialTheme.typography.bodyLarge.lineHeight
                                )
                            }
                        }
                    } else {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp),
                            colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                            border = CardDefaults.outlinedCardBorder(enabled = true)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Lightbulb,
                                        contentDescription = null,
                                        tint = Color(0xFFD97706),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Core Takeaways & Objectives",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = TextPrimary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = BorderLight)

                                module.keyTakeaways.split("\n").forEach { bullet ->
                                    if (bullet.isNotBlank()) {
                                        Row(
                                            modifier = Modifier.padding(vertical = 6.dp),
                                            verticalAlignment = Alignment.Top
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.CheckCircle,
                                                contentDescription = null,
                                                tint = PassGreen,
                                                modifier = Modifier
                                                    .size(18.dp)
                                                    .padding(top = 2.dp)
                                            )
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Text(
                                                text = bullet.trim(),
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = TextPrimary
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AnimatedLessonPlayer(
    module: ModuleEntity,
    videoUrl: String,
    isPlaying: Boolean,
    isMuted: Boolean,
    progressSeconds: Float,
    totalDurationSeconds: Float
) {
    // AudioTrack ambient tone generator for visualizer mode
    LaunchedEffect(isPlaying, isMuted) {
        if (isPlaying && !isMuted) {
            withContext(Dispatchers.Default) {
                val sampleRate = 22050
                val minBufSize = AudioTrack.getMinBufferSize(
                    sampleRate,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT
                )
                val track = try {
                    AudioTrack.Builder()
                        .setAudioAttributes(
                            AudioAttributes.Builder()
                                .setUsage(AudioAttributes.USAGE_MEDIA)
                                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                                .build()
                        )
                        .setAudioFormat(
                            AudioFormat.Builder()
                                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                                .setSampleRate(sampleRate)
                                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                                .build()
                        )
                        .setBufferSizeInBytes(minBufSize.coerceAtLeast(2048))
                        .build()
                } catch (e: Exception) {
                    null
                }

                if (track != null) {
                    try {
                        track.play()
                        val numSamples = 1024
                        val buffer = ShortArray(numSamples)
                        var sampleIdx = 0L

                        val freq1 = 440.0 // A4
                        val freq2 = 554.37 // C#5

                        while (isPlaying && !isMuted) {
                            for (i in 0 until numSamples) {
                                val t = (sampleIdx + i) / sampleRate.toDouble()
                                val wave1 = sin(2.0 * Math.PI * freq1 * t)
                                val wave2 = sin(2.0 * Math.PI * freq2 * t) * 0.4
                                val envelope = (sin(2.0 * Math.PI * 0.25 * t) + 1.2) * 0.2
                                val sample = ((wave1 + wave2) * envelope * 10000).toInt().coerceIn(-32768, 32767)
                                buffer[i] = sample.toShort()
                            }
                            sampleIdx += numSamples
                            track.write(buffer, 0, numSamples)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    } finally {
                        try {
                            track.stop()
                            track.release()
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
            }
        }
    }

    var wavePhase by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(isPlaying) {
        while (isPlaying) {
            delay(50)
            wavePhase += 0.15f
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF0F172A), Color(0xFF1E293B))
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        // Animated Canvas Waveforms
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            val centerY = height / 2f

            val barCount = 32
            val barWidth = width / (barCount * 1.5f)

            for (i in 0 until barCount) {
                val x = i * barWidth * 1.5f + barWidth / 2f
                val amplitude = if (isPlaying) (sin(wavePhase + i * 0.4f) * 40f + 50f) else 20f
                val color = if (i % 2 == 0) Color(0xFF0052CC) else Color(0xFF38BDF8)

                drawLine(
                    color = color.copy(alpha = 0.6f),
                    start = Offset(x, centerY - amplitude),
                    end = Offset(x, centerY + amplitude),
                    strokeWidth = barWidth * 0.7f
                )
            }
        }

        // Title & Topic Overlay
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(24.dp)
        ) {
            Surface(
                color = ExecutiveBlue.copy(alpha = 0.2f),
                shape = RoundedCornerShape(20.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, ExecutiveBlue)
            ) {
                Text(
                    text = "MODULE ${module.moduleOrder} LESSON VISUALIZER",
                    style = MaterialTheme.typography.labelSmall,
                    color = ExecutiveBlueSoft,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = module.title,
                style = MaterialTheme.typography.titleLarge,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

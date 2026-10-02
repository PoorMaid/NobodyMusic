package com.nobodymusic.tyxypoor.source.js

import android.os.Handler
import android.os.HandlerThread
import kotlinx.coroutines.suspendCancellableCoroutine
import org.mozilla.javascript.Context
import org.mozilla.javascript.Function
import org.mozilla.javascript.Scriptable
import org.mozilla.javascript.ScriptableObject
import java.util.concurrent.CountDownLatch
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class JsSourceEngine(
    private val scriptId: String,
    private val script: String,
    private val net: JsNetwork,
    private val logger: (String) -> Unit = {}
) {
    private val thread = HandlerThread("js-source-$scriptId").apply { start() }
    private val handler = Handler(thread.looper)
    private var scope: Scriptable? = null
    private var initialized = false
    private var lastError: String? = null
    private val initedLatch = CountDownLatch(1)

    private val bootstrap = StringBuilder().apply {
        append("var globalThis = this;")
        append("var window = this;")
        append("var self = this;")
    }

    fun start(timeoutMs: Long = 8000) {
        handler.post {
            try {
                val cx = Context.enter()
                try {
                    cx.optimizationLevel = -1
                    cx.languageVersion = Context.VERSION_ES6
                    val sc = cx.initStandardObjects(null, true)
                    ScriptableObject.putProperty(sc, "__nb", NativeBridge(cx, sc))
                    cx.evaluateString(sc, bootstrap.toString(), "bootstrap", 1, null)
                    cx.evaluateString(sc, PreloadScript.JS, "preload", 1, null)
                    cx.evaluateString(sc, script, scriptId, 1, null)
                    scope = sc
                    initialized = true
                } finally {
                    Context.exit()
                }
            } catch (t: Throwable) {
                lastError = t.message
                logger("load failed: ${t.message}")
            } finally {
                initedLatch.countDown()
            }
        }
        initedLatch.await()
    }

    suspend fun requestUrl(source: String, action: String, infoJson: String): String =
        withEngine { sc, cx ->
            val nb = ScriptableObject.getProperty(sc, "__nb")
            if (nb !is ScriptableObject) throw IllegalStateException("bridge missing")
            val dispatch = nb.get("dispatch", nb)
            if (dispatch !is Function) throw IllegalStateException("dispatch missing")
            val result = dispatch.call(cx, sc, nb, arrayOf(source, action, infoJson))
            val resolved = awaitPromise(cx, sc, result)
            resolved ?: throw IllegalStateException("empty result")
        }

    suspend fun sourceConfig(): String = withEngine { sc, cx ->
        val nb = ScriptableObject.getProperty(sc, "__nb")
        val fn = (nb as ScriptableObject).get("getAllSourceConfig", nb)
        if (fn is Function) {
            val r = fn.call(cx, sc, nb, arrayOf())
            Context.toString(r)
        } else ""
    }

    private suspend fun awaitPromise(cx: Context, sc: Scriptable, value: Any?): String? {
        val promise = value as? Scriptable ?: return value?.let { Context.toString(it) }
        val thenFn = promise.get("then", promise)
        if (thenFn !is Function) return Context.toString(promise)
        var out: String? = null
        var err: Throwable? = null
        val latch = CountDownLatch(1)
        val onOk = object : org.mozilla.javascript.BaseFunction() {
            override fun call(c: Context, s: Scriptable, t: Scriptable, args: Array<Any?>): Any? {
                out = args.getOrNull(0)?.let { Context.toString(it) }
                latch.countDown()
                return null
            }
        }
        val onErr = object : org.mozilla.javascript.BaseFunction() {
            override fun call(c: Context, s: Scriptable, t: Scriptable, args: Array<Any?>): Any? {
                err = RuntimeException(args.getOrNull(0)?.let { Context.toString(it) })
                latch.countDown()
                return null
            }
        }
        (thenFn as Function).call(cx, sc, promise, arrayOf(onOk, onErr))
        latch.await()
        err?.let { throw it }
        return out
    }

    private suspend fun <T> withEngine(block: suspend (Scriptable, Context) -> T): T {
        val (sc, cx) = onJsThread { sc, cx -> Pair(sc, cx) }
        return try {
            block(sc, cx)
        } finally {
            finishBlock(cx)
        }
    }

    private suspend fun <T> onJsThread(block: (Scriptable, Context) -> T): T =
        suspendCancellableCoroutine { cont ->
            handler.post {
                val sc = scope
                if (sc == null) {
                    cont.resumeWithException(IllegalStateException(lastError ?: "engine not ready"))
                    return@post
                }
                val cx = Context.enter()
                cx.optimizationLevel = -1
                cx.languageVersion = Context.VERSION_ES6
                try {
                    cont.resume(block(sc, cx))
                } catch (t: Throwable) {
                    Context.exit()
                    cont.resumeWithException(t)
                }
            }
        }

    private fun finishBlock(cx: Context) {
        try { Context.exit() } catch (_: Throwable) {}
    }

    fun destroy() {
        try { thread.quitSafely() } catch (_: Throwable) {}
    }

    private inner class NativeBridge(private val cx: Context, private val sc: Scriptable) :
        ScriptableObject() {
        override fun getClassName() = "__nb"
        override fun get(name: String, start: Scriptable): Any {
            return when (name) {
                "request" -> NetFn()
                "onInited" -> InitedFn()
                "onUpdateAlert" -> AlertFn()
                "setupInfo" -> SetupFn()
                "nativeMd5" -> Md5Fn()
                "nativeRandomBytes" -> RandomFn()
                "nativeBufferFrom" -> BufFromFn()
                "nativeBufToString" -> BufToStrFn()
                "nativeAesEncrypt" -> EncFn()
                "nativeRsaEncrypt" -> EncFn()
                else -> super.get(name, start)
            }
        }

        override fun has(name: String, start: Scriptable): Boolean =
            listOf("request","onInited","onUpdateAlert","setupInfo","nativeMd5","nativeRandomBytes",
                "nativeBufferFrom","nativeBufToString","nativeAesEncrypt","nativeRsaEncrypt")
                .contains(name) || super.has(name, start)

        inner class NetFn : org.mozilla.javascript.BaseFunction() {
            override fun call(c: Context, s: Scriptable, t: Scriptable, args: Array<Any?>): Any? {
                val url = Context.toString(args.getOrNull(0))
                val optJson = args.getOrNull(1)?.let { Context.toString(it) } ?: "{}"
                val cb = args.getOrNull(2)
                val cbFn = cb as? Function
                return net.request(url, optJson) { body, code, err ->
                    if (cbFn != null) handler.post {
                        val c2 = Context.enter()
                        try {
                            cbFn.call(c2, s, t, arrayOf(body, code, err))
                        } catch (_: Throwable) {
                        } finally {
                            Context.exit()
                        }
                    }
                }
            }
        }

        inner class InitedFn : org.mozilla.javascript.BaseFunction() {
            override fun call(c: Context, s: Scriptable, t: Scriptable, args: Array<Any?>): Any? {
                logger("inited: ${args.getOrNull(0)}")
                return null
            }
        }

        inner class AlertFn : org.mozilla.javascript.BaseFunction() {
            override fun call(c: Context, s: Scriptable, t: Scriptable, args: Array<Any?>): Any? {
                logger("alert: ${args.getOrNull(0)}")
                return null
            }
        }

        inner class SetupFn : org.mozilla.javascript.BaseFunction() {
            override fun call(c: Context, s: Scriptable, t: Scriptable, args: Array<Any?>): Any? {
                logger("setup: ${args.getOrNull(0)}")
                return null
            }
        }

        inner class Md5Fn : org.mozilla.javascript.BaseFunction() {
            override fun call(c: Context, s: Scriptable, t: Scriptable, args: Array<Any?>): Any? {
                val v = args.getOrNull(0)?.let { Context.toString(it) } ?: ""
                return JsCrypto.md5(v)
            }
        }

        inner class RandomFn : org.mozilla.javascript.BaseFunction() {
            override fun call(c: Context, s: Scriptable, t: Scriptable, args: Array<Any?>): Any? {
                val n = args.getOrNull(0)?.let { (it as? Number)?.toInt() ?: 16 } ?: 16
                return JsCrypto.randomHex(n)
            }
        }

        inner class BufFromFn : org.mozilla.javascript.BaseFunction() {
            override fun call(c: Context, s: Scriptable, t: Scriptable, args: Array<Any?>): Any? {
                val v = args.getOrNull(0)
                return if (v is Number) JsCrypto.buffer(v.toInt()) else Context.toString(v)
            }
        }

        inner class BufToStrFn : org.mozilla.javascript.BaseFunction() {
            override fun call(c: Context, s: Scriptable, t: Scriptable, args: Array<Any?>): Any? {
                val v = args.getOrNull(0)?.let { Context.toString(it) } ?: ""
                val fmt = args.getOrNull(1)?.let { Context.toString(it) } ?: "utf8"
                return JsCrypto.bufToString(v, fmt)
            }
        }

        inner class EncFn : org.mozilla.javascript.BaseFunction() {
            override fun call(c: Context, s: Scriptable, t: Scriptable, args: Array<Any?>): Any? {
                logger("unsupported crypto call")
                return ""
            }
        }
    }
}
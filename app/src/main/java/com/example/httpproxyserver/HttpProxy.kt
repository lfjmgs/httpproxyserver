package com.example.httpproxyserver

import android.util.Log
import java.net.*
import java.util.concurrent.CountDownLatch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.regex.Pattern


const val TAG = "HttpProxy"

val executorService: ExecutorService = Executors.newCachedThreadPool()
val connectPattern: Pattern =
    Pattern.compile("CONNECT (.+):(.+) HTTP/(1\\.[01])", Pattern.CASE_INSENSITIVE)
var useSystemProxy = false

class HttpProxy(private val port: Int = 1081) : Runnable {

    var messageListener: ((String) -> Unit)? = null
    var started = false

    private var serverSocket: ServerSocket? = null

    override fun run() {
        started = true
        try {
            serverSocket = ServerSocket(port)
            sendMsg("Http proxy is listening on port: $port")
        } catch (e: Exception) {
            Log.w(TAG, e)
            started = false
            sendMsg("failed to open http proxy on port: $port")
            return
        }
        try {
            while (true) {
                val socket = serverSocket!!.accept()
                executorService.submit(RequestHandler(socket))
            }
        } catch (e: Exception) {
            Log.w(TAG, e)
        } finally {
            serverSocket?.close()
            serverSocket = null
            started = false
            sendMsg("Http proxy is stopped")
        }
    }

    private fun sendMsg(msg: String) {
        Log.d(TAG, msg)
        messageListener?.invoke(msg)
    }

    fun stop() {
        serverSocket?.close()
        serverSocket = null
    }


    companion object {
        fun start(port: Int): HttpProxy {
            val httpProxy = HttpProxy(port)
            executorService.submit(httpProxy)
            return httpProxy
        }
    }
}

class RequestHandler(private val socket: Socket) : Runnable {

    override fun run() {
        val reader = socket.getInputStream().bufferedReader(Charsets.ISO_8859_1)
        var line = reader.readLine()
        Log.d(TAG, "start-line: $line")
        val matcher = connectPattern.matcher(line)
        if (matcher.matches()) {
            val writer = socket.getOutputStream().bufferedWriter(Charsets.ISO_8859_1)
            val host = matcher.group(1)
            val port = matcher.group(2)
            val version = matcher.group(3)
            if (port?.toIntOrNull() == null) {
                socket.close()
                return
            }
            while (true) {
                line = reader.readLine()
                if (!line.isNullOrEmpty()) {
                    Log.d(TAG, "header: $line")
                } else {
                    break
                }
            }
            val forwardSocket: Socket
            try {
                val proxyHost = System.getProperty("http.proxyHost")
                val proxyPort = System.getProperty("http.proxyPort")
                if (useSystemProxy && proxyHost != null && proxyPort != null) {
                    Log.d(TAG, "system http proxy: $proxyHost:$proxyPort")
                    val proxyAddress = InetSocketAddress(proxyHost, proxyPort.toInt())
                    // can't use Proxy.Type.HTTP
                    val proxy = Proxy(Proxy.Type.SOCKS, proxyAddress)
                    forwardSocket = Socket(proxy)
                    forwardSocket.connect(InetSocketAddress(host, port.toInt()))
                } else {
                    forwardSocket = Socket(host, port.toInt())
                }
            } catch (e: Exception) {
                Log.w(TAG, e)
                writer.write("HTTP/$version 502 Bad Gateway\r\n")
                writer.write("\r\n")
                writer.flush()
                socket.close()
                return
            }
            writer.write("HTTP/$version 200 Connection established\r\n")
            writer.write("\r\n")
            writer.flush()
            val latch = CountDownLatch(1)
            executorService.submit {
                try {
                    forwardSocket.getInputStream().copyTo(socket.getOutputStream())
                } catch (e: Exception) {
                    Log.w(TAG, e)
                } finally {
                    if (!forwardSocket.isInputShutdown) {
                        forwardSocket.shutdownInput()
                    }
                    if (!socket.isOutputShutdown) {
                        socket.shutdownOutput()
                    }
                    latch.countDown()
                }
            }
            try {
                socket.getInputStream().copyTo(forwardSocket.getOutputStream())
            } catch (e: Exception) {
                Log.w(TAG, e)
            } finally {
                if (!socket.isInputShutdown) {
                    socket.shutdownInput()
                }
                if (!forwardSocket.isOutputShutdown) {
                    forwardSocket.shutdownOutput()
                }
            }
            latch.await()
            forwardSocket.close()
        }
        socket.close()
    }

}
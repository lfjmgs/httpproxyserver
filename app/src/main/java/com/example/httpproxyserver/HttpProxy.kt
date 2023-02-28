package com.example.httpproxyserver

import android.util.Log
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.CountDownLatch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.regex.Pattern

const val TAG = "HttpProxy"

val executorService: ExecutorService = Executors.newCachedThreadPool()
val connectPattern: Pattern =
    Pattern.compile("CONNECT (.+):(.+) HTTP/(1\\.[01])", Pattern.CASE_INSENSITIVE)

class HttpProxy(private val port: Int = 1081) : Runnable {

    override fun run() {
        try {
            val serverSocket = ServerSocket(port)
            while (true) {
                val socket = serverSocket.accept()
                executorService.submit(RequestHandler(socket))
            }
        } catch (e: Exception) {
            Log.w(TAG, e)
        }

    }
}

class RequestHandler(private val socket: Socket) : Runnable {

    override fun run() {
        val reader = socket.getInputStream().bufferedReader(Charsets.ISO_8859_1)
        var line = reader.readLine()
        Log.d(TAG, "request line: $line")
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
                forwardSocket = Socket(host, port.toInt())
            } catch (e: Exception) {
                Log.w(TAG, e)
                writer.write("HTTP/${version} 502 Bad Gateway\r\n")
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
                        Log.d(TAG, "forwardSocket.shutdownInput()")
                        forwardSocket.shutdownInput()
                    }
                    if (!socket.isOutputShutdown) {
                        Log.d(TAG, "socket.shutdownOutput()")
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
                    Log.d(TAG, "socket.shutdownInput()")
                    socket.shutdownInput()
                }
                if (!forwardSocket.isOutputShutdown) {
                    Log.d(TAG, "forwardSocket.shutdownOutput()")
                    forwardSocket.shutdownOutput()
                }
            }
            latch.await()
            forwardSocket.close()
        }
        socket.close()
    }

}
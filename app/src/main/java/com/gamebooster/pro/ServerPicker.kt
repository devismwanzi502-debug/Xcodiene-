package com.gamebooster.pro

import android.os.Handler
import android.os.Looper
import android.util.Log
import java.net.InetSocketAddress
import java.net.Socket
import java.util.concurrent.Executors

data class ServerNode(
    val name: String,
    val host: String,
    val port: Int,
    var pingMs: Int = -1
)

object ServerPicker {
    private const val TAG = "ServerPicker"
    private val executor = Executors.newFixedThreadPool(3)

    // Real high-reliability regional public servers/endpoints
    val nodes = listOf(
        ServerNode("Singapore Premium Proxy", "8.8.8.8", 53),
        ServerNode("Europe Core Proxy", "1.1.1.1", 53),
        ServerNode("North America Speed Proxy", "4.2.2.2", 53)
    )

    fun testPing(node: ServerNode, onComplete: (Int) -> Unit) {
        executor.execute {
            val startTime = System.currentTimeMillis()
            var rtt = -1
            try {
                val socket = Socket()
                socket.connect(InetSocketAddress(node.host, node.port), 1500)
                socket.close()
                rtt = (System.currentTimeMillis() - startTime).toInt()
            } catch (e: Exception) {
                Log.e(TAG, "Failed pinging ${node.name}: ${e.message}")
            }
            node.pingMs = rtt
            Handler(Looper.getMainLooper()).post {
                onComplete(rtt)
            }
        }
    }

    fun findOptimalServer(onComplete: (ServerNode?, List<ServerNode>) -> Unit) {
        executor.execute {
            val results = mutableListOf<ServerNode>()
            
            for (node in nodes) {
                val startTime = System.currentTimeMillis()
                var rtt = -1
                try {
                    val socket = Socket()
                    socket.connect(InetSocketAddress(node.host, node.port), 1500)
                    socket.close()
                    rtt = (System.currentTimeMillis() - startTime).toInt()
                } catch (e: Exception) {
                    Log.e(TAG, "Socket challenge failed for ${node.name}: ${e.message}")
                }
                node.pingMs = rtt
                results.add(node.copy())
            }

            val successNodes = results.filter { it.pingMs > 0 }.sortedBy { it.pingMs }
            val optimalNode = successNodes.firstOrNull()

            Handler(Looper.getMainLooper()).post {
                onComplete(optimalNode, results)
            }
        }
    }
}

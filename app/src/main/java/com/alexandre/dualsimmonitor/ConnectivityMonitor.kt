package com.alexandre.dualsimmonitor

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import java.net.HttpURLConnection
import java.net.URL

object ConnectivityMonitor {
    fun read(context: Context): ConnectivityInfo {
        val manager = context.getSystemService(ConnectivityManager::class.java)
        val caps = manager.getNetworkCapabilities(manager.activeNetwork) ?: return ConnectivityInfo()
        val transport = when {
            caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "Celular"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "Wi‑Fi"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "Ethernet"
            else -> "Outro"
        }
        return ConnectivityInfo(true, transport, if (caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)) "Sim" else "Não")
    }

    /** Five lightweight HTTPS probes; no ICMP and no download-heavy speed test. */
    fun measure(context: Context): ConnectivityProbe {
        val manager = context.getSystemService(ConnectivityManager::class.java)
        val caps = manager.getNetworkCapabilities(manager.activeNetwork)
        if (caps == null || !caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) return ConnectivityProbe(null, 100f)
        val times = mutableListOf<Long>()
        repeat(5) {
            val start = System.nanoTime()
            val ok = runCatching {
                (URL("https://connectivitycheck.gstatic.com/generate_204").openConnection() as HttpURLConnection).run {
                    connectTimeout = 1500
                    readTimeout = 1500
                    requestMethod = "HEAD"
                    instanceFollowRedirects = false
                    connect()
                    val success = responseCode in 200..399
                    disconnect()
                    success
                }
            }.getOrDefault(false)
            if (ok) times += (System.nanoTime() - start) / 1_000_000
        }
        val loss = ((5 - times.size) * 100f) / 5f
        return ConnectivityProbe(times.average().takeIf { times.isNotEmpty() }?.toLong(), loss)
    }
}

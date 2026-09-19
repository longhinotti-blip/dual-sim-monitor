package com.alexandre.dualsimmonitor

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities

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
}

package com.alexandre.dualsimmonitor

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private enum class Tab(val label: String) { HOME("INÍCIO"), MONITOR("MONITORAMENTO"), HISTORY("HISTÓRICO"), DIAGNOSTIC("DIAGNÓSTICO") }
private fun timeOf(millis: Long, pattern: String = "HH:mm:ss"): String = SimpleDateFormat(pattern, Locale.getDefault()).format(Date(millis))

@Composable fun DualSimApp(repository: TelephonyRepository) {
    var tab by remember { mutableStateOf(Tab.HOME) }; var last by remember { mutableStateOf<MonitorState?>(null) }; var refreshing by remember { mutableStateOf(false) }; var monitoring by remember { mutableStateOf(false) }; var history by remember { mutableStateOf(repository.loadHistory()) }; var clear by remember { mutableStateOf(false) }
    fun accept(value: MonitorState) { if (value.sims.isNotEmpty()) last = value }
    fun refresh() { refreshing = true; repository.refresh { accept(it); refreshing = false } }
    fun sample() { repository.monitorAndSave { accept(it); history = repository.loadHistory() } }
    LaunchedEffect(Unit) { refresh() }; LaunchedEffect(monitoring) { if (monitoring) { sample(); while (monitoring) { delay(10_000); if (monitoring) sample() } } }
    Scaffold(bottomBar = { NavigationBar { Tab.values().forEach { t -> NavigationBarItem(tab == t, { tab = t }, icon = { Text(t.label.take(1)) }, label = { Text(t.label) }) } } }) { p -> Column(Modifier.fillMaxSize().padding(p).padding(16.dp)) { when(tab) { Tab.HOME -> Home(last, refreshing, ::refresh); Tab.MONITOR -> Monitor(last, monitoring, { monitoring = true }, { monitoring = false }); Tab.HISTORY -> History(history) { clear = true }; Tab.DIAGNOSTIC -> Diagnostic(last ?: MonitorState()) } } }
    if (clear) AlertDialog(onDismissRequest = { clear = false }, title = { Text("Limpar histórico?") }, text = { Text("Todas as medições locais serão apagadas.") }, confirmButton = { TextButton({ repository.clearHistory(); history = emptyList(); clear = false }) { Text("LIMPAR") } }, dismissButton = { TextButton({ clear = false }) { Text("CANCELAR") } })
}
@Composable private fun Home(state: MonitorState?, refreshing: Boolean, refresh: () -> Unit) { LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) { item { Text("Dual SIM Monitor v0.2") }; item { Text("SIM de dados: ${state?.dataSim?.carrier ?: "Sem medições ainda."}") }; state?.sims?.take(2)?.let { sims -> items(sims) { s -> Card { Column(Modifier.padding(12.dp)) { Text("SIM ${s.slot + 1} — ${s.carrier}"); Text("${s.cell?.technology ?: "—"} • ${s.cell?.dbm ?: "—"}") } } } }; item { Button(refresh, enabled = !refreshing, modifier = Modifier.fillMaxWidth()) { Text(if (refreshing) "Atualizando…" else "Atualizar") } } } }
@Composable private fun Monitor(state: MonitorState?, running: Boolean, start: () -> Unit, stop: () -> Unit) { Column(verticalArrangement = Arrangement.spacedBy(10.dp)) { Text("MONITORAMENTO"); Text(if (running) "● Monitorando" else "● Parado"); Row { Button(start, enabled = !running, modifier = Modifier.weight(1f)) { Text("INICIAR") }; Spacer(Modifier.width(8.dp)); Button(stop, enabled = running, modifier = Modifier.weight(1f)) { Text("PARAR") } }; if (state == null) Text("Sem medições ainda.") else { Text("Última medição: ${timeOf(state.timestamp)}"); Text("SIM de dados: ${state.dataSim?.carrier ?: "Indisponível"}"); Text("Latência: ${state.connectivity.latencyMs?.let { "$it ms" } ?: "—"} • Perda: ${state.connectivity.packetLossPercent?.let { "$it%" } ?: "—"}"); state.sims.forEach { s -> Card { Column(Modifier.padding(10.dp)) { Text("SIM ${s.slot + 1} — ${s.carrier}"); Text("${s.cell?.technology ?: "—"} • dBm ${s.cell?.dbm ?: "—"} • RSRP ${s.cell?.rsrp ?: "—"} • RSRQ ${s.cell?.rsrq ?: "—"} • SINR ${s.cell?.sinr ?: "—"}") } } } } } }
@Composable private fun History(history: List<Measurement>, clear: () -> Unit) { Column { Text("HISTÓRICO"); Text("${history.size} medições"); Button(clear) { Text("LIMPAR HISTÓRICO") }; LazyColumn { items(history.take(100)) { m -> Text("${timeOf(m.timestampMillis, "dd/MM/yyyy HH:mm:ss")} • ${m.carrier} • SIM ${m.slot} • ${if (m.isDefaultDataSim) "SIM de dados" else "SIM secundário"}") } } } }
@Composable private fun Diagnostic(state: MonitorState) { LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) { item { Text("DIAGNÓSTICO"); Text("DIAGNÓSTICO DA LEITURA") }; items(state.sims) { s -> Card { Column(Modifier.padding(12.dp)) { Text("SIM ${s.slot + 1} — ${s.carrier} • subscriptionId ${s.subscriptionId}"); Text("Célula registrada: ${s.cell?.cell ?: "—"}"); Text("Células encontradas: ${s.cells.size}") } } }; items(state.diagnostics) { d -> Card { Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) { Text("SIM ${d.slot + 1} • subscriptionId ${d.subscriptionId}"); Text("Classe: ${d.cellClass}"); Text("Registrada: ${d.registered} • Células retornadas: ${d.cellCount}"); Text("Timestamp: ${timeOf(d.timestampMillis)} • Idade: ${d.ageMillis / 1000}s"); Text("dBm bruto: ${d.dbmRaw}"); Text("RSRP bruto: ${d.rsrpRaw}"); Text("RSRQ bruto: ${d.rsrqRaw}"); Text("RSSNR/SINR bruto: ${d.rssnrRaw}"); Text("RSSNR/SINR exibido: ${d.rssnrFormatted}") } } } } }

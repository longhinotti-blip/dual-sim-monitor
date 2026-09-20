package com.alexandre.dualsimmonitor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private enum class Tab(val label: String) { HOME("INÍCIO"), MONITOR("MONITORAMENTO"), HISTORY("HISTÓRICO"), DIAGNOSTIC("DIAGNÓSTICO") }

@Composable
fun DualSimApp(repository: TelephonyRepository) {
    var tab by remember { mutableStateOf(Tab.HOME) }
    var state by remember { mutableStateOf(MonitorState()) }
    var refreshing by remember { mutableStateOf(false) }
    var monitoring by remember { mutableStateOf(false) }
    var history by remember { mutableStateOf(repository.loadHistory()) }
    var confirmClear by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    fun refresh() { refreshing = true; repository.refresh { state = it; refreshing = false } }
    LaunchedEffect(Unit) { refresh() }
    LaunchedEffect(monitoring) { while (monitoring) { refresh(); delay(10_000) } }
    Scaffold(bottomBar = { NavigationBar(Modifier.navigationBarsPadding()) { Tab.values().forEach { item -> NavigationBarItem(selected = tab == item, onClick = { tab = item }, icon = { Text(item.label.take(1)) }, label = { Text(item.label) }) } } }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            when (tab) {
                Tab.HOME -> HomeScreen(state, refreshing, ::refresh) { tab = Tab.DIAGNOSTIC }
                Tab.MONITOR -> MonitorScreen(state, monitoring, { monitoring = true }, { monitoring = false }, ::refresh)
                Tab.HISTORY -> HistoryScreen(history, { confirmClear = true })
                Tab.DIAGNOSTIC -> DiagnosticScreen(state, repository)
            }
        }
    }
    if (confirmClear) AlertDialog(onDismissRequest = { confirmClear = false }, title = { Text("Limpar histórico?") }, text = { Text("Todas as medições locais serão apagadas.") }, confirmButton = { TextButton(onClick = { repository.clearHistory(); history = emptyList(); confirmClear = false }) { Text("LIMPAR") } }, dismissButton = { TextButton(onClick = { confirmClear = false }) { Text("CANCELAR") } })
}

@Composable private fun HomeScreen(state: MonitorState, refreshing: Boolean, refresh: () -> Unit, diagnostic: () -> Unit) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text("Dual SIM Monitor v0.2") }
        item { Card(colors = CardDefaults.cardColors()) { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) { Text("CONEXÃO ATUAL"); Text("${state.connectivity.transport} • ${state.connectivity.validated}"); Text("SIM DE DADOS ATUAL"); Text(state.dataSim?.let { "${it.carrier} — SIM ${it.slot + 1} • subscriptionId ${it.subscriptionId}" } ?: "Indisponível") } } }
        items(state.sims.take(2)) { sim -> Card { Column(Modifier.padding(16.dp)) { Text("SIM ${sim.slot + 1} — ${sim.carrier}"); Text(sim.cell?.let { "${it.technology} • ${it.dbm}" } ?: "Indisponível"); Text(sim.cell?.let { "RSRP ${it.rsrp} • RSRQ ${it.rsrq} • SINR ${it.sinr}" } ?: "Qualidade indisponível") } } }
        item { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) { Button(onClick = refresh, enabled = !refreshing, modifier = Modifier.weight(1f)) { Text(if (refreshing) "Atualizando…" else "Atualizar") }; Button(onClick = diagnostic, modifier = Modifier.weight(1f)) { Text("Diagnóstico") } } }
    }
}

@Composable private fun MonitorScreen(state: MonitorState, monitoring: Boolean, start: () -> Unit, stop: () -> Unit, refresh: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) { Text("MONITORAMENTO"); Text(if (monitoring) "● Monitorando" else "● Parado"); Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { Button(onClick = start, enabled = !monitoring, modifier = Modifier.weight(1f)) { Text("INICIAR") }; Button(onClick = stop, enabled = monitoring, modifier = Modifier.weight(1f)) { Text("PARAR") } }; Text("Atualização: 10 segundos"); Text("Internet: ${state.connectivity.transport}. Nenhuma configuração de rede é alterada."); state.sims.forEach { sim -> Card { Column(Modifier.padding(12.dp)) { Text("SIM ${sim.slot + 1} • ${sim.carrier}"); Text("Hora | Rede | dBm | RSRP | RSRQ | SINR"); Text("${sim.cell?.technology ?: "Indisponível"} | ${sim.cell?.dbm ?: "Indisponível"} | ${sim.cell?.rsrp ?: "Indisponível"} | ${sim.cell?.rsrq ?: "Indisponível"} | ${sim.cell?.sinr ?: "Indisponível"}") } } } }
}

@Composable private fun HistoryScreen(history: List<Measurement>, clear: () -> Unit) { Column(verticalArrangement = Arrangement.spacedBy(10.dp)) { Text("HISTÓRICO"); Text("${history.size} medições locais"); Button(onClick = clear, enabled = history.isNotEmpty()) { Text("LIMPAR HISTÓRICO") }; LazyColumn { items(history.take(100)) { m -> Card(Modifier.padding(vertical = 3.dp)) { Column(Modifier.padding(10.dp)) { Text("${m.carrier} • SIM ${m.slot}"); Text("${m.technology} • dBm ${m.dbm} • RSRP ${m.rsrp} • RSRQ ${m.rsrq} • SINR ${m.sinr}") } } } } } }

@Composable private fun DiagnosticScreen(state: MonitorState, repository: TelephonyRepository) { Column(verticalArrangement = Arrangement.spacedBy(10.dp)) { Text("DIAGNÓSTICO"); Text("Dual SIM Monitor v0.2"); state.sims.forEach { sim -> Card { Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) { Text("SIM ${sim.slot + 1} — ${sim.carrier}"); Text("Slot ${sim.slot + 1} • subscriptionId ${sim.subscriptionId} • ativo ${sim.active}"); Text("Tecnologia ${sim.cell?.technology ?: "Indisponível"} • nível ${sim.cell?.asu ?: "Indisponível"}"); Text("dBm ${sim.cell?.dbm ?: "Indisponível"} • RSRP ${sim.cell?.rsrp ?: "Indisponível"}"); Text("RSRQ ${sim.cell?.rsrq ?: "Indisponível"} • RSSNR/SINR ${sim.cell?.sinr ?: "Indisponível"}"); Text("PCI ${sim.cell?.pci ?: "Indisponível"} • TAC ${sim.cell?.tac ?: "Indisponível"} • EARFCN ${sim.cell?.earfcn ?: "Indisponível"} • NRARFCN ${sim.cell?.nrarfcn ?: "Indisponível"}"); Text("Células: ${sim.cells.size} • atualização ${sim.timestamp}") } } }; Button(onClick = { repository.exportDiagnostic(state) }) { Text("EXPORTAR DIAGNÓSTICO") }; Text("VER LOG TÉCNICO: disponível no logcat do sistema durante a coleta.") } }

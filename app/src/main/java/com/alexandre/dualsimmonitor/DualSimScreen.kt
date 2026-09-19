package com.alexandre.dualsimmonitor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@Composable
fun DualSimScreen(repository: TelephonyRepository) {
    var state by remember { mutableStateOf(MonitorState()) }
    var refreshing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    fun reload() = scope.launch { refreshing = true; state = repository.refresh(); refreshing = false }
    LaunchedEffect(Unit) { reload() }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("DUAL SIM MONITOR", style = MaterialTheme.typography.headlineSmall)
        Text(if (state.connectivity.connected) "Status geral: conectado" else "Status geral: desconectado")
        InfoCard("CONECTIVIDADE", listOf("Transporte" to state.connectivity.transport, "Internet validada" to state.connectivity.validated))
        if (!state.locationGranted) Text("A localização é necessária para o Android liberar informações de células. É usada somente localmente para diagnóstico.")
        state.sims.take(2).forEachIndexed { index, sim -> SimCard("SIM ${index + 1}", sim) }
        if (state.sims.isEmpty()) Text(if (state.phoneGranted) "Nenhuma assinatura ativa encontrada." else "Permissão de telefone não concedida.")
        InfoCard("DADOS MÓVEIS", listOf("SIM de dados" to (state.dataSim?.let { "SIM no slot ${it.slot + 1}" } ?: "Indisponível"), "Operadora" to (state.dataSim?.carrier ?: "Indisponível"), "subscriptionId" to (state.dataSim?.subscriptionId?.toString() ?: "Indisponível"), "Slot" to (state.dataSim?.slot?.plus(1)?.toString() ?: "Indisponível")))
        Button(onClick = { reload() }, enabled = !refreshing, modifier = Modifier.fillMaxWidth()) { Text(if (refreshing) "Atualizando…" else "Atualizar") }
        Text("Versão 0.1 • Somente diagnóstico. Nenhuma configuração de rede é alterada.")
    }
}

@Composable private fun SimCard(title: String, sim: SimInfo) {
    val c = sim.cell
    InfoCard(title, listOf("Número (parcial)" to sim.number, "Operadora" to sim.carrier, "Slot" to if (sim.slot >= 0) (sim.slot + 1).toString() else "Indisponível", "subscriptionId" to sim.subscriptionId.toString(), "SIM ativo" to if (sim.active) "Sim" else "Não", "Rede / tecnologia" to (c?.technology ?: "Indisponível"), "Nível / dBm" to (c?.dbm ?: "Indisponível"), "ASU" to (c?.asu ?: "Indisponível"), "Célula registrada" to (c?.cell ?: "Indisponível"), "RSRP" to (c?.rsrp ?: "Indisponível"), "RSRQ" to (c?.rsrq ?: "Indisponível"), "RSSNR / SINR" to (c?.sinr ?: "Indisponível"), "Células encontradas" to sim.cells.size.toString()))
}

@Composable private fun InfoCard(title: String, values: List<Pair<String, String>>) {
    Card(colors = CardDefaults.cardColors()) { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) { Text(title, style = MaterialTheme.typography.titleMedium); values.forEach { (label, value) -> Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text(label); Text(value) } } } }
}

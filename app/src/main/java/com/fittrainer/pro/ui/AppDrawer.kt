package com.fittrainer.pro.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AttachMoney
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.CalendarViewMonth
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.MonitorHeart
import androidx.compose.material.icons.outlined.NorthEast
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.TrendingUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fittrainer.pro.ui.theme.FitColors

data class DrawerItem(
    val route: String,
    val label: String,
    val icon: ImageVector
)

val drawerItems = listOf(
    DrawerItem("home", "Visão geral", Icons.Outlined.Home),
    DrawerItem("agenda", "Agenda", Icons.Outlined.CalendarViewMonth),
    DrawerItem("clientes", "Alunos", Icons.Outlined.People),
    DrawerItem("treinos", "Treinos salvos", Icons.Outlined.FitnessCenter),
    DrawerItem("assessoria", "Assessoria", Icons.Outlined.DateRange),
    DrawerItem("exercicios", "Exercícios", Icons.Outlined.Bolt),
    DrawerItem("financeiro", "Financeiro", Icons.Outlined.AttachMoney),
    DrawerItem("relatorios", "Relatórios", Icons.Outlined.BarChart),
    DrawerItem("avaliacoes", "Avaliações", Icons.Outlined.MonitorHeart),
    DrawerItem("progresso", "Progresso", Icons.Outlined.TrendingUp),
    DrawerItem("backup", "Configurações", Icons.Outlined.Settings)
)

@Composable
fun AppDrawer(
    currentRoute: String,
    onSelect: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxHeight()
            .width(248.dp)
            .background(FitColors.Sidebar)
            .padding(18.dp, 28.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(horizontal = 8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(13.dp))
                    .background(FitColors.Green),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Outlined.NorthEast, contentDescription = null, tint = FitColors.Paper, modifier = Modifier.size(20.dp))
            }
            Column {
                Text("FitTrainer Pro", fontWeight = FontWeight.SemiBold, fontSize = 18.sp, color = FitColors.Ink)
                Text("espaço do treinador", fontSize = 12.sp, color = FitColors.Muted)
            }
        }
        Spacer(Modifier.height(22.dp))
        Text(
            "MENU",
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 1.3.sp,
            color = FitColors.NavLabel,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
        )
        drawerItems.forEach { item ->
            val selected = currentRoute == item.route ||
                (item.route == "home" && currentRoute.isBlank())
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(
                        if (selected) Modifier.shadow(2.dp, RoundedCornerShape(11.dp)) else Modifier
                    )
                    .clip(RoundedCornerShape(11.dp))
                    .background(if (selected) FitColors.Paper else FitColors.Sidebar)
                    .clickable { onSelect(item.route) }
                    .padding(horizontal = 12.dp, vertical = 11.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    item.icon,
                    contentDescription = item.label,
                    tint = if (selected) FitColors.GreenDark else FitColors.NavIdle,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    item.label,
                    color = if (selected) FitColors.GreenDark else FitColors.NavIdle,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                    fontSize = 15.sp
                )
            }
        }
        Spacer(Modifier.weight(1f))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(15.dp))
                .background(FitColors.SidebarBottom)
                .padding(15.dp)
        ) {
            Text("Treino vinculado", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = FitColors.Ink)
            Text(
                "Visualize o plano associado a cada agendamento ou abra qualquer treino salvo.",
                fontSize = 12.sp,
                color = FitColors.NavIdle
            )
        }
    }
}

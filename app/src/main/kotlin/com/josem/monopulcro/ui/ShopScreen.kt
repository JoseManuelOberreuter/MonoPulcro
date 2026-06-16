package com.josem.monopulcro.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.josem.monopulcro.R
import com.josem.monopulcro.data.MonkeyStateManager.Companion.AccessoryItem
import com.josem.monopulcro.data.MonkeyStateManager.Companion.ACCESSORIES

private val ShopWaveColor = Color(0xFF7DD3FC)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShopScreen(
    onNavigateBack: () -> Unit,
    vm: MonkeyViewModel = viewModel()
) {
    val state by vm.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Tienda",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White
                )
            )
        },
        containerColor = Color.White
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = painterResource(R.drawable.banana),
                    contentDescription = null,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "${state.bananas} bananas disponibles",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFFEA580C)
                )
            }

            Spacer(Modifier.height(4.dp))

            ACCESSORIES.forEach { accessory ->
                val isOwned    = accessory.id in state.ownedAccessories
                val isEquipped = state.equippedAccessory == accessory.id
                val canAfford  = state.bananas >= accessory.price

                AccessoryCard(
                    accessory  = accessory,
                    isOwned    = isOwned,
                    isEquipped = isEquipped,
                    canAfford  = canAfford,
                    onBuy      = { vm.buyAccessory(accessory.id) },
                    onUse      = { vm.useAccessory(accessory.id) }
                )
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun AccessoryCard(
    accessory: AccessoryItem,
    isOwned: Boolean,
    isEquipped: Boolean,
    canAfford: Boolean,
    onBuy: () -> Unit,
    onUse: () -> Unit
) {
    val cardBackground = when {
        isEquipped -> Color(0xFFE0F2FE)
        isOwned    -> Color(0xFFF0FDF4)
        else       -> Color(0xFFF8FAFC)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(cardBackground, RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Image(
            painter = painterResource(accessoryDrawable(accessory.id)),
            contentDescription = accessory.name,
            modifier = Modifier.size(72.dp)
        )

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = accessory.name,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1E293B)
            )
            Spacer(Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painter = painterResource(R.drawable.banana),
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = "${accessory.price} bananas",
                    fontSize = 13.sp,
                    color = Color(0xFF64748B)
                )
            }
        }

        when {
            isEquipped -> {
                Box(
                    modifier = Modifier
                        .background(ShopWaveColor, RoundedCornerShape(10.dp))
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "✓ Puesto",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
            isOwned -> {
                Button(
                    onClick = onUse,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF16A34A)
                    ),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text("Usar", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
            else -> {
                Button(
                    onClick = onBuy,
                    enabled = canAfford,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFEA580C),
                        disabledContainerColor = Color(0xFFE2E8F0)
                    ),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "Comprar",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (canAfford) Color.White else Color(0xFF94A3B8)
                    )
                }
            }
        }
    }
}

private fun accessoryDrawable(accessoryId: String): Int = when (accessoryId) {
    "glasses"   -> R.drawable.mono_cool
    "hat"       -> R.drawable.mono_gorro
    "crown"     -> R.drawable.mono_corona
    "astronaut" -> R.drawable.mono_astronauta
    else        -> R.drawable.mono_pulcro
}

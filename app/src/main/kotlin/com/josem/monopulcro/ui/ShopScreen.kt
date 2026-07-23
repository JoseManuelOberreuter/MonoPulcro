package com.josem.monopulcro.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.josem.monopulcro.R
import com.josem.monopulcro.data.MonkeyStateManager
import com.josem.monopulcro.data.MonkeyStateManager.Companion.AccessoryItem
import com.josem.monopulcro.data.MonkeyStateManager.Companion.ACCESSORIES
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

private val ShopWaveColor = Color(0xFF7DD3FC)
private val ShopWaveSoft = Color(0xFFE0F2FE)
private val ShopAccent = Color(0xFFEA580C)

private enum class ShopTab(val title: String) {
    OUTFITS("Atuendos"),
    OBJECTS("Objetos"),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShopScreen(
    onNavigateBack: () -> Unit,
    vm: MonkeyViewModel = viewModel()
) {
    val state by vm.uiState.collectAsStateWithLifecycle()
    val tabs = ShopTab.entries
    val pagerState = rememberPagerState(pageCount = { tabs.size })
    val scope = rememberCoroutineScope()

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
                actions = {
                    Row(
                        modifier = Modifier.padding(end = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Image(
                            painter = painterResource(R.drawable.banana),
                            contentDescription = null,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${state.bananas}",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = ShopAccent
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
        ) {
            ShopSegmentedTabs(
                tabs = tabs,
                pagerState = pagerState,
                onSelect = { tab ->
                    scope.launch { pagerState.animateScrollToPage(tab.ordinal) }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            )

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                when (tabs[page]) {
                    ShopTab.OUTFITS -> OutfitsShopPage(
                        bananas = state.bananas,
                        ownedAccessories = state.ownedAccessories,
                        equippedAccessory = state.equippedAccessory,
                        onBuy = { vm.buyAccessory(it) },
                        onUse = { vm.useAccessory(it) }
                    )
                    ShopTab.OBJECTS -> ObjectsShopPage(
                        bananas = state.bananas,
                        shieldsCount = state.shieldsCount,
                        maxShields = state.maxShields,
                        onBuyShield = { vm.buyShield() }
                    )
                }
            }
        }
    }
}

@Composable
private fun ShopSegmentedTabs(
    tabs: List<ShopTab>,
    pagerState: PagerState,
    onSelect: (ShopTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    var trackWidthPx by remember { mutableFloatStateOf(0f) }
    val gapPx = with(density) { 4.dp.toPx() }
    val tabCount = tabs.size.coerceAtLeast(1)
    val pillWidthPx = if (trackWidthPx > 0f) {
        (trackWidthPx - gapPx * (tabCount - 1)) / tabCount
    } else 0f

    // Pastilla sincronizada con el pager (swipe y tap con animateScrollToPage).
    val pageProgress = (pagerState.currentPage + pagerState.currentPageOffsetFraction)
        .coerceIn(0f, (tabCount - 1).toFloat())
    val pillOffsetPx = pageProgress * (pillWidthPx + gapPx)

    Box(
        modifier = modifier
            .height(44.dp)
            .background(ShopWaveSoft, RoundedCornerShape(14.dp))
            .border(1.5.dp, ShopWaveColor, RoundedCornerShape(14.dp))
            .padding(4.dp)
            .onSizeChanged { trackWidthPx = it.width.toFloat() }
    ) {
        if (pillWidthPx > 0f) {
            Box(
                modifier = Modifier
                    .offset { IntOffset(pillOffsetPx.roundToInt(), 0) }
                    .width(with(density) { pillWidthPx.toDp() })
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(10.dp))
                    .background(ShopWaveColor)
            )
        }

        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            tabs.forEachIndexed { index, tab ->
                val selectedAmount = (1f - abs(pageProgress - index)).coerceIn(0f, 1f)
                val textColor = lerp(Color(0xFF0369A1), Color.White, selectedAmount)
                val weight = FontWeight(
                    (FontWeight.SemiBold.weight +
                        ((FontWeight.Bold.weight - FontWeight.SemiBold.weight) * selectedAmount))
                        .roundToInt()
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(10.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { onSelect(tab) }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = tab.title,
                        fontSize = 15.sp,
                        fontWeight = weight,
                        color = textColor,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
private fun OutfitsShopPage(
    bananas: Int,
    ownedAccessories: Set<String>,
    equippedAccessory: String?,
    onBuy: (String) -> Unit,
    onUse: (String) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        ACCESSORIES.forEach { accessory ->
            val isOwned = accessory.id in ownedAccessories
            val isEquipped = equippedAccessory == accessory.id
            val canAfford = bananas >= accessory.price

            AccessoryCard(
                accessory = accessory,
                isOwned = isOwned,
                isEquipped = isEquipped,
                canAfford = canAfford,
                onBuy = { onBuy(accessory.id) },
                onUse = { onUse(accessory.id) }
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun ObjectsShopPage(
    bananas: Int,
    shieldsCount: Int,
    maxShields: Int,
    onBuyShield: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        ShieldShopCard(
            shieldsCount = shieldsCount,
            maxShields = maxShields,
            price = MonkeyStateManager.SHIELD_SHOP_PRICE,
            canAfford = bananas >= MonkeyStateManager.SHIELD_SHOP_PRICE,
            onBuy = onBuyShield
        )
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun ShieldShopCard(
    shieldsCount: Int,
    maxShields: Int,
    price: Int,
    canAfford: Boolean,
    onBuy: () -> Unit,
) {
    val atMax = shieldsCount >= maxShields
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFE0F2FE), RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Image(
            painter = painterResource(R.drawable.escudo_pulcritud),
            contentDescription = null,
            modifier = Modifier.size(72.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Escudo de Pulcritud",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1E293B)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "Tienes $shieldsCount/$maxShields",
                fontSize = 13.sp,
                color = Color(0xFF0369A1),
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painter = painterResource(R.drawable.banana),
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "$price bananas",
                    fontSize = 13.sp,
                    color = Color(0xFF64748B)
                )
            }
        }
        if (atMax) {
            Box(
                modifier = Modifier
                    .background(Color(0xFF94A3B8), RoundedCornerShape(10.dp))
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "Máximo",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        } else {
            Button(
                onClick = onBuy,
                enabled = canAfford,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF0284C7),
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
        AccessoryPreview(
            accessoryId = accessory.id,
            modifier = Modifier.size(72.dp)
        )

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = accessory.name,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1E293B)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painter = painterResource(R.drawable.banana),
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
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

@Composable
private fun AccessoryPreview(
    accessoryId: String,
    modifier: Modifier = Modifier
) {
    Image(
        painter = painterResource(MonkeyImageResolver.previewForAccessory(accessoryId)),
        contentDescription = null,
        modifier = modifier
    )
}

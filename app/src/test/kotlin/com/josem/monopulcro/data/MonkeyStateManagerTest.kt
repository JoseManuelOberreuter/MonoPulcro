package com.josem.monopulcro.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [28])
class MonkeyStateManagerTest {

  private val wednesday = LocalDate.of(2026, 7, 15) // ISO DOW 3

  private lateinit var context: Context

  @Before
  fun setUp() {
    context = ApplicationProvider.getApplicationContext()
  }

  private fun manager(
    gameDate: LocalDate = wednesday,
    loot: Int = 2,
  ): MonkeyStateManager {
    val prefs = context.getSharedPreferences(
      "monkey_test_${UUID.randomUUID()}",
      Context.MODE_PRIVATE,
    )
    return MonkeyStateManager(
      context = context,
      todayProvider = { gameDate },
      prefsOverride = prefs,
      chestLootProvider = { loot },
    )
  }

  private fun addTaskForDow(m: MonkeyStateManager, dow: Int, name: String = "Tarea"): Task {
    val task = Task(name = name, scheduledDays = listOf(dow))
    m.addTask(task)
    return task
  }

  @Test
  fun isCleanToday_noTasks_isFalse() {
    assertFalse(manager().isCleanToday)
  }

  @Test
  fun isCleanToday_taskPending_isFalse() {
    val m = manager()
    addTaskForDow(m, wednesday.dayOfWeek.value)
    assertFalse(m.isCleanToday)
  }

  @Test
  fun isCleanToday_allDone_isTrue() {
    val m = manager()
    val task = addTaskForDow(m, wednesday.dayOfWeek.value)
    m.toggleTask(task.id)
    assertTrue(m.isCleanToday)
  }

  @Test
  fun toggleTask_completeLast_incrementsStreakAndBananas() {
    val m = manager()
    val task = addTaskForDow(m, wednesday.dayOfWeek.value)
    assertTrue(m.toggleTask(task.id))
    assertEquals(1, m.streakCount)
    // +1 banana real por la tarea marcada + 2 del loot del cofre (loot fijo de prueba)
    assertEquals(3, m.bananas)
    assertEquals(2, m.lastRewardBananas)
  }

  @Test
  fun toggleTask_uncheckAfterComplete_revertsStreakAndBananas() {
    val m = manager()
    val task = addTaskForDow(m, wednesday.dayOfWeek.value)
    m.toggleTask(task.id)
    m.toggleTask(task.id)
    assertEquals(0, m.streakCount)
    assertEquals(0, m.bananas)
    assertEquals(0, m.lastRewardBananas)
  }

  @Test
  fun toggleTask_singleTaskOfSeveral_grantsOneBananaWithoutChest() {
    val m = manager()
    val dow = wednesday.dayOfWeek.value
    val task1 = addTaskForDow(m, dow, "Tarea 1")
    addTaskForDow(m, dow, "Tarea 2")

    assertFalse(m.toggleTask(task1.id))
    assertEquals(1, m.bananas)
    assertEquals(0, m.streakCount)
    assertEquals(0, m.lastRewardBananas)
  }

  @Test
  fun toggleTask_uncheckSingleTask_revertsItsBanana() {
    val m = manager()
    val dow = wednesday.dayOfWeek.value
    val task1 = addTaskForDow(m, dow, "Tarea 1")
    addTaskForDow(m, dow, "Tarea 2")

    m.toggleTask(task1.id)
    m.toggleTask(task1.id)
    assertEquals(0, m.bananas)
  }

  @Test
  fun tripleChestReward_succeedsOnceThenFails() {
    val m = manager()
    val task = addTaskForDow(m, wednesday.dayOfWeek.value)
    m.toggleTask(task.id)
    assertTrue(m.tripleChestReward())
    // 1 (tarea) + 2 (loot base) + 4 (extra x3 sobre el loot) = 7
    assertEquals(7, m.bananas)
    assertTrue(m.rewardDoubledToday)
    assertFalse(m.tripleChestReward())
  }

  @Test
  fun checkAndResetForNewDay_sameDay_isNoOp() {
    val m = manager()
    val task = addTaskForDow(m, wednesday.dayOfWeek.value)
    m.toggleTask(task.id)
    m.checkAndResetForNewDay()
    assertEquals(wednesday.toString(), m.lastResetDate)
    assertEquals(1, m.streakCount)
    assertEquals(3, m.bananas)
  }

  @Test
  fun checkAndResetForNewDay_missedYesterday_breaksStreak() {
    val tuesday = LocalDate.of(2026, 7, 14)
    val prefs = context.getSharedPreferences(
      "monkey_test_gap_${UUID.randomUUID()}",
      Context.MODE_PRIVATE,
    )
    prefs.edit()
      .putString(MonkeyStateManager.KEY_LAST_RESET, tuesday.toString())
      .putInt(MonkeyStateManager.KEY_STREAK, 5)
      .putBoolean(MonkeyStateManager.KEY_STREAK_COUNTED, false)
      .putBoolean(MonkeyStateManager.KEY_SHIELDS_INITIALIZED, true)
      .putInt(MonkeyStateManager.KEY_SHIELDS_COUNT, 0)
      .apply()
    val gapManager = MonkeyStateManager(
      context = context,
      todayProvider = { wednesday },
      prefsOverride = prefs,
      chestLootProvider = { 2 },
    )
    gapManager.addTask(Task(name = "Lavar", scheduledDays = listOf(tuesday.dayOfWeek.value)))
    gapManager.checkAndResetForNewDay()
    assertEquals(0, gapManager.streakCount)
    assertTrue(gapManager.streakBroken)
  }

  @Test
  fun checkAndResetForNewDay_missedYesterday_consumesShield() {
    val tuesday = LocalDate.of(2026, 7, 14)
    val prefs = context.getSharedPreferences(
      "monkey_test_shield_${UUID.randomUUID()}",
      Context.MODE_PRIVATE,
    )
    prefs.edit()
      .putString(MonkeyStateManager.KEY_LAST_RESET, tuesday.toString())
      .putInt(MonkeyStateManager.KEY_STREAK, 5)
      .putBoolean(MonkeyStateManager.KEY_STREAK_COUNTED, false)
      .putBoolean(MonkeyStateManager.KEY_SHIELDS_INITIALIZED, true)
      .putInt(MonkeyStateManager.KEY_SHIELDS_COUNT, 3)
      .apply()
    val m = MonkeyStateManager(
      context = context,
      todayProvider = { wednesday },
      prefsOverride = prefs,
      chestLootProvider = { 2 },
    )
    m.addTask(Task(name = "Lavar", scheduledDays = listOf(tuesday.dayOfWeek.value)))
    m.checkAndResetForNewDay()
    assertEquals(5, m.streakCount)
    assertEquals(2, m.shieldsCount)
    assertFalse(m.streakBroken)
  }

  @Test
  fun rewardDustCleaning_withMotes_grantsBanana() {
    val m = manager()
    m.debugAdvanceDustHours(2)
    assertTrue(m.dustCount > 0)
    assertEquals(1, m.rewardDustCleaning())
    assertEquals(1, m.bananas)
    assertEquals(0, m.dustCount)
  }

  @Test
  fun rewardDustCleaning_grantsOneBananaPerMote() {
    val m = manager()
    m.debugAdvanceDustHours(6)
    val motes = m.dustCount
    assertTrue(motes > 1)
    assertEquals(motes, m.rewardDustCleaning())
    assertEquals(motes, m.bananas)
    assertEquals(0, m.dustCount)
  }

  @Test
  fun toggleTask_updatesBestStreakAndTotalBananasEarned() {
    val m = manager()
    val task = addTaskForDow(m, wednesday.dayOfWeek.value)
    m.toggleTask(task.id)
    assertEquals(1, m.bestStreakCount)
    assertEquals(3, m.totalBananasEarned)
  }

  @Test
  fun bestStreakCount_survivesStreakBreak() {
    val tuesday = LocalDate.of(2026, 7, 14)
    val prefs = context.getSharedPreferences(
      "monkey_test_beststreak_${UUID.randomUUID()}",
      Context.MODE_PRIVATE,
    )
    prefs.edit()
      .putString(MonkeyStateManager.KEY_LAST_RESET, tuesday.toString())
      .putInt(MonkeyStateManager.KEY_STREAK, 5)
      .putInt(MonkeyStateManager.KEY_BEST_STREAK, 5)
      .putBoolean(MonkeyStateManager.KEY_STREAK_COUNTED, false)
      .putBoolean(MonkeyStateManager.KEY_SHIELDS_INITIALIZED, true)
      .putInt(MonkeyStateManager.KEY_SHIELDS_COUNT, 0)
      .apply()
    val m = MonkeyStateManager(
      context = context,
      todayProvider = { wednesday },
      prefsOverride = prefs,
      chestLootProvider = { 2 },
    )
    m.addTask(Task(name = "Lavar", scheduledDays = listOf(tuesday.dayOfWeek.value)))
    m.checkAndResetForNewDay()
    assertEquals(0, m.streakCount)
    assertEquals(5, m.bestStreakCount)
  }

  @Test
  fun rewardDustCleaning_updatesTotalMotesCleaned() {
    val m = manager()
    m.debugAdvanceDustHours(6)
    val motes = m.dustCount
    assertTrue(motes > 0)
    m.rewardDustCleaning()
    assertEquals(motes, m.totalMotesCleaned)
  }

  @Test
  fun checkAchievements_unlocksStreakMilestone_onlyOnce() {
    val prefs = context.getSharedPreferences(
      "monkey_test_ach_${UUID.randomUUID()}",
      Context.MODE_PRIVATE,
    )
    prefs.edit().putInt(MonkeyStateManager.KEY_BEST_STREAK, 3).apply()
    val m = MonkeyStateManager(
      context = context,
      todayProvider = { wednesday },
      prefsOverride = prefs,
      chestLootProvider = { 2 },
    )
    val unlocked = m.checkAchievements()
    assertTrue(unlocked.any { it.id == "streak_3" })
    assertTrue("streak_3" in m.unlockedAchievements)

    val again = m.checkAchievements()
    assertTrue(again.isEmpty())
  }

  @Test
  fun checkAchievements_unlocksAccessoryMilestones() {
    val m = manager()
    m.debugAddBananas(10)
    m.buyAccessory("glasses")
    val unlocked = m.checkAchievements()
    assertTrue(unlocked.any { it.id == "accessory_first" })
    assertFalse(unlocked.any { it.id == "accessory_all" })
  }

  @Test
  fun buyAura_deductsBananasAndOwnsIt() {
    val m = manager()
    m.debugAddBananas(500)
    assertTrue(m.buyAura("brillos"))
    assertEquals(0, m.bananas)
    assertTrue("brillos" in m.ownedAuras)
    assertFalse(m.buyAura("brillos"))
  }

  @Test
  fun buyAura_insufficientBananas_fails() {
    val m = manager()
    m.debugAddBananas(10)
    assertFalse(m.buyAura("brillos"))
    assertEquals(10, m.bananas)
    assertTrue(m.ownedAuras.isEmpty())
  }

  @Test
  fun useAura_equipsAndUnequips() {
    val m = manager()
    m.debugAddBananas(500)
    m.buyAura("brillos")
    m.useAura("brillos")
    assertEquals("brillos", m.equippedAura)
    m.useAura(null)
    assertEquals(null, m.equippedAura)
  }

  @Test
  fun grantPurchasedBananas_addsOncePerToken() {
    val m = manager()
    assertEquals(50, m.grantPurchasedBananas(50, "token-a"))
    assertEquals(50, m.bananas)
    assertEquals(null, m.grantPurchasedBananas(50, "token-a"))
    assertEquals(50, m.bananas)
    assertEquals(150, m.grantPurchasedBananas(150, "token-b"))
    assertEquals(200, m.bananas)
  }

  @Test
  fun grantPurchasedBananas_rejectsInvalidAmount() {
    val m = manager()
    assertEquals(null, m.grantPurchasedBananas(0, "token-z"))
    assertEquals(null, m.grantPurchasedBananas(-5, "token-z"))
    assertEquals(0, m.bananas)
  }
}

package com.voltwise.service

import android.app.*
import android.content.*
import android.content.pm.ServiceInfo
import android.os.*
import android.provider.Settings
import androidx.core.app.NotificationCompat
import androidx.room.withTransaction
import com.voltwise.data.local.AppDatabase
import com.voltwise.data.local.entity.*
import com.voltwise.ui.MainActivity
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel

class BatteryMonitorService : Service() {
 companion object {
  const val ACTION_START_SERVICE = "START_MONITORING"
  const val ACTION_STOP_SERVICE = "STOP_MONITORING"
  const val CHANNEL_ID = "voltwise_monitor_channel"
 }
 private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
 private val queue = Channel<BatteryReading>(Channel.UNLIMITED)
 private val database by lazy { AppDatabase.getDatabase(this) }
 private val dao by lazy { database.batteryDao() }
 private var wakeLock: PowerManager.WakeLock? = null
 @Volatile private var lastIntent: Intent? = null
 private var previous: BatteryReading? = null
 private var session: BatterySessionEntity? = null
 private var lastSaved = 0L
 private var lastSnapshotSaved = 0L
 private var lastDailySummaryDay = ""
 private var lastNightChargingAssist = 0L
 private var testStart: Long? = null
 private var started = false
 private lateinit var alerts: AlertManager
 private val receiver = object : BroadcastReceiver() {
  override fun onReceive(context: Context, intent: Intent) { lastIntent = intent; capture() }
 }
 private fun capture() {
  lastIntent?.let {
   val r = LiveBattery.read(it, getSystemService(BATTERY_SERVICE) as BatteryManager)
   LiveBattery.reading.value = r
   queue.trySend(r)
  }
 }
 override fun onCreate() {
  super.onCreate()
  alerts=AlertManager(this, com.voltwise.data.repository.BatteryRepository(dao))
  getSystemService(NotificationManager::class.java).createNotificationChannel(NotificationChannel(CHANNEL_ID,"Live battery monitoring",NotificationManager.IMPORTANCE_LOW))
 }
 override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
  if(intent?.action == ACTION_STOP_SERVICE) {
   queue.close(); return START_NOT_STICKY
  }
  if(Build.VERSION.SDK_INT >= 34) startForeground(1,notification("Waiting for battery reading"),ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
  else startForeground(1,notification("Waiting for battery reading"))
  if(!started) {
   started = true; LiveBattery.monitoring.value = true
   scope.launch {
    dao.getLastSession()?.takeIf { it.endTime == null }?.let { old ->
     val end = dao.samples(old.id).lastOrNull()?.timestamp ?: old.startTime
     dao.updateSession(old.copy(endTime=end,isFullyObserved=false))
    }
    for(r in queue) database.withTransaction { process(r) }
    session?.let { dao.updateSession(it.copy(endTime=lastSaved.takeIf { t -> t>0 } ?: it.startTime,isFullyObserved=false)) }
    stopSelf()
   }
   if(Build.VERSION.SDK_INT >= 33) registerReceiver(receiver,IntentFilter(Intent.ACTION_BATTERY_CHANGED),RECEIVER_NOT_EXPORTED)
   else registerReceiver(receiver,IntentFilter(Intent.ACTION_BATTERY_CHANGED))
   scope.launch { while(isActive) { delay(sampleIntervalMs()); capture() } }
  }
  return START_STICKY
 }
 private suspend fun event(r: BatteryReading, type: String, detail: String = type) { dao.insertEvent(TimelineEvent(timestamp=r.timestamp,type=type,detail=detail)) }
 private suspend fun process(r: BatteryReading) {
  saveSnapshot(r)
  maybeNotifyDailySummary(r.timestamp)
  maybeNotifyNightCharging(r)
  if (r.plugged) {
   val lock = wakeLock ?: (getSystemService(POWER_SERVICE) as PowerManager).newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,"Voltwise:ChargingSamples").also { it.setReferenceCounted(false); wakeLock=it }
   lock.acquire(60000)
  } else if (wakeLock?.isHeld == true) wakeLock?.release()
  if(session?.let { dao.getSessionById(it.id) == null } == true) session=null
  val old = previous
  if(r.plugged && old?.plugged != true) event(r,"Charger connected",r.source)
  if(!r.plugged && old?.plugged == true) event(r,"Charger disconnected")
  if(r.status == "Charging" && old?.status != "Charging") event(r,"Charging started")
  if(r.level == 100 && old?.level != 100) event(r,"Battery full")
  if((r.temperature ?: -999f) >= 45 && (old?.temperature ?: -999f) < 45) event(r,"High temperature")
  if(r.level != null && r.level <= 15 && (old?.level == null || old.level > 15)) event(r,"Low battery")
  val full = r.plugged && r.level == 100
  val wasFull = old?.plugged == true && old.level == 100
  if(full && !wasFull) event(r,"Overcharge started", "Time connected at 100%; battery charging is controlled by the phone")
  if(!full && wasFull) event(r,"Overcharge ended")
  if(r.plugged && session == null && r.level != null) {
   val s = BatterySessionEntity(startTime=r.timestamp,startLevel=r.level,isCharging=true,chargingSource=r.source,stabilityScore=-1f)
   session = s.copy(id=dao.insertSession(s)); lastSaved=0
  }
  session?.let { s ->
   if(r.timestamp-lastSaved >= sampleIntervalMs() || !r.plugged) {
    if(r.level != null) dao.insertObservation(BatteryObservationEntity(sessionId=s.id,timestamp=r.timestamp,level=r.level,temperature=r.temperature,voltage=r.rawVoltage?.takeIf { r.volts != null },currentNow=r.rawCurrent?.takeIf { r.currentMa != null }?.let { if(r.currentInMilliAmps) (it.toLong()*1000).takeIf { n -> n in Int.MIN_VALUE.toLong()..Int.MAX_VALUE.toLong() }?.toInt() else it },timeToFull=null))
    val samples=dao.samples(s.id)
    val currents=samples.mapNotNull { currentMilliAmps(it.currentNow) }
    val watts=samples.mapNotNull { o -> currentMilliAmps(o.currentNow)?.takeIf { (o.voltage ?: 0)>0 }?.let { it*o.voltage!!/1000000f } }
    val temps=samples.mapNotNull { it.temperature }.filter { it.isFinite() }
    val over=samples.zipWithNext().sumOf { (a,b) -> if(a.level==100) (b.timestamp-a.timestamp).coerceIn(0,30000) else 0L }
    val updated=s.copy(endTime=if(r.plugged) null else r.timestamp,endLevel=r.level ?: s.endLevel,avgTemperature=SessionMetrics.mean(temps),maxTemperature=temps.maxOrNull(),avgVoltage=SessionMetrics.mean(samples.filter { (it.voltage ?: 0)>0 }.map { it.voltage!!.toFloat() }),avgCurrent=SessionMetrics.mean(currents),avgWattage=SessionMetrics.mean(watts),overchargeDuration=over,stabilityScore=SessionMetrics.sessionStability(samples) ?: -1f)
    dao.updateSession(updated); session=if(r.plugged) updated else null; lastSaved=r.timestamp
    if(samples.size >= 2 && samples.last().timestamp-s.startTime >= 600000 && samples.last().level <= s.startLevel && old?.timestamp?.minus(s.startTime)?.let { it < 600000 } == true && r.level != 100) event(r,"Slow charging","Estimated: no battery percentage gain in 10 minutes")
    if(LiveBattery.requestTest && r.plugged) { testStart=r.timestamp; LiveBattery.requestTest=false }
    testStart?.let { start ->
     if(!r.plugged) { LiveBattery.test.value="Test interrupted: charger disconnected. Start a new test."; testStart=null }
     else if(r.timestamp-start >= 600000) {
      val result=SessionMetrics.quality(samples.filter { it.timestamp>=start })
      LiveBattery.test.value=result; event(r,"Charger test result",result); testStart=null
     } else LiveBattery.test.value="Collecting real samples: ${(r.timestamp-start)/1000} / 600 seconds"
    }
   }
  }
  r.level?.let { alerts.checkAlerts(it, r.temperature, r.status == "Charging") }
  previous=r
  val content="${r.status} · ${r.level?.toString() ?: "Unknown"}% · ${r.source}" + if(!r.plugged) " · Ready for charger" else " · ${r.currentMa?.let { "%.0f mA".format(it) } ?: "Current not supported"}"
 getSystemService(NotificationManager::class.java).notify(1,notification(content))
 }
 private fun sampleIntervalMs(): Long =
  getSharedPreferences("voltwise_prefs", MODE_PRIVATE).let { prefs ->
   when {
    prefs.getBoolean("standby_optimize_mode", false) && previous?.plugged == false -> 60000L
    prefs.getBoolean("power_saving_mode", false) -> 30000L
    else -> 5000L
   }
  }
 private fun maybeNotifyNightCharging(r: BatteryReading) {
  val prefs = getSharedPreferences("voltwise_prefs", MODE_PRIVATE)
  if(!prefs.getBoolean("night_charging_mode", false)) return
  val level = r.level ?: return
  val hour = java.util.Calendar.getInstance().apply { timeInMillis = r.timestamp }.get(java.util.Calendar.HOUR_OF_DAY)
  val night = hour >= 22 || hour < 6
  if(!night || !r.plugged || level < 80) return
  if(r.timestamp - lastNightChargingAssist < 2 * 60 * 60 * 1000L) return
  getSystemService(NotificationManager::class.java).notify(
   4,
   NotificationCompat.Builder(this, CHANNEL_ID)
    .setSmallIcon(android.R.drawable.ic_dialog_info)
    .setContentTitle("Night charging protection")
    .setContentText("Battery reached $level% overnight. Unplug near 80-90% to reduce aging.")
    .setContentIntent(PendingIntent.getActivity(this,4,Intent(this,MainActivity::class.java),PendingIntent.FLAG_IMMUTABLE))
    .setAutoCancel(true)
    .build()
  )
  lastNightChargingAssist = r.timestamp
 }
 private suspend fun maybeNotifyDailySummary(now: Long) {
  if(!getSharedPreferences("voltwise_prefs", MODE_PRIVATE).getBoolean("daily_report_enabled", false)) return
  val calendar = java.util.Calendar.getInstance().apply { timeInMillis = now }
  if(calendar.get(java.util.Calendar.HOUR_OF_DAY) < 21) return
  val dayKey = "%04d-%02d-%02d".format(
   calendar.get(java.util.Calendar.YEAR),
   calendar.get(java.util.Calendar.MONTH) + 1,
   calendar.get(java.util.Calendar.DAY_OF_MONTH)
  )
  if(dayKey == lastDailySummaryDay) return
  val start = calendar.apply {
   set(java.util.Calendar.HOUR_OF_DAY, 0)
   set(java.util.Calendar.MINUTE, 0)
   set(java.util.Calendar.SECOND, 0)
   set(java.util.Calendar.MILLISECOND, 0)
  }.timeInMillis
  val samples = dao.snapshotsSince(start).sortedBy { it.timestamp }
  if(samples.size < 6) return
  var gained = 0
  var lost = 0
  samples.zipWithNext().forEach { (a, b) ->
   val delta = b.level - a.level
   if(delta > 0 && b.plugged) gained += delta
   if(delta < 0 && !b.plugged) lost += -delta
  }
  if(gained == 0 && lost == 0) return
  getSystemService(NotificationManager::class.java).notify(
   2,
   NotificationCompat.Builder(this, CHANNEL_ID)
    .setSmallIcon(android.R.drawable.ic_dialog_info)
    .setContentTitle("Battery report ready")
    .setContentText("Charged +$gained%, used -$lost% today")
    .setContentIntent(PendingIntent.getActivity(this,0,Intent(this,MainActivity::class.java),PendingIntent.FLAG_IMMUTABLE))
    .setOnlyAlertOnce(true)
    .build()
  )
  lastDailySummaryDay = dayKey
 }
 private suspend fun saveSnapshot(r: BatteryReading) {
  val level = r.level ?: return
  val old = previous
  val shouldSave = r.timestamp - lastSnapshotSaved >= 60000 ||
   old == null ||
   old.level != level ||
   old.plugged != r.plugged ||
   old.status != r.status
  if (!shouldSave) return

  dao.insertSnapshot(
   BatterySnapshotEntity(
    timestamp = r.timestamp,
    level = level,
    plugged = r.plugged,
    status = r.status,
    source = r.source,
    temperature = r.temperature,
    voltage = r.rawVoltage?.takeIf { r.volts != null },
    currentNow = r.rawCurrent?.takeIf { r.currentMa != null }?.let {
     if (r.currentInMilliAmps) (it.toLong() * 1000).takeIf { n -> n in Int.MIN_VALUE.toLong()..Int.MAX_VALUE.toLong() }?.toInt() else it
    }
   )
  )
  lastSnapshotSaved = r.timestamp
 }
 private fun notification(text: String): Notification = NotificationCompat.Builder(this,CHANNEL_ID)
  .setSmallIcon(android.R.drawable.ic_lock_idle_charging).setContentTitle("Voltwise X").setContentText(text)
  .setContentIntent(PendingIntent.getActivity(this,0,Intent(this,MainActivity::class.java),PendingIntent.FLAG_IMMUTABLE))
  .setOnlyAlertOnce(true).setOngoing(true).build()
 override fun onBind(intent: Intent?): IBinder? = null
 override fun onDestroy() { if(started) unregisterReceiver(receiver); if(wakeLock?.isHeld==true) wakeLock?.release(); if(testStart!=null) LiveBattery.test.value="Test interrupted: monitoring stopped. Start a new test."; LiveBattery.requestTest=false; LiveBattery.monitoring.value=false; alerts.close(); scope.cancel(); super.onDestroy() }
}

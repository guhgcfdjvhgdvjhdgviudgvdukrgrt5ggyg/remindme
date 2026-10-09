import 'package:android_alarm_manager_plus/android_alarm_manager_plus.dart';

class AlarmService {
  static bool _initialized = false;

  static Future<void> init() async {
    if (_initialized) return;
    try {
      await AndroidAlarmManager.initialize();
      _initialized = true;
    } catch (e) {
      // Ignore
    }
  }

  static Future<void> scheduleOneShot({
    required int id,
    required DateTime dateTime,
  }) async {
    try {
      await AndroidAlarmManager.oneShotAt(
        dateTime,
        id,
        callback,
        allowWhileIdle: true,
        exact: true,
        wakeup: true,
        rescheduleOnReboot: true,
      );
    } catch (e) {
      // Ignore
    }
  }

  static Future<void> cancel(int id) async {
    try {
      await AndroidAlarmManager.cancel(id);
    } catch (e) {
      // Ignore
    }
  }
}

@pragma('vm:entry-point')
void callback(int id) async {
  // Placeholder - native side will handle full-screen activity
}

import 'package:flutter/services.dart';

class AlarmService {
  static const _channel = MethodChannel('com.remindme.app/alarm');

  static Future<void> init() async {
    try {
      await _channel.invokeMethod('init');
    } catch (_) {}
  }

  static Future<void> scheduleOneShot({
    required int id,
    required DateTime dateTime,
    required String title,
    required String ringtone,
    int snoozeMinutes = 10,
  }) async {
    try {
      await _channel.invokeMethod('scheduleAlarm', {
        'id': id,
        'timeMillis': dateTime.millisecondsSinceEpoch,
        'title': title,
        'ringtone': ringtone,
        'snoozeMinutes': snoozeMinutes,
      });
    } catch (_) {}
  }

  static Future<void> cancel(int id) async {
    try {
      await _channel.invokeMethod('cancelAlarm', {'id': id});
    } catch (_) {}
  }
}

import 'package:flutter/material.dart';
import '../database/db_helper.dart';
import '../models/reminder.dart';

class ReminderProvider with ChangeNotifier {
  List<Reminder> _reminders = [];
  List<Reminder> get reminders => _reminders;

  Future<void> loadReminders() async {
    _reminders = await DBHelper.instance.getReminders();
    notifyListeners();
  }

  Future<int> addReminder(Reminder r) async {
    final id = await DBHelper.instance.insertReminder(r);
    await loadReminders();
    return id;
  }

  Future<void> updateReminder(Reminder r) async {
    await DBHelper.instance.updateReminder(r);
    await loadReminders();
  }

  Future<void> deleteReminder(int id) async {
    await DBHelper.instance.deleteReminder(id);
    await loadReminders();
  }

  Future<void> deleteMany(List<int> ids) async {
    for (final id in ids) {
      await DBHelper.instance.deleteReminder(id);
    }
    await loadReminders();
  }

  // Upcoming shows only reminders due within the next 48 hours.
  // Alarms due later appear only in the "All" tab.
  // When an alarm's time passes (completes) it moves to the Completed tab.
  static const Duration upcomingWindow = Duration(hours: 48);

  List<Reminder> get upcoming {
    final now = DateTime.now();
    final cutoff = now.add(upcomingWindow);
    final list = _reminders.where((r) {
      if (r.isDone) return false;
      if (r.dateTime.isBefore(now)) return false;
      return r.dateTime.isBefore(cutoff);
    }).toList()
      ..sort((a, b) => a.dateTime.compareTo(b.dateTime));
    return list;
  }

  List<Reminder> get completed => _reminders.where((r) {
        if (r.isDone) return true;
        return !r.dateTime.isAfter(DateTime.now());
      }).toList();

  List<Reminder> get allReminders =>
      _reminders.toList()..sort((a, b) => a.dateTime.compareTo(b.dateTime));
}

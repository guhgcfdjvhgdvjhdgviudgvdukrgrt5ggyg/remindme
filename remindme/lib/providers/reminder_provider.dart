import 'package:flutter/material.dart';
import '../database/db_helper.dart';
import '../models/reminder.dart';

class ReminderProvider with ChangeNotifier {
  static const Duration completedRetention = Duration(days: 2);

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

  // A reminder whose time has passed is considered "completed" but stays in
  // the Upcoming list for `completedRetention` (2 days) before moving to the
  // Completed tab. Manually completed (swipe) reminders move immediately.
  List<Reminder> get upcoming => _reminders.where((r) {
        if (r.isDone) return false;
        return DateTime.now().difference(r.dateTime) < completedRetention;
      }).toList();

  List<Reminder> get completed => _reminders.where((r) {
        if (r.isDone) return true;
        return DateTime.now().difference(r.dateTime) >= completedRetention;
      }).toList();
}

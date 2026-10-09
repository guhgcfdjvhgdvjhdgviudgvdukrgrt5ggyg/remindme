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

  List<Reminder> get upcoming => _reminders
      .where((r) => !r.isDone && r.isActive)
      .toList();

  List<Reminder> get completed => _reminders
      .where((r) => r.isDone)
      .toList();
}

import 'dart:async';
import 'package:path/path.dart';
import 'package:sqflite/sqflite.dart';
import '../models/reminder.dart';

class DBHelper {
  static final DBHelper instance = DBHelper._internal();
  static Database? _db;
  DBHelper._internal();

  Future<Database> get db async {
    if (_db != null) return _db!;
    _db = await _initDB();
    return _db!;
  }

  Future<Database> _initDB() async {
    final dbPath = await getDatabasesPath();
    final path = join(dbPath, 'remindme.db');

    return await openDatabase(
      path,
      version: 2,
      onCreate: (Database db, int version) async {
        await db.execute('''
          CREATE TABLE reminders(
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            label TEXT,
            note TEXT,
            dateTime INTEGER,
            repeatType TEXT,
            repeatDays TEXT,
            snoozeMinutes INTEGER,
            ringtone TEXT,
            vibrate INTEGER,
            isActive INTEGER,
            isDone INTEGER
          )
        ''');
      },
      onUpgrade: (Database db, int oldVersion, int newVersion) async {
        if (oldVersion < 2) {
          await db.execute('ALTER TABLE reminders ADD COLUMN ringtone TEXT DEFAULT "Default"');
        }
      },
    );
  }

  Future<int> insertReminder(Reminder reminder) async {
    final d = await db;
    return await d.insert('reminders', reminder.toMap());
  }

  Future<List<Reminder>> getReminders() async {
    final d = await db;
    final List<Map<String, dynamic>> maps = await d.query(
      'reminders',
      orderBy: 'dateTime ASC',
    );
    return List.generate(maps.length, (i) => Reminder.fromMap(maps[i]));
  }

  Future<List<Reminder>> getActiveReminders() async {
    final d = await db;
    final List<Map<String, dynamic>> maps = await d.query(
      'reminders',
      where: 'isActive = 1 AND isDone = 0',
      orderBy: 'dateTime ASC',
    );
    return List.generate(maps.length, (i) => Reminder.fromMap(maps[i]));
  }

  Future<Reminder?> getReminder(int id) async {
    final d = await db;
    final maps = await d.query('reminders', where: 'id = ?', whereArgs: [id]);
    if (maps.isEmpty) return null;
    return Reminder.fromMap(maps.first);
  }

  Future<int> updateReminder(Reminder reminder) async {
    final d = await db;
    return await d.update(
      'reminders',
      reminder.toMap(),
      where: 'id = ?',
      whereArgs: [reminder.id],
    );
  }

  Future<int> deleteReminder(int id) async {
    final d = await db;
    return await d.delete('reminders', where: 'id = ?', whereArgs: [id]);
  }
}

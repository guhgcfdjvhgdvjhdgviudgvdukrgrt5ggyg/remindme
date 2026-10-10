import 'package:flutter/material.dart';
import 'package:shared_preferences/shared_preferences.dart';

class ThemeProvider extends ChangeNotifier {
  static const _key = 'themeMode';

  ThemeMode _mode;
  ThemeProvider(this._mode);

  ThemeMode get mode => _mode;

  String get label {
    switch (_mode) {
      case ThemeMode.light:
        return 'Light';
      case ThemeMode.dark:
        return 'Dark';
      default:
        return 'System default';
    }
  }

  static Future<ThemeProvider> load() async {
    final prefs = await SharedPreferences.getInstance();
    final stored = prefs.getString(_key);
    ThemeMode mode = ThemeMode.system;
    if (stored == 'light') {
      mode = ThemeMode.light;
    } else if (stored == 'dark') {
      mode = ThemeMode.dark;
    }
    return ThemeProvider(mode);
  }

  Future<void> setMode(ThemeMode mode) async {
    if (_mode == mode) return;
    _mode = mode;
    notifyListeners();
    final prefs = await SharedPreferences.getInstance();
    final value = mode == ThemeMode.light
        ? 'light'
        : mode == ThemeMode.dark
            ? 'dark'
            : 'system';
    await prefs.setString(_key, value);
  }
}

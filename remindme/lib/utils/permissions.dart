import 'package:permission_handler/permission_handler.dart';
import 'package:flutter_local_notifications/flutter_local_notifications.dart';

class PermissionUtils {
  static Future<bool> checkAll() async {
    final notif = await Permission.notification.request();
    return notif.isGranted;
  }
}

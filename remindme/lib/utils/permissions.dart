import 'package:permission_handler/permission_handler.dart';

class PermissionUtils {
  static Future<bool> checkAll() async {
    final notif = await Permission.notification.request();
    return notif.isGranted;
  }
}

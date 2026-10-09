import 'package:flutter/material.dart';
import 'package:permission_handler/permission_handler.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'onboarding_screen.dart';

class SettingsScreen extends StatelessWidget {
  const SettingsScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Settings'),
      ),
      body: ListView(
        padding: const EdgeInsets.all(16),
        children: [
          _buildSection(
            context,
            title: 'General',
            children: [
              _buildTile(
                context,
                icon: Icons.brightness_6_outlined,
                title: 'Theme',
                subtitle: 'Light / Dark (System default)',
                onTap: () {},
              ),
              _buildTile(
                context,
                icon: Icons.language_outlined,
                title: 'Language',
                subtitle: 'English',
                onTap: () {},
              ),
            ],
          ),
          _buildSection(
            context,
            title: 'Permissions',
            children: [
              _buildTile(
                context,
                icon: Icons.notifications_outlined,
                title: 'Notifications',
                subtitle: 'Check permission status',
                onTap: () async {
                  final status = await Permission.notification.status;
                  if (context.mounted) {
                    ScaffoldMessenger.of(context).showSnackBar(
                      SnackBar(
                        content: Text(
                          status.isGranted
                              ? 'Notifications are allowed'
                              : 'Notifications are not allowed',
                        ),
                        behavior: SnackBarBehavior.floating,
                        shape: RoundedRectangleBorder(
                          borderRadius: BorderRadius.circular(12),
                        ),
                      ),
                    );
                  }
                },
              ),
              _buildTile(
                context,
                icon: Icons.alarm_outlined,
                title: 'Exact Alarms',
                subtitle: 'Required for precise timing',
                onTap: () async {
                  await openAppSettings();
                },
              ),
              _buildTile(
                context,
                icon: Icons.battery_saver_outlined,
                title: 'Battery Optimization',
                subtitle: 'Set to Unrestricted',
                onTap: () async {
                  await openAppSettings();
                },
              ),
            ],
          ),
          _buildSection(
            context,
            title: 'App',
            children: [
              _buildTile(
                context,
                icon: Icons.info_outline,
                title: 'About RemindMe',
                subtitle: 'Version 1.0.0',
                onTap: () {
                  showAboutDialog(
                    context: context,
                    applicationName: 'RemindMe',
                    applicationVersion: '1.0.0',
                    applicationIcon: Container(
                      width: 48,
                      height: 48,
                      decoration: BoxDecoration(
                        color: const Color(0xFF6366F1),
                        borderRadius: BorderRadius.circular(12),
                      ),
                      child: const Icon(
                        Icons.alarm,
                        color: Colors.white,
                        size: 28,
                      ),
                    ),
                    children: const [
                      Text(
                        'RemindMe - Reminder & Alarm App\n\nAlarms ring on time whether your phone is locked or unlocked.',
                      ),
                    ],
                  );
                },
              ),
              _buildTile(
                context,
                icon: Icons.restart_alt_outlined,
                title: 'Show Onboarding Again',
                subtitle: 'Replay intro slides',
                onTap: () async {
                  final prefs = await SharedPreferences.getInstance();
                  await prefs.setBool('showOnboarding', true);
                  if (context.mounted) {
                    Navigator.of(context).pushReplacement(
                      MaterialPageRoute(
                        builder: (_) => const OnboardingScreen(),
                      ),
                    );
                  }
                },
              ),
            ],
          ),
        ],
      ),
    );
  }

  Widget _buildSection(
    BuildContext context, {
    required String title,
    required List<Widget> children,
  }) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Padding(
          padding: const EdgeInsets.only(left: 4, bottom: 8, top: 8),
          child: Text(
            title,
            style: TextStyle(
              fontSize: 13,
              fontWeight: FontWeight.w600,
              color: const Color(0xFF6366F1),
              letterSpacing: 0.5,
            ),
          ),
        ),
        Card(
          child: Column(
            children: children,
          ),
        ),
        const SizedBox(height: 8),
      ],
    );
  }

  Widget _buildTile(
    BuildContext context, {
    required IconData icon,
    required String title,
    required String subtitle,
    required VoidCallback onTap,
  }) {
    return ListTile(
      leading: Container(
        width: 40,
        height: 40,
        decoration: BoxDecoration(
          color: const Color(0xFF6366F1).withOpacity(0.1),
          borderRadius: BorderRadius.circular(10),
        ),
        child: Icon(icon, color: const Color(0xFF6366F1), size: 20),
      ),
      title: Text(title, style: const TextStyle(fontWeight: FontWeight.w500)),
      subtitle: Text(subtitle),
      trailing: const Icon(Icons.chevron_right, color: Colors.grey),
      onTap: onTap,
    );
  }
}

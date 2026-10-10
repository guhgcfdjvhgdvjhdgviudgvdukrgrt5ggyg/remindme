import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'package:intl/intl.dart';
import 'package:permission_handler/permission_handler.dart';
import '../providers/reminder_provider.dart';
import '../models/reminder.dart';
import '../services/notification_service.dart';
import '../services/alarm_service.dart';
import 'add_reminder_screen.dart';
import 'settings_screen.dart';

class HomeScreen extends StatefulWidget {
  const HomeScreen({super.key});

  @override
  State<HomeScreen> createState() => _HomeScreenState();
}

class _HomeScreenState extends State<HomeScreen>
    with SingleTickerProviderStateMixin {
  late TabController _tabController;
  final TextEditingController _searchController = TextEditingController();
  String _query = '';
  final Set<int> _selectedIds = {};

  bool get _selectionMode => _selectedIds.isNotEmpty;

  void _clearSelection() {
    setState(() => _selectedIds.clear());
  }

  void _toggleSelection(Reminder r) {
    final id = r.id;
    if (id == null) return;
    setState(() {
      if (_selectedIds.contains(id)) {
        _selectedIds.remove(id);
      } else {
        _selectedIds.add(id);
      }
    });
  }

  Future<void> _deleteSelected() async {
    final ids = _selectedIds.toList();
    setState(() => _selectedIds.clear());
    for (final id in ids) {
      await NotificationService.cancel(id);
      await AlarmService.cancel(id);
    }
    await context.read<ReminderProvider>().deleteMany(ids);
  }

  @override
  void initState() {
    super.initState();
    _tabController = TabController(length: 3, vsync: this);
    Future.microtask(() async {
      if (!mounted) return;
      await context.read<ReminderProvider>().loadReminders();
      if (mounted) await _rescheduleActiveAlarms();
    });
    WidgetsBinding.instance.addPostFrameCallback((_) {
      _requestNotificationPermission();
    });
  }

  // Re-arm every active, not-yet-due reminder when the app opens. This heals
  // alarms that were dropped while the app was closed/force-stopped so the
  // alarm keeps working in the background without opening the app.
  Future<void> _rescheduleActiveAlarms() async {
    final provider = context.read<ReminderProvider>();
    final now = DateTime.now();
    for (final r in provider.reminders) {
      if (r.id != null && r.isActive && !r.isDone && r.dateTime.isAfter(now)) {
        await AlarmService.scheduleOneShot(
          id: r.id!,
          dateTime: r.dateTime,
          title: r.label,
          ringtone: r.ringtone,
          snoozeMinutes: r.snoozeMinutes,
        );
      }
    }
  }

  Future<void> _requestNotificationPermission() async {
    final status = await Permission.notification.status;
    if (!status.isGranted) {
      await Permission.notification.request();
    }
  }

  @override
  void dispose() {
    _tabController.dispose();
    _searchController.dispose();
    super.dispose();
  }

  List<Reminder> _filter(List<Reminder> items) {
    if (_query.isEmpty) return items;
    return items
        .where((r) =>
            r.label.toLowerCase().contains(_query) ||
            r.note.toLowerCase().contains(_query))
        .toList();
  }

  Widget _buildSearchField() {
    return SizedBox(
      height: 46,
      child: TextField(
        controller: _searchController,
        onChanged: (v) => setState(() => _query = v.trim().toLowerCase()),
        textInputAction: TextInputAction.search,
        style: const TextStyle(fontSize: 14),
        decoration: InputDecoration(
          hintText: 'Search reminders or alarms...',
          hintStyle: TextStyle(fontSize: 14, color: Colors.grey.shade500),
          prefixIcon:
              Icon(Icons.search_rounded, size: 20, color: Colors.grey.shade500),
          prefixIconConstraints:
              const BoxConstraints(minWidth: 42, minHeight: 42),
          suffixIcon: _query.isEmpty
              ? null
              : IconButton(
                  icon: const Icon(Icons.close_rounded, size: 18),
                  splashRadius: 18,
                  onPressed: () {
                    _searchController.clear();
                    setState(() => _query = '');
                  },
                ),
          isDense: true,
          filled: true,
          contentPadding: const EdgeInsets.symmetric(horizontal: 12),
          border: OutlineInputBorder(
            borderRadius: BorderRadius.circular(14),
            borderSide: BorderSide.none,
          ),
          enabledBorder: OutlineInputBorder(
            borderRadius: BorderRadius.circular(14),
            borderSide: BorderSide.none,
          ),
          focusedBorder: OutlineInputBorder(
            borderRadius: BorderRadius.circular(14),
            borderSide: const BorderSide(color: Color(0xFF6366F1), width: 1.5),
          ),
        ),
      ),
    );
  }

  String _formatDateTime(DateTime dt) {
    final now = DateTime.now();
    final today = DateTime(now.year, now.month, now.day);
    final tomorrow = today.add(const Duration(days: 1));
    final date = DateTime(dt.year, dt.month, dt.day);

    final time = DateFormat('hh:mm a').format(dt);
    if (date == today) return 'Today, $time';
    if (date == tomorrow) return 'Tomorrow, $time';
    return '${DateFormat('dd MMM').format(dt)}, $time';
  }

  Widget _buildTab(IconData icon, String label, int? count) {
    return Tab(
      child: FittedBox(
        fit: BoxFit.scaleDown,
        child: Row(
          mainAxisSize: MainAxisSize.min,
          children: [
            Icon(icon, size: 18),
            const SizedBox(width: 6),
            Text(count == null ? label : '$label ($count)'),
          ],
        ),
      ),
    );
  }

  Future<void> _scheduleReminder(Reminder r) async {
    if (r.id != null &&
        r.isActive &&
        !r.isDone &&
        r.dateTime.isAfter(DateTime.now())) {
      await AlarmService.scheduleOneShot(
        id: r.id!,
        dateTime: r.dateTime,
        title: r.label,
        ringtone: r.ringtone,
        snoozeMinutes: r.snoozeMinutes,
      );
    }
  }

  Future<void> _cancelReminder(Reminder r) async {
    if (r.id != null) {
      await NotificationService.cancel(r.id!);
      await AlarmService.cancel(r.id!);
    }
  }

  @override
  Widget build(BuildContext context) {
    final provider = context.watch<ReminderProvider>();
    final all = [...provider.upcoming, ...provider.completed];
    final upcomingList = _filter(provider.upcoming);
    final completedList = _filter(provider.completed);
    final allList = _filter(all);

    return Scaffold(
      body: NestedScrollView(
        headerSliverBuilder: (context, innerBoxIsScrolled) {
          return [
            SliverAppBar(
              expandedHeight: 210,
              floating: true,
              pinned: true,
              elevation: 0,
              scrolledUnderElevation: 0,
              flexibleSpace: FlexibleSpaceBar(
                titlePadding: const EdgeInsets.only(left: 20, bottom: 112),
                title: Text(
                  'RemindMe',
                  style: TextStyle(
                    fontWeight: FontWeight.bold,
                    fontSize: 28,
                    color: Theme.of(context).colorScheme.onSurface,
                  ),
                ),
              ),
              actions: [
                IconButton(
                  icon: const Icon(Icons.settings_outlined),
                  onPressed: () {
                    Navigator.push(
                      context,
                      MaterialPageRoute(
                          builder: (_) => const SettingsScreen()),
                    );
                  },
                ),
              ],
              bottom: PreferredSize(
                preferredSize: const Size.fromHeight(104),
                child: Column(
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    Padding(
                      padding: const EdgeInsets.fromLTRB(16, 4, 16, 6),
                      child: _buildSearchField(),
                    ),
                    TabBar(
                      controller: _tabController,
                      labelPadding: const EdgeInsets.symmetric(horizontal: 4),
                      indicatorColor: const Color(0xFF6366F1),
                      labelColor: const Color(0xFF6366F1),
                      unselectedLabelColor: Colors.grey.shade600,
                      labelStyle: const TextStyle(
                        fontSize: 13,
                        fontWeight: FontWeight.w600,
                      ),
                      unselectedLabelStyle: const TextStyle(
                        fontSize: 13,
                        fontWeight: FontWeight.w500,
                      ),
                      tabs: [
                        _buildTab(Icons.upcoming_rounded, 'Upcoming',
                            provider.upcoming.length),
                        _buildTab(Icons.check_circle_outline, 'Completed',
                            provider.completed.length),
                        _buildTab(Icons.history_rounded, 'All', all.length),
                      ],
                    ),
                  ],
                ),
              ),
            ),
          ];
        },
        body: TabBarView(
          controller: _tabController,
          children: [
            _buildReminderList(upcomingList, isUpcoming: true),
            _buildReminderList(completedList, isCompleted: true),
            _buildAllReminders(allList),
          ],
        ),
      ),
      bottomNavigationBar: _selectionMode ? _buildSelectionBar() : null,
      floatingActionButton: _selectionMode
          ? null
          : FloatingActionButton.extended(
        onPressed: () async {
          await Navigator.push(
            context,
            PageRouteBuilder(
              pageBuilder: (_, __, ___) => const AddReminderScreen(),
              transitionsBuilder: (_, animation, __, child) {
                return SlideTransition(
                  position: Tween<Offset>(
                    begin: const Offset(0, 1),
                    end: Offset.zero,
                  ).animate(CurvedAnimation(
                    parent: animation,
                    curve: Curves.easeOutCubic,
                  )),
                  child: child,
                );
              },
            ),
          );
          if (context.mounted) context.read<ReminderProvider>().loadReminders();
        },
        icon: const Icon(Icons.add_rounded),
        label: const Text('New Reminder'),
      ),
    );
  }

  Widget _buildSelectionBar() {
    return Material(
      elevation: 12,
      color: Theme.of(context).colorScheme.surface,
      child: SafeArea(
        child: Padding(
          padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
          child: Row(
            children: [
              IconButton(
                icon: const Icon(Icons.close_rounded),
                tooltip: 'Cancel selection',
                onPressed: _clearSelection,
              ),
              const SizedBox(width: 4),
              Expanded(
                child: Text(
                  '${_selectedIds.length} selected',
                  style: const TextStyle(
                    fontWeight: FontWeight.w600,
                    fontSize: 15,
                  ),
                ),
              ),
              IconButton(
                icon: const Icon(Icons.delete_outline, color: Colors.red),
                tooltip: 'Delete selected',
                onPressed: _selectedIds.isEmpty
                    ? null
                    : () => _deleteSelected(),
              ),
            ],
          ),
        ),
      ),
    );
  }

  Widget _buildReminderList(List<Reminder> items,
      {bool isUpcoming = false, bool isCompleted = false}) {
    if (items.isEmpty) {
      return _query.isNotEmpty
          ? _buildNoResults()
          : _buildEmptyState(isUpcoming, isCompleted);
    }

    return ListView.builder(
      padding: const EdgeInsets.fromLTRB(16, 16, 16, 100),
      itemCount: items.length,
      itemBuilder: (context, i) {
        final r = items[i];
        return Dismissible(
          key: ValueKey('reminder_${r.id}_${r.label}_${r.dateTime.millisecondsSinceEpoch}'),
          direction: _selectionMode
              ? DismissDirection.none
              : isCompleted
                  ? DismissDirection.endToStart
                  : DismissDirection.horizontal,
          background: Container(
            alignment: Alignment.centerLeft,
            padding: const EdgeInsets.only(left: 24),
            margin: const EdgeInsets.only(bottom: 12),
            decoration: BoxDecoration(
              color: Colors.green,
              borderRadius: BorderRadius.circular(20),
            ),
            child: const Icon(Icons.check, color: Colors.white, size: 28),
          ),
          secondaryBackground: Container(
            alignment: Alignment.centerRight,
            padding: const EdgeInsets.only(right: 24),
            margin: const EdgeInsets.only(bottom: 12),
            decoration: BoxDecoration(
              color: Colors.red,
              borderRadius: BorderRadius.circular(20),
            ),
            child: const Icon(Icons.delete, color: Colors.white, size: 28),
          ),
          confirmDismiss: (direction) async {
            if (_selectionMode) return false;
            if ((isCompleted || _selectedIds.contains(r.id)) &&
                direction == DismissDirection.endToStart) {
              return true;
            }
            if (direction == DismissDirection.startToEnd) {
              await context.read<ReminderProvider>().updateReminder(
                    r.copyWith(isDone: true, isActive: false),
                  );
              await _cancelReminder(r);
              return false;
            }
            if (direction == DismissDirection.endToStart) {
              return true;
            }
            return false;
          },
          onDismissed: (direction) {
            if (direction == DismissDirection.endToStart && r.id != null) {
              context.read<ReminderProvider>().deleteReminder(r.id!);
            }
          },
          child: _buildReminderCard(r, isUpcoming, isCompleted),
        );
      },
    );
  }

  Widget _buildAllReminders(List<Reminder> all) {
    if (all.isEmpty) {
      return _query.isNotEmpty
          ? _buildNoResults()
          : _buildEmptyState(true, false);
    }
    return ListView.builder(
      padding: const EdgeInsets.fromLTRB(16, 16, 16, 100),
      itemCount: all.length,
      itemBuilder: (context, i) {
        final r = all[i];
        return Padding(
          padding: const EdgeInsets.only(bottom: 12),
          child: _buildReminderCard(r, false, r.isDone),
        );
      },
    );
  }

  Widget _buildReminderCard(Reminder r, bool isUpcoming, bool isCompleted) {
    final isPast = r.dateTime.isBefore(DateTime.now()) && !r.isDone;
    final isInactive = !r.isActive && !r.isDone;
    final id = r.id;
    final selected = id != null && _selectedIds.contains(id);
    final color = isCompleted
        ? Colors.green
        : isInactive
            ? Colors.grey
            : isPast
                ? Colors.orange
                : const Color(0xFF6366F1);

    return Card(
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(20),
        side: selected
            ? const BorderSide(color: Color(0xFF6366F1), width: 2)
            : BorderSide.none,
      ),
      color:
          selected ? const Color(0xFF6366F1).withValues(alpha: 0.06) : null,
      child: InkWell(
        onTap: () async {
          if (_selectionMode) {
            _toggleSelection(r);
            return;
          }
          await Navigator.push(
            context,
            MaterialPageRoute(
              builder: (_) => AddReminderScreen(reminder: r),
            ),
          );
          if (context.mounted) context.read<ReminderProvider>().loadReminders();
        },
        onLongPress: () => _toggleSelection(r),
        borderRadius: BorderRadius.circular(20),
        child: Padding(
          padding: const EdgeInsets.all(16),
          child: Row(
            children: [
              if (selected)
                Container(
                  width: 52,
                  height: 52,
                  decoration: BoxDecoration(
                    color: const Color(0xFF6366F1).withValues(alpha: 0.12),
                    borderRadius: BorderRadius.circular(14),
                  ),
                  child: const Icon(
                    Icons.check_circle,
                    color: Color(0xFF6366F1),
                    size: 26,
                  ),
                )
              else
                Container(
                  width: 52,
                  height: 52,
                  decoration: BoxDecoration(
                    color: color.withValues(alpha: 0.12),
                    borderRadius: BorderRadius.circular(14),
                  ),
                  child: Icon(
                    isCompleted
                        ? Icons.check_circle
                        : isInactive
                            ? Icons.alarm_off
                            : isPast
                                ? Icons.warning_rounded
                                : Icons.alarm,
                    color: color,
                    size: 26,
                  ),
                ),
              const SizedBox(width: 16),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      r.label,
                      maxLines: 1,
                      overflow: TextOverflow.ellipsis,
                      style: TextStyle(
                        fontWeight: FontWeight.w600,
                        fontSize: 16,
                        decoration:
                            isCompleted ? TextDecoration.lineThrough : null,
                        color: (isCompleted || isInactive)
                            ? Colors.grey
                            : null,
                      ),
                    ),
                    const SizedBox(height: 4),
                    Row(
                      children: [
                        Icon(
                          Icons.access_time,
                          size: 14,
                          color: Colors.grey.shade500,
                        ),
                        const SizedBox(width: 4),
                        Text(
                          _formatDateTime(r.dateTime),
                          style: TextStyle(
                            color: Colors.grey.shade600,
                            fontSize: 13,
                          ),
                        ),
                        if (r.repeatType != RepeatType.none) ...[
                          const SizedBox(width: 8),
                          Icon(
                            Icons.repeat,
                            size: 14,
                            color: Colors.grey.shade500,
                          ),
                          const SizedBox(width: 2),
                          Text(
                            r.repeatType == RepeatType.daily
                                ? 'Daily'
                                : r.repeatType == RepeatType.weekly
                                    ? 'Weekly'
                                    : 'Monthly',
                            style: TextStyle(
                              color: Colors.grey.shade600,
                              fontSize: 12,
                            ),
                          ),
                        ],
                        if (isInactive) ...[
                          const SizedBox(width: 8),
                          Icon(
                            Icons.notifications_off_outlined,
                            size: 14,
                            color: Colors.grey.shade500,
                          ),
                          const SizedBox(width: 2),
                          Text(
                            'Alarm off',
                            style: TextStyle(
                              color: Colors.grey.shade600,
                              fontSize: 12,
                            ),
                          ),
                        ],
                      ],
                    ),
                    if (r.note.isNotEmpty) ...[
                      const SizedBox(height: 4),
                      Text(
                        r.note,
                        maxLines: 1,
                        overflow: TextOverflow.ellipsis,
                        style: TextStyle(
                          color: Colors.grey.shade500,
                          fontSize: 12,
                        ),
                      ),
                    ],
                  ],
                ),
              ),
              if (isUpcoming && !_selectionMode)
                Switch(
                  value: r.isActive,
                  onChanged: (v) async {
                    final updated = r.copyWith(isActive: v);
                    await context
                        .read<ReminderProvider>()
                        .updateReminder(updated);
                    if (v) {
                      await _scheduleReminder(updated);
                    } else {
                      await _cancelReminder(updated);
                    }
                  },
                ),
              if (isCompleted && !_selectionMode)
                IconButton(
                  icon: const Icon(Icons.restore, color: Colors.grey),
                  onPressed: () async {
                    final updated = r.copyWith(isDone: false, isActive: true);
                    await context
                        .read<ReminderProvider>()
                        .updateReminder(updated);
                    await _scheduleReminder(updated);
                  },
                ),
            ],
          ),
        ),
      ),
    );
  }

  Widget _buildNoResults() {
    return Center(
      child: Column(
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          Icon(Icons.search_off_rounded, size: 56, color: Colors.grey.shade400),
          const SizedBox(height: 16),
          Padding(
            padding: const EdgeInsets.symmetric(horizontal: 32),
            child: Text(
              'No results for "${_searchController.text}"',
              textAlign: TextAlign.center,
              style: Theme.of(context)
                  .textTheme
                  .titleMedium
                  ?.copyWith(fontWeight: FontWeight.w600),
            ),
          ),
          const SizedBox(height: 6),
          Text(
            'Try a different keyword',
            style: TextStyle(color: Colors.grey.shade500),
          ),
        ],
      ),
    );
  }

  Widget _buildEmptyState(bool isUpcoming, bool isCompleted) {
    return Center(
      child: Column(
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          Container(
            width: 120,
            height: 120,
            decoration: BoxDecoration(
              color: const Color(0xFF6366F1).withValues(alpha: 0.1),
              shape: BoxShape.circle,
            ),
            child: Icon(
              isCompleted
                  ? Icons.task_alt
                  : isUpcoming
                      ? Icons.alarm_off_rounded
                      : Icons.inbox_rounded,
              size: 56,
              color: const Color(0xFF6366F1).withValues(alpha: 0.5),
            ),
          ),
          const SizedBox(height: 24),
          Text(
            isCompleted
                ? 'No completed reminders'
                : isUpcoming
                    ? 'No reminders yet'
                    : 'Nothing found',
            style: Theme.of(context).textTheme.titleLarge?.copyWith(
                  fontWeight: FontWeight.w600,
                ),
          ),
          const SizedBox(height: 8),
          Text(
            isCompleted
                ? 'Completed reminders will appear here'
                : 'Tap + to create a new reminder',
            style: TextStyle(
              color: Colors.grey.shade500,
            ),
          ),
        ],
      ),
    );
  }
}

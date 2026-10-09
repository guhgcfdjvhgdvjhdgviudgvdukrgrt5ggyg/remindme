import 'package:flutter/material.dart';
import 'package:audioplayers/audioplayers.dart';
import '../models/reminder.dart';

class RingtonePickerSheet extends StatefulWidget {
  final String selectedRingtone;
  const RingtonePickerSheet({super.key, required this.selectedRingtone});

  @override
  State<RingtonePickerSheet> createState() => _RingtonePickerSheetState();
}

class _RingtonePickerSheetState extends State<RingtonePickerSheet> {
  late String _selected;
  final _player = AudioPlayer();
  String? _playing;

  @override
  void initState() {
    super.initState();
    _selected = widget.selectedRingtone;
  }

  @override
  void dispose() {
    _player.stop();
    _player.dispose();
    super.dispose();
  }

  String _assetFor(String name) {
    final n = name.toLowerCase();
    switch (n) {
      case 'default': return 'audio/defaulttone.mp3';
      case 'alarm': return 'audio/alarm.mp3';
      case 'bell': return 'audio/bell.mp3';
      case 'birds': return 'audio/birds.mp3';
      case 'chime': return 'audio/chime.mp3';
      case 'digital': return 'audio/digital.mp3';
      case 'ding': return 'audio/ding.mp3';
      case 'drop': return 'audio/drop.mp3';
      case 'harmony': return 'audio/harmony.mp3';
      case 'marimba': return 'audio/marimba.mp3';
      case 'melody': return 'audio/melody.mp3';
      case 'morning': return 'audio/morning.mp3';
      case 'music': return 'audio/music.mp3';
      case 'piano': return 'audio/piano.mp3';
      case 'pop': return 'audio/pop.mp3';
      case 'radar': return 'audio/radar.mp3';
      case 'signal': return 'audio/signal.mp3';
      case 'siren': return 'audio/siren.mp3';
      case 'star': return 'audio/star.mp3';
      case 'sunrise': return 'audio/sunrise.mp3';
      case 'twinkle': return 'audio/twinkle.mp3';
      case 'whistle': return 'audio/whistle.mp3';
      default: return 'audio/defaulttone.mp3';
    }
  }

  Future<void> _play(String name) async {
    if (_playing == name) {
      await _player.stop();
      setState(() => _playing = null);
      return;
    }
    await _player.stop();
    setState(() => _playing = name);
    try {
      await _player.play(AssetSource(_assetFor(name)));
      _player.onPlayerComplete.listen((_) {
        if (mounted && _playing == name) {
          setState(() => _playing = null);
        }
      });
    } catch (e) {
      setState(() => _playing = null);
    }
  }

  @override
  Widget build(BuildContext context) {
    return DraggableScrollableSheet(
      initialChildSize: 0.6,
      minChildSize: 0.4,
      maxChildSize: 0.9,
      expand: false,
      builder: (context, scrollController) {
        return Column(
          children: [
            const SizedBox(height: 12),
            Container(
              width: 40,
              height: 4,
              decoration: BoxDecoration(
                color: Colors.grey.shade300,
                borderRadius: BorderRadius.circular(2),
              ),
            ),
            const SizedBox(height: 16),
            const Text('Select Ringtone',
                style: TextStyle(fontSize: 18, fontWeight: FontWeight.w600)),
            const SizedBox(height: 12),
            Expanded(
              child: ListView.builder(
                controller: scrollController,
                itemCount: availableRingtones.length,
                itemBuilder: (context, index) {
                  final ringtone = availableRingtones[index];
                  final isSelected = _selected == ringtone.name;
                  final isPlaying = _playing == ringtone.name;
                  return ListTile(
                    leading: Container(
                      width: 40,
                      height: 40,
                      decoration: BoxDecoration(
                        color: isSelected
                            ? const Color(0xFF6366F1).withOpacity(0.1)
                            : Colors.grey.shade100,
                        borderRadius: BorderRadius.circular(10),
                      ),
                      child: IconButton(
                        icon: Icon(
                          isPlaying ? Icons.stop_rounded : Icons.play_arrow_rounded,
                          color: isSelected
                              ? const Color(0xFF6366F1)
                              : Colors.grey,
                          size: 20,
                        ),
                        onPressed: () => _play(ringtone.name),
                      ),
                    ),
                    title: Text(ringtone.name,
                        style: TextStyle(
                            fontWeight: isSelected
                                ? FontWeight.w600
                                : FontWeight.normal)),
                    trailing: isSelected
                        ? const Icon(Icons.check_circle,
                            color: Color(0xFF6366F1))
                        : null,
                    onTap: () {
                      Navigator.pop(context, ringtone.name);
                    },
                  );
                },
              ),
            ),
          ],
        );
      },
    );
  }
}

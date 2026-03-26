package com.chunktasks.sound;

import com.chunktasks.ChunkTasksConfig;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.client.audio.AudioPlayer;

@Singleton
@Slf4j
public class SoundEngine {
    @Inject
    private ChunkTasksConfig config;

    @Inject
    private AudioPlayer audioPlayer;

    public void playClip(Sound sound, int gameVolume) {
        float gain = 20f * (float) Math.log10(gameVolume / 100f);

        try {
            audioPlayer.play(SoundFileManager.getSoundStream(sound), gain);
        } catch (Exception e) {
            log.warn("Failed to load sound {}", sound, e);
        }
    }
}

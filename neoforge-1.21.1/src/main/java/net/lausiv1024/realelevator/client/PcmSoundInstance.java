package net.lausiv1024.realelevator.client;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

import org.lwjgl.BufferUtils;

import net.lausiv1024.realelevator.RealElevator;
import net.minecraft.client.resources.sounds.AbstractSoundInstance;
import net.minecraft.client.resources.sounds.Sound;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.AudioStream;
import net.minecraft.client.sounds.SoundBufferLibrary;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;

/**
 * WAV のデータをそのまま鳴らす音。サウンドイベント {@code realelevator:announce.voicevox}（ストリーミング）として鳴らし、
 * 音声のファイルの代わりに、持っている PCM を流す（{@link #getStream}）。
 */
final class PcmSoundInstance extends AbstractSoundInstance {
    static final ResourceLocation EVENT = ResourceLocation.fromNamespaceAndPath(RealElevator.MODID, "announce.voicevox");

    private final AudioFormat format;
    private final byte[] pcm;

    private PcmSoundInstance(AudioFormat format, byte[] pcm, double x, double y, double z) {
        super(EVENT, SoundSource.VOICE, RandomSource.create());
        this.format = format;
        this.pcm = pcm;
        this.x = x;
        this.y = y;
        this.z = z;
        this.attenuation = SoundInstance.Attenuation.LINEAR;
    }

    /** WAV のデータ wav を、位置 (x, y, z) で鳴らす音にする。読めなければ例外。 */
    static PcmSoundInstance fromWav(byte[] wav, double x, double y, double z) throws IOException {
        try (AudioInputStream in = AudioSystem.getAudioInputStream(new ByteArrayInputStream(wav))) {
            return new PcmSoundInstance(in.getFormat(), in.readAllBytes(), x, y, z);
        } catch (UnsupportedAudioFileException e) {
            throw new IOException("not a WAV file", e);
        }
    }

    /** 鳴る長さ [秒]。 */
    double seconds() {
        return pcm.length / (double) (format.getFrameSize() * format.getFrameRate());
    }

    @Override
    public CompletableFuture<AudioStream> getStream(SoundBufferLibrary soundBuffers, Sound sound, boolean looping) {
        return CompletableFuture.completedFuture(new PcmStream(format, pcm));
    }

    /** PCM を少しずつ渡すストリーム。OpenAL に渡すので、直接バッファで返す。 */
    private static final class PcmStream implements AudioStream {
        private final AudioFormat format;
        private final byte[] pcm;
        private int position;

        PcmStream(AudioFormat format, byte[] pcm) {
            this.format = format;
            this.pcm = pcm;
        }

        @Override
        public AudioFormat getFormat() {
            return format;
        }

        @Override
        public ByteBuffer read(int size) {
            // フレームの途中で切らない
            int frame = Math.max(1, format.getFrameSize());
            int length = Math.min(size - size % frame, pcm.length - position);
            ByteBuffer buffer = BufferUtils.createByteBuffer(Math.max(0, length));
            if (length > 0) {
                buffer.put(pcm, position, length);
                position += length;
            }
            buffer.flip();
            return buffer;
        }

        @Override
        public void close() {
            position = pcm.length;
        }
    }
}

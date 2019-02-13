import javax.sound.sampled.*;
import java.io.ByteArrayInputStream;

public class AudioManager {
    /**
     * USED THE FOLLOWING STACKOVERFLOW PAGE TO OUTPUT AUDIO
     * https://stackoverflow.com/questions/28122097/live-audio-stream-java
     */

    private AudioInputStream audioInputStream;
    private AudioFormat format;
    private static final float SAMPLE_RATE = 44100f;
    private static final int SAMPLE_SIZE_IN_BITS = 16;
    private static final int NUM_CHANNELS = 2;
    private static final int FRAME_SIZE = 4;
    private static final int FRAME_RATE = 44100;
    private static DataLine.Info dataLineInfo;
    private static SourceDataLine sourceDataLine;

    public void start() throws Exception {
        format = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, SAMPLE_RATE, SAMPLE_SIZE_IN_BITS, NUM_CHANNELS, FRAME_SIZE, FRAME_RATE, false);
        dataLineInfo = new DataLine.Info(SourceDataLine.class, format);
        sourceDataLine = (SourceDataLine) AudioSystem.getLine(dataLineInfo);
        sourceDataLine.open(format);
        sourceDataLine.start();
    }

    public void playSound(byte[] soundData) {
        try
        {
            sourceDataLine.write(soundData, 0, soundData.length);
        } catch (Exception e) {
            System.out.println("Not working in speakers...");
            e.printStackTrace();
        }
    }

    public void end() {
        sourceDataLine.drain();
        sourceDataLine.close();
    }

    public void setAudioInputStream(AudioInputStream audioInputStream) {
        this.audioInputStream = audioInputStream;
    }

    public AudioFormat getFormat() {
        return format;
    }

    public void setFormat(AudioFormat format) {
        this.format = format;
    }
}

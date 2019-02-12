import javax.sound.sampled.*;
import java.io.ByteArrayInputStream;

public class AudioManager {
    /**
     * USED THE FOLLOWING STACKOVERFLOW PAGE TO OUTPUT AUDIO
     * https://stackoverflow.com/questions/28122097/live-audio-stream-java
     */

    private AudioInputStream audioInputStream;
    private AudioFormat format;
    private static int sampleRate = 44100;
    private static DataLine.Info dataLineInfo;
    private static SourceDataLine sourceDataLine;

    public void start() throws Exception {
        format = new AudioFormat(sampleRate, 16, 2, true, false);
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

    public AudioInputStream getAudioInputStream() {
        return audioInputStream;
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

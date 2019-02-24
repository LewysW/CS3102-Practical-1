import javax.sound.sampled.*;
import java.io.ByteArrayInputStream;

public class AudioManager {
    /**
     * USED THE FOLLOWING STACKOVERFLOW PAGE TO OUTPUT AUDIO
     * https://stackoverflow.com/questions/28122097/live-audio-stream-java
     */


    /**
     * Uses the audio format settings discussed in one of the emails regarding the practical
     */
    private AudioInputStream audioInputStream;
    private AudioFormat format;
    private static final float SAMPLE_RATE = 44100f;
    private static final int SAMPLE_SIZE_IN_BITS = 16;
    private static final int NUM_CHANNELS = 2;
    private static final int FRAME_SIZE = 4;
    private static final int FRAME_RATE = 44100;
    private static SourceDataLine sourceDataLine;

    /**
     * Initialises data structures required to write to a source data line for audio playback
     * @throws Exception
     */
    public void start() throws Exception {
        DataLine.Info dataLineInfo;
        format = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, SAMPLE_RATE, SAMPLE_SIZE_IN_BITS, NUM_CHANNELS, FRAME_SIZE, FRAME_RATE, false);
        dataLineInfo = new DataLine.Info(SourceDataLine.class, format);
        sourceDataLine = (SourceDataLine) AudioSystem.getLine(dataLineInfo);
        sourceDataLine.open(format);
        sourceDataLine.start();
    }

    /**
     * Plays a sound by writing bytes to a source data line
     * @param soundData
     */
    public void playSound(byte[] soundData) {
        try {
            sourceDataLine.write(soundData, 0, soundData.length);
        } catch (Exception e) {
            System.out.println("Not working in speakers...");
            e.printStackTrace();
        }
    }

    /**
     * Closes the source data line and drains any remaining data
     */
    public void end() {
        sourceDataLine.drain();
        sourceDataLine.close();
    }

    /**
     * Setter for audio input stream
     * @param audioInputStream - to assign
     */
    public void setAudioInputStream(AudioInputStream audioInputStream) {
        this.audioInputStream = audioInputStream;
    }

    /**
     * Getter for audio format
     * @return - audio format
     */
    public AudioFormat getFormat() {
        return format;
    }

}

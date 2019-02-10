import javax.sound.sampled.*;
import java.io.ByteArrayInputStream;

public class AudioManager {
    public void playSound(byte[] soundData) throws Exception {
        ByteArrayInputStream stream = new ByteArrayInputStream(soundData);
        Clip clip = AudioSystem.getClip();
        AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(stream);
        clip.open(audioInputStream);
        clip.start();
    }
}

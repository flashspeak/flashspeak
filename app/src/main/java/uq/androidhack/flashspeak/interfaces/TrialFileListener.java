package uq.androidhack.flashspeak.interfaces;

import android.graphics.Bitmap;

import java.net.URI;

/**
 * Created by uqdangus on 29/01/2015.
 */
public interface TrialFileListener {

    public void onRecording(URI uri);

    public void onFinishProcessing(String b);
}

package uq.androidhack.flashspeak;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link RecordFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link RecordFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class RecordFragment extends Fragment {
    private OnFragmentInteractionListener mListener;

    private Button mRecordButton = null;
    private MediaRecorder mRecorder = null;
    private static String mFileName = null;
    private Button mPlayButton = null;
    private MediaPlayer mPlayer = null;
    private static final String LOG_TAG = "AudioRecordTest";

    public Boolean isRecording = false;
    public Boolean isPlaying = false;
    public Boolean hasSound = false;

    private void onRecord() {
        if (!isRecording) {
            isRecording = true;
            hasSound = true;
            startRecording();
            mRecordButton.setText("Stop Recording...");
        } else {
            stopRecording();
            isRecording = false;
            mRecordButton.setText("Start Recording");
        }
    }

    private void onPlay() {
        if (!isPlaying && hasSound) {
            startPlaying();
            isPlaying = true;
            mPlayButton.setText("Stop Playing");
        } else {
            isPlaying = false;
            stopPlaying();
            mPlayButton.setText("Play your recording");
        }
    }

    private void startPlaying() {
        mPlayer = new MediaPlayer();
        try {
            mPlayer.setDataSource(mFileName);
            mPlayer.prepare();
            mPlayer.start();
        } catch (IOException e) {
            Log.e(LOG_TAG, "prepare() failed");
        }
    }

    private void stopPlaying() {
        mPlayer.release();
        mPlayer = null;
    }

    private void startRecording() {
        mRecorder = new MediaRecorder();
        mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        mRecorder.setOutputFile(mFileName);
        mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);

        try {
            mRecorder.prepare();
        } catch (IOException e) {
            Log.e(LOG_TAG, "prepare() failed");
        }

        mRecorder.start();
    }

    private void stopRecording() {
        mRecorder.stop();
        mRecorder.release();
        mRecorder = null;
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment Record.
     */
    // TODO: Rename and change types and number of parameters
    public static RecordFragment newInstance() {
        RecordFragment fragment = new RecordFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    public RecordFragment() {
        // Required empty public constructor
        mFileName = Environment.getExternalStorageDirectory().getAbsolutePath();
        mFileName += "/"+ UUID.randomUUID().toString()+".3gp";
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {

        }
        /*
        mRecordButton = (Button) getView().findViewById(R.id.button_record);
        mPlayButton = (Button) getView().findViewById(R.id.button_play);

        mPlayButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                onPlay();
            }
        });
        mRecordButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                onRecord();
            }
        });
        */
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        final View rootView = inflater.inflate(R.layout.fragment_record, container, false);


        mRecordButton = (Button) rootView.findViewById(R.id.button_record);
        mPlayButton = (Button) rootView.findViewById(R.id.button_play);

        Button submitSound = (Button) rootView.findViewById(R.id.button_submit_recording);

        mPlayButton.setText("Play your recording");
        mRecordButton.setText("Start recording");

        mPlayButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                onPlay();
            }
        });
        mRecordButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                onRecord();
            }
        });

        submitSound.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                File file = new File(mFileName);
                new UploadAsyncTask().doInBackground(file);

            }

        });

        return rootView;
    }

    class UploadAsyncTask extends AsyncTask<File, Void, Integer> {
        private File soundFile;

        @Override
        protected Integer doInBackground(File... params) {
            soundFile = params[0];

            try {
                HttpClient httpclient = new DefaultHttpClient();

                HttpPost httppost = new HttpPost("http://118.138.242.136/flashspeak/receive.php");

                InputStreamEntity reqEntity = new InputStreamEntity(new FileInputStream(soundFile), -1);
                reqEntity.setContentType("binary/octet-stream");
                reqEntity.setChunked(true); // Send in multiple parts if needed
                httppost.setEntity(reqEntity);

                try {
                    HttpResponse response = httpclient.execute(httppost);
                    BufferedReader rd = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
                    final StringBuilder out = new StringBuilder();
                    String line;
                    try {
                        while ((line = rd.readLine()) != null) {
                            out.append(line);
                        }
                    } catch (Exception e) {}
                    // wr.close();
                    try {
                        rd.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    // final String serverResponse = slurp(is);
                    Log.d("POST RESPONSE", "serverResponse: " + out.toString());
                } catch (ClientProtocolException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                //Do something with response...

            } catch (Exception e) {
                // show error
            }

            return 0;
        }
    }


    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (OnFragmentInteractionListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p/>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        public void onFragmentInteraction(Uri uri);
    }

}

package uq.androidhack.flashspeak;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.media.audiofx.Visualizer;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
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
import android.widget.LinearLayout;
import android.widget.TextView;

import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.NameValuePair;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.http.params.CoreProtocolPNames;
import org.apache.http.params.HttpParams;

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
import java.net.URI;
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

    //Constants for vizualizator - HEIGHT 50dip
    private static final float VISUALIZER_HEIGHT_DIP = 80f;

    //Your MediaPlayer
    MediaPlayer mp;

    //Vizualization
    private Visualizer mVisualizer;

    private LinearLayout mLinearLayout;
    private VisualizerView mVisualizerView;
    private TextView mStatusTextView;

    /**
     * A simple class that draws waveform data received from a
     * {@link Visualizer.OnDataCaptureListener#onWaveFormDataCapture }
     */
    class VisualizerView extends View {
        private byte[] mBytes;
        private float[] mPoints;
        private Rect mRect = new Rect();

        private Paint mForePaint = new Paint();

        public VisualizerView(Context context) {
            super(context);
            init();
        }

        private void init() {
            mBytes = null;

            mForePaint.setStrokeWidth(1f);
            mForePaint.setAntiAlias(true);
            mForePaint.setColor(Color.rgb(0, 128, 255));
        }

        public void updateVisualizer(byte[] bytes) {
            mBytes = bytes;
            invalidate();
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);

            if (mBytes == null) {
                return;
            }

            if (mPoints == null || mPoints.length < mBytes.length * 4) {
                mPoints = new float[mBytes.length * 4];
            }

            mRect.set(0, 0, getWidth(), getHeight());

            for (int i = 0; i < mBytes.length - 1; i++) {
                mPoints[i * 4] = mRect.width() * i / (mBytes.length - 1);
                mPoints[i * 4 + 1] = mRect.height() / 2
                        + ((byte) (mBytes[i] + 128)) * (mRect.height() / 2) / 128;
                mPoints[i * 4 + 2] = mRect.width() * (i + 1) / (mBytes.length - 1);
                mPoints[i * 4 + 3] = mRect.height() / 2
                        + ((byte) (mBytes[i + 1] + 128)) * (mRect.height() / 2) / 128;
            }

            canvas.drawLines(mPoints, mForePaint);
        }
    }

    //Our method that sets Vizualizer
    private void setupVisualizerFxAndUI() {
        // Create a VisualizerView (defined below), which will render the simplified audio
        // wave form to a Canvas.

        //You need to have something where to show Audio WAVE - in this case Canvas
        mVisualizerView = new VisualizerView(this.getActivity().getApplicationContext());
        mVisualizerView.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                (int)(VISUALIZER_HEIGHT_DIP * getResources().getDisplayMetrics().density)));
        mLinearLayout.addView(mVisualizerView);

        // Create the Visualizer object and attach it to our media player.
        //YOU NEED android.permission.RECORD_AUDIO for that in AndroidManifest.xml
        mVisualizer = new Visualizer(mPlayer.getAudioSessionId());
        mVisualizer.setCaptureSize(Visualizer.getCaptureSizeRange()[1]);
        mVisualizer.setDataCaptureListener(new Visualizer.OnDataCaptureListener() {
            public void onWaveFormDataCapture(Visualizer visualizer, byte[] bytes,
                                              int samplingRate) {
                mVisualizerView.updateVisualizer(bytes);
            }

            public void onFftDataCapture(Visualizer visualizer, byte[] bytes, int samplingRate) {}
        }, Visualizer.getMaxCaptureRate() / 2, true, false);
    }


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

            setupVisualizerFxAndUI();
            mVisualizer.setEnabled(true);
            //mStatusTextView.setText("Playing audio...");

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

        //set content view to new Layout that we create
        //setContentView(mLinearLayout);

        //start media player - like normal
        mp = new MediaPlayer();


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

        //Info textView
        mStatusTextView = new TextView(this.getActivity().getApplicationContext());
        //Create new LinearLayout ( because main.xml is empty )
        mLinearLayout = (LinearLayout) rootView.findViewById(R.id.linear_layout_wave);
        //mLinearLayout.setOrientation(LinearLayout.VERTICAL);
        mLinearLayout.addView(mStatusTextView);


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

            URI url = URI.create("http://118.138.242.136:9000/echoLs");
            HttpPut p = new HttpPut( url );
            DefaultHttpClient client = new DefaultHttpClient();

            try {
                // new file and and entity
                File file = soundFile;

                byte[] bytes = null;
                ByteArrayEntity requestEntity = null;

                try
                {
                    InputStream inputStream = new FileInputStream(file);
                    ByteArrayOutputStream bos = new ByteArrayOutputStream();
                    byte[] b = new byte[1024*8];
                    int bytesRead =0;

                    while ((bytesRead = inputStream.read(b)) != -1)
                    {
                        bos.write(b, 0, bytesRead);
                    }

                    bytes = bos.toByteArray();
                    requestEntity = new ByteArrayEntity( bytes );
                }
                catch (IOException e)
                {
                    e.printStackTrace();
                }

                HttpParams clientParams = client.getParams();
                clientParams.setParameter( CoreProtocolPNames.PROTOCOL_VERSION, HttpVersion.HTTP_1_1 );
                clientParams.setParameter( CoreConnectionPNames.SO_TIMEOUT, new Integer( 15000 ) );
                clientParams.setParameter( CoreConnectionPNames.CONNECTION_TIMEOUT, new Integer( 15000 ) );

                p.setEntity( requestEntity );

                p.getParams().setBooleanParameter(CoreProtocolPNames.USE_EXPECT_CONTINUE, false);

                Log.i( "UPLOAD", "execute" );
                HttpResponse response = client.execute( p );

                StatusLine line = response.getStatusLine();
                Log.i( "UPLOAD", "complete: " + line );

                // return code indicates upload failed so throw exception
                if( line.getStatusCode() < 200 || line.getStatusCode() >= 300 ) {
                    throw new Exception( "Failed upload" );
                }

                // shut down connection
                client.getConnectionManager().shutdown();

                // notify user that file has been uploaded
                //notification.finished();
            } catch ( Exception e ) {
                //Log.e( "UPLOAD", "exception: " + sUrl, e );
                // file upload failed so abort post and close connection
                p.abort();
                client.getConnectionManager().shutdown();

                // get user preferences and number of retries for failed upload
                //SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
                //int maxRetries = Integer.valueOf( prefs.getString( "retries", "" ).substring( 1 ) );

                // check if we can connect to internet and if we still have any tries left
                // to try upload again
                /*if( CheckInternet.getInstance().canConnect( context, prefs ) && retries < maxRetries ) {
                    // remove notification for failed upload and queue item again
                    Log.i( TAG, "will retry" );
                    notification.remove();
                    queue.execute( new MiltonPutUploader( context, queue, item, config, retries + 1 ) );
                } else {
                    // upload failed, so let's notify user
                    Log.i( TAG, "will not retry" );
                    notification.failed();
                }*/
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

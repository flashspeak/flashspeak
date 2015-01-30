package uq.androidhack.flashspeak;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import java.io.File;
import java.io.InputStream;
import java.net.URI;

import uq.androidhack.flashspeak.interfaces.TargetFileListener;
import uq.androidhack.flashspeak.interfaces.TrialFileListener;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link VisualisationFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link VisualisationFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class VisualisationFragment extends Fragment implements TargetFileListener,TrialFileListener{

    private OnFragmentInteractionListener mListener;

    //Here is your URL defined
    String url = "http://vprbbc.streamguys.net/vprbbc24.mp3";



    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment VisualisationFragment.
     */
    public static VisualisationFragment newInstance() {
        VisualisationFragment fragment = new VisualisationFragment();
        Bundle args = new Bundle();
        //args.putString(ARG_PARAM1, param1);
        //args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    public VisualisationFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {

        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_visualisation, container, false);
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

    @Override
    public void onFileChange(String uri) {
        File file = new File (uri);
        ImageView ogAudioSampleImageView = (ImageView)getView().findViewById(R.id.originalAudioSampleVisualisation);
        ogAudioSampleImageView.setImageDrawable(Drawable.createFromPath(file.getAbsolutePath()));
    }

    @Override
    public void onRecording(URI uri) {
        ImageView targetAudioSampleImageView = (ImageView)getView().findViewById(R.id.usersAudioSampleVisualisation);
        targetAudioSampleImageView.setImageResource(android.R.color.transparent);
    }

    @Override
    public void onFinishProcessing(String b) {
        Log.i("VISUALIZER", "in HERE!");
        ImageView ogAudioSampleImageView = (ImageView)getView().findViewById(R.id.originalAudioSampleVisualisation);
        //ogAudioSampleImageView.setImageURI(new URI(b));

        new DownloadImageTask(ogAudioSampleImageView).execute(b);

    }


    private class DownloadImageTask extends AsyncTask<String, Void, Bitmap> {
        ImageView bmImage;

        public DownloadImageTask(ImageView bmImage) {
            this.bmImage = bmImage;
        }

        protected Bitmap doInBackground(String... urls) {
            String urldisplay = urls[0];
            Bitmap mIcon11 = null;
            try {
                InputStream in = new java.net.URL(urldisplay).openStream();
                mIcon11 = BitmapFactory.decodeStream(in);
            } catch (Exception e) {
                Log.e("Error", e.getMessage());
                e.printStackTrace();
            }
            return mIcon11;
        }

        protected void onPostExecute(Bitmap result) {
            bmImage.setImageBitmap(result);
        }
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
        public void onFragmentInteraction(Bitmap uri);
    }
}

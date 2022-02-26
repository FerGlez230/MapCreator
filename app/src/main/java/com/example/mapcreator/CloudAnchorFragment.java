package com.example.mapcreator;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;

import com.example.mapcreator.controllers.BDController;
import com.example.mapcreator.controllers.BDRequest;
import com.example.mapcreator.helpers.CloudAnchorManager;
import com.example.mapcreator.helpers.SnackbarHelper;
import  com.example.mapcreator.helpers.*;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationAvailability;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.CancellationToken;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.OnTokenCanceledListener;
import com.google.ar.core.Anchor;
import com.google.ar.core.ArCoreApk;
import com.google.ar.core.Camera;
import com.google.ar.core.Config;
import com.google.ar.core.Frame;
import com.google.ar.core.HitResult;
import com.google.ar.core.Plane;
import com.google.ar.core.Point;
import com.google.ar.core.Point.OrientationMode;
import com.google.ar.core.PointCloud;
import com.google.ar.core.Session;
import com.google.ar.core.Trackable;
import com.google.ar.core.TrackingState;
import com.example.mapcreator.rendering.*;
import com.google.ar.core.exceptions.CameraNotAvailableException;
import com.google.ar.core.exceptions.UnavailableApkTooOldException;
import com.google.ar.core.exceptions.UnavailableArcoreNotInstalledException;
import com.google.ar.core.exceptions.UnavailableDeviceNotCompatibleException;
import com.google.ar.core.exceptions.UnavailableSdkTooOldException;
import com.google.ar.core.exceptions.UnavailableUserDeclinedInstallationException;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import com.example.mapcreator.models.AnchorStorageObject;

import static android.content.Context.LOCATION_SERVICE;

/**
 * Main Fragment for the Cloud Anchors Codelab.
 *
 * <p>This is where the AR Session and the Cloud Anchors are managed.
 */
public class CloudAnchorFragment extends Fragment implements GLSurfaceView.Renderer {

    private static final String TAG = CloudAnchorFragment.class.getSimpleName();

    //GET THE CURRENT LOCATION
    private FusedLocationProviderClient fusedLocationClient;
    private  Location currentLocation;
    // Rendering. The Renderers are created here, and initialized when the GL surface is created.
    private GLSurfaceView surfaceView;
    private long totalAnchors=0;
    private boolean installRequested;
    private Button resolveButton, listButton;
    private EditText shortCodeEdit;
    private Session session;
    private final SnackbarHelper messageSnackbarHelper = new SnackbarHelper();
    private final CloudAnchorManager cloudAnchorManager = new CloudAnchorManager();
    private DisplayRotationHelper displayRotationHelper;
    private TrackingStateHelper trackingStateHelper;
    private TapHelper tapHelper;
    private BDRequest bdRequest = new BDRequest(getContext());
    private final BackgroundRenderer backgroundRenderer = new BackgroundRenderer();
    private final ObjectRenderer virtualObject = new ObjectRenderer();
    private final ObjectRenderer virtualObjectShadow = new ObjectRenderer();
    private final PlaneRenderer planeRenderer = new PlaneRenderer();
    private final PointCloudRenderer pointCloudRenderer = new PointCloudRenderer();

    // Temporary matrix allocated here to reduce number of allocations for each frame.
    private final float[] anchorMatrix = new float[16];
    private static final String SEARCHING_PLANE_MESSAGE = "Searching for surfaces...";
    private final float[] andyColor = {139.0f, 195.0f, 74.0f, 255.0f};

    @Nullable
    private Anchor currentAnchor = null;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        tapHelper = new TapHelper(context);
        trackingStateHelper = new TrackingStateHelper(requireActivity());
    }

    @Override
    public View onCreateView(
            LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Inflate from the Layout XML file.
        View rootView = inflater.inflate(R.layout.fragment_cloud_anchor, container, false);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(getContext());
        getLocation();
        GLSurfaceView surfaceView = rootView.findViewById(R.id.surfaceView);
        this.surfaceView = surfaceView;
        displayRotationHelper = new DisplayRotationHelper(requireContext());
        surfaceView.setOnTouchListener(tapHelper);

        surfaceView.setPreserveEGLContextOnPause(true);
        surfaceView.setEGLContextClientVersion(2);
        surfaceView.setEGLConfigChooser(8, 8, 8, 8, 16, 0); // Alpha used for plane blending.
        surfaceView.setRenderer(this);
        surfaceView.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
        surfaceView.setWillNotDraw(false);

        Button clearButton = rootView.findViewById(R.id.clear_button);
        clearButton.setOnClickListener(v -> onClearButtonPressed());
        shortCodeEdit = rootView.findViewById(R.id.shortCode_edit_text);
        resolveButton = rootView.findViewById(R.id.resolve_button);
        listButton = rootView.findViewById(R.id.list_button);
        listButton.setOnClickListener( v -> listAnchors() );
        resolveButton.setOnClickListener(v -> onResolveButtonPressed());


        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();

        if (session == null) {
            Exception exception = null;
            String message = null;
            try {
                switch (ArCoreApk.getInstance().requestInstall(requireActivity(), !installRequested)) {
                    case INSTALL_REQUESTED:
                        installRequested = true;
                        return;
                    case INSTALLED:
                        break;
                }

                // ARCore requires camera permissions to operate. If we did not yet obtain runtime
                // permission on Android M and above, now is a good time to ask the user for it.
                if (!CameraPermissionHelper.hasCameraPermission(requireActivity())) {
                    CameraPermissionHelper.requestCameraPermission(requireActivity());
                    return;
                }
                //ASK FOR LOCATION PERMISSION
                if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                        && ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                            100);

                }

                // Create the session.
                session = new Session(requireActivity());
                Config config =new Config(session);
                config.setCloudAnchorMode(Config.CloudAnchorMode.ENABLED);
                session.configure(config);

            } catch (UnavailableArcoreNotInstalledException
                    | UnavailableUserDeclinedInstallationException e) {
                message = "Please install ARCore";
                exception = e;
            } catch (UnavailableApkTooOldException e) {
                message = "Please update ARCore";
                exception = e;
            } catch (UnavailableSdkTooOldException e) {
                message = "Please update this app";
                exception = e;
            } catch (UnavailableDeviceNotCompatibleException e) {
                message = "This device does not support AR";
                exception = e;
            } catch (Exception e) {
                message = "Failed to create AR session";
                exception = e;
            }

            if (message != null) {
                messageSnackbarHelper.showError(requireActivity(), message);
                Log.e(TAG, "Exception creating session", exception);
                return;
            }
        }

        // Note that order matters - see the note in onPause(), the reverse applies here.
        try {
            session.resume();
        } catch (CameraNotAvailableException e) {
            messageSnackbarHelper
                    .showError(requireActivity(), "Camera not available. Try restarting the app.");
            session = null;
            return;
        }

        surfaceView.onResume();
        displayRotationHelper.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (session != null) {
            // Note that the order matters - GLSurfaceView is paused first so that it does not try
            // to query the session. If Session is paused before GLSurfaceView, GLSurfaceView may
            // still call session.update() and get a SessionPausedException.
            displayRotationHelper.onPause();
            surfaceView.onPause();
            session.pause();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] results) {
        if (!CameraPermissionHelper.hasCameraPermission(requireActivity())) {
            Toast.makeText(requireActivity(), "Camera permission is needed to run this application",
                    Toast.LENGTH_LONG)
                    .show();
            if (!CameraPermissionHelper.shouldShowRequestPermissionRationale(requireActivity())) {
                // Permission denied with checking "Do not ask again".
                CameraPermissionHelper.launchPermissionSettings(requireActivity());
            }
            requireActivity().finish();
        }
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        GLES20.glClearColor(0.1f, 0.1f, 0.1f, 1.0f);

        // Prepare the rendering objects. This involves reading shaders, so may throw an IOException.
        try {
            // Create the texture and pass it to ARCore session to be filled during update().
            backgroundRenderer.createOnGlThread(getContext());
            planeRenderer.createOnGlThread(getContext(), "models/trigrid.png");
            pointCloudRenderer.createOnGlThread(getContext());

            virtualObject.createOnGlThread(getContext(), "models/andy.obj", "models/andy.png");
            virtualObject.setMaterialProperties(0.0f, 2.0f, 0.5f, 6.0f);

            virtualObjectShadow
                    .createOnGlThread(getContext(), "models/andy_shadow.obj", "models/andy_shadow.png");
            virtualObjectShadow.setBlendMode(ObjectRenderer.BlendMode.Shadow);
            virtualObjectShadow.setMaterialProperties(1.0f, 0.0f, 0.0f, 1.0f);

        } catch (IOException e) {
            Log.e(TAG, "Failed to read an asset file", e);
        }
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        displayRotationHelper.onSurfaceChanged(width, height);
        GLES20.glViewport(0, 0, width, height);
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        // Clear screen to notify driver it should not load any pixels from previous frame.
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

        if (session == null) {
            return;
        }
        // Notify ARCore session that the view size changed so that the perspective matrix and
        // the video background can be properly adjusted.
        displayRotationHelper.updateSessionIfNeeded(session);

        try {
            session.setCameraTextureName(backgroundRenderer.getTextureId());

            // Obtain the current frame from ARSession. When the configuration is set to
            // UpdateMode.BLOCKING (it is by default), this will throttle the rendering to the
            // camera framerate.
            Frame frame = session.update();
            cloudAnchorManager.onUpdate();
            Camera camera = frame.getCamera();

            // Handle one tap per frame.
            handleTap(frame, camera);

            // If frame is ready, render camera preview image to the GL surface.
            backgroundRenderer.draw(frame);

            // Keep the screen unlocked while tracking, but allow it to lock when tracking stops.
            trackingStateHelper.updateKeepScreenOnFlag(camera.getTrackingState());

            // If not tracking, don't draw 3D objects, show tracking failure reason instead.
            if (camera.getTrackingState() == TrackingState.PAUSED) {
                messageSnackbarHelper.showMessage(
                        getActivity(), TrackingStateHelper.getTrackingFailureReasonString(camera));
                return;
            }

            // Get projection matrix.
            float[] projmtx = new float[16];
            camera.getProjectionMatrix(projmtx, 0, 0.1f, 100.0f);

            // Get camera matrix and draw.
            float[] viewmtx = new float[16];
            camera.getViewMatrix(viewmtx, 0);

            // Compute lighting from average intensity of the image.
            // The first three components are color scaling factors.
            // The last one is the average pixel intensity in gamma space.
            final float[] colorCorrectionRgba = new float[4];
            frame.getLightEstimate().getColorCorrection(colorCorrectionRgba, 0);

            // Visualize tracked points.
            // Use try-with-resources to automatically release the point cloud.
            try (PointCloud pointCloud = frame.acquirePointCloud()) {
                pointCloudRenderer.update(pointCloud);
                pointCloudRenderer.draw(viewmtx, projmtx);
            }

            // No tracking error at this point. If we didn't detect any plane, show searchingPlane message.
            if (!hasTrackingPlane()) {
                messageSnackbarHelper.showMessage(getActivity(), SEARCHING_PLANE_MESSAGE);
            }

            // Visualize planes.
            planeRenderer.drawPlanes(
                    session.getAllTrackables(Plane.class), camera.getDisplayOrientedPose(), projmtx);
            session.getAllAnchors().forEach(anchor -> {
                if (anchor != null && anchor.getTrackingState() == TrackingState.TRACKING) {
                    anchor.getPose().toMatrix(anchorMatrix, 0);
                    // Update and draw the model and its shadow.
                    virtualObject.updateModelMatrix(anchorMatrix, 1f);
                    virtualObjectShadow.updateModelMatrix(anchorMatrix, 1f);

                    virtualObject.draw(viewmtx, projmtx, colorCorrectionRgba, andyColor);
                    virtualObjectShadow.draw(viewmtx, projmtx, colorCorrectionRgba, andyColor);
                }
            });
        } catch (Throwable t) {
            // Avoid crashing the application due to unhandled exceptions.
            Log.e(TAG, "Exception on the OpenGL thread", t);
        }
    }

    // Handle only one tap per frame, as taps are usually low frequency compared to frame rate.
    private void handleTap(Frame frame, Camera camera) {
        if(!shortCodeEdit.getText().toString().isEmpty()) {
            MotionEvent tap = tapHelper.poll();
            if (tap != null && camera.getTrackingState() == TrackingState.TRACKING) {
                for (HitResult hit : frame.hitTest(tap)) {
                    // Check if any plane was hit, and if it was hit inside the plane polygon
                    Trackable trackable = hit.getTrackable();
                    // Creates an anchor if a plane or an oriented point was hit.
                    if ((trackable instanceof Plane
                            && ((Plane) trackable).isPoseInPolygon(hit.getHitPose())
                            && (PlaneRenderer.calculateDistanceToPlane(hit.getHitPose(), camera.getPose()) > 0))
                            || (trackable instanceof Point
                            && ((Point) trackable).getOrientationMode()
                            == OrientationMode.ESTIMATED_SURFACE_NORMAL)) {
                        // Hits are sorted by depth. Consider only closest hit on a plane or oriented point.

                        // Adding an Anchor tells ARCore that it should track this position in
                        // space. This anchor is created on the Plane to place the 3D model
                        // in the correct position relative both to the world and to the plane.
                        currentAnchor = hit.createAnchor();

                        getActivity().runOnUiThread(() -> resolveButton.setEnabled(false));
                        messageSnackbarHelper.showMessage(getActivity(), "Now hosting anchor...");
                        cloudAnchorManager.hostCloudAnchor(session, currentAnchor, /* ttl= */ 300, this::onHostedAnchorAvailable);
                        break;
                    }
                }
            }
        }else {
            messageSnackbarHelper.showMessage(getActivity(), "Enter short code" );
        }
    }
    private synchronized void onResolveButtonPressed() {
        String cloudAnchorId = "";
        BDController admin;
        admin=new BDController(getContext(), "cetiColomosAR.db", null, 1);
        SQLiteDatabase bd=admin.getReadableDatabase();
        Cursor anchorsRows=bd.rawQuery("select * from anchors", null);
        int total=anchorsRows.getCount();
        messageSnackbarHelper.showMessage(getActivity(),"Total anchors" + total);
        if(anchorsRows != null){
            for (int i = 0; i < total ; i++) {
                anchorsRows.moveToNext();
                cloudAnchorId = anchorsRows.getString(1); //GET THE ID FOR THE CURRENT ANCHOR (not short code)
                if (cloudAnchorId == null || cloudAnchorId.isEmpty()) {
                    messageSnackbarHelper.showMessage(
                            getActivity(),
                            "A Cloud Anchor ID for the short code " + anchorsRows.getInt(0) + " was not found.");
                    return;
                }
                resolveButton.setEnabled(false);
                int shortCode = anchorsRows.getInt(0);
                cloudAnchorManager.resolveCloudAnchor(
                        session,
                        cloudAnchorId,
                        anchor -> onResolvedAnchorAvailable(anchor, shortCode));
            }
        }else {
            messageSnackbarHelper.showMessage(getActivity(),"No data to display");
        }
        anchorsRows.close();
        bd.close();
    }
    /**
     * Checks if we detected at least one plane.
     */
    private boolean hasTrackingPlane() {
        for (Plane plane : session.getAllTrackables(Plane.class)) {
            if (plane.getTrackingState() == TrackingState.TRACKING) {
                return true;
            }
        }
        return false;
    }

    private synchronized void onClearButtonPressed() {
        // Clear the anchor from the scene.
        cloudAnchorManager.clearListeners();
        resolveButton.setEnabled(true);
        currentAnchor = null;
    }
    private synchronized void onHostedAnchorAvailable(Anchor anchor) {

        Anchor.CloudAnchorState cloudState = anchor.getCloudAnchorState();
        if (cloudState == Anchor.CloudAnchorState.SUCCESS) {
            currentAnchor = anchor;

            do{
                getLocation();

                messageSnackbarHelper.showMessage(getActivity(), "Location is null");

            } while(currentLocation == null);
            BDController admin;
            admin=new BDController(getContext(), "cetiColomosAR.db", null, 1);
            totalAnchors = bdRequest.addAnchorOnBD(new AnchorStorageObject(
                    Integer.parseInt(shortCodeEdit.getText().toString()),
                    currentAnchor.getCloudAnchorId(),
                    currentLocation.getLatitude(),
                    currentLocation.getLongitude()), admin);

            messageSnackbarHelper.showMessage(
                    getActivity(), "Cloud Anchor Hosted. Short code: " + totalAnchors
                            + "\n lat" + currentLocation.getLatitude()
                            + "\n long" + currentLocation.getLongitude()
                            + "\n total anchors" + totalAnchors);



        } else {
            messageSnackbarHelper.showMessage(getActivity(), "Error while hosting: " + cloudState.toString());
        }
    }
    private synchronized void onResolvedAnchorAvailable(Anchor anchor, int shortCode) {
        Anchor.CloudAnchorState cloudState = anchor.getCloudAnchorState();

        if (cloudState == Anchor.CloudAnchorState.SUCCESS) {

            messageSnackbarHelper.showMessage(getActivity(), "Cloud Anchor Resolved. Short code: " + shortCode +'\n'+ anchor.toString());
            Log.i("hola", anchor.toString());
            currentAnchor = anchor;
        } else {
            messageSnackbarHelper.showMessage(
                    getActivity(),
                    "Error while resolving anchor with short code " + shortCode + ". Error: "
                            + cloudState.toString());
            resolveButton.setEnabled(true);
        }
    }

    private void getLocation() {
        if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                    1000);

        } else {

                fusedLocationClient.getLastLocation().addOnSuccessListener(location -> currentLocation = location);

        }
    }
    public void listAnchors() {
        String shortCodes ="";
        BDController admin;
        admin=new BDController(getContext(), "cetiColomosAR.db", null, 1);
        SQLiteDatabase bd=admin.getReadableDatabase();
        Cursor anchorsRows=bd.rawQuery("select shortCode from anchors", null);

        int total=anchorsRows.getCount();
        if(anchorsRows != null){
            for (int i = 0; i < total ; i++) {
                anchorsRows.moveToNext();
                shortCodes += anchorsRows.getString(0) + ",  "; //GET THE ID FOR THE CURRENT ANCHOR (not short code)
            }


        }else {
            messageSnackbarHelper.showMessage(getActivity(),"No data to display");
        }
        anchorsRows.close();
        bd.close();
        messageSnackbarHelper.showMessage(getActivity(),
                shortCodes);

        /*AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        builder.setTitle("Titulo")
                .setMessage(shortCodes)
                .setPositiveButton("OK", null);
        return builder.create();*/
        new AlertDialog.Builder(getActivity())
                .setTitle("Codes")
                .setMessage(shortCodes)
                .setPositiveButton("Ok", null)
                .show();
    }
}

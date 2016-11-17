package com.bq.daggerskeleton.sample.hardware;

import android.content.Context;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.util.Size;
import android.view.Surface;

import com.bq.daggerskeleton.flux.Dispatcher;
import com.bq.daggerskeleton.flux.Store;
import com.bq.daggerskeleton.sample.app.App;
import com.bq.daggerskeleton.sample.app.AppScope;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import dagger.Module;
import dagger.Provides;
import dagger.multibindings.IntoSet;
import timber.log.Timber;


@AppScope
public class CameraStore extends Store<CameraState> {

   private final App app;
   private final CameraManager cameraManager;
   private final Handler backgroundHandler;
   private final Semaphore cameraLock = new Semaphore(1);
   private static final int CAMERA_LOCK_TIMEOUT = 3000; //3s

   @Override protected CameraState initialState() {
      return new CameraState();
   }

   @Inject CameraStore(App app, Handler backgroundHandler) {
      this.app = app;
      this.cameraManager = ((CameraManager) app.getSystemService(Context.CAMERA_SERVICE));
      this.backgroundHandler = backgroundHandler;

      Dispatcher.subscribe(CameraPermissionChanged.class, permissionChanged -> {
         CameraState newState = new CameraState(state());
         newState.canOpenCamera = permissionChanged.granted;
         setState(newState);
      });

      Dispatcher.subscribe(OpenCameraAction.class, a -> {
         if (state().cameraDevice == null) setState(openCamera(state()));
      });

      Dispatcher.subscribe(CloseCameraAction.class, a -> setState(closeCamera(state())));

      Dispatcher.subscribe(PreviewSurfaceDestroyedAction.class, a -> {
         CameraState newState = new CameraState(state());
         if (newState.previewTexture != null) {
            //Nothing to do here, surface auto releases, don't call release on it
            newState.previewTexture = null;
         }
         setState(newState);
      });

      Dispatcher.subscribe(PreviewSurfaceReadyAction.class, a -> {
         CameraState newState = new CameraState(state());

         //Can't modify the session, so we close and open again
         if (newState.session != null) {
            newState = releaseSession(newState);
         }

         //Update buffer and surface target
         newState.previewTexture = a.surfaceTexture;
         newState.previewSurface = new Surface(a.surfaceTexture);
         newState.previewSize = new Size(a.width, a.height);
         setState(newState);

         tryToStartSession();
      });

      Dispatcher.subscribe(CameraOpenedAction.class, a -> {
         CameraState newState = new CameraState(state());
         newState.cameraDevice = a.camera;
         setState(newState);

         tryToStartSession();
      });


      Dispatcher.subscribe(CaptureSessionStarted.class, a -> {
         CameraState newState = new CameraState(state());
         newState.session = a.session;
         setState(newState);
      });
   }

   private CameraState openCamera(CameraState state) {
      CameraState newState = new CameraState(state);
      if (state().canOpenCamera) {
         try {
            if (!cameraLock.tryAcquire(CAMERA_LOCK_TIMEOUT, TimeUnit.MILLISECONDS)) {
               // STOPSHIP: 16/11/2016
               // TODO: 16/11/2016 Move to an error state and notify user properly
               throw new IllegalStateException("Failed to open camera in time");
            }
            Timber.v("Opening camera");

            populateCameraMap(newState.availableCameras);
            newState.selectedCamera = selectDefaultCamera(newState.availableCameras);
            try {
               //noinspection MissingPermission
               cameraManager.openCamera(newState.selectedCamera, new CameraDevice.StateCallback() {
                  @Override public void onOpened(@NonNull CameraDevice camera) {
                     cameraLock.release();
                     Dispatcher.dispatchOnUi(new CameraOpenedAction(camera));
                  }

                  @Override public void onDisconnected(@NonNull CameraDevice camera) {
                     Timber.d("Camera disconnected: %s", camera.getId());
                  }

                  @Override public void onError(@NonNull CameraDevice camera, int error) {
                     cameraLock.release();
                     Timber.e("Camera error: %d", error);
                  }
               }, backgroundHandler);
            } catch (CameraAccessException e) {
               Timber.e(e);
            }
         } catch (InterruptedException e) {
            e.printStackTrace();
         }
      }

      return newState;
   }

   private CameraState closeCamera(CameraState state) {
      CameraState newState = new CameraState(state);
      try {
         if (!cameraLock.tryAcquire(CAMERA_LOCK_TIMEOUT, TimeUnit.MILLISECONDS)) {
            // STOPSHIP: 16/11/2016
            // TODO: 16/11/2016 Move to an error state and notify user properly
            throw new IllegalStateException("Failed to close camera in time");
         }

         Timber.v("Closing camera");
         newState = releaseSession(newState);
         if (newState.cameraDevice != null) {
            newState.cameraDevice.close();
            newState.cameraDevice = null;
         }
      } catch (InterruptedException e) {
         e.printStackTrace(); //Should never happen
      } finally {
         cameraLock.release();
      }
      return newState;
   }

   /**
    * Start the session if preconditions are met. For this example, the preview surface is ready and
    * the camera opened
    */
   private void tryToStartSession() {
      //Preconditions
      if (state().session != null) return; //Already opened
      if (state().cameraDevice == null) return;
      if (state().previewTexture == null) return;

      try {
         if (!cameraLock.tryAcquire(CAMERA_LOCK_TIMEOUT, TimeUnit.MILLISECONDS)) {
            // STOPSHIP: 16/11/2016
            // TODO: 16/11/2016 Move to an error state and notify user properly
            throw new IllegalStateException("Failed to create camera in time");
         }

         //Already opened, need this double check to avoid locking
         //for the most common case, only 1 source trying to create the session
         if (state().session != null || state().previewTexture == null || state().cameraDevice == null) {
            cameraLock.release();
            return;
         }

         CaptureRequest.Builder request;
         try {
            request = state().cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
         } catch (SecurityException e) {
            // STOPSHIP: 16/11/2016
            // TODO: 16/11/2016 Move to an error state and notify user properly
            Timber.e(e);
            throw new IllegalStateException("Failed to create session, there is a race condition between 2 camera apps");
         }

         request.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);

         //Configure preview surface
         Size previewSize = state().previewSize;
         state().previewTexture.setDefaultBufferSize(previewSize.getWidth(), previewSize.getHeight());
         request.addTarget(state().previewSurface);


         state().cameraDevice
               .createCaptureSession(Collections.singletonList(state().previewSurface), new CameraCaptureSession.StateCallback() {
                  @Override public void onConfigured(@NonNull CameraCaptureSession session) {
                     cameraLock.release();
                     Dispatcher.dispatchOnUi(new CaptureSessionStarted(session));
                     try {
                        session.setRepeatingRequest(request.build(), null, backgroundHandler);
                     } catch (CameraAccessException e) {
                        Timber.e(e);
                     }
                  }

                  @Override public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                     cameraLock.release();
                     Timber.e("Failed to create session. %s", state());
                     throw new IllegalStateException("Failed to create session with the current configuration");
                     //TODO: Move to a default / safe state when the session can be created
                  }
               }, backgroundHandler);

      } catch (IllegalStateException | CameraAccessException e) {
         cameraLock.release();
         Timber.e(e);
      } catch (InterruptedException e) {
         e.printStackTrace(); //Should never happen
      }
   }

   private CameraState releaseSession(CameraState state) {
      CameraState newState = new CameraState(state);
      if (newState.session != null) {
         try {
            newState.session.close();
         } catch (IllegalStateException e) {
            //Will throw IllegalStateException if it is already closed
            //due to a race condition not worth controlling.
            //There is no way to check the camera state other than capturing the exception
            Timber.e(e, "Error trying to release the session");
         }
         newState.session = null;
      }
      return newState;
   }

   private void populateCameraMap(Map<String, CameraCharacteristics> cameraMap) {
      try {
         for (String cameraId : cameraManager.getCameraIdList()) {
            cameraMap.put(cameraId, cameraManager.getCameraCharacteristics(cameraId));
         }
      } catch (CameraAccessException e) {
         e.printStackTrace(); //Permission denied, should not happen
      }
   }

   @NonNull private String selectDefaultCamera(Map<String, CameraCharacteristics> cameras) {
      for (String cameraId : cameras.keySet()) {
         CameraCharacteristics characteristics = cameras.get(cameraId);
         Integer facing = characteristics.get(CameraCharacteristics.LENS_FACING);
         if (facing != null && facing == CameraCharacteristics.LENS_FACING_BACK) {
            return cameraId;
         }
      }
      return cameras.keySet().iterator().next();
   }

   @Module
   public static class CameraModule {
      @Provides @AppScope @IntoSet
      static Store<?> provideCameraStoreToSet(CameraStore store) {
         return store;
      }
   }
}

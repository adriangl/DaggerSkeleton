package com.bq.daggerskeleton.camera.storage;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.graphics.BitmapFactory;
import android.hardware.camera2.TotalCaptureResult;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;

import com.bq.daggerskeleton.App;
import com.bq.daggerskeleton.AppScope;
import com.bq.daggerskeleton.camera.hw.CaptureBytesTakenAction;
import com.bq.daggerskeleton.camera.hw.CaptureCompletedAction;
import com.bq.daggerskeleton.flux.Dispatcher;
import com.bq.daggerskeleton.flux.Store;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import javax.inject.Inject;

import dagger.Provides;
import dagger.multibindings.IntoSet;
import io.reactivex.Single;
import io.reactivex.SingleEmitter;
import io.reactivex.SingleOnSubscribe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;

@AppScope
public class StorageStore extends Store<StorageState> {

    private static final SimpleDateFormat fileNameDateFormat =
            new SimpleDateFormat("yyyyMMdd_HHmmss_SSS", Locale.getDefault());

    private final File cameraFilesRootDirectory;
    private final ContentResolver contentResolver;

    @Inject
    public StorageStore(App app) {
        this.cameraFilesRootDirectory = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DCIM);
        // Ensure that the parent folder is created by doing mkdirs()
        cameraFilesRootDirectory.mkdirs();

        this.contentResolver = app.getContentResolver();

        Dispatcher.subscribe(CaptureBytesTakenAction.class, new Consumer<CaptureBytesTakenAction>() {
            @Override
            public void accept(CaptureBytesTakenAction action) throws Exception {
                StorageState newState = new StorageState(state());
                newState.pendingCaptureBytes = action.imageBytes;
                setState(newState);

                tryToSaveCapture();
            }
        });

        Dispatcher.subscribe(CaptureCompletedAction.class, new Consumer<CaptureCompletedAction>() {
            @Override
            public void accept(CaptureCompletedAction action) throws Exception {
                StorageState newState = new StorageState(state());
                newState.pendingCaptureTotalResult = action.result;
                setState(newState);

                tryToSaveCapture();
            }
        });

        Dispatcher.subscribe(CaptureSavedAction.class, new Consumer<CaptureSavedAction>() {
            @Override
            public void accept(CaptureSavedAction action) throws Exception {
                setState(initialState());
            }
        });
    }

    private void tryToSaveCapture() {
        if (state().pendingCaptureBytes == null) return;
        if (state().pendingCaptureTotalResult == null) return;

        Single.create(new SingleOnSubscribe<String>() {
            @Override
            public void subscribe(SingleEmitter<String> e) throws Exception {
                try {
                    String fileUri = storeImage(
                            state().pendingCaptureBytes,
                            state().pendingCaptureTotalResult);
                    e.onSuccess(fileUri);
                } catch (Exception ex) {
                    e.onError(ex);
                }
            }
        })
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .subscribe(new Consumer<String>() {
                    @Override
                    public void accept(String s) throws Exception {
                        Dispatcher.dispatch(new CaptureSavedAction(s));
                    }
                });
    }

    private String storeImage(byte[] pendingCaptureBytes, TotalCaptureResult pendingCaptureTotalResult) throws IOException {
        String date = fileNameDateFormat.format(new Date());
        File outputFile = new File(cameraFilesRootDirectory, String.format("IMG_%s.jpg", date));

        saveRawImageToDisk(pendingCaptureBytes, outputFile, pendingCaptureTotalResult);
        return addImageToMediaStore(outputFile, pendingCaptureTotalResult);
    }

    private void saveRawImageToDisk(byte[] data, File output, TotalCaptureResult totalCaptureResult) throws IOException {
        // TODO: 16/11/16 Add EXIF to file
        try (FileOutputStream fos = new FileOutputStream(output)) {
            fos.write(data);
            fos.flush();
        }
    }

    private String addImageToMediaStore(File output, TotalCaptureResult pendingCaptureTotalResult) {
        // Get image width & height
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(output.getAbsolutePath(), options);
        // Insert data to MediaStore database
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.DATA, output.getAbsolutePath());
        values.put(MediaStore.Images.Media.DISPLAY_NAME, output.getName());
        values.put(MediaStore.Images.Media.DATE_TAKEN, output.lastModified());
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
        values.put(MediaStore.Images.Media.HEIGHT, options.outHeight);
        values.put(MediaStore.Images.Media.WIDTH, options.outWidth);

        // TODO: 16/11/16 Add values from pendingCaptureTotalResult to add it to Media Store

        Uri fileUri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

        if (fileUri != null) {
            return fileUri.toString();
        } else {
            return null;
        }
    }

    @Override
    public StorageState initialState() {
        return new StorageState();
    }

    @dagger.Module
    public static abstract class Module {
        @Provides
        @AppScope
        @IntoSet
        static Store<?> provideStorageStore(StorageStore impl) {
            return impl;
        }
    }
}

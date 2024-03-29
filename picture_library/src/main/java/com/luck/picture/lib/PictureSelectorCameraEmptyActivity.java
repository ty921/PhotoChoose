package com.luck.picture.lib;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;


import com.luck.picture.lib.config.PictureConfig;
import com.luck.picture.lib.config.PictureMimeType;
import com.luck.picture.lib.entity.LocalMedia;
import com.luck.picture.lib.immersive.ImmersiveManage;
import com.luck.picture.lib.permissions.PermissionChecker;
import com.luck.picture.lib.tools.MediaUtils;
import com.luck.picture.lib.tools.PictureFileUtils;
import com.luck.picture.lib.tools.SdkVersionUtils;
import com.luck.picture.lib.tools.ToastUtils;
import com.luck.picture.lib.tools.ValueOf;
import com.yalantis.ucrop.UCrop;
import com.yalantis.ucrop.UCropMulti;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * @author：luck
 * @date：2019-11-15 21:41
 * @describe：单独拍照承载空Activity
 */
public class PictureSelectorCameraEmptyActivity extends PictureBaseActivity {

    @Override
    public void immersive() {
        ImmersiveManage.immersiveAboveAPI23(this
                , ContextCompat.getColor(this, R.color.picture_color_transparent)
                , ContextCompat.getColor(this, R.color.picture_color_transparent)
                , openWhiteStatusBar);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (PermissionChecker
                .checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) &&
                PermissionChecker
                        .checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            onTakePhoto();
        } else {
            PermissionChecker.requestPermissions(this, new String[]{
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE}, PictureConfig.APPLY_STORAGE_PERMISSIONS_CODE);
        }
        setTheme(R.style.Picture_Theme_Translucent);
        super.onCreate(savedInstanceState);
    }


    @Override
    public int getResourceId() {
        return R.layout.picture_empty;
    }


    /**
     * 启动相机
     */
    private void onTakePhoto() {
        // 启动相机拍照,先判断手机是否有拍照权限
        if (PermissionChecker
                .checkSelfPermission(this, Manifest.permission.CAMERA)) {
            startCamera();
        } else {
            PermissionChecker.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA}, PictureConfig.APPLY_CAMERA_PERMISSIONS_CODE);
        }
    }

    /**
     * 根据类型启动相应相机
     */
    private void startCamera() {
        switch (config.chooseMode) {
            case PictureConfig.TYPE_ALL:
            case PictureConfig.TYPE_IMAGE:
                // 拍照
                startOpenCamera();
                break;
            case PictureConfig.TYPE_VIDEO:
                // 录视频
                startOpenCameraVideo();
                break;
            case PictureConfig.TYPE_AUDIO:
                // 录音
                startOpenCameraAudio();
                break;
            default:
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case UCrop.REQUEST_CROP:
                    singleCropHandleResult(data);
                    break;
                case UCropMulti.REQUEST_MULTI_CROP:
                    multiCropHandleResult(data);
                    break;
                case PictureConfig.REQUEST_CAMERA:
                    requestCamera(data);
                    break;
                default:
                    break;
            }
        } else if (resultCode == RESULT_CANCELED) {
            closeActivity();
        } else if (resultCode == UCrop.RESULT_ERROR) {
            Throwable throwable = (Throwable) data.getSerializableExtra(UCrop.EXTRA_ERROR);
            ToastUtils.s(getContext(), throwable.getMessage());
        }
    }

    /**
     * 单张图片裁剪
     *
     * @param data
     */
    private void singleCropHandleResult(Intent data) {
        List<LocalMedia> medias = new ArrayList<>();
        Uri resultUri = UCrop.getOutput(data);
        String cutPath = resultUri.getPath();
        // 单独拍照
        LocalMedia media = new LocalMedia(cameraPath, 0, false,
                config.isCamera ? 1 : 0, 0, config.chooseMode);
        if (SdkVersionUtils.checkedAndroid_Q()) {
            int lastIndexOf = cameraPath.lastIndexOf("/") + 1;
            media.setId(lastIndexOf > 0 ? ValueOf.toLong(cameraPath.substring(lastIndexOf)) : -1);
        }
        media.setCut(true);
        media.setCutPath(cutPath);
        String mimeType = PictureMimeType.getImageMimeType(cutPath);
        media.setMimeType(mimeType);
        medias.add(media);
        handlerResult(medias);
    }


    /**
     * 拍照后处理结果
     *
     * @param data
     */

    private void requestCamera(Intent data) {
        // on take photo success
        String mimeType = null;
        long duration = 0;
        boolean isAndroidQ = SdkVersionUtils.checkedAndroid_Q();
        if (config.chooseMode == PictureMimeType.ofAudio()) {
            // 音频处理规则
            cameraPath = getAudioPath(data);
            if (TextUtils.isEmpty(cameraPath)) {
                return;
            }
            mimeType = PictureMimeType.MIME_TYPE_AUDIO;
            duration = MediaUtils.extractDuration(getContext(), isAndroidQ, cameraPath);
        }
        if (TextUtils.isEmpty(cameraPath) || new File(cameraPath) == null) {
            return;
        }
        long size = 0;
        int[] newSize = new int[2];
        final File file = new File(cameraPath);
        if (!isAndroidQ) {
            if (config.isFallbackVersion3) {
                new PictureMediaScannerConnection(getContext(), cameraPath,
                        () -> {

                        });
            } else {
                sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(file)));
            }
        }
        LocalMedia media = new LocalMedia();
        if (config.chooseMode != PictureMimeType.ofAudio()) {
            // 图片视频处理规则
            if (isAndroidQ) {
                String path = PictureFileUtils.getPath(getApplicationContext(), Uri.parse(cameraPath));
                File f = new File(path);
                size = f.length();
                mimeType = PictureMimeType.fileToType(f);
                if (PictureMimeType.eqImage(mimeType)) {
                    newSize = MediaUtils.getLocalImageSizeToAndroidQ(this, cameraPath);
                } else {
                    newSize = MediaUtils.getLocalVideoSize(this, Uri.parse(cameraPath));
                    duration = MediaUtils.extractDuration(getContext(), true, cameraPath);
                }
                int lastIndexOf = cameraPath.lastIndexOf("/") + 1;
                media.setId(lastIndexOf > 0 ? ValueOf.toLong(cameraPath.substring(lastIndexOf)) : -1);
            } else {
                mimeType = PictureMimeType.fileToType(file);
                size = new File(cameraPath).length();
                if (PictureMimeType.eqImage(mimeType)) {
                    int degree = PictureFileUtils.readPictureDegree(this, cameraPath);
                    PictureFileUtils.rotateImage(degree, cameraPath);
                    newSize = MediaUtils.getLocalImageWidthOrHeight(cameraPath);
                } else {
                    newSize = MediaUtils.getLocalVideoSize(cameraPath);
                    duration = MediaUtils.extractDuration(getContext(), false, cameraPath);
                }
            }
        }
        media.setDuration(duration);
        media.setWidth(newSize[0]);
        media.setHeight(newSize[1]);
        media.setPath(cameraPath);
        media.setMimeType(mimeType);
        media.setSize(size);
        media.setChooseModel(config.chooseMode);
        cameraHandleResult(media, mimeType);
    }

    /**
     * 摄像头后处理方式
     *
     * @param media
     * @param mimeType
     */
    private void cameraHandleResult(LocalMedia media, String mimeType) {
        // 如果是单选 拍照后直接返回
        boolean eqImg = PictureMimeType.eqImage(mimeType);
        if (config.enableCrop && eqImg) {
            // 去裁剪
            originalPath = cameraPath;
            startCrop(cameraPath);
        } else if (config.isCompress && eqImg && !config.isCheckOriginalImage) {
            // 去压缩
            List<LocalMedia> result = new ArrayList<>();
            result.add(media);
            compressImage(result);
        } else {
            // 不裁剪 不压缩 直接返回结果
            List<LocalMedia> result = new ArrayList<>();
            result.add(media);
            onResult(result);
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        closeActivity();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case PictureConfig.APPLY_STORAGE_PERMISSIONS_CODE:
                // 存储权限
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    PermissionChecker.requestPermissions(this,
                            new String[]{Manifest.permission.CAMERA}, PictureConfig.APPLY_CAMERA_PERMISSIONS_CODE);
                } else {
                    ToastUtils.s(getContext(), getString(R.string.picture_jurisdiction));
                    closeActivity();
                }
                break;
            case PictureConfig.APPLY_CAMERA_PERMISSIONS_CODE:
                // 相机权限
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    onTakePhoto();
                } else {
                    closeActivity();
                    ToastUtils.s(getContext(), getString(R.string.picture_camera));
                }
                break;
        }
    }
}

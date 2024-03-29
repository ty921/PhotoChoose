package com.luck.picture.lib.widget;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Handler;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;


import com.luck.picture.lib.R;
import com.luck.picture.lib.adapter.PictureAlbumDirectoryAdapter;
import com.luck.picture.lib.config.PictureSelectionConfig;
import com.luck.picture.lib.entity.LocalMedia;
import com.luck.picture.lib.entity.LocalMediaFolder;
import com.luck.picture.lib.tools.AnimUtils;
import com.luck.picture.lib.tools.AttrsUtils;
import com.luck.picture.lib.tools.ScreenUtils;

import java.util.List;

/**
 * @author：luck
 * @date：2017-5-25 17:02
 * @describe：文件目录PopupWindow
 */

public class FolderPopWindow extends PopupWindow {
    private Context context;
    private View window;
    private RecyclerView recyclerView;
    private PictureAlbumDirectoryAdapter adapter;
    private boolean isDismiss = false;
    private ImageView ivArrowView;
    private Drawable drawableUp, drawableDown;
    private int chooseMode;
    private PictureSelectionConfig config;
    private int maxHeight;
    private View rootViewBg;

    public FolderPopWindow(Context context, PictureSelectionConfig config) {
        this.context = context;
        this.config = config;
        this.chooseMode = config.chooseMode;
        this.window = LayoutInflater.from(context).inflate(R.layout.picture_window_folder, null);
        this.setContentView(window);
        this.setWidth(RelativeLayout.LayoutParams.MATCH_PARENT);
        this.setHeight(RelativeLayout.LayoutParams.WRAP_CONTENT);
        this.setAnimationStyle(R.style.PictureThemeWindowStyle);
        this.setFocusable(true);
        this.setOutsideTouchable(true);
        this.update();
        if (config.style != null) {
            if (config.style.pictureTitleUpResId != 0) {
                this.drawableUp = ContextCompat.getDrawable(context, config.style.pictureTitleUpResId);
            }
            if (config.style.pictureTitleDownResId != 0) {
                this.drawableDown = ContextCompat.getDrawable(context, config.style.pictureTitleDownResId);
            }
        } else {
            if (config.isWeChatStyle) {
                this.drawableUp = ContextCompat.getDrawable(context, R.drawable.picture_icon_wechat_up);
                this.drawableDown = ContextCompat.getDrawable(context, R.drawable.picture_icon_wechat_down);
            } else {
                if (config.upResId != 0) {
                    this.drawableUp = ContextCompat.getDrawable(context, config.upResId);
                } else {
                    // 兼容老的Theme方式
                    this.drawableUp = AttrsUtils.getTypeValueDrawable(context, R.attr.picture_arrow_up_icon);
                }
                if (config.downResId != 0) {
                    this.drawableDown = ContextCompat.getDrawable(context, config.downResId);
                } else {
                    // 兼容老的Theme方式 picture.arrow_down.icon
                    this.drawableDown = AttrsUtils.getTypeValueDrawable(context, R.attr.picture_arrow_down_icon);
                }
            }
        }
        this.maxHeight = (int) (ScreenUtils.getScreenHeight(context) * 0.6);
        initView();
    }

    public void initView() {
        rootViewBg = window.findViewById(R.id.rootViewBg);
        adapter = new PictureAlbumDirectoryAdapter(config);
        recyclerView = window.findViewById(R.id.folder_list);
        recyclerView.setLayoutManager(new LinearLayoutManager(context));
        recyclerView.setAdapter(adapter);
        rootViewBg.setOnClickListener(v -> dismiss());
    }

    public void bindFolder(List<LocalMediaFolder> folders) {
        adapter.setChooseMode(chooseMode);
        adapter.bindFolderData(folders);
        ViewGroup.LayoutParams lp = recyclerView.getLayoutParams();
        lp.height = folders != null && folders.size() > 8 ? maxHeight
                : ViewGroup.LayoutParams.WRAP_CONTENT;
    }

    public void setArrowImageView(ImageView ivArrowView) {
        this.ivArrowView = ivArrowView;
    }

    @Override
    public void showAsDropDown(View anchor) {
        try {
            if (Build.VERSION.SDK_INT == Build.VERSION_CODES.N) {
                int[] location = new int[2];
                anchor.getLocationInWindow(location);
                showAtLocation(anchor, Gravity.NO_GRAVITY, 0, location[1] + anchor.getHeight());
            } else {
                super.showAsDropDown(anchor);
            }
            isDismiss = false;
            ivArrowView.setImageDrawable(drawableUp);
            AnimUtils.rotateArrow(ivArrowView, true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void setOnItemClickListener(PictureAlbumDirectoryAdapter.OnItemClickListener onItemClickListener) {
        adapter.setOnItemClickListener(onItemClickListener);
    }

    @Override
    public void dismiss() {
        if (isDismiss) {
            return;
        }
        ivArrowView.setImageDrawable(drawableDown);
        AnimUtils.rotateArrow(ivArrowView, false);
        isDismiss = true;
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.JELLY_BEAN) {
            dismiss4Pop();
            isDismiss = false;
        } else {
            FolderPopWindow.super.dismiss();
            isDismiss = false;
        }
    }

    /**
     * 在android4.1.1和4.1.2版本关闭PopWindow
     */
    private void dismiss4Pop() {
        new Handler().post(() -> FolderPopWindow.super.dismiss());
    }

    /**
     * 设置选中状态
     */
    public void notifyDataCheckedStatus(List<LocalMedia> medias) {
        try {
            // 获取选中图片
            List<LocalMediaFolder> folders = adapter.getFolderData();
            for (LocalMediaFolder folder : folders) {
                folder.setCheckedNum(0);
            }
            if (medias.size() > 0) {
                for (LocalMediaFolder folder : folders) {
                    int num = 0;// 记录当前相册下有多少张是选中的
                    List<LocalMedia> images = folder.getImages();
                    for (LocalMedia media : images) {
                        String path = media.getPath();
                        for (LocalMedia m : medias) {
                            if (path.equals(m.getPath())) {
                                num++;
                                folder.setCheckedNum(num);
                            }
                        }
                    }
                }
            }
            adapter.bindFolderData(folders);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

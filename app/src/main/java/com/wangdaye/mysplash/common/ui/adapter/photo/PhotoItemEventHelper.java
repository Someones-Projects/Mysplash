package com.wangdaye.mysplash.common.ui.adapter.photo;

import android.view.View;

import com.wangdaye.mysplash.R;
import com.wangdaye.mysplash.collection.ui.CollectionActivity;
import com.wangdaye.mysplash.common.basic.activity.MysplashActivity;
import com.wangdaye.mysplash.common.download.DownloadHelper;
import com.wangdaye.mysplash.common.utils.helper.NotificationHelper;
import com.wangdaye.mysplash.common.network.json.Photo;
import com.wangdaye.mysplash.common.network.json.User;
import com.wangdaye.mysplash.common.ui.dialog.DeleteCollectionPhotoDialog;
import com.wangdaye.mysplash.common.ui.dialog.DownloadRepeatDialog;
import com.wangdaye.mysplash.common.ui.dialog.SelectCollectionDialog;
import com.wangdaye.mysplash.common.utils.FileUtils;
import com.wangdaye.mysplash.common.bus.event.CollectionEvent;
import com.wangdaye.mysplash.common.bus.MessageBus;
import com.wangdaye.mysplash.common.bus.event.PhotoEvent;
import com.wangdaye.mysplash.common.utils.helper.IntentHelper;
import com.wangdaye.mysplash.common.utils.manager.AuthManager;
import com.wangdaye.mysplash.common.presenter.DispatchCollectionsChangedPresenter;
import com.wangdaye.mysplash.common.presenter.list.LikeOrDislikePhotoPresenter;
import com.wangdaye.mysplash.user.ui.UserActivity;

import java.util.ArrayList;
import java.util.List;

public abstract class PhotoItemEventHelper implements PhotoAdapter.ItemEventCallback {

    private MysplashActivity activity;
    private List<Photo> photoList;
    private LikeOrDislikePhotoPresenter likeOrDislikePhotoPresenter;

    public PhotoItemEventHelper(MysplashActivity activity,
                                List<Photo> photoList,
                                LikeOrDislikePhotoPresenter likeOrDislikePhotoPresenter) {
        this.activity = activity;
        this.photoList = photoList;
        this.likeOrDislikePhotoPresenter = likeOrDislikePhotoPresenter;
    }

    @Override
    public void onStartPhotoActivity(View image, View background, int adapterPosition) {
        ArrayList<Photo> list = new ArrayList<>();
        int headIndex = adapterPosition - 2;
        int size = 5;
        if (headIndex < 0) {
            headIndex = 0;
        }
        if (headIndex + size - 1 > photoList.size() - 1) {
            size = photoList.size() - headIndex;
        }
        for (int i = headIndex; i < headIndex + size; i ++) {
            list.add(photoList.get(i));
        }

        IntentHelper.startPhotoActivity(activity, image, background, list, adapterPosition, headIndex);
    }

    @Override
    public void onStartUserActivity(View avatar, View background, User user, int index) {
        IntentHelper.startUserActivity(activity, avatar, background, user, UserActivity.PAGE_PHOTO);
    }

    @Override
    public void onDeleteButtonClicked(Photo photo, int adapterPosition) {
        if (activity instanceof CollectionActivity) {
            DeleteCollectionPhotoDialog dialog = new DeleteCollectionPhotoDialog();
            dialog.setDeleteInfo(((CollectionActivity) activity).getCollection(), photo);
            dialog.setOnDeleteCollectionListener(result -> {
                MessageBus.getInstance().post(new PhotoEvent(
                        result.photo, result.collection, PhotoEvent.Event.REMOVE_FROM_COLLECTION));

                MessageBus.getInstance().post(new CollectionEvent(
                        result.collection, CollectionEvent.Event.UPDATE));

                MessageBus.getInstance().post(result.user);
            });
            dialog.show(activity.getSupportFragmentManager(), null);
        }
    }

    @Override
    public void onLikeButtonClicked(Photo photo, int adapterPosition, boolean setToLike) {
        if (AuthManager.getInstance().isAuthorized()) {
            photo.settingLike = true;
            MessageBus.getInstance().post(new PhotoEvent(photo));

            likeOrDislikePhotoPresenter.likeOrDislikePhoto(photo, setToLike);
        } else {
            IntentHelper.startLoginActivity(activity);
        }
    }

    @Override
    public void onCollectButtonClicked(Photo photo, int adapterPosition) {
        if (!AuthManager.getInstance().isAuthorized()) {
            IntentHelper.startLoginActivity(activity);
        } else {
            SelectCollectionDialog dialog = new SelectCollectionDialog();
            dialog.setPhotoAndListener(photo, new DispatchCollectionsChangedPresenter());
            dialog.show(activity.getSupportFragmentManager(), null);
        }
    }

    @Override
    public void onDownloadButtonClicked(Photo photo, int adapterPosition) {
        if (DownloadHelper.getInstance(activity)
                .readDownloadingEntityCount(activity, photo.id) > 0) {
            NotificationHelper.showSnackbar(
                    activity, activity.getString(R.string.feedback_download_repeat));
        } else if (FileUtils.isPhotoExists(activity, photo.id)) {
            DownloadRepeatDialog dialog = new DownloadRepeatDialog();
            dialog.setDownloadKey(photo);
            dialog.setOnCheckOrDownloadListener(new DownloadRepeatDialog.OnCheckOrDownloadListener() {
                @Override
                public void onCheck(Object obj) {
                    IntentHelper.startCheckPhotoActivity(activity, photo.id);
                }

                @Override
                public void onDownload(Object obj) {
                    downloadPhoto(photo);
                }
            });
            dialog.show(activity.getSupportFragmentManager(), null);
        } else {
            downloadPhoto(photo);
        }
    }

    public abstract void downloadPhoto(Photo photo);
}

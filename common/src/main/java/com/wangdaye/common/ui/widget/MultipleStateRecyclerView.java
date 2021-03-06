package com.wangdaye.common.ui.widget;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.wangdaye.common.ui.widget.insets.FitBottomSystemBarRecyclerView;

import java.util.ArrayList;
import java.util.List;

public class MultipleStateRecyclerView extends FitBottomSystemBarRecyclerView {

    private Adapter[] multipleAdapters;
    private LayoutManager[] multipleLayouts;
    private List<ItemDecoration> decorationList;
    private List<OnScrollListener> onScrollListenerList;

    private ObjectAnimator animator;

    private Rect windowInsets = new Rect();

    private int paddingLeft;
    private int paddingTop;
    private int paddingRight;
    private int paddingBottom;

    private boolean layoutFinished;

    @StateRule
    private int state;

    public static final int STATE_NORMALLY = 0;
    public static final int STATE_LOADING = 1;
    public static final int STATE_ERROR = 2;
    @IntDef({STATE_NORMALLY, STATE_LOADING, STATE_ERROR})
    public @interface StateRule {}

    public MultipleStateRecyclerView(@NonNull Context context) {
        super(context);
        this.initialize(context);
    }

    public MultipleStateRecyclerView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        this.initialize(context);
    }

    public MultipleStateRecyclerView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.initialize(context);
    }

    private void initialize(Context context) {
        multipleAdapters = new Adapter[STATE_ERROR + 1];
        multipleLayouts = new LayoutManager[] {
                null,
                new LinearLayoutManager(context),
                new LinearLayoutManager(context)
        };
        decorationList = new ArrayList<>();
        onScrollListenerList = new ArrayList<>();

        paddingLeft = getPaddingLeft();
        paddingTop = getPaddingTop();
        paddingRight = getPaddingRight();
        paddingBottom = getPaddingBottom();

        state = STATE_LOADING;
        setLayoutManager(multipleLayouts[STATE_LOADING], STATE_LOADING);

        setFitsSystemWindows(true);
    }

    @Override
    public boolean fitSystemWindows(Rect insets) {
        this.windowInsets = insets;
        return false;
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
        this.layoutFinished = true;
    }

    @Override
    public void setPadding(int left, int top, int right, int bottom) {
        if (state == STATE_NORMALLY) {
            super.setPadding(left, top, right, bottom);
        }
        paddingLeft = left;
        paddingTop = top;
        paddingRight = right;
        paddingBottom = bottom;
    }

    @Override
    public void setPaddingRelative(int start, int top, int end, int bottom) {
        if (state == STATE_NORMALLY) {
            super.setPaddingRelative(start, top, end, bottom);
        }

        paddingTop = top;
        paddingBottom = bottom;
        switch (getLayoutDirection()) {
            case View.LAYOUT_DIRECTION_LTR:
                paddingLeft = start;
                paddingRight = end;
                break;

            case View.LAYOUT_DIRECTION_RTL:
                paddingLeft = end;
                paddingRight = start;
                break;
        }
    }

    @Override
    public void addOnScrollListener(@NonNull OnScrollListener listener) {
        if (state == STATE_NORMALLY) {
            super.addOnScrollListener(listener);
        }
        onScrollListenerList.add(listener);
    }

    @Override
    public void removeOnScrollListener(@NonNull OnScrollListener listener) {
        if (state == STATE_NORMALLY) {
            super.removeOnScrollListener(listener);
        }
        onScrollListenerList.remove(listener);
    }

    @Override
    public void clearOnScrollListeners() {
        if (state == STATE_NORMALLY) {
            super.clearOnScrollListeners();
        }
        onScrollListenerList.clear();
    }

    @Override
    public void setLayoutManager(@Nullable LayoutManager layout) {
        setLayoutManager(layout, STATE_NORMALLY);
    }

    public void setLayoutManager(@Nullable LayoutManager layout, @StateRule int state) {
        if (getState() == state) {
            super.setLayoutManager(layout);
        }
        multipleLayouts[state] = layout;
    }

    @Nullable
    public LayoutManager getLayoutManager(@StateRule int state) {
        return multipleLayouts[state];
    }

    @Override
    public void setAdapter(@Nullable Adapter adapter) {
        setAdapter(adapter, STATE_NORMALLY);
    }

    public void setAdapter(@Nullable Adapter adapter, @StateRule int state) {
        if (getState() == state) {
            super.setAdapter(adapter);
        }
        multipleAdapters[state] = adapter;
    }

    @Override
    public Adapter getAdapter() {
        return getAdapter(STATE_NORMALLY);
    }

    public Adapter getAdapter(@StateRule int state) {
        return multipleAdapters[state];
    }

    @Override
    public void addItemDecoration(@NonNull ItemDecoration decor) {
        addItemDecoration(decor, -1);
    }

    @Override
    public void addItemDecoration(@NonNull ItemDecoration decor, int index) {
        if (getState() == STATE_NORMALLY) {
            super.addItemDecoration(decor, index);
        }
        if (index < 0) {
            decorationList.add(decor);
        } else {
            decorationList.add(index, decor);
        }
    }

    @Override
    public void removeItemDecoration(@NonNull ItemDecoration decor) {
        if (getState() == STATE_NORMALLY) {
            super.removeItemDecoration(decor);
        }
        decorationList.remove(decor);
    }

    @Override
    public void removeItemDecorationAt(int index) {
        if (getState() == STATE_NORMALLY) {
            super.removeItemDecorationAt(index);
        }
        decorationList.remove(index);
    }

    @NonNull
    @Override
    public ItemDecoration getItemDecorationAt(int index) {
        if (getState() == STATE_NORMALLY) {
            return super.getItemDecorationAt(index);
        }
        return decorationList.get(index);
    }

    @Override
    public int getItemDecorationCount() {
        if (getState() == STATE_NORMALLY) {
            return super.getItemDecorationCount();
        }
        return decorationList.size();
    }

    @Override
    public void invalidateItemDecorations() {
        if (getState() == STATE_NORMALLY) {
            super.invalidateItemDecorations();
        }
    }

    public void setState(@StateRule int state) {
        if (getState() != state) {
            if (layoutFinished) {
                animSwitchState(state);
            } else {
                bindStateData(state);
            }
        }
    }

    @StateRule
    public int getState() {
        return state;
    }

    private void animSwitchState(@StateRule final int state) {
        if (animator != null) {
            animator.cancel();
        }
        if (getAlpha() == 0) {
            animShow(state);
        } else {
            animator = ObjectAnimator
                    .ofFloat(this, "alpha", getAlpha(), 0)
                    .setDuration(150);
            animator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    super.onAnimationEnd(animation);
                    animator = null;
                    animShow(state);
                }
            });
            animator.start();
        }
    }

    private void bindStateData(@StateRule int state) {
        this.state = state;
        setAdapter(multipleAdapters[state], state);
        setLayoutManager(multipleLayouts[state], state);
        if (state == STATE_NORMALLY) {
            for (ItemDecoration d : decorationList) {
                super.addItemDecoration(d, -1);
            }
            for (OnScrollListener l : onScrollListenerList) {
                super.addOnScrollListener(l);
            }
            super.setPadding(paddingLeft, paddingTop, paddingRight, paddingBottom);
        } else {
            for (ItemDecoration d : decorationList) {
                super.removeItemDecoration(d);
            }
            super.clearOnScrollListeners();
            super.setPadding(0, 0, 0, 0);
        }
    }

    private void animShow(@StateRule int state) {
        bindStateData(state);
        if (animator != null) {
            animator.cancel();
        }
        animator = ObjectAnimator.ofFloat(
                this, "alpha", 0, 1F
        ).setDuration(150);
        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                animator = null;
            }
        });
        animator.start();
    }

    public Rect getWindowInsets() {
        return windowInsets;
    }
}
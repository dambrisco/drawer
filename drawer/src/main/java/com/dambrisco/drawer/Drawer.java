package com.dambrisco.drawer;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.*;
import android.widget.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Drawer layout
 *
 * @author dambrisco@itriagehealth.com
 * @since 2015-03-16
 */
public class Drawer extends LinearLayout {
    private LinearLayout mWrapper;
    private ScrollView mContent;
    private FrameLayout mHandle;
    private View mOpenHandle;
    private View mClosedHandle;
    private Drawable mHandleBackground;
    private float mMaxTranslation;
    private float mTranslationStart;
    private float mVelocity;
    private int mSlideDuration;
    private DrawerState mDrawerState = DrawerState.OPEN;
    private boolean mDragging = false;
    private GestureDetector mGestureDetector;
    private List<DrawerListener> mDrawerListeners;

    /**
     * {@inheritDoc}
     */
    public Drawer(Context context) {
        super(context);
        TypedArray array = context.obtainStyledAttributes(new int[0]);
        setup(context, array);
    }

    /**
     * {@inheritDoc}
     */
    public Drawer(Context context, AttributeSet attrs) {
        super(context, attrs);
        TypedArray array = context.obtainStyledAttributes(attrs, R.styleable.Drawer);
        setup(context, array);
    }

    /**
     * {@inheritDoc}
     */
    public Drawer(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        TypedArray array = context.obtainStyledAttributes(attrs, R.styleable.Drawer, defStyleAttr, 0);
        setup(context, array);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addView(@NonNull View child) {
        this.addView(child, -1, child.getLayoutParams());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addView(@NonNull View child, int index) {
        this.addView(child, index, child.getLayoutParams());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addView(@NonNull View child, int width, int height) {
        ViewGroup.LayoutParams params = child.getLayoutParams();
        params.width = width;
        params.height = height;
        this.addView(child, params);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addView(@NonNull View child, ViewGroup.LayoutParams params) {
        this.addView(child, -1, params);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addView(@NonNull View child, int index, ViewGroup.LayoutParams params) {
        if (mContent.getChildCount() > 0) {
            throw new IllegalStateException("Drawer can host only one direct child");
        }

        mContent.addView(child, index, params);
    }

    @SuppressWarnings("deprecation")
    private void setup(Context context, TypedArray array) {
        mDrawerListeners = new ArrayList<>();
        mWrapper = new LinearLayout(context);
        mContent = new ScrollView(context);
        mHandle = new FrameLayout(context);
        LinearLayout handleWrapper = new LinearLayout(context);
        LinearLayout handleContainer = new LinearLayout(context);

        mContent.setVerticalScrollBarEnabled(false);
        mContent.setHorizontalScrollBarEnabled(false);

        final int handleId, initialState, openColor, closedColor;
        Drawable openDrawable, closedDrawable;
        String openString, closedString;
        final float handleWidth, handleHeight, handleTextSize, peek;

        try {
            openDrawable = array.getDrawable(R.styleable.Drawer_drawerOpen);
        } catch (Resources.NotFoundException e) {
            openDrawable = null;
        }
        try {
            closedDrawable = array.getDrawable(R.styleable.Drawer_drawerClosed);
        } catch (Resources.NotFoundException e) {
            closedDrawable = null;
        }
        try {
            openString = array.getString(R.styleable.Drawer_drawerOpen);
        } catch (Resources.NotFoundException e) {
            openString = null;
        }
        try {
            closedString = array.getString(R.styleable.Drawer_drawerClosed);
        } catch (Resources.NotFoundException e) {
            closedString = null;
        }

        openColor = array.getColor(R.styleable.Drawer_drawerOpenColor, 0x000000);
        closedColor = array.getColor(R.styleable.Drawer_drawerClosedColor, 0x000000);

        mHandleBackground = array.getDrawable(R.styleable.Drawer_drawerHandle);

        handleWidth = array.getDimension(R.styleable.Drawer_drawerHandleWidth, -2);
        handleHeight = array.getDimension(R.styleable.Drawer_drawerHandleHeight, -2);
        handleTextSize = array.getDimension(R.styleable.Drawer_drawerHandleTextSize, 10);
        handleId = array.getResourceId(R.styleable.Drawer_drawerHandleId, 0);
        peek = array.getDimension(R.styleable.Drawer_drawerPeek, 0);
        mSlideDuration = array.getInt(R.styleable.Drawer_drawerSlideDuration, 250);

        initialState = array.getInt(R.styleable.Drawer_drawerInitialState, 0);

        mOpenHandle = new View(context);
        if (openDrawable != null) {
            openDrawable.setColorFilter(openColor, PorterDuff.Mode.DST);
            ImageView openView = new ImageView(context);
            openView.setImageDrawable(openDrawable);
            mOpenHandle = openView;
        } else if (openString != null) {
            TextView openView = new TextView(context);
            openView.setTextColor(openColor);
            openView.setText(openString);
            openView.setTextSize(TypedValue.COMPLEX_UNIT_PX, handleTextSize);
            mOpenHandle = openView;
        }

        mClosedHandle = new View(context);
        if (closedDrawable != null) {
            closedDrawable.setColorFilter(closedColor, PorterDuff.Mode.DST);
            ImageView closedView = new ImageView(context);
            closedView.setImageDrawable(closedDrawable);
            mClosedHandle = closedView;
        } else if (closedString != null) {
            TextView closedView = new TextView(context);
            closedView.setTextColor(closedColor);
            closedView.setText(closedString);
            closedView.setTextSize(TypedValue.COMPLEX_UNIT_PX, handleTextSize);
            mClosedHandle = closedView;
        }

        MarginLayoutParams openHandleParams = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.CENTER);
        openHandleParams.topMargin = (int) handleTextSize / 5 * -1;
        MarginLayoutParams closedHandleParams = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.CENTER);
        closedHandleParams.topMargin = (int) handleTextSize / 5 * -1;
        mOpenHandle.setLayoutParams(openHandleParams);
        mClosedHandle.setLayoutParams(closedHandleParams);

        LayoutParams handleParams = new LayoutParams((int) handleWidth, (int) handleHeight);
        handleParams.gravity = Gravity.CENTER_HORIZONTAL;
        mHandle.setLayoutParams(handleParams);
        if (Build.VERSION.SDK_INT >= 16) {
            mHandle.setBackground(mHandleBackground);
        } else {
            mHandle.setBackgroundDrawable(mHandleBackground);
        }
        mHandle.addView(mOpenHandle);
        mHandle.addView(mClosedHandle);
        mHandle.setId(handleId);
        mHandle.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (mGestureDetector.onTouchEvent(event)) {
                    return true;
                }

                switch (event.getAction()) {
                    case MotionEvent.ACTION_UP:
                        mDragging = false;
                        mHandle.setPressed(false);
                        mTranslationStart = (int) mWrapper.getTranslationY();
                        if (mTranslationStart > mMaxTranslation / -2) {
                            open();
                        } else {
                            close();
                        }
                        break;
                }
                return false;
            }
        });

        handleContainer.setOrientation(LinearLayout.HORIZONTAL);
        handleContainer.addView(mHandle);

        LayoutParams handleWrapperParams = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        handleWrapperParams.gravity = Gravity.CENTER_HORIZONTAL;
        // Prevents thin gray line from showing up because of slightly incorrect draw
        handleWrapperParams.topMargin = -2;
        handleWrapper.setLayoutParams(handleWrapperParams);
        handleWrapper.setOrientation(VERTICAL);
        handleWrapper.addView(handleContainer);
        handleWrapper.setClickable(true);

        mWrapper.setOrientation(VERTICAL);
        mWrapper.addView(mContent, -1,
                new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        mWrapper.addView(handleWrapper, -1, handleWrapper.getLayoutParams());

        super.addView(mWrapper, -1,
                new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            @SuppressWarnings("deprecation")
            public void onGlobalLayout() {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                    getViewTreeObserver().removeOnGlobalLayoutListener(this);
                } else {
                    getViewTreeObserver().removeGlobalOnLayoutListener(this);
                }
                mMaxTranslation = mContent.getHeight() - peek;
                if (initialState == 0) {
                    close(false, false);
                } else {
                    open(false, false);
                }
            }
        });

        mGestureDetector = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() {
            private float mTotalDelta = 0;

            @Override
            public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
                mDragging = true;
                mVelocity = 0;
                mTotalDelta += distanceY;
                if (distanceY < 0) {
                    mDrawerState = DrawerState.CLOSING;
                } else {
                    mDrawerState = DrawerState.OPENING;
                }
                float newPosition = mWrapper.getTranslationY() - mTotalDelta;
                if (newPosition > 0) {
                    newPosition = 0;
                } else if (newPosition < -1 * mMaxTranslation) {
                    newPosition = -1 * mMaxTranslation;
                }
                toggleEnabled(true, mContent);
                setTranslationPercent(1 - (-1 * newPosition / mMaxTranslation));
                mWrapper.requestLayout();
                return true;
            }

            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                mDragging = false;
                mVelocity = velocityY;
                mHandle.setPressed(false);
                mTranslationStart = (int) mWrapper.getTranslationY();
                switch (mDrawerState) {
                    case OPENING:
                        open();
                        break;
                    case CLOSING:
                        close();
                        break;
                }
                return true;
            }

            @Override
            public boolean onSingleTapUp(MotionEvent e) {
                if (!mDragging) {
                    toggle();
                }
                mDragging = false;
                mHandle.setPressed(false);
                return true;
            }

            @Override
            public boolean onDown(MotionEvent e) {
                mHandle.setPressed(true);
                return true;
            }
        });

        array.recycle();
    }

    /**
     * Sets the open handle to the provided view
     */
    @SuppressWarnings({ "unused", "deprecation" })
    public void setOpenHandle(Drawable d) {
        if (Build.VERSION.SDK_INT >= 16) {
            mHandle.setBackground(mHandleBackground);
        } else {
            mHandle.setBackgroundDrawable(mHandleBackground);
        }
        mHandleBackground = d;
        invalidate();
        requestLayout();
    }

    /**
     * Sets the open handle to the provided view
     */
    @SuppressWarnings("unused")
    public void setOpenHandle(View view) {
        mHandle.removeView(mOpenHandle);
        mOpenHandle = view;
        mHandle.addView(mOpenHandle);
        invalidate();
        requestLayout();
    }

    /**
     * Sets the closed handle to the provided view
     */
    @SuppressWarnings("unused")
    public void setClosedHandle(View view) {
        mHandle.removeView(mClosedHandle);
        mClosedHandle = view;
        mHandle.addView(mClosedHandle);
        invalidate();
        requestLayout();
    }

    /**
     * Gets the drawer state
     *
     * @return OPEN, CLOSED, OPENING, or CLOSING
     */
    @SuppressWarnings("unused")
    public DrawerState getDrawerState() {
        return mDrawerState;
    }

    /**
     * @return The slide duration in milliseconds
     */
    @SuppressWarnings("unused")
    public int getSlideDuration() {
        return mSlideDuration;
    }

    /**
     * Open the drawer
     */
    public void open() {
        open(true, true);
    }

    /**
     * Open the drawer
     *
     * @param animate          True to animate drawer, false to snap
     * @param triggerListeners Determines whether or not the drawer state listeners are triggered
     */
    private void open(boolean animate, final boolean triggerListeners) {
        if (mDrawerState == DrawerState.OPEN) {
            return;
        }

        mTranslationStart = (int) mWrapper.getTranslationY();
        mVelocity = 0;

        if (mDrawerListeners.size() > 0) {
            for (DrawerListener listener : mDrawerListeners) {
                listener.onOpen();
            }
        }

        toggleEnabled(true, mContent);
        if (animate) {
            changeDrawerState(DrawerState.OPENING, triggerListeners);
            ObjectAnimator animator = new ObjectAnimator();
            animator.addListener(new Animator.AnimatorListener() {
                @Override
                public void onAnimationStart(Animator animation) { }

                @Override
                public void onAnimationEnd(Animator animation) {
                    changeDrawerState(DrawerState.OPEN, triggerListeners);
                }

                @Override
                public void onAnimationCancel(Animator animation) { }

                @Override
                public void onAnimationRepeat(Animator animation) { }
            });
            float translationPercent = 1 - (-1 * mTranslationStart / mMaxTranslation);
            animator.setFloatValues(translationPercent, 1);
            animator.setPropertyName("translationPercent");
            if (translationPercent == 1) {
                animator.setDuration(0);
            } else {
                animator.setDuration(getDuration(mSlideDuration));
            }
            animator.setTarget(this);
            animator.start();
        } else {
            setTranslationPercent(1);
            changeDrawerState(DrawerState.OPEN, triggerListeners);
        }
    }

    /**
     * Close the drawer
     */
    public void close() {
        close(true, true);
    }

    /**
     * Close the drawer
     *
     * @param animate          True to animate drawer, false to snap
     * @param triggerListeners Determines whether or not the drawer state listeners are triggered
     */
    public void close(boolean animate, final boolean triggerListeners) {
        if (mDrawerState == DrawerState.CLOSED) {
            return;
        }

        mTranslationStart = (int) mWrapper.getTranslationY();
        mVelocity = 0;

        if (mDrawerListeners.size() > 0) {
            for (DrawerListener listener : mDrawerListeners) {
                listener.onClose();
            }
        }

        clearFocus();

        if (animate) {
            changeDrawerState(DrawerState.CLOSING, triggerListeners);
            ObjectAnimator animator = new ObjectAnimator();
            animator.addListener(new Animator.AnimatorListener() {
                @Override
                public void onAnimationStart(Animator animation) { }

                @Override
                public void onAnimationEnd(Animator animation) {
                    changeDrawerState(DrawerState.CLOSED, triggerListeners);
                    toggleEnabled(false, mContent);
                }

                @Override
                public void onAnimationCancel(Animator animation) { }

                @Override
                public void onAnimationRepeat(Animator animation) { }
            });
            float translationPercent = 1 - (-1 * mTranslationStart / mMaxTranslation);
            animator.setFloatValues(translationPercent, 0);
            animator.setPropertyName("translationPercent");
            if (translationPercent < 0.01) {
                animator.setDuration(0);
            } else {
                animator.setDuration(getDuration(mSlideDuration));
            }
            animator.setTarget(this);
            animator.start();
        } else {
            setTranslationPercent(0);
            changeDrawerState(DrawerState.CLOSED, triggerListeners);
            toggleEnabled(false, mContent);
        }
    }

    private int getDuration(int baseTime) {
        if (mVelocity == 0) {
            return baseTime;
        }

        return Math.min((int) Math.abs(baseTime / (mVelocity / mTranslationStart)), baseTime);
    }

    /**
     * Toggles the drawer based on its current state
     */
    public void toggle() {
        switch (mDrawerState) {
            case OPEN:
                close();
                break;
            case CLOSED:
                open();
                break;
            case OPENING:
                break;
            case CLOSING:
                break;
        }
    }

    private void changeDrawerState(DrawerState state, boolean triggerListeners) {
        mDrawerState = state;
        if (mDrawerListeners.size() > 0 && triggerListeners) {
            for (DrawerListener listener : mDrawerListeners) {
                listener.onDrawerStateChanged(state);
            }
        }
    }

    /**
     * Sets the drawer's current translation state: 1 is open, 0 is closed, 1 > x > 0 is somewhere in between
     *
     * @param translationPercent Value between 1 and 0 inclusive
     */
    public void setTranslationPercent(float translationPercent) {
        mWrapper.setTranslationY(mMaxTranslation * -1 * (1 - translationPercent));
        if (translationPercent > 0.5) {
            mClosedHandle.setAlpha(0);
            mOpenHandle.setAlpha(1);
        } else {
            mOpenHandle.setAlpha(0);
            mClosedHandle.setAlpha(1);
        }

        if (mDrawerListeners.size() > 0) {
            for (DrawerListener listener : mDrawerListeners) {
                listener.onVisiblePercentChange(translationPercent);
            }
        }
    }

    /**
     * Add a drawer listener
     */
    @SuppressWarnings("unused")
    public void addDrawerListener(DrawerListener listener) {
        mDrawerListeners.add(listener);
    }

    /**
     * Remove drawer listener
     *
     * @return true if the listener was removed, false if it was not in the list
     */
    @SuppressWarnings("unused")
    public boolean removeDrawerListener(DrawerListener listener) {
        return mDrawerListeners.remove(listener);
    }

    public enum DrawerState {
        OPEN, CLOSED, OPENING, CLOSING
    }

    public interface DrawerListener {
        void onVisiblePercentChange(float percent);
        void onDrawerStateChanged(DrawerState state);
        void onOpen();
        void onClose();
    }

    private static void toggleEnabled(boolean enable, ViewGroup vg) {
        for (int i = 0; i < vg.getChildCount(); i++) {
            View child = vg.getChildAt(i);
            child.setEnabled(enable);
            if (child instanceof ViewGroup) {
                toggleEnabled(enable, (ViewGroup) child);
            }
        }
    }
}

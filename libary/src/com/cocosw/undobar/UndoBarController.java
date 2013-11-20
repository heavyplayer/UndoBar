/*
 * Copyright 2012 Roman Nurik
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.cocosw.undobar;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.TextView;

public class UndoBarController extends FrameLayout {
	/*
	 * Default UndoBar styles.
	 */
	public static UndoBarStyle UNDOSTYLE = new UndoBarStyle(R.drawable.ic_undobar_undo, R.string.undo);
	public static UndoBarStyle RETRYSTYLE = new UndoBarStyle(R.drawable.ic_retry, R.string.retry, -1);
	public static UndoBarStyle MESSAGESTYLE = new UndoBarStyle(-1, -1, 5000);

	public interface UndoListener {
		void onUndo(Parcelable token);
	}

	private final TextView mMessageView;
    private final TextView mButton;

	private UndoListener mUndoListener;
	private UndoBarStyle mStyle;

	private Animation mFadeInAnimation;
	private Animation mFadeOutAnimation;

	private boolean mImmediate = false;
	private boolean mDismissOnOutsideTouch = false;
	private Parcelable mUndoToken;

	// Used to control whether touches were in or out the undo bar.
	private boolean mDispatchedTouchEvent = false;

	private final Handler mHideHandler = new Handler();
	private final Runnable mHideRunnable = new Runnable() {
		@Override
		public void run() {
			hideUndoBar(false);
		}
	};

	public UndoBarController(final Context context, final AttributeSet attrs) {
		super(context, attrs);
		// Important to find it in the view tree and to save and restore instance state.
		setId(R.id._undobar_controller);

		LayoutInflater.from(context).inflate(R.layout.undobar, this, true);
		mMessageView = (TextView) findViewById(R.id.undobar_message);
        mButton = (TextView) findViewById(R.id.undobar_button);
        mButton.setOnClickListener(
		        new View.OnClickListener() {
			        @Override
			        public void onClick(final View view) {
				        if (mUndoListener != null) {
					        mUndoListener.onUndo(mUndoToken);
				        }
				        hideUndoBar(false);
			        }
		        });

		// Start hidden.
		setVisibility(View.GONE);
	}

	protected void setStyle(UndoBarStyle style) {
		if(style != null && !style.equals(mStyle)) {
			mStyle = style;

			final String buttonText;
			if(mStyle.titleRes > 0 &&
				(buttonText = getResources().getString(mStyle.titleRes)) != null) {

				mButton.setVisibility(View.VISIBLE);
				mButton.setText(buttonText.toUpperCase());
				mButton.setCompoundDrawablesWithIntrinsicBounds(getResources()
						.getDrawable(mStyle.iconRes), null, null, null);

				// Change button background, but preserve the padding.
				final int paddingLeft, paddingRight;
				paddingLeft = mButton.getPaddingLeft();
				paddingRight = mButton.getPaddingRight();
				mButton.setBackgroundResource(mStyle.buttonBgRes);
				mButton.setPadding(paddingLeft, 0, paddingRight, 0);

				// Show divider.
				findViewById(R.id.undobar_divider).setVisibility(View.VISIBLE);
			}
			else {
				mButton.setVisibility(View.GONE);
				findViewById(R.id.undobar_divider).setVisibility(View.GONE);
			}
			findViewById(R.id.undobar).setBackgroundResource(mStyle.bgRes);
		}
	}

	protected void setUndoListener(final UndoListener undoListener) {
		mUndoListener = undoListener;
	}

	public UndoListener getUndoListener() {
		return mUndoListener;
	}

	protected void showUndoBar(final boolean immediate, final boolean dismissOnOutsideTouch, final CharSequence message,
	                           final Parcelable undoToken) {
		mUndoToken = undoToken;
		mMessageView.setText(message);

		mHideHandler.removeCallbacks(mHideRunnable);
		if(mStyle.duration > 0)
			mHideHandler.postDelayed(mHideRunnable, mStyle != null ? mStyle.duration : UndoBarStyle.DEFAULT_DURATION);

		final Animation showAnimation;
		if(!immediate &&
			(showAnimation = onCreateShowAnimation()) != null) {
			clearAnimation();
			startAnimation(showAnimation);
		}
		setVisibility(View.VISIBLE);

		mImmediate = immediate;
		mDismissOnOutsideTouch = dismissOnOutsideTouch;
	}

	protected Animation onCreateShowAnimation() {
		if(mFadeInAnimation == null) {
			final Context context = getContext();
			mFadeInAnimation = context != null ?
					AnimationUtils.loadAnimation(getContext(), android.R.anim.fade_in) :
					null;
		}
		return mFadeInAnimation;
	}

	private void hideUndoBar(final boolean immediate) {
		mHideHandler.removeCallbacks(mHideRunnable);
		mUndoToken = null;

		final Animation hideAnimation;
		if(!immediate &&
			(hideAnimation = onCreateHideAnimation()) != null) {
			clearAnimation();
			startAnimation(hideAnimation);
		}
		setVisibility(View.GONE);
	}

	protected Animation onCreateHideAnimation() {
		if(mFadeOutAnimation == null) {
			final Context context = getContext();
			mFadeOutAnimation = context != null ?
					AnimationUtils.loadAnimation(getContext(), android.R.anim.fade_out) :
					null;
		}
		return mFadeOutAnimation;
	}

	@Override
	protected void onAttachedToWindow() {
		ensureOutsideTouchLayout();
		super.onAttachedToWindow();
	}

	private void ensureOutsideTouchLayout() {
		final ViewGroup rootView = (ViewGroup)getRootView();
		if(rootView != null && rootView.findViewById(R.id._undobar_outside_touch_layout) == null) {
			// Inject the outside touch layout, if it is not already present.

			// Initialize outside touch layout.
			final ViewGroup outsideTouchLayout = new DismissOnOutsideTouchFrameLayout(getContext());

			final int rootCount = rootView.getChildCount();
			for(int i = 0; i < rootCount; i++) {
				final View child = rootView.getChildAt(i);
				if(child != null) {
					rootView.removeViewAt(i);

					outsideTouchLayout.addView(child);
				}
			}

			rootView.addView(outsideTouchLayout);
		}
	}

	private class DismissOnOutsideTouchFrameLayout extends FrameLayout {
		public DismissOnOutsideTouchFrameLayout(Context context) {
			super(context);
			setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
			setId(R.id._undobar_outside_touch_layout);
		}

		@Override
		public boolean dispatchTouchEvent(MotionEvent ev) {
			if(mDismissOnOutsideTouch) {
				final int action = ev.getAction();
				if(action == MotionEvent.ACTION_DOWN) {
					if(UndoBarController.this.getVisibility() == View.VISIBLE) {
						// We clear the controller's flag for dispatched touch events.
						mDispatchedTouchEvent = false;
						final boolean dispatched = super.dispatchTouchEvent(ev);

						// After dispatching all touches, if the undo bar didn't dispatch any events,
						// it means the motion event was actually outside of the undo bar.
						if(!mDispatchedTouchEvent) {
							mDismissOnOutsideTouch = false; // Stop dismissing, since we are hiding the undo bar.
							hide((ViewGroup)getRootView(), mImmediate);
						}

						return dispatched;
					}
					else {
						// Stop dismissing, since the undo bar is already hidden.
						mDismissOnOutsideTouch = false;
					}
				}
			}

			return super.dispatchTouchEvent(ev);
		}
	}

	@Override
	public boolean dispatchTouchEvent(MotionEvent ev) {
		mDispatchedTouchEvent = super.dispatchTouchEvent(ev);
		return mDispatchedTouchEvent;
	}

	@Override
	public Parcelable onSaveInstanceState() {
		SavedState ss = new SavedState(super.onSaveInstanceState());
		// Save visibility, token and style.
		ss.visibility = getVisibility();
		ss.immediate = mImmediate;
		ss.dismissOnOutsideTouch = mDismissOnOutsideTouch;
		ss.style = mStyle;
		ss.token = mUndoToken;

		return ss;
	}

	@Override
	public void onRestoreInstanceState(Parcelable state) {
		if(!(state instanceof SavedState)) {
			super.onRestoreInstanceState(state);
			return;
		}

		SavedState ss = (SavedState)state;
		super.onRestoreInstanceState(ss.getSuperState());

		// Restore visibility.
		if(getVisibility() != ss.visibility)
			setVisibility(ss.visibility);

		// Restore dismiss variables.
		mImmediate = ss.immediate;
		mDismissOnOutsideTouch = ss.dismissOnOutsideTouch;

		// Restore style.
		setStyle(ss.style);

		// Restore token.
		mUndoToken = ss.token;
	}

	static class SavedState extends BaseSavedState {
		int visibility;
		boolean immediate;
		boolean dismissOnOutsideTouch;
		UndoBarStyle style;
		Parcelable token;

		public SavedState(Parcelable superState) {
			super(superState);
		}

		@Override
		public void writeToParcel(Parcel dest, int flags) {
			super.writeToParcel(dest, flags);
			dest.writeInt(visibility);
			dest.writeInt(immediate ? 1 : 0);
			dest.writeInt(dismissOnOutsideTouch ? 1 : 0);
			dest.writeParcelable(style, 0);

			// Marshall token.
			final boolean hasToken = token != null;
			dest.writeInt(hasToken ? 1 : 0);
			if(hasToken) {
				dest.writeString(token.getClass().getName());
				dest.writeParcelable(token, 0);
			}
		}

		private SavedState(Parcel source) {
			super(source);
			visibility = source.readInt();
			immediate = source.readInt() == 1;
			dismissOnOutsideTouch = source.readInt() == 1;
			style = source.readParcelable(UndoBarStyle.class.getClassLoader());

			// Unmarshall token.
			final boolean hasToken = source.readInt() == 1;
			if(hasToken) {
				try {
					token = source.readParcelable(Class.forName(source.readString()).getClassLoader());
				} catch (ClassNotFoundException e) {
					throw new IllegalStateException("Undo token class not found.");
				}
			}
		}

		public static final Parcelable.Creator<SavedState> CREATOR = new Parcelable.Creator<SavedState>() {
			@Override
			public SavedState createFromParcel(Parcel source) {
				return new SavedState(source);
			}

			@Override
			public SavedState[] newArray(int size) {
				return new SavedState[size];
			}
		};
	}

	/**
	 * Quick method to initialize the UndoBar into an Activity.
	 * Usually called inside Activity's onCreate() method.
	 *
	 * @param activity Activity to hold this view.
	 * @param listener Callback listener triggered after click undo bar.
	 * @param style {@link UndoBarStyle}
	 *
	 * @return the created/configured UndoBarController.
	 */
	public static UndoBarController setup(final Activity activity, final UndoBarStyle style,
	                                      final UndoListener listener) {
		return setup(ensureView(activity), style, listener);
	}

	/**
	 * Quick method to initialize the UndoBar inside a ViewGroup.
	 *
	 * If you want to setup it inside a fragment, make sure you call this inside onCreateView() method
	 * using the view that was created, in order to properly save and restore instance state.
	 *
	 * @param container The group that will contain the UndoBar.
	 * @param listener Callback listener triggered after click undo bar.
	 * @param style {@link UndoBarStyle}
	 *
	 * @return the created/configured UndoBarController.
	 */
	public static UndoBarController setup(final ViewGroup container, final UndoBarStyle style,
	                                      final UndoListener listener) {
		return setup(ensureView(container), style, listener);
	}

	/**
	 * Initialize the UndoBar's style and listener.
	 */
	private static UndoBarController setup(final UndoBarController undo, final UndoBarStyle style,
	                                       final UndoListener listener) {
		undo.setUndoListener(listener);
		undo.setStyle(style);
		return undo;
	}

	private static UndoBarController ensureView(final Activity activity) {
		final ViewGroup decorView = (ViewGroup)activity.getWindow().getDecorView();
		final ViewGroup contentView = (ViewGroup)decorView.findViewById(android.R.id.content);
		// Try to inject the view in the activity's content view. Otherwise, inject it directly in the decor view.
		return ensureView(contentView != null ? contentView : decorView);
	}
	private static UndoBarController ensureView(final ViewGroup container) {
		UndoBarController undo = (UndoBarController)container.findViewById(R.id._undobar_controller);
		if(undo == null) {
			// Create the undo bar controller as it doesn't already exist.
			undo = new UndoBarController(container.getContext(), null);
			container.addView(undo);
		}
		return undo;
	}

	/**
	 * Quick method to show a UndoBar into an Activity.
	 *
	 * @param activity Activity to hold this view.
	 * @param message The message will be shown in left side in undo bar.
	 * @param immediate Show undo bar immediately or show it with animation.
	 * @param dismissOnOutsideTouch Dismiss undo bar if user clicks outside of it.
	 * @param undoToken Token info,will pass to callback to help you to undo.
	 *
	 * @return the shown UndoBarController.
	 */
	public static UndoBarController show(final Activity activity, final CharSequence message, final boolean immediate,
	                                     final boolean dismissOnOutsideTouch, final Parcelable undoToken) {
		final UndoBarController undo = UndoBarController.ensureView(activity);
		undo.showUndoBar(immediate, dismissOnOutsideTouch, message, undoToken);
		return undo;
	}

	/**
	 * Quick method to show a UndoBar inside a ViewGroup.
	 * Use this method if you want to show it inside a Fragment's view.
	 *
	 * @param container The ViewGroup that will contain the UndoBar.
	 * @param message The message will be shown in left side in undo bar.
	 * @param immediate Show undo bar immediately or show it with animation.
	 * @param dismissOnOutsideTouch Dismiss undo bar if user clicks outside of it.
	 * @param undoToken Token info,will pass to callback to help you to undo.
	 *
	 * @return the shown UndoBarController.
	 */
	public static UndoBarController show(final ViewGroup container, final CharSequence message, final boolean immediate,
	                                     final boolean dismissOnOutsideTouch, final Parcelable undoToken) {
		final UndoBarController undo = UndoBarController.ensureView(container);
		undo.showUndoBar(immediate, dismissOnOutsideTouch, message, undoToken);
		return undo;
	}

	public static void hide(final Activity activity, final boolean immediate) {
		hide((ViewGroup)activity.getWindow().getDecorView(), immediate);
	}
	public static void hide(final ViewGroup container, final boolean immediate) {
		final UndoBarController undo = (UndoBarController)container.findViewById(R.id._undobar_controller);
		if (undo != null) {
			undo.hideUndoBar(immediate);
		}
	}
}

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
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnticipateOvershootInterpolator;
import android.view.animation.OvershootInterpolator;
import android.view.animation.TranslateAnimation;
import android.widget.FrameLayout;
import android.widget.TextView;

public class UndoBarController extends FrameLayout {
	private static final String UNDO_TOKEN_TAG = ":undo_token";
	private static final String STYLE_TAG = ":style";

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

	private Parcelable mUndoToken;

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

			if(mStyle.titleRes > 0) {
				mButton.setVisibility(View.VISIBLE);
				findViewById(R.id.undobar_divider).setVisibility(View.VISIBLE);
				mButton.setText(mStyle.titleRes);
				mButton.setCompoundDrawablesWithIntrinsicBounds(getResources()
						.getDrawable(mStyle.iconRes), null, null, null);

				// Change button background, but preserve the padding.
				final int paddingLeft, paddingRight;
				paddingLeft = mButton.getPaddingLeft();
				paddingRight = mButton.getPaddingRight();
				mButton.setBackgroundResource(mStyle.buttonBgRes);
				mButton.setPadding(paddingLeft, 0, paddingRight, 0);
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

	private void hideUndoBar(final boolean immediate) {
		mHideHandler.removeCallbacks(mHideRunnable);
		mUndoToken = null;
		if (immediate) {
			setVisibility(View.GONE);
		} else {
			clearAnimation();
			startAnimation(UndoBarController.outToBottomAnimation(null));
			setVisibility(View.GONE);
		}
	}

	private static Animation outToBottomAnimation(
			final android.view.animation.Animation.AnimationListener animationlistener) {
		final TranslateAnimation translateanimation = new TranslateAnimation(2,
				0F, 2, 0F, 2, 0F, 2, 1F);
		translateanimation.setDuration(500L);
		translateanimation.setInterpolator(new AnticipateOvershootInterpolator(
				1.0f));
		translateanimation.setAnimationListener(animationlistener);
		return translateanimation;
	}

	protected void showUndoBar(final boolean immediate,
			final CharSequence message, final Parcelable undoToken) {
		mUndoToken = undoToken;
		mMessageView.setText(message);

		mHideHandler.removeCallbacks(mHideRunnable);
		if(mStyle.duration > 0)
			mHideHandler.postDelayed(mHideRunnable, mStyle != null ? mStyle.duration : UndoBarStyle.DEFAULT_DURATION);

		if (!immediate) {
			clearAnimation();
			startAnimation(UndoBarController.inFromBottomAnimation(null));
		}
		setVisibility(View.VISIBLE);
	}

	private static Animation inFromBottomAnimation(
			final android.view.animation.Animation.AnimationListener animationlistener) {
		final TranslateAnimation translateanimation = new TranslateAnimation(2,
				0F, 2, 0F, 2, 1F, 2, 0F);
		translateanimation.setDuration(500L);
		translateanimation.setInterpolator(new OvershootInterpolator(1.0f));
		translateanimation.setAnimationListener(animationlistener);
		return translateanimation;
	}

	@Override
	public Parcelable onSaveInstanceState() {
		SavedState ss = new SavedState(super.onSaveInstanceState());
		// Save visibility, token and style.
		ss.visibility = getVisibility();
		ss.bundle = new Bundle();
		ss.bundle.putParcelable(STYLE_TAG, mStyle);
		ss.bundle.putParcelable(UNDO_TOKEN_TAG, mUndoToken);

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

		// Restore style.
		setStyle(ss.bundle.<UndoBarStyle>getParcelable(STYLE_TAG));

		// Restore token.
		mUndoToken = ss.bundle.getParcelable(UNDO_TOKEN_TAG);
	}

	static class SavedState extends BaseSavedState {
		int visibility;
		Bundle bundle;

		public SavedState(Parcelable superState) {
			super(superState);
		}

		@Override
		public void writeToParcel(Parcel dest, int flags) {
			super.writeToParcel(dest, flags);
			dest.writeInt(visibility);
			dest.writeBundle(bundle);
		}

		private SavedState(Parcel source) {
			super(source);
			visibility = source.readInt();
			bundle = source.readBundle();
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
	 * @param listener Callback listener triggered after click undobar.
	 * @param style {@link UndoBarStyle}
	 *
	 * @return the created/configured UndoBarController.
	 */
	public static UndoBarController setup(final Activity activity, final UndoBarStyle style, final UndoListener listener) {
		return setup(ensureView(activity), style, listener);

	}

	/**
	 * Quick method to initialize the UndoBar inside a ViewGroup.
	 *
	 * If you want to setup it inside a fragment, make sure you call this inside onCreateView() method
	 * using the view that was created, in order to properly save and restore instance state.
	 *
	 * @param group The group that will contain the UndoBar.
	 * @param listener Callback listener triggered after click undobar.
	 * @param style {@link UndoBarStyle}
	 *
	 * @return the created/configured UndoBarController.
	 */
	public static UndoBarController setup(final ViewGroup group, final UndoBarStyle style,
	                                      final UndoListener listener) {
		return setup(ensureView(group), style, listener);
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

	/**
	 * Quick method to show a UndoBar into an Activity.
	 *
	 * @param activity Activity to hold this view.
	 * @param message The message will be shown in left side in undobar.
	 * @param undoToken Token info,will pass to callback to help you to undo.
	 * @param immediate Show undobar immediately or show it with animation.
	 *
	 * @return the shown UndoBarController.
	 */
	public static UndoBarController show(final Activity activity, final CharSequence message,
	                                     final boolean immediate, final Parcelable undoToken) {
		final UndoBarController undo = UndoBarController.ensureView(activity);
		undo.showUndoBar(immediate, message, undoToken);
		return undo;
	}

	/**
	 * Quick method to show a UndoBar inside a ViewGroup.
	 * Use this method if you want to show it inside a Fragment's view.
	 *
	 * @param group The group that will contain the UndoBar.
	 * @param message The message will be shown in left side in undobar.
	 * @param undoToken Token info,will pass to callback to help you to undo.
	 * @param immediate Show undobar immediately or show it with animation.
	 *
	 * @return the shown UndoBarController.
	 */
	public static UndoBarController show(final ViewGroup group, final CharSequence message,
	                                     final boolean immediate, final Parcelable undoToken) {
		final UndoBarController undo = UndoBarController.ensureView(group);
		undo.showUndoBar(immediate, message, undoToken);
		return undo;
	}

	private static UndoBarController ensureView(final Activity activity) {
		final ViewGroup decorView = (ViewGroup)activity.getWindow().getDecorView();
		final ViewGroup contentView = (ViewGroup)decorView.findViewById(android.R.id.content);
		return ensureView(decorView, contentView);
	}
	private static UndoBarController ensureView(final ViewGroup view) {
		return ensureView(view, view);
	}

	/**
	 * If the view doesn't exist in the root view, add it to the container view.
	 */
	private static UndoBarController ensureView(final ViewGroup root, final ViewGroup container) {
		UndoBarController undo = (UndoBarController)root.findViewById(R.id._undobar_controller);
		if(undo == null) {
			// Create the undobar controller as it doesn't already exist.
			undo = new UndoBarController(root.getContext(), null);
			container.addView(undo);
		}
		return undo;
	}

	/**
	 * Hide the UndoBar immediately.
	 */
	public static void hide(final Activity activity) {
		final UndoBarController undo = (UndoBarController)activity.findViewById(R.id._undobar_controller);
		if (undo != null) {
			undo.setVisibility(View.GONE);
		}
	}
}

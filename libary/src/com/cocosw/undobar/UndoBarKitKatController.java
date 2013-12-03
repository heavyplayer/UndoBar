package com.cocosw.undobar;

import android.content.Context;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

public class UndoBarKitKatController extends UndoBarController {
	private View mButtonWrapper;
	private ImageView mUndoIcon;

	public UndoBarKitKatController(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	protected void inflateUndoBar(Context context) {
		final Typeface typeFace = Typeface.createFromAsset(context.getAssets(), "fonts/RobotoCondensed-Regular.ttf");

		LayoutInflater.from(context).inflate(R.layout.undobar_kitkat, this, true);

		mMessageView = (TextView) findViewById(R.id.undobar_message);
		mMessageView.setTypeface(typeFace);

		mButton = (TextView) findViewById(R.id.undobar_button);
		mButton.setTypeface(typeFace);

		mUndoIcon = (ImageView) findViewById(R.id.undobar_icon);

		mButtonWrapper = findViewById(R.id.undobar_button_wrapper);
		mButtonWrapper.setOnClickListener(
				new View.OnClickListener() {
					@Override
					public void onClick(final View view) {
						if (mUndoListener != null) {
							mUndoListener.onUndo(mUndoToken);
						}
						hideUndoBar(false);
					}
				});
	}

	protected void setStyle(UndoBarStyle style) {
		if(style != null && !style.equals(mStyle)) {
			mStyle = style;

			final String buttonText;
			if(mStyle.titleRes != UndoBarStyle.IGNORE_RESOURCE &&
				(buttonText = getResources().getString(mStyle.titleRes)) != null) {
				// Show button wrapper (divider, icon, button text).
				mButtonWrapper.setVisibility(View.VISIBLE);

				mButton.setText(buttonText);

				if(mStyle.iconRes != UndoBarStyle.IGNORE_RESOURCE)
					mUndoIcon.setImageResource(mStyle.iconRes);

				// Change button background, but preserve the padding.
				if(mStyle.buttonBgRes != UndoBarStyle.IGNORE_RESOURCE) {
					final int paddingLeft, paddingRight;
					paddingLeft = mButtonWrapper.getPaddingLeft();
					paddingRight = mButtonWrapper.getPaddingRight();
					mButtonWrapper.setBackgroundResource(mStyle.buttonBgRes);
					mButtonWrapper.setPadding(paddingLeft, 0, paddingRight, 0);
				}
			}
			else {
				// Hide button wrapper (divider, icon, button text).
				mButtonWrapper.setVisibility(View.GONE);
			}

			if(mStyle.bgRes != UndoBarStyle.IGNORE_RESOURCE)
				findViewById(R.id.undobar).setBackgroundResource(mStyle.bgRes);
		}
	}
}

package com.cocosw.undobar;

import com.cocosw.undobar.R.drawable;

public class UndoBarStyle {

	int iconRes;
	int titleRes;
	int buttonBgRes = drawable.undobar_button;
	int bgRes = drawable.undobar;
	long duration = 5000;

	public UndoBarStyle(final int icon, final int title) {
		iconRes = icon;
		titleRes = title;
	}

	public UndoBarStyle(final int icon, final int title, final int buttonBg) {
		this(icon, title);
		buttonBgRes = buttonBg;
	}

	public UndoBarStyle(final int icon, final int title, final int buttonBg, final long duration) {
		this(icon, title, buttonBg);
		this.duration = duration;
	}

	public UndoBarStyle(final int icon, final int title, final int buttonBg, final int bg, final long duration) {
		this(icon, title, buttonBg, duration);
		bgRes = bg;
	}

    @Override
    public String toString() {
        return "UndoBarStyle{" +
                "iconRes=" + iconRes +
                ", titleRes=" + titleRes +
                ", buttonBg=" + buttonBgRes +
                ", bgRes=" + bgRes +
                ", duration=" + duration +
                '}';
    }
}

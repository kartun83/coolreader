package org.coolreader.crengine;

import org.coolreader.R;
import org.coolreader.readerview.ReaderView;
import org.coolreader.utils.Utils;

import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.PopupWindow;
import android.widget.SeekBar;

public class FindNextDlg {
	PopupWindow mWindow;
	View mAnchor;
	int mAnchorHeight = 0;
	//CoolReader mCoolReader;
	ReaderView mReaderView;
	View mPanel;
	SeekBar mSeekBar;
	final String pattern;
	final boolean caseInsensitive;
	boolean isEInk = false;

	static public void showDialog(Bookmark fromPage, BaseActivity coolReader, ReaderView readerView, final String pattern, final boolean caseInsensitive, final boolean skim)
	{
		FindNextDlg dlg = new FindNextDlg(fromPage, coolReader, readerView, pattern, caseInsensitive, skim);
		//dlg.mWindow.update(dlg.mAnchor, width, height)
		Log.d("cr3", "popup: " + dlg.mWindow.getWidth() + "x" + dlg.mWindow.getHeight());
		//dlg.update();
		//dlg.showAtLocation(readerView, Gravity.LEFT|Gravity.TOP, readerView.getLeft()+50, readerView.getTop()+50);
		//dlg.showAsDropDown(readerView);
		//dlg.update();
	}

	public FindNextDlg(Bookmark fromPage, BaseActivity coolReader, ReaderView readerView, final String pattern, final boolean caseInsensitive, final boolean skim)
	{
		this.pattern = pattern;
		this.caseInsensitive = caseInsensitive;
		isEInk = DeviceInfo.isEinkScreen(BaseActivity.getScreenForceEink());
		//mCoolReader = coolReader;
		mReaderView = readerView;
		mAnchor = readerView.getSurface();
		mAnchorHeight = mAnchor.getHeight();

		View panel = (LayoutInflater.from(coolReader.getApplicationContext()).inflate(R.layout.search_popup, null));
		panel.measure(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
		
		//mReaderView.getS
		
		mWindow = new PopupWindow(mAnchor.getContext());
		mWindow.setTouchInterceptor((v, event) -> {
			if ( event.getAction()==MotionEvent.ACTION_OUTSIDE ) {
				mReaderView.clearSelection();
				mWindow.dismiss();
				return true;
			}
			return false;
		});
		//super(panel);
		mReaderView.mBookInfo.sortBookmarks();
		mPanel = panel;
		mPanel.findViewById(R.id.search_btn_prev).setOnTouchListener(new RepeatOnTouchListener(500, 150,
				v -> mReaderView.findNext(pattern, true, caseInsensitive)));
		mPanel.findViewById(R.id.search_btn_next).setOnTouchListener(new RepeatOnTouchListener(500, 150,
				v -> mReaderView.findNext(pattern, false, caseInsensitive)));
		mPanel.findViewById(R.id.search_btn_plus_1).setOnTouchListener(new RepeatOnTouchListener(500, 150,
				v -> mReaderView.onCommand(ReaderCommand.DCMD_PAGEDOWN, 1, null)));
		mPanel.findViewById(R.id.search_btn_plus_10).setOnTouchListener(new RepeatOnTouchListener(500, 150,
				v -> mReaderView.onCommand(ReaderCommand.DCMD_PAGEDOWN, 10, null)));
		mPanel.findViewById(R.id.search_btn_minus_1).setOnTouchListener(new RepeatOnTouchListener(500, 150,
				v -> mReaderView.onCommand(ReaderCommand.DCMD_PAGEUP, 1, null)));
		mPanel.findViewById(R.id.search_btn_minus_10).setOnTouchListener(new RepeatOnTouchListener(500, 150,
				v -> mReaderView.onCommand(ReaderCommand.DCMD_PAGEUP, 10, null)));
		mPanel.findViewById(R.id.search_btn_plus_ch).setOnTouchListener(new RepeatOnTouchListener(500, 150,
				v -> mReaderView.onCommand(ReaderCommand.DCMD_MOVE_BY_CHAPTER, 1, null)));
		mSeekBar = mPanel.findViewById(R.id.page_seek);
		if (mSeekBar != null) {
			mReaderView.getCurrentPositionProperties((props, positionText) -> {
				if (props == null) {
					Utils.hideView(mSeekBar);
					return;
				}
				mSeekBar.setMax(props.pageCount);
				mSeekBar.setProgress(props.pageNumber);
				mSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
					@Override
					public void onStopTrackingTouch(SeekBar seekBar) {
					}

					@Override
					public void onStartTrackingTouch(SeekBar seekBar) {
					}

					@Override
					public void onProgressChanged(SeekBar seekBar, int progress,
												  boolean fromUser) {
						if (fromUser) {
							try {
								mReaderView.goToPage(progress);
							} catch (Exception e) {
								// ignore
							}
						}
					}
				});
			});
		}
		mPanel.findViewById(R.id.search_btn_minus_ch).setOnTouchListener(new RepeatOnTouchListener(500, 150,
				v -> mReaderView.onCommand(ReaderCommand.DCMD_MOVE_BY_CHAPTER, -1, null)));
		mPanel.findViewById(R.id.search_btn_plus_bmk).setOnTouchListener(new RepeatOnTouchListener(500, 150,
				v -> mReaderView.onCommand(ReaderCommand.DCMD_NEXT_BOOKMARK, 1, null)));
		mPanel.findViewById(R.id.search_btn_minus_bmk).setOnTouchListener(new RepeatOnTouchListener(500, 150,
				v -> mReaderView.onCommand(ReaderCommand.DCMD_PREV_BOOKMARK, -1, null)));

		mPanel.findViewById(R.id.search_goback).setOnClickListener(v -> {
			mReaderView.goToBookmark(fromPage);
			mWindow.dismiss();
		});

		int colorGrayC;
		int colorGray;
		TypedArray a = mReaderView.getActivity().getTheme().obtainStyledAttributes(new int[]
				{R.attr.colorThemeGray2Contrast, R.attr.colorThemeGray2});
		colorGrayC = a.getColor(0, Color.GRAY);
		colorGray = a.getColor(1, Color.GRAY);
		a.recycle();

		ColorDrawable c = new ColorDrawable(colorGrayC);
		if (!isEInk) c.setAlpha(130);
		mPanel.findViewById(R.id.search_btn_prev).setBackground(c);
		if (isEInk) Utils.setBtnBackground(mPanel.findViewById(R.id.search_btn_prev), null, isEInk);
		mPanel.findViewById(R.id.search_btn_prev).setPadding(6, 15, 6, 15);
		mPanel.findViewById(R.id.search_btn_plus_1).setBackground(c);
		if (isEInk) Utils.setBtnBackground(mPanel.findViewById(R.id.search_btn_plus_1), null, isEInk);
		mPanel.findViewById(R.id.search_btn_plus_1).setPadding(6, 15, 6, 15);
		mPanel.findViewById(R.id.search_btn_plus_10).setBackground(c);
		if (isEInk) Utils.setBtnBackground(mPanel.findViewById(R.id.search_btn_plus_10), null, isEInk);
		mPanel.findViewById(R.id.search_btn_plus_10).setPadding(6, 15, 6, 15);
		mPanel.findViewById(R.id.search_btn_plus_ch).setBackground(c);
		if (isEInk) Utils.setBtnBackground(mPanel.findViewById(R.id.search_btn_plus_ch), null, isEInk);
		mPanel.findViewById(R.id.search_btn_plus_ch).setPadding(6, 15, 6, 15);
		mPanel.findViewById(R.id.search_btn_plus_bmk).setBackground(c);
		if (isEInk) Utils.setBtnBackground(mPanel.findViewById(R.id.search_btn_plus_bmk), null, isEInk);
		mPanel.findViewById(R.id.search_btn_plus_bmk).setPadding(6, 15, 6, 15);
		mPanel.findViewById(R.id.search_btn_next).setBackground(c);
		if (isEInk) Utils.setBtnBackground(mPanel.findViewById(R.id.search_btn_next), null, isEInk);
		mPanel.findViewById(R.id.search_btn_next).setPadding(6, 15, 6, 15);
		mPanel.findViewById(R.id.search_btn_minus_1).setBackground(c);
		if (isEInk) Utils.setBtnBackground(mPanel.findViewById(R.id.search_btn_minus_1), null, isEInk);
		mPanel.findViewById(R.id.search_btn_minus_1).setPadding(6, 15, 6, 15);
		mPanel.findViewById(R.id.search_btn_minus_10).setBackground(c);
		if (isEInk) Utils.setBtnBackground(mPanel.findViewById(R.id.search_btn_minus_10), null, isEInk);
		mPanel.findViewById(R.id.search_btn_minus_10).setPadding(6, 15, 6, 15);
		mPanel.findViewById(R.id.search_btn_minus_ch).setBackground(c);
		if (isEInk) Utils.setBtnBackground(mPanel.findViewById(R.id.search_btn_minus_ch), null, isEInk);
		mPanel.findViewById(R.id.search_btn_minus_ch).setPadding(6, 15, 6, 15);
		mPanel.findViewById(R.id.search_btn_minus_bmk).setBackground(c);
		if (isEInk) Utils.setBtnBackground(mPanel.findViewById(R.id.search_btn_minus_bmk), null, isEInk);
		mPanel.findViewById(R.id.search_btn_minus_bmk).setPadding(6, 15, 6, 15);

		mPanel.findViewById(R.id.search_goback).setPadding(6, 15, 6, 15);
		mPanel.findViewById(R.id.search_goback).setBackground(c);
		if (isEInk) Utils.setBtnBackground(mPanel.findViewById(R.id.search_goback), null, isEInk);

		if (skim) {
			Utils.hideView(mPanel.findViewById(R.id.search_btn_prev));
			Utils.hideView(mPanel.findViewById(R.id.search_btn_next));
		}

		mPanel.findViewById(R.id.search_btn_close).setOnClickListener(v -> {
			mReaderView.clearSelection();
			mWindow.dismiss();
		});
		mPanel.findViewById(R.id.search_btn_close).setBackground(c);
		if (isEInk) Utils.setBtnBackground(mPanel.findViewById(R.id.search_btn_close), null, isEInk);
		coolReader.tintViewIcons(mPanel,true);
		mPanel.setFocusable(true);
		mPanel.setOnKeyListener((v, keyCode, event) -> {
			if (event.getAction() == KeyEvent.ACTION_UP) {
				if (keyCode == 0)
					keyCode = event.getScanCode();
				keyCode = mReaderView.translateKeyCode(keyCode);
				switch (keyCode) {
					case KeyEvent.KEYCODE_BACK:
						mReaderView.clearSelection();
						mWindow.dismiss();
						return true;
					case KeyEvent.KEYCODE_DPAD_LEFT:
					case KeyEvent.KEYCODE_VOLUME_UP:
					case KeyEvent.KEYCODE_DPAD_UP:
						mReaderView.findNext(pattern, true, caseInsensitive);
						return true;
					case KeyEvent.KEYCODE_DPAD_RIGHT:
					case KeyEvent.KEYCODE_VOLUME_DOWN:
					case KeyEvent.KEYCODE_DPAD_DOWN:
						mReaderView.findNext(pattern, false, caseInsensitive);
						return true;
				}
			} else if (event.getAction() == KeyEvent.ACTION_DOWN) {
				switch (keyCode) {
					case KeyEvent.KEYCODE_BACK:
					case KeyEvent.KEYCODE_DPAD_LEFT:
					case KeyEvent.KEYCODE_DPAD_UP:
					case KeyEvent.KEYCODE_DPAD_RIGHT:
					case KeyEvent.KEYCODE_DPAD_DOWN:
					case KeyEvent.KEYCODE_VOLUME_UP:
					case KeyEvent.KEYCODE_VOLUME_DOWN:
						return true;
				}
			}
			return keyCode == KeyEvent.KEYCODE_BACK;
		});

		mWindow.setOnDismissListener(() -> mReaderView.clearSelection());
		
		mWindow.setBackgroundDrawable(new BitmapDrawable());
		mWindow.setWidth(WindowManager.LayoutParams.WRAP_CONTENT);
		mWindow.setHeight(WindowManager.LayoutParams.WRAP_CONTENT);

		mWindow.setFocusable(true);
		mWindow.setTouchable(true);
		mWindow.setOutsideTouchable(true);
		if (isEInk)
			panel.setBackgroundColor(Color.WHITE);
		else
			panel.setBackgroundColor(Color.argb(170, Color.red(colorGray),Color.green(colorGray),Color.blue(colorGray)));
		mWindow.setContentView(panel);
		mWindow.getContentView().setFocusableInTouchMode(true);
		mWindow.getContentView().requestFocus();
		
		mWindow.setWidth(mReaderView.getSurface().getWidth());
		mWindow.showAtLocation(mAnchor, Gravity.TOP | Gravity.CENTER_HORIZONTAL,
				mReaderView.locationFindNext[0],
				mAnchorHeight - mPanel.getHeight());
	}
	
}

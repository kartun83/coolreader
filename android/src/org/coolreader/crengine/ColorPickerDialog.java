package org.coolreader.crengine;

// based on color picker from 
// http://www.anddev.org/announce_color_picker_dialog-t10771.html

import android.content.Context;
import android.content.res.Resources;
import android.content.res.Resources.NotFoundException;
import android.content.res.TypedArray;
import android.database.DataSetObserver;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff.Mode;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Build;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Transformation;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

import org.coolreader.R;
import org.coolreader.utils.Utils;

import java.util.ArrayList;

public class ColorPickerDialog extends BaseDialog implements OnSeekBarChangeListener {

    public interface OnColorChangedListener {
        void colorChanged(int color);
    }

	private SeekBar mR;
	private SeekBar mG;
	private SeekBar mB;
	private SeekBar mHue;
	private SeekBar mSaturation;
	private SeekBar mValue;
	private TextView mLabel;
	private OnColorChangedListener mListener;
	private int mColor;
	private GradientDrawable mPreviewDrawable;
	private BaseActivity mActivity;
	private LayoutInflater mInflater;
	private ColorList mList;
	private String[] colorNames;
	private TypedArray colorValues;
	private String currentFilter;

	private void setupColor(int color, Resources res) {
		mColor = color;
		int r = Color.red(mColor);
		int g = Color.green(mColor);
		int b = Color.blue(mColor);
		float[] hsv = new float[3];
		Color.colorToHSV(color, hsv);
		int h = (int) (hsv[0] * mHue.getMax() / 360);
		int s = (int) (hsv[1] * mSaturation.getMax());
		int v = (int) (hsv[2] * mValue.getMax());
		setupSeekBar(mR, R.string.options_color_r, r, res);
		setupSeekBar(mG, R.string.options_color_g, g, res);
		setupSeekBar(mB, R.string.options_color_b, b, res);
		setupSeekBar(mHue, R.string.options_color_hue, h, res);
		setupSeekBar(mSaturation, R.string.options_color_saturation, s, res);
		setupSeekBar(mValue, R.string.options_color_brightness, v, res);
		updatePreview(color);
	}

	public ColorPickerDialog(BaseActivity activity, OnColorChangedListener listener, int color, String title) {
		super(activity, title, true, false);
		mInflater = LayoutInflater.from(getContext());
		mListener = listener;
		mActivity = activity;
		colorNames = activity.getResources().getStringArray(R.array.colorNames);
		colorValues = activity.getResources().obtainTypedArray(R.array.colors);
		currentFilter = "";

		Resources res = activity.getResources();
		setTitle(title);
		View root = null;
		if (Build.VERSION.SDK_INT >= 21)
			root = LayoutInflater.from(activity).inflate(R.layout.color_picker, null);
		else
			root = LayoutInflater.from(activity).inflate(R.layout.color_picker_v19, null);

		ViewGroup body = (ViewGroup)root.findViewById(R.id.color_list);
		mList = new ColorList(activity, false);
		body.addView(mList);
		setFlingHandlers(mList, null, null);

		setView(root);
		
		View preview = root.findViewById(R.id.preview);
		mPreviewDrawable = new GradientDrawable();
		// 2 pix more than color_picker_frame's radius
		mPreviewDrawable.setCornerRadius(7);
		Drawable[] layers;
		layers = new Drawable[] {
				mPreviewDrawable,
				res.getDrawable(R.drawable.color_picker_frame),
		};
		preview.setBackground(new LayerDrawable(layers));
		
		mR = (SeekBar) root.findViewById(R.id.r);
		mG = (SeekBar) root.findViewById(R.id.g);
		mB = (SeekBar) root.findViewById(R.id.b);
		mHue = (SeekBar) root.findViewById(R.id.hue);
		mSaturation = (SeekBar) root.findViewById(R.id.saturation);
		mValue = (SeekBar) root.findViewById(R.id.value);
		mLabel = (TextView) root.findViewById(R.id.value_label);

		setupColor(color, res);

		final EditText tvSearchText = root.findViewById(R.id.search_text);
		ImageButton ibSearch = root.findViewById(R.id.btn_search);
		ibSearch.setOnClickListener(v1 -> {
			tvSearchText.setText("");
			currentFilter = "";
			ColorListAdapter cla = new ColorListAdapter();
			mList.setAdapter(cla);
			cla.notifyDataSetChanged();
		});
		tvSearchText.addTextChangedListener(new TextWatcher() {
			@Override
			public void onTextChanged(CharSequence cs, int arg1, int arg2, int arg3) {
			    currentFilter = cs.toString();
			    boolean isHex = false;
			    try {
					int color = Color.parseColor("#" + currentFilter.replace("#", ""));
					isHex = true;
					setupColor(color, res);
				} catch (Exception e) {

				}
			    if (!isHex) {
					ColorPickerDialog.ColorListAdapter cla = new ColorPickerDialog.ColorListAdapter();
					mList.setAdapter(cla);
					cla.notifyDataSetChanged();
				}
			}

			@Override
			public void beforeTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) { }

			@Override
			public void afterTextChanged(Editable arg0) {}
		});

		updatePreview(color);
		ibSearch.requestFocus();
	}

	private void setupSeekBar(SeekBar seekBar, int id, int value, Resources res) {
		seekBar.setProgressDrawable(new TextSeekBarDrawable(res, id, value < seekBar.getMax() / 2,
				mActivity, true,null));
		seekBar.setProgress(value);
		seekBar.setOnSeekBarChangeListener(this);
	}

	private void updateHSV() {
		float[] hsv = {
			360 * mHue.getProgress() / (float) mHue.getMax(),
			mSaturation.getProgress() / (float) mSaturation.getMax(),
			mValue.getProgress() / (float) mValue.getMax(),
		};
		mColor = Color.HSVToColor(hsv);
		mR.setProgress(Color.red(mColor));
		mG.setProgress(Color.green(mColor));
		mB.setProgress(Color.blue(mColor));
		updatePreview(mColor);
	}
	
	private void updateRGB() {
		mColor = Color.rgb(mR.getProgress(), mG.getProgress(), mB.getProgress());
		float[] hsv = new float[3];
		Color.colorToHSV(mColor, hsv);
		int h = (int) (hsv[0] * mHue.getMax() / 360);
		int s = (int) (hsv[1] * mSaturation.getMax());
		int v = (int) (hsv[2] * mValue.getMax());
		mHue.setProgress(h);
		mSaturation.setProgress(s);
		mValue.setProgress(v);
		updatePreview(mColor);
	}

	private void updatePreview(int color) {
		mPreviewDrawable.setColor(color);
		mPreviewDrawable.invalidateSelf();
		mLabel.setText(Utils.colorToHex(mColor));
	}

	public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
		if ( fromUser ) {
			if ( seekBar==mR || seekBar==mG || seekBar==mB )
				updateRGB();
			else
				updateHSV();
		}
	}

	public void onStartTrackingTouch(SeekBar seekBar) {
	}

	public void onStopTrackingTouch(SeekBar seekBar) {
	}

	@Override
	protected void onPositiveButtonClick() {
		mListener.colorChanged(mColor);
		super.onPositiveButtonClick();
	}

	@Override
	protected void onNegativeButtonClick() {
	//	mListener.colorChanged(mColor);
		super.onPositiveButtonClick();
	}

//	@Override
//	protected void onNegativeButtonClick() {
//		onPositiveButtonClick();
//	}

	static class IconPreviewDrawable extends Drawable {
		private Bitmap mBitmap;
		private Bitmap mTmpBitmap;
		private Canvas mTmpCanvas;
		private int mTintColor;
		

		public IconPreviewDrawable(Resources res, int id) {
			Bitmap b;
			try {
				b = BitmapFactory.decodeResource(res, id);
				if (b == null) {
					b = BitmapFactory.decodeResource(res, R.drawable.color_picker_icon);
				}
			} catch (NotFoundException e) {
				b = BitmapFactory.decodeResource(res, R.drawable.color_picker_icon);
			}
			mBitmap = b;
			mTmpBitmap = Bitmap.createBitmap(b.getWidth(), b.getHeight(), Config.ARGB_8888);
			mTmpCanvas = new Canvas(mTmpBitmap);
		}
		
		@Override
		public void draw(Canvas canvas) {
			Rect b = getBounds();
			float x = (b.width() - mBitmap.getWidth()) / 2.0f;
			float y = 0.75f * b.height() - mBitmap.getHeight() / 2.0f;
			
			mTmpCanvas.drawColor(0, Mode.CLEAR);
			mTmpCanvas.drawBitmap(mBitmap, 0, 0, null);
			mTmpCanvas.drawColor(mTintColor, Mode.SRC_ATOP);
			canvas.drawBitmap(mTmpBitmap, x, y, null);
		}

		@Override
		public int getOpacity() {
			return PixelFormat.TRANSLUCENT;
		}

		@Override
		public void setAlpha(int alpha) {
		}

		@Override
		public void setColorFilter(ColorFilter cf) {
		}
		
		@Override
		public void setColorFilter(int color, Mode mode) {
			mTintColor = color;
		}
	}

	static class ScrollAnimation extends Animation {
		private static final String TAG = "ScrollAnimation";
		private static final long DURATION = 750;
		private float mFrom;
		private float mTo;
		private float mCurrent;
		
		public ScrollAnimation() {
			setDuration(DURATION);
			setInterpolator(new DecelerateInterpolator());
		}
		
		public void startScrolling(float from, float to) {
			mFrom = from;
			mTo = to;
			startNow();
		}
		
		@Override
		protected void applyTransformation(float interpolatedTime, Transformation t) {
			mCurrent = mFrom + (mTo - mFrom) * interpolatedTime;
//			Log.d(TAG, "applyTransformation " + mCurrent);
		}
		
		public float getCurrent() {
			return mCurrent;
		}
	}

	public final static int ITEM_POSITION=0;

	class ColorListAdapter extends BaseAdapter {
		public boolean areAllItemsEnabled() {
			return true;
		}

		public boolean isEnabled(int arg0) {
			return true;
		}

		public int getCount() {
			int j = 0;
			for (int i=0;i<colorNames.length;i++) {
				if (colorNames[i].toLowerCase().contains(currentFilter.toLowerCase())) j++;
			}
			return j;
		}

		public Object getItem(int position) {
			if ( position<0 || position>=colorNames.length )
				return null;
			int j=0;
			for (int i=0;i<colorNames.length;i++) {
				if ((j==position)&&(colorNames[i].toLowerCase().contains(currentFilter.toLowerCase()))) return colorNames[i];
				if (colorNames[i].toLowerCase().contains(currentFilter.toLowerCase())) j++;
			}
			return null;
		}

		public long getItemId(int position) {
			return position;
		}

		public int getItemViewType(int position) {
			return ITEM_POSITION;
		}

		public int getViewTypeCount() {
			return 4;
		}

		public View getView(int position, View convertView, ViewGroup parent) {
			View view;
			int res = R.layout.color_item;
			view = mInflater.inflate(res, null);
			TextView text1 = view.findViewById(R.id.color_item_title_w);
			TextView text2 = view.findViewById(R.id.color_item_title_b);
			int j=0;
			String colorName = "undefined";
			int c = 0;
			for (int i=0;i<colorNames.length;i++) {
				if ((j==position)&&(colorNames[i].toLowerCase().contains(currentFilter.toLowerCase()))) {
					colorName = colorNames[i];
					c=colorValues.getColor(i, 0);
				}
				if (colorNames[i].toLowerCase().contains(currentFilter.toLowerCase())) j++;
			}

			text1.setBackgroundColor(c);
			text2.setBackgroundColor(c);

			text1.setText(colorName);
			text2.setText(colorName);

			return view;
		}

		public boolean hasStableIds() {
			return true;
		}

		public boolean isEmpty() {
			int sz = 0;
			for (int i=0;i<colorNames.length;i++) {
				if (colorNames[i].toLowerCase().contains(currentFilter.toLowerCase())) sz++;
			}
			return sz > 0;
		}

		private ArrayList<DataSetObserver> observers = new ArrayList<DataSetObserver>();

		public void registerDataSetObserver(DataSetObserver observer) {
			observers.add(observer);
		}

		public void unregisterDataSetObserver(DataSetObserver observer) {
			observers.remove(observer);
		}
	}

	class ColorList extends BaseListView {

		public ColorList(Context context, boolean shortcutMode ) {
			super(context, true);
			setChoiceMode(ListView.CHOICE_MODE_SINGLE);
			setLongClickable(true);
			setAdapter(new ColorPickerDialog.ColorListAdapter());
			setOnItemLongClickListener(new OnItemLongClickListener() {
				@Override
				public boolean onItemLongClick(AdapterView<?> arg0, View arg1,
											   int position, long arg3) {
					openContextMenu(ColorPickerDialog.ColorList.this);
					return true;
				}
			});
		}

		@Override
		public boolean performItemClick(View view, int position, long id) {
			int j=0;
			int c=0;
			for (int i=0;i<colorNames.length;i++) {
				if ((j==position)&&(colorNames[i].toLowerCase().contains(currentFilter.toLowerCase())))
					c=colorValues.getColor(i, 0);
				if (colorNames[i].toLowerCase().contains(currentFilter.toLowerCase())) j++;
			}
			mColor=c;
			ColorPickerDialog.this.onPositiveButtonClick();
			return true;
		}
	}

}

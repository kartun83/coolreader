package org.coolreader.options;

import android.content.Context;

import org.coolreader.R;
import org.coolreader.crengine.BaseActivity;
import org.coolreader.crengine.BaseDialog;
import org.coolreader.crengine.OptionOwner;
import org.coolreader.crengine.Settings;

public class ToolbarOption extends SubmenuOption {

	static int [] mSelPanelBackground = new int [] {
			0,
			1,
			2
	};

	static int [] mSelPanelBackgroundTitles = new int [] {
			R.string.sel_panel_background_1,
			R.string.sel_panel_background_2,
			R.string.sel_panel_background_3
	};

	static int [] mSelPanelBackgroundAddInfos = new int [] {
			R.string.option_add_info_empty_text,
			R.string.option_add_info_empty_text,
			R.string.option_add_info_empty_text
	};

	static int [] mSelPanelSliders = new int [] {
			0,
			1,
			2
	};

	static int [] mSelPanelSlidersTitles = new int [] {
			R.string.sel_panel_sliders_0,
			R.string.sel_panel_sliders_1,
			R.string.sel_panel_sliders_2
	};

	static int [] mSelPanelSlidersAddInfos = new int [] {
			R.string.option_add_info_empty_text,
			R.string.option_add_info_empty_text,
			R.string.option_add_info_empty_text
	};

	static int [] mSelPanelRecentDics = new int [] {
			0,
			1,
			2
	};

	static int [] mSelPanelRecentDicsTitles = new int [] {
			R.string.sel_panel_recent_dics_0,
			R.string.sel_panel_recent_dics_1,
			R.string.sel_panel_recent_dics_2
	};

	static int [] mSelPanelRecentDicsAddInfos = new int [] {
			R.string.option_add_info_empty_text,
			R.string.option_add_info_empty_text,
			R.string.option_add_info_empty_text
	};

	int[] mToolbarPositions = new int[] {
			Settings.VIEWER_TOOLBAR_NONE, Settings.VIEWER_TOOLBAR_TOP, Settings.VIEWER_TOOLBAR_BOTTOM, Settings.VIEWER_TOOLBAR_LEFT, Settings.VIEWER_TOOLBAR_RIGHT, Settings.VIEWER_TOOLBAR_SHORT_SIDE, Settings.VIEWER_TOOLBAR_LONG_SIDE
	};
	int[] mToolbarPositionsTitles = new int[] {
			R.string.options_view_toolbar_position_none, R.string.options_view_toolbar_position_top, R.string.options_view_toolbar_position_bottom, R.string.options_view_toolbar_position_left, R.string.options_view_toolbar_position_right, R.string.options_view_toolbar_position_short_side, R.string.options_view_toolbar_position_long_side
	};
	int[] mToolbarPositionsAddInfos = new int[] {
			R.string.option_add_info_empty_text, R.string.option_add_info_empty_text, R.string.option_add_info_empty_text, R.string.option_add_info_empty_text, R.string.option_add_info_empty_text, R.string.option_add_info_empty_text, R.string.option_add_info_empty_text
	};

	int[] mToolbarApperance = new int[] {
			Settings.VIEWER_TOOLBAR_100, Settings.VIEWER_TOOLBAR_100_gray, //Settings.VIEWER_TOOLBAR_100_inv,
			Settings.VIEWER_TOOLBAR_75, Settings.VIEWER_TOOLBAR_75_gray, //Settings.VIEWER_TOOLBAR_75_inv,
			Settings.VIEWER_TOOLBAR_50, Settings.VIEWER_TOOLBAR_50_gray //, Settings.VIEWER_TOOLBAR_50_inv
	};
	int[] mToolbarApperanceTitles = new int[] {
			R.string.options_view_toolbar_appear_100, R.string.options_view_toolbar_appear_100_gray, //R.string.options_view_toolbar_appear_100_inv,
			R.string.options_view_toolbar_appear_75, R.string.options_view_toolbar_appear_75_gray, //R.string.options_view_toolbar_appear_75_inv,
			R.string.options_view_toolbar_appear_50, R.string.options_view_toolbar_appear_50_gray //, R.string.options_view_toolbar_appear_50_inv
	};

	int[] mToolbarApperanceAddInfos = new int[] {
			R.string.option_add_info_empty_text, R.string.option_add_info_empty_text, //R.string.option_add_info_empty_text,
			R.string.option_add_info_empty_text, R.string.option_add_info_empty_text, //R.string.option_add_info_empty_text,
			R.string.option_add_info_empty_text, R.string.option_add_info_empty_text //, R.string.option_add_info_empty_text
	};

	final BaseActivity mActivity;
	final Context mContext;

	public ToolbarOption(BaseActivity activity, Context context, OptionOwner owner, String label, String addInfo, String filter ) {
		super(owner, label, Settings.PROP_TOOLBAR_TITLE, addInfo, filter);
		mActivity = activity;
		mContext = context;
	}

	public void onSelect() {
		if (!enabled)
			return;
		BaseDialog dlg = new BaseDialog("ToolbarDialog", mActivity, label, false, false);
		OptionsListView listView = new OptionsListView(mContext, this);
		listView.add(new ListOption(mOwner, mActivity.getString(R.string.options_view_toolbar_position), Settings.PROP_TOOLBAR_LOCATION,
				mActivity.getString(R.string.options_view_toolbar_position_add_info), this.lastFilteredValue).add(mToolbarPositions, mToolbarPositionsTitles,
				mToolbarPositionsAddInfos).setDefaultValue("1").setIconIdByAttr(R.attr.attr_icons8_navigation_toolbar_top, R.drawable.icons8_navigation_toolbar_top));
		listView.add(OptionsDialog.getOption(Settings.PROP_TOOLBAR_HIDE_IN_FULLSCREEN, this.lastFilteredValue));
		listView.add(new ListOption(mOwner, mActivity.getString(R.string.options_view_toolbar_appearance), Settings.PROP_TOOLBAR_APPEARANCE,
				mActivity.getString(R.string.options_view_toolbar_appearance_add_info), this.lastFilteredValue).
				add(mToolbarApperance, mToolbarApperanceTitles, mToolbarApperanceAddInfos).setDefaultValue("6").setIconIdByAttr(
				R.attr.attr_icons8_navigation_toolbar_top, R.drawable.icons8_navigation_toolbar_top));
		ReaderToolbarOption rtO = (ReaderToolbarOption) new ReaderToolbarOption(mOwner, mActivity.getString(R.string.options_reader_toolbar_buttons),
				mActivity.getString(R.string.options_reader_toolbar_buttons_add_info), this.lastFilteredValue).setIconIdByAttr(R.attr.cr3_option_controls_keys_drawable, R.drawable.cr3_option_controls_keys);
		rtO.updateFilterEnd();
		listView.add(rtO);
		listView.add(OptionsDialog.getOption(Settings.PROP_APP_OPTIONS_EXT_SELECTION_TOOLBAR, this.lastFilteredValue));
		listView.add(new ListOption(mOwner, mActivity.getString(R.string.sel_panel_background), Settings.PROP_APP_OPTIONS_SELECTION_TOOLBAR_BACKGROUND,
				mActivity.getString(R.string.sel_panel_add_info), this.lastFilteredValue).
				add(mSelPanelBackground, mSelPanelBackgroundTitles, mSelPanelBackgroundAddInfos).setDefaultValue("0").
				setIconIdByAttr(R.attr.attr_icons8_toolbar_background,R.drawable.icons8_toolbar_background));
		listView.add(OptionsDialog.getOption(Settings.PROP_APP_OPTIONS_SELECTION_TOOLBAR_TRANSP_BUTTONS, this.lastFilteredValue));
		listView.add(new ListOption(mOwner, mActivity.getString(R.string.sel_panel_sliders), Settings.PROP_APP_OPTIONS_SELECTION_TOOLBAR_SLIDERS,
				mActivity.getString(R.string.option_add_info_empty_text), this.lastFilteredValue).
				add(mSelPanelSliders, mSelPanelSlidersTitles, mSelPanelSlidersAddInfos).setDefaultValue("0").
				setIconIdByAttr(R.attr.attr_icons8_slider,R.drawable.icons8_slider));
		listView.add(new ListOption(mOwner, mActivity.getString(R.string.sel_panel_recent_dics), Settings.PROP_APP_OPTIONS_SELECTION_TOOLBAR_RECENT_DICS,
				mActivity.getString(R.string.option_add_info_empty_text), this.lastFilteredValue).
				add(mSelPanelRecentDics, mSelPanelRecentDicsTitles, mSelPanelRecentDicsAddInfos).setDefaultValue("0").
				setIconIdByAttr(R.attr.attr_icons8_dic_panel,R.drawable.icons8_dic_panel));
		listView.add(OptionsDialog.getOption(Settings.PROP_PAGE_VIEW_MODE_SEL_DONT_CHANGE, this.lastFilteredValue));
		dlg.setView(listView);
		dlg.show();
	}

	public boolean updateFilterEnd() {
		this.updateFilteredMark(mActivity.getString(R.string.options_view_toolbar_position), Settings.PROP_TOOLBAR_LOCATION,
				mActivity.getString(R.string.options_view_toolbar_position_add_info));
		this.updateFilteredMark(mActivity.getString(R.string.options_view_toolbar_hide_in_fullscreen), Settings.PROP_TOOLBAR_HIDE_IN_FULLSCREEN,
				mActivity.getString(R.string.options_view_toolbar_hide_in_fullscreen_add_info));
		this.updateFilteredMark(mActivity.getString(R.string.options_view_toolbar_appearance), Settings.PROP_TOOLBAR_APPEARANCE,
				mActivity.getString(R.string.options_view_toolbar_appearance_add_info));
		this.updateFilteredMark(mActivity.getString(R.string.options_reader_toolbar_buttons), Settings.PROP_TOOLBAR_BUTTONS,
				mActivity.getString(R.string.options_reader_toolbar_buttons_add_info));
		this.updateFilteredMark(mActivity.getString(R.string.sel_panel_extended), Settings.PROP_APP_OPTIONS_EXT_SELECTION_TOOLBAR,
				mActivity.getString(R.string.sel_panel_extended_add_info));
		this.updateFilteredMark(mActivity.getString(R.string.sel_panel_background), Settings.PROP_APP_OPTIONS_SELECTION_TOOLBAR_BACKGROUND,
				mActivity.getString(R.string.sel_panel_add_info));
		this.updateFilteredMark(mActivity.getString(R.string.sel_panel_background_1));
		this.updateFilteredMark(mActivity.getString(R.string.sel_panel_background_2));
		this.updateFilteredMark(mActivity.getString(R.string.sel_panel_background_3));
		this.updateFilteredMark(mActivity.getString(R.string.sel_panel_transp_buttons), Settings.PROP_APP_OPTIONS_SELECTION_TOOLBAR_TRANSP_BUTTONS,
				mActivity.getString(R.string.sel_panel_add_info));
		this.updateFilteredMark(mActivity.getString(R.string.sel_panel_sliders_0));
		this.updateFilteredMark(mActivity.getString(R.string.sel_panel_sliders_1));
		this.updateFilteredMark(mActivity.getString(R.string.sel_panel_sliders_2));
		this.updateFilteredMark(mActivity.getString(R.string.sel_panel_sliders), Settings.PROP_APP_OPTIONS_SELECTION_TOOLBAR_SLIDERS,
				mActivity.getString(R.string.option_add_info_empty_text));
		for (int i: mSelPanelSlidersTitles) if (i > 0) this.updateFilteredMark(mActivity.getString(i));
		for (int i: mSelPanelSlidersAddInfos) if (i > 0) this.updateFilteredMark(mActivity.getString(i));
		this.updateFilteredMark(mActivity.getString(R.string.sel_panel_recent_dics), Settings.PROP_APP_OPTIONS_SELECTION_TOOLBAR_RECENT_DICS,
				mActivity.getString(R.string.option_add_info_empty_text));
		for (int i: mSelPanelRecentDicsTitles) if (i > 0) this.updateFilteredMark(mActivity.getString(i));
		for (int i: mSelPanelRecentDicsAddInfos) if (i > 0) this.updateFilteredMark(mActivity.getString(i));
		this.updateFilteredMark(mActivity.getString(R.string.tts_page_mode_dont_change2), Settings.PROP_PAGE_VIEW_MODE_SEL_DONT_CHANGE,
				mActivity.getString(R.string.tts_page_mode_dont_change_add_info));
		return this.lastFiltered;
	}

	public String getValueLabel() { return ">"; }
}

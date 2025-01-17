package org.coolreader.crengine;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.view.*;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import org.coolreader.CoolReader;
import org.coolreader.R;
import org.coolreader.cloud.CloudAction;
import org.coolreader.cloud.dropbox.DBXInputTokenDialog;
import org.coolreader.cloud.litres.LitresCredentialsDialog;
import org.coolreader.cloud.litres.LitresMainDialog;
import org.coolreader.cloud.yandex.YNDInputTokenDialog;
import org.coolreader.db.CRDBService;
import org.coolreader.dic.DictsDlg;
import org.coolreader.layouts.FlowLayout;
import org.coolreader.options.OptionsDialog;
import org.coolreader.utils.FileUtils;
import org.coolreader.utils.StorageDirectory;
import org.coolreader.utils.StrUtils;
import org.coolreader.utils.Utils;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.zip.CRC32;

import static org.coolreader.crengine.FileInfo.AUTHORS_TAG;

public class CRRootView extends ViewGroup {

	public static final Logger log = L.create("cr");

	boolean isEInk = false;
	HashMap<Integer, Integer> themeColors;
	public boolean needRefreshOnlineCatalogs = false;
	private final CoolReader mActivity;
	public ViewGroup mView;
	private LinearLayout mRecentBooksScroll;
	private TextView mTextFilesystem;
	private ImageView mImgFilesystem;
	private boolean bFilesystemHidden = false;
	private FlowLayout mFilesystemScroll;
	private TextView mTextLibrary;
	private ImageView mImgLibrary;
	private boolean bLibraryHidden = false;
	private FlowLayout mLibraryScroll;
	private TextView mTextOnlineCatalogs;
	private ImageView mImgOnlineCatalogs;
	private TextView mTextSortAZ;
	private ImageView mImgSortAZ;
	private boolean bOnlineCatalogsHidden = false;
	private boolean bOnlineCatalogsSortAZ = false;
	private boolean bLitresHidden = false;
	private FlowLayout mOnlineCatalogsScroll;
	private Button btnCalendar;
    private Button btnStateToRead;
    private Button btnStateReading;
    private Button btnStateFinished;
	private Button btnRecentToRead;
	private Button btnRecentReading;
	private Button btnRecentFinished;
	private boolean bRecentToRead = true;
	private boolean bRecentReading = true;
	private boolean bRecentFinished = true;
	private final CoverpageManager mCoverpageManager;
	private final int coverWidth;
	private final int coverHeight;
	private BookInfo currentBook;
	//private CoverpageReadyListener coverpageListener;
	public ArrayList<FileInfo> lastRecentFiles = new ArrayList<>();

	public CRRootView(CoolReader activity) {
		super(activity);
		this.mActivity = activity;
		isEInk = DeviceInfo.isEinkScreen(BaseActivity.getScreenForceEink());
		themeColors = Utils.getThemeColors(activity, isEInk);
		this.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT));
		this.mCoverpageManager = Services.getCoverpageManager();
		this.mCoverpageManager.setmCoolReader(mActivity);
//		coverpageListener = files -> {
//			onCoverpagesReady(files);
//		};
//		this.mCoverpageManager.addCoverpageReadyListener(coverpageListener);
		int screenHeight = mActivity.getWindowManager().getDefaultDisplay().getHeight();
		int screenWidth = mActivity.getWindowManager().getDefaultDisplay().getWidth();
		int h = screenHeight / 4;
		int w = screenWidth / 4;
		if (h > w) {
			h = w;
		}
    	w = h * 3 / 4;
    	coverWidth = w;
    	coverHeight = h;
    	setFocusable(true);
    	setFocusableInTouchMode(true);
		createViews();
		
	}

	private long menuDownTs = 0;
	private long backDownTs = 0;

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (event.getKeyCode() == KeyEvent.KEYCODE_MENU) {
			//L.v("CRRootView.onKeyDown(" + keyCode + ")");
			if (event.getRepeatCount() == 0)
				menuDownTs = Utils.timeStamp();
			return true;
		}
		if (event.getKeyCode() == KeyEvent.KEYCODE_BACK) {
			//L.v("CRRootView.onKeyDown(" + keyCode + ")");
			if (event.getRepeatCount() == 0)
				backDownTs = Utils.timeStamp();
			return true;
		}
		return super.onKeyDown(keyCode, event);
	}

	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event) {
		if (event.getKeyCode() == KeyEvent.KEYCODE_MENU) {
			long duration = Utils.timeInterval(menuDownTs);
			L.v("CRRootView.onKeyUp(" + keyCode + ") duration = " + duration);
			if (duration > 700 && duration < 10000)
				mActivity.showOptionsDialog(OptionsDialog.Mode.READER);
			else
				showMenu();
			return true;
		}
		if (event.getKeyCode() == KeyEvent.KEYCODE_BACK) {
			// prevents from closing when dialog was closed
			if ((System.currentTimeMillis() - mActivity.mLastDialogClosed) < 2000) return true;
			long duration = Utils.timeInterval(backDownTs);
			L.v("CRRootView.onKeyUp(" + keyCode + ") duration = " + duration);
			if (duration > 700 && duration < 10000 || !mActivity.isBookOpened()) {
				mActivity.finish();
				return true;
			} else {
				//mActivity.showReader(); // plotn - bad logic, we will press true back
				mActivity.onBackPressed();
				return true;
			}
		}
		return super.onKeyUp(keyCode, event);
	}



	private InterfaceTheme lastTheme;
	public void onThemeChange(InterfaceTheme theme) {
		if (lastTheme != theme) {
			lastTheme = theme;
			createViews();
		}
	}
	
	public void onClose() {
		//this.mCoverpageManager.removeCoverpageReadyListener(coverpageListener);
		//coverpageListener = null;
		super.onDetachedFromWindow();
	}

	private void setBookInfoItem(ViewGroup baseView, int viewId, String value) {
		TextView view = baseView.findViewById(viewId);
		if (view != null) {
			if (value != null && value.length() > 0) {
				view.setText(value);
			} else {
				view.setText("");
			}
		}
	}
	
	private void updateCurrentBook(BookInfo book) {
    	currentBook = book;
    	
    	// set current book cover page
		ImageView cover = mView.findViewById(R.id.book_cover);
		if (currentBook != null) {
			FileInfo item = currentBook.getFileInfo();
			cover.setImageDrawable(mCoverpageManager.getCoverpageDrawableFor(mActivity.getDB(), item, coverWidth, coverHeight));
			cover.setMinimumHeight(coverHeight);
			cover.setMinimumWidth(coverWidth);
			cover.setMaxHeight(coverHeight);
			cover.setMaxWidth(coverWidth);
			cover.setTag(new CoverpageManager.ImageItem(item, coverWidth, coverHeight));

			setBookInfoItem(mView, R.id.lbl_book_author, Utils.formatAuthors(item.getAuthors()));
			setBookInfoItem(mView, R.id.lbl_book_title, currentBook.getFileInfo().title);
			setBookInfoItem(mView, R.id.lbl_book_series, Utils.formatSeries(item.series, item.seriesNumber));
			String state = Utils.formatReadingState(mActivity, item);
			int colorBlue = themeColors.get(R.attr.colorThemeBlue);
			int colorGreen = themeColors.get(R.attr.colorThemeGreen);
			int colorGray = themeColors.get(R.attr.colorThemeGray);
			TextView tvInfo = mView.findViewById(R.id.lbl_book_info1);
			int n = item.getReadingState();
			tvInfo.setTag("notint");
			if (n == FileInfo.STATE_READING)
				tvInfo.setTextColor(colorGreen);
			else if (n == FileInfo.STATE_TO_READ)
				tvInfo.setTextColor(colorBlue);
			else if (n == FileInfo.STATE_FINISHED)
				tvInfo.setTextColor(colorGray);
			setBookInfoItem(mView, R.id.lbl_book_info1, state);
			state =  " " + Utils.formatFileInfo(mActivity, item) + " ";
			if (Services.getHistory() != null)
				state = state + " " + Utils.formatLastPosition(mActivity, Services.getHistory().getLastPos(item));
			setBookInfoItem(mView, R.id.lbl_book_info, state);
		} else {
			log.w("No current book in history");
			cover.setImageDrawable(null);
			cover.setMinimumHeight(0);
			cover.setMinimumWidth(0);
			cover.setMaxHeight(0);
			cover.setMaxWidth(0);

			setBookInfoItem(mView, R.id.lbl_book_author, "");
			setBookInfoItem(mView, R.id.lbl_book_title, "No last book"); // TODO: i18n
			setBookInfoItem(mView, R.id.lbl_book_series, "");
		}
	}	
	
	private final static int MAX_RECENT_BOOKS = 12;
	private void updateRecentBooks(ArrayList<BookInfo> booksF) {
		ArrayList<BookInfo> books = new ArrayList<>();
		boolean bFirst = true;
		for (BookInfo bi: booksF) {
			boolean bSkip = false;
			int n = bi.getFileInfo().getReadingState();
			if ((n == FileInfo.STATE_READING)  && (!bRecentReading)) bSkip = true;
			if ((n == FileInfo.STATE_TO_READ)  && (!bRecentToRead)) bSkip = true;
			if ((n == FileInfo.STATE_FINISHED)  && (!bRecentFinished)) bSkip = true;
			if (bFirst) bSkip = false;
			bFirst = false;
			if (!bSkip) books.add(bi);
		}
		int colorBlue = themeColors.get(R.attr.colorThemeBlue);
		int colorGreen = themeColors.get(R.attr.colorThemeGreen);
		int colorGray = themeColors.get(R.attr.colorThemeGray);
		int colorIcon = themeColors.get(R.attr.colorIcon);
		lastRecentFiles.clear();
		for (int i=0;i<books.size();i++) lastRecentFiles.add(books.get(i).getFileInfo());
		ArrayList<FileInfo> files = new ArrayList<>();
		for (int i = 1; i <= MAX_RECENT_BOOKS && i < books.size(); i++)
			files.add(books.get(i).getFileInfo());
		if (books.size() > MAX_RECENT_BOOKS && Services.getScanner() != null)
			files.add(Services.getScanner().createRecentRoot());
		LayoutInflater inflater = LayoutInflater.from(mActivity);
		mRecentBooksScroll.removeAllViews();
		for (final FileInfo item : files) {
			final View view = inflater.inflate(R.layout.root_item_recent_book, null);
			ImageView cover = view.findViewById(R.id.book_cover);
			TextView label = view.findViewById(R.id.book_name);
			cover.setMinimumHeight(coverHeight);
			cover.setMaxHeight(coverHeight);
			cover.setMaxWidth(coverWidth);
			if (item.isRecentDir()) {
				cover.setImageResource(Utils.resolveResourceIdByAttr(mActivity, R.attr.cr3_button_next_drawable, R.drawable.cr3_button_next));
				mActivity.tintViewIcons(cover,true);
				if (label != null) {
					label.setText("More...");
					label.setTextColor(mActivity.getTextColor(colorIcon));
				}
				view.setOnClickListener(v -> mActivity.showRecentBooks());
			} else {
				cover.setMinimumWidth(coverWidth);
				cover.setTag(new CoverpageManager.ImageItem(item, coverWidth, coverHeight));
				cover.setImageDrawable(mCoverpageManager.getCoverpageDrawableFor(mActivity.getDB(), item, coverWidth, coverHeight));
				if (label != null) {
					String title = item.title;
					String authors = Utils.formatAuthors(item.getAuthors());
					String s = item.getFileNameToDisplay();
					if (!Utils.empty(title) && !Utils.empty(authors))
						s = title + " - " + authors;
					else if (!Utils.empty(title))
						s = title;
					else if (!Utils.empty(authors))
						s = authors;
					label.setText(s != null ? s : "");
					label.setTextColor(mActivity.getTextColor(colorIcon));
                    int n = item.getReadingState();
                    if (n == FileInfo.STATE_READING)
                        label.setTextColor(colorGreen);
                    else if (n == FileInfo.STATE_TO_READ)
                        label.setTextColor(colorBlue);
                    else if (n == FileInfo.STATE_FINISHED)
                        label.setTextColor(colorGray);
                    label.setMaxWidth(coverWidth);
				}
				view.setOnClickListener(v -> mActivity.loadDocument(item, true));
				view.setOnLongClickListener(v -> {
					mActivity.editBookInfo(Services.getScanner().createRecentRoot(), item);
					return true;
				});
			}
			mRecentBooksScroll.addView(view);
			addTextViewSpacing(mRecentBooksScroll);
		}
		mRecentBooksScroll.invalidate();
	}

	public void refreshRecentBooks() {
		BackgroundThread.instance().postGUI(() -> mActivity.waitForCRDBService(() -> {
			if (Services.getHistory() != null && mActivity.getDB() != null)
				Services.getHistory().getOrLoadRecentBooks(mActivity.getDB(), new CRDBService.RecentBooksLoadingCallback() {

					@Override
					public void onRecentBooksListLoadBegin() {

					}

					@Override
					public void onRecentBooksListLoaded(ArrayList<BookInfo> bookList) {
							updateCurrentBook(bookList != null && bookList.size() > 0 ? bookList.get(0) : null);
							updateRecentBooks(bookList);
						}
					});
			}));
	}

	public void refreshOnlineCatalogs(boolean readSetting) {
		needRefreshOnlineCatalogs = false;
		bOnlineCatalogsHidden = false;
		bLitresHidden = false;
		mActivity.waitForCRDBService(() ->
				mActivity.getDB().loadOPDSCatalogs(catalogs -> updateOnlineCatalogs(catalogs, readSetting)));
	}

    public void refreshFileSystemFolders(boolean readSetting) {
        ArrayList<FileInfo> folders = Services.getFileSystemFolders().getFileSystemFolders();
		File f = new File(Environment.getExternalStorageDirectory().toString()+ File.separator + Environment.DIRECTORY_DOWNLOADS);
		if ((f.exists()) && (folders != null)) {
			String label = f.getName();
			FileInfo fiDownl = new FileInfo(f);
			fiDownl.isDirectory = true;
			fiDownl.setType(FileInfo.TYPE_DOWNLOAD_DIR);
			fiDownl.setTitle(label);
			folders.add(fiDownl);
		}
        updateFilesystems(folders);
		bFilesystemHidden = false;
		if (readSetting) bFilesystemHidden = mActivity.settings().getBool(Settings.PROP_APP_ROOT_VIEW_FS_SECTION_HIDE, false);
		if (bFilesystemHidden) {
			mFilesystemScroll.removeAllViews();
			Drawable d = mActivity.getResources().getDrawable(R.drawable.icons8_toc_item_collapsed);
			mImgFilesystem.setImageDrawable(d);
			mActivity.tintViewIcons(mImgFilesystem);
		}
    }

	public static ArrayList<FileInfo> lastCatalogs = new ArrayList<>();
	private void updateOnlineCatalogs(ArrayList<FileInfo> catalogs, boolean readSetting) {
		int colorIcon = themeColors.get(R.attr.colorIcon);
		int colorIconL = themeColors.get(R.attr.colorIconL);
		String lang = mActivity.getCurrentLanguage();
		if (readSetting)
			bLitresHidden = mActivity.settings().getBool(Settings.PROP_CLOUD_LITRES_DISABLED, false);
		if (!bLitresHidden) catalogs.add(0, Scanner.createLitresItem("LitRes"));
		if (Services.getScanner() == null)
			return;
		FileInfo opdsRoot = Services.getScanner().getOPDSRoot();
		if (opdsRoot.dirCount() == 0)
			opdsRoot.addItems(catalogs);
		catalogs.add(0, opdsRoot);
		lastCatalogs = catalogs;
		
		LayoutInflater inflater = LayoutInflater.from(mActivity);
		mOnlineCatalogsScroll.removeAllViews();
		if (readSetting) {
			bOnlineCatalogsSortAZ = mActivity.settings().getBool(Settings.PROP_APP_ROOT_VIEW_OPDS_SECTION_SORT_AZ, false);
		}
		if (bOnlineCatalogsSortAZ) {
			FileInfo catAdd = catalogs.get(0);
			Comparator<FileInfo> compareByName = (o1, o2) -> o1.getFilename().compareToIgnoreCase(o2.getFilename());
			Collections.sort(catalogs, compareByName);
			catalogs.remove(catAdd);
			catalogs.add(0, catAdd);
		}
		for (final FileInfo item : catalogs) {
			final View view = inflater.inflate(R.layout.root_item_library_h, null);
			ImageView icon = view.findViewById(R.id.item_icon);
			setImageResourceSmall(icon,Utils.resolveResourceIdByAttr(mActivity, R.attr.cr3_browser_folder_authors_drawable, R.drawable.cr3_browser_folder_authors));
			TextView label = view.findViewById(R.id.item_name);
			if (item.isOPDSRoot()) {
				setImageResourceSmall(icon, Utils.resolveResourceIdByAttr(mActivity, R.attr.cr3_browser_folder_opds_add_drawable, R.drawable.cr3_browser_folder_opds_add));
                mActivity.tintViewIcons(icon,true);
				label.setText(mActivity.getString(R.string.str_manage));
				label.setTypeface(Typeface.DEFAULT_BOLD);
				label.setTextColor(mActivity.getTextColor(colorIcon));
				view.setOnClickListener(v -> showAddCatalogMenu(icon, mView));
			} else if (item.isLitresDir()) {
				setImageResourceSmall(icon, R.drawable.litres);
				label.setText(item.getFilename());
				label.setTextColor(mActivity.getTextColor(colorIcon));
				view.setOnLongClickListener(v -> {
					mActivity.litresCredentialsDialog = new LitresCredentialsDialog(mActivity);
					mActivity.litresCredentialsDialog.show();
					return true;
				});
				view.setOnClickListener(v -> {
					LitresMainDialog litresMainDialog = new LitresMainDialog(mActivity, FileBrowser.saveParams);
					litresMainDialog.show();
				});
			} else {
				if (label != null) {
					label.setText(item.getFileNameToDisplay());
					if (item.was_error==1) {
						label.setTextColor(Color.argb(100,Color.red(colorIcon),Color.green(colorIcon),Color.blue(colorIcon)));
					}
					else
						label.setTextColor(mActivity.getTextColor(colorIcon));
					label.setMaxWidth(coverWidth * 3 / 2);
				}

				if (label.getText().toString().toLowerCase().contains("gutenberg"))
					setImageResourceSmall(icon, R.drawable.projectgutenberg); else
				if (label.getText().toString().toLowerCase().contains("legimi"))
					setImageResourceSmall(icon, R.drawable.legimi); else
				if (label.getText().toString().toLowerCase().matches(".*revues.*org.*"))
					setImageResourceSmall(icon, R.drawable.revues_org); else
				if (label.getText().toString().toLowerCase().matches(".*libres.*et.*gratuits.*"))
					setImageResourceSmall(icon, R.drawable.ebooks_gratuits); else
				if (label.getText().toString().toLowerCase().matches(".*internet.*archive.*"))
					setImageResourceSmall(icon, R.drawable.internet_archive); else
				if (label.getText().toString().toLowerCase().matches(".*feed.*books.*"))
					setImageResourceSmall(icon, R.drawable.feedbooks); else
				if (label.getText().toString().toLowerCase().matches(".*flibusta.*"))
					setImageResourceSmall(icon, R.drawable.flibusta); else
				if (label.getText().toString().toLowerCase().contains("manybooks"))
					setImageResourceSmall(icon, R.drawable.manybooks); else
				if (label.getText().toString().toLowerCase().contains("smashwords"))
					setImageResourceSmall(icon, R.drawable.smashwords); else
				if (label.getText().toString().toLowerCase().contains("webnovel"))
					setImageResourceSmall(icon, R.drawable.webnovel); else
					mActivity.tintViewIcons(icon,true);
				findIconForCatalog(item, icon);
				view.setOnClickListener(
					v -> {
							if (item.isCalibreRoot()) {
								showCalibreMenu(icon, mView, item);
							} else mActivity.showCatalog(item, "");
						}
				);
				view.setOnLongClickListener(v -> {
					mActivity.editOPDSCatalog(item);
					return true;
				});
			}
			mOnlineCatalogsScroll.addView(view);
			addTextViewSpacing(mOnlineCatalogsScroll);
		}
		mOnlineCatalogsScroll.invalidate();
		if (readSetting) {
			bOnlineCatalogsHidden = mActivity.settings().getBool(Settings.PROP_APP_ROOT_VIEW_OPDS_SECTION_HIDE, false);
			bLitresHidden = mActivity.settings().getBool(Settings.PROP_CLOUD_LITRES_DISABLED, false);
		}
		if (bOnlineCatalogsHidden) {
			mOnlineCatalogsScroll.removeAllViews();
			Drawable d = mActivity.getResources().getDrawable(R.drawable.icons8_toc_item_collapsed);
			mImgOnlineCatalogs.setImageDrawable(d);
			mActivity.tintViewIcons(mImgOnlineCatalogs);
		}
		mImgSortAZ.setImageDrawable(null);
		mTextSortAZ.setVisibility(bOnlineCatalogsHidden? View.INVISIBLE: View.VISIBLE);
		if (bOnlineCatalogsSortAZ && (!bOnlineCatalogsHidden)) {
			Drawable d = mActivity.getResources().getDrawable(R.drawable.icons8_check_no_frame);
			mImgSortAZ.setImageDrawable(d);
			mActivity.tintViewIcons(mImgSortAZ);
		}
	}

	private void addTextViewSpacing(ViewGroup v) {
		LinearLayout.LayoutParams llp = new LinearLayout.LayoutParams(
				ViewGroup.LayoutParams.WRAP_CONTENT,
				ViewGroup.LayoutParams.WRAP_CONTENT);
		llp.setMargins(4, 4, 4, 4);
		TextView tv = new TextView(mActivity);
		tv.setText(" ");
		tv.setPadding(5, 1, 1, 1);
		tv.setLayoutParams(llp);
		//tv.setBackgroundColor(Color.argb(0, Color.red(colorGrayC), Color.green(colorGrayC), Color.blue(colorGrayC)));
		//tv.setTextColor(mActivity.getTextColor(colorIcon));
		v.addView(tv);
	}

	private void updateFilesystems(List<FileInfo> dirs) {
		if (dirs==null) return;
		int colorIcon = themeColors.get(R.attr.colorIcon);
		ArrayList<StorageDirectory> arrStorages = mActivity.getStorageDirectories();
		for (StorageDirectory storageDirectory: arrStorages) {
//			fi.usbDeviceName = key;
//			fi.usbDevice = device;
			boolean bFound = false;
			for (FileInfo fi1: dirs)
				if (fi1.pathname.equals(storageDirectory.mPath)) {
					bFound = true;
					break;
				}
			if (!bFound) {
				FileInfo fi = new FileInfo();
				fi.setFilename("OTG:" + FileUtils.getFileName(storageDirectory.mPath));
				fi.setTitle("OTG:" + FileUtils.getFileName(storageDirectory.mPath));
				fi.pathname = FileInfo.OTG_DIR_PREFIX + storageDirectory.mPath;
				dirs.add(fi);
			}
		}
//		for (Map.Entry<String, UsbDevice> entry : mActivity.usbDevices.entrySet()) {
//			String key = entry.getKey();
//			UsbDevice device = entry.getValue();
//			FileInfo fi = new FileInfo();
//			fi.usbDeviceName = key;
//			fi.usbDevice = device;
//			fi.pathname = fi.getUsbDeviceLabel();
//			dirs.add(fi);
//		}
		if (dirs.size() != 0) {
			LayoutInflater inflater = LayoutInflater.from(mActivity);
			mFilesystemScroll.removeAllViews();
			int idx = 0;
			ImageView icon;
			TextView label;
			View view = null;

			View viewGD = inflater.inflate(R.layout.root_item_library_h, null);
			ImageView iconGD = viewGD.findViewById(R.id.item_icon);
			TextView labelGD = viewGD.findViewById(R.id.item_name);
			setImageResourceSmall(iconGD, Utils.resolveResourceIdByAttr(mActivity,
					R.attr.attr_icons8_google_drive_2, R.drawable.icons8_google_drive_2));
			mActivity.tintViewIcons(iconGD,true);
			labelGD.setText(R.string.open_book_from_gd_short);
			labelGD.setTextColor(mActivity.getTextColor(colorIcon));
			labelGD.setMaxWidth(coverWidth * 25 / 10);
			viewGD.setOnClickListener(v -> {
				if (!FlavourConstants.PREMIUM_FEATURES) {
					mActivity.showToast(R.string.only_in_premium);
					return;
				}
				mActivity.showToast("Coming soon...");
			});
			viewGD.setOnLongClickListener(v -> {
				if (!FlavourConstants.PREMIUM_FEATURES) {
					mActivity.showToast(R.string.only_in_premium);
					return true;
				}
				mActivity.showToast("Coming soon...");
				return true;
			});
			//plotn //not now
			//mFilesystemScroll.addView(viewGD);

			View viewDBX = inflater.inflate(R.layout.root_item_library_h, null);
			ImageView iconDBX = viewDBX.findViewById(R.id.item_icon);
			TextView labelDBX = viewDBX.findViewById(R.id.item_name);
			setImageResourceSmall(iconDBX, Utils.resolveResourceIdByAttr(mActivity,
					R.attr.attr_icons8_dropbox_filled, R.drawable.icons8_dropbox_filled));
			mActivity.tintViewIcons(iconDBX,true);
			labelDBX.setText(R.string.open_book_from_dbx_short);
			labelDBX.setTextColor(mActivity.getTextColor(colorIcon));
			labelDBX.setMaxWidth(coverWidth * 25 / 10);
			viewDBX.setOnClickListener(v -> {
				if (!FlavourConstants.PREMIUM_FEATURES) {
					mActivity.showToast(R.string.only_in_premium);
					return;
				}
				CloudAction.dbxOpenBookDialog(mActivity);
			});
			viewDBX.setOnLongClickListener(v -> {
				if (!FlavourConstants.PREMIUM_FEATURES) {
					mActivity.showToast(R.string.only_in_premium);
					return true;
				}
				mActivity.dbxInputTokenDialog = new DBXInputTokenDialog(mActivity);
				mActivity.dbxInputTokenDialog.show();
				return true;
			});
			mFilesystemScroll.addView(viewDBX);
			addTextViewSpacing(mFilesystemScroll);

			View viewYandex = inflater.inflate(R.layout.root_item_library_h, null);
			ImageView iconYandex = viewYandex.findViewById(R.id.item_icon);
			TextView labelYandex = viewYandex.findViewById(R.id.item_name);
			setImageResourceSmall(iconYandex, Utils.resolveResourceIdByAttr(mActivity,
					R.attr.attr_icons8_yandex, R.drawable.icons8_yandex_logo));
			mActivity.tintViewIcons(iconYandex,true);
			labelYandex.setText(R.string.open_book_from_y_short);
			addTextViewSpacing(mFilesystemScroll);
			labelYandex.setTextColor(mActivity.getTextColor(colorIcon));
			labelYandex.setMaxWidth(coverWidth * 25 / 10);
			viewYandex.setOnClickListener(v -> {
				if (!FlavourConstants.PREMIUM_FEATURES) {
					mActivity.showToast(R.string.only_in_premium);
					return;
				}
				CloudAction.yndOpenBookDialog(mActivity, null,true);
			});
			viewYandex.setOnLongClickListener(new OnLongClickListener() {
				@Override
				public boolean onLongClick(View v) {
					if (!FlavourConstants.PREMIUM_FEATURES) {
						mActivity.showToast(R.string.only_in_premium);
						return true;
					}
					mActivity.yndInputTokenDialog = new YNDInputTokenDialog(mActivity);
					mActivity.yndInputTokenDialog.show();
					return true;
				}
			});
			mFilesystemScroll.addView(viewYandex);
			addTextViewSpacing(mFilesystemScroll);

			for (final FileInfo item : dirs) {
				if (item == null)
					continue;
				view = inflater.inflate(R.layout.root_item_library_h, null);
				icon = view.findViewById(R.id.item_icon);
				label = view.findViewById(R.id.item_name);
				if (item.getType() == FileInfo.TYPE_DOWNLOAD_DIR) {
					setImageResourceSmall(icon, Utils.resolveResourceIdByAttr(mActivity, R.attr.folder_big_bookmark_drawable, R.drawable.folder_bookmark));
				} else if (item.getType() == FileInfo.TYPE_FS_ROOT) {
					setImageResourceSmall(icon, Utils.resolveResourceIdByAttr(mActivity, R.attr.media_flash_microsd_drawable, R.drawable.media_flash_sd_mmc));
				} else {
					setImageResourceSmall(icon, Utils.resolveResourceIdByAttr(mActivity, R.attr.folder_big_drawable, R.drawable.folder_blue));
				}
                mActivity.tintViewIcons(icon,true);
				String labText = "";

				if (item.title != null)
                    labText = item.title; //  filename
				else if (item.getType() == FileInfo.TYPE_FS_ROOT || item.getType() == FileInfo.TYPE_DOWNLOAD_DIR)
                    labText = item.getFilename(); //  filename
				else
                    labText = item.pathname; //  filename
				label.setMaxWidth(coverWidth * 25 / 10);
				label.setTextColor(mActivity.getTextColor(colorIcon));
				//if (labText.startsWith("OTG:"))
				View finalView = view;
				view.setOnClickListener(v -> {
						Uri uri =  mActivity.usbDevices.get(item.pathname);
						if ((Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) &&
								(item.isOTGDir())) {
							if (uri == null) {
								ArrayList<String> sButtons = new ArrayList<>();
								sButtons.add(mActivity.getString(R.string.str_yes));
								sButtons.add(mActivity.getString(R.string.str_no));
								sButtons.add(0,"*" + mActivity.getString(R.string.saf_otg_explanation));
								SomeButtonsToolbarDlg.showDialog(mActivity, finalView, 10,
									true,
									"", sButtons, null, (o22, btnPressed) -> {
										if (btnPressed.equals(mActivity.getString(R.string.str_yes)) ||
												(btnPressed.equals("{{timeout}}"))) {
											Intent safIntent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
											mActivity.mOpenDocumentTreeArg = item;
											mActivity.mOpenDocumentTreeCommand = mActivity.ODT_CMD_SELECT_OTG;
											mActivity.startActivityForResult(safIntent, mActivity.REQUEST_CODE_OPEN_DOCUMENT_TREE);
										}
									});
							} else {
								mActivity.showDirectory(item, "");
							}
						} else
							mActivity.showDirectory(item, "");
					}
				);
				String[] arrLab = labText.split("/");
				if (arrLab.length > 2)
					if (!labText.startsWith("OTG:")) labText="../"+arrLab[arrLab.length-2]+"/"+arrLab[arrLab.length-1];
					else labText="OTG:../"+arrLab[arrLab.length-2]+"/"+arrLab[arrLab.length-1];
                label.setText(labText);
				label.setTextColor(mActivity.getTextColor(colorIcon));
				view.setOnLongClickListener(view1 -> {
					registerFoldersContextMenu(item);
					return false;
				});
				mFilesystemScroll.addView(view);
				addTextViewSpacing(mFilesystemScroll);
				++idx;
			}
			mFilesystemScroll.invalidate();
		}
	}

    private void registerFoldersContextMenu(final FileInfo folder) {
        mActivity.registerForContextMenu(mFilesystemScroll);
        mFilesystemScroll.setOnCreateContextMenuListener((contextMenu, view, contextMenuInfo) -> {
			MenuInflater inflater = mActivity.getMenuInflater();
			inflater.inflate(R.menu.cr3_favorite_folder_context_menu,contextMenu);
			boolean isFavorite = folder.getType() == FileInfo.TYPE_NOT_SET;
			final FileSystemFolders service = Services.getFileSystemFolders();
			for(int idx = 0 ; idx< contextMenu.size(); ++idx){
				MenuItem item = contextMenu.getItem(idx);
				boolean enabled = isFavorite;
				if(item.getItemId() == R.id.folder_left) {
					enabled = enabled && service.canMove(folder, true);
					if(enabled)
						item.setOnMenuItemClickListener(menuItem -> {
							service.moveFavoriteFolder(mActivity.getDB(), folder, true);
							return true;
						});
				} else if(item.getItemId() == R.id.folder_right) {
					enabled = enabled && service.canMove(folder, false);
					if(enabled)
						item.setOnMenuItemClickListener(menuItem -> {
							service.moveFavoriteFolder(mActivity.getDB(), folder, false);
							return true;
						});
				} else if(item.getItemId() == R.id.folder_remove) {
					if(enabled)
						item.setOnMenuItemClickListener(menuItem -> {
							service.removeFavoriteFolder(mActivity.getDB(), folder);
							return true;
						});
				}
				item.setEnabled(enabled);
			}
		});
    }

    private void setImageResourceSmall(ImageView image, int resId) {
		if (resId>0) {
			Drawable fakeIcon = mActivity.getResources().getDrawable(resId);
			final Bitmap bmp = Bitmap.createBitmap(fakeIcon.getIntrinsicWidth()*3/4, fakeIcon.getIntrinsicHeight()*3/4, Bitmap.Config.ARGB_8888);
			final Canvas canvas = new Canvas(bmp);
			fakeIcon.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
			fakeIcon.draw(canvas);
			Bitmap resizedBitmap = Bitmap.createScaledBitmap(
					bmp, fakeIcon.getIntrinsicWidth()*3/4, fakeIcon.getIntrinsicHeight()*3/4, false);
			image.setImageBitmap(resizedBitmap);
		}
	}

	private void findIconForCatalog(FileInfo item, ImageView icon) {
		if (!StrUtils.isEmptyStr(item.pathname)) {
			String path = item.pathname.replace(FileInfo.OPDS_DIR_PREFIX, "");
			CRC32 crc = new CRC32();
			crc.update(path.getBytes());
			final String sFName = crc.getValue() + "_icon.png";
			String sDir = "";
			ArrayList<String> tDirs = Engine.getDataDirsExt(Engine.DataDirType.IconDirs, true);
			if (tDirs.size() > 0) sDir = tDirs.get(0);
			if (!StrUtils.isEmptyStr(sDir))
				if ((!sDir.endsWith("/")) && (!sDir.endsWith("\\"))) sDir = sDir + "/";
			if (!StrUtils.isEmptyStr(sDir)) {
				try {
					File f = new File(sDir + sFName);
					if (f.exists()) {
						Bitmap bitmap = BitmapFactory.decodeFile(sDir + sFName);
						icon.setColorFilter(null);
						Bitmap resizedBitmap = Bitmap.createScaledBitmap(
								bitmap, icon.getDrawable().getIntrinsicWidth(),
								icon.getDrawable().getIntrinsicHeight(), false);
						icon.setImageBitmap(resizedBitmap);
					}
				} catch (Exception e) {
					log.e("Catalog custom icon error: " + e.getMessage());
				}
			}
		}
//		if (label.getText().toString().toLowerCase().contains("webnovel"))
//			setImageResourceSmall(icon, R.drawable.webnovel); else
//			mActivity.tintViewIcons(icon,true);

	}

	private void updateLibraryItems(ArrayList<FileInfo> dirs, boolean readSetting) {
		int colorIcon = themeColors.get(R.attr.colorIcon);
		LayoutInflater inflater = LayoutInflater.from(mActivity);
		mLibraryScroll.removeAllViews();
		for (final FileInfo item : dirs) {
			final View view = inflater.inflate(R.layout.root_item_library_h, null);
			ImageView image = view.findViewById(R.id.item_icon);
			TextView label = view.findViewById(R.id.item_name);
			setImageResourceSmall(image,Utils.resolveResourceIdByAttr(mActivity, R.attr.cr3_browser_folder_authors_drawable, R.drawable.cr3_browser_folder_authors));
			if (item.isRescanShortcut())
				setImageResourceSmall(image,Utils.resolveResourceIdByAttr(mActivity, R.attr.attr_icons8_folder_scan, R.drawable.icons8_folder_scan));
			if (item.isCalcLibraryStatsShortcut())
				setImageResourceSmall(image,Utils.resolveResourceIdByAttr(mActivity, R.attr.attr_icons8_db_stats, R.drawable.icons8_db_stats));
			if (item.isSearchShortcut())
				setImageResourceSmall(image,Utils.resolveResourceIdByAttr(mActivity, R.attr.cr3_browser_find_drawable, R.drawable.cr3_browser_find));
			else if (item.isBooksByRatingRoot())
				setImageResourceSmall(image,Utils.resolveResourceIdByAttr(mActivity, R.attr.attr_icons8_folder_stars, R.drawable.icons8_folder_stars));
			else if (item.isBooksByTitleRoot() || item.isBooksByLitresCollectionDir() || item.isBooksByLitresCollectionRoot() || item.isLitresCollection())
				setImageResourceSmall(image,Utils.resolveResourceIdByAttr(mActivity, R.attr.cr3_browser_folder_authors_drawable, R.drawable.cr3_browser_folder_authors));
			else if (item.isBooksBySeriesRoot())
				setImageResourceSmall(image,Utils.resolveResourceIdByAttr(mActivity, R.attr.attr_icons8_folder_hash, R.drawable.icons8_folder_hash));
			else if (item.isBooksByAuthorRoot())
				setImageResourceSmall(image,Utils.resolveResourceIdByAttr(mActivity, R.attr.attr_icons8_folder_author, R.drawable.icons8_folder_author));
			else if (item.isBooksByGenreRoot())
				setImageResourceSmall(image,Utils.resolveResourceIdByAttr(mActivity, R.attr.attr_icons8_theatre_mask, R.drawable.icons8_theatre_mask));
			else if (item.isBooksByTagRoot())
				setImageResourceSmall(image,Utils.resolveResourceIdByAttr(mActivity, R.attr.attr_icons8_tag, R.drawable.icons8_tag));
			//else if (item.isBooksByGenreRoot() // CR implementation
			else if (item.isBooksByBookdateRoot() || item.isBooksByDocdateRoot() ||
					item.isBooksByPublyearRoot() || item.isBooksByFiledateRoot())
				setImageResourceSmall(image, Utils.resolveResourceIdByAttr(mActivity, R.attr.attr_icons8_folder_year, R.drawable.icons8_folder_year));
            mActivity.tintViewIcons(image,true);
			if (label != null) {
				label.setText(item.getFilename());
				label.setTextColor(mActivity.getTextColor(colorIcon));
				label.setMinWidth(coverWidth);
				label.setMaxWidth(coverWidth * 2);
			}
			// new behavior
			view.setOnClickListener(v -> {
				if (item.isRescanShortcut()) {
					ScanLibraryDialog sld = new ScanLibraryDialog(mActivity);
					sld.show();
				} else
					if (item.isCalcLibraryStatsShortcut()) {
						mActivity.getDB().getLibraryCategStats(o -> {
							mActivity.showToast(R.string.succ_finished);
						});
					} else mActivity.showDirectory(item, "");
			});
			if (item.isBooksByTagRoot())
				view.setOnLongClickListener(v -> {
					TagsEditDialog dlg1 = new TagsEditDialog(mActivity, null, true, null);
					dlg1.show();
					return true;
				});
			mLibraryScroll.addView(view);
			addTextViewSpacing(mLibraryScroll);
		}
		mLibraryScroll.invalidate();
		bLibraryHidden = false;
		if (readSetting) bLibraryHidden = mActivity.settings().getBool(Settings.PROP_APP_ROOT_VIEW_LIB_SECTION_HIDE, false);
		if (bLibraryHidden) {
			mLibraryScroll.removeAllViews();
			Drawable d = mActivity.getResources().getDrawable(R.drawable.icons8_toc_item_collapsed);
			mImgLibrary.setImageDrawable(d);
			mActivity.tintViewIcons(mImgLibrary);
		}
	}
	
	private void updateDelimiterTheme(int viewId) {
		View view = mView.findViewById(viewId);
		InterfaceTheme theme = mActivity.getCurrentTheme();
		view.setBackgroundResource(theme.getRootDelimiterResourceId());
		view.setMinimumHeight(theme.getRootDelimiterHeight());
		view.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.FILL_PARENT, theme.getRootDelimiterHeight()));
	}

	private void paintRecentButtons() {
		int colorGrayC = themeColors.get(R.attr.colorThemeGray2Contrast);
		int colorGrayCT=Color.argb(30,Color.red(colorGrayC),Color.green(colorGrayC),Color.blue(colorGrayC));
		int colorGrayCT2=Color.argb(200,Color.red(colorGrayC),Color.green(colorGrayC),Color.blue(colorGrayC));
		mActivity.tintViewIcons(btnRecentToRead, PorterDuff.Mode.CLEAR,true);
		mActivity.tintViewIcons(btnRecentReading, PorterDuff.Mode.CLEAR,true);
		mActivity.tintViewIcons(btnRecentFinished, PorterDuff.Mode.CLEAR,true);
		if (bRecentToRead) {
			btnRecentToRead.setBackgroundColor(colorGrayCT2);
			mActivity.tintViewIcons(btnRecentToRead,true);
			if (isEInk) Utils.setSolidButtonEink(btnRecentToRead);
		} else btnRecentToRead.setBackgroundColor(colorGrayCT);
		if (bRecentReading) {
			btnRecentReading.setBackgroundColor(colorGrayCT2);
			mActivity.tintViewIcons(btnRecentReading,true);
			if (isEInk) Utils.setSolidButtonEink(btnRecentReading);
		} else btnRecentReading.setBackgroundColor(colorGrayCT);
		if (bRecentFinished) {
			btnRecentFinished.setBackgroundColor(colorGrayCT2);
			mActivity.tintViewIcons(btnRecentFinished,true);
			if (isEInk) Utils.setSolidButtonEink(btnRecentFinished);
		} else btnRecentFinished.setBackgroundColor(colorGrayCT);
	}
	
	private void createViews() {
		LayoutInflater inflater = LayoutInflater.from(mActivity);
		View view = inflater.inflate(R.layout.root_window, null);
		LinearLayout llMain = view.findViewById(R.id.ll_main_rootwindow);
		LayoutParams lp = llMain.getLayoutParams();
		int globalMargins = mActivity.settings().getInt(Settings.PROP_GLOBAL_MARGIN, 0);
		if (globalMargins > 0)
			if( lp instanceof MarginLayoutParams )
			{
				((MarginLayoutParams) lp).topMargin = globalMargins;
				((MarginLayoutParams) lp).bottomMargin = globalMargins;
				((MarginLayoutParams) lp).leftMargin = globalMargins;
				((MarginLayoutParams) lp).rightMargin = globalMargins;
			}

		mView = (ViewGroup)view;
		
		updateDelimiterTheme(R.id.delimiter1);
		updateDelimiterTheme(R.id.delimiter2);
		updateDelimiterTheme(R.id.delimiter3);
		updateDelimiterTheme(R.id.delimiter4);
		updateDelimiterTheme(R.id.delimiter5);
		
		mRecentBooksScroll = mView.findViewById(R.id.scroll_recent_books);

		mTextFilesystem = mView.findViewById(R.id.tv_item_filesystem);
		mTextFilesystem.setPaintFlags(mTextFilesystem.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
		mImgFilesystem = mView.findViewById(R.id.iv_item_filesystem);
		mActivity.tintViewIcons(mImgFilesystem);

		OnClickListener lstnr3 = v -> {
			bFilesystemHidden = !bFilesystemHidden;
			if (bFilesystemHidden) {
				mFilesystemScroll.removeAllViews();
				Drawable d = mActivity.getResources().getDrawable(R.drawable.icons8_toc_item_collapsed);
				mImgFilesystem.setImageDrawable(d);
				//boolean bVis = mActivity.settings().getBool(Settings.PROP_APP_ROOT_VIEW_FS_SECTION_HIDE, false);
				mActivity.tintViewIcons(mImgFilesystem);
			} else {
				mFilesystemScroll.removeAllViews();
				refreshFileSystemFolders(false);
				Drawable d = mActivity.getResources().getDrawable(R.drawable.icons8_toc_item_expanded);
				mImgFilesystem.setImageDrawable(d);
				mActivity.tintViewIcons(mImgFilesystem);
			}
			Properties props = new Properties(mActivity.settings());
			props.setProperty(Settings.PROP_APP_ROOT_VIEW_FS_SECTION_HIDE, bFilesystemHidden?"1":"0");
			mActivity.setSettings(props, -1, false);
		};
		mTextFilesystem.setOnClickListener(lstnr3);
		mImgFilesystem.setOnClickListener(lstnr3);

		mFilesystemScroll = mView.findViewById(R.id.scroll_filesystem);

		mTextLibrary = mView.findViewById(R.id.tv_item_library);
		mTextLibrary.setPaintFlags(mTextLibrary.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
		mImgLibrary = mView.findViewById(R.id.iv_item_library);
		mActivity.tintViewIcons(mImgLibrary);

		OnClickListener lstnr = v -> {
			bLibraryHidden = !bLibraryHidden;
			if (bLibraryHidden) {
				mLibraryScroll.removeAllViews();
				Drawable d = mActivity.getResources().getDrawable(R.drawable.icons8_toc_item_collapsed);
				mImgLibrary.setImageDrawable(d);
				mActivity.tintViewIcons(mImgLibrary);
			} else {
				mLibraryScroll.removeAllViews();
				BackgroundThread.instance().postGUI(() -> {
					if (Services.getScanner() != null)
						updateLibraryItems(Services.getScanner().getLibraryItems(), false);
					bLibraryHidden = false;
					Drawable d = mActivity.getResources().getDrawable(R.drawable.icons8_toc_item_expanded);
					mImgLibrary.setImageDrawable(d);
					mActivity.tintViewIcons(mImgLibrary);
				});
			}
			Properties props = new Properties(mActivity.settings());
			props.setProperty(Settings.PROP_APP_ROOT_VIEW_LIB_SECTION_HIDE, bLibraryHidden?"1":"0");
			mActivity.setSettings(props, -1, false);
		};
		mTextLibrary.setOnClickListener(lstnr);
		mImgLibrary.setOnClickListener(lstnr);
		mLibraryScroll = mView.findViewById(R.id.scroll_library);

		mTextOnlineCatalogs = mView.findViewById(R.id.tv_item_online_catalogs);
		mTextOnlineCatalogs.setPaintFlags(mTextOnlineCatalogs.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
		mImgOnlineCatalogs = mView.findViewById(R.id.iv_item_online_catalogs);
		mActivity.tintViewIcons(mImgOnlineCatalogs);

		OnClickListener lstnr2 = v -> {
			bOnlineCatalogsHidden = !bOnlineCatalogsHidden;
			Properties props = new Properties(mActivity.settings());
			props.setProperty(Settings.PROP_APP_ROOT_VIEW_OPDS_SECTION_HIDE, bOnlineCatalogsHidden?"1":"0");
			mActivity.setSettings(props, -1, false);
			if (bOnlineCatalogsHidden) {
				mOnlineCatalogsScroll.removeAllViews();
				Drawable d = mActivity.getResources().getDrawable(R.drawable.icons8_toc_item_collapsed);
				mImgOnlineCatalogs.setImageDrawable(d);
				mActivity.tintViewIcons(mImgOnlineCatalogs);
			} else {
				mOnlineCatalogsScroll.removeAllViews();
				mActivity.waitForCRDBService(() -> mActivity.getDB().loadOPDSCatalogs(catalogs -> {
					updateOnlineCatalogs(catalogs, false);
					bOnlineCatalogsHidden = false;
					Drawable d = mActivity.getResources().getDrawable(R.drawable.icons8_toc_item_expanded);
					mImgOnlineCatalogs.setImageDrawable(d);
					mActivity.tintViewIcons(mImgOnlineCatalogs);
				}));
			}
			mTextSortAZ.setVisibility(bOnlineCatalogsHidden? View.INVISIBLE: View.VISIBLE);
			mImgSortAZ.setVisibility(bOnlineCatalogsHidden? View.INVISIBLE: View.VISIBLE);
		};
		mTextOnlineCatalogs.setOnClickListener(lstnr2);
		mImgOnlineCatalogs.setOnClickListener(lstnr2);

		OnClickListener lstnrAZ = v -> {
			bOnlineCatalogsSortAZ = !bOnlineCatalogsSortAZ;
			Properties props = new Properties(mActivity.settings());
			props.setProperty(Settings.PROP_APP_ROOT_VIEW_OPDS_SECTION_SORT_AZ, bOnlineCatalogsSortAZ? "1": "0");
			mActivity.setSettings(props, -1, false);
			if ((bOnlineCatalogsSortAZ) && (!bOnlineCatalogsHidden)) {
				Drawable d = mActivity.getResources().getDrawable(R.drawable.icons8_check_no_frame);
				mImgSortAZ.setImageDrawable(d);
				mActivity.tintViewIcons(mImgSortAZ);
			} else {
				mImgSortAZ.setImageDrawable(null);
			}
			if (!bOnlineCatalogsHidden) {
				mOnlineCatalogsScroll.removeAllViews();
				mActivity.waitForCRDBService(() -> mActivity.getDB().loadOPDSCatalogs(catalogs -> {
					updateOnlineCatalogs(catalogs, false);
				}));
			}
		};

		mTextSortAZ = mView.findViewById(R.id.tv_sort_a_z);
		mTextSortAZ.setPaintFlags(mTextSortAZ.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
		mImgSortAZ = mView.findViewById(R.id.iv_sort_a_z);

		mTextSortAZ.setOnClickListener(lstnrAZ);
		mImgSortAZ.setOnClickListener(lstnrAZ);

		mOnlineCatalogsScroll = mView.findViewById(R.id.scroll_online_catalogs);

		btnCalendar = view.findViewById(R.id.btn_calendar);
		btnCalendar.setOnClickListener(v -> {
			ReadCalendarDlg dlgCalendar = new ReadCalendarDlg(mActivity);
			dlgCalendar.show();
		});
		TextView tvRecentBooks = view.findViewById(R.id.recent_books_tv);
		tvRecentBooks.setPaintFlags(tvRecentBooks.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
		tvRecentBooks.setOnClickListener(v -> {
			mActivity.showRecentBooks();
		});
        btnStateToRead  = view.findViewById(R.id.book_state_toread);
        btnStateToRead.setOnClickListener(v -> {
			FileInfo dir = new FileInfo();
			dir.isDirectory = true;
			dir.pathname = FileInfo.STATE_TO_READ_TAG;
			dir.setFilename(mActivity.getString(R.string.folder_name_books_by_state_to_read));
			dir.isListed = true;
			dir.isScanned = true;
			mActivity.showDirectory(dir, "");
		});
		btnStateReading  = view.findViewById(R.id.book_state_reading);
		btnStateReading.setOnClickListener(v -> {
			FileInfo dir = new FileInfo();
			dir.isDirectory = true;
			dir.pathname = FileInfo.STATE_READING_TAG;
			dir.setFilename(mActivity.getString(R.string.folder_name_books_by_state_reading));
			dir.isListed = true;
			dir.isScanned = true;
			mActivity.showDirectory(dir, "");
		});
		btnStateFinished  = view.findViewById(R.id.book_state_finished);
		btnStateFinished.setOnClickListener(v -> {
			FileInfo dir = new FileInfo();
			dir.isDirectory = true;
			dir.pathname = FileInfo.STATE_FINISHED_TAG;
			dir.setFilename(mActivity.getString(R.string.folder_name_books_by_state_finished));
			dir.isListed = true;
			dir.isScanned = true;
			mActivity.showDirectory(dir, "");
		});
		btnRecentToRead  = view.findViewById(R.id.book_recent_toread);
		btnRecentToRead.setOnClickListener(v -> {
			bRecentToRead = !bRecentToRead;
			paintRecentButtons();
			refreshRecentBooks();
		});
		btnRecentReading  = view.findViewById(R.id.book_recent_reading);
		btnRecentReading.setOnClickListener(v -> {
			bRecentReading = !bRecentReading;
			paintRecentButtons();
			refreshRecentBooks();
		});
		btnRecentFinished  = view.findViewById(R.id.book_recent_finished);
		btnRecentFinished.setOnClickListener(v -> {
			bRecentFinished = !bRecentFinished;
			paintRecentButtons();
			refreshRecentBooks();
		});
		Drawable img = getContext().getResources().getDrawable(R.drawable.icons8_toc_item_normal);
		Drawable img1 = img.getConstantState().newDrawable().mutate();
		Drawable img2 = img.getConstantState().newDrawable().mutate();
		Drawable img3 = img.getConstantState().newDrawable().mutate();
		btnRecentToRead.setCompoundDrawablesWithIntrinsicBounds(img1, null, null, null);
		btnRecentReading.setCompoundDrawablesWithIntrinsicBounds(img2, null, null, null);
		btnRecentFinished.setCompoundDrawablesWithIntrinsicBounds(img3, null, null, null);
		ImageButton btnQuickSearch = view.findViewById(R.id.btn_quick_search);
		mActivity.tintViewIcons(btnQuickSearch, true);
		final EditText edQuickSearch = view.findViewById(R.id.quick_search);
		btnQuickSearch.setOnClickListener(v -> {
			try {
				btnQuickSearch.requestFocus();
				View view1 = mActivity.findViewById(android.R.id.content);
				if (view1 != null) {
					InputMethodManager imm = (InputMethodManager) mActivity.getSystemService(Context.INPUT_METHOD_SERVICE);
					imm.hideSoftInputFromWindow(view1.getWindowToken(), 0);
				}
			} catch (Exception e) {
				log.e(e.getMessage());
			}
			String sText = edQuickSearch.getText().toString();
			if (!StrUtils.isEmptyStr(sText)) {
				FileInfo dir = new FileInfo();
				dir.isDirectory = true;
				dir.pathname = FileInfo.QSEARCH_SHORTCUT_TAG;
				dir.setFilename(sText);
				dir.isListed = true;
				dir.isScanned = true;
				edQuickSearch.setText("");
				mActivity.showDirectory(dir, "");
			}
		});
		int colorBlue = themeColors.get(R.attr.colorThemeBlue);
		int colorGreen = themeColors.get(R.attr.colorThemeGreen);
		int colorGray = themeColors.get(R.attr.colorThemeGray);
		int colorGrayC = themeColors.get(R.attr.colorThemeGray2Contrast);

		btnStateReading.setTextColor(colorGreen);
        btnStateToRead.setTextColor(colorBlue);
        btnStateFinished.setTextColor(colorGray);
		btnRecentReading.setTextColor(colorGreen);
		btnRecentToRead.setTextColor(colorBlue);
		btnRecentFinished.setTextColor(colorGray);
		int colorGrayCT=Color.argb(128,Color.red(colorGrayC),Color.green(colorGrayC),Color.blue(colorGrayC));
		btnCalendar.setBackgroundColor(colorGrayCT);
        btnStateToRead.setBackgroundColor(colorGrayCT);
        btnStateReading.setBackgroundColor(colorGrayCT);
        btnStateFinished.setBackgroundColor(colorGrayCT);
		if (isEInk) Utils.setSolidButtonEink(btnStateToRead);
		if (isEInk) Utils.setSolidButtonEink(btnStateReading);
		if (isEInk) Utils.setSolidButtonEink(btnStateFinished);
		if (isEInk) Utils.setSolidButtonEink(btnCalendar);
		paintRecentButtons();
		edQuickSearch.setBackgroundColor(colorGrayCT);
		edQuickSearch.setPadding(8,8,8,8);
		if (isEInk) Utils.setSolidEditEink(edQuickSearch);

        updateCurrentBook(Services.getHistory().getLastBook());

		mView.findViewById(R.id.btn_menu).setOnClickListener(v -> showMenu(mView.findViewById(R.id.btn_menu), mView));
		mView.findViewById(R.id.btn_dic).setOnClickListener(v -> {
			DictsDlg dlg = new DictsDlg(mActivity, mActivity.getmReaderView(), "", null, true);
			dlg.show();
		});
		mActivity.tintViewIcons(mView.findViewById(R.id.btn_menu),true);
		mActivity.tintViewIcons(mView.findViewById(R.id.btn_dic),true);

		mView.findViewById(R.id.current_book).setOnClickListener(v -> {
			if (currentBook != null) {
				mActivity.loadDocument(currentBook.getFileInfo(), true);
			}

		});
		mView.findViewById(R.id.current_book).setOnLongClickListener(v -> {
			if (currentBook != null)
				mActivity.editBookInfo(Services.getScanner().createRecentRoot(), currentBook.getFileInfo());
			return true;
		});

		refreshRecentBooks();

		// Must be initialized FileSystemFolders.favoriteFolders firstly to exclude NullPointerException.
		mActivity.waitForCRDBService(() -> Services.getFileSystemFolders().loadFavoriteFolders(mActivity.getDB()));

        Services.getFileSystemFolders().addListener((object, onlyProperties) -> BackgroundThread.instance().postGUI(
        		() -> refreshFileSystemFolders(true)));

		BackgroundThread.instance().postGUI(() -> refreshOnlineCatalogs(true));

		BackgroundThread.instance().postGUI(() -> {
			if (Services.getScanner() != null)
				updateLibraryItems(Services.getScanner().getLibraryItems(), true);
		});

		removeAllViews();
		addView(mView);
		mActivity.tintViewIcons(mView);
		//setFocusable(false);
		//setFocusableInTouchMode(false);
//		requestFocus();
//		setOnTouchListener(new OnTouchListener() {
//			@Override
//			public boolean onTouch(View v, MotionEvent event) {
//				return true;
//			}
//		});
	}

	// called after user grant permissions for external storage
	public void refreshView() {
		updateDelimiterTheme(R.id.delimiter1);
		updateDelimiterTheme(R.id.delimiter2);
		updateDelimiterTheme(R.id.delimiter3);
		updateDelimiterTheme(R.id.delimiter4);
		updateDelimiterTheme(R.id.delimiter5);

		// Must be initialized FileSystemFolders.favoriteFolders firstly to exclude NullPointerException.
		mActivity.waitForCRDBService(() -> Services.getFileSystemFolders().loadFavoriteFolders(mActivity.getDB()));

		updateCurrentBook(Services.getHistory().getLastBook());
		refreshRecentBooks();

		BackgroundThread.instance().postGUI(() -> refreshFileSystemFolders(true));

		BackgroundThread.instance().postGUI(() -> {
			refreshOnlineCatalogs(true);
			if (Services.getScanner() != null)
				updateLibraryItems(Services.getScanner().getLibraryItems(), true);
		});
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		log.d("CRRootView.onTouchEvent(" + event.getAction() + ")");
		return false;
	}
	
	

	@Override
	public void onWindowFocusChanged(boolean hasWindowFocus) {
		log.d("CRRootView.onWindowFocusChanged(" + hasWindowFocus + ")");
		super.onWindowFocusChanged(hasWindowFocus);
	}

	public void onCoverpagesReady(ArrayList<CoverpageManager.ImageItem> files) {
		//invalidate();
		log.d("CRRootView.onCoverpagesReady(" + files + ")");
		CoverpageManager.invalidateChildImages(mView, files);
//		for (int i=0; i<mRecentBooksScroll.getChildCount(); i++) {
//			mRecentBooksScroll.getChildAt(i).invalidate();
//		}
//		//mRecentBooksScroll.invalidate();
		//ImageView cover = (ImageView)mView.findViewById(R.id.book_cover);
		//cover.invalidate();
//		//mView.invalidate();
	}

	@Override
	protected void onLayout(boolean changed, int l, int t, int r, int b) {
		r -= l;
		b -= t;
		t = 0;
		l = 0;
		mView.layout(l, t, r, b);
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		mView.measure(widthMeasureSpec, heightMeasureSpec);
        setMeasuredDimension(mView.getMeasuredWidth(), mView.getMeasuredHeight());
	}
	
	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		super.onSizeChanged(w, h, oldw, oldh);
	}

	public void showMenu() {
		showMenu(null, null);
	}

	public void showMenu(View anchor, View parentAnchor) {
		ReaderAction[] actions = {
			ReaderAction.ABOUT,
			ReaderAction.SHOW_DICTIONARY,
			ReaderAction.CURRENT_BOOK,
			ReaderAction.RECENT_BOOKS,
			ReaderAction.USER_MANUAL,
			ReaderAction.OPTIONS,
			ReaderAction.SAVE_LOGCAT,
			ReaderAction.INIT_APP_DIALOG,
			ReaderAction.HIDE,
			ReaderAction.EXIT,
		};
		mActivity.showActionsToolbarMenu(actions, anchor, parentAnchor, item -> {
			if (item == ReaderAction.EXIT) {
				mActivity.finish();
				return true;
			} else if (item == ReaderAction.HIDE) {
				mActivity.onBackPressed();
				return true;
			} else if (item == ReaderAction.ABOUT) {
				mActivity.showAboutDialog();
				return true;
			} else if (item == ReaderAction.SHOW_DICTIONARY) {
				DictsDlg dlg = new DictsDlg(mActivity, mActivity.getmReaderView(), "", null, true);
				dlg.show();
				return true;
			} else if (item == ReaderAction.RECENT_BOOKS) {
				mActivity.showRecentBooks();
				return true;
			} else if (item == ReaderAction.CURRENT_BOOK) {
				mActivity.showCurrentBook();
				return true;
			} else if (item == ReaderAction.USER_MANUAL) {
				mActivity.showManual();
				return true;
			} else if (item == ReaderAction.OPTIONS) {
				mActivity.showOptionsDialog(OptionsDialog.Mode.READER);
				return true;
			} else if (item == ReaderAction.SAVE_LOGCAT) {
				mActivity.createLogcatFile();
				return true;
			} else if (item == ReaderAction.INIT_APP_DIALOG) {
				InitAppDialog iad = new InitAppDialog(mActivity);
				iad.show();
				return true;
			}
			return false;
		});
	}

	public void showAddCatalogMenu(View anchor, View parentAnchor) {
		ReaderAction[] actions = {
				ReaderAction.ADD_OPDS_CATALOG,
				ReaderAction.ADD_REMOVE_LITRES_CATALOG,
				ReaderAction.ADD_CALIBRE_CATALOG_LOCAL,
				ReaderAction.ADD_CALIBRE_CATALOG_YD
		};
		mActivity.showActionsToolbarMenu(actions, anchor, parentAnchor, item -> {
			if (item == ReaderAction.ADD_OPDS_CATALOG) {
				mActivity.editOPDSCatalog(null);
				return true;
			} else if (item == ReaderAction.ADD_REMOVE_LITRES_CATALOG) {
				bLitresHidden = !bLitresHidden;
				Properties props = new Properties(mActivity.settings());
				props.setProperty(Settings.PROP_CLOUD_LITRES_DISABLED, bLitresHidden?"1":"0");
				mActivity.setSettings(props, -1, false);
				mActivity.settings().setProperty(Settings.PROP_CLOUD_LITRES_DISABLED, bLitresHidden?"1":"0");
				mOnlineCatalogsScroll.removeAllViews();
				mActivity.waitForCRDBService(() -> mActivity.getDB().loadOPDSCatalogs(catalogs -> {
					updateOnlineCatalogs(catalogs, false);
				}));
				return true;
			} else if (item == ReaderAction.ADD_CALIBRE_CATALOG_LOCAL) {
				//mActivity.showToast("Work in progress...");
				//return false;
				mActivity.addOrEditCalibreCatalog(null, true, "");
				return true;
			} else if (item == ReaderAction.ADD_CALIBRE_CATALOG_YD) {
				mActivity.showToast(R.string.create_remote_calibre_msg);
				//return false;
				//mActivity.addOrEditCalibreCatalog(null, true, "");
				return true;
			} else if (item == ReaderAction.SAVE_LOGCAT) {
				mActivity.createLogcatFile();
			}
			return false;
		});
	}

	public void showCalibreMenu(View anchor, View parentAnchor, FileInfo calibreCatalog) {
		ReaderAction[] actions = {
				ReaderAction.CALIBRE_SEARCH,
				ReaderAction.CALIBRE_SHOW_AUTHORS,
				ReaderAction.CALIBRE_SHOW_TITLES,
				ReaderAction.CALIBRE_SHOW_SERIES,
				ReaderAction.CALIBRE_SHOW_RATING,
				ReaderAction.CALIBRE_SHOW_PUB_DATES,
				ReaderAction.CALIBRE_SHOW_TAGS
		};
		mActivity.showActionsToolbarMenu(actions, anchor, parentAnchor, item -> {
			if (item == ReaderAction.CALIBRE_SHOW_AUTHORS) {
				FileInfo fi = new FileInfo();
				fi.isDirectory = true;
				fi.isScanned = true;
				fi.isListed = true;
				fi.id = -1L;
				fi.pathname = calibreCatalog.pathname;
				fi.setFilename(AUTHORS_TAG);
				mActivity.showDirectory(fi, "");
				return true;
			}
//			else if (item == ReaderAction.ADD_LITRES_CATALOG) {
//				mActivity.showToast("Coming soon...");
//				return true;
//			} else if (item == ReaderAction.ADD_CALIBRE_CATALOG_LOCAL) {
//				mActivity.editCalibreCatalog(null);
//				return true;
//			} else if (item == ReaderAction.ADD_CALIBRE_CATALOG_YD) {
//				mActivity.editCalibreCatalog(null);
//				return true;
//			}
			return false;
		});
	}

}

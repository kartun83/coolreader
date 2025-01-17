package org.coolreader.crengine;

import java.util.HashMap;

import org.coolreader.R;
import org.coolreader.plugins.AuthenticationCallback;
import org.coolreader.plugins.OnlineStoreRegistrationParam;
import org.coolreader.plugins.OnlineStoreWrapper;

import android.content.res.TypedArray;
import android.graphics.Color;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

public class OnlineStoreNewAccountDialog extends BaseDialog {
	private BaseActivity mActivity;
	private OnlineStoreWrapper mPlugin;
	private LayoutInflater mInflater;
	private Runnable mOnLoginHandler;
	public OnlineStoreNewAccountDialog(BaseActivity activity, OnlineStoreWrapper plugin, Runnable onLoginHandler)
	{
		super(activity, null, false, false);
		DisplayMetrics outMetrics = new DisplayMetrics();
		activity.getWindowManager().getDefaultDisplay().getMetrics(outMetrics);
		this.mActivity = activity;
		this.mPlugin = plugin;
		this.mOnLoginHandler = onLoginHandler;
	}

	@Override
	protected void onCreate() {
        setCancelable(true);
        setCanceledOnTouchOutside(true);
        super.onCreate();
	}

	
    TextView lblTitle;
    TextView lblDescription;
    Button btnRegister;
    EditText edLogin;
    EditText edPassword;
    EditText edPassword2;
    EditText edEmail;
    EditText edFirstName;
    EditText edLastName;
    EditText edMiddleName;
    EditText edPhone;
    EditText edCity;
    CheckBox cbSubscribe;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		int colorGray;
		int colorGrayC;
		int colorIcon;
		TypedArray a = this.mActivity.getTheme().obtainStyledAttributes(new int[]
				{R.attr.colorThemeGray2, R.attr.colorThemeGray2Contrast, R.attr.colorIcon});
		colorGray = a.getColor(0, Color.GRAY);
		colorGrayC = a.getColor(1, Color.GRAY);
		colorIcon = a.getColor(2, Color.GRAY);
		a.recycle();
		int colorGrayCT= colorGrayC;// Color.argb(128,Color.red(colorGrayC),Color.green(colorGrayC),Color.blue(colorGrayC));

		mInflater = LayoutInflater.from(getContext());
        ViewGroup view = (ViewGroup)mInflater.inflate(R.layout.online_store_new_account_dialog, null);
        
        ImageButton btnBack = view.findViewById(R.id.base_dlg_btn_back);
        btnBack.setOnClickListener(v -> onNegativeButtonClick());
        btnRegister = view.findViewById(R.id.btn_new_account);
        btnRegister.setOnClickListener(v -> onPositiveButtonClick());
        btnRegister.setBackgroundColor(colorGrayCT);

        lblTitle = view.findViewById(R.id.dlg_title);
        lblDescription = view.findViewById(R.id.lbl_description);

		lblTitle.setText(mPlugin.getName());
		lblDescription.setText(mPlugin.getDescription());
		
        edLogin = view.findViewById(R.id.ed_login);
        edPassword = view.findViewById(R.id.ed_password);
        edPassword2 = view.findViewById(R.id.ed_password_repeat);
        edFirstName = view.findViewById(R.id.ed_first_name);
        edLastName = view.findViewById(R.id.ed_last_name);
        edMiddleName = view.findViewById(R.id.ed_middle_name);
        edEmail = view.findViewById(R.id.ed_email);
        edCity = view.findViewById(R.id.ed_city);
        edPhone = view.findViewById(R.id.ed_phone);
        cbSubscribe = view.findViewById(R.id.cb_subscribe);

        
        edLogin.setText(mPlugin.getLogin());
        edPassword.setText(mPlugin.getPassword());
        edPassword2.setText(mPlugin.getPassword());
		
        setView(view);
		progress = new ProgressPopup(mActivity, view);
	}
	
	private ProgressPopup progress;
	
	static boolean isEmpty(String s) {
		return s == null || s.trim().length() == 0;
	}
	
	protected void showError(String msg) {
		mActivity.showToast(msg);
	}
	
	@Override
	protected void onPositiveButtonClick() {
		String login = edLogin.getText().toString().trim();
		String password = edPassword.getText().toString().trim();
		String password2 = edPassword2.getText().toString().trim();
		if (isEmpty(login)) {
			showError("Mandatory field: login");
			return;
		}
		if (isEmpty(password)) {
			showError("Mandatory field: password");
			return;
		}
		if (isEmpty(password2)) {
			showError("Mandatory field: repeat password");
			return;
		}
		if (!password.equals(password2)) {
			showError("Both passwords should match!");
			return;
		}
		String firstName = edFirstName.getText().toString().trim();
		String lastName = edLastName.getText().toString().trim();
		String middleName = edMiddleName.getText().toString().trim();
		String email = edEmail.getText().toString().trim();
		String city = edCity.getText().toString().trim();
		String phone = edPhone.getText().toString().trim();
		boolean subscribe = cbSubscribe.isChecked();
		HashMap<String, String> params = new HashMap<String, String>();
		params.put(OnlineStoreRegistrationParam.NEW_ACCOUNT_PARAM_LOGIN, login);
		params.put(OnlineStoreRegistrationParam.NEW_ACCOUNT_PARAM_PASSWORD, password);
		params.put(OnlineStoreRegistrationParam.NEW_ACCOUNT_PARAM_EMAIL, email);
		params.put(OnlineStoreRegistrationParam.NEW_ACCOUNT_PARAM_FIRST_NAME, firstName);
		params.put(OnlineStoreRegistrationParam.NEW_ACCOUNT_PARAM_LAST_NAME, lastName);
		params.put(OnlineStoreRegistrationParam.NEW_ACCOUNT_PARAM_MIDDLE_NAME, middleName);
		params.put(OnlineStoreRegistrationParam.NEW_ACCOUNT_PARAM_CITY, city);
		params.put(OnlineStoreRegistrationParam.NEW_ACCOUNT_PARAM_PHONE, phone);
		params.put(OnlineStoreRegistrationParam.NEW_ACCOUNT_PARAM_SUBSCRIBE, subscribe ? "1" : "0");
		
		progress.show();
		L.i("trying to register new LitRes account " + login);
		mPlugin.registerNewAccount(params, new AuthenticationCallback() {
			@Override
			public void onError(int errorCode, String errorMessage) {
				L.e("registerNewAccount - error " + errorCode + " : " + errorMessage);
				progress.hide();
				mActivity.showToast(mActivity.getString(R.string.online_store_error_succesful_registration) + " " + errorMessage);
			}
			@Override
			public void onSuccess() {
				L.i("registerNewAccount - successful");
				progress.hide();
				mActivity.showToast(R.string.online_store_error_succesful_registration);
				OnlineStoreNewAccountDialog.super.onPositiveButtonClick();
				mOnLoginHandler.run();
			}
		});
	}

	@Override
	protected void onNegativeButtonClick() {
		super.onNegativeButtonClick();
	}
}


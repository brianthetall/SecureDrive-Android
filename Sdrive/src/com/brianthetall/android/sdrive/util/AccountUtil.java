package com.brianthetall.android.sdrive.util;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Context;

import com.brianthetall.android.sdrive.Constant;
import com.google.android.gms.auth.GoogleAuthUtil;

public class AccountUtil {

    private AccountUtil() {}
    
    public static String[] getAccountNames(Context context) {
        Account[] accounts = AccountManager.get(context)
                .getAccountsByType(GoogleAuthUtil.GOOGLE_ACCOUNT_TYPE);
        String[] names = new String[accounts.length];
        for (int i = 0; i < names.length; i++) {
            names[i] = accounts[i].name;
        }
        return names;
    }
    
    public static String getMostRecentAccountName(Context context, String defaultAccount) {
        return context.getSharedPreferences(Constant.PREF_NAME_AUTH, Context.MODE_PRIVATE)
                .getString(Constant.ACCOUNT_NAME, defaultAccount);
    }

}

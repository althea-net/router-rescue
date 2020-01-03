/*
 * Mercury-SSH
 * Copyright (C) 2017 Skarafaz
 *
 * This file is part of Mercury-SSH.
 *
 * Mercury-SSH is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * Mercury-SSH is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Mercury-SSH.  If not, see <http://www.gnu.org/licenses/>.
 */

package it.skarafaz.mercury;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.view.ViewConfiguration;
import it.skarafaz.mercury.fragment.ProgressDialogFragment;
import org.greenrobot.eventbus.EventBus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;

public class MercuryApplication extends Application {
    private static final String S_HAS_PERMANENT_MENU_KEY = "sHasPermanentMenuKey";
    private static final Logger logger = LoggerFactory.getLogger(MercuryApplication.class);
    private static Context context;

    public static Context getContext() {
        return context;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        context = this;

        EventBus.builder().addIndex(new EventBusIndex()).build();

        // hack for devices with hw options button
        try {
            ViewConfiguration config = ViewConfiguration.get(this);
            Field menuKeyField = ViewConfiguration.class.getDeclaredField(S_HAS_PERMANENT_MENU_KEY);
            if (menuKeyField != null) {
                menuKeyField.setAccessible(true);
                menuKeyField.setBoolean(config, false);
            }
        } catch (Exception e) {
            logger.error(e.getMessage().replace("\n", " "));
        }
        // allows interacting with offline routers without turning on airplane mode
        registerHandler();
    }

    public static void showProgressDialog(FragmentManager manager, String content) {
        FragmentTransaction transaction = manager.beginTransaction();
        transaction.add(ProgressDialogFragment.newInstance(content), ProgressDialogFragment.TAG);
        transaction.commitAllowingStateLoss();
    }

    public static void dismissProgressDialog(FragmentManager manager) {
        FragmentTransaction transaction = manager.beginTransaction();
        Fragment fragment = manager.findFragmentByTag(ProgressDialogFragment.TAG);
        if (fragment != null) {
            transaction.remove(fragment);
        }
        transaction.commitAllowingStateLoss();
    }

    public static boolean hasPermission(String permission) {
        return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED;
    }

    public static boolean requestPermission(Activity activity, int requestCode, String permission) {
        boolean requested = false;
        if (!ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)) {
            ActivityCompat.requestPermissions(activity, new String[]{permission}, requestCode);
            requested = true;
        }
        return requested;
    }

    public static boolean isExternalStorageReadable() {
        String state = Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED.equals(state) || Environment.MEDIA_MOUNTED_READ_ONLY.equals(state);
    }

    public static boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED.equals(state);
    }

    private void registerHandler() {
        final Context context = MercuryApplication.getContext();
        final ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkRequest.Builder builder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            builder = new NetworkRequest.Builder();
            //set the transport type do WIFI
            builder.addTransportType(NetworkCapabilities.TRANSPORT_WIFI);

            connectivityManager.requestNetwork(builder.build(), new ConnectivityManager.NetworkCallback() {
                @Override
                public void onAvailable(Network network) {
                    //Application.logger.log(String.format("We are connected to a new wifi network, ssid %s", NetworkUtils.getCurrentSsid()));
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        if (Build.VERSION.RELEASE.equalsIgnoreCase("6.0")) {
                            if (!Settings.System.canWrite(context)) {
                                Intent goToSettings = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS);
                                goToSettings.setData(Uri.parse("package:" + context.getPackageName()));
                                context.startActivity(goToSettings);
                            }
                        }
                        connectivityManager.bindProcessToNetwork(null);
                        connectivityManager.bindProcessToNetwork(network);

                    } else {
                        //This method was deprecated in API level 23
                        ConnectivityManager.setProcessDefaultNetwork(null);
                        ConnectivityManager.setProcessDefaultNetwork(network);

                    }
                }
            });
        }
    }
}

package tk.parmclee.o_droid;


import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.support.v4.app.Fragment;
import android.widget.Toast;

public class RuntimePermissionHelper {

    public static final int REQUEST_CODE_PERMISSION_WRITE_EXTERNAL_STORAGE = 2;
    public static final int REQUEST_CODE_LOCATION = 3;

    @TargetApi(23)
    public static boolean checkAndRequestStoragePermission(Activity activity, Fragment fragment) {
        String perms[] = {Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE};
        boolean read = (activity.checkSelfPermission(perms[0]) ==
                PackageManager.PERMISSION_GRANTED);
        boolean write = (activity.checkSelfPermission(perms[0]) ==
                PackageManager.PERMISSION_GRANTED);
        boolean accessDenied = !read || !write;
        if (accessDenied) {
            if (activity.shouldShowRequestPermissionRationale(perms[0]) ||
                    activity.shouldShowRequestPermissionRationale(perms[1]))
                Toast.makeText(activity.getApplicationContext(),
                        activity.getString(R.string.storage_permissions),
                        Toast.LENGTH_SHORT).show();
            if (fragment != null) {
                fragment.requestPermissions(perms, REQUEST_CODE_PERMISSION_WRITE_EXTERNAL_STORAGE);
            } else {
                activity.requestPermissions(perms, REQUEST_CODE_PERMISSION_WRITE_EXTERNAL_STORAGE);
            }
        }
        return !accessDenied;
    }

    @TargetApi(23)
    public static boolean checkAndRequestLocationPermission(Activity activity, Fragment fragment) {
        String perms[] = {Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION};
        boolean fine = (activity.checkSelfPermission(perms[0]) ==
                PackageManager.PERMISSION_GRANTED);
        boolean coarse = (activity.checkSelfPermission(perms[0]) ==
                PackageManager.PERMISSION_GRANTED);
        boolean accessDenied = !fine || !coarse;
        if (accessDenied) {
            if (activity.shouldShowRequestPermissionRationale(perms[0]) ||
                    activity.shouldShowRequestPermissionRationale(perms[1]))
                Toast.makeText(activity.getApplicationContext(),
                        activity.getString(R.string.location_permissions),
                        Toast.LENGTH_SHORT).show();
            if (fragment != null) fragment.requestPermissions(perms, REQUEST_CODE_LOCATION);
            else activity.requestPermissions(perms, REQUEST_CODE_LOCATION);
        }
        return !accessDenied;
    }

}


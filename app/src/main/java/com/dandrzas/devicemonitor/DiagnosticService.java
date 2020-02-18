package com.dandrzas.devicemonitor;

import android.Manifest;
import android.app.ActivityManager;
import android.app.AppOpsManager;
import android.app.IntentService;
import android.app.usage.NetworkStats;
import android.app.usage.NetworkStatsManager;
import android.content.Intent;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Environment;
import android.os.SystemClock;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.telephony.gsm.GsmCellLocation;

import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class DiagnosticService extends IntentService {

    public static volatile boolean shouldContinueService = true;
    private static final long MEGABYTE = 1024L * 1024L;

    public static final String ACTION_START_DIAG = "com.dandrzas.mtm_projekt.action.START_DIAG";
    public static final String RESULT_NETWORK_DIAG = "com.dandrzas.mtm_projekt.result.NETWORK_DIAG";
    public static final String RESULT_HARDWARE_DIAG = "com.dandrzas.mtm_projekt.result.HARDWARE_DIAG";
    public static final String RESULT_NETWORK_TYPE = "com.dandrzas.mtm_projekt.result.NETWORK_TYPE";
    public static final String RESULT_NETWORK_SPEED = "com.dandrzas.mtm_projekt.result.NETWORK_SPEED";
    public static final String RESULT_NETWORK_DOWNLOAD_SIZE = "com.dandrzas.mtm_projekt.result.NETWORK_DOWNLOAD_SIZE";
    public static final String RESULT_HARDWARE_CPU_HIST = "com.dandrzas.mtm_projekt.result.HARDWARE_CPU";
    public static final String RESULT_HARDWARE_RAM_HIST = "com.dandrzas.mtm_projekt.result.HARDWARE_RAM";
    public static final String RESULT_HARDWARE_RAM = "com.dandrzas.mtm_projekt.result.HARDWARE_RAM_PERC";

    private float[] cpuLoadHist = new float[30];
    private float[] ramUsagePercHist = new float[30];
    private float[] downloadSpeedHist = new float[30];
    private String[] resultStringData = new String[10];

    private float ramUsageMB, speed;
    private int _packageUId;
    private String _subscriberId, FILENAME, networkType = "", prevNetworkType = "";
    private long downloadedData, downloadedDataStart, downloadedDataMB, lastDownloadedData;
    private File _file;

    public DiagnosticService() {
        super("DiagnosticService");
    }

    public static void startActionDiag(Context context) {
        Intent intent = new Intent(context, DiagnosticService.class);
        intent.setAction(ACTION_START_DIAG);
        shouldContinueService = true;
        context.startService(intent);
    }

    @Override
    protected void onHandleIntent(Intent intent) {

        if (intent != null) {
            final String action = intent.getAction();
            if (ACTION_START_DIAG.equals(action)) {

                // Utworzenie pliku
                DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
                FILENAME = "MTM_Projekt_" + dateFormat.format(new Date()) + ".txt";
                try {
                    File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + "/Device Monitor/");
                    if (!file.exists()) {
                        file.mkdirs();
                    }
                    _file = new File(file, FILENAME);
                    _file.createNewFile();
                } catch (Exception e) {
                    e.printStackTrace();
                }

                // Pętla odczytująca dane
                while (shouldContinueService) {
                    handleNetworkDiag();
                    handleHardwareDiag();

                    try {
                        String dataToSave = networkType + "," + downloadedDataMB + "," + speed + "," + cpuLoadHist[0] + "," + ramUsagePercHist[0] + "," + ramUsageMB;
                        SaveToFile(dataToSave);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }


                    SystemClock.sleep(900);
                }

            }
        }
    }


    /////////////
    // Diagnostyka sprzętowa

    private void handleHardwareDiag() {

        // przesunięcie elementów tablicy z danymi
        for (int i = 29; i >= 1; i--) {
            cpuLoadHist[i] = cpuLoadHist[i - 1];
            ramUsagePercHist[i] = ramUsagePercHist[i - 1];
        }

        float[] cores = CpuInfo.getCoresUsage();
        cpuLoadHist[0] = (float) CpuInfo.getCpuUsage(cores);

        ActivityManager activityManager = (ActivityManager) getApplicationContext().getSystemService(getApplicationContext().ACTIVITY_SERVICE);
        ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
        activityManager.getMemoryInfo(memoryInfo);

        double useRAM = (double) (memoryInfo.totalMem - memoryInfo.availMem) / 0x100000L;
        int percentUsedRAM = (int) (((memoryInfo.totalMem - memoryInfo.availMem) / (double) memoryInfo.totalMem) * 100.0);

        ramUsagePercHist[0] = (float) (((memoryInfo.totalMem - memoryInfo.availMem) / (double) memoryInfo.totalMem) * 100.0);
        ramUsageMB = (float) (memoryInfo.totalMem - memoryInfo.availMem) / 0x100000L;

        Intent localResultIntent = new Intent(RESULT_HARDWARE_DIAG);

        localResultIntent.putExtra(RESULT_HARDWARE_CPU_HIST, cpuLoadHist);
        localResultIntent.putExtra(RESULT_HARDWARE_RAM_HIST, ramUsagePercHist);
        localResultIntent.putExtra(RESULT_HARDWARE_RAM, Float.toString(ramUsageMB));

        LocalBroadcastManager.getInstance(this).sendBroadcast(localResultIntent);

    }


    ////////////////
    // Diagnostyka połączenia sieciowego

    private void handleNetworkDiag() {

        // przesunięcie elementów tablicy z danymi
        for (int i = 29; i >= 1; i--) {
            downloadSpeedHist[i] = downloadSpeedHist[i - 1];
        }
        TelephonyManager telephonyManager = (TelephonyManager) getApplicationContext().getSystemService(Context.TELEPHONY_SERVICE);

        if ((ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED)
                && (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED)) {

            ConnectivityManager cm = (ConnectivityManager) getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo activeNetwork = cm.getActiveNetworkInfo();

            if (activeNetwork != null) {
                switch (activeNetwork.getType()) {
                    case ConnectivityManager.TYPE_WIFI:
                        WifiManager wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
                        WifiInfo info = wifiManager.getConnectionInfo();
                        String ssid = info.getSSID();
                        networkType = "Wi-fi\nSSID: " + ssid + "\n";
                        downloadedData = getDownloadedDataKb(getApplicationContext(), false);
                        break;
                    case ConnectivityManager.TYPE_MOBILE:
                        networkType = getNetworkType(getApplicationContext()) + "\n";
                        downloadedData = getDownloadedDataKb(getApplicationContext(), true);
                        break;
                    default:
                        break;
                }
            } else {
                networkType = "brak połączenia do Internetu";
            }

            if (networkType.equals(prevNetworkType))
                speed = (float) ((downloadedData - lastDownloadedData) * 8 / MEGABYTE);   // B -> Kb
            else {
                speed = 0;
                downloadedDataStart = downloadedData;
            }

            downloadedDataMB = (downloadedData-downloadedDataStart) / MEGABYTE;
            lastDownloadedData = downloadedData;
            prevNetworkType = networkType;
            downloadSpeedHist[0] = speed;

            Intent localResultIntent = new Intent(RESULT_NETWORK_DIAG);

            localResultIntent.putExtra(RESULT_NETWORK_TYPE, networkType);
            localResultIntent.putExtra(RESULT_NETWORK_SPEED, downloadSpeedHist);
            localResultIntent.putExtra(RESULT_NETWORK_DOWNLOAD_SIZE, Long.toString(downloadedDataMB));

            LocalBroadcastManager.getInstance(this).sendBroadcast(localResultIntent);

        }
    }


    private long getDownloadedDataKb(Context context, Boolean isMobileNet) {
        AppOpsManager appOps = (AppOpsManager) getSystemService(Context.APP_OPS_SERVICE);
        int mode = appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(), getPackageName());
        if (mode != AppOpsManager.MODE_ALLOWED) {
            SystemClock.sleep(10000);
            Intent intent = new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS);
            startActivity(intent);
        }

        try {
            NetworkStatsManager networkStatsManager = (NetworkStatsManager) getApplicationContext().getSystemService(Context.NETWORK_STATS_SERVICE);
            if (isMobileNet) {
                NetworkStats.Bucket bucket;
                try {
                    bucket = networkStatsManager.querySummaryForDevice(ConnectivityManager.TYPE_MOBILE,
                            _subscriberId,
                            0,
                            System.currentTimeMillis());

                    return bucket.getRxBytes();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return -1;

            } else {
                NetworkStats.Bucket bucket;
                try {
                    bucket = networkStatsManager.querySummaryForDevice(ConnectivityManager.TYPE_WIFI,
                            "",
                            0,
                            System.currentTimeMillis());
                    return bucket.getRxBytes();
                } catch (Exception e) {
                    e.printStackTrace();
                }

                return -1;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return -1;
    }

    private String getNetworkType(Context context) {
        String btsId = "-";
        TelephonyManager mTelephonyManager = (TelephonyManager)
                context.getSystemService(Context.TELEPHONY_SERVICE);
        int networkType = mTelephonyManager.getNetworkType();

        _subscriberId = mTelephonyManager.getSubscriberId();

        final GsmCellLocation location = (GsmCellLocation) mTelephonyManager.getCellLocation();
        if (location != null) {
            btsId = "LAC: " + location.getLac() + "\nCID: " + location.getCid();
        } else {
            btsId = "Nieznane LAC i CID";
        }

        switch (networkType) {
            case TelephonyManager.NETWORK_TYPE_GPRS:
            case TelephonyManager.NETWORK_TYPE_EDGE:
            case TelephonyManager.NETWORK_TYPE_CDMA:
            case TelephonyManager.NETWORK_TYPE_1xRTT:
            case TelephonyManager.NETWORK_TYPE_IDEN:
                return "2G\n" + btsId;
            case TelephonyManager.NETWORK_TYPE_UMTS:
            case TelephonyManager.NETWORK_TYPE_EVDO_0:
            case TelephonyManager.NETWORK_TYPE_EVDO_A:
            case TelephonyManager.NETWORK_TYPE_HSDPA:
            case TelephonyManager.NETWORK_TYPE_HSUPA:
            case TelephonyManager.NETWORK_TYPE_HSPA:
            case TelephonyManager.NETWORK_TYPE_EVDO_B:
            case TelephonyManager.NETWORK_TYPE_EHRPD:
            case TelephonyManager.NETWORK_TYPE_HSPAP:
                return "3G\n" + btsId;
            case TelephonyManager.NETWORK_TYPE_LTE:
                return "4G\n" + btsId;
            case TelephonyManager.NETWORK_TYPE_NR:
                return "5G\n" + btsId;
            default:
                return "Nieznany rodzaj sieci";
        }
    }

    private void SaveToFile(String data) {
        try {
            FileOutputStream fileinput = new FileOutputStream(_file, true);
            PrintStream printstream = new PrintStream(fileinput);
            printstream.print(data + "\n\n");
            fileinput.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

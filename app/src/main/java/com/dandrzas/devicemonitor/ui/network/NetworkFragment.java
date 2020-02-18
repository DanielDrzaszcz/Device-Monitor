package com.dandrzas.devicemonitor.ui.network;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.icu.text.SimpleDateFormat;
import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.dandrzas.devicemonitor.DiagnosticService;
import com.dandrzas.devicemonitor.PlotterView;
import com.dandrzas.devicemonitor.R;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import java.io.FileOutputStream;
import java.util.Date;

public class NetworkFragment extends Fragment {

    private TextView textViewNetworkType, textViewData, textViewSpeed;
    private PlotterView plotterDownloadSpeed;
    private float[] downloadSpeed = new float[30];

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        View root = inflater.inflate(R.layout.fragment_network, container, false);
        textViewNetworkType = root.findViewById(R.id.textView);
        textViewData = root.findViewById(R.id.textViewData);
        textViewSpeed = root.findViewById(R.id.textViewSpeed);
        plotterDownloadSpeed = (PlotterView) root.findViewById(R.id.UIPlotterViewDownloadSpeed);

        FloatingActionButton fab = root.findViewById(R.id.UISaveNetwork);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //Zapis pliku
                plotterDownloadSpeed.setDrawingCacheEnabled(true);
                Bitmap bitmap = plotterDownloadSpeed.getDrawingCache();
                SimpleDateFormat formatter = new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss");
                Date now = new Date();
                String fileDateFime = formatter.format(now);
                String filePath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + "/Device Monitor/";
                String filename = "Device_Monitor" + fileDateFime + "_DownloadSpeed.jpg";
                try {
                    FileOutputStream file = new FileOutputStream(filePath+filename);
                    bitmap.compress(Bitmap.CompressFormat.PNG, 50, file);
                    Snackbar.make(view, "Zapis zakończony pomyślnie.  Lokalizacja:\n" + filePath, Snackbar.LENGTH_LONG)
                            .setAction("Action", null).show();

                } catch (Exception e) {
                    e.printStackTrace();
                    Snackbar.make(view, "Błąd zapisu do pliku...", Snackbar.LENGTH_LONG)
                            .setAction("Action", null).show();
                }
            }
        });

        IntentFilter statusIntentFilter = new IntentFilter(DiagnosticService.RESULT_NETWORK_DIAG);
        NetworkDiagDataReceiver networkDiagDataReceiver = new NetworkDiagDataReceiver();
        LocalBroadcastManager.getInstance(getContext()).registerReceiver(networkDiagDataReceiver, statusIntentFilter);

        return root;
    }


    private class NetworkDiagDataReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {

            if (intent.hasExtra(DiagnosticService.RESULT_NETWORK_TYPE)) {
                String networkData = intent.getStringExtra(DiagnosticService.RESULT_NETWORK_TYPE);
                if (networkData != null) textViewNetworkType.setText(networkData);
            }
            if (intent.hasExtra(DiagnosticService.RESULT_NETWORK_SPEED)) {
                downloadSpeed = intent.getFloatArrayExtra(DiagnosticService.RESULT_NETWORK_SPEED);
                {
                    if (downloadSpeed != null) {
                        plotterDownloadSpeed.setData(downloadSpeed);
                        plotterDownloadSpeed.invalidate();
                        textViewSpeed.setText(downloadSpeed[0] + " Mb/s");
                    }
                }
            }

            if (intent.hasExtra(DiagnosticService.RESULT_NETWORK_DOWNLOAD_SIZE)) {
                String downloadedData = intent.getStringExtra(DiagnosticService.RESULT_NETWORK_DOWNLOAD_SIZE);
                if (downloadedData != null) textViewData.setText(downloadedData + " MB");
            }
        }
    }


}
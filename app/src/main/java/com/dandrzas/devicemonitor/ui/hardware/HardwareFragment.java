package com.dandrzas.devicemonitor.ui.hardware;

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

public class HardwareFragment extends Fragment {

    private TextView textViewRAM, textViewRAMProc, textViewCPU;
    private PlotterView plotterCPU, plotterRAM;


    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        View root = inflater.inflate(R.layout.fragment_hardware, container, false);
        textViewCPU = root.findViewById(R.id.text_CPU);
        textViewRAM = root.findViewById(R.id.text_RAM);
        textViewRAMProc = root.findViewById(R.id.text_RAM_CPU);
        plotterCPU = root.findViewById(R.id.UIPlotterCPU);
        plotterRAM = root.findViewById(R.id.UIPlotterViewRAM);

        FloatingActionButton fab = root.findViewById(R.id.UISaveHardware);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //Zapis pliku
                plotterCPU.setDrawingCacheEnabled(true);
                plotterRAM.setDrawingCacheEnabled(true);
                Bitmap bitmap1 = plotterCPU.getDrawingCache();
                Bitmap bitmap2 = plotterRAM.getDrawingCache();
                SimpleDateFormat formatter = new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss");
                Date now = new Date();
                String fileDateFime = formatter.format(now);
                String filePath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + "/Device Monitor/";
                String filename1 = "Device_Monitor_" + fileDateFime + "_CPU.jpg";
                String filename2 = "Device_Monitor_" + fileDateFime + "_RAM.jpg";
                try {
                    FileOutputStream file1 = new FileOutputStream(filePath+filename1);
                    FileOutputStream file2 = new FileOutputStream(filePath+filename2);
                    bitmap1.compress(Bitmap.CompressFormat.PNG, 50, file1);
                    bitmap2.compress(Bitmap.CompressFormat.PNG, 50, file2);
                    Snackbar.make(view, "Zapis zakończony pomyślnie.  Lokalizacja:\n" + filePath, Snackbar.LENGTH_LONG)
                            .setAction("Action", null).show();

                } catch (Exception e) {
                    e.printStackTrace();
                    Snackbar.make(view, "Błąd zapisu do pliku...", Snackbar.LENGTH_LONG)
                            .setAction("Action", null).show();
                }
            }
        });

        IntentFilter statusIntentFilter = new IntentFilter(DiagnosticService.RESULT_HARDWARE_DIAG);
        HardwareDiagDataReceiver hardwareDiagDataReceiver = new HardwareDiagDataReceiver();
        LocalBroadcastManager.getInstance(getContext()).registerReceiver(hardwareDiagDataReceiver, statusIntentFilter);

        return root;
    }

    private class HardwareDiagDataReceiver extends BroadcastReceiver
    {
        @Override
        public void onReceive(Context context, Intent intent) {
            float[] cpuLoadHist = new float[30];
            float[] ramUsageHist = new float[30];

            // CPU
            if (intent.hasExtra(DiagnosticService.RESULT_HARDWARE_CPU_HIST))
            {
                cpuLoadHist = intent.getFloatArrayExtra(DiagnosticService.RESULT_HARDWARE_CPU_HIST);
                if(cpuLoadHist!=null){
                    if (cpuLoadHist[0]!=-1)
                    {
                        plotterCPU.setData(cpuLoadHist);
                        plotterCPU.invalidate();
                        textViewCPU.setText("CPU: "+Float.toString(cpuLoadHist[0])+" %");
                    }
                    else textViewCPU.setText("CPU: brak danych.\nDostępne do wersji 7.1.1 systemu.");
                }
            }

            // RAM
            if (intent.hasExtra(DiagnosticService.RESULT_HARDWARE_CPU_HIST))
            {
                String ramUsage = intent.getStringExtra(DiagnosticService.RESULT_HARDWARE_RAM);
                textViewRAM.setText("RAM: " + ramUsage + "MB");
            }

            // RAM Perc
            if (intent.hasExtra(DiagnosticService.RESULT_HARDWARE_CPU_HIST))
            {
                ramUsageHist = intent.getFloatArrayExtra(DiagnosticService.RESULT_HARDWARE_RAM_HIST);
                if(ramUsageHist!=null){
                    plotterRAM.setData(ramUsageHist);
                    plotterRAM.invalidate();
                    textViewRAMProc.setText(" "+Float.toString(ramUsageHist[0])+" %");
                }
            }
        }
    }
}
# Device-Monitor 

An Android application for displaying the usage of hardware and network data of the device. <br /> <br />

Features:
- displaying data about the actual network connection, the actual download speed and the total amount of downloaded data
- displaying actual usage of CPU and RAM
- data visualizing on time graphs
- saving data in a text file in the device's memory
- the ability to saving time graphs to a file in the device's memory
- the ability to running in the background <br /> <br />

Technical solutions:
- created Intent Service
- created Custom View (time graph display)
- used Fragments with NavController
- used a class from github project AndroidCPU (https://github.com/souch/AndroidCPU) to read CPU usage
- used Android ActivityManager, TelephonyManager and ConnectivityManager
- used Java FileOutputStream to save data in device memory <br /> <br />

Group project - my coworker created network diagnostic functions and save to text file function (in DiagnosticService), I created the rest of the application. <br /> <br />

![Screenshot_1](https://user-images.githubusercontent.com/59321506/75929407-da4c4100-5e70-11ea-8885-f9984ba37089.png)
![Screenshot_2](https://user-images.githubusercontent.com/59321506/75929409-dae4d780-5e70-11ea-8dd4-fa1b6100a85f.png)

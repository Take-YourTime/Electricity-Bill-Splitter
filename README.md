<<<<<<< HEAD
# Electricity Bill Splitter
=======
# 電費分攤助手  Electricity Bill Splitter
>>>>>>> 325b520f881fbd9dcaec40448c6e95e9fc118cd9

![Android](https://img.shields.io/badge/Platform-Android-brightgreen.svg) 
![Kotlin](https://img.shields.io/badge/Language-Kotlin-blue.svg) 
![Compose](https://img.shields.io/badge/UI-Jetpack%20Compose-orange.svg)

This is an Android application designed for tenants living in **shared rental suites** or **shared rental rooms**. It is especially useful for tenants who have individual electricity meters but do not know the total meter reading.

This app fairly splits each resident's electricity bill based on **Taipower bill information** and **residents' individual meter readings**.

---

<<<<<<< HEAD
## Download Instructions
=======
## 下載方式 | How to download?
>>>>>>> 325b520f881fbd9dcaec40448c6e95e9fc118cd9

Click release on the right side of the page to download the latest APK file of the app. Then, find the APK file in your phone's file directory and select "Install this application".

Because this APK is not published through the Google Play Store, the system may mark it as potentially risky software by default. If you are concerned that it may contain a virus, you can scan it with the following online APK scanning websites before installation:

https://metadefender.com/

https://www.virustotal.com/gui/home/upload

**-> Latest Version: 1.3 <-**

---

<<<<<<< HEAD
## User Guide
=======
## 功能特點 | Features
>>>>>>> 325b520f881fbd9dcaec40448c6e95e9fc118cd9

1. **Enter Bill Information** Enter the "current billing amount" and "current electricity usage" from the Taipower bill at the top of the main page.

2. **Manage Resident List** Tap the "+" button in the bottom-right corner to add a resident. You can tap the name field to customize the resident's name.

3. **Enter Meter Readings** Enter the "previous" and "current" meter readings for each resident's electricity meter.

4. **Calculate and Save** After tapping "Calculate and Save", the system calculates each resident's electricity fee, allocates the shared electricity fee, and saves the calculation result to the history list.

5. **View Detailed Process** After the calculation is complete, tap the "View Detailed Calculation Process" button below to see how each resident's electricity fee is calculated.

---

<<<<<<< HEAD
## Calculation Logic
=======
## 使用指南 | User Guide
>>>>>>> 325b520f881fbd9dcaec40448c6e95e9fc118cd9

This program uses the following precise formulas for bill splitting:

<<<<<<< HEAD
1. **Cost per Unit of Electricity**:  
=======
3. **填寫電表讀數 (Enter Meter Readings)** 輸入每位住戶電表的「前期」與「當期」讀數，系統會自動算出此用戶本期用電度數。

4. **計算與存檔 (Calculate & Save)** 點擊「計算並存檔」後，系統會分配公電費並將計算結果儲存至歷史清單中。

5. **查看詳細過程 (Detailed Breakdown)** 計算完成後，點擊下方的「查看詳細計算過程」按鈕，可以知曉每人電費的計算過程。

---

## 計算邏輯 | Calculation Logic

本程式採用以下精確公式進行分攤：

1. **每度單價 (Unit Price)**:  
>>>>>>> 325b520f881fbd9dcaec40448c6e95e9fc118cd9
   $$Price_{unit} = \frac{Total\ Amount}{Total\ Units}$$

2. **Shared Electricity Units**:  
   $$Units_{public} = Total\ Units - \sum Individual\ Usages$$

3. **Final Amount Payable**:  
   $$PersonalCost_{final} = (PersonalUsage \times Price_{unit}) + \frac{Units_{public} \times Price_{unit}}{Residents\ Count}$$

---

<<<<<<< HEAD
## Features
=======
## 開發技術 | Tech Stack
>>>>>>> 325b520f881fbd9dcaec40448c6e95e9fc118cd9

- **Automatic Shared Electricity Calculation**: Automatically estimates shared electricity usage and accurately splits it equally among all residents.

- **History Management**: Provides a dedicated page for managing records, supporting viewing, deleting, and reusing past data at any time.

- **Electricity Usage Analytics Chart**: Uses pie charts to visualize each resident's total electricity usage and electricity fee ratio.

- **Detailed Calculation Process**: Shows the three-step calculation logic, with values rounded to two decimal places for a clear breakdown.

- **Multi-language Support**: Supports instant switching between **Traditional Chinese** and **English** interfaces.

- **Local Persistent Storage**: Uses a Room database to store the resident list and previous meter readings, so data is not lost when switching pages or restarting the app.

- **Offline Application**: Works without an internet connection!

---

## Tech Stack

| Item | Technology / Library |
| ---- | ---- |
| **Programming Language** | Kotlin |
| **UI Framework** | Jetpack Compose (Material 3) |
| **Navigation** | Jetpack Navigation |
| **Database** | Room Persistence Library |
| **Data Parsing** | Gson |
| **Architecture Pattern** | MVVM (ViewModel + State) |
| **Visual Icons** | O3 Minimalist Style (Adaptive Icons) |

---

<<<<<<< HEAD
## UI Preview
=======
## 介面預覽 | UI Preview
>>>>>>> 325b520f881fbd9dcaec40448c6e95e9fc118cd9

Version: 1.2.1

| Main Page | Feature Panel | History Records | 
| ---- | ---- | ---- | 
| <img src="./preview/main%20page.jpg" width="100%"> | <img src="./preview/left%20side%20table.jpg" width="100%"> | <img src="./preview/history%20page.jpg" width="100%"> | 

| Data Analytics | User Guide | Calculation Process |  
| ---- | ---- | ---- | 
| <img src="./preview/analysis%20page.jpg" width="100%"> | <img src="./preview/guild%20line%20page.jpg" width="100%"> | <img src="./preview/calculation%20detail.jpg" width="100%"> | 

---

<<<<<<< HEAD
## Notes
=======
## 備註 (Notes)
>>>>>>> 325b520f881fbd9dcaec40448c6e95e9fc118cd9

- All monetary amounts are rounded to the nearest integer, and electricity units are displayed to two decimal places.
- All data is stored locally on the phone to ensure your privacy.
- Created with assistance from Gemini 3

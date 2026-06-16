# Electricity Bill Splitter

>  [English] | [繁體中文](README_zh-TW.md)

![Android](https://img.shields.io/badge/Platform-Android-brightgreen.svg) 
![Kotlin](https://img.shields.io/badge/Language-Kotlin-blue.svg) 
![Compose](https://img.shields.io/badge/UI-Jetpack%20Compose-orange.svg)

This is an Android application designed for tenants living in **shared rental suites** or **shared rental rooms**. It is especially useful for tenants who have individual electricity meters but do not know the total meter reading.

This app fairly splits each resident's electricity bill based on **Taipower bill information** and **residents' individual meter readings**.

---

## Download Instructions

Click release on the right side of the page to download the latest APK file of the app. Then, find the APK file in your phone's file directory and select "Install this application".

Because this APK is not published through the Google Play Store, the system may mark it as potentially risky software by default. If you are concerned that it may contain a virus, you can scan it with the following online APK scanning websites before installation:

https://metadefender.com/

https://www.virustotal.com/gui/home/upload

**-> Latest Version: 1.3 <-**

---

## User Guide

1. **Enter Bill Information** Enter the "current billing amount" and "current electricity usage" from the Taipower bill at the top of the main page.

2. **Manage Resident List** Tap the "+" button in the bottom-right corner to add a resident. You can tap the name field to customize the resident's name.

3. **Enter Meter Readings** Enter the "previous" and "current" meter readings for each resident's electricity meter.

4. **Calculate and Save** After tapping "Calculate and Save", the system calculates each resident's electricity fee, allocates the shared electricity fee, and saves the calculation result to the history list.

5. **View Detailed Process** After the calculation is complete, tap the "View Detailed Calculation Process" button below to see how each resident's electricity fee is calculated.

---

## Calculation Logic

This program uses the following precise formulas for bill splitting:

1. **Cost per Unit of Electricity**:  
   $$Price_{unit} = \frac{Total\ Amount}{Total\ Units}$$

2. **Shared Electricity Units**:  
   $$Units_{public} = Total\ Units - \sum Individual\ Usages$$

3. **Final Amount Payable**:  
   $$PersonalCost_{final} = (PersonalUsage \times Price_{unit}) + \frac{Units_{public} \times Price_{unit}}{Residents\ Count}$$

---

## Features

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

## UI Preview

Version: 1.2.1

| Main Page | Feature Panel | History Records | 
| ---- | ---- | ---- | 
| <img src="./preview/main%20page.jpg" width="100%"> | <img src="./preview/left%20side%20table.jpg" width="100%"> | <img src="./preview/history%20page.jpg" width="100%"> | 

| Data Analytics | User Guide | Calculation Process |  
| ---- | ---- | ---- | 
| <img src="./preview/analysis%20page.jpg" width="100%"> | <img src="./preview/guild%20line%20page.jpg" width="100%"> | <img src="./preview/calculation%20detail.jpg" width="100%"> | 

---

## Notes

- All monetary amounts are rounded to the nearest integer, and electricity units are displayed to two decimal places.
- All data is stored locally on the phone to ensure your privacy.
- Created with assistance from Gemini 3

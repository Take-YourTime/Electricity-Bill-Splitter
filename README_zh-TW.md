# 電費分攤助手

![Android](https://img.shields.io/badge/Platform-Android-brightgreen.svg) 
![Kotlin](https://img.shields.io/badge/Language-Kotlin-blue.svg) 
![Compose](https://img.shields.io/badge/UI-Jetpack%20Compose-orange.svg)

這是一款專為**分租套房**或**分租雅房**等家庭式租屋族群設計的 Android 應用程式。尤其適用於擁有個人電表，但不清楚總電表度數的租客。

這款軟體能根據**台電帳單資訊**與**住戶個人電表讀數**，公平地分攤每人所需支付的電費。

---

## 下載方式

點選頁面右側的 release，即可下載最新版本 App 的 apk 檔案，而後在手機中檔案目錄中找到該 apk 檔案，選擇「安裝此應用程式」。

因為此 apk 不是 google play 商店發布版，會被系統默認為有風險軟體，若擔心藏有病毒，安裝前可先透過以下線上 apk 掃描網站檢查：

https://metadefender.com/

https://www.virustotal.com/gui/home/upload

**-> 最新版本：1.3 <-**

---

## 使用指南

1. **輸入帳單資訊** 在主頁上方輸入台電帳單上的「本期金額」與「本期用電度數」。

2. **管理住戶名單** 點擊右下角的「+」按鈕新增住戶，可點擊名稱欄位來自訂稱呼。

3. **填寫電表讀數** 輸入每位住戶電表的「前期」與「當期」讀數。

4. **計算與存檔** 點擊「計算並存檔」後，系統會計算每個住戶的電費，並分配公電費，同時將計算結果儲存至歷史清單中。

5. **查看詳細過程** 計算完成後，點擊下方的「查看詳細計算過程」按鈕，可以知曉每人電費的計算過程。

---

## 計算邏輯

本程式採用以下精確公式進行分攤：

1. **每度電的費用**:  
   $$Price_{unit} = \frac{Total\ Amount}{Total\ Units}$$

2. **公電度數**:  
   $$Units_{public} = Total\ Units - \sum Individual\ Usages$$

3. **最終應付金額**:  
   $$PersonalCost_{final} = (PersonalUsage \times Price_{unit}) + \frac{Units_{public} \times Price_{unit}}{Residents\ Count}$$

---

## 功能特點

- **自動計算公電**: 可自動試算公電度數，並精確平均分配給所有住戶。

- **歷史紀錄管理**: 獨立頁面管理，支援隨時查看、刪除或重新代入舊數據。

- **用電分析圖表**: 以圓餅圖視覺化呈現各住戶的總用電度數與電費比例。

- **詳細試算過程**: 展示三步驟試算邏輯，小數點後兩位四捨五入，清清楚楚。

- **多語系支援**: 支援**繁體中文**與**英文**介面即時切換。

- **本地永久儲存**: 使用 Room 資料庫儲存住戶名單、前期度數，換頁或重開 App 都不遺失。

- **離線應用程式**: 不需網路也可運作！

---

## 開發技術

| 項目 | 技術 / 函式庫 |
| ---- | ---- |
| **程式語言** | Kotlin |
| **介面框架** | Jetpack Compose (Material 3) |
| **頁面導覽** | Jetpack Navigation |
| **資料庫** | Room Persistence Library |
| **資料解析** | Gson |
| **架構模式** | MVVM (ViewModel + State) |
| **視覺圖標** | O3 Minimalist Style (Adaptive Icons) |

---

## 介面預覽

版本：1.2.1

| 主頁面 | 功能欄 | 歷史紀錄 | 
| ---- | ---- | ---- | 
| <img src="./preview/main%20page.jpg" width="100%"> | <img src="./preview/left%20side%20table.jpg" width="100%"> | <img src="./preview/history%20page.jpg" width="100%"> | 

| 數據分析 | 使用者指南 | 計算過程 |  
| ---- | ---- | ---- | 
| <img src="./preview/analysis%20page.jpg" width="100%"> | <img src="./preview/guild%20line%20page.jpg" width="100%"> | <img src="./preview/calculation%20detail.jpg" width="100%"> | 

---

## 備註

- 所有金額計算均採四捨五入至整數，度數顯示至小數點後兩位。
- 數據皆儲存於手機本地端，確保您的隱私。
- 使用 Gemini 3 協助製作

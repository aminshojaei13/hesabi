# سند پیشرفت و تحویل کار

> نسخه: ۱٫۰ — آبان ۱۴۰۵
> هدف: ثبت وضعیت دقیق پیاده‌سازی تا هر توسعه‌دهنده یا عامل دیگری بتواند کار را بدون حدس ادامه دهد.

---

## ۱. وضعیت کلی

> **فاز جاری:** فاز ۰ تا ۵ تکمیل شد؛ هستهٔ محلی فاز ۶ نیز پیاده‌سازی شد.
> **فاز بعدی پیشنهادی:** تصمیم کارفرما برای زیرساخت سرور فاز ۶، یا آزمون دستگاه روی اندروید ۸ و ۱۶.

| مورد | وضعیت |
|---|---|
| ساخت پروژه | سبز (`:app:assembleDebug`) |
| تست واحد | ۱۶۹ تست در ۲۷ فایل، بدون خطا |
| تست دستگاه | ۱۷ تست روی شبیه‌ساز اندروید ۱۲ (مهاجرت Room، SQLCipher، Compose، OCR واقعی) |
| فاز ۰ (تثبیت) | کامل روی اندروید ۱۲؛ آزمون دستی ۸ و ۱۶ باز است |
| فاز ۱ (تجربهٔ پایه) | کامل |
| فاز ۲ (داده و امنیت) | کامل؛ رمزنگاری کل فایل پایگاه داده با SQLCipher |
| فاز ۳ (ورود هوشمند) | کامل؛ OCR رسید با مدل داخلی ML Kit |
| فاز ۴ (برنامه‌ریزی) | کامل |
| فاز ۵ (دارایی و گزارش) | کامل؛ قیمت لحظه‌ای TGJU با کش و برچسب منبع |
| فاز ۶ (همگام‌سازی) | هستهٔ محلی آماده؛ حساب کاربری و سرور نیازمند تصمیم و زیرساخت |

**نکته:** تغییرات این مرحله در یک commit روی شاخهٔ جاری ثبت شد. برای کار موازی، شاخهٔ جدا بسازید.

---

## ۲. دستورهای تأیید

```bash
./gradlew :app:testDebugUnitTest --offline
./gradlew :app:assembleDebug --offline
./gradlew :app:connectedDebugAndroidTest      # نیازمند شبیه‌ساز/دستگاه
```

- نتیجهٔ فعلی: هر سه سبز (۱۶۹ تست واحد + ۱۷ تست دستگاه).
- `lintDebug` در این محیط اجرا نمی‌شود: نسخهٔ `lint-gradle` متناظر با AGP 9.4.1 (32.4.1) در مخزن Google Maven موجود نیست. این وظیفه در CI با نسخهٔ سازگار AGP باید اجرا شود.
- گزارش تست واحد: `app/build/reports/tests/testDebugUnitTest/index.html`
- گزارش تست دستگاه: `app/build/reports/androidTests/connected/debug/index.html`

---

## ۳. کارهای انجام‌شدهٔ فاز ۰

| شناسه | موضوع | محل اصلی |
|---|---|---|
| C1 | انتقال پیامکی حساب مقصد را بستانکار می‌کند | `data/repository/SmsRepositoryImpl.kt` |
| C2 | تراکنش دوره‌ای تکرار نمی‌شود و تاریخ سررسید درست است | `core/recurring/RecurringTransactionProcessor.kt` |
| H1 | رمز با PBKDF2 و نمک تصادفی | `core/security/SecurityManager.kt` |
| H2 | قفل ۶۰ ثانیه‌ای پس از ۵ تلاش | `core/security/PinAttemptPolicy.kt` |
| H4 | قفل دوباره پس از ۳۰ ثانیه پس‌زمینه | `ui/navigation/HesabiNavHost.kt` |
| H5 | تأیید رمز فعلی برای تغییر/خاموش‌کردن قفل | `ui/security/CurrentPinDialog.kt` |
| H6 | درخواست مجوز اعلان اندروید ۱۳+ | `ui/settings/SettingsScreen.kt` |
| H7 | واریز حقوق با روز ۳۱ و جبران ماه‌های جاافتاده | `di/AppContainer.kt` |
| H9 | شروع تراکنش دوره‌ای از روز انتخابی | `domain/logic/RecurringFrequencyCalculator.kt` |
| H10 | انتخاب صریح حساب مقصد دوره‌ای | `ui/recurring/AddEditRecurringDialog.kt` |
| M2 | نگه‌داشتن روز لنگر ماهانه | `domain/logic/RecurringFrequencyCalculator.kt` |
| M14 | دستهٔ درست در ثبت گروهی پیامک | `ui/sms/SmsViewModel.kt` |

**آمادگی انتشار:**

- `docs/06-privacy-policy.md` — پیش‌نویس سیاست حریم خصوصی.
- `docs/07-release-checklist.md` — چک‌لیست انتشار و انطباق پیامک.
- `.github/workflows/android.yml` — تست، lint و assemble.
- `core/diagnostics/CrashLogger.kt` — ثبت محلی خطا بدون ارسال داده.

---

## ۴. کارهای انجام‌شدهٔ فازهای ۱ تا ۵

### فاز ۱ — تجربهٔ پایه

| کار | وضعیت | محل |
|---|---|---|
| خانهٔ جدید با عدد امروز و خرج امن | انجام شد | `ui/home/HomeScreen.kt` |
| ثبت دو-لمسی | انجام شد | `ui/entry/QuickEntryScreen.kt` |
| ماشین‌حساب مبلغ | انجام شد | `core/money/AmountExpressionEvaluator.kt` |
| الگوهای پرتکرار | انجام شد | `data/repository/QuickTemplateRepositoryImpl.kt` |
| ناوبری چهار تب + افزودن | انجام شد | `ui/navigation/HesabiNavHost.kt` |
| حالت خالی و خطا | انجام شد | `ui/components/Primitives.kt` |
| متن فارسی و دسترس‌پذیری پایه | انجام شد | برچسب‌های `contentDescription` و `semantics` |
| تأیید دوبارهٔ تراکنش مشابه | انجام شد | `ui/entry/QuickEntryViewModel.kt` |
| حفظ حساب مقصد در الگوی انتقال | انجام شد | `ui/entry/QuickEntryViewModel.kt` |

### فاز ۲ — داده و امنیت

- رمزنگاری ستون‌های حساس با Keystore: `core/security/FieldCrypto.kt`.
- رمزنگاری رسیدها: `core/security/FileCrypto.kt` و `core/receipt/ReceiptStorageManager.kt`.
- پشتیبان کامل شامل رسیدها: `data/backup/BackupManager.kt`.
- ورود CSV/Excel: `data/importer/` و `ui/importer/`.
- سطل بازیافت ۳۰ روزه: `ui/trash/TrashScreen.kt` و `AppContainer.purgeExpiredTrash`.
- صفحهٔ «دادهٔ من»: `ui/privacy/MyDataScreen.kt`.
- `FLAG_SECURE`: `ui/security/SecureScreen.kt`.
- پنهان‌کردن مبالغ: `ui/components/MoneyComponents.kt`.
- **رمزنگاری کل فایل پایگاه داده (H3):** `core/security/DatabaseKeyManager.kt` و
  `core/security/EncryptedDatabaseMigrator.kt`؛ مهاجرت خودکار از SQLite ساده به
  SQLCipher با `sqlcipher_export` و بازگردانی امن در صورت خطا.

### فاز ۳ — ورود هوشمند

- دستیار متنی فارسی: `core/assistant/` و `ui/assistant/AssistantScreen.kt`.
- پرسش‌های مالی: همان مسیر دستیار.
- بهبود پیامک و پوشاندن متن حساس: `data/sms/SmsTextMasker.kt` و `SmsNotifier.kt`.
- تشخیص تراکنش تکراری: `domain/logic/BudgetAndDuplicates.kt`.
- دستهٔ هوشمند: `domain/logic/SmartCategorySuggester.kt`.
- ورود صوتی: `ui/voice/VoiceEntrySheet.kt`.
- **OCR رسید (۳٫۴):** `core/ocr/ReceiptOcrScanner.kt` (ML Kit Bundled، تمام‌محلی)،
  `domain/logic/ReceiptOcrParser.kt` (پارس مبلغ/فروشنده/تاریخ فارسی) و
  `ui/receipt/ReceiptScanDialog.kt` (تأیید اجباری کاربر پیش از ثبت).

### فاز ۴ — برنامه‌ریزی و تعهدات

- چرخهٔ حقوق: `domain/logic/SalarySchedule.kt`.
- پیش‌بینی پایان ماه: `domain/logic/MonthForecast.kt` و `ui/components/ForecastCard.kt`.
- بودجهٔ پاکتی: `domain/logic/EnvelopeBudgetCalculator.kt`.
- اهداف پس‌انداز و شبیه‌ساز خرید: `ui/components/SavingsGoalDialogs.kt` و `ui/simulator/`.
- تقویم سررسید: `ui/calendar/DueCalendarScreen.kt`.
- بهبود چک، وام و اشخاص: `ui/debts/` و `ui/accounts/`.
- تراکنش دوره‌ای مطمئن: `core/recurring/`.

### فاز ۵ — دارایی و گزارش

- دارایی و سود/زیان: `domain/logic/AssetPortfolio.kt` و `ui/assets/AssetsScreen.kt`.
- PDF فارسی: `core/export/PdfReportWriter.kt`.
- Excel چندبرگی: `core/export/XlsxWriter.kt`.
- گزارش سالانه: `domain/logic/AnnualReport.kt` و `ui/annual/AnnualReportScreen.kt`.
- مغایرت بانکی: `ui/accounts/ReconciliationDialog.kt`.
- تست جدید دارایی: `app/src/test/.../AssetPortfolioTest.kt`.
- **قیمت لحظه‌ای (۵٫۲):** `core/price/TgjuPriceClient.kt` (TGJU بدون کلید)،
  `domain/logic/AssetPriceCatalog.kt` (نگاشت محافظه‌کارانهٔ دارایی به نماد)،
  `core/price/AssetPriceSyncManager.kt` و `core/price/PriceCacheStore.kt`
  (کش آفلاین + برچسب «TGJU · تاریخ/ساعت»).

### زیرساخت تست و فاز ۶

- **مهاجرت Room:** `data/local/HesabiMigrations.kt` از `AppContainer` جدا شد و
  `HesabiMigrationTest` نسخه‌های ۱→۶ و هر گام را با اسکیمای صادرشده اعتبارسنجی می‌کند.
- **تست دستگاه:** `DatabaseEncryptionTest` (SQLCipher و رمز اشتباه)،
  `ReceiptScanDialogTest` (Compose) و `ReceiptOcrScannerTest` (ML Kit واقعی).
- **فاز ۶ (هستهٔ محلی):** `core/sync/SyncPackage.kt`، `SyncPackageCodec.kt`
  (AES-256-GCM + PBKDF2) و `SyncConflictResolver.kt` (LWW + سنگ‌قبر + تساوی قطعی).
  طراحی کامل و کارهای سرور در `docs/09-sync-design.md`.

---

## ۵. کارهای باقی‌مانده (به‌ترتیب اولویت)

| # | کار | چرا مانده | قدم بعدی |
|---|---|---|---|
| ۱ | آزمون دستی روی اندروید ۸ و ۱۶ | فقط شبیه‌ساز اندروید ۱۲ در دسترس بود | همان چک‌لیست `07-release-checklist.md` را روی دو API دیگر اجرا و امضا کن |
| ۲ | ایمیل پشتیبانی واقعی | تصمیم کارفرما | جای‌نگهدار `[ایمیل پشتیبانی]` را در `06-privacy-policy.md` و صفحهٔ فروشگاه پر کن |
| ۳ | دقت OCR روی ۵۰ رسید واقعی | نیاز به مجموعهٔ تصویر واقعی | معیار پذیرش فاز ۳: دقت مبلغ بالای ۸۵٪؛ پارسر واحد و تست تصویر ساختگی سبز است |
| ۴ | پایداری و مجوز منبع TGJU | منبع عمومی و بدون قرارداد است | پیش از انتشار، شرایط استفادهٔ TGJU و جایگزین دوم بررسی شود |
| ۵ | سرور و حساب کاربری فاز ۶ | نیاز به زیرساخت و تصمیم حقوقی | طبق `09-sync-design.md` پیش برو |
| ۶ | حالت خانواده (۶٫۴) | وابسته به سرور | پس از راه‌اندازی همگام‌سازی پایه |
| ۷ | کلید امضای انتشار | تصمیم کارفرما | کلید و رمز در جای امن خارج از مخزن |

---

## ۶. نکات فنی مهم برای ادامه‌دهنده

1. **مسیر پول را بدون تست تغییر نده.** برای هر تغییر در تراکنش، انتقال، دوره‌ای یا حقوق، تست واحد اضافه کن.
2. **تراکنش‌های خودکار `externalRef` یکتا دارند:**
   - دوره‌ای: `recurring:<id>:<occurrenceEpochDay>`
   - حقوق: `salary:<year-month>`
3. **پیامک انتقال بدون حساب مقصد معتبر ثبت نمی‌شود.** این محافظ عمدی است.
4. **روز لنگر ماهانه** در `dayOfMonth` ذخیره می‌شود؛ برای ماه کوتاه فقط نمایش محدود می‌شود و لنگر عوض نمی‌شود.
5. **رمز قدیمی SHA-256** هنگام ورود یا خاموش‌کردن قفل به PBKDF2 ارتقا می‌یابد.
6. **نسخهٔ پایگاه داده ۶ است** و مهاجرت‌های ۱ تا ۶ در `AppContainer.kt` تعریف شده‌اند.
7. **همهٔ متن‌های رابط فارسی و راست‌به‌چپ هستند.** متن انگلیسی یا اصطلاح فنی اضافه نکن.
8. **داده‌های حساس در لاگ نوشته نمی‌شوند.** `CrashLogger` فقط نوع خطا و ۱۶۰ کاراکتر اول پیام را ذخیره می‌کند.
9. **کلید پایگاه داده را دور نزن.** `AppContainer` باید همیشه `SupportOpenHelperFactory` با
   `DatabaseKeyManager.passphrase(appContext)` بسازد. هیچ تست یا کدی نباید `hesabi.db` را بدون کلید باز کند.
10. **مهاجرت پایگاه داده فقط یک‌بار و فقط روی فایل plaintext اجرا می‌شود**
    (`EncryptedDatabaseMigrator.prepare`). اگر وریفای شکست بخورد، فایل اصلی بازگردانده می‌شود.
11. **قیمت لحظه‌ای هرگز اجباری نیست.** اگر شبکه یا منبع در دسترس نباشد، آخرین کش با برچسب
    «TGJU (کش)» اعمال می‌شود و خطای روشن به کاربر نشان داده می‌شود.
12. **OCR هیچ‌وقت خودکار ثبت نمی‌کند.** نتیجه فقط پیشنهاد است و در `ReceiptScanDialog`
    تا تأیید صریح کاربر ذخیره نمی‌شود.
13. **همگام‌سازی بدون سرور فعال نیست.** بسته‌ها رمزنگاری‌شده‌اند؛ عبارت همگام را نباید در لاگ یا
    پشتیبان ذخیره کرد.
14. **مهاجرت رمزنگاری در `Application.onCreate` همگام اجرا می‌شود** و فقط یک‌بار برای هر نصب
    رخ می‌دهد. برای پایگاه دادهٔ بزرگ‌تر از ۵۰ مگابایت، پیش از انتشار باید به صفحهٔ راه‌اندازی
    با نوار پیشرفت منتقل شود.

---

## ۷. فایل‌های کلیدی

| حوزه | فایل |
|---|---|
| تزریق وابستگی | `di/AppContainer.kt` |
| مدل‌ها | `domain/model/Models.kt` |
| مخازن | `domain/repository/Repositories.kt` |
| ناوبری | `ui/navigation/HesabiNavHost.kt` |
| ثبت سریع | `ui/entry/QuickEntryViewModel.kt` |
| امنیت | `core/security/` |
| پشتیبان | `data/backup/BackupManager.kt` |
| رمزنگاری پایگاه داده | `core/security/DatabaseKeyManager.kt`، `EncryptedDatabaseMigrator.kt` |
| OCR | `core/ocr/ReceiptOcrScanner.kt`، `domain/logic/ReceiptOcrParser.kt` |
| قیمت لحظه‌ای | `core/price/`، `domain/logic/AssetPriceCatalog.kt` |
| همگام‌سازی (هسته) | `core/sync/` |
| مهاجرت‌ها | `data/local/HesabiMigrations.kt` |
| تست واحد | `app/src/test/java/com/braveboy/hesabi/` |
| تست دستگاه | `app/src/androidTest/java/com/braveboy/hesabi/` |

---

## ۸. تعریف «تمام‌شدن» برای ادامه

یک کار وقتی تمام است که:

- [ ] کد کامپایل می‌شود.
- [ ] تست واحد مربوطه اضافه یا به‌روز شده است.
- [ ] `./gradlew :app:testDebugUnitTest --offline` سبز است.
- [ ] `./gradlew :app:connectedDebugAndroidTest` روی شبیه‌ساز یا دستگاه سبز است.
- [ ] متن فارسی و نیم‌فاصله رعایت شده است.
- [ ] مسیر پول با سناریوی واقعی بررسی شده است.
- [ ] این سند و `docs/05-roadmap.md` به‌روز شده‌اند.

# VibControl — ESP32-C3 Vibration Motor App

Control your ESP32-C3 vibration motor patterns over Bluetooth.

---

## How to get the APK (step-by-step, no coding needed)

### Step 1 — Create a free GitHub account
Go to https://github.com and sign up (it's free).

### Step 2 — Create a new repository
1. After logging in, click the **+** button (top right) → **New repository**
2. Name it: `VibControl`
3. Set it to **Public**
4. Click **Create repository**

### Step 3 — Upload the project files
1. On your new repository page, click **uploading an existing file**
2. Drag and drop ALL the files and folders from this zip into the upload area
   - Make sure to upload folders too (app/, .github/, gradle/)
3. Scroll down, click **Commit changes**

### Step 4 — Watch it build
1. Click the **Actions** tab at the top of your repository
2. You'll see a workflow called **Build APK** running (yellow circle = in progress)
3. Wait about 3–5 minutes for it to finish (turns green ✓)

### Step 5 — Download your APK
1. Click on the completed **Build APK** run
2. Scroll to the bottom — you'll see **Artifacts**
3. Click **VibControl-debug** to download the APK zip
4. Unzip it — inside is `app-debug.apk`

### Step 6 — Install on your Android phone
1. On your phone, go to **Settings → Security** (or Privacy)
2. Enable **Install unknown apps** (or "Allow from this source")
3. Transfer the APK to your phone (USB, WhatsApp to yourself, Google Drive, etc.)
4. Tap the APK file on your phone → **Install**

---

## Using the app

1. Power on your ESP32-C3 (it should be running the VibMotor firmware)
2. Open VibControl on your phone
3. Tap **Connect** → grant Bluetooth permission → select **VibMotor-C3**
4. Choose a preset pattern and tap **Send pattern**
5. Build custom sequences with the step builder

## Patterns

| Pattern     | Description              |
|-------------|--------------------------|
| Single pulse | One short 100ms buzz    |
| Double tap  | Two quick buzzes          |
| SOS         | · · · — — — · · ·        |
| Heartbeat   | Lub-dub rhythm            |
| Escalate    | Intensity ramps up        |
| Continuous  | 1-second constant buzz    |

## Custom sequence
Tap the circles to toggle on/off steps, then hit **Send**.
Each step = 60ms. Max 16 steps.

---

## Wiring reminder (ESP32-C3 side)

```
GPIO2 → NPN transistor Base (e.g. 2N2222)
Collector → Motor (-)
Motor (+) → 3.3V
Emitter  → GND
Flyback diode (1N4148) across motor terminals
```

Requires Android 8.0+ and Bluetooth LE support.

# POS Terminal — Spring Boot 3 / Java 17 / SQLite

Lightweight, offline-capable point-of-sale server for legacy terminal
hardware (FEC MP-3435 class, Windows 7, 2–4 GB RAM). Spring MVC +
Thymeleaf + Bootstrap 5 + vanilla JS. No cloud, no external database.

---

## 1. Prerequisites (one-time install on the terminal)

| Software | Version | Check with |
|---|---|---|
| Java (JRE or JDK) | **17 or newer** | `java -version` |
| Maven | 3.8+ *(only needed to build)* | `mvn -version` |

> If `java -version` shows anything below 17, install a Java 17 runtime
> (e.g. [Eclipse Temurin 17](https://adoptium.net/temurin/releases/?version=17))
> and make sure `JAVA_HOME` points to it.

## 2. Build the application

```bash
cd POS_Lightweight
mvn clean package
```

Result: **`target/pos-terminal-1.0.0.jar`** — a self-contained executable
JAR (Tomcat, Hibernate and the SQLite driver are all inside).

## 3. Run it

```bash
java -jar target/pos-terminal-1.0.0.jar
```

Then open the terminal's browser (or any browser on that machine) at:

```
http://localhost:8080
```

First launch creates the SQLite database file at `data/pos.db` next to
where you ran the command (the folder is created automatically).

### Useful startup options

```bash
# Port already in use? Pick another:
java -jar target/pos-terminal-1.0.0.jar --server.port=9090

# Demo mode: no receipt printing, no cash drawer pulses
java -jar target/pos-terminal-1.0.0.jar --app.hardware.enabled=false

# Give the JVM a small heap (recommended on 2 GB RAM machines)
java -Xms128m -Xmx512m -jar target/pos-terminal-1.0.0.jar
```

## 4. Using the app

| URL | What it does |
|---|---|
| `http://localhost:8080/pos` | **Sale screen** — dashboard, barcode scanning, inventory search, cart, checkout |
| `http://localhost:8080/inventory` | **Products** — add/edit/deactivate, stock adjust |
| `http://localhost:8080/reports` | **Daily sales** — totals, cash/card split, transactions |
| `http://localhost:8080/reports/order/{id}` | Single receipt detail |

Typical workflow:

1. **Inventory** → add a product (barcode, name, category, price, stock). With the
   cursor in the *Barcode* field, just scan the product's label.
2. **Sale** → scan a barcode (works anywhere on the screen, no need to click a field
   first) or search the inventory by name / barcode / category, or tap a product tile.
3. **Charge** (`F9`) → choose Cash (quick-cash buttons, change is calculated live) or
   Card (last 4 digits), then **Finalize bill** (or press Enter).
4. The order is saved, **stock is deducted**, the **cash drawer is unlocked**, and the
   receipt prints (if hardware is enabled).
5. **Reports** → pick a date to see that day's totals and every transaction.

### Barcode scanner

Any USB / Bluetooth scanner in **keyboard mode** (HID — the factory default for
almost all models) works with no driver or setup. The sale screen detects the fast
keystroke burst + Enter, looks the code up in the inventory, adds the item and beeps.
Unknown codes show a red "Not found" alert. Scans are ignored while the payment
window is open so items can't be added mid-payment. Serial/COM-port scanners are not
supported.

Shortcuts on the sale screen: `F2` focus search · `F9` charge · `Enter` finalize.
The **Drawer** button in the top bar opens the drawer without a sale.

### Cash drawer (FEC MP-3435 — drawer plugged into the terminal's RJ11 port)

On the MP-3435 the drawer is **not** opened through the receipt printer. The terminal
has its own 12 V RJ11 "Cash Drawer" port, switched by a motherboard I/O port. Windows 7
blocks programs from writing to I/O ports, so the app uses the free **InpOut** driver.

**One-time setup**

1. Download *InpOutBinaries* from <https://www.highrez.co.uk/downloads/inpout32/> and copy
   `inpoutx64.dll` (64-bit Java) or `inpout32.dll` (32-bit Java) into the `lib/` folder next
   to the jar. Check with `java -version` — it says "64-Bit" for 64-bit Java.
2. Plug the drawer into the RJ11 port marked *Cash Drawer* on the back of the terminal.
3. Start the app **as Administrator** (right-click the shortcut → *Run as administrator*;
   or Properties → Shortcut → Advanced → *Run as administrator*). The driver installs on
   the first run.
4. Log in as **admin** and open `http://localhost:8080/api/drawer/diagnose` — it must show
   `"dllLoaded": true` and `"driverActive": true`.
5. Find your port: open `http://localhost:8080/api/drawer/test?port=284`. If the drawer
   stays shut, try `http://localhost:8080/api/drawer/test?port=280`. Put the one that works
   in `application.properties` as `app.hardware.drawer.io-port` (hex, e.g. `280`).

FEC documents I/O port `0x284` for the AerPOS AP-3435 and `0x280` for the MP-2258; the
MP-3435 manual wasn't available, so confirm on your machine with step 5.

The screen now shows the real result: **"Drawer did NOT open: …"** with the reason (DLL
missing, driver not active, …) instead of always saying "unlocked". "Open signal sent"
means the port was written; it cannot detect whether the lock physically released.

Other wirings: `app.hardware.drawer.mode=PRINTER` (drawer on the receipt printer's kick
connector) or `COMMAND` (run FEC's own cash-drawer utility via
`app.hardware.drawer.command`). Use `drawer.number=2` for the second drawer.


## 5. Auto-start on the POS terminal (optional)

Save as `start-pos.bat` next to the JAR and put a shortcut to it in the
Windows **Startup** folder (`shell:startup`):

```bat
@echo off
cd /d C:\pos
java -Xms128m -Xmx512m -jar pos-terminal-1.0.0.jar
```

Notes:
- `cd /d C:\pos` matters — the SQLite file is created in the *working
  directory*, so always start from the same folder.
- To run it hidden/minimised, start the shortcut with
  `javaw -jar ...` instead of `java -jar ...`.

## 6. Where the data lives

Everything is in one file: **`data/pos.db`** (relative to the run folder).

- **Back up** = copy that file (while the app is stopped).
- **Wipe / fresh start** = stop the app and delete the `data` folder.
- **Move to another machine** = copy the JAR + the `data` folder.

## 7. Troubleshooting

| Symptom | Fix |
|---|---|
| `Port 8080 was already in use` | Another app owns the port. Start with `--server.port=9090`, or find the process: `netstat -ano | findstr :8080` then `taskkill /PID <pid> /F` |
| Page opens but is another app / asks for login | Same cause as above — something else is on 8080 |
| `Cannot load driver class: org.sqlite.JDBC` | You are running an old JAR. Rebuild: `mvn clean package` |
| Browser can't connect | App binds to `127.0.0.1` only (by design, offline). Use the terminal itself, not another PC |
| No receipt prints | Check the printer is the **Windows default** and prints a test page; run with `--app.hardware.enabled=false` to confirm sales still work, then check logs for `No receipt printer found` |
| Drawer says "did NOT open" | Read the reason on screen; check `/api/drawer/diagnose` as admin: DLL in `lib/` matching your Java bits, app started as Administrator, then try `/api/drawer/test?port=280` |
| Hardware errors never block a sale | This is intentional — checkout completes even if the printer is offline; check the log for the reason |
| DB locked errors | Only one app instance may open the DB. Close duplicate `java` processes |

## 8. Configuration reference

All settings are in `src/main/resources/application.properties`
(override any of them at runtime with `--key=value`). Highlights:

| Property | Default | Meaning |
|---|---|---|
| `server.port` | 8080 | HTTP port |
| `server.address` | 127.0.0.1 | Local-only binding (offline security) |
| `app.hardware.enabled` | true | Receipt printing + cash drawer |
| `app.hardware.drawer.mode` | IOPORT | `IOPORT` (RJ11 port on the terminal), `PRINTER`, or `COMMAND` |
| `app.hardware.drawer.io-port` | 284 | Hex I/O port of the drawer (try `280` if 284 doesn't open it) |
| `app.hardware.drawer-open-on` | ALL | `ALL` = unlock the drawer on every finalized bill, `CASH` = cash sales only |
| `spring.datasource.url` | `jdbc:sqlite:data/pos.db` | Database file location |
| `server.tomcat.threads.max` | 4 | Kept tiny for low-RAM terminals |

## 9. Developer notes

```bash
mvn spring-boot:run     # dev loop, no jar step
mvn clean package       # production jar
```

Project layout (Spring MVC):

```
src/main/java/com/pos/
├── PosTerminalApplication.java   # entry point (+ ensures data/ exists)
├── controller/                   # C — PosController, CheckoutController,
│                                 #     InventoryController, ReportController
├── model/                        # M — Product, Order, OrderItem, ItemDto, ...
├── repository/                   # Spring Data JPA (SQLite)
└── service/                      # HardwareService (ESC/POS + drawer)
src/main/resources/
├── application.properties
└── templates/                    # V — Thymeleaf (pos, inventory, reports, ...)
```


## Finalization notes (legacy 800x600 terminal)
- Local persistence uses SQLite at `data/pos.db` (`spring.datasource.url=jdbc:sqlite:data/pos.db`). Back up this file before upgrades.
- UI currency is displayed in Sri Lankan Rupees (Rs). Confirm the store's tax rate and pricing rules before live use.
- Login role selector chooses the matching username; authentication still requires the configured password. Demo users: `admin` / `admin123`, `cashier` / `cashier123`. Change these credentials before production use.
- Every newly saved product must have a unique barcode. USB barcode scanners that emulate a keyboard can be used in the barcode/search fields.
- This build has not been Maven-compiled in this environment because Maven is unavailable here. Run `mvn clean package` and test a backup copy of the database before deploying to the terminal.

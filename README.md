# Community Voice

An app for raising local community issues and letting neighbours back them or push back — roads, water, schools and anything else.

- **`android/`** — the phone app (Kotlin + Jetpack Compose)
- **`backend/`** — the REST API (Kotlin + Spring Boot), which is the only thing that talks to MongoDB
- **`tools/`** — a one-time setup script

The phone **never** connects to MongoDB directly. It only ever calls the backend over HTTP, and the backend holds the database credentials.

```
Phone app  ──HTTP──>  Spring Boot backend  ──>  MongoDB Atlas
```

---

## What the app does

### Accounts

1. **Register** with a name, email and password. Passwords are hashed with BCrypt and never stored or logged in plain text.
2. **Sign in** and stay signed in. The backend hands out a signed token (a JWT) which the app stores and sends on every later request. It lasts 30 days by default, after which the app signs you out quietly rather than showing an error.
3. **One person, one vote** — per account, not per phone. Logging in on a second device does not get you a second vote, and nobody can vote on your behalf by guessing an ID.
4. **Browse before joining.** The feed, the search, the counts and the comments all work with no account. Only voting, posting and commenting need one, and the app asks for an account at the moment you try.

### Issues

5. **Raise an issue** with a title, description, category and optional location. Eight categories — Roads, Water, Electricity, Sanitation, Health, Schools, Safety, Other — each with its own colour and icon. A **live preview** shows the real feed card with your text in it before you post.
6. **Every issue shows who raised it** — name, initials avatar, and an "Official" badge where it applies. Tap the name to open that member's profile and see everything else they have raised.
7. **Support or decline.** Both counts are shown side by side, with a split bar so you can see at a glance how one-sided the feeling is. Tap your own choice again to withdraw it; tap the other side to move your single vote across.
8. **Comment** on an issue. Threads read oldest-first, like a conversation.
9. **Delete your own** issues and comments. Admins can delete anybody's.

### Following through

10. **Five lifecycle stages** — Open, Under review, In progress, Resolved, Not planned. Only a verified **official** can move an issue along, and every move is appended to the issue's history with the official's name, the date and a note. The detail screen draws that history as a timeline, so an issue has a story rather than just a state.
11. **Share** an issue to any app on the phone.

### Finding things

12. **Search** across titles, descriptions, locations and author names, debounced as you type.
13. **Filter** by category, by stage, and by "raised by me" / "I voted on".
14. **Five orderings** — Trending (recent feeling, decayed by age), Most supported, Newest, Most discussed, and **Most debated** (where support and declines are both high and closely matched).
15. **Endless scrolling**, paged 20 at a time, and pull-to-refresh.

### Insights

16. An **Insights** screen with the community's headline numbers: issues raised, members, votes, comments, a resolution-rate meter, a breakdown by category, where issues currently stand, and the most active members. Tapping a category bar filters the feed to it.

### Throughout

17. Loading, empty and error states on every screen, with a retry that works.
18. A full **dark theme**, with its own colour steps rather than an automatic flip.
19. Relative timestamps ("14m ago", "3d ago"), initials avatars in a stable per-person colour, and compact counts (1.2k).

---

## Before you start

You need four things. Work through them in order.

### 1. A JDK (Java 17 or newer)

Both projects are built with Gradle, which needs a JDK.

Check what you have — open PowerShell and run:

```powershell
java -version
```

If you see `17`, `21` or higher, you are fine. If the command is not found, install **Eclipse Temurin JDK 17** (or 21) from <https://adoptium.net/>. During setup, tick the option to **set `JAVA_HOME`**. Close and reopen PowerShell afterwards, then check again.

### 2. The Android SDK

The easiest way to get the SDK *and* an emulator is to install **Android Studio** once from <https://developer.android.com/studio>. You do not have to write code in it — we only want the SDK it downloads and its emulator.

On first launch, let its setup wizard finish. Then open **More Actions → SDK Manager** and make sure **Android 14 (API 34)** is ticked, because this project builds against API 34.

Note where the SDK landed. On Windows it is almost always:

```
C:\Users\<your-name>\AppData\Local\Android\Sdk
```

### 3. VS Code extensions

Open this folder in VS Code (`File → Open Folder…`, pick the `community voice` folder), then install:

- **Extension Pack for Java** (Microsoft) — Kotlin files are not Java, but this brings the JDK tooling Gradle needs.
- **Kotlin** (fwcd) — syntax highlighting and basic completion for `.kt` files.
- **Gradle for Java** (Microsoft) — optional, lets you run Gradle tasks by clicking.

VS Code will not give you the full red-squiggle experience that Android Studio does for Compose. That is normal — the real check is whether Gradle builds, which you run from the terminal below.

### 4. A free MongoDB Atlas database

1. Sign up at <https://www.mongodb.com/cloud/atlas/register>.
2. Create a free **M0** cluster (any cloud, any region near you).
3. **Database Access** → *Add New Database User*. Pick a username and password. **Write the password down** — you cannot see it again.
4. **Network Access** → *Add IP Address* → *Add Current IP Address*. (While learning you may use `0.0.0.0/0`, "allow from anywhere". That is fine for a throwaway practice cluster and a bad idea for anything real.)
5. **Database → Connect → Drivers** and copy the connection string. It looks like:

   ```
   mongodb+srv://myuser:<db_password>@cluster0.ab1cd.mongodb.net/?retryWrites=true&w=majority
   ```

   Replace `<db_password>` with the password from step 3. If your password has characters like `@`, `:`, `/` or `#`, they must be percent-encoded (`@` → `%40`, `#` → `%23`). The simplest fix is to use a password with only letters and numbers.

---

## Step 1 — Finish the Gradle wrapper (run once)

The Gradle wrapper lets you build without installing Gradle. One piece of it is a binary file that is not committed to this repo, so fetch it now. From the project root in PowerShell:

```powershell
powershell -ExecutionPolicy Bypass -File tools\get-gradle-wrapper.ps1
```

This writes `gradle-wrapper.properties` and downloads `gradle-wrapper.jar` into both `backend/` and `android/`. You only ever do this once.

> On macOS or Linux use `sh tools/get-gradle-wrapper.sh` instead.

---

## Step 2 — Give the backend its secrets

The backend reads two things from the environment. In PowerShell, from the project root:

```powershell
$env:MONGODB_URI = "mongodb+srv://myuser:mypassword@cluster0.ab1cd.mongodb.net/?retryWrites=true&w=majority"
$env:JWT_SECRET  = "any-long-random-string-you-keep-private"
```

**`MONGODB_URI`** is required — the app refuses to start without it. Use **your** string from step 4 above, with the real password substituted in.

**`JWT_SECRET`** is the key used to sign login tokens. It is optional while you are learning: leave it unset and the server invents a random one at startup and logs a warning. The cost of that is that **every restart signs everybody out**, because tokens signed with the old key no longer verify. Set it as soon as that gets annoying. Any length of string works — the server hashes whatever you give it to the right size.

These last only for **that one terminal window**. Open a new terminal and you must set them again. If you get tired of that, set them permanently (then restart VS Code):

```powershell
setx MONGODB_URI "mongodb+srv://myuser:mypassword@cluster0.ab1cd.mongodb.net/?retryWrites=true&w=majority"
setx JWT_SECRET "any-long-random-string-you-keep-private"
```

Two more optional settings:

```powershell
$env:MONGODB_DATABASE    = "communityvoice"   # defaults to communityvoice
$env:TOKEN_VALIDITY_DAYS = "30"               # how long a login lasts
```

---

## Step 3 — Start the backend

```powershell
cd backend
.\gradlew.bat bootRun
```

The first run downloads Gradle and the dependencies, so it may take a few minutes. You are looking for a line like:

```
Started CommunityVoiceApplicationKt in 3.412 seconds
```

Now check it in a browser: <http://localhost:8080/api/health> should show

```json
{ "status": "ok", "service": "community-voice-backend", "time": "2026-10-11T09:15:00Z" }
```

and <http://localhost:8080/api/issues> should show an empty page:

```json
{ "items": [], "page": 0, "size": 20, "totalItems": 0, "totalPages": 0, "hasMore": false }
```

Leave this terminal running. Press `Ctrl+C` to stop the server when you are done.

> **If it fails with `Could not resolve placeholder 'MONGODB_URI'`** — the environment variable is not set in *this* terminal. Go back to step 2.
>
> **If it fails with a timeout talking to Atlas** — your current IP is not on the Atlas access list. Redo *Network Access* in step 4 of the prerequisites.

---

## Step 4 — Tell Gradle where the Android SDK is

Create a file called `local.properties` inside the `android/` folder with one line pointing at your SDK. Note the **doubled backslashes**:

```properties
sdk.dir=C:\\Users\\ISSAH\\AppData\\Local\\Android\\Sdk
```

Replace `ISSAH` with your own Windows user name if it differs. This file is machine-specific and is deliberately listed in `.gitignore` — never commit it.

---

## Step 5 — Point the app at the backend

Open [android/gradle.properties](android/gradle.properties). The last line is the only setting you are likely to change:

```properties
API_BASE_URL=http://10.0.2.2:8080/
```

Which value you need depends on where the app runs:

| Running on | Use | Why |
| --- | --- | --- |
| **Android emulator** | `http://10.0.2.2:8080/` | `10.0.2.2` is a special address meaning "the computer hosting this emulator". `localhost` would mean the emulator itself. |
| **A real phone on the same Wi-Fi** | `http://<your-pc-ip>:8080/` | The phone needs your computer's actual address on the network. |

It must **end with a slash**.

### Finding your computer's Wi-Fi address

```powershell
ipconfig
```

Look under your Wi-Fi adapter for **IPv4 Address** — something like `192.168.1.42`. Then set:

```properties
API_BASE_URL=http://192.168.1.42:8080/
```

For a real phone, also make sure:

- The phone and the computer are on the **same Wi-Fi network** (and the network is not "guest"/client-isolated).
- Windows Firewall is letting port 8080 through. The first time you run the backend, Windows may pop up a prompt — tick **Private networks** and allow it. To add the rule by hand, in an **Administrator** PowerShell:

  ```powershell
  New-NetFirewallRule -DisplayName "Community Voice backend" -Direction Inbound -LocalPort 8080 -Protocol TCP -Action Allow -Profile Private
  ```

- Check it from the phone's browser first: open `http://192.168.1.42:8080/api/health`. If that shows `{"status":"ok",...}`, the app will work too.

### Why plain `http://` is allowed

Android blocks unencrypted HTTP by default. This project re-enables it **only for debug builds**, via [android/app/src/debug/AndroidManifest.xml](android/app/src/debug/AndroidManifest.xml) and [android/app/src/debug/res/xml/network_security_config.xml](android/app/src/debug/res/xml/network_security_config.xml). Both live in `src/debug/`, so a release build cannot use plain HTTP — it would need an `https://` URL.

Note that a login token travels in a header on every request. Over plain `http://` on a shared network, anyone watching the traffic can read it and act as that user. That is acceptable on your own Wi-Fi while learning; **a real deployment must be HTTPS**.

---

## Step 6 — Build and run the app

Start an emulator (easiest from Android Studio's **Device Manager**), or plug in a phone with **USB debugging** enabled in Developer Options.

Confirm the device is visible:

```powershell
adb devices
```

You should see one entry that is not `offline`. If `adb` is not found, use the full path, e.g. `C:\Users\ISSAH\AppData\Local\Android\Sdk\platform-tools\adb.exe`, or add that `platform-tools` folder to your `PATH`.

Then, in a **second terminal** (leave the backend running in the first):

```powershell
cd android
.\gradlew.bat installDebug
adb shell am start -n com.example.communityvoice/.MainActivity
```

The first build downloads a lot and can take several minutes. After that, rebuilds are quick.

To build the APK without installing it:

```powershell
.\gradlew.bat assembleDebug
```

The file lands at `android/app/build/outputs/apk/debug/app-debug.apk`.

To see the app's HTTP traffic:

```powershell
adb logcat -s OkHttp
```

> The logger is set to `HEADERS`, not `BODY`, on purpose — a password typed into the sign-up form would otherwise be printed to the log. If you need to see response bodies while debugging, change the level in [android/app/src/main/java/com/example/communityvoice/data/ApiClient.kt](android/app/src/main/java/com/example/communityvoice/data/ApiClient.kt) and change it back afterwards.

---

## Making somebody an official

Officials are the accounts that can move an issue from Open to Resolved. There is deliberately **no endpoint that promotes an account** — if there were, anybody could call it and claim to speak for the council. The role is set in the database by whoever administers the deployment.

Register the account through the app as normal, then in Atlas (**Browse Collections → `users`**) find the document by its `email` and change:

```json
"role": "CITIZEN"
```

to

```json
"role": "OFFICIAL"
```

Or from `mongosh`:

```
db.users.updateOne({ email: "clerk@council.example" }, { $set: { role: "OFFICIAL" } })
```

The change takes effect on that account's **very next request** — no sign-out needed, because the backend reads the role from the database on each call rather than trusting the copy inside the token. `"ADMIN"` does the same plus the ability to delete anybody's issue or comment.

---

## The API

Base URL: `http://localhost:8080` (from your computer).

Endpoints marked **auth** need an `Authorization: Bearer <token>` header. Endpoints marked *optional auth* work either way — send a token and the response includes your own vote in `myVote`; leave it off and you get the public view.

| Method | Path | Auth | What it does |
| --- | --- | --- | --- |
| `GET` | `/api/health` | — | Is the server alive |
| `POST` | `/api/auth/register` | — | Create an account, returns a token |
| `POST` | `/api/auth/login` | — | Sign in, returns a token |
| `GET` | `/api/me` | **auth** | Who this token belongs to |
| `PATCH` | `/api/me` | **auth** | Edit your own name, bio, neighbourhood |
| `GET` | `/api/users/{id}` | — | Anybody's public profile + contribution totals |
| `GET` | `/api/issues` | *optional* | The feed — search, filter, sort, page |
| `GET` | `/api/issues/{id}` | *optional* | One issue, with its status history |
| `POST` | `/api/issues` | **auth** | Raise an issue |
| `DELETE` | `/api/issues/{id}` | **auth** | Delete your own (or any, as admin) |
| `PUT` | `/api/issues/{id}/vote` | **auth** | Set your vote to SUPPORT or DECLINE |
| `DELETE` | `/api/issues/{id}/vote` | **auth** | Withdraw your vote |
| `PATCH` | `/api/issues/{id}/status` | **auth, official** | Move the issue along its lifecycle |
| `GET` | `/api/issues/{id}/comments` | *optional* | The discussion, oldest first |
| `POST` | `/api/issues/{id}/comments` | **auth** | Add a comment |
| `DELETE` | `/api/comments/{id}` | **auth** | Delete your own (or any, as admin) |
| `GET` | `/api/stats` | — | The numbers behind the Insights screen |

### `POST /api/auth/register`

```json
{
  "displayName": "Amina Yusuf",
  "email": "amina@example.com",
  "password": "marketroad7",
  "neighbourhood": "Market Road"
}
```

`password` must be at least 8 characters with at least one letter and one number. `neighbourhood` is optional.

**Response — `201 Created`**

```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "expiresAt": "2026-11-10T09:15:00Z",
  "user": {
    "id": "6705f1c2a4b3c21d9e4f8a01",
    "displayName": "Amina Yusuf",
    "email": "amina@example.com",
    "role": "CITIZEN",
    "initials": "AY",
    "avatarColor": "#1E5F8C",
    "bio": "",
    "neighbourhood": "Market Road",
    "joinedAt": "2026-10-11T09:15:00Z"
  }
}
```

**`409 Conflict`** if the email is taken:

```json
{
  "error": "EMAIL_ALREADY_REGISTERED",
  "message": "An account already exists for amina@example.com.",
  "fieldErrors": { "email": "That email is already registered" }
}
```

### `POST /api/auth/login`

```json
{ "email": "amina@example.com", "password": "marketroad7" }
```

Same response shape as register. A wrong email and a wrong password give the **same** `401` on purpose — telling somebody "no account has that email" would let a stranger work out who is registered here just by trying addresses.

```json
{
  "error": "INVALID_CREDENTIALS",
  "message": "That email and password do not match an account.",
  "fieldErrors": {}
}
```

### `GET /api/issues`

Every query parameter is optional.

| Parameter | Values | Default |
| --- | --- | --- |
| `q` | free text over title, description, location, author name | — |
| `category` | `ROADS` `WATER` `ELECTRICITY` `SANITATION` `HEALTH` `SCHOOLS` `SAFETY` `OTHER`, or `all` | all |
| `status` | `OPEN` `UNDER_REVIEW` `IN_PROGRESS` `RESOLVED` `DECLINED`, or `all` | all |
| `author` | a user id, or `me` | — |
| `votedBy` | a user id, or `me` — issues that member voted on, either way | — |
| `sort` | `trending` `top` `new` `discussed` `controversial` | `trending` |
| `page` | 0-based | 0 |
| `size` | 1–50 | 20 |

Values are case-insensitive, so `category=water` works. An unknown value gets a `400` listing the ones that would have worked, rather than silently ignoring it.

**Response — `200 OK`**

```json
{
  "items": [
    {
      "id": "6705f1c2a4b3c21d9e4f8a02",
      "title": "Broken water pump at the market",
      "description": "The pump has not worked for three weeks. Traders carry water from the next street.",
      "category": "WATER",
      "status": "IN_PROGRESS",
      "location": "Market Road, by the north gate",
      "author": {
        "id": "6705f1c2a4b3c21d9e4f8a01",
        "displayName": "Amina Yusuf",
        "initials": "AY",
        "avatarColor": "#1E5F8C",
        "role": "CITIZEN"
      },
      "supportCount": 12,
      "declineCount": 2,
      "netScore": 10,
      "commentCount": 4,
      "myVote": "SUPPORT",
      "authoredByViewer": false,
      "createdAt": "2026-10-07T16:40:00Z",
      "updatedAt": "2026-10-09T11:02:00Z",
      "statusHistory": [
        {
          "status": "UNDER_REVIEW",
          "note": "Passed to the water department.",
          "updatedByName": "Council Office",
          "updatedAt": "2026-10-08T08:30:00Z"
        },
        {
          "status": "IN_PROGRESS",
          "note": "Contractor scheduled for Monday.",
          "updatedByName": "Council Office",
          "updatedAt": "2026-10-09T11:02:00Z"
        }
      ]
    }
  ],
  "page": 0,
  "size": 20,
  "totalItems": 37,
  "totalPages": 2,
  "hasMore": true
}
```

`myVote` is `"SUPPORT"`, `"DECLINE"` or `null`. It is **always the caller's own vote** — there is no way to ask how a particular other person voted. `authoredByViewer` is what lets the app offer a Delete button only to the author.

### `POST /api/issues` — **auth**

```json
{
  "title": "Broken water pump at the market",
  "description": "The pump has not worked for three weeks. Traders carry water from the next street.",
  "category": "WATER",
  "location": "Market Road, by the north gate"
}
```

Title 6–120 characters, description 20–2000, `location` optional. Returns `201` with the created issue, which already has `supportCount: 1` — **the author counts as its first supporter**, which they can withdraw like any other vote.

### `PUT /api/issues/{id}/vote` — **auth**

```json
{ "stance": "SUPPORT" }
```

`PUT` rather than `POST` because it *sets* your vote rather than adding one:

- No vote yet → records it.
- You had voted the other way → **moves** your single vote across. Support goes up by one, declines go down by one.
- Already this stance → nothing changes, and the current issue comes back. (Sending the same thing twice is harmless.)

Returns `200` with the whole issue and its new counts. `DELETE` on the same path withdraws your vote, and is harmless if you had not voted.

### `PATCH /api/issues/{id}/status` — **auth, official only**

```json
{ "status": "IN_PROGRESS", "note": "Contractor scheduled for Monday." }
```

Appends to `statusHistory` rather than overwriting it, so the whole trail survives. A `CITIZEN` calling this gets:

```json
{
  "error": "FORBIDDEN",
  "message": "Only a verified official can change the status of an issue.",
  "fieldErrors": {}
}
```

### Error shape

Every error, from every endpoint, has the same three fields:

```json
{
  "error": "VALIDATION_FAILED",
  "message": "Title must be between 6 and 120 characters",
  "fieldErrors": { "title": "Title must be between 6 and 120 characters" }
}
```

`error` is a stable code the app can branch on; `message` is the sentence to show; `fieldErrors` maps a field name to its own message, which is what lets a form put the message under the right box.

| `error` | Status | When |
| --- | --- | --- |
| `VALIDATION_FAILED` | 400 | A field was blank, too short or badly formatted |
| `INVALID_BODY` | 400 | Malformed JSON, or an unknown enum value |
| `INVALID_REQUEST` | 400 | An unknown query parameter value |
| `INVALID_PARAMETER` | 400 | A query parameter of the wrong type |
| `UNAUTHORIZED` | 401 | No token, or an expired one, on an endpoint that needs one |
| `INVALID_CREDENTIALS` | 401 | Wrong email or wrong password |
| `FORBIDDEN` | 403 | Signed in, but not allowed to do this |
| `ISSUE_NOT_FOUND` / `USER_NOT_FOUND` / `COMMENT_NOT_FOUND` | 404 | No such thing |
| `EMAIL_ALREADY_REGISTERED` | 409 | That email is taken |
| `INTERNAL_ERROR` | 500 | Something unexpected — the real cause is in the server log, not the response |

### Trying the API from the terminal

Windows ships `curl.exe`. Call it by its full name — in PowerShell, plain `curl` is an alias for a different command with different syntax.

```powershell
# Register, and keep the token
curl.exe -X POST http://localhost:8080/api/auth/register ^
  -H "Content-Type: application/json" ^
  -d "{\"displayName\":\"Test Person\",\"email\":\"test@example.com\",\"password\":\"testpass1\"}"

# Paste the token from that response
$T = "eyJhbGciOiJIUzI1NiJ9..."

# Raise an issue
curl.exe -X POST http://localhost:8080/api/issues ^
  -H "Content-Type: application/json" ^
  -H "Authorization: Bearer $T" ^
  -d "{\"title\":\"Potholes on Market Road\",\"description\":\"Deep potholes near the junction that flood when it rains.\",\"category\":\"ROADS\"}"

# Read the feed as a signed-in user
curl.exe http://localhost:8080/api/issues?sort=new -H "Authorization: Bearer $T"

# Support it, then change your mind, then withdraw
curl.exe -X PUT http://localhost:8080/api/issues/PASTE_ID/vote -H "Content-Type: application/json" -H "Authorization: Bearer $T" -d "{\"stance\":\"SUPPORT\"}"
curl.exe -X PUT http://localhost:8080/api/issues/PASTE_ID/vote -H "Content-Type: application/json" -H "Authorization: Bearer $T" -d "{\"stance\":\"DECLINE\"}"
curl.exe -X DELETE http://localhost:8080/api/issues/PASTE_ID/vote -H "Authorization: Bearer $T"
```

Watch `supportCount` and `declineCount` across those last three calls — the total never goes above one vote from you.

---

## How the data is stored

### A user

```json
{
  "_id": { "$oid": "6705f1c2a4b3c21d9e4f8a01" },
  "displayName": "Amina Yusuf",
  "email": "amina@example.com",
  "passwordHash": "$2a$10$N9qo8uLOickgx2ZMRZoMye...",
  "role": "CITIZEN",
  "bio": "",
  "neighbourhood": "Market Road",
  "avatarColor": "#1E5F8C",
  "createdAt": { "$date": "2026-10-11T09:15:00Z" },
  "lastSeenAt": { "$date": "2026-10-11T09:15:00Z" }
}
```

`passwordHash` is a BCrypt hash. BCrypt is deliberately slow and salts every hash, so two people with the same password get different stored values and guessing one by brute force is expensive. The plain password is never stored and cannot be recovered from the hash. `email` carries a **unique index**, which is what actually stops two people registering the same address.

### An issue

```json
{
  "_id": { "$oid": "6705f1c2a4b3c21d9e4f8a02" },
  "title": "Broken water pump at the market",
  "description": "The pump has not worked for three weeks.",
  "category": "WATER",
  "status": "IN_PROGRESS",
  "location": "Market Road, by the north gate",
  "authorId": "6705f1c2a4b3c21d9e4f8a01",
  "authorName": "Amina Yusuf",
  "authorAvatarColor": "#1E5F8C",
  "authorRole": "CITIZEN",
  "supportCount": 12,
  "declineCount": 2,
  "supporterIds": ["6705f1c2a4b3c21d9e4f8a01", "..."],
  "declinerIds": ["..."],
  "commentCount": 4,
  "createdAt": { "$date": "2026-10-07T16:40:00Z" },
  "updatedAt": { "$date": "2026-10-09T11:02:00Z" },
  "statusHistory": [ { "status": "IN_PROGRESS", "note": "...", "updatedByName": "Council Office", "updatedAt": { "$date": "..." } } ]
}
```

Two things to notice:

**`supporterIds` and `declinerIds` never leave the server.** The app only ever sees the two counts plus its own `myVote`. Who voted which way is not something the API will tell you about anybody else.

**The author's name is copied onto the issue.** That means the feed is one query rather than one lookup per row. The cost is that a rename has to be propagated — `AuthService.updateProfile` rewrites `authorName` across that member's issues and comments when they change it.

### How a vote is applied

Each case below is a **single** MongoDB update whose query also describes the state it expects to find:

```
# Case 1 - no vote yet
find issue _id = X AND supporterIds does not contain U AND declinerIds does not contain U
  then add U to supporterIds, supportCount += 1

# Case 2 - changing sides
find issue _id = X AND declinerIds contains U
  then add U to supporterIds, remove U from declinerIds, supportCount += 1, declineCount -= 1
```

Because the check and the write happen together, two taps arriving at the same instant cannot both match — a vote can never be counted twice, and switching sides can never leave both counters raised. If neither case matches, that is already the stance, so the request was a no-op. See [backend/src/main/kotlin/com/communityvoice/backend/service/IssueService.kt](backend/src/main/kotlin/com/communityvoice/backend/service/IssueService.kt).

### How "Trending" is worked out

```
engagement = (supportCount - declineCount) + (commentCount × 0.5) + 1
score      = engagement / (hoursOld + 2) ^ 1.5
```

The classic news-aggregator shape: score over age, with age raised to a power so interest decays smoothly. The `+ 2` stops a brand-new issue dividing by nearly zero; the `+ 1` means a fresh issue with no votes yet still appears somewhere.

Because that score moves with the clock, it cannot be a database sort. `Trending` and `Most debated` are scored in Kotlin over the newest 500 matching issues, which is documented as `RANKING_WINDOW` in `IssueService`. Anything older than that falls back to the plain database ordering — which is the right answer anyway, since a month-old issue is not trending.

---

## Where everything lives

```
community voice/
├── README.md
├── tools/
│   ├── get-gradle-wrapper.ps1        one-time wrapper setup (Windows)
│   └── get-gradle-wrapper.sh         same, for macOS/Linux
│
├── backend/
│   ├── build.gradle.kts              dependencies and plugin versions
│   └── src/main/
│       ├── resources/application.yml reads MONGODB_URI and JWT_SECRET from the environment
│       └── kotlin/com/communityvoice/backend/
│           ├── CommunityVoiceApplication.kt   starts the server
│           ├── config/
│           │   ├── AppProperties.kt           the communityvoice: settings block
│           │   └── WebConfig.kt               BCrypt bean, @CurrentUser, CORS
│           ├── security/
│           │   ├── JwtService.kt              signs and verifies login tokens
│           │   ├── AuthUser.kt                the caller, + the @CurrentUser annotation
│           │   ├── AuthTokenFilter.kt         reads the Bearer header on every request
│           │   └── CurrentUserArgumentResolver.kt   fills in @CurrentUser, or returns 401
│           ├── model/
│           │   ├── User.kt                    the user document, roles, avatar colours
│           │   ├── Issue.kt                   the issue document, categories, statuses, trending
│           │   └── Comment.kt                 the comment document
│           ├── repository/                    UserRepository, IssueRepository, CommentRepository
│           ├── service/
│           │   ├── AuthService.kt             register / login / profiles
│           │   ├── IssueService.kt            feed, search, ranking, the vote logic
│           │   ├── CommentService.kt          threads, with the count kept in step
│           │   ├── StatsService.kt            the Insights aggregations
│           │   └── Exceptions.kt              one class per failure, mapped to a status
│           └── web/
│               ├── AuthController.kt          /auth/*, /me, /users/{id}
│               ├── IssueController.kt         /issues/*, incl. lenient query parsing
│               ├── CommentController.kt       /issues/{id}/comments, /comments/{id}
│               ├── MetaController.kt          /health, /stats
│               ├── AuthDtos.kt · IssueDtos.kt · CommentDtos.kt · StatsDtos.kt · CommonDtos.kt
│               └── ApiExceptionHandler.kt     turns every exception into tidy JSON
│
└── android/
    ├── gradle.properties             <-- API_BASE_URL lives here
    ├── local.properties              <-- you create this (SDK path)
    └── app/
        ├── build.gradle.kts          Android + Compose config
        └── src/
            ├── debug/                debug-only permission to use http://
            └── main/
                ├── AndroidManifest.xml
                ├── res/              app name, colours, launcher icon
                └── java/com/example/communityvoice/
                    ├── CommunityVoiceApplication.kt  wires ServiceLocator at startup
                    ├── MainActivity.kt               edge-to-edge + theme
                    ├── data/
                    │   ├── ApiModels.kt            every wire shape, in one file
                    │   ├── CommunityVoiceApi.kt    the endpoints, as an interface
                    │   ├── ApiClient.kt            Retrofit + the auth and 401 interceptors
                    │   ├── SessionStore.kt         remembers the token across restarts
                    │   ├── Repositories.kt         Auth / Issue / Comment / Stats
                    │   ├── ApiErrorMapper.kt       exceptions -> message + field errors
                    │   ├── Formatters.kt           relative times, compact counts
                    │   └── ServiceLocator.kt       the app's single instances
                    └── ui/
                        ├── CommunityVoiceApp.kt    routes, bottom bar, share intent
                        ├── theme/Theme.kt          light + dark, category + status colours
                        ├── components/Common.kt    avatars, pills, vote bar, state views
                        ├── auth/                   welcome, sign in, sign up
                        ├── feed/                   the list, filters, the issue card
                        ├── detail/                 one issue, its history, its discussion
                        ├── create/                 the form, with a live preview
                        ├── profile/                a member, their issues, edit + sign out
                        └── insights/               the community dashboard
```

---

## Notes on the design

A few decisions worth knowing about, and their costs.

**Authentication is hand-rolled rather than Spring Security.** The whole of it is four short files in `security/`: sign a token, read the header, look the user up, fill in a `@CurrentUser` parameter. Spring Security would give more for free — OAuth, sessions, method-level rules — at the price of a filter chain whose behaviour is hard to follow when you are learning. The tradeoff here favours being able to read the code.

**Whether an endpoint needs a login is declared on the endpoint.** `AuthTokenFilter` never rejects anything; it only works out who is calling. The decision lives in the parameter — `@CurrentUser user: AuthUser` means "required", `@CurrentUser(required = false) user: AuthUser?` means "optional". That keeps the rule next to the endpoint instead of in a URL pattern list that quietly drifts out of date as endpoints are added.

**The role is read from the database on every request, not from the token.** Slightly more work per call, but promoting somebody to `OFFICIAL`, renaming them, or deleting their account takes effect immediately rather than whenever their old token happens to expire.

**`supporterIds` and `declinerIds` are arrays inside the issue document.** Simple, and it makes the one-vote-per-person check a single atomic update. It would not survive an issue with a hundred thousand voters — MongoDB documents cap at 16MB, and the whole array is loaded on every read. At that scale votes belong in their own collection with a compound unique index on `(issueId, userId)`. For a community app this is the right shape.

**The token is kept in plain `SharedPreferences`.** That is app-private storage, which other apps cannot read on a normal device. On a rooted phone it is readable. Anything handling money or health records should move to `EncryptedSharedPreferences` or the Keystore; see the comment in `SessionStore.kt`.

**There is no "forgot password".** It needs an email sender, which needs an account with a mail provider, which is more setup than this project asks of you. It is the first thing to add for real use.

---

## Troubleshooting

**`Could not resolve placeholder 'MONGODB_URI'`**
The environment variable is not set in the terminal you are using. Repeat step 2 in that terminal.

**Backend hangs, then `MongoTimeoutException`**
Atlas is refusing the connection. Check *Network Access* has your current IP, and that the password in the URI is correct and percent-encoded if it has symbols.

**"JWT_SECRET is not set" in the startup log, and everybody is signed out after a restart**
Working as described — the server made up a random signing key, so the old tokens no longer verify. Set `JWT_SECRET` (step 2).

**App shows "Could not reach the server"**
Work through it in this order:

1. Is the backend still running in its terminal?
2. Does <http://localhost:8080/api/health> work in your computer's browser?
3. Is `API_BASE_URL` right for where the app is running — `10.0.2.2` for the emulator, your `192.168.x.x` for a real phone?
4. Did you rebuild after changing it? The URL is baked in at build time.
5. On a real phone, does `http://<your-ip>:8080/api/health` load in the phone's browser? If not, it is Wi-Fi or firewall, not the app.

**Signed in, then immediately back at the welcome screen**
The token was rejected, so the app signed out. Either it expired, or the server restarted without a fixed `JWT_SECRET`, or the account was deleted from the database. `adb logcat -s OkHttp` will show the `401`.

**"Only a verified official can change the status of an issue"**
Correct behaviour. See *Making somebody an official* above.

**The Support button says you already voted — or the count does not go up**
Tapping the side you already chose **withdraws** your vote; that is the design. Tap the other side to move it across. One account gets one vote per issue however many devices it uses, so reinstalling the app no longer gets you a second one — you would need a second account.

**`SDK location not found`**
`android/local.properties` is missing or the path is wrong. See step 4, and remember the doubled backslashes.

**`Failed to install the following SDK components` / licence errors**
Open Android Studio's SDK Manager and install API 34, or accept the licences from the terminal:

```powershell
C:\Users\ISSAH\AppData\Local\Android\Sdk\cmdline-tools\latest\bin\sdkmanager.bat --licenses
```

**`gradle-wrapper.jar is missing`**
You skipped step 1. Run the `tools\get-gradle-wrapper.ps1` script.

**Gradle build fails with a Java version error**
Run `java -version`. You need 17 or newer. If you have several JDKs, point `JAVA_HOME` at the right one.

**Changed the API URL and nothing happened**
It is compiled into the app as `BuildConfig.API_BASE_URL`, so a rebuild is required: `.\gradlew.bat installDebug`. You can also override it for a single build without editing the file:

```powershell
.\gradlew.bat installDebug -PAPI_BASE_URL=http://192.168.1.42:8080/
```

---

## Versions used

Kept deliberately on well-tested stable releases:

| | |
| --- | --- |
| Gradle | 8.10.2 |
| Kotlin | 2.0.21 |
| JDK | 17 (or newer) |
| Spring Boot | 3.3.5 |
| jjwt | 0.12.6 |
| Android Gradle Plugin | 8.6.1 |
| compileSdk / targetSdk | 34 |
| minSdk | 24 (Android 7.0) |
| Compose BOM | 2024.09.03 |
| Navigation Compose | 2.8.2 |
| Retrofit | 2.11.0 |

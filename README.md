<img width="863" height="487" alt="image" src="https://github.com/user-attachments/assets/3c15459a-7f7a-42ea-a681-6e3e273b0769" />

## Overview

**Project Description:** Divvy is a mobile app that simplifies group expense splitting by bringing it to the moment of payment. Rather than relying on users to manually log expenses afterward, Divvy integrates with bank transactions to quickly create a Splitwise-style interface whenever a purchase is likely to be shared.

Using [Plaid](https://plaid.com/) (or via bank statements), Divvy detects relevant transactions and allows users to instantly form groups through deep links or shared context. Users can scan receipts to extract itemized purchases, assign items to individuals, and automatically split tax and tip proportionally. Divvy also supports real-world scenarios such as one person covering another’s share, ensuring flexibility for how groups actually pay.

Each shared purchase generates a clear ledger showing who owes whom, making expenses easy to track and settle later. By combining payment context, receipt scanning, and seamless group setup, Divvy reduces friction and makes shared expenses faster, more accurate, and harder to forget.

## Getting Started

1. Open the repo root in Android Studio.
2. Sync Gradle.
3. Run the `app` configuration.
4. CLI build: `./gradlew :app:assembleDebug`

### Local environment setup

Add the following to `local.properties` (do not commit):
`@TeamMembers` Ask @vivekbw for env vars async

```
SUPABASE_URL=https://xxxx.supabase.co
SUPABASE_ANON_KEY=eyJ...
```

In Supabase Auth settings, enable Google and add `com.divvy.divvy://auth` as a redirect URL.

## Project Structure

- `app/src/main/java/com/example/divvy/backend` — repositories and data access
- `app/src/main/java/com/example/divvy/ui` — screens and navigation
- `app/src/main/java/com/example/divvy/models` — domain models
- `docs/` — meeting minutes, team contract, weekly updates

## Team

**Members:** Vivek Bhardwaj (vivekbw), Rayton Chen (raytonc), Alston (als10), Fayiz Ahmed Mohideen (FayizMohideen), Aadhyaaa Mashru (aadhyaaamashru), Aiden Ramgoolam (AidenAR)

## Links

- [Team contract](./docs/team-contract.md)
- [Meeting minutes](./docs/meetings/)
- [Meeting 1 - Jan 14th](./docs/meetings/2026_01_14/01%20-%20meeting%20minutes.md)
- [Meeting 2 - Jan 26th](./docs/meetings/2026_01_26/02%20-%20meeting_minutes.md)
- [Meeting 3 - Feb 4th](./docs/meetings/2026_02_04/03%20-%20meeting_minutes.md)

## Tech Stack

- Kotlin Android App + Compose (MVVM architechture)
- Supabase for DB + Authentication
- Vercel for CI + LLM capabilities (transaction categorization)

## Team Workflow

- [Linear](https://linear.app/) for project management / issue tracking / prioritization
- Figma for UI/UX mockup
- Discord for communication


## Acknowledgements

Parts of this codebase were developed with assistance from Claude Code (Anthropic, claude-opus-4-6, 2026).

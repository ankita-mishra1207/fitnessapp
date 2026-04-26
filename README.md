# 💪 FitnessApp - Your Android Health Companion

FitnessApp is a modern, feature-rich Android application designed to help users track their health and fitness journey seamlessly. It integrates with Google Health Connect for data synchronization and uses Supabase for cloud-based user profiles.

## 🚀 Key Features

*   **Google Health Connect Integration:** Automatically sync your steps, distance, and calories burned from other health apps.
*   **Real-time Cloud Sync:** Powered by **Supabase**, your profile data (weight, height, goals) is safely backed up and synced across devices.
*   **Intuitive Dashboard:** A clean, Material3-based UI providing a quick overview of your daily progress.
*   **Comprehensive Tracking:**
    *   **Water Intake:** Stay hydrated with a dedicated water tracker and goal setting.
    *   **Workout Logging:** Log your exercises and see them summarized in your reports.
    *   **Sleep Tracking:** Keep track of your rest patterns (Beta).
*   **Personalized Profile:** Calculate your BMI and set custom daily step and water goals.
*   **Onboarding Experience:** A smooth welcome for new users to set up their health journey.

## 🛠 Tech Stack

*   **Language:** Kotlin
*   **UI Framework:** Android XML with Material Design 3
*   **Architecture:** MVVM (Model-View-ViewModel) with Room Database for local persistence.
*   **Backend:** Supabase (PostgREST & Auth)
*   **Health APIs:** Android Health Connect
*   **Concurrency:** Kotlin Coroutines & Flow
*   **Dependency Management:** Gradle (Kotlin DSL)

## 📦 Getting Started

### Prerequisites
*   Android Studio Ladybug or newer.
*   Android device/emulator with Android 8.0 (API 26) or higher.
*   Health Connect app installed (for syncing features).

### Installation
1.  Clone the repository:
    ```bash
    git clone https://github.com/ankita-mishra1207/fitnessapp.git
    ```
2.  Open the project in Android Studio.
3.  Configure your Supabase URL and Key in `secrets.properties` (see `secrets.properties.example`).
4.  Build and run the app on your device!

## 📸 Screenshots
*(Coming Soon - Add your screenshots here!)*

## 📄 License
This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

---
Developed with ❤️ by [Aditya](https://github.com/Adityajain2829) & [Ankita](https://github.com/ankita-mishra1207)

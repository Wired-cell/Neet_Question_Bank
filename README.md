# Question Bank App

A comprehensive Android question bank application with offline OCR, diagram support, and AI-powered explanations.

## Features

### Vault System
- Hierarchical vault structure (Subjects → Sub-subjects)
- Default vaults: Physics, Chemistry (Physical, Inorganic, Organic), Biology (Botany, Zoology)
- Custom vault creation and management
- Color-coded vaults with icons

### Question Management
- Add questions with text, images, and diagrams
- Edit existing questions
- Organize questions by vault
- Mark questions as favorites
- Difficulty levels (Easy, Medium, Hard)
- Tag support

### Image & OCR
- **Offline OCR** using ML Kit Text Recognition
- Camera capture for question images
- Gallery selection for existing images
- Automatic text extraction from images
- Math symbol recognition and conversion (√, ², ³, →, etc.)

### Options Auto-Detection
- Automatic detection of options from OCR text
- Supports patterns: A) ..., B) ..., A. ..., B. ..., etc.
- Manual option editing
- Correct answer selection

### Diagram Support
- Diagram extraction from question images
- Manual cropping from OCR images
- Diagram preview
- Automatic diagram detection (physics/bio drawings)

### AI Explanation
- Fetch explanations using Google AI Studio API
- Configurable API key in settings
- Fallback to manual explanation entry

## Architecture

### Tech Stack
- **Language**: Kotlin
- **UI**: Jetpack Compose
- **Database**: Room (SQLite)
- **Image Capture**: CameraX
- **OCR**: ML Kit Text Recognition (offline)
- **Image Processing**: Custom utilities
- **Navigation**: Compose Navigation
- **Dependency Injection**: Manual (ViewModelFactory)

### Project Structure
```
app/
├── src/main/
│   ├── java/com/example/questionbank/
│   │   ├── database/          # Room database classes
│   │   ├── model/             # Data models (Vault, Question)
│   │   ├── ui/                # UI components
│   │   │   ├── screens/       # Composable screens
│   │   │   ├── theme/         # Theme and styling
│   │   │   └── viewmodel/     # ViewModels
│   │   ├── utils/             # Utility classes
│   │   └── QuestionBankApp.kt
│   └── res/                   # Resources
└── build.gradle.kts           # Module dependencies
```

### Database Schema

#### Vault Entity
- `id`: Primary key
- `name`: Vault name
- `parentId`: Parent vault ID (null for root vaults)
- `description`: Optional description
- `color`: Vault color
- `icon`: Emoji icon
- `createdAt`: Creation timestamp
- `updatedAt`: Last update timestamp

#### Question Entity
- `id`: Primary key
- `vaultId`: Foreign key to Vault
- `questionText`: Question text
- `options`: List of options (A, B, C, D)
- `correctOptionIndex`: Index of correct option (null if no correct answer)
- `explanation`: Explanation text
- `imageUri`: URI to question image
- `diagramUri`: URI to diagram image
- `isFavorite`: Favorite flag
- `difficulty`: Difficulty level
- `tags`: List of tags
- `createdAt`: Creation timestamp
- `updatedAt`: Last update timestamp

## Setup

### Prerequisites
- Android Studio (latest version)
- Android SDK 34 (compileSdk)
- Minimum SDK: 24 (Android 7.0 Nougat)
- Java 17+

### Dependencies
The app uses the following major dependencies:
- `androidx.compose`: Jetpack Compose
- `androidx.room`: Room Database
- `androidx.camera`: CameraX
- `com.google.mlkit:text-recognition`: ML Kit OCR
- `com.google.mlkit:object-detection-custom`: ML Kit Object Detection
- `io.coil-kt:coil-compose`: Image loading
- `com.squareup.retrofit2`: REST API calls
- `org.jetbrains.kotlinx:kotlinx-coroutines`: Coroutines

### Google AI Studio API
To enable AI-powered explanations:
1. Get an API key from [Google AI Studio](https://aistudio.google.com/)
2. Add the key in the app's Settings screen
3. The app will use this key to fetch explanations

## Usage

### Adding a Question
1. Tap the + button on the Vault List or Question List screen
2. Select a vault for the question
3. Add question text (or use OCR from image)
4. Add options (auto-detected from OCR or manually)
5. Select the correct option (if applicable)
6. Add an explanation (or fetch from AI)
7. Optionally add a diagram by cropping from the question image
8. Tap Save

### Using OCR
1. On the Add Question screen, tap "Image"
2. Choose Camera or Gallery
3. The app will automatically extract text from the image
4. Options will be auto-detected if found in the text
5. You can edit the extracted text as needed

### Managing Vaults
- Vaults are organized hierarchically
- Root vaults: Physics, Chemistry, Biology
- Sub-vaults: Physical, Inorganic, Organic (under Chemistry), Botany, Zoology (under Biology)
- Tap on a vault to view its questions
- Tap on a sub-vault to view questions in that category

## Implementation Notes

### Offline OCR
The app uses ML Kit's Text Recognition API with the Latin script model, which works completely offline. No internet connection is required for OCR functionality.

### Math Symbol Recognition
The app includes a post-processing step that converts common math symbols to their textual representations:
- √ → \sqrt
- ² → ^2
- ³ → ^3
- → → ->
- etc.

This makes math expressions more readable and searchable.

### Diagram Detection
The app includes basic heuristics for detecting diagrams:
- High edge density
- Low color variance
- These are placeholders for more sophisticated ML-based detection

### Image Cropping
The current implementation copies the full image as a diagram. In a production app, you would integrate a proper image cropping library like `com.canhub.cropper`.

## Future Enhancements

1. **Advanced Diagram Detection**: Use ML Kit's custom object detection to identify physics diagrams and biology drawings
2. **Better Image Cropping**: Integrate a proper image cropping library
3. **Math Rendering**: Use a proper math rendering library like MathView
4. **Search**: Implement full-text search across questions and explanations
5. **Export/Import**: Add functionality to export/import question banks
6. **Cloud Sync**: Sync questions across devices
7. **Statistics**: Track performance and provide analytics
8. **Quiz Mode**: Create quizzes from question banks

## Building the App

```bash
# Clone the repository
git clone <repository-url>

# Open in Android Studio
# Build and run on an Android device or emulator

# Or build from command line
./gradlew assembleDebug
```

## License

This project is licensed under the MIT License.

## Contributing

Contributions are welcome! Please fork the repository and submit pull requests.

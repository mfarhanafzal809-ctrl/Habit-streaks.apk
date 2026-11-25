import re

file_path = "app/src/main/java/com/example/native/SoundPlayer.kt"
with open(file_path, "r") as f:
    content = f.read()

# I will replace `saveCustomRingtone` entirely.
match = re.search(r'fun saveCustomRingtone\(inputStream: InputStream, originalFileName: String\): Result<Pair<String, String>> \{.*', content, flags=re.DOTALL)
if match:
    before = content[:match.start()]
    
new_method = """fun saveCustomRingtone(inputStream: InputStream, originalFileName: String): Result<Pair<String, String>> {
        return runCatching {
            // Save to external files dir so NotificationManager can read it natively!
            val ringtonesDir = File(context.getExternalFilesDir(android.os.Environment.DIRECTORY_RINGTONES), "custom_ringtones").apply {
                if (!exists()) mkdirs()
            }
            val sanitizedName = originalFileName.replace("[^a-zA-Z0-9._-]".toRegex(), "_")
            val targetFile = File(ringtonesDir, "custom_${System.currentTimeMillis()}_$sanitizedName")
            
            FileOutputStream(targetFile).use { out ->
                inputStream.copyTo(out)
            }
            val uriString = Uri.fromFile(targetFile).toString()
            val displayName = originalFileName.substringBeforeLast(".")
            Pair(displayName, uriString)
        }
    }
}
"""

with open(file_path, "w") as f:
    f.write(before + new_method)

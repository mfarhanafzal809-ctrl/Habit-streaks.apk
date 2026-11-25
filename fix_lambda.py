main_activity_path = "app/src/main/java/com/example/MainActivity.kt"
with open(main_activity_path, "r") as f:
    lines = f.readlines()

for i, line in enumerate(lines):
    if "onCustomRingtonePicked = { uri ->" in line:
        lines[i+1] = "                context.contentResolver.openInputStream(uri)?.let { inputStream ->\n"
        lines[i+2] = "                    val fileName = \"custom_ringtone_${System.currentTimeMillis()}.mp3\"\n"
        lines[i+3] = "                    val result = viewModel.saveCustomRingtone(inputStream, fileName)\n"
        lines[i+4] = "                    if (result.isSuccess) {\n"
        lines[i+5] = "                        result.getOrNull()\n"
        lines[i+6] = "                    } else null\n"
        lines[i+7] = "                }\n"
        break

with open(main_activity_path, "w") as f:
    f.writelines(lines)

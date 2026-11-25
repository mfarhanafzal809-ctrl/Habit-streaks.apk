import re

file_path = "gradle/libs.versions.toml"
with open(file_path, "r") as f:
    content = f.read()

if "vico" not in content:
    content = content.replace("[versions]", "[versions]\nvico = \"1.15.0\"")
    vico_libs = """
vico-compose = { group = "com.patrykandpatrick.vico", name = "compose", version.ref = "vico" }
vico-compose-m3 = { group = "com.patrykandpatrick.vico", name = "compose-m3", version.ref = "vico" }
vico-core = { group = "com.patrykandpatrick.vico", name = "core", version.ref = "vico" }
"""
    content = content.replace("[libraries]", "[libraries]" + vico_libs)
    with open(file_path, "w") as f:
        f.write(content)

file_path = "app/build.gradle.kts"
with open(file_path, "r") as f:
    content = f.read()

if "vico" not in content:
    deps = """
  implementation(libs.vico.compose)
  implementation(libs.vico.compose.m3)
  implementation(libs.vico.core)
"""
    content = content.replace("implementation(libs.androidx.compose.material3)", "implementation(libs.androidx.compose.material3)" + deps)
    with open(file_path, "w") as f:
        f.write(content)

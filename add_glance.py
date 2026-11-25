import re

file_path = "app/build.gradle.kts"
with open(file_path, "r") as f:
    content = f.read()

glance_deps = """
  implementation("androidx.glance:glance-appwidget:1.0.0")
  implementation("androidx.glance:glance-material3:1.0.0")
"""
content = content.replace("dependencies {", "dependencies {" + glance_deps)

with open(file_path, "w") as f:
    f.write(content)

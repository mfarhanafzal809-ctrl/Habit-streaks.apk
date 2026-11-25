import re

file_path = "app/src/main/AndroidManifest.xml"
with open(file_path, "r") as f:
    content = f.read()

widget_xml = """
        <receiver
            android:name=".widget.HabitWidgetReceiver"
            android:exported="true">
            <intent-filter>
                <action android:name="android.appwidget.action.APPWIDGET_UPDATE" />
            </intent-filter>
            <meta-data
                android:name="android.appwidget.provider"
                android:resource="@xml/habit_widget_info" />
        </receiver>
"""

content = content.replace("</application>", widget_xml + "</application>")

with open(file_path, "w") as f:
    f.write(content)

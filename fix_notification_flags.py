import re

file_path = "app/src/main/java/com/example/native/NotificationHelper.kt"
with open(file_path, "r") as f:
    content = f.read()

# Replace the notify line with insistent flags
content = content.replace("notificationManager.notify(notificationId, builder.build())", """val notification = builder.build()
        notification.flags = notification.flags or android.app.Notification.FLAG_INSISTENT
        notificationManager.notify(notificationId, notification)""")

with open(file_path, "w") as f:
    f.write(content)

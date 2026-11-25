#!/bin/bash
awk '
BEGIN { inApp = 0; }
/^@Composable/ { if (inApp == 0) { lastAt = NR; } }
/^fun HabitStreaksApp/ { inApp = 1; print "REPLACE_MARKER"; next; }
{
    if (inApp) {
        if (/^}$/) {
            inApp = 0;
            next;
        }
    } else {
        print $0;
    }
}
' app/src/main/java/com/example/MainActivity.kt > tmp_main.kt

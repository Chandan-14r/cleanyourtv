#!/system/bin/sh
# Watch ActivityManager for Fire TV's stock home launch and bring CleanYourTV forward.
# This runs as the ADB shell user after the PC has authorized this host.
PID_FILE=/data/local/tmp/home_daemon.pid
printf '%s\n' "$$" > "$PID_FILE"
while true; do
  logcat -v brief -b system -s ActivityManager:I 2>/dev/null | while IFS= read -r line; do
    case "$line" in
      *cmp=com.amazon.tv.launcher*|*cmp=com.amazon.firehomestarter*)
        case "$line" in
          *START*|*category.HOME*|*HomeActivity*)
            am start -n com.gathin.tvhome/.MainActivity >/dev/null 2>&1
            ;;
        esac
        ;;
    esac
  done
  sleep 1
done
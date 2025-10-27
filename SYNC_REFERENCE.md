# Syndicate Project Sync Reference

## Quick Sync Command
Run this command in PowerShell to sync from mounted directory to local:

```powershell
robocopy "Z:\dev\syndicate" "F:\syndicate" /MIR /R:3 /W:10 /XD .gradle build .idea /XF *.log *.tmp
```

## Command Explanation
- `/MIR` - Mirror directories (copies all files and removes files that don't exist in source)
- `/R:3` - Retry failed copies 3 times
- `/W:10` - Wait 10 seconds between retries
- `/XD .gradle build .idea` - Exclude these directories (build artifacts and IDE files)
- `/XF *.log *.tmp` - Exclude log and temporary files

## Usage
1. Run the command from any PowerShell window
2. Wait for sync to complete
3. Open `F:\syndicate` in Android Studio
4. Start developing!

## Notes
- Source: `Z:\dev\syndicate` (mounted network/VM directory)
- Destination: `F:\syndicate` (local Windows directory for Android Studio)
- This command is safe to run multiple times - it only copies changed files
p = 'android/app/src/main/AndroidManifest.xml'
s = open(p, encoding='utf-8').read()
perms = ('<uses-permission android:name="android.permission.FOREGROUND_SERVICE"/>\n'
         '    <uses-permission android:name="android.permission.FOREGROUND_SERVICE_MEDIA_PLAYBACK"/>\n'
         '    <uses-permission android:name="android.permission.POST_NOTIFICATIONS"/>\n'
         '    <uses-permission android:name="android.permission.WAKE_LOCK"/>\n    ')
s = s.replace('<application', perms + '<application', 1)
s = s.replace('</application>',
              '<service android:name=".MediaService" android:exported="false" '
              'android:foregroundServiceType="mediaPlayback"/>\n    </application>', 1)
open(p, 'w', encoding='utf-8').write(s)
print(s)

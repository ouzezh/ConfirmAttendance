http://www.seleniumhq.org/download/


selenium 3.14.0
GoogleChrome 65.0.3325.146
chromedriver.exe 2.38.552522


https://www.portablesoft.org/google-chrome/
http://npm.taobao.org/mirrors/chromedriver/



--start_shell start--
cd C:\Dev\workspace\ConfirmAttendance
java -cp ConfirmAttendance.jar com.ozz.ConfirmAttendance
taskkill /f /t /im chromedriver.exe
pause
--start_shell end--

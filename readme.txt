cd C:\Dev\workspace\ConfirmAttendance
java -Djava.ext.dirs=ext -jar target.jar
taskkill /f /t /im chromedriver.exe
pause
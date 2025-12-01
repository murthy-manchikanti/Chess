# Chess.java.app

Attributions:
https://www.flaticon.com/free-icons/chess Chess icons created by Freepik - Flaticon

# Usage
To run it locally, first install maven using:
```
mvn install
```
Once installed, go into the root directory of src using:
```
cd ..
```
From there, run:
```
mvn clean package
```
so that it rebuilds the jar files. Once that is done, please run:
```
java -jar target/chess-game-1.0-SNAPSHOT-jar-with-dependencies.jar
```
this will check if the .jar file is working correctly and launching ChessGameGUI. Once it successfully opens, close the tab and return to the terminal.
After that has been done, finally run this command:
```
jpackage \
  --input target/ \
  --name Chess.java \
  --main-jar chess-game-1.0-SNAPSHOT-jar-with-dependencies.jar \
  --main-class com.chessgame.ChessGameGUI \
  --type dmg \
  --icon src/main/resources/icon.icns \
  --dest /Users/<your-mac-username>/Downloads \
  --mac-package-name Chess.java
```
this will compile all the source files into a .dmg file and places it in your local download folder. After this, double click the .dmg file and drag it into the Applications folder.
Once that has been done, go into 
```
Launchpad or Spotlight >> Applications (macOS Tahoe® and Later)
```
and click on the app again. It will ask you if you are sure to open the app. Click on open. It will say that it is not a verified app (waiting on Developer ID). 
To bypass it, go to System Settings >> Privacy and Security and scroll down until you see:
```
Security
```
After which there will be a button on the bottom left:
```
Open Anyway
```
and then Enter your password or Touch ID. It will then open the Chess.app and you are good to go 😃!


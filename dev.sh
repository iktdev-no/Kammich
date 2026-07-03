#!/bin/bash

REMOTE_USER="bskjon"
REMOTE_HOST="192.168.2.22"
REMOTE_DIR="/opt/kammich"
REMOTE_JAR="kammich.jar"
LOCAL_JAR="build/libs/Kammich-0.0.1-SNAPSHOT.jar"

cleanup() {
    echo -e "\n>>> Avbryter..."
    kill "$LOG_PID" 2>/dev/null

    ssh "$REMOTE_USER@$REMOTE_HOST" "pkill -f $REMOTE_JAR" 2>/dev/null

    exit
}

trap cleanup SIGINT

echo ">>> Bygger første versjon..."
./gradlew clean bootJar -x test || exit 1

echo ">>> Overfører første jar..."
scp "$LOCAL_JAR" "$REMOTE_USER@$REMOTE_HOST:$REMOTE_DIR/$REMOTE_JAR"

echo ">>> Starter jar på remote..."
ssh "$REMOTE_USER@$REMOTE_HOST" "nohup java -jar $REMOTE_DIR/$REMOTE_JAR > $REMOTE_DIR/out.log 2>&1 &"

echo ">>> Starter loggstrøm..."
ssh "$REMOTE_USER@$REMOTE_HOST" "tail -f $REMOTE_DIR/out.log" &
LOG_PID=$!

echo ">>> Overvåker src/... Trykk Ctrl+C for å stoppe."

while true; do
    inotifywait -q -r -e close_write src/ build.gradle.kts

    echo ">>> Endring oppdaget! Re-bygger..."

    if ./gradlew clean bootJar -x test; then
        echo ">>> Stopper remote jar..."
        ssh "$REMOTE_USER@$REMOTE_HOST" "pkill -f $REMOTE_JAR"

        echo ">>> Overfører ny jar..."
        scp "$LOCAL_JAR" "$REMOTE_USER@$REMOTE_HOST:$REMOTE_DIR/$REMOTE_JAR"

        echo ">>> Starter jar på nytt..."
        ssh "$REMOTE_USER@$REMOTE_HOST" "nohup java -jar $REMOTE_DIR/$REMOTE_JAR > $REMOTE_DIR/out.log 2>&1 &"

        echo ">>> Restartet. Loggstrøm fortsetter."
    else
        echo ">>> Bygg feilet!"
    fi
done

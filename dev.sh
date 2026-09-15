#!/bin/bash

REMOTE_USER="bskjon"
#REMOTE_HOST="192.168.2.22"
REMOTE_HOST="koya.tail2f4332.ts.net"
REMOTE_DIR="/var/lib/kammich"
REMOTE_JAR="Kammich.jar"
LOCAL_JAR="build/libs/Kammich-0.0.1-SNAPSHOT.jar"

KILL_CMD='pkill -9 -f "java.*Kammich.jar"'
KILL_SERVICE='sudo systemctl stop kammich-backend.service'

cleanup() {
    echo -e "\n>>> Avbryter..."
    kill "$LOG_PID" 2>/dev/null

    echo ">>> Stopper remote Kammich.jar..."
    ssh "$REMOTE_USER@$REMOTE_HOST" "$KILL_CMD" 2>/dev/null

    exit
}

trap cleanup SIGINT

echo ">>> Bygger første versjon..."
./gradlew bootJar -x test || exit 1

echo ">>> Stopper remote kammich.jar..."
ssh "$REMOTE_USER@$REMOTE_HOST" "$KILL_CMD"
ssh "$REMOTE_USER@$REMOTE_HOST" "$KILL_SERVICE"

echo ">>> Overfører første jar..."
scp "$LOCAL_JAR" "$REMOTE_USER@$REMOTE_HOST:$REMOTE_DIR/$REMOTE_JAR"

echo ">>> Starter jar på remote..."
ssh "$REMOTE_USER@$REMOTE_HOST" \
    "cd '$REMOTE_DIR' && (nohup java -jar '$REMOTE_JAR' > '$REMOTE_DIR/out.log' 2>&1 </dev/null &)"

echo ">>> Starter loggstrøm..."
ssh "$REMOTE_USER@$REMOTE_HOST" "tail -f $REMOTE_DIR/out.log" &
LOG_PID=$!

echo ">>> Overvåker src/... Trykk Ctrl+C for å stoppe."

while true; do
    inotifywait -q -r -e close_write src/ build.gradle.kts

    echo ">>> Endring oppdaget! Re-bygger..."

    if ./gradlew bootJar -x test; then
        echo ">>> Stopper remote jar..."
        ssh "$REMOTE_USER@$REMOTE_HOST" "$KILL_CMD"

        echo ">>> Overfører ny jar..."
        scp "$LOCAL_JAR" "$REMOTE_USER@$REMOTE_HOST:$REMOTE_DIR/$REMOTE_JAR"

        echo ">>> Starter jar på nytt..."
        ssh "$REMOTE_USER@$REMOTE_HOST" \
            "cd '$REMOTE_DIR' && (nohup java -jar '$REMOTE_JAR' > '$REMOTE_DIR/out.log' 2>&1 </dev/null &)"

        echo ">>> Restartet. Loggstrøm fortsetter."
    else
        echo ">>> Bygg feilet!"
    fi
done

from vosk import Model, KaldiRecognizer
import pyaudio
import json
import firebase_admin
from firebase_admin import credentials
from firebase_admin import db
import concurrent.futures
from pygatt import GATTToolBackend
import Levenshtein
import time
import sys
from periphery import GPIO
import time
import bluetooth
import multiprocessing
from datetime import datetime
import asyncio
import websockets

sys.stdout.flush()

cred = credentials.Certificate("/home/rock/Desktop/scripts/Final/service_account.json")
firebase_admin.initialize_app(cred,{"databaseURL": "https://wheelcontroller-25adc-default-rtdb.firebaseio.com"})


PIN_ENABLE = 146 # 23
PIN_INPUT_11 = 150 # 24
PIN_INPUT_12 = 153 # 26

PIN_INPUT_21 = 147 # 19
PIN_INPUT_22 = 149 # 21

motor_enable1 = GPIO(PIN_ENABLE, "out")
motor_input_11 = GPIO(PIN_INPUT_11,"out")
motor_input_12 = GPIO(PIN_INPUT_12,"out")
motor_input_21 = GPIO(PIN_INPUT_21,"out")
motor_input_22 = GPIO(PIN_INPUT_22,"out")

def control_motor_1(enable, direction):
    motor_enable1.write(enable)

    if direction == "forward":
        motor_input_11.write(True)
        motor_input_12.write(False)
    elif direction == "backward":
        motor_input_11.write(False)
        motor_input_12.write(True)
    else:
        motor_input_11.write(False)
        motor_input_12.write(False)

def control_motor_2(enable, direction):
    motor_enable1.write(enable)

    if direction == "forward":
        motor_input_21.write(True)
        motor_input_22.write(False)
    elif direction == "backward":
        motor_input_21.write(False)
        motor_input_22.write(True)
    else:
        motor_input_21.write(False)
        motor_input_22.write(False)

def pushLog(type,log):
    logRef = db.reference("/chats/123456/rock/logs/")

    ts = time.time()
    logRef.child(str(int(ts*100))).set({
        'type': type,
        'message': log,
        'timestamp': ts
    })


last_execution_time = 0
def execute(command):
    global last_execution_time
    cur_time = time.time()

    if(cur_time - last_execution_time) < .2: # if multiple requests within 200 milli-seconds
        return
    
    last_execution_time = cur_time
    direction = "Invalid"

    if command == 1:
        print("left")
        direction = "left"
        control_motor_1(enable=True,direction="stop")
        control_motor_2(enable=True,direction="forward")
    elif command == 2:
        print("right")
        direction = "right"
        control_motor_1(enable=True,direction="forward")
        control_motor_2(enable=True,direction="stop")
    elif command == 3:
        print("forward")
        direction = "forward"
        control_motor_1(enable=True,direction="forward")
        control_motor_2(enable=True,direction="forward")
    elif command == 4:
        print("backward")
        direction = "backward"
        control_motor_1(enable=True,direction="backward")
        control_motor_2(enable=True,direction="backward")
    elif command == 5:
        print("stop")
        direction = "stop"
        control_motor_1(enable=True,direction="stop")
        control_motor_2(enable=True,direction="stop")
    else:
        print("Invalid")
        pushLog("E","Invalid command requested. Ignoring...")
    
#    if(direction != "Invalid"):
#        pushLog("I","Executed command for user command: "+str(direction))



predefined_words = ["start", "stop", "left", "right", "up", "down"]
def match_text(input_text, max_difference=3):
    best_match = None
    min_difference = max_difference + 1  # Initialize with a value higher than the maximum allowed difference

    for word in predefined_words:
        difference = Levenshtein.distance(input_text, word)
        if difference < min_difference:
            min_difference = difference
            best_match = word

    if min_difference <= max_difference:
        if(best_match == "left"):
            return 1
        elif best_match == "up":
            return 3
        elif best_match == "right":
            return 2
        elif best_match == "down":
            return 4
        else:
            return 5
    else:
        return -1

def startListening():
    model = Model(r"vosk-model-small-en-us-0.15")

    #model = Model(r"vosk-model-en-us-0.22-lgraph")

    recognizer = KaldiRecognizer(model, 48000)
        
    mic = pyaudio.PyAudio()
    stream = mic.open(format=pyaudio.paInt16, channels=1, rate=48000, input=True, frames_per_buffer=8192)
    stream.start_stream()
    
    count = 0
    while True:
        try:
            if( stream.is_active()):
                data = stream.read(4096)

                if recognizer.AcceptWaveform(data):
                    text = recognizer.Result()

                    data = json.loads(text)

                    dummy_speech = data['text']

                    command = match_text(dummy_speech)
                    
                    if(command != -1):
                        execute(command)
                    
                    print(command)
            else:
                break

        except Exception as e:
            print(e)
            if count > 5:
                break
            count = count+1

def readFromDatabase_no_longer_used():
    
    ref = db.reference("/commands/123456/")

    def listener(event):
        full_data = ref.get()
        
        pushLog("I","Data retrieved: "+str(full_data))
        #print(full_data)
        command = full_data['my_command']['command']

        execute(command)
        print(command)

    # Attach the listener
    ref.listen(listener)

def readFromBT():
    server_sock = bluetooth.BluetoothSocket(bluetooth.RFCOMM)
    
    port = 1
    server_sock.bind(("",port))
    server_sock.listen(1)

    pushLog("I","Bluetooth ready. Waiting for connection via bluetooth...")

    #uuid = "00001101-0000-1000-8000-00805F9B34FB"
    #bluetooth.advertise_service(
    #    server_sock, "MyService",service_id=uuid,
    #    service_classes=[uuid,bluetooth.SERIAL_PORT_CLASS],
    #    profiles=[bluetooth.SERIAL_PORT_PROFILE]
    #)
    print("Advertised")

    client_sock, address = server_sock.accept();

    print("Accepted from ",address)
    pushLog("I","Bluetooth connected with address: "+str(address))

    try:
        while True:
            data = client_sock.recv(1024)
            if not data:
                break
            print("Received data: {}".format(data.decode()))
            # Process the received data as needed
            command = int(data.decode())
            execute(command)
            #pushLog("I","Received data via bluetooth: {}".format(data.decode()))

    except Exception as e:
        pushLog("E","Error occurred while connecting via Bluetooth. Retrying after 4s...")
        print("Error during data reception:", e)
        time.sleep(4)

    finally:
        client_sock.close()
        server_sock.close()

    pushLog("E","Restarting bluetooth server...")
    print("Restarting bluetooth server....")
    readFromBT()


def readFromSocket():

    async def handler(websocket, path):
        #print("Connection eshtablished")
        data = await websocket.recv()
        print(f"Data received from socket: {data}")
            # Process the received data as needed
        command = int(data)
        execute(command)

    #    reply = f"Data received as: {data}!"
    #    await websocket.send(reply)
    #    print("Response sent")

    try:
        start_server = websockets.serve(handler,"0.0.0.0",8000)
        asyncio.get_event_loop().run_until_complete(start_server)
        asyncio.get_event_loop().run_forever()
    except Exception as e:
        pushLog("E","Unknown error in socket. Restarting socket...")
        readFromSocket()


def readAppData():
    readRef = db.reference("/chats/123456/app/")

    def readListener(event):
        data = readRef.get()
        print(data)
        # do other operation if needed
    readRef.listen(readListener)

def writeLastActive():
    writeRef = db.reference("/chats/123456/rock/")

    while True:
        try:
            ts = time.time()
            writeRef.child("last_active").set(ts)
            time.sleep(5)
        except Exception as e:
            continue


if __name__ == "__main__":
#     readFromBT()
    process1 = multiprocessing.Process(target=readFromBT)
    #process2 = multiprocessing.Process(target=readFromDatabase)
    process2 = multiprocessing.Process(target=readFromSocket)
    process3 = multiprocessing.Process(target=readAppData)
    process4 = multiprocessing.Process(target=writeLastActive)

    process1.start()
    process2.start()
    process3.start()
    process4.start()

#with concurrent.futures.ThreadPoolExecutor() as executor:
        
        #futureSpeech = executor.submit( startListening )
        
        #futureOffline = executor.submit( readFromBT )
        #futureDatabase = executor.submit( readFromDatabase )
        #futureAppData = executor.submit( readAppData )
        #futureLastActive = executor.submit( writeLastActive )

        #concurrent.futures.wait([futureOffline])#,futureDatabase, futureAppData, futureLastActive]) # it will wait indefinitely

        # Access the result of each function (if needed)
        #result1 = futureSpeech.result()
        #result2 = futureDatabase.result()
        #result3 = futureOffline.result()
        #result4 = futureAppData.result()
        #result5 = futureLastActive.result()


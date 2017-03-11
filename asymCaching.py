import os,sys,thread,socket
#from maxp import maxpFingerprinting
import hashlib
import httplib
import urllib2
import signal
#import fcntl
import struct
import threading
import urlparse
import time
from datetime import datetime
import httplib
from httplib import HTTP
from urlparse import urlparse
from lepl.apps.rfc3696 import HttpUrl

#********* CONSTANTS *********
BACKLOG = 50            # how many pending connections queue will hold
MAX_DATA_RECV = 100000    # max number of bytes we receive at once
DEBUG = True           # set to True to see the debug msgs
CACHE_SIZE = 500000
CURRENT_CACHE_SIZE = 0
BLOCK_SIZE = 2000
TOTAL_REQUESTS = 0
RANGE = 100000
hash_size_table = {}
temp_hash_list = []
overall_list = []
reqs = 1
int_resp_list = []
byteslist = []

class Chunk:
    def __init__(self):
        self.hash = ""
        self.chunk = ""

    def getHash(self):
        return self.hash


def maxpFingerprinting(data):
    global CURRENT_CACHE_SIZE
    print "In MAXP"
    cachedMarkers = []
    hitMarkers = []
    newHashes = []
    oldHashes = []
    previousMarker = 0
    currentMarker = 0
    nextMarker=0
    window = 128
    p = 256
    totalLength = len(data)
    totalChunksCached = 0
    hitCount = 0
    totcahedsize = 0
    redusize = 0
    while currentMarker+window<totalLength:
        currentMarker = nextMarker + window
        if currentMarker >= totalLength:
            previousMarker=nextMarker
            nextMarker = len(data)-1
        else:
            value = data[currentMarker]
            maxp = currentMarker
            for x in range(1,p,1):
                if currentMarker+x > totalLength-1:
                    break
                val = data[currentMarker+x]
                if value < val:
                    value = val
                    maxp = currentMarker+x

            previousMarker=nextMarker
            nextMarker=maxp
        ch = Chunk()
        ch.chunk = data[previousMarker:nextMarker+1]
        sha_1 = hashlib.sha1(ch.chunk)
        sha_1.update(ch.chunk)
        ch.hash = sha_1.hexdigest()

        temp_hash_list.append(ch.hash)

    print "Size of Hashes temp", len(temp_hash_list)
    for ha in temp_hash_list:
        if ha not in hash_size_table.keys():
            return False

    return True


def getHashes(data):
    global CURRENT_CACHE_SIZE
    print "In MAXP"
    cachedMarkers = []
    hitMarkers = []
    newHashes = []
    oldHashes = []
    previousMarker = 0
    currentMarker = 0
    nextMarker=0
    window = 128
    p = 256
    totalLength = len(data)
    totalChunksCached = 0
    hitCount = 0
    totcahedsize = 0
    redusize = 0
    while currentMarker+window<totalLength:
        currentMarker = nextMarker + window
        if currentMarker >= totalLength:
            previousMarker=nextMarker
            nextMarker = len(data)-1
        else:
            value = data[currentMarker]
            maxp = currentMarker
            for x in range(1,p,1):
                if currentMarker+x > totalLength-1:
                    break
                val = data[currentMarker+x]
                if value < val:
                    value = val
                    maxp = currentMarker+x

            previousMarker=nextMarker
            nextMarker=maxp
        ch = Chunk()
        ch.chunk = data[previousMarker:nextMarker+1]
        sha_1 = hashlib.sha1(ch.chunk)
        sha_1.update(ch.chunk)
        ch.hash = sha_1.hexdigest()

        temp_hash_list.append(ch.hash)

    hashes = ','.join(temp_hash_list)
    return hashes


def putHashesandSizes(data):
    global CURRENT_CACHE_SIZE
    size = 0
    print "In storing hashes"
    hashandsizes = data.split(',')
    print hashandsizes
    print "Length of hashes", len(hashandsizes)
    if len(hashandsizes) % 2 != 0:
        hashandsizes.pop()
    for x in range(0,len(hashandsizes),2):
        overall_list.append(hashandsizes[x])
        hash_size_table[hashandsizes[x]] = int(hashandsizes[x+1])
        CURRENT_CACHE_SIZE += int(hashandsizes[x+1])
        if CURRENT_CACHE_SIZE > CACHE_SIZE:
            evict_chunks_from_cache()
        size += int(hashandsizes[x+1])

    print "current cache size", CURRENT_CACHE_SIZE
    print "Cached size", size
    return size

def evict_chunks_from_cache():
    global CURRENT_CACHE_SIZE
    print "In Cache eviction"
    for x in range(0,len(overall_list)/3,1):
        removedhash = overall_list.pop(0)
        print "Removed hash",removedhash
        size = hash_size_table.pop(removedhash,None)
        if size is not None:
            CURRENT_CACHE_SIZE -= size
    # while (sys.getsizeof(chunklist) + sys.getsizeof(ch)) <= CACHE_SIZE:
    #     removedChunk = chunklist.pop(0)
    #     table.pop(removedChunk.hash,None)
    return None

def checkUrl(url):
    validator = HttpUrl()
    if validator(url):
        # p = urlparse(url)
        # conn = httplib.HTTPConnection(p.netloc)
        # conn.request('HEAD', p.path)
        # resp = conn.getresponse()
        # return resp.status < 400
        return True
    else:
        return False



def proxy_thread(conn, client_addr):
    global TOTAL_RESPONSE_SIZE
    global TOTAL_REQUESTS
    global reqs
    try:
        print >>sys.stderr, 'connection from', client_addr

        # Receive the data in small chunks and retransmit it
        while True:
            data = conn.recv(MAX_DATA_RECV)
            total_sent = 0
            total_recieved = 0
            data = "http://" + data.rstrip()
            if(len(data) == 7):
                continue
            print >>sys.stderr, 'received "%s"' % data
            if not checkUrl(data):
                conn.sendall("ERRor")
                continue
            try:
                ret = urllib2.urlopen(data)
                if ret.code == 200:
                    print "Exists!"
                response = urllib2.urlopen(data).read()
            except urllib2.HTTPError, urllib2.URLError:
                print "HTTP or URL error"
                conn.sendall("ERRor")
                continue

            block_of_response = response[:BLOCK_SIZE]
            if maxpFingerprinting(block_of_response):
                # hashes for the block in server
                print "Hashes here"
                hashes = getHashes(block_of_response)
                conn.sendall(hashes)
                total_sent += len(hashes)
                data = conn.recv(MAX_DATA_RECV)
                print data[0], len(data)
                if(data.startswith("N")):
                    print "current response not cached in client"
                    conn.sendall(response)
                    total_sent += len(response)
                elif(data.startswith("T")):
                    print "current response cached in client"

            else:
                print "Hashes not here"
                # conn.sendall(response)
                # total_sent += len(response)
                # data = conn.recv(RANGE)
                # putHashesandSizes(data)
                # total_recieved += len(data)
                # hashes for the block not in server, so send the block
                conn.sendall(block_of_response)
                total_sent += len(block_of_response)
                # receive
                data = conn.recv(RANGE)
                total_recieved += len(data)
                if(data.startswith("Y")):
                    # data in the client store the hashes
                    print "received Hashes"
                    k = putHashesandSizes(data[1:])
                    total_recieved += k
                elif(data.startswith("O")):
                    # data not in the client, send the whole response
                    print "Sending Original Response"
                    conn.sendall(response)
                    total_sent += len(response)

            print "Amount of bytes sent for this request", total_sent
            print "Amount of bytes received for this request", total_recieved
            print "Amount of actual bytes of response", len(response)
            byteslist.append(total_sent+total_recieved)
            int_resp_list.append(len(response))
            with open("bwsavings.txt", "a") as myfile:
                # myfile.write( datetime.now().strftime('%Y-%m-%d %H:%M:%S') + "\t\tRequest " + data + "\t\tOriginal response size " + str(len(response)) + "\t\tEncode response size " + str(len(encodedresponse)) + "\t\tBytes Saved from transmitting " + str(len(response)-len(encodedresponse)) + "\n" )
                if reqs % 15 is 0:
                    # myfile.write("Bandwidth Savings" + str(float(sum(byteslist)) * 100 /float(sum(int_resp_list))) + " Bandwidth Gain" + str((float(sum(int_resp_list))/float(sum(int_encoresp_list)))) + "\n")
                    myfile.write(str(round((float(sum(int_resp_list)) - float(sum(byteslist))) * 100 /float(sum(int_resp_list)),2)) + "\n")
                    print "Total Bytes in AC", sum(byteslist), "Actual Bytes", sum(int_resp_list)
                    print "BW saving", str(round((float(sum(int_resp_list)) - float(sum(byteslist))) * 100 /float(sum(int_resp_list)),2))
                    byteslist[:] = []
                    int_resp_list[:] = []
            myfile.close()
            reqs += 1
    finally:
        # Clean up the connection
        conn.close()


#********* MAIN PROGRAM ***************
def main():

    # check the length of command running
    if (len(sys.argv) < 2):
        print "usage: proxyServer <port>"
        return sys.stdout

    # host and port info.
    # host = '192.168.1.133'               # blank for localhost
    # print get_ip_address('wlan0')
    host = sys.argv[1]
    port = int(sys.argv[2]) # port from argument
    # m = threading.Thread(target=monitorActivity, args=())
    # m.start()
    try:
        os.remove("bwsavings.txt")
    except OSError:
        pass

    try:
        # create a socket
        s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)

        # associate the socket to host and port
        s.bind((host, port))

        # listenning
        s.listen(BACKLOG)

    except socket.error, (value, message):
        if s:
            s.close()
        print "Could not open socket:", message
        sys.exit(1)

    # get the connection from client
    while 1:
        conn, client_addr = s.accept()

        # create a thread to handle request

        t = threading.Thread(target=proxy_thread, args=(conn, client_addr,))
        t.start()


if __name__ == '__main__':
    main()
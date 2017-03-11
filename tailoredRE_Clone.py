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
from lepl.apps.rfc3696 import HttpUrl

table = {}
chunklist = []
temptable = {}
tempchunklist = []
hashList =[]
temphashList =[]
activityTable = {}

#********* CONSTANTS *********
BACKLOG = 50            # how many pending connections queue will hold
MAX_DATA_RECV = 100000    # max number of bytes we receive at once
DEBUG = True           # set to True to see the debug msgs
CACHE_SIZE = 2000000
CURRENT_CACHE_SIZE = 0
RE_TAILOR = "www.bloomberg.com"
RE_TAILOR_NOT = ["www.youtube.com","www.cnn.com","www.quora.com","www.nytimes.com","www.bloomberg.com","www.twitter.com","www.techcrunch.com", "www.instagram.com"]
RE_TAILOR_NOT = ["www.youtube.com", "www.facebook.com","www.twitter.com"]

TOTAL_RESPONSE_SIZE = 0
TAILORED_RE_ON = True
m = 0.0
TBS_EI = []
Interval = 3
timelist = []
byteslist = []
total_response_bytes = []
RE = True
reqs = 1
int_resp_list = []
int_encoresp_list = []
averageBWS = []

class AppActivity:
    def __init__(self):
        self.name = ""
        self.cumulSize = 0
        self.redunSize = 0
        self.redunRatio = 0.0
        self.bwsaving = 0
        self.activityfactor = 0
        self.valuefactor = 0

class Chunk:
    def __init__(self):
        self.hash = ""
        self.chunk = ""

    def getHash(self):
        return self.hash


def ubp_activity_update(request,dataSize):
    print "in ubp activity update"
    parsed_uri = urlparse.urlparse(request)
    domain = '{uri.netloc}'.format(uri=parsed_uri)
    print domain
    val = activityTable.get(domain)
    if val is None:
        temp = AppActivity()
        temp.cumulSize = dataSize
        temp.name = domain
        temp.activityfactor = dataSize/TOTAL_RESPONSE_SIZE
        activityTable[domain] = temp
    else:
        val.cumulSize += dataSize
        val.name = domain
        val.activityfactor = val.cumulSize/TOTAL_RESPONSE_SIZE
        activityTable[domain] = val


def ubp_redundancy_update(request,redunSize):
    print "in ubp redu update"
    global RE_TAILOR
    global m
    parsed_uri = urlparse.urlparse(request)
    domain = '{uri.netloc}'.format(uri=parsed_uri)
    val = activityTable.get(domain)
    if val is not None:
        val.redunSize += redunSize
        print val.redunSize, val.cumulSize
        val.redunRatio = float(val.redunSize)/float(val.cumulSize)
        print val.redunRatio
        val.bwsaving = val.cumulSize * val.redunRatio
        val.valuefactor = val.redunRatio * val.activityfactor
        activityTable[domain] = val
        print "domain: ", domain, "Current Value Factor ", val.valuefactor

    #update the RE_TAILOR based on the value
    for key, value in activityTable.iteritems():
        if value.valuefactor > m:
            m = value.valuefactor
            RE_TAILOR = key
    print "Current Chosen App or Domain", RE_TAILOR


def tailoring(data):
    if TAILORED_RE_ON:
        parsed_uri = urlparse.urlparse(data)
        domain = '{uri.netloc}'.format(uri=parsed_uri)
        # if domain == RE_TAILOR:
        if domain not in RE_TAILOR_NOT:
            return True
        else:
            return False
    return True


def maxpFingerprinting(data):
    global CURRENT_CACHE_SIZE
    print "In MAXP"
    cachedMarkers = []
    hitMarkers = []
    newOffsets = []
    oldOffsets = []
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
        if ch.hash not in table.keys():
            if CURRENT_CACHE_SIZE > CACHE_SIZE:
                evict_chunks_from_cache(ch)
            cache_chunk(ch)
            cachedMarkers.append((previousMarker,nextMarker))
            totalChunksCached += 1
            totcahedsize += len(ch.chunk)
            CURRENT_CACHE_SIZE += len(ch.chunk)
        else:
            hitCount += 1
            hitMarkers.append((previousMarker,nextMarker))
            oldOffsets.append(hashList.index(ch.hash))
            redusize += len(ch.chunk)
        nextMarker += 1
    # print "Hit Markers" , hitMarkers
    # print "Cache Markers" , cachedMarkers
    # sizeofchlist(tempchunklist)
    print "Current Cache Size", CURRENT_CACHE_SIZE
    # print "Hit Count", hitCount
    # print "Chunks Cached for current trnsaction", totalChunksCached
    # print "No of bytes of chunks that are cached for current trnsaction", totcahedsize
    # print "No of bytes of chunks that are redundant (already in the cache) for current trnsaction", redusize
    # print "Redundancy hit ratio for current trnsaction", float(redusize)/float(len(data))

    # Copy the temp value to global and Clearing all the temp values
    for elem in tempchunklist:
        chunklist.append(elem)

    for elem in temphashList:
        hashList.append(elem)
        newOffsets.append(len(hashList))

    merge_two_dicts(temptable)
    tempchunklist[:] = []
    temphashList[:] = []
    temptable.clear()

    return cachedMarkers, hitMarkers, newOffsets, oldOffsets, redusize


def sizeofchlist(chlist):
    totBytes = 0
    for i in chlist:
        # print len(i.chunk)
        totBytes += len(i.chunk)
    print "Length" ,len(chlist)
    return totBytes



def merge_two_dicts(x):
    '''Given two dicts, merge them into a new dict as a shallow copy.'''
    table.update(x)



def cache_chunk(ch):
    temptable[ch.hash] = ch.chunk
    tempchunklist.append(ch)
    temphashList.append(ch.hash)

def evict_chunks_from_cache(ch):
    global CURRENT_CACHE_SIZE
    print "In Cache eviction"
    for x in range(0,len(chunklist)/3,1):
        removedChunk = chunklist.pop(0)
        table.pop(removedChunk.hash,None)
        CURRENT_CACHE_SIZE -= len(removedChunk.chunk)
    # while (sys.getsizeof(chunklist) + sys.getsizeof(ch)) <= CACHE_SIZE:
    #     removedChunk = chunklist.pop(0)
    #     table.pop(removedChunk.hash,None)
    return None




def encode(cachedMarkers, hitMarkers, newOffsets, oldOffsets, response):
    i = 0
    j = 0

    # print len(cachedMarkers)
    # print len(newOffsets)
    encodedresponse = ''
    print "response length before encoding", len(response), len(hitMarkers), len(cachedMarkers)
    # print "Hit Markers are", hitMarkers
    # print "Cached Markers are", cachedMarkers
    l = 0
    k = 0
    combinedMarkersUnsorted = hitMarkers + cachedMarkers
    combinedMarkers = sorted(combinedMarkersUnsorted, key=lambda tup: tup[0])
    # print "Combined Markers are", combinedMarkers

    if len(hitMarkers) > 0 and len(cachedMarkers) > 0:
        for x in combinedMarkers:
            if x in hitMarkers:
                justoffs = '!' + str(oldOffsets[i]) + '!'
                encodedresponse += justoffs
                i += 1
            else:
                chunkoffs = "?" + str(newOffsets[j]) + "?" + str(len(response[x[0]:x[1]+1])) + "?" + response[x[0]:x[1]+1] + "?"
                encodedresponse += chunkoffs
                j += 1

    elif len(hitMarkers) is 0 and len(cachedMarkers) > 0:
        for x in cachedMarkers:
            chunkhash = "?" + str(newOffsets[j]) + "?" + str(len(response[x[0]:x[1]+1])) + "?" + response[x[0]:x[1]+1] + "?"
            encodedresponse += chunkhash
            j += 1
    elif len(hitMarkers) > 0 and len(cachedMarkers) is 0:
        for x in hitMarkers:
            justoffs = '!' + str(oldOffsets[i]) + '!'
            encodedresponse += justoffs
            i += 1

    encodedresponse += "\nend\n"
    print "response length after encoding", len(encodedresponse)
    return encodedresponse

# For testing purposes I have built this decoding module in the server

def decode(response):
    decodeResponse = ''
    hitcount = 0
    retrievedChunkcount = 0
    print "response length before decoding", len(response)
    x = 0
    while x < len(response):
        if response[x] is '!' and x+21 < len(response):
            if response[x+21] is '!':
                # print "hash found"
                hitcount += 1
                hashVl = response[x+1:x+21]
                retrivedchunk = table.get(hashVl.encode("hex"))
                if (retrivedchunk is not None):
                    retrievedChunkcount += 1
                    decodeResponse += retrivedchunk
                x += 21

            else:
                x += 1
        else:
            decodeResponse += response[x]
            x += 1
        # print len(decodeResponse)
    print "response length after decoding", len(decodeResponse), hitcount, retrievedChunkcount

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
    global reqs
    try:
        print >>sys.stderr, 'connection from', client_addr

        # Receive the data in small chunks and retransmit it
        while True:
            data = conn.recv(MAX_DATA_RECV)
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

            print "Length of the response", len(response)
            TOTAL_RESPONSE_SIZE += len(response)
            ubp_activity_update(data,len(response))
            # cachedMarkers, hitMarkers, newHashes, oldHashes, red_ratio = maxpFingerprinting(response)
            # ubp_redundancy_update(data,red_ratio)
            if (RE):
                if tailoring(data):
                    print "Performing Redundancy Elimination for this request" , data
                    cachedMarkers, hitMarkers, newHashes, oldHashes, red_ratio = maxpFingerprinting(response)
                    ubp_redundancy_update(data,red_ratio)
                    encodedresponse = encode(cachedMarkers, hitMarkers, newHashes, oldHashes, response)
                else:
                    print "Not Performing Redundancy Elimination for this request" , data
                    encodedresponse = response
                # encodedresponse = "<html>\r\n<body><b>TRE Demo</b></body>\r</html>\r\n"
                print "Length of the Encoded response", len(encodedresponse)
                encodedresponse = encodedresponse.replace('\r','')
                # encodedresponse = response
                print "Length of the Encoded response", len(encodedresponse)
            else:
                encodedresponse = response
            dt = datetime.now()
            a = dt.microsecond
            if encodedresponse:
                print >>sys.stderr, 'sending response back to the client'
                conn.sendall(encodedresponse)
            else:
                print >>sys.stderr, 'no more data from', client_addr
                break

            dt = datetime.now()
            b = dt.microsecond
            timetaken = b-a
            bytes_conserved = (float(len(response)) - float(len(encodedresponse)))
            int_resp_list.append(len(response))
            int_encoresp_list.append(len(encodedresponse))
            timelist.append(timetaken)
            byteslist.append(bytes_conserved)
            total_response_bytes.append(len(encodedresponse))
            t = sum(timelist)
            by = sum(total_response_bytes)
            with open("bwsavings.txt", "a") as myfile:
                # myfile.write( datetime.now().strftime('%Y-%m-%d %H:%M:%S') + "\t\tRequest " + data + "\t\tOriginal response size " + str(len(response)) + "\t\tEncode response size " + str(len(encodedresponse)) + "\t\tBytes Saved from transmitting " + str(len(response)-len(encodedresponse)) + "\n" )
                if reqs % 20 is 0:
                    # myfile.write("Bandwidth Savings" + str(float(sum(byteslist)) * 100 /float(sum(int_resp_list))) + " Bandwidth Gain" + str((float(sum(int_resp_list))/float(sum(int_encoresp_list)))) + "\n")
                    myfile.write(str(round(float(sum(byteslist)) * 100 /float(sum(int_resp_list)),2)) + "\n")
                    averageBWS.append(float(sum(byteslist)) * 100 /float(sum(int_resp_list)))
                    byteslist[:] = []
                    int_resp_list[:] = []
            myfile.close()
            reqs += 1
    finally:
        # Clean up the connection
        conn.close()


def monitorActivity():
    global RE_TAILOR
    global m
    while(True):
        print "Monitoring"
        time.sleep(20)
        for key in activityTable.keys():
            print key
        for key, value in activityTable.iteritems():
            if value.valuefactor > m:
                m = value.valuefactor
                RE_TAILOR = key
        print "Chosen App", RE_TAILOR



def handler(signum, frame):
    print 'Interrupt Capture'


def testmodule():
    data = "http://media.clemson.edu/ia/services/IS140_Request_for_Post-Completion_OPT.pdf"
    # data = "http://www.relacweb.org/conferencia/images/documentos/Hoteles_cerca.pdf"
    data = "http://www.comma.ai"

    try:
        ret = urllib2.urlopen(data)
        if ret.code == 200:
            print "Exists!"
        response = urllib2.urlopen(data).read()
    except urllib2.HTTPError, urllib2.URLError:
        print "HTTP or URL error"
        sys.exit(1)
    print "Length of response", len(response)
    cachedMarkers, hitMarkers, newHashes, oldHashes = maxpFingerprinting(response)
    encodedresponse = encode(cachedMarkers, hitMarkers, newHashes, oldHashes, response)
    print "Length of the Encoded response", len(encodedresponse)
    try:
        ret = urllib2.urlopen(data)
        if ret.code == 200:
            print "Exists!"
        response = urllib2.urlopen(data).read()
    except urllib2.HTTPError, urllib2.URLError:
        print "HTTP or URL error"
        sys.exit(1)
    print "Length of response", len(response)
    cachedMarkers, hitMarkers, newHashes, oldHashes = maxpFingerprinting(response)
    encodedresponse = encode(cachedMarkers, hitMarkers, newHashes, oldHashes, response)
    print "Length of the Encoded response", len(encodedresponse)
    # print response
    # print encodedresponse
    decode(encodedresponse)

# def main():
#     table.clear()
#     chunklist[:] = []
#     t = threading.Thread(target=testmodule, args=())
#     t.start()
    # testmodule()

#********* MAIN PROGRAM ***************
def main():

    signal.signal(signal.SIGINT, handler)
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
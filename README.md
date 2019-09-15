# Simple Key Value based DataStore in Java

# Functional Requirements:

1. It can be initialized using an optional file path. If one is not provided, it will reliably
create itself in a reasonable location on the laptop. 

## Done> Provided a way to set paths and DefaultTTL through JVM flags.

2. A new key-value pair can be added to the data store using the Create operation. The key
is always a string - capped at 32chars. The value is always a JSON object - capped at
16KB.

## Done> Assumed the input will be in that range.

3. If Create is invoked for an existing key, an appropriate error must be returned.

## Done> Will get error message if key already exists in DataStore.

4. A Read operation on a key can be performed by providing the key, and receiving the
value in response, as a JSON object.

## Done> Done the feature.

5. A Delete operation can be performed by providing the key.

## Done> Done the feature

6. Every key supports setting a Time-To-Live property when it is created. This property is
optional. If provided, it will be evaluated as an integer defining the number of seconds
the key must be retained in the data store. Once the Time-To-Live for a key has expired,
the key will no longer be available for Read operations.

## Done> I have TTL information of all keys, and if TTL expired, client won't be able to read anymore. That value will be removed from DataStore.

7. Appropriate error responses must always be returned to a client if it uses the data store in
unexpected ways or breaches any limits.

## Done> Mostly I have checked for corner cases


# Non-Functional requirements.

1. The size of the file storing data must never exceed 1GB.

## Done> Checking file size before writing into file and throws proper error response

2. More than one client process cannot be allowed to use the same file as a data store at any
given time.

## Done> One JVM process only can access the file at a time

3. A client process is allowed to access the data store using multiple threads, if it desires to.
The data store must therefore be thread-safe.

## Done> Using BufferedReader which is Thread-safe, ConcurrentHashMap which is Thread-safe and fail-safe since it creates new copy if another thread modifies the map.

4. The client will bear as little memory costs as possible to use this data store, while
deriving maximum performance with respect to response times for accessing the data
store.

## Done> Using In-Memory Datastructures for verify key exists or not and checking TTL before doing read/delete instead of doing Disk I/O calls everytime, since it can be 100X slower

# IMPORTANT NOTES

1) Security : Value has been encoded as Base64 string instead of storing it as Plaintext. Also it is useful to store complex JSOn with many newlines as single line. So it can be efficiently retrieved from file
2) Memory: Not loading entire file into memory since it can be huge(upto 1GB) and will take up lots of memory. So loading one line into memory at a time and processing it.
3) Performance: Using BufferedReader and BufferedWriter so that it will use buffer(8 KB) and read/write in batch instead of reading/writing single character.
4) Performance: For faster retrieval, whenever our application starts, we will be reading from file and populate the ConcurrentHashMap. 2 Maps hold KeyValuePairs, KeyAndTTL info.
5) Adaptability: Provided a way to change default values through JVM flags 

# Guide on using the Application:

1) The Applications is based on REPL(Read, Eval, Print, Loop), an interactive commandline interface.
2) It is a file-based key-value data store that supports the basic CRD (create, read, and delete)
operations. This data store is meant to be used as a local storage for one single process on one
laptop.
3) The key-value will be stored in (./data/db.txt) file along with metadata file which contains in a separate (./data/db-meta.txt) file.
4) If you want to modify default values, add the jvm flags while starting the application.
 i)  To set DB file to some other location: -Dorg.freshworks.datastore.dbfilepath=<YOUR_DB_FILEPATH>
 ii) To set DB metadata file to some other location: -Dorg.freshworks.datastore.dbfilepath=<YOUR_DB_META_FILEPATH>
 iii) To set default TTl time : <Time_in_millisecs> //By default it is 86400(i.e. 1 day)

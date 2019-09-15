package com.freshworks;

import java.io.IOException;
import java.io.Serializable;
import java.nio.charset.Charset;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class DataStore implements Serializable {

  private static final long serialVersionUID = 1L;
  private static final DataStore INSTANCE = new DataStore();
  private static String dbFilePath   = System.getProperty("org.freshworks.datastore.dbfilepath", "./data/db.txt");
  private static String metaFilePath = System.getProperty("org.freshworks.datastore.metafilepath", "./data/db-meta.txt");
  private static Integer defaultTTL = Integer.valueOf(System.getProperty("org.freshworks.datastore.defaultTTL", "86400"));//1 Day
  private static Map<String, String> keyValueMap = new ConcurrentHashMap<>(); // Making the class Thread-safe
  private static Map<String, String> metadataMap = new ConcurrentHashMap<>(); // Making the class Thread-safe
  private static Scanner scanner = new Scanner(System.in);
  private static final long ONE_SEC_IN_MILLIS = 1000;//1 sec = 1000 millisecs
  static String DELIMITER = ":";

  private DataStore() {
  }

  public static DataStore getInstance() {
    return DataStore.INSTANCE;
  }

  @SuppressWarnings("unused")
  private DataStore readResolve() {
    return DataStore.INSTANCE;
  }

  public String createKeyValue(String key, String value){
    StringBuilder stringBuilder = new StringBuilder();
    // Encode data using Base64
    String base64EncodedValue = Base64.getEncoder().encodeToString(value.getBytes());
    stringBuilder.append(key).append(DELIMITER).append(base64EncodedValue);
    return stringBuilder.toString();
  }

  public String getDecodedValue(String keyValue){
    // Decode data using Base64
    byte[] decodedBytes = Base64.getDecoder().decode(keyValue);
    String decodedStr = new String(decodedBytes, Charset.defaultCharset());
    return decodedStr;
  }

  private void createFile(String path){
    Path filePath = Paths.get(path);
    try {
      Files.createDirectories(filePath.getParent());
      Files.createFile(filePath);
    } catch (FileAlreadyExistsException fae) {
      //Don't want to throw exception if File already exists
    } catch(IOException e){
        e.printStackTrace();
    }
  }

  private String createKeyWithTTL(String key, Long dateInMilliSecs) {
    return key + DELIMITER + dateInMilliSecs.toString();
  }
  /*
  * When the process has been started, this will read the DB file and Metadata file and
  * load the keyValuePairs and its metadata in ConcurrentHashMap
  */
  private void preProcess(String dbFilePath, String metaFilePath) {
    this.createFile(dbFilePath);
    this.createFile(metaFilePath);
    this.keyValueMap = Utils.getAllLinesFromFile(dbFilePath);
    this.metadataMap = Utils.getAllLinesFromFile(metaFilePath);
  }

  public static void main(String[] args) {
    DataStore.INSTANCE.preProcess(dbFilePath, metaFilePath);
    //Followed REPL to have interactive flow.
    while(true) {
      try {
        System.out.println("\n<--------------------Welcome to Freshworks Key-Value DataStore-------------------->");
        System.out.println("Press 1 : Create Key-Value pair");
        System.out.println("Press 2 : Read Key-Value pair");
        System.out.println("Press 3 : Delete by Key");
        int option = scanner.nextInt();
        scanner.nextLine();//To take care of '\n'
        String key = null, value = null;
        Long dateInMilliSecs = 0L;
        Calendar calendar = Calendar.getInstance();
        switch (option) {
          case 1://Create
            System.out.print("Give the key : ");
            key = scanner.nextLine();
            if (key.equals("")) {
              System.err.println("FAILED: Key cannot be empty");
              continue;
            }
            if (keyValueMap.containsKey(key)) {
              System.err.println("FAILED: Key already exists in DataStore. Please change the key and try again");
              continue;
            }
            System.out.println("Give the value : ");
            value = scanner.nextLine();
            if (value.equals("")) {
              System.err.println("FAILED: Value cannot be empty");
              continue;
            }
            System.out.println("Give the TTL in seconds [Type 0 for default(1 day)] : ");
            int TTL = scanner.nextInt();
            if (TTL <= 0) {
              dateInMilliSecs = calendar.getTimeInMillis() + (defaultTTL * ONE_SEC_IN_MILLIS); //Default value is 1 Day
            } else {
              dateInMilliSecs = calendar.getTimeInMillis() + (TTL * ONE_SEC_IN_MILLIS);
            }
            String key_value = DataStore.INSTANCE.createKeyValue(key, value);
            boolean success = Utils.writeToFile(dbFilePath, key_value, true);
            if(success) {
              keyValueMap.put(key, value);
              success = Utils.writeToFile(metaFilePath, DataStore.INSTANCE.createKeyWithTTL(key, dateInMilliSecs), true);
              if (success)
                metadataMap.put(key, dateInMilliSecs.toString());
              if(success)
                System.out.println("SUCCESS: Key-Value pair has been added successfully to DataStore");
            }
            else{
              System.err.println("FAILED: Reasons could be File doesn't have write permission or File is too big\n");
            }
            break;
          case 2://Read
            System.out.print("Give the key : ");
            key = scanner.nextLine();
            if (key.equals("")) {
              System.err.println("FAILED: Key cannot be empty");
              continue;
            }
            if (!keyValueMap.containsKey(key)) {
              System.err.println("FAILED: Key does not exist in DataStore. Please change the key and try again");
              continue;
            }
            dateInMilliSecs = Long.valueOf(metadataMap.get(key));
            if (calendar.getTimeInMillis() > dateInMilliSecs) {
              Utils.deleteLineInFile(dbFilePath, key);
              keyValueMap.remove(key);
              Utils.deleteLineInFile(metaFilePath, key);
              metadataMap.remove(key);
              System.err.println("FAILED: Key expired and it has been removed from DataStore");
              continue;
            }
            String str = Utils.getLineFromFile(dbFilePath, key);
            int delimiterPos = str.indexOf(DELIMITER);
            value = str.substring(delimiterPos + 1);
            System.out.println(String.format("SUCCESS\nKey : %s, Value : %s", key, DataStore.INSTANCE.getDecodedValue(value)));
            break;
          case 3://Delete
            System.out.print("Give the key : ");
            key = scanner.nextLine();
            if (key.equals("")) {
              System.err.println("FAILED: Key cannot be empty");
              continue;
            }
            if (!keyValueMap.containsKey(key)) {
              System.err.println("FAILED: Key does not exist in DataStore. Please try again with different key");
              continue;
            }
            Utils.deleteLineInFile(dbFilePath, key);
            keyValueMap.remove(key);
            Utils.deleteLineInFile(metaFilePath, key);
            metadataMap.remove(key);
            //Even if key has been expired we will anyway delete that unwanted entry to save space
            System.out.println("SUCCESS: Key-Value pair has been deleted successfully from DataStore");
            break;
          default:
            System.err.println("FAILED: Please give the correct option");
            break;
        }
      } catch(Exception e){
        continue;
      }
    }
  }
}

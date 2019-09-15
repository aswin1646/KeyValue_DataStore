package com.freshworks;

import java.io.*;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import static com.freshworks.DataStore.DELIMITER;

public class Utils {

  public static String getLineFromFile(String filePath, String key) {

    //BufferedReader is synchronized, so read operations on a BufferedReader can safely be done from multiple threads
    try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(new File(filePath))))) {
      String line;
      while ((line = br.readLine()) != null) {
        if (line.startsWith(key)) {
          return line;
        }
      }
    } catch (IOException e) {
      e.printStackTrace();

    }
    return "";
  }

  public static Map<String, String> getAllLinesFromFile(String filePath) {

    Map<String, String> keyValueMap = new ConcurrentHashMap<>();
    //BufferedReader is synchronized, so read operations on a BufferedReader can safely be done from multiple threads
    try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(new File(filePath))))) {
      String line;
      int pos = 0;
      while ((line = br.readLine()) != null) {
        pos = line.indexOf(DELIMITER);
        keyValueMap.put(line.substring(0, pos), line.substring(pos + 1));
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
    return keyValueMap;

  }

  public static boolean writeToFile(String filePath, Object value, boolean append) {
    File file = new File(filePath);
    if(!file.canWrite() || (file.length() + (16 * 1024)) > 1073741824L) //if File size + current data(16KB) exceeds 1 GB or File is not writable error
    {
      return false;
    }
    try (PrintWriter pr = new PrintWriter(new BufferedWriter(new FileWriter(file, append)))) {
      pr.println(value);
    } catch (IOException e) {
      e.printStackTrace();
      return false;
    }
    return true;
  }

  public static void deleteLineInFile(String filePath, String key) {

    try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(new File(filePath))));
         PrintWriter pr = new PrintWriter(new BufferedWriter(new FileWriter(new File(filePath))))) {
      String line;
      while ((line = br.readLine()) != null) {
        if (line.startsWith(key)) {
          continue;
        }
        pr.println(line);
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

}

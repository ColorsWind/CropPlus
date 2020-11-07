package com.github.schooluniform.cropplus.data.manager;

import com.github.schooluniform.cropplus.data.CropData;
import com.github.schooluniform.cropplus.data.Timer;
import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Location;
import org.bukkit.configuration.file.YamlConfiguration;

public class CropManager implements Runnable {
  private int time;

  static ConcurrentHashMap<String, CropData> crops = new ConcurrentHashMap<>();
  static ConcurrentHashMap<String, Timer> timer = new ConcurrentHashMap<>();

  /**
   * 初始化.
   * @param path 文件路径
   */
  public static void init(File path) {
    crops.clear();
    loadRecipes(crops, path, "");
    File file = new File(Manager.getDataFolder() + "/timer.yml");
    YamlConfiguration timerData = YamlConfiguration.loadConfiguration(file);
    for (String key : timerData.getKeys(false)) {
      Timer timer = new Timer(timerData.getString(key + ".name"), 
          timerData.getDouble(key + ".time"),
          timerData.getInt(key + "multiple"), 
          timerData.getString(key + ".world"), 
          timerData.getInt(key + ".x"),
          timerData.getInt(key + ".y"), 
          timerData.getInt(key + ".z"), 
          timerData.getInt(key + ".no-water"),
          timerData.getBoolean(key + ".no-death"));
      CropManager.timer.put(key, timer);
    }
  }

  @Override
  public void run() {
    try {
      if (time >= 60) {
        File file = new File(Manager.getDataFolder() + "/timer.yml");
        file.delete();
        YamlConfiguration timerData = YamlConfiguration.loadConfiguration(file);
        timer.forEach(timer.mappingCount(), (k, v) -> {
          v.up();
          timerData.set(k + ".name", v.getName());
          timerData.set(k + ".time", v.getTime());
          timerData.set(k + ".multiple", v.getMultiple());
          timerData.set(k + ".world", v.getWorld());
          timerData.set(k + ".x", v.getX());
          timerData.set(k + ".y", v.getY());
          timerData.set(k + ".z", v.getZ());
          timerData.set(k + ".no-death", v.isNoDeath());
          timerData.set(k + ".no-water", v.getNowater());
        });

        try {
          timerData.save(file);
        } catch (IOException e) {
          e.printStackTrace();
        }

        time = 0;
      } else {
        timer.forEach(timer.mappingCount(), (k, v) -> {
          v.up();
        });
      }
      time++;
    } catch (Exception e) {
      e.printStackTrace();
    }

  }

  /**
   * 保存.
   */
  public static void save() {
    File file = new File(Manager.getDataFolder() + "/timer.yml");
    file.delete();
    YamlConfiguration timerData = YamlConfiguration.loadConfiguration(file);

    for (Map.Entry<String, Timer> entry : timer.entrySet()) {
      timerData.set(entry.getKey() + ".name", entry.getValue().getName());
      timerData.set(entry.getKey() + ".time", entry.getValue().getTime());
      timerData.set(entry.getKey() + ".multiple", entry.getValue().getMultiple());
      timerData.set(entry.getKey() + ".world", entry.getValue().getWorld());
      timerData.set(entry.getKey() + ".x", entry.getValue().getX());
      timerData.set(entry.getKey() + ".y", entry.getValue().getY());
      timerData.set(entry.getKey() + ".z", entry.getValue().getZ());
      timerData.set(entry.getKey() + ".no-death", entry.getValue().isNoDeath());
      timerData.set(entry.getKey() + ".no-water", entry.getValue().getNowater());
    }

    try {
      timerData.save(file);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public static CropData getCropData(String name) {
    return crops.get(name);
  }

  public static Timer getTimer(String id) {
    return timer.get(id);
  }

  public static boolean containsTimer(String id) {
    return timer.containsKey(id);
  }

  public static void removeTimer(String id) {
    timer.remove(id);
  }

  /**
   * 注册计时器.
   * @param timer 计时器
   */
  public static void registerTimer(Timer timer) {
    if (CropManager.timer.containsKey(getID(timer.getLocation()))) {
      return;
    }
    CropManager.timer.put(getID(timer.getLocation()), timer);
  }

  public static String getID(Location l) {
    return l.getWorld().getName() + l.getBlockX() + l.getBlockY() + l.getBlockZ();
  }

  private static void loadRecipes(
      ConcurrentHashMap<String, CropData> recipe, File path, String recipeName) {
    for (File sf : path.listFiles()) {
      if (sf.isFile()) {
        String fileName = sf.getName();
        if (!fileName.substring(fileName.lastIndexOf(".")).equalsIgnoreCase(".yml")) {
          continue;          
        }
        recipe.put(recipeName + fileName.substring(0, fileName.lastIndexOf(".")),
            CropData.load(recipeName + fileName.substring(0, fileName.lastIndexOf("."))));
      } else {
        loadRecipes(recipe, sf, recipeName + sf.getName() + "/");        
      }
    }
  }
}

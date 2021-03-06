package com.github.schooluniform.cropplus.data;

import com.github.schooluniform.cropplus.CropPlus;
import com.github.schooluniform.cropplus.api.event.natural.CPCropDeathEvent;
import com.github.schooluniform.cropplus.api.event.natural.CropDeathEnum;
import com.github.schooluniform.cropplus.data.manager.CropManager;
import com.github.schooluniform.cropplus.data.manager.Manager;
import miskyle.realsurvival.api.Season;
import miskyle.realsurvival.randomday.RandomDayManager;
import org.bukkit.Bukkit;
import org.bukkit.CropState;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.material.Crops;

@SuppressWarnings("deprecation")
public class Timer {

  private String name;
  private double time;
  private String world;
  private int x;
  private int y;
  private int z;
  private int multiple;
  private int nowater = 0;
  private boolean noDeath;

  /**
   * 初始化Timer.
   * @param name 名字
   * @param time 时间
   * @param multiple 次数
   * @param world 世界
   * @param x x
   * @param y y
   * @param z z
   * @param nowater 是否缺水
   * @param noDeath 是否死亡
   */
  public Timer(String name,
      double time, int multiple, String world, int x, int y, int z, int nowater,
      boolean noDeath) {
    super();
    this.name = name;
    this.time = time;
    this.multiple = multiple;
    this.world = world;
    this.x = x;
    this.y = y;
    this.z = z;
    this.noDeath = noDeath;
    this.nowater = nowater;
  }

  /**
   * 初始化Timer.
   * @param name 名字
   * @param time 时间
   * @param multiple 成熟次数
   * @param location 位置
   */
  public Timer(String name, double time, int multiple, Location location) {
    super();
    this.name = name;
    this.time = time;
    this.multiple = multiple;
    this.world = location.getWorld().getName();
    this.x = location.getBlockX();
    this.y = location.getBlockY();
    this.z = location.getBlockZ();
    noDeath = false;
  }

  public Location getLocation() {
    return new Location(Bukkit.getWorld(world), x, y, z);
  }

  private double getFix(double num) {
    if (num > 0) {
      num = 0 - num;      
    }
    return num / 16D;
  }

  /**
   * 生长.
   */
  public void up() {
    BlockState crop = getLocation().getBlock().getState();
    Season season = Manager.rsAPI == null 
        ?  null : RandomDayManager.getWorldSeason(getLocation().getWorld().getName());
    if (!(crop.getData() instanceof Crops)) {
      CropManager.removeTimer(CropManager.getID(getLocation()));
      return;
    }
    CropData data = CropManager.getCropData(name);

    // Check Season
    if (!data.inSeason(season) && !noDeath) {
      if (Math.random() * 100 < data.getSeasonDeathChance()) {
        callDeathEvent(data.getId(), CropDeathEnum.NotAdaptingToTheSeason);
        return;
      } else {
        noDeath = true;
      }
    } else if (data.inSeason(season) && noDeath) {
      noDeath = false;
    }

    // Check Water
    if (getLocation().getBlock().getRelative(BlockFace.DOWN)
        .getState().getData().toItemStack().getDurability() != 7) {
      nowater++;
      if (nowater >= Manager.noWater) {
        callDeathEvent(data.getId(), CropDeathEnum.Dehydration);
      }
      return;
    }

    double temp = data.getGrowthRate(season) + getFix(crop.getLightLevel() - data.getLight());
    time += temp > 0 ? temp : 0;

    double pass = data.getNeedTime() / 8D;
    Crops c = (Crops) crop.getData();
    if (time >= data.getNeedTime()) {
      c.setState(CropState.RIPE);      
    } else if (time >= pass * 7) {
      c.setState(CropState.VERY_TALL);
    } else if (time >= pass * 6) {
      c.setState(CropState.TALL);      
    } else if (time >= pass * 5) {
      c.setState(CropState.MEDIUM);
    } else if (time >= pass * 4) {
      c.setState(CropState.SMALL);
    } else if (time >= pass * 3) {
      c.setState(CropState.VERY_SMALL);
    } else if (time >= pass * 2) {
      c.setState(CropState.GERMINATED);
    } else {
      c.setState(CropState.SEEDED);
    }
    CropPlus.plugin.getServer().getScheduler().runTask(CropPlus.plugin, () -> {
      crop.setData(c);
      crop.update(true);      
    });
    if (time > data.getNeedTime()) {
      time = data.getNeedTime();
    }
  }

  public String getName() {
    return name;
  }

  public String getWorld() {
    return world;
  }

  public double getTime() {
    return time;
  }

  /**
   * 设定时间.
   * @param time 时间
   */
  public void setTime(double time) {
    this.time = time;
    multiple--;
    if (multiple == 0) {
      getLocation().getBlock().setType(Material.AIR);
      CropManager.removeTimer(CropManager.getID(getLocation()));
    } else if (multiple < -1) {
      multiple = -1;      
    }
  }
  
  private void callDeathEvent(String cropId, CropDeathEnum type) {
    CropPlus.plugin.getServer().getScheduler().runTask(CropPlus.plugin, () -> {
      CPCropDeathEvent event = 
          new CPCropDeathEvent(cropId, getLocation(), type);
      Bukkit.getPluginManager().callEvent(event);
      if (!event.isCancelled()) {
        CropManager.removeTimer(CropManager.getID(getLocation()));
        getLocation().getBlock().setType(Material.DEAD_BUSH);
      }      
    });
  }

  public void changeTime(double time) {
    this.time += time;
  }

  public int getMultiple() {
    return multiple;
  }

  public int getX() {
    return x;
  }

  public int getY() {
    return y;
  }

  public int getZ() {
    return z;
  }

  public int getNowater() {
    return nowater;
  }

  public boolean isNoDeath() {
    return noDeath;
  }

}

package net.xsapi.panat.xscasinoredis.xscore;

import net.xsapi.panat.xscasinoredis.configuration.config;
import net.xsapi.panat.xscasinoredis.models.XSLottery;
import org.bukkit.Bukkit;
import java.time.LocalTime;

public class XSHandlers {

    private static String timer;
    private static long timerMillis;
    private static XSLottery xsLottery;

    public static void setUpConfig() {
        timer = config.customConfig.getString("config.prizeTime");
        timerMillis = config.customConfig.getLong("config.prizeMillis");


        xsLottery = new XSLottery();
    }

    public static XSLottery getXsLottery() {
        return xsLottery;
    }

    public static void saveData() {
       // config.customConfig.set("config.prizeMillis", timerMillis);
        config.customConfig.set("config.lockPrize", xsLottery.getLockPrize());
        config.customConfig.set("config.lockPrizeSetter", xsLottery.getSetterLockPrize());
        config.save();
        config.reload();
        Bukkit.getConsoleSender().sendMessage("§x§E§7§F§F§0§0[XSCasinoRedis] Data : §x§6§0§F§F§0§0Saved");
    }

    public static void createTask() {
        Bukkit.getScheduler().scheduleSyncRepeatingTask(core.getPlugin(), new Runnable() {
            @Override
            public void run() {

                LocalTime currentTime = LocalTime.now();

                LocalTime timerLocalTime = LocalTime.parse(timer);

                if (currentTime.isAfter(timerLocalTime) && System.currentTimeMillis() - timerMillis >= 0L) {
                    //Bukkit.broadcastMessage("SEND NOW! " + timer + " " + currentTime + "<<");
                    timerMillis = System.currentTimeMillis() + (86400L * 1000L);
                    getXsLottery().xsLotteryEndEvent();
                }
            }
        }, 0L, 20L);
    }

}

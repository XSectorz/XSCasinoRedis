package net.xsapi.panat.xscasinoredis.models;

import com.google.gson.Gson;
import net.xsapi.panat.xscasinoredis.configuration.config;
import net.xsapi.panat.xscasinoredis.xscore.core;
import org.bukkit.Bukkit;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class XSLottery {

    private int lockPrize = -1;
    private String setterLockPrize = "";
    private HashMap<String,Integer> dataLottery = new HashMap<>();

    public XSLottery() {
        setLockPrize(config.customConfig.getInt("config.lockPrize"));
        setSetterLockPrize(config.customConfig.getString("config.lockPrizeSetter"));
    }

    public HashMap<String, Integer> getDataLottery() {
        return dataLottery;
    }

    public String getSetterLockPrize() {
        return setterLockPrize;
    }

    public void setSetterLockPrize(String setterLockPrize) {
        this.setterLockPrize = setterLockPrize;
    }

    public int getLockPrize() {
        return lockPrize;
    }

    public void setLockPrize(int lockPrize) {
        this.lockPrize = lockPrize;
    }

    public void xsLotteryEndEvent() {
        int prizeNum;

        Bukkit.broadcastMessage("LockPrize: " + getLockPrize());
        if(getLockPrize() == -1) {
            prizeNum = generatePrizeNumber();
        } else {
            prizeNum = getLockPrize();
        }

        core.sendMessageToRedisAsync("XSCasinoRedisData/XSLottery/EndPrizeNumber/"+core.getRedisCrossServerHostName(),"" + prizeNum);

        Bukkit.broadcastMessage("Waiting system calculate data...");
        Bukkit.getScheduler().scheduleSyncDelayedTask(core.getPlugin(), new Runnable() {
            @Override
            public void run() {
                calculateWinner();
            }
        }, 100L);
    }

    public void calculateWinner() {
        Bukkit.broadcastMessage("Send... Data Winner");

        Gson gson = new Gson();
        String json = gson.toJson(dataLottery);

        core.sendMessageToRedisAsync("XSCasinoRedisData/XSLottery/EndSendWinnerList/"+core.getRedisCrossServerHostName(),json);
    }

    public int generatePrizeNumber() {
        Random r = new Random();
        int rand = r.nextInt(99);
        return rand;
    }
}

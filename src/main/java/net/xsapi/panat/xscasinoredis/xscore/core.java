package net.xsapi.panat.xscasinoredis.xscore;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.xsapi.panat.xscasinoredis.api.HashMapDeserializer;
import net.xsapi.panat.xscasinoredis.configuration.config;
import net.xsapi.panat.xscasinoredis.configuration.configLoader;
import net.xsapi.panat.xscasinoredis.models.XSLottery;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPubSub;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public final class core extends JavaPlugin {

    public static core plugin;
    public static String redisHost;
    public static String redisCrossServerHostName;
    public static int redisPort;
    public static String redisPass;

    public static core getPlugin() {
        return plugin;
    }
    public static ArrayList<Thread> threads = new ArrayList<>();

    public static String getRedisCrossServerHostName() {
        return redisCrossServerHostName;
    }

    public static String getRedisHost() {
        return redisHost;
    }

    public static String getRedisPass() {
        return redisPass;
    }

    public static int getRedisPort() {
        return redisPort;
    }

    @Override
    public void onEnable() {

        plugin = this;
        new configLoader();

        if(redisConnection()) {
            for(String server : config.customConfig.getStringList("cross-server.servers")) {
                subscribeToChannelAsync("XSCasinoRedisData/XSLottery/" + config.customConfig.getString("redis.host-server") + "/" + server);
                subscribeToChannelAsync("XSCasinoRedisData/XSLottery/Change/" + config.customConfig.getString("redis.host-server") + "/" + server);
                subscribeToChannelAsync("XSCasinoRedisData/XSLottery/WinnerList/" + config.customConfig.getString("redis.host-server") + "/" + server);
                subscribeToChannelAsync("XSCasinoRedisData/XSLottery/Requests/" + config.customConfig.getString("redis.host-server") + "/" + server);
                subscribeToChannelAsync("XSCasinoRedisData/XSLottery/CheckStatus/" + config.customConfig.getString("redis.host-server") + "/" + server);
                checkStatus(server,"true");
            }
        }

        XSHandlers.setupAPI();
        XSHandlers.setUpConfig();
        XSHandlers.createTask();

    }

    @Override
    public void onDisable() {
        for(String server : config.customConfig.getStringList("cross-server.servers")) {
            checkStatus(server,"false");
        }
        for(Thread thread : threads) {
            thread.interrupt();
        }
        XSHandlers.saveData();
    }

    private void subscribeToChannelAsync(String channelName) {

        Thread thread = new Thread(() -> {
            try (Jedis jedis = new Jedis(getRedisHost(), getRedisPort())) {
                if(!getRedisPass().isEmpty()) {
                    jedis.auth(getRedisPass());
                }
                JedisPubSub jedisPubSub = new JedisPubSub() {
                    @Override
                    public void onMessage(String channel, String message) {
                        if (Thread.currentThread().isInterrupted()) {
                            return;
                        }
                       // Bukkit.getConsoleSender().sendMessage("Received data (send from serverRedis)" + channel + " -->" + message);
                        for(String server : config.customConfig.getStringList("cross-server.servers")) {
                            if(channel.equalsIgnoreCase("XSCasinoRedisData/XSLottery/" + config.customConfig.getString("redis.host-server") + "/" + server)) {
                                convertToObject(message);
                            } else if(channel.equalsIgnoreCase("XSCasinoRedisData/XSLottery/Change/" + config.customConfig.getString("redis.host-server") + "/" + server)) {
                                /*
                                    @Change format {type}:{value}
                                    {type}
                                        - LockPrize
                                */
                                changeConvert(message);
                            } else if(channel.equalsIgnoreCase("XSCasinoRedisData/XSLottery/WinnerList/" + config.customConfig.getString("redis.host-server") + "/" + server)) {
                                winnerListConvert(message);
                            } else if(channel.equalsIgnoreCase("XSCasinoRedisData/XSLottery/Requests/" + config.customConfig.getString("redis.host-server") + "/" + server)) {
                                sendCurrentDataToRequests(message);
                            } else if(channel.equalsIgnoreCase("XSCasinoRedisData/XSLottery/CheckStatus/" + config.customConfig.getString("redis.host-server") + "/" + server)) {
                                checkStatus(message,"true");
                            }
                        }
                    }
                };
                jedis.subscribe(jedisPubSub, channelName);
            } catch (Exception e) {
                // จัดการข้อผิดพลาดที่เกิดขึ้นwd
                e.printStackTrace();
            }
        });
        thread.start();
        threads.add(thread);
    }

    public static void checkStatus(String message,String status) {
       // Bukkit.getConsoleSender().sendMessage("set status to " + message + " as " + status);
        sendMessageToRedisAsync("XSCasinoRedisData/XSLottery/ResponseStatus/"+getRedisCrossServerHostName()+"/"+message,status);
    }

    public static void sendCurrentDataToRequests(String message) {
        Gson gson = new Gson();
        String json = gson.toJson(XSHandlers.getXsLottery().getLotteryList());
        sendDataToXSCasinoClient("XSCasinoRedisData/XSLottery/RequestsReturn/"+getRedisCrossServerHostName()+"/"+message,json);

    }

    public static void winnerListConvert(String message) {
        Gson gson = new Gson();
        HashMap<String, Integer> resultMap = gson.fromJson(message, new TypeToken<HashMap<String, Integer>>(){}.getType());
        XSHandlers.getXsLottery().getDataLottery().putAll(resultMap);

        for(Map.Entry<String,Integer> user : resultMap.entrySet()) {
            if(XSHandlers.getXsLottery().getDataLottery().containsKey(user.getKey())) {
                if(XSHandlers.getXsLottery().getDataLottery().get(user.getKey())
                < user.getValue()) {
                    XSHandlers.getXsLottery().getDataLottery().replace(user.getKey(),user.getValue());
                }
            } else {
                XSHandlers.getXsLottery().getDataLottery().put(user.getKey(),user.getValue());
            }
        }

        //for(Map.Entry<String,Integer> winner : resultMap.entrySet()) {
         //   Bukkit.broadcastMessage(winner.getKey() + " ---> " + winner.getValue());
        //}
    }

    public static void changeConvert(String msg) {
        String type = msg.split(":")[0];

        if(type.equalsIgnoreCase("LockPrize")) {
            int prizeNum = Integer.parseInt(msg.split(":")[1]);
            String setterName = (msg.split(":")[2]);
            XSHandlers.getXsLottery().setLockPrize(prizeNum);
            XSHandlers.getXsLottery().setSetterLockPrize(setterName);
          //  Bukkit.getConsoleSender().sendMessage("Update LockPrize (send from redis server): " + setterName + " with " + prizeNum);
        }
    }

    public static void sendDataToXSCasinoClient(String CHName,String lotteryData) {
        sendMessageToRedisAsync(CHName,lotteryData);
        //Bukkit.broadcastMessage("XSCasinoRedis Send.... From " + CHName);
    }

    public static void sendMessageToRedisAsync(String CHName, String message) {

        new Thread(() -> {
            try (Jedis jedis = new Jedis(getRedisHost(), getRedisPort())) {
                if(!getRedisPass().isEmpty()) {
                    jedis.auth(getRedisPass());
                }
                jedis.publish(CHName, message);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void convertToObject(String data) {

        if (data.isEmpty()) {
            return;
        }

        int ticketNumber = Integer.parseInt(data.split(":")[0]);
        int ticketAmount = Integer.parseInt(data.split(":")[1]);

        if(XSHandlers.getXsLottery().getLotteryList().containsKey(ticketNumber)) {
            XSHandlers.getXsLottery().getLotteryList().replace(ticketNumber,
                    XSHandlers.getXsLottery().getLotteryList().get(ticketNumber)+ticketAmount);
        } else {
            XSHandlers.getXsLottery().getLotteryList().put(ticketNumber,ticketAmount);
        }
        XSHandlers.getXsLottery().addTicket(ticketAmount);

        sendDataToXSCasinoClient("XSCasinoRedisData/XSLottery/Update/"+getRedisCrossServerHostName(),ticketNumber + ":" + ticketAmount);

    }

    private boolean redisConnection() {
        redisHost = config.customConfig.getString("redis.host");
        redisPort = config.customConfig.getInt("redis.port");
        redisPass = config.customConfig.getString("redis.password");
        redisCrossServerHostName = config.customConfig.getString("redis.host-server");

        try {
            Jedis jedis = new Jedis(getRedisHost(), getRedisPort());
            if(!getRedisPass().isEmpty()) {
                jedis.auth(getRedisPass());
            }
            jedis.close();
            Bukkit.getConsoleSender().sendMessage("§x§E§7§F§F§0§0[XSCASINO REDIS] Redis Server : §x§6§0§F§F§0§0Connected");
            return true;
        } catch (Exception e) {
            Bukkit.getConsoleSender().sendMessage("§x§E§7§F§F§0§0[XSCASINO REDIS] Redis Server : §x§C§3§0§C§2§ANot Connected");
            e.printStackTrace();
        }
        return false;
    }


}

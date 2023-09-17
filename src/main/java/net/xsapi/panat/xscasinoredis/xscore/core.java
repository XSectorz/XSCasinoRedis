package net.xsapi.panat.xscasinoredis.xscore;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.xsapi.panat.xscasinoredis.api.HashMapDeserializer;
import net.xsapi.panat.xscasinoredis.configuration.config;
import net.xsapi.panat.xscasinoredis.configuration.configLoader;
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
    private final HashMap<Integer,Integer> dataLottery = new HashMap<>();

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
            }
        }
    }

    @Override
    public void onDisable() {
        for(Thread thread : threads) {
            thread.interrupt();
        }
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
                        Bukkit.getConsoleSender().sendMessage("XSCasinoRedis Received data from ---> " + channel);
                        for(String server : config.customConfig.getStringList("cross-server.servers")) {
                            if(channel.equalsIgnoreCase("XSCasinoRedisData/XSLottery/" + config.customConfig.getString("redis.host-server") + "/" + server)) {
                                Bukkit.getConsoleSender().sendMessage("XSCasinoRedis Received Data... -->" + message);
                                convertToObject(message);
                            }
                        }
                    }
                };
                jedis.subscribe(jedisPubSub, channelName);
            } catch (Exception e) {
                // จัดการข้อผิดพลาดที่เกิดขึ้น
                e.printStackTrace();
            }
        });
        thread.start();
        threads.add(thread);
    }

    public static void sendDataToXSCasinoClient(String CHName,HashMap<Integer,Integer> lotteryList) {
        Gson gson = new Gson();
        String jsonString = gson.toJson(lotteryList);
        sendMessageToRedisAsync(CHName,jsonString);
        Bukkit.broadcastMessage("XSCasinoRedis Send.... From " + CHName);
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

    private void convertToObject(String json) {

        if (json.isEmpty()) {
            return;
        }

        GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.registerTypeAdapter(new TypeToken<HashMap<Integer, Integer>>() {}.getType(), new HashMapDeserializer());
        Gson gson = gsonBuilder.create();

        HashMap<Integer, Integer> hashMap = gson.fromJson(json, new TypeToken<HashMap<Integer, Integer>>() {}.getType());

        for(Map.Entry<Integer,Integer> map : hashMap.entrySet()) {
            Bukkit.getConsoleSender().sendMessage(map.getKey() + " : " + map.getValue());

            if(dataLottery.containsKey(map.getKey())) {
                dataLottery.replace(map.getKey(), map.getValue()+dataLottery.get(map.getKey()));
            } else {
                dataLottery.put(map.getKey(),map.getValue());
            }
        }

        sendDataToXSCasinoClient("XSCasinoRedisData/XSLottery/Update/"+getRedisCrossServerHostName(),dataLottery);

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

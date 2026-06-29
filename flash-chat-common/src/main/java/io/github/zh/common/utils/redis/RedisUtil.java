package io.github.zh.common.utils.redis;

import jakarta.annotation.Resource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.concurrent.TimeUnit;

@Component
public class RedisUtil {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    public boolean keyIsExist(String key) {
        return stringRedisTemplate.hasKey(key);
    }

    public long ttl(String key) {
        return stringRedisTemplate.getExpire(key);
    }

    public void expire(String key, long timeout) {
        stringRedisTemplate.expire(key, timeout, TimeUnit.SECONDS);
    }

    public long increment(String key,long delta) {
        return stringRedisTemplate.opsForValue().increment(key, delta);
    }

    public long incrementHash(String name, String key, long delta) {
        return stringRedisTemplate.opsForHash().increment(name,key, delta);
    }

    public long decrementHash(String name, String key, long delta) {
        delta = delta*(-1);
        return stringRedisTemplate.opsForHash().increment(name,key, delta);
    }

    public void setHashValue(String name, String key, String value) {
        stringRedisTemplate.opsForHash().put(name, key, value);
    }

    public String getHashValue(String name, String key) {
        return (String) stringRedisTemplate.opsForHash().get(name, key);
    }
    public long decrement(String key,long delta) {
        return stringRedisTemplate.opsForValue().decrement(key, delta);
    }

    public Set<String> Keys(String pattern) {
        return stringRedisTemplate.keys(pattern);
    }

    public void del(String key){
        stringRedisTemplate.delete(key);
    }

    public void allDel(String key){
        Set<String> keys = stringRedisTemplate.keys(key+"*");
        stringRedisTemplate.delete(keys);
    }

    public void set(String key, String value, long timeout) {
        stringRedisTemplate.opsForValue().set(key, value, timeout, TimeUnit.SECONDS);
    }

    public void setByDays(String key, String value,long days) {
        stringRedisTemplate.opsForValue().set(key, value, days, TimeUnit.DAYS);
    }

    public void setnx60s(String key, String value) {
        stringRedisTemplate.opsForValue().setIfAbsent(key, value, 60, TimeUnit.SECONDS);
    }

    public Boolean setnx(String key, String value) {
        return stringRedisTemplate.opsForValue().setIfAbsent(key, value);
    }

    public Boolean setnx(String key, String value,Integer seconds) {
        return stringRedisTemplate.opsForValue().setIfAbsent(key, value, seconds, TimeUnit.SECONDS);
    }
    public String get(String key) {
        return stringRedisTemplate.opsForValue().get(key);
    }

    /**
     * 原子获取并删除 key，返回删除前的值
     */
    public String getAndDel(String key) {
        String script = "local v = redis.call('get', KEYS[1]); if v then redis.call('del', KEYS[1]); end; return v";
        Object result = stringRedisTemplate.execute(
                new org.springframework.data.redis.core.script.DefaultRedisScript<>(script, String.class),
                java.util.Collections.singletonList(key));
        return (String) result;
    }




}

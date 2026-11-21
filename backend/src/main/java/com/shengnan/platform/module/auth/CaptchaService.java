package com.shengnan.platform.module.auth;

import com.shengnan.platform.common.BusinessException;
import org.springframework.stereotype.Service;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.Base64;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import javax.imageio.ImageIO;

/** 图形验证码：内存缓存，5分钟过期，用后即删 */
@Service
public class CaptchaService {
    private final Map<String, Cache> store = new ConcurrentHashMap<>();
    private static final long TTL = 5 * 60_000L;

    public Map<String, Object> generate() {
        String id = java.util.UUID.randomUUID().toString();
        String code = randomCode();
        String image = draw(code);
        store.put(id, new Cache(code, System.currentTimeMillis()));
        Map<String, Object> result = new java.util.HashMap<>();
        result.put("captchaId", id);
        result.put("image", image);
        return result;
    }

    public void verify(String id, String code) {
        if (id == null || code == null) throw new BusinessException("验证码不能为空");
        Cache c = store.get(id);
        if (c == null || System.currentTimeMillis() - c.time > TTL) {
            store.remove(id);
            throw new BusinessException("验证码已过期");
        }
        if (!c.code.equalsIgnoreCase(code)) throw new BusinessException("验证码错误");
        store.remove(id);
    }

    private String randomCode() {
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
        Random r = new Random();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 4; i++) sb.append(chars.charAt(r.nextInt(chars.length())));
        return sb.toString();
    }

    private String draw(String code) {
        int w = 120, h = 40;
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, w, h);
        g.setFont(new Font("Arial", Font.BOLD, 24));
        Random r = new Random();
        char[] cs = code.toCharArray();
        for (int i = 0; i < cs.length; i++) {
            g.setColor(new Color(r.nextInt(120), r.nextInt(120), r.nextInt(120)));
            g.drawString(String.valueOf(cs[i]), 18 + i * 26, 28 + r.nextInt(5));
        }
        for (int i = 0; i < 30; i++) {
            g.setColor(new Color(r.nextInt(255), r.nextInt(255), r.nextInt(255)));
            g.drawLine(r.nextInt(w), r.nextInt(h), r.nextInt(w), r.nextInt(h));
        }
        g.dispose();
        try {
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            ImageIO.write(img, "png", os);
            return "data:image/png;base64," + Base64.getEncoder().encodeToString(os.toByteArray());
        } catch (Exception e) {
            throw new BusinessException("验证码生成失败");
        }
    }

    private static class Cache {
        String code;
        long time;
        Cache(String code, long time) { this.code = code; this.time = time; }
    }
}

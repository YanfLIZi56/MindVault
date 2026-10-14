package com.yanglizi.common.utils;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * @author win11
 */
public class Encrypt {

    public static String sha256(String input) {
        try {
            // 获取SHA-256实例
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            // 计算SHA-256摘要
            byte[] messageDigest = md.digest(input.getBytes());
            // 将字节数组转换为十六进制字符串
            StringBuilder sb = new StringBuilder();
            for (byte b : messageDigest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("密码加密失败"+e);
        }
    }
}

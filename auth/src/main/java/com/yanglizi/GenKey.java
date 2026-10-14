package com.yanglizi;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Encoders;

public class GenKey {
    public static void main(String[] args) {
        var key = Jwts.SIG.HS256.key().build();
        System.out.println(Encoders.BASE64.encode(key.getEncoded()));
    }
}
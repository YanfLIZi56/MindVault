package com.yanglizi.business.controller;

import com.yanglizi.common.anno.RequireLevel;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/hi")
public class hi {

    @RequireLevel(apiKey = "recipe:submit")
    @GetMapping
    public String hia(){
        return "hi";
    }
}

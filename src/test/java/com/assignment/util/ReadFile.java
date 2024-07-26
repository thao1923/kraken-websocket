package com.assignment.util;

import org.springframework.util.ResourceUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public class ReadFile {
    public static String readFile(String path){
        String result = "";
        try{
            File file = ResourceUtils.getFile(path);
            InputStreamReader isr = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8);
            BufferedReader br = new BufferedReader(isr);
            String line = null;
            while((line = br.readLine()) != null){
                result += line;
            }
            br.close();
        }catch (Exception e){
            e.printStackTrace();
        }
        return result;
    }
}

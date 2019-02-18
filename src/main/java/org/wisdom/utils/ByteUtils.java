package org.wisdom.utils;

import com.fasterxml.jackson.databind.DeserializationConfig;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;

public class ByteUtils {
    private static ObjectMapper objectMapper=new ObjectMapper();
    private static List<String> datePattern=new ArrayList<String>();
    static {
        datePattern.add("yyyy-MM-dd hh/mm/ss");
        datePattern.add("yyyy-MM-dd hh:mm:ss");
        datePattern.add("yyyy/MM/dd hh:mm:dd");
    }
    static {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat();
        DeserializationConfig  config=objectMapper.getDeserializationConfig();
        for (String s : datePattern) {
            objectMapper.setDateFormat(new SimpleDateFormat(s));
        }
        objectMapper.setTimeZone(TimeZone.getTimeZone("GMT+8"));
    }
    public  static ObjectMapper InstanceObjectMapper(){
        return  objectMapper;
    }
    public List<String> getDatePattern() {
        return datePattern;
    }

    public void setDatePattern(List<String> datePattern) {
        this.datePattern = datePattern;
    }
}

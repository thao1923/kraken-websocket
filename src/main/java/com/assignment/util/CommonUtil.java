package com.assignment.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanWrapperImpl;

@Slf4j
public class CommonUtil {

    public static Object getFieldByFieldName(Object o, String fieldName){
        if (o == null) return null;
        try{
            BeanWrapperImpl beanWrapper = new BeanWrapperImpl(o);
            return beanWrapper.getPropertyValue(fieldName);
        }catch (Exception e){
            log.error("get field value error {}", e.getMessage());
        }
        return null;

    }
}

package com.assignment.util;

import com.assignment.dto.Level;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class CommonUtilTest {
    @Test
    void testGetFieldByFieldName(){
        Level level = new Level();
        level.setPrice(BigDecimal.ONE);
        level.setQty(BigDecimal.TEN);

        assertEquals(BigDecimal.ONE, CommonUtil.getFieldByFieldName(level, "price"));
        assertEquals(BigDecimal.TEN, CommonUtil.getFieldByFieldName(level, "qty"));
    }

    @Test
    void testGetFieldByFieldName_nullInput(){
        assertNull(CommonUtil.getFieldByFieldName(null, "price"));
    }

    @Test
    void testGetFieldByFieldName_exception(){
        Level level = new Level();
        level.setPrice(BigDecimal.ONE);
        level.setQty(BigDecimal.TEN);

        assertNull(CommonUtil.getFieldByFieldName(level, "test"));
    }

}
package com.alshuibi.grocery;

import com.alshuibi.grocery.model.Item;
import com.alshuibi.grocery.utils.LocalFormat;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ExampleUnitTest {

    @Test
    public void currency_isFormattedAsYemeniRial() {
        assertEquals("0 ر.ي", LocalFormat.getCurrencyFormat(0));
        assertEquals("1,500 ر.ي", LocalFormat.getCurrencyFormat(1500));
    }

    @Test
    public void item_defaultsAreSafe() {
        Item item = new Item();
        item.setName(null);
        item.setSku(null);
        item.setCategory(null);
        item.setUnitType(null);
        item.setStock(-4);
        assertEquals("", item.getName());
        assertEquals("", item.getSku());
        assertEquals("أخرى", item.getCategory());
        assertEquals("حبة", item.getUnitType());
        assertEquals(0, item.getStock());
        assertTrue(item.getMinStock() >= 0);
    }
}

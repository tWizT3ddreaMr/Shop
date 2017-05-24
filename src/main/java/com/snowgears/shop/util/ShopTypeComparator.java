package com.snowgears.shop.util;

import com.snowgears.shop.ShopObject;

import java.util.Comparator;

public class ShopTypeComparator implements Comparator<ShopObject>{
	@Override
    public int compare(ShopObject o1, ShopObject o2) {
        return o1.getType().toString().compareTo(o2.getType().toString());
    }
}

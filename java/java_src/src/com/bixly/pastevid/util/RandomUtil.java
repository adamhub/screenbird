/*
 * RandomUtil.java
 *
 * Version 1.0
 * 
 * 17 May 2013
 */
package com.bixly.pastevid.util;

import com.bixly.pastevid.common.Unimplemented;
import java.util.HashMap;
import java.util.Random;

/**
 * Utility class for generating random slugs. Used in unit tests only.
 * @author cevaris
 */
public class RandomUtil {
    public final static Random rand = new Random(System.currentTimeMillis());
    
    @Unimplemented
    public static String generateSlug(int length, HashMap<String,Object> collection){
        String slug;
        
        while (true) {
            slug = generateSlug(length);
            if (collection.get(slug) == null) {
                return slug;
            }
        }
    }
    
    public static String generateSlug(int length){
        StringBuilder str = new StringBuilder();
        for (int i = 0; i < length; i++) {
            str.append((char)(rand.nextInt(25)+65));
        }
        return str.toString();
    }
}

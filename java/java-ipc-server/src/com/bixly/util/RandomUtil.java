/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.bixly.util;

import java.util.HashMap;
import java.util.Random;

/**
 *
 * @author cevaris
 */
public class RandomUtil {
    
    public final static Random rand = new Random(System.currentTimeMillis());
    
    
    public static String generateSlug(int length, HashMap<String,Object> collection){
        
        String slug;
        
        while(true){
            slug = generateSlug(length);
            
            if(collection.get(slug) == null)
                return slug;
        }
        
    }
    public static String generateSlug(int length){
        
        StringBuilder str = new StringBuilder();
        for (int i = 0; i < length; i++){
            
            str.append((char)(rand.nextInt(25)+65));
        }
        return str.toString();
        
    }
}

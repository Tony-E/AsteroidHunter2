/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package AsterUtil;

import AsterTracker.Settings;

/**
 *
 * @author Tony
 */
public class Tester {
    
    int i;
    int j;
    int group;
    float motion;
    float pa;
    Settings settings;
            
    
    /* construct tester with xy limits per group */

    /**
     *
     * @param s
     * @param x
     * @param y
     * @param m
     * @param p
     * @param g
     */
    
    public Tester(Settings s, int x, int y, int g) {
      settings = s; 
      i=x;
      j=y;
      group = g;
    
    }
    /* return yes if values are inside limits */

    /**
     *
     * @param x
     * @param y
     * @param g
     * @return
     */
    
    public boolean test(int x, int y, int g ) {
        boolean flag = true;
        if (x>i+7) flag = false;
        if (x<i-7) flag = false;
        if (y>j+7) flag = false;
        if (y<j-7) flag = false;
        
        if (g != group) flag = false;
        return flag;
}
    
    
}


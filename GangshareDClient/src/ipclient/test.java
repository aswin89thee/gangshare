/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ipclient;

/**
 *
 * @author hp
 */
public class test {
    public static void main(String args[]){
        byte b = -128;
        int count = 0, i=0;
        while(i < 8){
            System.out.print(b&1);
            count += b&1;
            b>>=1;
            i++;
        }
        System.out.println();
        System.out.println(count);
    }
}

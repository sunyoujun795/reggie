package com.itheima.test;

import org.junit.jupiter.api.Test;

public class UploadFileTest {

    @Test
    public void test1(){
        String filename = "124124.jpg";
        String substring = filename.substring(filename.lastIndexOf("."));
        System.out.println(substring);

    }
}

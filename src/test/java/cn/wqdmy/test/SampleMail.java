package cn.wqdmy.test;


import cn.wqdmy.utils.EmailUtils;

import java.io.File;

public class SampleMail {

    public static void main(String[] args) {
        File[] files = new  File[2];
        files[0] = new File("D:\\data\\log\\redis\\log4j");
        files[1] = new File("D:\\data\\log\\redis\\中文名附件测试.txt");
        EmailUtils.sendMail("mail@wq-dmy.cn",null,"韦一笑","测试邮件","附件测试" , true , files);
        System.out.println("邮件发送结束！");
    }
}

package cn.wqdmy.utils;

import javax.activation.DataHandler;
import javax.activation.FileDataSource;
import javax.mail.*;
import javax.mail.internet.*;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * 电子邮件发送工具
 * @author dmy
 * @date 2019/11/3
 */
public final class EmailUtils {

    /**
     * 工具禁止外部创建对象
     */
    private EmailUtils(){}

    /**
     * 默认配置文件路径
     */
    private static String defaultConfigPath = "email.properties";

    /**
     * 默认富文本类型编码
     */
    private final static String defaultHtmlCharset = "text/html;charset=UTF-8";

    /**
     * 配置对象
     */
    private static Properties properties;

    /**
     * 设置邮件配置文件路径
     * @param path
     */
    public static void setConfigPath(String path){
        // 如果指定配置文件覆盖默认配置
        defaultConfigPath = path;
        properties = new Properties();
        try (InputStream inputStream = EmailUtils.class.getResourceAsStream("/" + defaultConfigPath);) {
            properties.load(inputStream);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 设置邮件配置
     * @param props
     */
    public static void setConfig(Properties props){
        properties = props;
    }

    /**
     * 获取邮件使用配置
     * @return
     */
    public static Properties getConfig(){
        // 配置为空
        if(properties == null){
            // 防止多线程并发加载配置，只加载一次配置
            synchronized (EmailUtils.class){
                if(properties == null){
                    // 使用默认配置文件加载配置
                    setConfigPath(defaultConfigPath);
                }
            }
        }
        return properties;
    }

    /**
     * 普通文本邮件
     * @param toAddresses 发送地址 多个“,”隔开
     * @param personal 签名
     * @param subject 邮件标题
     * @param content 邮件内容
     */
    public static void sendTextMail(String toAddresses, String personal, String subject, String content){
        sendMail(toAddresses, null, personal, subject ,content, false, null);
    }

    /**
     * 普通文本邮件
     * @param toAddresses 发送地址 多个“,”隔开
     * @param ccAddresses 抄送地址 多个“,”隔开
     * @param personal 签名
     * @param subject 邮件标题
     * @param content 邮件内容
     */
    public static void sendTextMail(String toAddresses, String ccAddresses, String personal, String subject, String content){
        sendMail(toAddresses, ccAddresses, personal, subject ,content, false, null);
    }

    /**
     * 发送普通富文本邮件
     * @param toAddresses 发送地址 多个“,”隔开
     * @param ccAddresses 抄送地址 多个“,”隔开
     * @param personal 签名
     * @param subject 邮件标题
     * @param content 邮件内容
     */
    public static void sendHtmlMail(String toAddresses, String ccAddresses, String personal, String subject, String content){
        sendMail(toAddresses, ccAddresses, personal, subject ,content, true, null);
    }

    /**
     * 发送邮件方法
     * @param toAddresses 发送地址 多个“,”隔开
     * @param ccAddresses 抄送地址 多个“,”隔开
     * @param personal 签名
     * @param subject 邮件标题
     * @param content 邮件内容
     * @param isHtml 是否事富文本
     * @param files  附件
     */
    public static void sendMail(String toAddresses, String ccAddresses, String personal, String subject, String content, boolean isHtml, File[] files){
        properties = getConfig();
        // 用户名、密码
        String userName = properties.getProperty("mail.user");
        String password = properties.getProperty("mail.password");
        // 发件地址
        String fromAddress = properties.getProperty("email.from.address");
        // 接受回复地址
        String replyAddress = properties.getProperty("email.reply.address");
        if(replyAddress == null){
            replyAddress = fromAddress;
        }
        if(personal == null){
            personal = properties.getProperty("email.from.personal");
        }
        // 构建授权信息，用于进行SMTP进行身份验证
        Authenticator authenticator = new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(userName, password);
            }
        };
        // 使用环境属性和授权信息，创建邮件会话
        Session mailSession = Session.getInstance(properties, authenticator);
        // 创建邮件消息
        MimeMessage message = new MimeMessage(mailSession);

        try {
            // personal
            // 设置发件人邮件地址和名称。填写控制台配置的发信地址,比如xxx@xxx.com。和上面的mail.user保持一致。名称用户可以自定义填写。
            InternetAddress from = new InternetAddress(fromAddress, personal);
            message.setFrom(from);
            //可选。设置回信地址
            Address[] a = new Address[1];
            a[0] = new InternetAddress(replyAddress);
            message.setReplyTo(a);
            // 设置收件人邮件地址，比如yyy@yyy.com
            //如果同时发给多人,地址","隔开，但是单次发送人数不要太多
            if(null != toAddresses && !toAddresses.isEmpty()){
                String[] as = toAddresses.split(",");
                int alength = as.length;
                InternetAddress[] internetAddressTos = new InternetAddress[alength];
                for(int i=0; i < alength; i++){
                    internetAddressTos[i] = new InternetAddress(as[i]);
                }
                message.setRecipients(Message.RecipientType.TO, internetAddressTos);
            }
            // 设置多个抄送地址
            if(null != ccAddresses && !ccAddresses.isEmpty()){
                String[] ccs = ccAddresses.split(",");
                InternetAddress[] internetAddressCC = new InternetAddress[ccs.length];
                for(int i=0; i < ccs.length; i++){
                    internetAddressCC[i] = new InternetAddress(ccs[i]);
                }
                message.setRecipients(Message.RecipientType.CC, internetAddressCC);
            }
            // 设置多个密送地址
            // todo message.setRecipients(Message.RecipientType.BCC, internetAddressBCC);

            // 设置邮件标题
            message.setSubject(subject);
            // 设置邮件的内容体 message.setContent(content, "text/html;charset=UTF-8");
            // 创建多重消息
            Multipart multipart = new MimeMultipart();
            // 发送附件，总的邮件大小不超过15M，创建消息部分
            BodyPart messageBodyPart = new MimeBodyPart();
            // 消息
            if(isHtml){
                messageBodyPart.setContent(content, defaultHtmlCharset);
            }else {
                messageBodyPart.setText(content);
            }
            // 设置文本消息部分
            multipart.addBodyPart(messageBodyPart);
            // 有附件添加附件
            if(files != null && files.length > 0){
                // 多个附件循环
                for (File file : files){
                    messageBodyPart = new MimeBodyPart();
                    //设置要发送附件的文件
                    FileDataSource source = new FileDataSource(file);
                    messageBodyPart.setDataHandler(new DataHandler(source));
                    //处理附件名称中文（附带文件路径）乱码问题
                    messageBodyPart.setFileName(MimeUtility.encodeText(file.getName()));
                    //添加附件对象
                    multipart.addBodyPart(messageBodyPart);
                }
            }
            // 发送含有附件的完整消息
            message.setContent(multipart);
            // 发送附件代码，结束
            // 发送邮件
            Transport.send(message);
        }catch (Exception e) {
            e.printStackTrace();
        }
    }

}

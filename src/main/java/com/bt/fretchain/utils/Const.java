package com.bt.fretchain.utils;

import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

@Component("constants")
public class Const {
    public static final boolean DEVELOPMENT = true;
    @PostConstruct
    public void postConstructInitialization() {
        refreshSettingVariables();
    }

    public void refreshSettingVariables() {
//        refreshSmsSettingVariable();
//        refreshEmailSetting();
//        refreshAmazonS3Bucket();
        refreshSystemSetting();
        refreshStaticContentCMSVariables();
//        refreshSystemAdminSetting();
        refreshWebSiteCMSVariables();
    }


    public void refreshSystemSetting() {
//        SystemSetting.ContactUsQueryToEmail = settingService.getSettingValueByKey(SettingEnum.ContactUsQueryToEmail);
//        SystemSetting.TestMode = settingService.getSettingValueByKey(SettingEnum.TestMode).equalsIgnoreCase("1");
//        SystemSetting.BaseUrl = settingService.getSettingValueByKey(SettingEnum.BaseUrl);
//        SystemSetting.SupportEmail = settingService.getSettingValueByKey(SettingEnum.SupportEmail);
//        SystemSetting.HeaderAlertLine = settingService.getSettingValueByKey(SettingEnum.HeaderAlertLine);
//        SystemSetting.CashOnDelivery = settingService.getSettingValueByKey(SettingEnum.CashOnDelivery).equalsIgnoreCase("1");
//        SystemSetting.SupportContactNo = settingService.getSettingValueByKey(SettingEnum.SupportContactNo);
//        SystemSetting.companyAddress = settingService.getSettingValueByKey(SettingEnum.CompanyAddress);
//        SystemSetting.HeaderAlertLine = settingService.getSettingValueByKey(SettingEnum.HeaderAlertLine);
//        SystemSetting.facebookUrl = settingService.getSettingValueByKey(SettingEnum.FacebookUrl);
//        SystemSetting.twitterUrl = settingService.getSettingValueByKey(SettingEnum.TwitterUrl);
//        SystemSetting.youtubeUrl = settingService.getSettingValueByKey(SettingEnum.YoutubeUrl);
//        SystemSetting.telegramUrl = settingService.getSettingValueByKey(SettingEnum.TelegramUrl);
//        SystemSetting.instagramUrl = settingService.getSettingValueByKey(SettingEnum.InstagramUrl);
//        refreshPrivatePublicKey();
    }

    public void refreshSystemAdminSetting() {
//        SystemAdminSetting.SESEmail = settingService.getSettingValueByKey(SettingKeysEnum.SESEmail);
//        SystemAdminSetting.SESHost = settingService.getSettingValueByKey(SettingKeysEnum.SESHost);
//        SystemAdminSetting.SESPort = settingService.getSettingValueByKey(SettingKeysEnum.SESPort);
//        SystemAdminSetting.SESEmailSender = settingService.getSettingValueByKey(SettingKeysEnum.SESEmailSender);
//        SystemAdminSetting.SESAccessKey = settingService.getSettingValueByKey(SettingKeysEnum.SESAccessKey);
//        SystemAdminSetting.SESSecretKey = settingService.getSettingValueByKey(SettingKeysEnum.SESSecretKey);
    }

//    public void refreshPrivatePublicKey() {
//        SystemSetting.PublicKey = settingService.getSettingValueByKey(SettingEnum.PublicKey);
//        SystemSetting.PrivateKey = settingService.getSettingValueByKey(SettingKeysEnum.PrivateKey);
//    }

//    public void refreshAmazonS3Bucket() {
//        AmazonS3Bucket.S3AccessKey = settingService.getSettingValueByKey(SettingKeysEnum.S3AccessKey);
//        AmazonS3Bucket.S3SecretKey = settingService.getSettingValueByKey(SettingKeysEnum.S3SecretKey);
//        AmazonS3Bucket.S3Bucket = settingService.getSettingValueByKey(SettingKeysEnum.S3Bucket);
//        AmazonS3Bucket.S3BucketBaseURL = settingService.getSettingValueByKey(SettingKeysEnum.S3BucketBaseURL);
//        AmazonS3Bucket.refresh();
//    }

    public void refreshWebSiteCMSVariables() {
//        WebSiteCMS.CompanyLogoDark = webSiteCMSService.getWebSiteCMSValueByKey(WebSiteCMSKeysEnum.CompanyLogoDark);
//        WebSiteCMS.FaviconIcon = webSiteCMSService.getWebSiteCMSValueByKey(WebSiteCMSKeysEnum.FaviconIcon);
//        WebSiteCMS.CompanyName = webSiteCMSService.getWebSiteCMSValueByKey(WebSiteCMSKeysEnum.CompanyName);

//        WebSiteCMS.CompanyLogoLight = webSiteCMSService.getWebSiteCMSValueByKey(WebSiteCMSKeysEnum.CompanyLogoLight);
//        WebSiteCMS.CompanyAddress = webSiteCMSService.getWebSiteCMSValueByKey(WebSiteCMSKeysEnum.CompanyAddress);
//        WebSiteCMS.CompanyContactNumber = webSiteCMSService.getWebSiteCMSValueByKey(WebSiteCMSKeysEnum.CompanyContactNumber);
//        WebSiteCMS.CompanyContactEmail = webSiteCMSService.getWebSiteCMSValueByKey(WebSiteCMSKeysEnum.CompanyContactEmail);
    }

    public static class SystemSetting {
        public static String AndroidAppUrl;
        public static String IosAppUrl;
        public static String CustomerAndroidAppVersion;
        public static String CustomerIosAppVersion;
        public static boolean UnderMaintenance;
        public static boolean AdminLogin;
        public static boolean CustomerLogin;
        public static boolean TestMode;
        public static boolean DebugMode;
        public static String GoogleMapApiKey;
        public static String BaseUrl;
        public static String CurrencySymbol;
        public static String CurrencyCode;
        public static String SupportEmail;
        public static String SupportContactNo;

        public static String companyAddress;
        public static String HeaderAlertLine;
        public static boolean CashOnDelivery;
        public static String facebookUrl;
        public static String twitterUrl;
        public static String youtubeUrl;
        public static String telegramUrl;
        public static String instagramUrl;

    }

    public static class SystemAdminSetting {
    }

    public static class WebSiteCMS {
        public static String AboutUs;
        public static String TermsAndCondition;
        public static String HelpAndSupport;
        public static String PrivacyPolicy ;
        public static String ContactUs;
        public static String ReturnAndReplacement;
        public static String RefundPolicy;
        public static String TrackOrder;
        public static String ShippingPolicy;
        public static String CancellationPolicy;
        public static String CompanyLogoDark;
        public static String FaviconIcon;
        public static String CompanyName;
        public static String CompanyLogoLight;
        public static String CompanyAddress;
        public static String CompanyContactNumber;
        public static String CompanyContactEmail;
        public static String Instagram;
        public static String GPlus;
        public static String Pinterest;
        public static String Facebook;
        public static String Twitter;
        public static String Youtube;
        public static String Tumblr;
    }

    public void refreshStaticContentCMSVariables() {
//        WebSiteCMS.AboutUs = websiteCmsSettingService.get(WebsiteCmsSettingEnum.AboutUs);
//        WebSiteCMS.TermsAndCondition = websiteCmsSettingService.get(WebsiteCmsSettingEnum.TermsAndCondition);
//        WebSiteCMS.HelpAndSupport = websiteCmsSettingService.get(WebsiteCmsSettingEnum.AboutUs);
//        WebSiteCMS.PrivacyPolicy = websiteCmsSettingService.get(WebsiteCmsSettingEnum.PrivacyPolicy);
//        WebSiteCMS.ContactUs = websiteCmsSettingService.get(WebsiteCmsSettingEnum.ContactUs);
//        WebSiteCMS.ReturnAndReplacement = websiteCmsSettingService.get(WebsiteCmsSettingEnum.ReturnAndReplacement);
//        WebSiteCMS.RefundPolicy = websiteCmsSettingService.get(WebsiteCmsSettingEnum.RefundPolicy);
//        WebSiteCMS.TrackOrder = websiteCmsSettingService.get(WebsiteCmsSettingEnum.TrackOrder);
//        WebSiteCMS.ShippingPolicy = websiteCmsSettingService.get(WebsiteCmsSettingEnum.ShippingPolicy);
//        WebSiteCMS.CancellationPolicy = websiteCmsSettingService.get(WebsiteCmsSettingEnum.CancellationPolicy);
    }

    public static class AmazonS3Bucket {
        public static String S3AccessKey;
        public static String S3SecretKey;
        public static String S3Bucket;
        public static String S3BucketBaseURL;

        public static String BUCKET_File_Folder;
        public static String BUCKET_Primary_Image_Folder;
        public static String BUCKET_Thumbnail_Image_Folder;

        public static String BUCKET_File_Folder_URL;
        public static String BUCKET_Primary_Image_Folder_URL;
        public static String BUCKET_Thumbnail_Image_Folder_URL;

        public static void refresh() {
            BUCKET_File_Folder = S3Bucket + "/" + "files";
            BUCKET_Primary_Image_Folder = S3Bucket + "/" + "primary-image";
            BUCKET_Thumbnail_Image_Folder = S3Bucket + "/" + "thumbnail-image";

            BUCKET_File_Folder_URL = S3BucketBaseURL + "/" + BUCKET_File_Folder;
            BUCKET_Primary_Image_Folder_URL = S3BucketBaseURL + "/" + BUCKET_Primary_Image_Folder;
            BUCKET_Thumbnail_Image_Folder_URL = S3BucketBaseURL + "/" + BUCKET_Thumbnail_Image_Folder;
        }

        public static String getSmallImageUrl(String fileName) {
            return BUCKET_Thumbnail_Image_Folder_URL + "/" + fileName;
        }

        public static String getLargeImageUrl(String fileName) {
            return BUCKET_Primary_Image_Folder_URL + "/" + fileName;
        }

        public static String getFileUrl(String fileName) {
            return BUCKET_File_Folder_URL + "/" + fileName;
        }
    }

    public enum PaymentMode {
        Razorpay("Razorpay"),
        Paytm("Paytm"),
        CashOnDelivery("Cash On Delivery");

        String mode;

        PaymentMode(String pMode) {
            this.mode = pMode;
        }

        @Override
        public String toString() {
            return mode;
        }

        public String getMode() {
            return mode;
        }
    }

}

package com.nse.analyser.constants;

public final class BrowserHeaders {

    public static final String PRIORITY = "u=1, i";
    public static final String SEC_CH_UA = "\"Not=A?Brand\";v=\"99\", \"Microsoft Edge\";v=\"151\", \"Chromium\";v=\"151\"";
    public static final String sec_ch_ua_mobile = "?0";
    public static final String sec_ch_ua_platform = "\"Windows\"";
    public static final String sec_fetch_dest = "empty";
    public static final String sec_fetch_mode = "cors";
    public static final String sec_fetch_site = "same-origin";
    private BrowserHeaders() {
    }


    public static final String ACCEPT_ENCODING = "gzip, deflate, br" ;

    public static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/151.0.0.0 Safari/537.36 Edg/151.0.0.0";

//"Mozilla/5.0 (Windows NT 10.0; Win64; x64) " +
//        "AppleWebKit/537.36 (KHTML, like Gecko) " +
//        "Chrome/138.0.0.0 Safari/537.36";
//    public static final String ACCEPT = "application/json,text/plain,*/*";
    public static final String ACCEPT = "*/*";
    public static final String ACCEPT_LANGUAGE = "en-US,en;q=0.9,en-IN;q=0.8";
}
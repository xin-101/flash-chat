package io.github.zh.common.constants;

import java.time.format.DateTimeFormatter;

public interface DateConstants {
    /**
     *  DateTimeFormatter:年-月-日 时:分:秒
     */
    DateTimeFormatter DATE_FORMAT_Y_M_D_H_M_S = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    /**
     *  DateTimeFormatter:年-月-日
     */
    DateTimeFormatter DATE_FORMAT_Y_M_D = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    /**
     *  DateTimeFormatter:时：分:秒
     */
    DateTimeFormatter DATE_FORMAT_H_M_S = DateTimeFormatter.ofPattern("HH:mm:ss");
    /**
     *  DateTimeFormatter:年-月
     */
    DateTimeFormatter DATE_FORMAT_Y_M = DateTimeFormatter.ofPattern("yyyy-MM");
    /**
     *  GMT+8
     */
    String GMT_8 = "GMT+8";
    /**
     *  yyyy-MM-dd
     */
    String DATE_FORMAT_Y_M_D_STR = "yyyy-MM-dd";
    /**
     *  yyyy-MM-dd HH:mm:ss
     */
    String DATE_FORMAT_Y_M_D_H_M_S_STR = "yyyy-MM-dd HH:mm:ss";

    /**
     *  yyyy-MM-dd HH:mm:ss.SSS
     */
    String DATE_FORMAT_Y_M_D_H_M_S_SSS_STR = "yyyy-MM-dd HH:mm:ss.SSS";

    /**
     *  60s
     */
    long ONE_MINUTE = 60 * 1000;
    /**
     *  24h
     */
    long ONE_DAY = 24 *60* ONE_MINUTE;
    /**
     *  7d
     */
    long ONE_WEEK = 7 * ONE_DAY;
}

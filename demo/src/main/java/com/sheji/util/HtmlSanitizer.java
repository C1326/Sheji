package com.sheji.util;

import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;

/**
 * 富文本内容消毒，防止存储型 XSS。
 * 使用白名单保留富文本常用标签与 wangeditor 生成的 video/img/内联样式，
 * 同时剔除 script、事件属性、javascript: 等危险内容。
 */
public final class HtmlSanitizer {

    private static final Safelist SAFELIST = Safelist.relaxed()
            .addTags("video", "source", "img")
            .addAttributes("video", "src", "controls", "poster", "width", "height")
            .addAttributes("source", "src", "type")
            .addAttributes(":all", "style", "class", "align")
            .addProtocols("video", "src", "http", "https")
            .addProtocols("img", "src", "http", "https");

    private HtmlSanitizer() {
    }

    public static String sanitize(String html) {
        if (html == null || html.isEmpty()) {
            return html;
        }
        return Jsoup.clean(html, SAFELIST);
    }
}

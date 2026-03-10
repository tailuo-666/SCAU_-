package addressbook.util;

import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public final class PinyinUtil {
    private static final Map<Character, String> PINYIN_MAP = new HashMap<>();
    private static final Charset GBK = Charset.forName("GBK");
    private static final int[] SEC_POS_VALUE_LIST = {
            1601, 1637, 1833, 2078, 2274,
            2302, 2433, 2594, 2787, 3106,
            3212, 3472, 3635, 3722, 3730,
            3858, 4027, 4086, 4390, 4558,
            4684, 4925, 5249, 5600
    };
    private static final char[] FIRST_LETTER = {
            'a', 'b', 'c', 'd', 'e',
            'f', 'g', 'h', 'j', 'k',
            'l', 'm', 'n', 'o', 'p',
            'q', 'r', 's', 't', 'w',
            'x', 'y', 'z'
    };

    static {
        add('张', "zhang"); add('王', "wang"); add('李', "li"); add('赵', "zhao"); add('刘', "liu");
        add('陈', "chen"); add('杨', "yang"); add('黄', "huang"); add('周', "zhou"); add('吴', "wu");
        add('徐', "xu"); add('孙', "sun"); add('胡', "hu"); add('朱', "zhu"); add('高', "gao");
        add('林', "lin"); add('何', "he"); add('郭', "guo"); add('马', "ma"); add('罗', "luo");
        add('梁', "liang"); add('宋', "song"); add('郑', "zheng"); add('谢', "xie"); add('韩', "han");
        add('唐', "tang"); add('冯', "feng"); add('于', "yu"); add('董', "dong"); add('萧', "xiao");
        add('程', "cheng"); add('曹', "cao"); add('袁', "yuan"); add('邓', "deng"); add('许', "xu");
        add('傅', "fu"); add('沈', "shen"); add('曾', "zeng"); add('彭', "peng"); add('吕', "lv");
        add('苏', "su"); add('卢', "lu"); add('蒋', "jiang"); add('蔡', "cai"); add('贾', "jia");
        add('丁', "ding"); add('魏', "wei"); add('薛', "xue"); add('叶', "ye"); add('阎', "yan");
        add('余', "yu"); add('潘', "pan"); add('杜', "du"); add('戴', "dai"); add('夏', "xia");
        add('钟', "zhong"); add('汪', "wang"); add('田', "tian"); add('任', "ren"); add('姜', "jiang");
        add('范', "fan"); add('方', "fang"); add('石', "shi"); add('姚', "yao"); add('谭', "tan");
        add('廖', "liao"); add('邹', "zou"); add('熊', "xiong"); add('金', "jin"); add('陆', "lu");
        add('郝', "hao"); add('孔', "kong"); add('白', "bai"); add('崔', "cui"); add('康', "kang");
        add('毛', "mao"); add('邱', "qiu"); add('秦', "qin"); add('江', "jiang"); add('史', "shi");
        add('顾', "gu"); add('侯', "hou"); add('邵', "shao"); add('孟', "meng"); add('龙', "long");
        add('万', "wan"); add('段', "duan"); add('雷', "lei"); add('钱', "qian"); add('汤', "tang");
        add('尹', "yin"); add('黎', "li"); add('易', "yi"); add('常', "chang"); add('武', "wu");
        add('乔', "qiao"); add('贺', "he"); add('赖', "lai"); add('龚', "gong"); add('文', "wen");

        add('小', "xiao"); add('伟', "wei"); add('芳', "fang"); add('娜', "na"); add('敏', "min");
        add('静', "jing"); add('丽', "li"); add('强', "qiang"); add('磊', "lei"); add('军', "jun");
        add('洋', "yang"); add('勇', "yong"); add('艳', "yan"); add('杰', "jie"); add('娟', "juan");
        add('涛', "tao"); add('明', "ming"); add('超', "chao"); add('秀', "xiu"); add('英', "ying");
        add('霞', "xia"); add('平', "ping"); add('刚', "gang"); add('桂', "gui"); add('华', "hua");
        add('建', "jian"); add('国', "guo"); add('志', "zhi"); add('海', "hai"); add('云', "yun");
        add('春', "chun"); add('红', "hong"); add('玉', "yu"); add('金', "jin"); add('波', "bo");
        add('庆', "qing"); add('新', "xin"); add('月', "yue"); add('星', "xing"); add('乐', "le");
        add('安', "an"); add('宁', "ning"); add('天', "tian"); add('心', "xin"); add('成', "cheng");
        add('思', "si"); add('慧', "hui"); add('佳', "jia"); add('辰', "chen"); add('宇', "yu");
        add('晨', "chen"); add('清', "qing"); add('凯', "kai"); add('博', "bo"); add('嘉', "jia");
        add('源', "yuan"); add('家', "jia"); add('宁', "ning"); add('欣', "xin"); add('依', "yi");
        add('然', "ran"); add('可', "ke"); add('航', "hang"); add('迪', "di"); add('扬', "yang");
        add('嘉', "jia"); add('琪', "qi"); add('轩', "xuan"); add('泽', "ze"); add('涵', "han");
        add('雨', "yu"); add('倩', "qian"); add('阳', "yang"); add('学', "xue"); add('生', "sheng");
        add('同', "tong"); add('事', "shi"); add('朋', "peng"); add('友', "you"); add('亲', "qin");
    }

    private PinyinUtil() {
    }

    public static PinyinResult build(String name, String override) {
        String normalizedOverride = normalizeAlphaNum(override);
        if (!normalizedOverride.isBlank()) {
            return new PinyinResult(normalizedOverride, initialsFromAscii(normalizedOverride));
        }

        if (name == null || name.isBlank()) {
            return new PinyinResult("", "");
        }

        StringBuilder full = new StringBuilder();
        StringBuilder initials = new StringBuilder();
        boolean prevAsciiLetterOrDigit = false;

        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (Character.isWhitespace(c)) {
                prevAsciiLetterOrDigit = false;
                continue;
            }

            if (isAsciiLetterOrDigit(c)) {
                char lower = Character.toLowerCase(c);
                full.append(lower);
                if (!prevAsciiLetterOrDigit || Character.isDigit(lower)) {
                    initials.append(lower);
                }
                prevAsciiLetterOrDigit = true;
                continue;
            }

            prevAsciiLetterOrDigit = false;
            String mapped = PINYIN_MAP.get(c);
            if (mapped != null) {
                full.append(mapped);
                initials.append(mapped.charAt(0));
                continue;
            }

            char initial = initialForChar(c);
            if (initial != 0) {
                full.append(initial);
                initials.append(initial);
            }
        }

        return new PinyinResult(full.toString(), initials.toString());
    }

    public static char initialForChar(char c) {
        String mapped = PINYIN_MAP.get(c);
        if (mapped != null && !mapped.isBlank()) {
            return mapped.charAt(0);
        }
        if (!isChinese(c)) {
            return 0;
        }
        byte[] bytes = String.valueOf(c).getBytes(GBK);
        if (bytes.length < 2) {
            return 0;
        }
        int secPosValue = (bytes[0] & 0xFF) - 160;
        secPosValue = secPosValue * 100 + (bytes[1] & 0xFF) - 160;
        int maxRangeCount = Math.min(FIRST_LETTER.length, SEC_POS_VALUE_LIST.length);
        for (int i = 0; i < maxRangeCount; i++) {
            int start = SEC_POS_VALUE_LIST[i];
            int end = (i + 1 < SEC_POS_VALUE_LIST.length)
                    ? SEC_POS_VALUE_LIST[i + 1]
                    : Integer.MAX_VALUE;
            if (secPosValue >= start && secPosValue < end) {
                return FIRST_LETTER[i];
            }
        }
        return 0;
    }

    public static String normalizeAlphaNum(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        String lower = text.toLowerCase(Locale.ROOT);
        for (int i = 0; i < lower.length(); i++) {
            char c = lower.charAt(i);
            if (isAsciiLetterOrDigit(c)) {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private static String initialsFromAscii(String text) {
        StringBuilder sb = new StringBuilder();
        boolean wordStart = true;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (!isAsciiLetterOrDigit(c)) {
                wordStart = true;
                continue;
            }
            if (wordStart) {
                sb.append(c);
                wordStart = false;
            }
        }
        if (sb.isEmpty()) {
            return text;
        }
        return sb.toString();
    }

    private static boolean isAsciiLetterOrDigit(char c) {
        return (c >= 'a' && c <= 'z')
                || (c >= 'A' && c <= 'Z')
                || (c >= '0' && c <= '9');
    }

    private static boolean isChinese(char c) {
        Character.UnicodeBlock block = Character.UnicodeBlock.of(c);
        return block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS
                || block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A
                || block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_B
                || block == Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS;
    }

    private static void add(char ch, String pinyin) {
        PINYIN_MAP.put(ch, pinyin);
    }

    public record PinyinResult(String full, String initials) {
    }
}

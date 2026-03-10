package addressbook.io;

import addressbook.model.ContactDraft;
import addressbook.util.DateUtil;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import static addressbook.util.CsvUtil.parse;

public class CsvImporter {
    public List<ImportedContact> importFrom(Path file) throws IOException {
        String content = Files.readString(file, StandardCharsets.UTF_8);
        List<List<String>> rows = parse(content);
        if (rows.isEmpty()) {
            return List.of();
        }

        Map<String, Integer> headerIndex = buildHeaderIndex(rows.get(0));
        List<ImportedContact> imported = new ArrayList<>();

        for (int i = 1; i < rows.size(); i++) {
            List<String> row = rows.get(i);
            ContactDraft draft = new ContactDraft();
            draft.setName(value(row, headerIndex, "name", "姓名"));
            if (draft.getName().isBlank()) {
                continue;
            }
            draft.setPhone(value(row, headerIndex, "phone", "电话"));
            draft.setMobile(value(row, headerIndex, "mobile", "手机"));
            draft.setImType(value(row, headerIndex, "imtype", "即时通信工具"));
            draft.setImNumber(value(row, headerIndex, "imnumber", "即时通信号码", "im号码"));
            draft.setEmail(value(row, headerIndex, "email", "电子邮箱"));
            draft.setWebsite(value(row, headerIndex, "website", "个人主页"));
            draft.setBirthday(DateUtil.parseFlexible(value(row, headerIndex, "birthday", "生日")));
            draft.setPhotoPath(value(row, headerIndex, "photopath", "photo", "像片", "照片"));
            draft.setCompany(value(row, headerIndex, "company", "工作单位"));
            draft.setHomeAddress(value(row, headerIndex, "homeaddress", "家庭地址"));
            draft.setPostalCode(value(row, headerIndex, "postalcode", "邮编"));
            draft.setNote(value(row, headerIndex, "note", "备注"));
            draft.setPinyinOverride(value(row, headerIndex, "pinyinoverride", "拼音修正", "拼音"));

            Set<String> groupNames = splitGroups(value(row, headerIndex, "groups", "所属组", "分组"));
            imported.add(new ImportedContact(draft, groupNames));
        }

        return imported;
    }

    private Map<String, Integer> buildHeaderIndex(List<String> header) {
        Map<String, Integer> map = new LinkedHashMap<>();
        for (int i = 0; i < header.size(); i++) {
            String key = normalize(header.get(i));
            if (!key.isBlank()) {
                map.put(key, i);
            }
        }
        return map;
    }

    private String value(List<String> row, Map<String, Integer> headerIndex, String... aliases) {
        for (String alias : aliases) {
            Integer idx = headerIndex.get(normalize(alias));
            if (idx != null && idx >= 0 && idx < row.size()) {
                return row.get(idx) == null ? "" : row.get(idx).trim();
            }
        }
        return "";
    }

    private String normalize(String text) {
        if (text == null) {
            return "";
        }
        return text.trim().toLowerCase(Locale.ROOT).replace("_", "").replace(" ", "");
    }

    private Set<String> splitGroups(String text) {
        Set<String> names = new LinkedHashSet<>();
        if (text == null || text.isBlank()) {
            return names;
        }
        String[] parts = text.split("[|;,，；]");
        for (String part : parts) {
            String trimmed = part.trim();
            if (!trimmed.isBlank()) {
                names.add(trimmed);
            }
        }
        return names;
    }
}

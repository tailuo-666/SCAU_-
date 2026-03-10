package addressbook.service;

import addressbook.model.Contact;
import addressbook.util.PinyinUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class SearchService {
    private final QueryService queryService;

    public SearchService(QueryService queryService) {
        this.queryService = queryService;
    }

    public List<Contact> search(String keyword, GroupFilter filter, SortOption sort) {
        List<Contact> base = queryService.query(filter, sort);
        if (keyword == null || keyword.isBlank()) {
            return base;
        }

        String lower = keyword.trim().toLowerCase(Locale.ROOT);
        String alphaNum = PinyinUtil.normalizeAlphaNum(lower);
        List<Contact> result = new ArrayList<>();

        for (Contact contact : base) {
            if (matches(contact, lower, alphaNum)) {
                result.add(contact);
            }
        }
        return result;
    }

    public Map<String, List<Contact>> classifyByInitial(List<Contact> contacts) {
        return queryService.classifyByInitial(contacts);
    }

    private boolean matches(Contact contact, String lowerKeyword, String alphaNumKeyword) {
        if (containsIgnoreCase(contact.getName(), lowerKeyword)
                || containsIgnoreCase(contact.getPhone(), lowerKeyword)
                || containsIgnoreCase(contact.getMobile(), lowerKeyword)
                || containsIgnoreCase(contact.getEmail(), lowerKeyword)
                || containsIgnoreCase(contact.getCompany(), lowerKeyword)
                || containsIgnoreCase(contact.getNote(), lowerKeyword)) {
            return true;
        }

        String pinyinFull = safe(contact.getPinyinFull());
        String pinyinInitials = safe(contact.getPinyinInitials());
        if (pinyinFull.isBlank() && pinyinInitials.isBlank()) {
            PinyinUtil.PinyinResult generated = PinyinUtil.build(contact.getName(), contact.getPinyinOverride());
            pinyinFull = generated.full();
            pinyinInitials = generated.initials();
        }

        String pinyinFullNorm = PinyinUtil.normalizeAlphaNum(pinyinFull);
        String pinyinInitialsNorm = PinyinUtil.normalizeAlphaNum(pinyinInitials);
        return !alphaNumKeyword.isBlank()
                && (pinyinFullNorm.contains(alphaNumKeyword) || pinyinInitialsNorm.contains(alphaNumKeyword));
    }

    private static boolean containsIgnoreCase(String text, String keywordLower) {
        return safe(text).toLowerCase(Locale.ROOT).contains(keywordLower);
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}

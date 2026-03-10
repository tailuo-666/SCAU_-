package addressbook.io;

import addressbook.model.ContactDraft;
import addressbook.util.DateUtil;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class VCardImporter {
    public List<ImportedContact> importFrom(Path file) throws IOException {
        List<String> rawLines = Files.readAllLines(file, StandardCharsets.UTF_8);
        List<String> qpMerged = mergeQuotedPrintable(rawLines);
        List<String> lines = unfold(qpMerged);

        List<ImportedContact> contacts = new ArrayList<>();
        List<String> current = null;

        for (String line : lines) {
            String upper = line.toUpperCase(Locale.ROOT);
            if (upper.startsWith("BEGIN:VCARD")) {
                current = new ArrayList<>();
                continue;
            }
            if (upper.startsWith("END:VCARD")) {
                if (current != null) {
                    ImportedContact imported = parseCard(current);
                    if (imported != null) {
                        contacts.add(imported);
                    }
                }
                current = null;
                continue;
            }
            if (current != null) {
                current.add(line);
            }
        }
        return contacts;
    }

    private ImportedContact parseCard(List<String> lines) {
        ContactDraft draft = new ContactDraft();
        Set<String> groupNames = new LinkedHashSet<>();

        for (String line : lines) {
            if (line == null || line.isBlank()) {
                continue;
            }
            int idx = line.indexOf(':');
            if (idx < 0) {
                continue;
            }

            String left = line.substring(0, idx);
            String valuePart = line.substring(idx + 1);
            String[] leftParts = left.split(";");
            String prop = leftParts[0].toUpperCase(Locale.ROOT);
            List<String> params = new ArrayList<>();
            if (leftParts.length > 1) {
                params.addAll(Arrays.asList(leftParts).subList(1, leftParts.length));
            }

            String value = decodeIfNeeded(valuePart, params);
            value = unescape(value);

            switch (prop) {
                case "FN" -> {
                    if (draft.getName().isBlank()) {
                        draft.setName(value);
                    }
                }
                case "N" -> {
                    if (draft.getName().isBlank()) {
                        draft.setName(parseN(value));
                    }
                }
                case "TEL" -> fillTel(draft, value, params);
                case "EMAIL" -> {
                    if (draft.getEmail().isBlank()) {
                        draft.setEmail(value);
                    }
                }
                case "URL" -> {
                    if (draft.getWebsite().isBlank()) {
                        draft.setWebsite(value);
                    }
                }
                case "BDAY" -> draft.setBirthday(DateUtil.parseFlexible(value));
                case "PHOTO" -> draft.setPhotoPath(value);
                case "ORG" -> draft.setCompany(value);
                case "ADR" -> fillAddress(draft, value);
                case "CATEGORIES" -> splitGroups(value, groupNames);
                case "NOTE" -> draft.setNote(value);
                case "X-IM-TYPE" -> draft.setImType(value);
                case "X-IM-NUMBER" -> draft.setImNumber(value);
                case "X-PINYIN-OVERRIDE" -> draft.setPinyinOverride(value);
                default -> {
                }
            }
        }

        if (draft.getName().isBlank()) {
            return null;
        }
        return new ImportedContact(draft, groupNames);
    }

    private void fillTel(ContactDraft draft, String value, List<String> params) {
        boolean isCell = hasType(params, "CELL") || hasType(params, "MOBILE");
        if (isCell) {
            if (draft.getMobile().isBlank()) {
                draft.setMobile(value);
            } else if (draft.getPhone().isBlank()) {
                draft.setPhone(value);
            }
        } else {
            if (draft.getPhone().isBlank()) {
                draft.setPhone(value);
            } else if (draft.getMobile().isBlank()) {
                draft.setMobile(value);
            }
        }
    }

    private boolean hasType(List<String> params, String target) {
        String upperTarget = target.toUpperCase(Locale.ROOT);
        for (String param : params) {
            String upper = param.toUpperCase(Locale.ROOT);
            if (upper.equals(upperTarget) || upper.contains("TYPE=" + upperTarget) || upper.contains("," + upperTarget)) {
                return true;
            }
            if (upper.startsWith("TYPE=")) {
                String[] types = upper.substring(5).split(",");
                for (String type : types) {
                    if (type.trim().equals(upperTarget)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private void fillAddress(ContactDraft draft, String value) {
        String[] parts = value.split(";", -1);
        String street = parts.length > 2 ? parts[2] : "";
        String locality = parts.length > 3 ? parts[3] : "";
        String region = parts.length > 4 ? parts[4] : "";
        String postal = parts.length > 5 ? parts[5] : "";
        String country = parts.length > 6 ? parts[6] : "";

        StringBuilder address = new StringBuilder();
        appendIfPresent(address, street);
        appendIfPresent(address, locality);
        appendIfPresent(address, region);
        appendIfPresent(address, country);

        if (draft.getHomeAddress().isBlank()) {
            draft.setHomeAddress(address.toString());
        }
        if (draft.getPostalCode().isBlank()) {
            draft.setPostalCode(postal);
        }
    }

    private void appendIfPresent(StringBuilder sb, String value) {
        if (value == null || value.isBlank()) {
            return;
        }
        if (!sb.isEmpty()) {
            sb.append(' ');
        }
        sb.append(value.trim());
    }

    private void splitGroups(String value, Set<String> target) {
        if (value == null || value.isBlank()) {
            return;
        }
        String[] parts = value.split("[,，;；|]");
        for (String part : parts) {
            String trimmed = part.trim();
            if (!trimmed.isBlank()) {
                target.add(trimmed);
            }
        }
    }

    private String parseN(String value) {
        String[] parts = value.split(";", -1);
        String family = parts.length > 0 ? parts[0].trim() : "";
        String given = parts.length > 1 ? parts[1].trim() : "";
        String combined = (family + given).trim();
        if (!combined.isEmpty()) {
            return combined;
        }
        return value.replace(";", "").trim();
    }

    private String decodeIfNeeded(String value, List<String> params) {
        String charsetName = "UTF-8";
        boolean quotedPrintable = false;

        for (String param : params) {
            String upper = param.toUpperCase(Locale.ROOT);
            if (upper.startsWith("CHARSET=")) {
                charsetName = param.substring(param.indexOf('=') + 1).trim();
            }
            if (upper.equals("QUOTED-PRINTABLE") || upper.contains("ENCODING=QUOTED-PRINTABLE")) {
                quotedPrintable = true;
            }
        }

        if (!quotedPrintable) {
            return value;
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c == '=') {
                if (i + 2 < value.length() && isHex(value.charAt(i + 1)) && isHex(value.charAt(i + 2))) {
                    int b = Integer.parseInt("" + value.charAt(i + 1) + value.charAt(i + 2), 16);
                    out.write(b);
                    i += 2;
                }
                continue;
            }
            out.write((byte) c);
        }

        Charset charset;
        try {
            charset = Charset.forName(charsetName);
        } catch (Exception ex) {
            charset = StandardCharsets.UTF_8;
        }
        return new String(out.toByteArray(), charset);
    }

    private boolean isHex(char c) {
        return (c >= '0' && c <= '9')
                || (c >= 'A' && c <= 'F')
                || (c >= 'a' && c <= 'f');
    }

    private String unescape(String value) {
        return value
                .replace("\\n", "\n")
                .replace("\\N", "\n")
                .replace("\\;", ";")
                .replace("\\,", ",")
                .replace("\\\\", "\\");
    }

    private List<String> unfold(List<String> lines) {
        List<String> unfolded = new ArrayList<>();
        for (String line : lines) {
            if (!unfolded.isEmpty() && (line.startsWith(" ") || line.startsWith("\t"))) {
                int last = unfolded.size() - 1;
                unfolded.set(last, unfolded.get(last) + line.substring(1));
            } else {
                unfolded.add(line);
            }
        }
        return unfolded;
    }

    private List<String> mergeQuotedPrintable(List<String> lines) {
        List<String> merged = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (String line : lines) {
            if (current.isEmpty()) {
                current.append(line);
            } else {
                current.append(line);
            }

            if (current.length() > 0 && current.charAt(current.length() - 1) == '=') {
                current.setLength(current.length() - 1);
                continue;
            }
            merged.add(current.toString());
            current.setLength(0);
        }
        if (!current.isEmpty()) {
            merged.add(current.toString());
        }
        return merged;
    }
}

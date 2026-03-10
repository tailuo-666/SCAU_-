package addressbook.model;

import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.Set;

public class Contact {
    private String id;
    private String name = "";
    private String phone = "";
    private String mobile = "";
    private String imType = "";
    private String imNumber = "";
    private String email = "";
    private String website = "";
    private LocalDate birthday;
    private String photoPath = "";
    private String company = "";
    private String homeAddress = "";
    private String postalCode = "";
    private Set<String> groupIds = new LinkedHashSet<>();
    private String note = "";
    private String pinyinFull = "";
    private String pinyinInitials = "";
    private String pinyinOverride = "";

    public Contact() {
    }

    public Contact(Contact other) {
        this.id = other.id;
        this.name = other.name;
        this.phone = other.phone;
        this.mobile = other.mobile;
        this.imType = other.imType;
        this.imNumber = other.imNumber;
        this.email = other.email;
        this.website = other.website;
        this.birthday = other.birthday;
        this.photoPath = other.photoPath;
        this.company = other.company;
        this.homeAddress = other.homeAddress;
        this.postalCode = other.postalCode;
        this.groupIds = new LinkedHashSet<>(other.groupIds);
        this.note = other.note;
        this.pinyinFull = other.pinyinFull;
        this.pinyinInitials = other.pinyinInitials;
        this.pinyinOverride = other.pinyinOverride;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getMobile() {
        return mobile;
    }

    public void setMobile(String mobile) {
        this.mobile = mobile;
    }

    public String getImType() {
        return imType;
    }

    public void setImType(String imType) {
        this.imType = imType;
    }

    public String getImNumber() {
        return imNumber;
    }

    public void setImNumber(String imNumber) {
        this.imNumber = imNumber;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getWebsite() {
        return website;
    }

    public void setWebsite(String website) {
        this.website = website;
    }

    public LocalDate getBirthday() {
        return birthday;
    }

    public void setBirthday(LocalDate birthday) {
        this.birthday = birthday;
    }

    public String getPhotoPath() {
        return photoPath;
    }

    public void setPhotoPath(String photoPath) {
        this.photoPath = photoPath;
    }

    public String getCompany() {
        return company;
    }

    public void setCompany(String company) {
        this.company = company;
    }

    public String getHomeAddress() {
        return homeAddress;
    }

    public void setHomeAddress(String homeAddress) {
        this.homeAddress = homeAddress;
    }

    public String getPostalCode() {
        return postalCode;
    }

    public void setPostalCode(String postalCode) {
        this.postalCode = postalCode;
    }

    public Set<String> getGroupIds() {
        return groupIds;
    }

    public void setGroupIds(Set<String> groupIds) {
        this.groupIds = groupIds;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public String getPinyinFull() {
        return pinyinFull;
    }

    public void setPinyinFull(String pinyinFull) {
        this.pinyinFull = pinyinFull;
    }

    public String getPinyinInitials() {
        return pinyinInitials;
    }

    public void setPinyinInitials(String pinyinInitials) {
        this.pinyinInitials = pinyinInitials;
    }

    public String getPinyinOverride() {
        return pinyinOverride;
    }

    public void setPinyinOverride(String pinyinOverride) {
        this.pinyinOverride = pinyinOverride;
    }
}

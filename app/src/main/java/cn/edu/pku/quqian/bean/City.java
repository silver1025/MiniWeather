package cn.edu.pku.quqian.bean;

//城市类
public class City {
    //省份
    private String province;
    //城市名
    private String city;
    //cityCode
    private String number;
    //城市名第一个字拼音的首字母
    private String firstPY;
    //城市名的拼音
    private String allPY;
    //城市名所有字拼音的首字母
    private String allFristPY;
    public City(String province, String city, String number, String
            firstPY, String allPY, String allFristPY) {
        this.province = province;
        this.city = city;
        this.number = number;
        this.firstPY = firstPY;
        this.allPY = allPY;
        this.allFristPY = allFristPY;
    }

    public void setProvince(String province) {
        this.province = province;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public void setNumber(String number) {
        this.number = number;
    }

    public void setFirstPY(String firstPY) {
        this.firstPY = firstPY;
    }

    public void setAllPY(String allPY) {
        this.allPY = allPY;
    }

    public void setAllFristPY(String allFristPY) {
        this.allFristPY = allFristPY;
    }

    public String getProvince() {

        return province;
    }

    public String getCity() {
        return city;
    }

    public String getNumber() {
        return number;
    }

    public String getFirstPY() {
        return firstPY;
    }

    public String getAllPY() {
        return allPY;
    }

    public String getAllFristPY() {
        return allFristPY;
    }
}

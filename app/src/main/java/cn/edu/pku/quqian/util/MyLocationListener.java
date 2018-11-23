package cn.edu.pku.quqian.util;

import android.util.Log;

import com.baidu.location.BDLocation;
import com.baidu.location.BDLocationListener;

import cn.edu.pku.quqian.miniweather.MainActivity;

public class MyLocationListener implements BDLocationListener {
    String city = null;

    public String getCity() {
        return city;
    }

    @Override
    public void onReceiveLocation(BDLocation location) {
        //此处的BDLocation为定位结果信息类，通过它的各种get方法可获取定位相关的全部结果
        //以下只列举部分获取地址相关的结果信息
        //更多结果信息获取说明，请参照类参考中BDLocation类中的说明

        String addr = location.getAddrStr();    //获取详细地址信息
        Log.d("myWeather", "获取详细地址信息：" + addr);
        String country = location.getCountry();    //获取国家
        Log.d("myWeather", "获取国家：" + country);
        String province = location.getProvince();    //获取省份
        Log.d("myWeather", "获取省份：" + province);
        city = location.getCity();    //获取城市
        Log.d("myWeather", "获取城市：" + city);
        String district = location.getDistrict();    //获取区县
        Log.d("myWeather", "获取区县：" + district);
        String street = location.getStreet();    //获取街道信息
        Log.d("myWeather", "获取街道信息：" + street);
        //定位成功后便停止
        if(!city.isEmpty()) {
            MainActivity.mLocationClient.stop();
        }
    }
}
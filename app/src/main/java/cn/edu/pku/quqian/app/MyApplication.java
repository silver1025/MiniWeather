package cn.edu.pku.quqian.app;

import android.app.Application;
import android.content.SharedPreferences;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import cn.edu.pku.quqian.bean.City;
import cn.edu.pku.quqian.db.CityDB;

public class MyApplication extends Application {
    private static final String TAG = "MyAPP";
    //静态实例化
    private static MyApplication mApplication;
    private SharedPreferences sharedPreferences;
    private CityDB mCityDB;
    private ArrayList<City> mCityList;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "MyApplication->Oncreate");
        mApplication = this;
        //打开数据库
        mCityDB = openCityDB();
        //获取SharedPreferences对象
        sharedPreferences = getSharedPreferences("config", MODE_PRIVATE);
        //初始化城市列表
        initCityList();
    }

    private void initCityList() {
        mCityList = new ArrayList<City>();
        //启动新线程去准备城市列表
        new Thread(new Runnable() {
            @Override
            public void run() {
// TODO Auto-generated method stub
                prepareCityList();
            }
        }).start();
    }

    //准备城市列表
    private boolean prepareCityList() {
        //获取城市信息列表
        mCityList = mCityDB.getAllCity();
        int i = 0;
        //打印所有cityCode以及城市名
        for (City city : mCityList) {
            i++;
            String cityName = city.getCity();
            String cityCode = city.getNumber();
            Log.d(TAG, cityCode + ":" + cityName);
        }
        Log.d(TAG, "i=" + i);
        return true;
    }

    //获取城市列表
    public ArrayList<City> getCityList() {
        return mCityList;
    }

    //获取MyApplication实例
    public static MyApplication getInstance() {
        return mApplication;
    }

    //获取sharedPreferences中存储的信息
    public String getString(String name, String defaultValue) {
        return mApplication.sharedPreferences.getString(name, defaultValue);
    }

    //更新sharedPreferences中的信息
    public boolean putString(String name, String value) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(name, value);
        editor.commit();
        return true;

    }

    //打开数据库
    private CityDB openCityDB() {
        //拼接数据库路径
        String path = "/data"
                + Environment.getDataDirectory().getAbsolutePath()
                + File.separator + getPackageName()
                + File.separator + "databases1"
                + File.separator
                + CityDB.CITY_DB_NAME;
        File db = new File(path);
        Log.d(TAG, path);
        if (!db.exists()) {
            String pathfolder = "/data"
                    + Environment.getDataDirectory().getAbsolutePath()
                    + File.separator + getPackageName()
                    + File.separator + "databases1"
                    + File.separator;
            File dirFirstFolder = new File(pathfolder);
            if (!dirFirstFolder.exists()) {
                dirFirstFolder.mkdirs();
                Log.i("MyApp", "mkdirs");
            }
            Log.i("MyApp", "db is not exists");
            try {
                InputStream is = getAssets().open("city.db");
                FileOutputStream fos = new FileOutputStream(db);
                int len = -1;
                byte[] buffer = new byte[1024];
                while ((len = is.read(buffer)) != -1) {
                    fos.write(buffer, 0, len);
                    fos.flush();
                }
                fos.close();
                is.close();
            } catch (IOException e) {
                e.printStackTrace();
                System.exit(0);
            }
        }
        return new CityDB(this, path);
    }
}

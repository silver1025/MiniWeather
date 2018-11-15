package cn.edu.pku.quqian.miniweather;

import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import cn.edu.pku.quqian.app.MyApplication;
import cn.edu.pku.quqian.bean.TodayWeather;
import cn.edu.pku.quqian.bean.ViewPagerAdapter;
import cn.edu.pku.quqian.util.NetUtil;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, ViewPager.OnPageChangeListener {
    private static final int UPDATE_TODAY_WEATHER = 1;
    private ImageView mUpdateBtn, mCitySelect, weatherImg, pmImg;
    private TextView cityTv, timeTv, humidityTv, weekTv, pmDataTv, pmQualityTv,
            temperatureTv, climateTv, windTv, city_name_Tv, now_temperature_Tv;
    private ProgressBar updateProgress;
    //六日天气信息
    private ImageView weather_img_day1, weather_img_day2, weather_img_day3,
            weather_img_day4, weather_img_day5, weather_img_day6;
    private TextView week_day1, temperature_day1, climate_day1, wind_day1,
            week_day2, temperature_day2, climate_day2, wind_day2,
            week_day3, temperature_day3, climate_day3, wind_day3,
            week_day4, temperature_day4, climate_day4, wind_day4,
            week_day5, temperature_day5, climate_day5, wind_day5,
            week_day6, temperature_day6, climate_day6, wind_day6;
    private ViewPagerAdapter vpAdapter;
    private ViewPager vp;
    private ArrayList<View> views;
    //小圆点
    private ImageView[] dots;
    private int[] ids = {R.id.iv1, R.id.iv2};

    //主线程中增加handler，处理子线程传回的天气信息
    private Handler mHandler = new Handler() {
        public void handleMessage(android.os.Message msg) {
            switch (msg.what) {
                case UPDATE_TODAY_WEATHER:
                    //更新今日天气
                    updateTodayWeather((TodayWeather) msg.obj);
                    break;
                default:
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.weather_info);
        //将控件与布局中的控件一一对应
        mUpdateBtn = (ImageView) findViewById(R.id.title_update_btn);
        mUpdateBtn.setOnClickListener(this);
        mCitySelect = (ImageView) findViewById(R.id.title_city_manager);
        mCitySelect.setOnClickListener(this);
        updateProgress = (ProgressBar) findViewById(R.id.title_update_progress);
        //初始化两个滑动页面
        initViews();
        //初始化小圆点
        initDots();
        //初始化视图
        initView();
    }

    @Override
    //点击后的响应事件
    public void onClick(View view) {
        //点击选择城市button
        if (view.getId() == R.id.title_city_manager) {
            //启动选择城市Activity，并接收返回的数据
            Intent i = new Intent(this, SelectCity.class);
            startActivityForResult(i, 1);
        }
        //点击更新button
        if (view.getId() == R.id.title_update_btn) {
            //获取sharedPreferences中的cityCode
            String cityCode = MyApplication.getInstance().getString("cityCode", "101010100");
            Log.d("myWeather", cityCode);
            if (NetUtil.getNetworkState(this) != NetUtil.NETWORN_NONE) {
                //如果网络OK
                Log.d("myWeather", "网络OK");
                //查询并更新天气信息
                queryWeatherCode(cityCode);
//              Toast.makeText(MainActivity.this,"网络OK！", Toast.LENGTH_LONG).show();
            } else {
                //如果网络异常，弹出文字提示
                Log.d("myWeather", "网络挂了");
                Toast.makeText(MainActivity.this, "网络挂了！", Toast.LENGTH_LONG).show();
            }
        }
    }

    //主线程实现处理子线程返回数据的函数
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 1 && resultCode == RESULT_OK) {
            String newCityCode;
            if (data.getStringExtra("cityCode").equals("")) {
                //如果返回的cityCode为空，newCityCode为sharedPreferences中原来的cityCode
                newCityCode = MyApplication.getInstance().getString("cityCode", "101010100");
            } else {
                //不为空
                newCityCode = data.getStringExtra("cityCode");
            }
            //更新sharedPreferences中的cityCode
            MyApplication.getInstance().putString("cityCode", newCityCode);
            Log.d("myWeather", "选择的城市代码为" + newCityCode);
            if (NetUtil.getNetworkState(this) != NetUtil.NETWORN_NONE) {
                Log.d("myWeather", "网络OK");
                //查询并更新天气信息
                queryWeatherCode(newCityCode);
            } else {
                Log.d("myWeather", "网络挂了");
                Toast.makeText(MainActivity.this, "网络挂了！", Toast.LENGTH_LONG).show();
            }
        }
    }

    //查询天气信息的实现函数
    private void queryWeatherCode(String cityCode) {
        //使ProgressBar可见，更新button不可见
        updateProgress.setVisibility(View.VISIBLE);
        mUpdateBtn.setVisibility(View.GONE);
        //拼接获取天气信息的网址
        final String address = "http://wthrcdn.etouch.cn/WeatherApi?citykey=" + cityCode;
        Log.d("myWeather", address);
        //启动子线程获取天气信息
        new Thread(new Runnable() {
            @Override
            public void run() {
                HttpURLConnection con = null;
                TodayWeather todayWeather = null;
                try {
                    //定义URL地址
                    URL url = new URL(address);
                    //通过地址打开连接
                    con = (HttpURLConnection) url.openConnection();
                    //设置请求模式为get
                    con.setRequestMethod("GET");
                    //设置连接超时 毫秒
                    con.setConnectTimeout(8000);
                    //设置读取超时 毫秒
                    con.setReadTimeout(8000);
                    //获得网络返回的输入流
                    InputStream in = con.getInputStream();
                    //设置缓冲区
                    BufferedReader reader = new BufferedReader(new InputStreamReader(in));
                    StringBuilder response = new StringBuilder();
                    String str;
                    while ((str = reader.readLine()) != null) {
                        //读取输入流中的每一行字符，并复制到response中
                        response.append(str);
                        Log.d("myWeather", str);
                    }
                    String responseStr = response.toString();
                    Log.d("myWeather", responseStr);
                    //解析输入流中的信息
                    todayWeather = parseXML(responseStr);
                    if (todayWeather != null) {
                        Log.d("myWeather", todayWeather.toString());
                        //通过进程间通信将解析得到的天气信息返回主线程，主线程会自动调用Handler更新视图
                        Message msg = new Message();
                        msg.what = UPDATE_TODAY_WEATHER;
                        msg.obj = todayWeather;
                        mHandler.sendMessage(msg);
                    }
                } catch (Exception e) {
                    //抛出异常
                    e.printStackTrace();
                } finally {
                    if (con != null) {
                        //关闭网络连接
                        con.disconnect();
                    }
                }
            }
        }).start();
        //更新完毕，使ProgressBar不可见，更新button可见
        mUpdateBtn.setVisibility(View.VISIBLE);
        updateProgress.setVisibility(View.GONE);
    }

    //解析XML信息的函数
    private TodayWeather parseXML(String xmldata) {
        TodayWeather todayWeather = null;
        //初始化计数器，只记录第一次遇到的相应信息，即今日天气信息
        int fengxiangCount = 0;
        int fengliCount = 0;
        int dateCount = 0;
        int highCount = 0;
        int lowCount = 0;
        int typeCount = 0;
        try {
            XmlPullParserFactory fac = XmlPullParserFactory.newInstance();
            XmlPullParser xmlPullParser = fac.newPullParser();
            xmlPullParser.setInput(new StringReader(xmldata));
            int eventType = xmlPullParser.getEventType();
            Log.d("myWeather", "parseXML");
            while (eventType != XmlPullParser.END_DOCUMENT) {
                switch (eventType) {
                    // 判断当前事件是否为文档开始事件
                    case XmlPullParser.START_DOCUMENT:
                        break;
                    // 判断当前事件是否为标签元素开始事件
                    case XmlPullParser.START_TAG:
                        if (xmlPullParser.getName().equals("resp")) {
                            todayWeather = new TodayWeather();
                        }
                        if (todayWeather != null) {
                            if (xmlPullParser.getName().equals("city")) {
                                eventType = xmlPullParser.next();
                                todayWeather.setCity(xmlPullParser.getText());
                                Log.d("myWeather", "city: " + xmlPullParser.getText());
                            } else if (xmlPullParser.getName().equals("updatetime")) {
                                eventType = xmlPullParser.next();
                                todayWeather.setUpdatetime(xmlPullParser.getText());
                                Log.d("myWeather", "updatetime: " + xmlPullParser.getText());
                            } else if (xmlPullParser.getName().equals("shidu")) {
                                eventType = xmlPullParser.next();
                                todayWeather.setShidu(xmlPullParser.getText());
                                Log.d("myWeather", "shidu: " + xmlPullParser.getText());
                            } else if (xmlPullParser.getName().equals("wendu")) {
                                eventType = xmlPullParser.next();
                                todayWeather.setWendu(xmlPullParser.getText());
                                Log.d("myWeather", "wendu: " + xmlPullParser.getText());
                            } else if (xmlPullParser.getName().equals("pm25")) {
                                eventType = xmlPullParser.next();
                                todayWeather.setPm25(xmlPullParser.getText());
                                Log.d("myWeather", "pm25: " + xmlPullParser.getText());
                            } else if (xmlPullParser.getName().equals("quality")) {
                                eventType = xmlPullParser.next();
                                todayWeather.setQuality(xmlPullParser.getText());
                                Log.d("myWeather", "quality: " + xmlPullParser.getText());
                            } else if (xmlPullParser.getName().equals("fengxiang") && fengxiangCount == 0) {
                                eventType = xmlPullParser.next();
                                todayWeather.setFengxiang(xmlPullParser.getText());
                                Log.d("myWeather", "fengxiang: " + xmlPullParser.getText());
                                fengxiangCount++;
                            } else if (xmlPullParser.getName().equals("fengli")) {
                                eventType = xmlPullParser.next();
                                switch (fengliCount) {
                                    case 0:
                                        todayWeather.setFengli(xmlPullParser.getText());
                                        todayWeather.setWind_day2(xmlPullParser.getText());
                                        break;
                                    case 1:
                                        todayWeather.setWind_day3(xmlPullParser.getText());
                                        break;
                                    case 2:
                                        todayWeather.setWind_day4(xmlPullParser.getText());
                                        break;
                                    case 3:
                                        todayWeather.setWind_day5(xmlPullParser.getText());
                                        break;
                                    case 4:
                                        todayWeather.setWind_day6(xmlPullParser.getText());
                                        break;
                                    default:
                                        break;
                                }
                                Log.d("myWeather", "fengli: " + xmlPullParser.getText());
                                fengliCount++;
                            } else if (xmlPullParser.getName().equals("fl_1")) {
                                eventType = xmlPullParser.next();
                                todayWeather.setWind_day1(xmlPullParser.getText());
                                Log.d("myWeather", "fengli: " + xmlPullParser.getText());
                            } else if (xmlPullParser.getName().equals("date")) {
                                eventType = xmlPullParser.next();
                                switch (dateCount) {
                                    case 0:
                                        todayWeather.setDate(xmlPullParser.getText());
                                        todayWeather.setWeek_day2(xmlPullParser.getText());
                                        break;
                                    case 1:
                                        todayWeather.setWeek_day3(xmlPullParser.getText());
                                        break;
                                    case 2:
                                        todayWeather.setWeek_day4(xmlPullParser.getText());
                                        break;
                                    case 3:
                                        todayWeather.setWeek_day5(xmlPullParser.getText());
                                        break;
                                    case 4:
                                        todayWeather.setWeek_day6(xmlPullParser.getText());
                                        break;
                                    default:
                                        break;
                                }
                                Log.d("myWeather", "date: " + xmlPullParser.getText());
                                dateCount++;
                            } else if (xmlPullParser.getName().equals("date_1")) {
                                eventType = xmlPullParser.next();
                                todayWeather.setWeek_day1(xmlPullParser.getText());
                                Log.d("myWeather", "date: " + xmlPullParser.getText());
                            } else if (xmlPullParser.getName().equals("high")) {
                                eventType = xmlPullParser.next();
                                switch (highCount) {
                                    case 0:
                                        todayWeather.setHigh(findNumber(xmlPullParser.getText()));
                                        todayWeather.setHigh_day2(findNumber(xmlPullParser.getText()));
                                        break;
                                    case 1:
                                        todayWeather.setHigh_day3(findNumber(xmlPullParser.getText()));
                                        break;
                                    case 2:
                                        todayWeather.setHigh_day4(findNumber(xmlPullParser.getText()));
                                        break;
                                    case 3:
                                        todayWeather.setHigh_day5(findNumber(xmlPullParser.getText()));
                                        break;
                                    case 4:
                                        todayWeather.setHigh_day6(findNumber(xmlPullParser.getText()));
                                        break;
                                    default:
                                        break;
                                }
                                Log.d("myWeather", "high: " + xmlPullParser.getText());
                                highCount++;
                            } else if (xmlPullParser.getName().equals("high_1")) {
                                eventType = xmlPullParser.next();
                                todayWeather.setHigh_day1(findNumber(xmlPullParser.getText()));
                                Log.d("myWeather", "high: " + xmlPullParser.getText());
                            } else if (xmlPullParser.getName().equals("low")) {
                                eventType = xmlPullParser.next();
                                switch (lowCount) {
                                    case 0:
                                        todayWeather.setLow(findNumber(xmlPullParser.getText()));
                                        todayWeather.setLow_day2(findNumber(xmlPullParser.getText()));
                                        break;
                                    case 1:
                                        todayWeather.setLow_day3(findNumber(xmlPullParser.getText()));
                                        break;
                                    case 2:
                                        todayWeather.setLow_day4(findNumber(xmlPullParser.getText()));
                                        break;
                                    case 3:
                                        todayWeather.setLow_day5(findNumber(xmlPullParser.getText()));
                                        break;
                                    case 4:
                                        todayWeather.setLow_day6(findNumber(xmlPullParser.getText()));
                                        break;
                                    default:
                                        break;
                                }
                                Log.d("myWeather", "low: " + xmlPullParser.getText());
                                lowCount++;
                            } else if (xmlPullParser.getName().equals("low_1")) {
                                eventType = xmlPullParser.next();
                                todayWeather.setLow_day1(findNumber(xmlPullParser.getText()));
                                Log.d("myWeather", "low: " + xmlPullParser.getText());
                            } else if (xmlPullParser.getName().equals("type")) {
                                eventType = xmlPullParser.next();
                                switch (typeCount) {
                                    case 0:
                                        todayWeather.setType(xmlPullParser.getText());
                                        todayWeather.setClimate_day2(xmlPullParser.getText());
                                        break;
                                    case 1:
                                        todayWeather.setClimate_day3(xmlPullParser.getText());
                                        break;
                                    case 2:
                                        todayWeather.setClimate_day4(xmlPullParser.getText());
                                        break;
                                    case 3:
                                        todayWeather.setClimate_day5(xmlPullParser.getText());
                                        break;
                                    case 4:
                                        todayWeather.setClimate_day6(xmlPullParser.getText());
                                        break;
                                    default:
                                        break;
                                }
                                Log.d("myWeather", "type: " + xmlPullParser.getText());
                                typeCount++;
                            } else if (xmlPullParser.getName().equals("type_1")) {
                                eventType = xmlPullParser.next();
                                todayWeather.setClimate_day1(xmlPullParser.getText());
                                Log.d("myWeather", "type: " + xmlPullParser.getText());
                            }
                        }
                        break;
                    // 判断当前事件是否为标签元素结束事件
                    case XmlPullParser.END_TAG:
                        break;
                }
                // 进入下一个元素并触发相应事件
                eventType = xmlPullParser.next();
            }
        } catch (XmlPullParserException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return todayWeather;
    }

    //通过正则表达式筛选出字符串中的数字
    private String findNumber(String str) {
        String regEx = "[^0-9]";
        Pattern p = Pattern.compile(regEx);
        Matcher m = p.matcher(str);
        return m.replaceAll("").trim();
    }

    //初始化视图
    void initView() {
        //将控件一一对应
        city_name_Tv = (TextView) findViewById(R.id.title_city_name);
        cityTv = (TextView) findViewById(R.id.city);
        timeTv = (TextView) findViewById(R.id.time);
        humidityTv = (TextView) findViewById(R.id.humidity);
        weekTv = (TextView) findViewById(R.id.week_today);
        pmDataTv = (TextView) findViewById(R.id.pm_data);
        pmQualityTv = (TextView) findViewById(R.id.pm2_5_quality);
        pmImg = (ImageView) findViewById(R.id.pm2_5_img);
        temperatureTv = (TextView) findViewById(R.id.temperature);
        now_temperature_Tv = (TextView) findViewById(R.id.now_temperature);
        climateTv = (TextView) findViewById(R.id.climate);
        windTv = (TextView) findViewById(R.id.wind);
        weatherImg = (ImageView) findViewById(R.id.weather_img);

        week_day1 = views.get(0).findViewById(R.id.week_day1);
        temperature_day1 = views.get(0).findViewById(R.id.temperature_day1);
        climate_day1 = views.get(0).findViewById(R.id.climate_day1);
        wind_day1 = views.get(0).findViewById(R.id.wind_day1);
        weather_img_day1 = views.get(0).findViewById(R.id.weather_img_day1);

        week_day2 = views.get(0).findViewById(R.id.week_day2);
        temperature_day2 = views.get(0).findViewById(R.id.temperature_day2);
        climate_day2 = views.get(0).findViewById(R.id.climate_day2);
        wind_day2 = views.get(0).findViewById(R.id.wind_day2);
        weather_img_day2 = views.get(0).findViewById(R.id.weather_img_day2);

        week_day3 = views.get(0).findViewById(R.id.week_day3);
        temperature_day3 = views.get(0).findViewById(R.id.temperature_day3);
        climate_day3 = views.get(0).findViewById(R.id.climate_day3);
        wind_day3 = views.get(0).findViewById(R.id.wind_day3);
        weather_img_day3 = views.get(0).findViewById(R.id.weather_img_day3);

        week_day4 = views.get(1).findViewById(R.id.week_day4);
        temperature_day4 = views.get(1).findViewById(R.id.temperature_day4);
        climate_day4 = views.get(1).findViewById(R.id.climate_day4);
        wind_day4 = views.get(1).findViewById(R.id.wind_day4);
        weather_img_day4 = views.get(1).findViewById(R.id.weather_img_day4);

        week_day5 = views.get(1).findViewById(R.id.week_day5);
        temperature_day5 = views.get(1).findViewById(R.id.temperature_day5);
        climate_day5 = views.get(1).findViewById(R.id.climate_day5);
        wind_day5 = views.get(1).findViewById(R.id.wind_day5);
        weather_img_day5 = views.get(1).findViewById(R.id.weather_img_day5);

        week_day6 = views.get(1).findViewById(R.id.week_day6);
        temperature_day6 = views.get(1).findViewById(R.id.temperature_day6);
        climate_day6 = views.get(1).findViewById(R.id.climate_day6);
        wind_day6 = views.get(1).findViewById(R.id.wind_day6);
        weather_img_day6 = views.get(1).findViewById(R.id.weather_img_day6);
        //将控件内容都初始化为N/A
        city_name_Tv.setText("N/A");
        cityTv.setText("N/A");
        timeTv.setText("N/A");
        humidityTv.setText("N/A");
        pmDataTv.setText("N/A");
        pmQualityTv.setText("N/A");
        weekTv.setText("N/A");
        temperatureTv.setText("N/A");
        now_temperature_Tv.setText("N/A");
        climateTv.setText("N/A");
        windTv.setText("N/A");

        week_day1.setText("N/A");
        temperature_day1.setText("N/A");
        climate_day1.setText("N/A");
        wind_day1.setText("N/A");

        week_day2.setText("N/A");
        temperature_day2.setText("N/A");
        climate_day2.setText("N/A");
        wind_day2.setText("N/A");

        week_day3.setText("N/A");
        temperature_day3.setText("N/A");
        climate_day3.setText("N/A");
        wind_day3.setText("N/A");

        week_day4.setText("N/A");
        temperature_day4.setText("N/A");
        climate_day4.setText("N/A");
        wind_day4.setText("N/A");

        week_day5.setText("N/A");
        temperature_day5.setText("N/A");
        climate_day5.setText("N/A");
        wind_day5.setText("N/A");

        week_day6.setText("N/A");
        temperature_day6.setText("N/A");
        climate_day6.setText("N/A");
        wind_day6.setText("N/A");
    }

    //初始化两个滑动页面
    private void initViews() {
        LayoutInflater inflater = LayoutInflater.from(this);
        views = new ArrayList<View>();
        views.add(inflater.inflate(R.layout.sixday1, null));
        views.add(inflater.inflate(R.layout.sixday2, null));
        vpAdapter = new ViewPagerAdapter(views);
        vp = (ViewPager) findViewById(R.id.viewpager);
        vp.setAdapter(vpAdapter);
        //为pageviewer配置监听事件
        vp.setOnPageChangeListener(this);
    }

    //初始化校⼩小圆点
    private void initDots() {
        dots = new ImageView[views.size()];
        for (int i = 0; i < views.size(); i++) {
            dots[i] = (ImageView) findViewById(ids[i]);
        }
    }


    @Override
    public void onPageScrolled(int position, float positionOffset, int
            positionOffsetPixels) {
    }

    @Override
    public void onPageSelected(int position) {
        for (int a = 0; a < ids.length; a++) {
            if (a == position) {
                dots[a].setImageResource(R.drawable.page_indicator_focused);
            } else {
                dots[a].setImageResource(R.drawable.page_indicator_unfocused);
            }
        }
    }

    @Override
    public void onPageScrollStateChanged(int state) {
    }

    //更新天气信息的实现函数
    private void updateTodayWeather(TodayWeather todayWeather) {
        city_name_Tv.setText(todayWeather.getCity() + "天气");
        cityTv.setText(todayWeather.getCity());
        timeTv.setText(todayWeather.getUpdatetime() + "发布");
        humidityTv.setText("湿度：" + todayWeather.getShidu());
        pmDataTv.setText(todayWeather.getPm25());
        pmQualityTv.setText(todayWeather.getQuality());
        weekTv.setText(todayWeather.getDate());
        temperatureTv.setText(todayWeather.getLow() + "℃~" + todayWeather.getHigh() + "℃");
        now_temperature_Tv.setText("实时：" + todayWeather.getWendu() + "℃");
        climateTv.setText(todayWeather.getType());
        windTv.setText("风力:" + todayWeather.getFengli());

        week_day1.setText(todayWeather.getWeek_day1());
        temperature_day1.setText(todayWeather.getLow_day1() + "℃~" + todayWeather.getHigh_day1() + "℃");
        wind_day1.setText("风力:" + todayWeather.getWind_day1());
        climate_day1.setText(todayWeather.getClimate_day1());

        week_day2.setText(todayWeather.getWeek_day2());
        temperature_day2.setText(todayWeather.getLow_day2() + "℃~" + todayWeather.getHigh_day2() + "℃");
        wind_day2.setText("风力:" + todayWeather.getWind_day2());
        climate_day2.setText(todayWeather.getClimate_day2());

        week_day3.setText(todayWeather.getWeek_day3());
        temperature_day3.setText(todayWeather.getLow_day3() + "℃~" + todayWeather.getHigh_day3() + "℃");
        wind_day3.setText("风力:" + todayWeather.getWind_day3());
        climate_day3.setText(todayWeather.getClimate_day3());

        week_day4.setText(todayWeather.getWeek_day4());
        temperature_day4.setText(todayWeather.getLow_day4() + "℃~" + todayWeather.getHigh_day4() + "℃");
        wind_day4.setText("风力:" + todayWeather.getWind_day4());
        climate_day4.setText(todayWeather.getClimate_day4());

        week_day5.setText(todayWeather.getWeek_day5());
        temperature_day5.setText(todayWeather.getLow_day5() + "℃~" + todayWeather.getHigh_day5() + "℃");
        wind_day5.setText("风力:" + todayWeather.getWind_day5());
        climate_day5.setText(todayWeather.getClimate_day5());

        week_day6.setText(todayWeather.getWeek_day6());
        temperature_day6.setText(todayWeather.getLow_day6() + "℃~" + todayWeather.getHigh_day6() + "℃");
        wind_day6.setText("风力:" + todayWeather.getWind_day6());
        climate_day6.setText(todayWeather.getClimate_day6());

        //城市有pm2.5信息时，根据pm2.5值更新相应的图片
        if (!todayWeather.getPm25().equals("N/A")) {
            //通过整除得到的结果判定类型
            int pmImgType = (Integer.parseInt(todayWeather.getPm25()) - 1) / 50;
            //解决pm2.5为0时的特殊情况
            pmImgType = pmImgType > 0 ? pmImgType : 0;
            switch (pmImgType) {
                case 0:
                    pmImg.setImageDrawable(ContextCompat.getDrawable(getApplicationContext(), R.drawable.biz_plugin_weather_0_50));
                    break;
                case 1:
                    pmImg.setImageDrawable(ContextCompat.getDrawable(getApplicationContext(), R.drawable.biz_plugin_weather_51_100));
                    break;
                case 2:
                    pmImg.setImageDrawable(ContextCompat.getDrawable(getApplicationContext(), R.drawable.biz_plugin_weather_101_150));
                    break;
                case 3:
                    pmImg.setImageDrawable(ContextCompat.getDrawable(getApplicationContext(), R.drawable.biz_plugin_weather_151_200));
                    break;
                case 4:
                case 5:
                    pmImg.setImageDrawable(ContextCompat.getDrawable(getApplicationContext(), R.drawable.biz_plugin_weather_201_300));
                    break;
                default:
                    pmImg.setImageDrawable(ContextCompat.getDrawable(getApplicationContext(), R.drawable.biz_plugin_weather_greater_300));
                    break;
            }
        }

        //城市有天气类型信息时，更新相应的图片
        changeWeatherImg(todayWeather.getType(), weatherImg);
        changeWeatherImg(todayWeather.getClimate_day1(), weather_img_day1);
        changeWeatherImg(todayWeather.getClimate_day2(), weather_img_day2);
        changeWeatherImg(todayWeather.getClimate_day3(), weather_img_day3);
        changeWeatherImg(todayWeather.getClimate_day4(), weather_img_day4);
        changeWeatherImg(todayWeather.getClimate_day5(), weather_img_day5);
        changeWeatherImg(todayWeather.getClimate_day6(), weather_img_day6);

        Toast.makeText(MainActivity.this, "更新成功！", Toast.LENGTH_SHORT).show();
    }

    private void changeWeatherImg(String weatherImgType, ImageView weatherImg) {
        switch (weatherImgType) {
            case "晴":
                weatherImg.setImageDrawable(ContextCompat.getDrawable(getApplicationContext(), R.drawable.biz_plugin_weather_qing));
                break;
            case "暴雪":
                weatherImg.setImageDrawable(ContextCompat.getDrawable(getApplicationContext(), R.drawable.biz_plugin_weather_baoxue));
                break;
            case "暴雨":
                weatherImg.setImageDrawable(ContextCompat.getDrawable(getApplicationContext(), R.drawable.biz_plugin_weather_baoyu));
                break;
            case "大暴雨":
                weatherImg.setImageDrawable(ContextCompat.getDrawable(getApplicationContext(), R.drawable.biz_plugin_weather_dabaoyu));
                break;
            case "大雪":
                weatherImg.setImageDrawable(ContextCompat.getDrawable(getApplicationContext(), R.drawable.biz_plugin_weather_daxue));
                break;
            case "大雨":
                weatherImg.setImageDrawable(ContextCompat.getDrawable(getApplicationContext(), R.drawable.biz_plugin_weather_dayu));
                break;
            case "多云":
                weatherImg.setImageDrawable(ContextCompat.getDrawable(getApplicationContext(), R.drawable.biz_plugin_weather_duoyun));
                break;
            case "雷阵雨":
                weatherImg.setImageDrawable(ContextCompat.getDrawable(getApplicationContext(), R.drawable.biz_plugin_weather_leizhenyu));
                break;
            case "雷阵雨冰雹":
                weatherImg.setImageDrawable(ContextCompat.getDrawable(getApplicationContext(), R.drawable.biz_plugin_weather_leizhenyubingbao));
                break;
            case "沙尘暴":
                weatherImg.setImageDrawable(ContextCompat.getDrawable(getApplicationContext(), R.drawable.biz_plugin_weather_shachenbao));
                break;
            case "特大暴雨":
                weatherImg.setImageDrawable(ContextCompat.getDrawable(getApplicationContext(), R.drawable.biz_plugin_weather_tedabaoyu));
                break;
            case "雾":
                weatherImg.setImageDrawable(ContextCompat.getDrawable(getApplicationContext(), R.drawable.biz_plugin_weather_wu));
                break;
            case "小雪":
                weatherImg.setImageDrawable(ContextCompat.getDrawable(getApplicationContext(), R.drawable.biz_plugin_weather_xiaoxue));
                break;
            case "小雨":
                weatherImg.setImageDrawable(ContextCompat.getDrawable(getApplicationContext(), R.drawable.biz_plugin_weather_xiaoyu));
                break;
            case "阴":
                weatherImg.setImageDrawable(ContextCompat.getDrawable(getApplicationContext(), R.drawable.biz_plugin_weather_yin));
                break;
            case "雨夹雪":
                weatherImg.setImageDrawable(ContextCompat.getDrawable(getApplicationContext(), R.drawable.biz_plugin_weather_yujiaxue));
                break;
            case "阵雪":
                weatherImg.setImageDrawable(ContextCompat.getDrawable(getApplicationContext(), R.drawable.biz_plugin_weather_zhenxue));
                break;
            case "阵雨":
                weatherImg.setImageDrawable(ContextCompat.getDrawable(getApplicationContext(), R.drawable.biz_plugin_weather_zhenyu));
                break;
            case "中雪":
                weatherImg.setImageDrawable(ContextCompat.getDrawable(getApplicationContext(), R.drawable.biz_plugin_weather_zhongxue));
                break;
            case "中雨":
                weatherImg.setImageDrawable(ContextCompat.getDrawable(getApplicationContext(), R.drawable.biz_plugin_weather_zhongyu));
                break;
            default:
                weatherImg.setImageDrawable(ContextCompat.getDrawable(getApplicationContext(), R.drawable.biz_plugin_weather_qing));
                break;
        }
        return;
    }

}

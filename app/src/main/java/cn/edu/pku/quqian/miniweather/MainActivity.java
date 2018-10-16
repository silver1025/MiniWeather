package cn.edu.pku.quqian.miniweather;

import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Message;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import cn.edu.pku.quqian.bean.TodayWeather;
import cn.edu.pku.quqian.util.NetUtil;

public class MainActivity extends AppCompatActivity implements View.OnClickListener{
    private static final int UPDATE_TODAY_WEATHER = 1;
    private ImageView mUpdateBtn;
    private TextView cityTv, timeTv, humidityTv, weekTv, pmDataTv, pmQualityTv,
            temperatureTv, climateTv, windTv, city_name_Tv,now_temperature_Tv;
    private ImageView weatherImg, pmImg;

    private Handler mHandler = new Handler() {
        public void handleMessage(android.os.Message msg) {
            switch (msg.what) {
                case UPDATE_TODAY_WEATHER:
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

        mUpdateBtn=(ImageView) findViewById(R.id.title_update_btn);
        mUpdateBtn.setOnClickListener(this);
        initView();
    }

    @Override
    public  void onClick(View view){
        if(view.getId()==R.id.title_update_btn){
            SharedPreferences sharedPreferences=getSharedPreferences("config",MODE_PRIVATE);
            String cityCode=sharedPreferences.getString("main_city_code","101010100");
            Log.d("myWeather",cityCode);

            if (NetUtil.getNetworkState(this) != NetUtil.NETWORN_NONE) {
                Log.d("myWeather", "网络OK");
                queryWeatherCode(cityCode);
//              Toast.makeText(MainActivity.this,"网络OK！", Toast.LENGTH_LONG).show();
            }else
            {
                Log.d("myWeather", "网络挂了");
                Toast.makeText(MainActivity.this,"网络挂了！", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void queryWeatherCode(String cityCode){
        final String address="http://wthrcdn.etouch.cn/WeatherApi?citykey="+cityCode;
        Log.d("myWeather",address);
        new Thread(new Runnable() {
            @Override
            public void run() {
                HttpURLConnection con=null;
                TodayWeather todayWeather=null;
                try{
                    URL url=new URL(address);
                    con=(HttpURLConnection)url.openConnection();
                    con.setRequestMethod("GET");
                    con.setConnectTimeout(8000);
                    con.setReadTimeout(8000);
                    InputStream in=con.getInputStream();
                    BufferedReader reader=new BufferedReader(new InputStreamReader(in));
                    StringBuilder response=new StringBuilder();
                    String str;
                    while((str=reader.readLine()) != null){
                        response.append(str);
                        Log.d("myWeather",str);
                    }
                    String responseStr=response.toString();
                    parseXML(responseStr);
                    Log.d("myWeather",responseStr);

                    todayWeather=parseXML(responseStr);
                    if(todayWeather!=null){
                        Log.d("myWeather",todayWeather.toString());
                        Message msg =new Message();
                        msg.what = UPDATE_TODAY_WEATHER;
                        msg.obj=todayWeather;
                        mHandler.sendMessage(msg);
                    }
                }catch (Exception e){
                    e.printStackTrace();
                }finally {
                    if(con!=null){
                        con.disconnect();
                    }
                }
            }
        }).start();
    }

    private TodayWeather parseXML(String xmldata) {
        TodayWeather todayWeather = null;
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
                        if(xmlPullParser.getName().equals("resp")){
                            todayWeather= new TodayWeather();
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
                            } else if (xmlPullParser.getName().equals("fengli") && fengliCount == 0) {
                                eventType = xmlPullParser.next();
                                todayWeather.setFengli(xmlPullParser.getText());
                                Log.d("myWeather", "fengli: " + xmlPullParser.getText());
                                fengliCount++;
                            } else if (xmlPullParser.getName().equals("date") && dateCount == 0) {
                                eventType = xmlPullParser.next();
                                todayWeather.setDate(xmlPullParser.getText());
                                Log.d("myWeather", "date: " + xmlPullParser.getText());
                                dateCount++;
                            } else if (xmlPullParser.getName().equals("high") && highCount == 0) {
                                eventType = xmlPullParser.next();
                                todayWeather.setHigh(findNumber(xmlPullParser.getText()));
                                Log.d("myWeather", "high: " + xmlPullParser.getText());
                                highCount++;
                            } else if (xmlPullParser.getName().equals("low") && lowCount == 0) {
                                eventType = xmlPullParser.next();
                                todayWeather.setLow(findNumber(xmlPullParser.getText()));
                                Log.d("myWeather", "low: " + xmlPullParser.getText());
                                lowCount++;
                            } else if (xmlPullParser.getName().equals("type") && typeCount == 0) {
                                eventType = xmlPullParser.next();
                                todayWeather.setType(xmlPullParser.getText());
                                Log.d("myWeather", "type: " + xmlPullParser.getText());
                                typeCount++;
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

    private String findNumber(String str){
        String regEx="[^0-9]";
        Pattern p=Pattern.compile(regEx);
        Matcher m=p.matcher(str);
        return m.replaceAll("").trim();
    }

    void initView(){
        city_name_Tv = (TextView) findViewById(R.id.title_city_name);
        cityTv = (TextView) findViewById(R.id.city);
        timeTv = (TextView) findViewById(R.id.time);
        humidityTv = (TextView) findViewById(R.id.humidity);
        weekTv = (TextView) findViewById(R.id.week_today);
        pmDataTv = (TextView) findViewById(R.id.pm_data);
        pmQualityTv = (TextView) findViewById(R.id.pm2_5_quality);
        pmImg = (ImageView) findViewById(R.id.pm2_5_img);
        temperatureTv = (TextView) findViewById(R.id.temperature);
        now_temperature_Tv=(TextView) findViewById(R.id.now_temperature);
        climateTv = (TextView) findViewById(R.id.climate);
        windTv = (TextView) findViewById(R.id.wind);
        weatherImg = (ImageView) findViewById(R.id.weather_img);
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
    }

    void updateTodayWeather(TodayWeather todayWeather){
        city_name_Tv.setText(todayWeather.getCity()+"天气");
        cityTv.setText(todayWeather.getCity());
        timeTv.setText(todayWeather.getUpdatetime()+ "发布");
        humidityTv.setText("湿度："+todayWeather.getShidu());
        pmDataTv.setText(todayWeather.getPm25());
        pmQualityTv.setText(todayWeather.getQuality());
        weekTv.setText(todayWeather.getDate());
        temperatureTv.setText(todayWeather.getLow()+"℃~"+todayWeather.getHigh()+"℃");
        now_temperature_Tv.setText("实时："+todayWeather.getWendu()+"℃");
        climateTv.setText(todayWeather.getType());
        windTv.setText("风力:"+todayWeather.getFengli());

        int pmImgType = (Integer.parseInt(todayWeather.getPm25()) - 1) / 50;
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
        String weatherImgType=todayWeather.getType();
        switch (weatherImgType){
            case "晴":weatherImg.setImageDrawable(ContextCompat.getDrawable(getApplicationContext(), R.drawable.biz_plugin_weather_qing));
            break;
            case "暴雪":weatherImg.setImageDrawable(ContextCompat.getDrawable(getApplicationContext(), R.drawable.biz_plugin_weather_baoxue));
                break;
            case "暴雨":weatherImg.setImageDrawable(ContextCompat.getDrawable(getApplicationContext(), R.drawable.biz_plugin_weather_baoyu));
                break;
            case "大暴雨":weatherImg.setImageDrawable(ContextCompat.getDrawable(getApplicationContext(), R.drawable.biz_plugin_weather_dabaoyu));
                break;
            case "大雪":weatherImg.setImageDrawable(ContextCompat.getDrawable(getApplicationContext(), R.drawable.biz_plugin_weather_daxue));
                break;
            case "大雨":weatherImg.setImageDrawable(ContextCompat.getDrawable(getApplicationContext(), R.drawable.biz_plugin_weather_dayu));
                break;
            case "多云":weatherImg.setImageDrawable(ContextCompat.getDrawable(getApplicationContext(), R.drawable.biz_plugin_weather_duoyun));
                break;
            case "雷阵雨":weatherImg.setImageDrawable(ContextCompat.getDrawable(getApplicationContext(), R.drawable.biz_plugin_weather_leizhenyu));
                break;
            case "雷阵雨冰雹":weatherImg.setImageDrawable(ContextCompat.getDrawable(getApplicationContext(), R.drawable.biz_plugin_weather_leizhenyubingbao));
                break;
            case "沙尘暴":weatherImg.setImageDrawable(ContextCompat.getDrawable(getApplicationContext(), R.drawable.biz_plugin_weather_shachenbao));
                break;
            case "特大暴雨":weatherImg.setImageDrawable(ContextCompat.getDrawable(getApplicationContext(), R.drawable.biz_plugin_weather_tedabaoyu));
                break;
            case "雾":weatherImg.setImageDrawable(ContextCompat.getDrawable(getApplicationContext(), R.drawable.biz_plugin_weather_wu));
                break;
            case "小雪":weatherImg.setImageDrawable(ContextCompat.getDrawable(getApplicationContext(), R.drawable.biz_plugin_weather_xiaoxue));
                break;
            case "小雨":weatherImg.setImageDrawable(ContextCompat.getDrawable(getApplicationContext(), R.drawable.biz_plugin_weather_xiaoyu));
                break;
            case "阴":weatherImg.setImageDrawable(ContextCompat.getDrawable(getApplicationContext(), R.drawable.biz_plugin_weather_yin));
                break;
            case "雨夹雪":weatherImg.setImageDrawable(ContextCompat.getDrawable(getApplicationContext(), R.drawable.biz_plugin_weather_yujiaxue));
                break;
            case "阵雪":weatherImg.setImageDrawable(ContextCompat.getDrawable(getApplicationContext(), R.drawable.biz_plugin_weather_zhenxue));
                break;
            case "阵雨":weatherImg.setImageDrawable(ContextCompat.getDrawable(getApplicationContext(), R.drawable.biz_plugin_weather_zhenyu));
                break;
            case "中雪":weatherImg.setImageDrawable(ContextCompat.getDrawable(getApplicationContext(), R.drawable.biz_plugin_weather_zhongxue));
                break;
            case "中雨":weatherImg.setImageDrawable(ContextCompat.getDrawable(getApplicationContext(), R.drawable.biz_plugin_weather_zhongyu));
                break;
            default:weatherImg.setImageDrawable(ContextCompat.getDrawable(getApplicationContext(), R.drawable.biz_plugin_weather_qing));
                break;


        }
        Toast.makeText(MainActivity.this,"更新成功！",Toast.LENGTH_SHORT).show();
    }

}

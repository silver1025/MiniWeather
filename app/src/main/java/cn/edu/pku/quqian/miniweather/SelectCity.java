package cn.edu.pku.quqian.miniweather;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import cn.edu.pku.quqian.bean.City;
import cn.edu.pku.quqian.app.MyApplication;

public class SelectCity extends AppCompatActivity implements View.OnClickListener {
    //返回
    private ImageView mBackBtn;
    //城市列表
    private ListView mListView;
    //查询输入框
    private EditText mEditText;
    //顶栏城市名
    private TextView title_name_Tv;
    //城市列表
    private ArrayList<City> mCityList = new ArrayList<>();
    //城市名
    private ArrayList<String> mCityNameList = new ArrayList<>();
    //选择的城市名与cityCode
    private String selectedCity;
    private String selectedCityCode;
    private ArrayAdapter<String> adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.select_city);
        //控件一一对应
        mBackBtn = (ImageView) findViewById(R.id.title_back);
        mListView = (ListView) findViewById(R.id.list_view);
        title_name_Tv = (TextView) findViewById(R.id.title_name);
        mEditText = (EditText)findViewById(R.id.search_edit);
        //返回键的监听器
        mBackBtn.setOnClickListener(this);
        //用已保存的CityCode初始化
        selectedCityCode = MyApplication.getInstance().getString("cityCode","");

        //用数据库中的信息初始化城市名以及城市列表
        for (City city : MyApplication.getInstance().getCityList()) {
            mCityNameList.add(city.getCity());
            mCityList.add(city);
        }
        //ListView的适配器
        adapter = new ArrayAdapter<String>(
                SelectCity.this, android.R.layout.simple_list_item_1, mCityNameList);
        mListView.setAdapter(adapter);
        //ListView单击选项时
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                selectedCity = mCityList.get(position).getCity();
                selectedCityCode = mCityList.get(position).getNumber();
                Toast.makeText(SelectCity.this, "你选择了：" +
                        selectedCity, Toast.LENGTH_SHORT).show();
                title_name_Tv.setText("当前城市：" + selectedCity);
            }
        });
        //输入框实时监控
        TextWatcher mTextWatcher = new TextWatcher() {
            private CharSequence temp;
            private int editStart ;
            private int editEnd ;
            @Override
            //改变前
            public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {
                temp = charSequence;
                Log.d("myapp","beforeTextChanged:"+temp) ;
            }
            @Override
            //改变时
            public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {
                //更改所要展示的列表信息
                filterData(charSequence.toString());
                //更新ListView适配器
                adapter.notifyDataSetChanged();
                Log.d("myapp","onTextChanged:"+charSequence) ;
            }
            @Override
            //改变后
            public void afterTextChanged(Editable editable) {
                editStart = mEditText.getSelectionStart();
                editEnd = mEditText.getSelectionEnd();
                //如果输入字数超过限制，提示
                if (temp.length() > 10) {
                    Toast.makeText(SelectCity.this,
                            "你输⼊的字数已经超过了限制！", Toast.LENGTH_SHORT)
                            .show();
                    editable.delete(editStart-1, editEnd);
                    int tempSelection = editStart;
                    mEditText.setText(editable);
                    mEditText.setSelection(tempSelection);
                }
                Log.d("myapp","afterTextChanged:") ;
            }
        };
        mEditText.addTextChangedListener(mTextWatcher);
    }

    //更改所要展示的列表信息
    private void filterData(String filterStr){
        //清空两个list
        mCityList.clear();
        mCityNameList.clear();
        if(filterStr.isEmpty()){
            //如果没有查询关键字，list与之前一样
            for (City city : MyApplication.getInstance().getCityList()) {
                mCityNameList.add(city.getCity());
                mCityList.add(city);
            }
            Log.d("myapp","kong") ;
        }else {
            //如果有关键字，根据关键字筛选，并将相关信息更新到两个list中
            for (City city :MyApplication.getInstance().getCityList()) {
                if(city.getCity().indexOf(filterStr.toString())!=-1){
                    mCityNameList.add(city.getCity());
                    mCityList.add(city);
                    Log.d("myapp","ok"+city.getCity()) ;
                }
            }
        }
    }

    @Override
    //单击事件
    public void onClick(View v) {
        switch (v.getId()) {
            //单击返回
            case R.id.title_back:
                //返回数据
                Intent i = new Intent();
                i.putExtra("cityCode", selectedCityCode);
                setResult(RESULT_OK, i);
                finish();
            default:
                break;
        }
    }
}

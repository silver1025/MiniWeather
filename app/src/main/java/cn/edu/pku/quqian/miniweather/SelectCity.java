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
    private ImageView mBackBtn;
    private ListView mListView;
    private EditText mEditText;
    private TextView title_name_Tv;
    private ArrayList<City> mCityList = new ArrayList<>();
    private ArrayList<String> mCityNameList = new ArrayList<>();
    private String selectedCity;
    private String selectedCityCode;
    private ArrayAdapter<String> adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.select_city);
        mBackBtn = (ImageView) findViewById(R.id.title_back);
        mListView = (ListView) findViewById(R.id.list_view);
        title_name_Tv = (TextView) findViewById(R.id.title_name);
        mEditText = (EditText)findViewById(R.id.search_edit);
        mBackBtn.setOnClickListener(this);
        selectedCityCode = MyApplication.getInstance().getString("cityCode","");

        for (City city : MyApplication.getInstance().getCityList()) {
            mCityNameList.add(city.getCity());
            mCityList.add(city);
        }
        adapter = new ArrayAdapter<String>(
                SelectCity.this, android.R.layout.simple_list_item_1, mCityNameList);
        mListView.setAdapter(adapter);
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

        TextWatcher mTextWatcher = new TextWatcher() {
            private CharSequence temp;
            private int editStart ;
            private int editEnd ;
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {
                temp = charSequence;
                Log.d("myapp","beforeTextChanged:"+temp) ;
            }
            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {
                filterData(charSequence.toString());
                adapter.notifyDataSetChanged();
                Log.d("myapp","onTextChanged:"+charSequence) ;
            }
            @Override
            public void afterTextChanged(Editable editable) {
                editStart = mEditText.getSelectionStart();
                editEnd = mEditText.getSelectionEnd();
                if (temp.length() > 10) {
                    Toast.makeText(SelectCity.this,
                            "你输⼊入的字数已经超过了限制！", Toast.LENGTH_SHORT)
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

    private void filterData(String filterStr){
        mCityList.clear();
        mCityNameList.clear();
        if(filterStr.isEmpty()){
            for (City city : MyApplication.getInstance().getCityList()) {
                mCityNameList.add(city.getCity());
                mCityList.add(city);
            }
            Log.d("myapp","kong") ;
        }else {
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
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.title_back:
                Intent i = new Intent();
                i.putExtra("cityCode", selectedCityCode);
                setResult(RESULT_OK, i);
                finish();
            default:
                break;
        }
    }
}

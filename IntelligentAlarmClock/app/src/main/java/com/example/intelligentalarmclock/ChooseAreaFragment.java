package com.example.intelligentalarmclock;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.fragment.app.Fragment;

import com.example.intelligentalarmclock.db.City;
import com.example.intelligentalarmclock.db.County;
import com.example.intelligentalarmclock.db.Province;
import com.example.intelligentalarmclock.util.HttpUtil;
import com.example.intelligentalarmclock.util.Utility;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.litepal.LitePal;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;
/*
 *说明：这个是切换城市的左滑子页面，主要内含一个ListView
 */

public class ChooseAreaFragment extends Fragment {
    public static final int LEVEL_PROVINCE = 0;
    public static final int LEVEL_CITY = 1;
    public static final int LEVEL_COUNTY = 2;
    private ProgressDialog progressDialog;
    private TextView titilText;
    private Button backButton;
    private ListView listView;
    private ArrayAdapter<String> adapter;
    private ArrayList<String> dataList=new ArrayList<String>();

    /**
     * 省列表
     */
    private  List<Province> m_provinceList;
    /**
     * 市列表
     */
    private List<City> m_cityList;
    /**
     * 县列表
     */
    private List<County> m_countyList;
    /**
     *选中的省
     */
    private Province m_selectedProvince;
    /**
     * 选中的市
     */
    private City m_selectedCity;

    /**
     * 选中的县
     */
    public static County m_selectedCounty;

    /**
     * 当前选中的级别
     */
    private int m_currentLevel;
    /**
     *加载控件，为ListView设置适配器
     */
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.choose_arae, container,false);
        LogInfo.d("coolWeather","ChooseAreaFragment onCreateView. threadID="+Thread.currentThread().getId());

        m_selectedCounty=new County();
        m_selectedCounty.setCountyName("请选择城市");

        titilText = (TextView) view.findViewById(R.id.title_text);
        backButton = (Button) view.findViewById(R.id.back_button);
        listView = (ListView) view.findViewById(R.id.list_view);

        adapter = new  ArrayAdapter<>(getContext(), android.R.layout.simple_list_item_1,dataList);
        listView.setAdapter(adapter);
        listView.setHeaderDividersEnabled(true);
        listView.setFooterDividersEnabled(true);
        //listView.setVisibility(View.VISIBLE);
        return view;
    }

    /**
     *为Butto和ListView设置点击事件
     */
    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        LogInfo.d("ChooseAreaFragment onActivityCreated.ThreadID="+Thread.currentThread().getId() );
        //LogInfo.d("coolWeather","m_currentLevel = "+ m_currentLevel);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                //Log.d("coolWeather","ChooseAreaFragment onItemClick position="+position);
                if (m_currentLevel == LEVEL_PROVINCE){
                    m_selectedProvince = m_provinceList.get(position);
                    LogInfo.d("coolWeather","ChooseAreaFragment onActivityCreated m_currentLevel=LEVEL_PROVINCE");
                    queryCities();
                }else if (m_currentLevel == LEVEL_CITY){
                    LogInfo.d("coolWeather","ChooseAreaFragment onActivityCreated m_currentLevel=LEVEL_CITY");
                    m_selectedCity = m_cityList.get(position);
                    //Log.d("coolWeather","*m_selectedCity: name="+m_selectedCity.getCityName()+"  id="+String.valueOf(m_selectedCity.getCityCode()));
                    queryCounties();
                }else if (m_currentLevel == LEVEL_COUNTY){
                    LogInfo.d("m_currentLevel=LEVEL_COUNTY");
                    m_selectedCounty = m_countyList.get(position);
                    String weatherId = m_selectedCounty.getWeatherId();
                    //Log.d("coolWeather"," CountyName="+m_selectedCounty.getCountyName()+" WeatherId="+m_selectedCounty.getWeatherId());
                    MainActivity mainActivity=(MainActivity)getActivity();
                    mainActivity.drawerLayout.closeDrawers();//收起切换城市列表
                    mainActivity.swipeRefreshLayout.setRefreshing(true);//显示刷新图标
                    mainActivity.requestWeather(weatherId);//根据经纬度请求hourly天气信息
                    mainActivity.requestDailyWeather(weatherId);//根据经纬度请求daily天气信息
                }
            }
        });

        backButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                if (m_currentLevel == LEVEL_COUNTY){
                    queryCities();
                }else if (m_currentLevel == LEVEL_CITY){
                    queryProvinces();
                }
            }
        });
        queryProvinces();
    }

    /**
     * 查询全国所有的省，优先从数据库查询，如果没有查询到再去assets里的json数据查询
     */
     public void queryProvinces(){
        titilText.setText("中国");
        //backButton.setVisibility(View.GONE);
        LogInfo.d( " ChooseAreaFragment queryProvinces start.Thread="+Thread.currentThread().getId());
         m_provinceList = LitePal.findAll(Province.class);
        if (m_provinceList.size() > 0){
            //Log.d("coolWeather", "ChooseAreaFragment queryProvinces from LitePal");
            dataList.clear();
            for (Province province: m_provinceList){
                //Log.d("coolWeather", "province.getProvinceName()="+province.getProvinceName());
                dataList.add(province.getProvinceName());
            }
            //Log.d("coolWeather", "ChooseAreaFragment notifyDataSetChanged start");
            adapter.notifyDataSetChanged();
            listView.setSelection(0);
            m_currentLevel = LEVEL_PROVINCE;
        }else {
            Log.d("coolWeather", "ChooseAreaFragment queryProvinces from province.json");
            String address = "assets/"+"province.json";
            queryFromJSONFile(address, "province");
        }
    }

    /**
     *查询选中省内所有的市，优先从数据库查询，如果没有查询到JSON文件中查询
     */
    private void queryCities(){
        LogInfo.d("ChooseAreaFragment queryCities start.ThreadID="+Thread.currentThread().getId());
        titilText.setText(m_selectedProvince.getProvinceName());
        backButton.setVisibility(View.VISIBLE);

        m_cityList = LitePal.where("provinceId=?",String.valueOf(m_selectedProvince.getProvinceCode())).find(City.class);

        if (m_cityList.size() > 0){
            LogInfo.d("coolWeather","* queryCities from LitePal start" );
            dataList.clear();
            for (City city: m_cityList){
                //Log.d("coolWeather","* ******city name="+city.getCityName()+"   cityId ="+String.valueOf(city.getCityCode()) +"  provinceID="+
                //        String.valueOf(city.getProvinceId()));
                dataList.add(city.getCityName());
            }
            adapter.notifyDataSetChanged();
            //listView.setSelection(0);
            m_currentLevel = LEVEL_CITY;
        }else {
            LogInfo.d("coolWeather","* queryCities from JSONFile start" );
            String address =  "assets/"+"city.json";
            queryFromJSONFile(address, "city");
        }
    }

    /**
     * 查询选中市内所有的县，优先从数据库查询，如果没有查询到JSON文件上查询
     */

    private void queryCounties(){
        LogInfo.d("coolWeather","ChooseAreaFragment queryCounties  start.ThreadID="+Thread.currentThread().getId() );
        titilText.setText(m_selectedCity.getCityName());
        backButton.setVisibility(View.VISIBLE);
        //Log.d("coolWeather","* current provinceId="+String.valueOf(m_selectedProvince.getProvinceCode())+"  cityId="+
        //        String.valueOf(m_selectedCity.getCityCode() ));
        m_countyList = LitePal.where("cityId=? and provinceId=?",String.valueOf(m_selectedCity.getCityCode()),
                String.valueOf(m_selectedProvince.getProvinceCode())).find(County.class);
        if (m_countyList.size() > 0){
            LogInfo.d("coolWeather","*ChooseAreaFragment queryCounties from LitePal start" );
            dataList.clear();
            for (County county: m_countyList){
                dataList.add(county.getCountyName());
            }
            adapter.notifyDataSetChanged();
            listView.setSelection(0);
            m_currentLevel = LEVEL_COUNTY;
        }else {
            LogInfo.d("coolWeather","ChooseAreaFragment queryCounties from JSON file start" );
            //Log.d("coolWeather","*m_selectedProvince name="+m_selectedProvince.getProvinceName());
            int provinceID = m_selectedProvince.getProvinceCode();
            String address =  "assets/"+"countyOf"+ String.valueOf(provinceID)+".json";
            queryFromJSONFile(address, "county");
        }
    }

    /**
     * 根据传入的地址和类型从JSON文件上查询省市县数据
     */
    private void queryFromJSONFile(String address, final String type){
        LogInfo.d("coolWeather","ChooseAreaFragment queryFromJSONFile start.ThreadID="+Thread.currentThread().getId());
        showProgressDialog();
        try {
            LogInfo.d("queryFrom " + address);
            InputStream is = ChooseAreaFragment.this.getClass().getClassLoader().
                    getResourceAsStream(address);
            InputStreamReader streamReader = new InputStreamReader(is);
            BufferedReader reader  = new BufferedReader(streamReader);
            String line;
            StringBuilder stringBuilder = new StringBuilder();
            while ((line = reader.readLine()) != null){
                stringBuilder.append(line);
            }
            reader.close();
            streamReader.close();
            is.close();

           LogInfo.d("get the data and save to the database" );
            line = stringBuilder.toString();
            //Log.d("coolWeather","*line="+line );
            try{
                if ("province".equals(type)){
                    Log.d("coolWeather","type=province");
                    JSONObject allProvince = new JSONObject(line);
                    line = allProvince.getString("province");
                    JSONArray provinces = new JSONArray(line);
                    for (int i=0; i<provinces.length(); i++){
                        allProvince = provinces.getJSONObject(i);
                        Province province = new Province();
                        province.setProvinceName(allProvince.getString("name"));
                        province.setProvinceCode(allProvince.getInt("id"));
                        province.save();//将全部的省级数据存储到数据库
                    }
                }else if ("city".equals(type)){
                    Log.d("coolWeather","type=city");
                    int provinceID = m_selectedProvince.getProvinceCode();
                    //Log.d("coolWeather","selsected provinceID="+String.valueOf(provinceID));
                    JSONObject allCities = new JSONObject(line);
                    //Log.d("coolWeather","new JSONObject(line) ok");
                    line = allCities.getString("city");
                    //Log.d("coolWeather","allCities.getString ok, line="+line);
                    JSONArray cities = new JSONArray(line);
                   // Log.d("coolWeather","select cities");
                    for (int i=0; i<cities.length(); i++){
                        //Log.d("coolWeather","i="+String.valueOf(i));
                        allCities = cities.getJSONObject(i);
                        if ( provinceID == allCities.getInt("provinceId")){
                            City city = new City();
                            city.setCityCode(allCities.getInt("id"));
                            city.setCityName(allCities.getString("name"));
                            city.setProviceId(provinceID);
                            //Log.d("coolWeather", "save city provinceID="+String.valueOf(city.getProvinceId())+"  city name="+
                            //        city.getCityName()+"  cityID="+String.valueOf(city.getCityCode()));
                            city.save();//将某个省里的全部市的数据存储到数据库
                        }
                    }
                }else if ("county".equals(type)){
                    Log.d("coolWeather","type=county");
                    int cityID = m_selectedCity.getCityCode();
                    JSONObject allCounties = new JSONObject(line);
                    JSONArray counties = allCounties.getJSONArray("county");
                    for (int i=0; i<counties.length(); i++){
                        allCounties = counties.getJSONObject(i);
                        if ( cityID == allCounties.getInt("cityId") ){
                            County county = new County();
                            county.setWeatherId(allCounties.getString("id"));
                            county.setCountyName(allCounties.getString("name"));
                            county.setCityId(m_selectedCity.getCityCode());
                            county.setProvinceId(m_selectedProvince.getProvinceCode());
                            //Log.d("coolWeather", "save county provinceID="+String.valueOf(county.getProvinceId())+"  county name="+
                            //        county.getCountyName()+"  countyID="+String.valueOf(county.getWeatherId())+
                            //        "  cityID"+String.valueOf(county.getCityId()));
                            county.save();//将某个省下的某个市下的某个城市的数据存储到数据库
                        }
                    }
                }
            }catch (JSONException e){
                e.printStackTrace();
                LogInfo.d( "%%%%%%%%%%%%%%%%%% province JSONException fail");
            }

            LogInfo.d("*show the select list" );
            //回到主界面上操作
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    LogInfo.d("runOnUiThread start.ThreadID="+Thread.currentThread().getId());
                    closeProgressDialog();
                    if ("province".equals(type)){
                        queryProvinces();
                    }else if ("city".equals(type)){
                        queryCities();
                    }else if ("county".equals(type)){
                        queryCounties();
                    }
                }
            });
        }catch (IOException e){
            e.printStackTrace();
            Log.d("coolWeather", "%%%%%%%%%%%%%%%%%% province IOException fail");
        }
    }


    /**
     * 根据传入的地址和类型从服务器上查询省市县数据
     */
    private void queryFromServer(String address, final String type){
        showProgressDialog();
        HttpUtil.sendOkHttpRequest(address, new Callback() {
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseText = response.body().string();
                Log.d("coolWeather","ChooseAreaFragment onResponse start，type="+type );
                boolean result = false;
                if ("province".equals(type)){
                    result = Utility.handleProvinceResponse(responseText);
                }else if ("city".equals(type)){
                    result = Utility.handleCityResponse(responseText,m_selectedProvince.getId() );
                }else if ("county".equals(type)){
                    result = Utility.handleCountyResponse(responseText, m_selectedCity.getCityCode());
                }
                if (result){
                    //UI操作，必须在主线程中进行
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            closeProgressDialog();
                            if ("province".equals(type)){
                                queryProvinces();
                            }else if ("city".equals(type)){
                                queryCities();
                            }else if ("county".equals(type)){
                                queryCounties();
                            }
                        }
                    });
                }
            }

            @Override
            public void onFailure(Call call, IOException e) {
                //通过runOnUiThread回归到主线程处理逻辑
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        closeProgressDialog();
                        Toast.makeText(getContext(), "加载失败，请检查网络连接", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }

    /**
     * 显示进度对话框
     */
    private void showProgressDialog(){
        if (progressDialog == null){
            progressDialog = new ProgressDialog(getActivity());
            progressDialog.setMessage("正在加载");
            progressDialog.setCanceledOnTouchOutside(false);
        }
        progressDialog.show();
    }

    /**
     * 关闭进度对话框
     */
    private void closeProgressDialog(){
        if (progressDialog != null){
            progressDialog.dismiss();
        }
    }

    public static County getSelectedCounty(){
        LogInfo.d("getSelectedCounty start");
        return m_selectedCounty;
    }

    public static void setSelectedCounty(String countyName, String weatherId){
        m_selectedCounty.setCountyName(countyName);
        m_selectedCounty.setWeatherId(weatherId);
    }
}

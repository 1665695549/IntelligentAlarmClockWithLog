package com.example.intelligentalarmclock.util;

import android.text.TextUtils;
import android.util.Log;

import com.example.intelligentalarmclock.LogInfo;
import com.example.intelligentalarmclock.db.City;
import com.example.intelligentalarmclock.db.County;
import com.example.intelligentalarmclock.db.Province;
import com.example.intelligentalarmclock.gson.CaiyunDailyWeatherContent;
import com.example.intelligentalarmclock.gson.CaiyunWeatherContent;
import com.google.gson.Gson;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class Utility {
    /**
     * *解析和处理服务器返回的省级数据
     */
    public static boolean handleProvinceResponse(String response) {
        Log.d("coolWeather", "Utility handleProvinceResponse start");
        if (!TextUtils.isEmpty(response)) {
            try {
                JSONArray allProvince = new JSONArray(response);
                for (int i = 0; i < allProvince.length(); i++) {
                    JSONObject provinceObject = allProvince.getJSONObject(i);
                    Province province = new Province();
                    province.setProvinceName(provinceObject.getString("name"));
                    province.setProvinceCode(provinceObject.getInt("id"));
                    province.save();//将数据存储到数据库中
                }
                Log.d("coolWeather", "Utility handleProvinceResponse secessful");
                return true;
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        Log.d("coolWeather", "Utility handleProvinceResponse failed");
        return false;
    }

    /**
     * 解析和处理服务器返回的市级数据
     */
    public static boolean handleCityResponse(String response, int provinceID){
        Log.d("coolWeather","Utility handleCityResponse start");
        if (!TextUtils.isEmpty(response)){
            try {

                JSONArray allCities = new JSONArray(response);
                for (int i=0; i<allCities.length(); i++){
                    JSONObject cityObject = allCities.getJSONObject(i);
                    City city = new City();
                    city.setCityName(cityObject.getString("name"));
                    city.setCityCode(cityObject.getInt("id"));
                    city.setProviceId(provinceID);
                    city.save();//将数据存储到数据库中
                }
                Log.d("coolWeather","Utility handleCityResponse seccessful");
                return true;
            }catch (JSONException e){
                e.printStackTrace();
            }
        }
        Log.d("coolWeather","Utility handleCityResponse failed");
        return false;
    }

    /**
     * 解析和处理服务器返回的县级数据
     */
    public static boolean handleCountyResponse(String response, int cityId){
        Log.d("coolWeather","Utility handleCountyResponse start");
        if (!TextUtils.isEmpty(response)){
            try {
                JSONArray allCounties = new JSONArray(response);
                for (int i=0; i<allCounties.length(); i++ ){
                    JSONObject countyObject = allCounties.getJSONObject(i);
                    County county = new County();
                    county.setCountyName(countyObject.getString("name"));
                    county.setWeatherId(countyObject.getString("weather_id"));
                    county.setCityId(cityId);
                    county.save();//将数据存储到数据库中
                }
                Log.d("coolWeather","Utility handleCountyResponse sucessful");
                return true;
            }catch (JSONException e){
                e.printStackTrace();
            }
        }
        Log.d("coolWeather","Utility handleCountyResponse failed");
        return false;
    }

    /**
     * 将返回的JSON数据解析成CaiyunWeatherContent实体类
     */
    public static CaiyunWeatherContent handleWeatherResponse(String response){
        Log.d("coolWeather","Utility handleWeatherResponse start");
        //LogInfo.d("try start response="+response);
        try{
            JSONObject jsonObject = new JSONObject(response);
            Log.d("coolWeather"," new JSONObject ok");
            jsonObject = jsonObject.getJSONObject("result").getJSONObject("hourly");
            Log.d("coolWeather","jsonObject.getJSONObject ok");
            String weatherContent = jsonObject.toString();
            Log.d("coolWeather","weatherContent="+weatherContent);
            /**
             * 从JSON结构中取需要的数据到自定义类中
             */
            return new Gson().fromJson(weatherContent,CaiyunWeatherContent.class );
        }catch (Exception e){
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 将返回的daily级JSON数据解析成CaiyunWeatherContent实体类
     */
    public static CaiyunDailyWeatherContent handleDailyWeatherResponse(String response){
        LogInfo.d("coolWeather","Utility handleDailyWeatherResponse start");
        try{
            LogInfo.d("try start");
            JSONObject jsonObject = new JSONObject(response);
            LogInfo.d("coolWeather"," new JSONObject ok");
            jsonObject = jsonObject.getJSONObject("result");
            LogInfo.d("coolWeather","getJSONObject(result) ok");
            String dailyWeatherContent = jsonObject.toString();
            LogInfo.d("coolWeather","dailyWeatherContent="+dailyWeatherContent);
            jsonObject = jsonObject.getJSONObject("daily");
            LogInfo.d("coolWeather","getJSONObject(daily) ok");
            dailyWeatherContent = jsonObject.toString();
            LogInfo.d("coolWeather","dailyWeatherContent="+dailyWeatherContent);
            /**
             * 从JSON结构中取需要的数据到自定义类中
             */
            return new Gson().fromJson(dailyWeatherContent,CaiyunDailyWeatherContent.class );
        }catch (Exception e){
            LogInfo.d("somthing wrong");
            e.printStackTrace();
        }
        return null;
    }
}



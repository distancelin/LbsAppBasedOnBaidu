package com.jason.com.mmp;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

/**
 * Created by 16276 on 2016/10/26.
 */
//方向传感器监听器

 class myDirectionListener implements SensorEventListener {
    //传感器manager
    private SensorManager mSensorManager;
    private Context mContext;
    //传感器
    private Sensor mSensor;
    private float  lastX;
    //监听器借口引用
    private onOrientationListener mOnOrientationListener;
    //setter
    public void setOnOrientationListener(onOrientationListener onOrientationListener) {
        mOnOrientationListener = onOrientationListener;
    }

    public myDirectionListener(Context context) {
        mContext = context;
    }
    //开启方向传感器
    public  void start(){
        mSensorManager= (SensorManager) mContext.getSystemService(Context.SENSOR_SERVICE);
        if(mSensorManager!=null){
            mSensor=mSensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION);
        }
        if(mSensor!=null){
            mSensorManager.registerListener(this,mSensor,SensorManager.SENSOR_DELAY_FASTEST);
        }
    }
    //停止方向传感器
    public  void stop(){
        mSensorManager.unregisterListener(this);
    }
    @Override
    //回调方法，在方向传感器改变之后回调
    public void onSensorChanged(SensorEvent event) {
        if(event.sensor.getType()==Sensor.TYPE_ORIENTATION){
            //values有三个内容
            //第一个参数，代表方位，手机绕着z轴旋转的角度，东南西北，北为0
            //第二个参数，代表倾斜程度，手机绕着x轴倾斜的角度（比如抬起手机头部）,【-180,180】
            //第三个参数，代表绕Y轴滚动的程度，【-90,90】
            float x=event.values[SensorManager.DATA_X];
            //旋转的度数超过1
            if(Math.abs(x-lastX)>0.1){
                mOnOrientationListener.onOrientationChanged(x);
            }
            lastX=x;
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }
}
interface  onOrientationListener{

    void onOrientationChanged(float x);

}

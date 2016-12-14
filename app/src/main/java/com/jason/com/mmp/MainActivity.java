package com.jason.com.mmp;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.LocationManager;
import android.os.Environment;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.baidu.location.BDLocation;
import com.baidu.location.BDLocationListener;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.baidu.mapapi.SDKInitializer;
import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.BitmapDescriptor;
import com.baidu.mapapi.map.BitmapDescriptorFactory;
import com.baidu.mapapi.map.MapPoi;
import com.baidu.mapapi.map.MapStatus;
import com.baidu.mapapi.map.MapStatusUpdate;
import com.baidu.mapapi.map.MapStatusUpdateFactory;
import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.map.MarkerOptions;
import com.baidu.mapapi.map.MyLocationConfiguration;
import com.baidu.mapapi.map.MyLocationData;
import com.baidu.mapapi.map.OverlayOptions;
import com.baidu.mapapi.model.LatLng;
import com.baidu.mapapi.search.core.PoiInfo;
import com.baidu.mapapi.search.core.SearchResult;
import com.baidu.mapapi.search.poi.OnGetPoiSearchResultListener;
import com.baidu.mapapi.search.poi.PoiDetailResult;
import com.baidu.mapapi.search.poi.PoiDetailSearchOption;
import com.baidu.mapapi.search.poi.PoiIndoorResult;
import com.baidu.mapapi.search.poi.PoiNearbySearchOption;
import com.baidu.mapapi.search.poi.PoiResult;
import com.baidu.mapapi.search.poi.PoiSearch;
import com.baidu.mapapi.search.poi.PoiSortType;
import com.baidu.mapapi.utils.DistanceUtil;
import com.baidu.navisdk.adapter.BNRoutePlanNode;
import com.baidu.navisdk.adapter.BaiduNaviManager;
import java.io.File;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    MapView MapView;
    BaiduMap baiduMap;
    CoordinatorLayout rootView;
    Button btMyLocation, btRoadStatus, btMapChange, btPioSearch;
    //与定位相关
    LocationClient locationClient = new LocationClient(this);
    myLocationListener myLocationListener = new myLocationListener();
    boolean isFirstIn = true;
    double latitude, longitude;
    BitmapDescriptor myLocationIcon;
    String myLocation;
    myDirectionListener mMyDirectionListener;
    float currentX;
    MyLocationConfiguration.LocationMode mLocationMode = MyLocationConfiguration.LocationMode.NORMAL;
    //导航相关
    LatLng destnation;
    private String mSDCardPath = null;
    private static final String APP_FOLDER_NAME = "BNSDKSimpleDemo";
    public static final String ROUTE_PLAN_NODE = "routePlanNode";
    //搜索相关
    PoiSearch search=null;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //在使用SDK各组件之前初始化context信息，传入ApplicationContext
        //注意该方法要再setContentView方法之前实现
        SDKInitializer.initialize(getApplicationContext());
        setContentView(R.layout.activity_main);
        //初始化视图
        initView();
        //初始化定位
        initLocation();
        //初始化导航相关
        if (initDirs()) {
            initNavi();
        }
    }
    @Override
    protected void onStart() {
        super.onStart();
        //开启定位,设置定位图层为显示（定位图层就是定位显示的小圆圈）
        baiduMap.setMyLocationEnabled(true);
        locationClient.start();
        mMyDirectionListener.start();
    }

    @Override
    protected void onStop() {
        super.onStop();
        //停止定位
        baiduMap.setMyLocationEnabled(false);
        locationClient.stop();
        mMyDirectionListener.stop();
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        //在activity执行onDestroy时执行mMapView.onDestroy()，实现地图生命周期管理
        MapView.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();
        //在activity执行onResume时执行mMapView. onResume ()，实现地图生命周期管理
        MapView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        //在activity执行onPause时执行mMapView. onPause ()，实现地图生命周期管理
        MapView.onPause();
    }

    private void initLocation() {
        locationClient.registerLocationListener(myLocationListener);
        LocationClientOption option = new LocationClientOption();
        option.setCoorType("bd09ll");// 设置坐标类型
        option.setOpenGps(true);
        //设置是否需要位置描述
        option.setIsNeedAddress(true);
        option.setIsNeedLocationDescribe(true);
        option.setScanSpan(1000);
        locationClient.setLocOption(option);
    }

    private void initView() {
        //初始化定位图标
        btMyLocation = (Button) findViewById(R.id.myLocation);
        btRoadStatus = (Button) findViewById(R.id.roadStatus);
        btMapChange = (Button) findViewById(R.id.mapChange);
        btPioSearch = (Button) findViewById(R.id.poiSearch);
        myLocationIcon = BitmapDescriptorFactory.fromResource(R.drawable.my_location_icon);
        rootView = (CoordinatorLayout) findViewById(R.id.activity_main);
        MapView = (MapView) findViewById(R.id.mapView);
        baiduMap = MapView.getMap();
        search=PoiSearch.newInstance();
        search.setOnGetPoiSearchResultListener(new OnGetPoiSearchResultListener() {
            @Override
            public void onGetPoiResult(PoiResult result) {
                if (result == null || result.error == SearchResult.ERRORNO.RESULT_NOT_FOUND) {
                    Toast.makeText(MainActivity.this, "未找到结果", Toast.LENGTH_LONG)
                            .show();
                    return;
                }
                //搜索成功
                if (result.error == SearchResult.ERRORNO.NO_ERROR) {
                    baiduMap.clear();
                    PoiOverlay overlay = new MyPoiOverlay(baiduMap);
                    baiduMap.setOnMarkerClickListener(overlay);
                    overlay.setData(result);
                    overlay.addToMap();
                    overlay.zoomToSpan();
            }
            }
            @Override
            public void onGetPoiDetailResult(PoiDetailResult result) {
                if (result.error != SearchResult.ERRORNO.NO_ERROR) {
                    Toast.makeText(MainActivity.this, "抱歉，未找到结果", Toast.LENGTH_SHORT)
                            .show();
                } else {
                    final LatLng poiLatlng = result.getLocation();
                    final LatLng myLatlng = new LatLng(latitude, longitude);
                    Snackbar.make(rootView,result.getName() + ": " + result.getAddress(),Snackbar.LENGTH_SHORT)
                            .setAction("到这儿去", new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    routeplanToNavi(myLatlng, poiLatlng);
                                }
                            })
                            .show();

                }
            }
            @Override
            public void onGetPoiIndoorResult(PoiIndoorResult poiIndoorResult) {

            }
        });
        //隐藏百度地图logo
        for (int i = 0; i < MapView.getChildCount(); i++) {
            View child = MapView.getChildAt(i);
            if (child instanceof ImageView) {
                child.setVisibility(View.GONE);
            }
        }
        btMyLocation.setOnClickListener(this);
        btRoadStatus.setOnClickListener(this);
        btMapChange.setOnClickListener(this);
        btPioSearch.setOnClickListener(this);

        //判断gps是否打开
        if (!isGpsOpen()) {
            Toast.makeText(this, "打开GPS定位服务，定位更准确哟", Toast.LENGTH_SHORT).show();
        }
        //在地图上点击我的位置
        baiduMap.setOnMyLocationClickListener(new BaiduMap.OnMyLocationClickListener() {
            @Override
            public boolean onMyLocationClick() {
                centerToMyLocation(latitude, longitude);
                return false;
            }
        });
        //设置地图点击监听事件
        baiduMap.setOnMapClickListener(new BaiduMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng latLng) {
                baiduMap.clear();
            }

            @Override
            public boolean onMapPoiClick(final MapPoi mapPoi) {
                baiduMap.clear();
                BitmapDescriptor bitmap = BitmapDescriptorFactory
                        .fromResource(R.drawable.mark);
                //获得点击poi的name
                String poiName = mapPoi.getName();
                final LatLng poiLatlng = mapPoi.getPosition();
                final LatLng myLatlng = new LatLng(latitude, longitude);
                //计算点击poi到我的位置的距离
                int distance = (int) DistanceUtil.getDistance(poiLatlng, myLatlng);
                Snackbar.make(rootView, poiName + " 距离我 " + distance + " 米", Snackbar.LENGTH_LONG)
                        .setAction("到这儿去", new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                routeplanToNavi(myLatlng, poiLatlng);
                            }
                        })
                        .show();
                OverlayOptions option = new MarkerOptions()
                        .position(poiLatlng)
                        .icon(bitmap);
                //在地图上添加Marker，并显示
                baiduMap.addOverlay(option);
                centerToMyLocation(poiLatlng.latitude, poiLatlng.longitude);
                return false;
            }
        });
        //设置一开始的范围为50m
        MapStatusUpdate mus = MapStatusUpdateFactory.zoomTo(18.0f);
        baiduMap.setMapStatus(mus);
        mMyDirectionListener = new myDirectionListener(this);
        mMyDirectionListener.setOnOrientationListener(new onOrientationListener() {
            @Override
            public void onOrientationChanged(float x) {
                currentX = x;
            }
        });
    }

    private boolean initDirs() {
        mSDCardPath = getSdcardDir();
        if (mSDCardPath == null) {
            return false;
        }
        File f = new File(mSDCardPath, APP_FOLDER_NAME);
        if (!f.exists()) {
            try {
                f.mkdir();
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        }
        return true;
    }

    String authinfo = null;

    private void initNavi() {


        BaiduNaviManager.getInstance().init(this, mSDCardPath, APP_FOLDER_NAME, new BaiduNaviManager.NaviInitListener() {
            @Override
            public void onAuthResult(int status, String msg) {
                if (0 == status) {
                    authinfo = "key校验成功!";
                } else {
                    authinfo = "key校验失败, " + msg;
                }
                MainActivity.this.runOnUiThread(new Runnable() {

                    @Override
                    public void run() {
                        Toast.makeText(MainActivity.this, authinfo, Toast.LENGTH_LONG).show();
                    }
                });
            }

            public void initSuccess() {
                Toast.makeText(MainActivity.this, "导航引擎初始化成功", Toast.LENGTH_SHORT).show();
            }

            public void initStart() {
            }

            public void initFailed() {
                Toast.makeText(MainActivity.this, "导航引擎初始化失败", Toast.LENGTH_SHORT).show();
            }

        }, null, null, null);

    }

    private String getSdcardDir() {
        if (Environment.getExternalStorageState().equalsIgnoreCase(Environment.MEDIA_MOUNTED)) {
            return Environment.getExternalStorageDirectory().toString();
        }
        return null;
    }

    //导航方法，传入起点和终点
    private void routeplanToNavi(LatLng start, LatLng end) {
        BNRoutePlanNode.CoordinateType coType = BNRoutePlanNode.CoordinateType.BD09LL;
        BNRoutePlanNode sNode;
        BNRoutePlanNode eNode;
        sNode = new BNRoutePlanNode(start.longitude, start.latitude, "起点", null, coType);
        eNode = new BNRoutePlanNode(end.longitude, end.latitude, "终点", null, coType);
        if (sNode != null && eNode != null) {
            List<BNRoutePlanNode> list = new ArrayList<BNRoutePlanNode>();
            list.add(sNode);
            list.add(eNode);
            BaiduNaviManager.getInstance().launchNavigator(this, list, 1, false, new DemoRoutePlanListener(sNode));
        }
    }

    public class DemoRoutePlanListener implements BaiduNaviManager.RoutePlanListener {

        private BNRoutePlanNode mBNRoutePlanNode = null;

        public DemoRoutePlanListener(BNRoutePlanNode node) {
            mBNRoutePlanNode = node;
        }

        @Override
        public void onJumpToNavigator() {
        /*
         * 设置途径点以及resetEndNode会回调该接口
         */
            Intent intent = new Intent(MainActivity.this, BNDemoGuideActivity.class);
            Bundle bundle = new Bundle();
            bundle.putSerializable(ROUTE_PLAN_NODE, mBNRoutePlanNode);
            intent.putExtras(bundle);
            startActivity(intent);

        }

        @Override
        public void onRoutePlanFailed() {
            // TODO Auto-generated method stub
            Toast.makeText(MainActivity.this, "算路失败", Toast.LENGTH_SHORT).show();
        }
    }



    //判断gps是否开启
    private boolean isGpsOpen() {
        LocationManager manager = (LocationManager) getApplicationContext().getSystemService(Context.LOCATION_SERVICE);
        return manager.isProviderEnabled(LocationManager.GPS_PROVIDER);
    }



    //将我的位置设置到地图正中间
    private void centerToMyLocation(double latitude, double longitude) {
        LatLng latlng = new LatLng(latitude, longitude);
        MapStatus.Builder builder = new MapStatus.Builder();
        //target方法用于设置地图的中心点，zoom设置地图的缩放级别,500m为15f
        builder.target(latlng).zoom(19.0f);
        //在确定位置以后以动画的方式更新地图
        baiduMap.animateMapStatus(MapStatusUpdateFactory.newMapStatus(builder.build()));
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.myLocation:
                centerToMyLocation(latitude, longitude);
                Snackbar.make(rootView, "我的位置：" + myLocation, Snackbar.LENGTH_LONG).show();
                if (mLocationMode == MyLocationConfiguration.LocationMode.COMPASS) {
                    mLocationMode = MyLocationConfiguration.LocationMode.NORMAL;
                } else {
                    mLocationMode = MyLocationConfiguration.LocationMode.COMPASS;
                }
                break;
            case R.id.roadStatus:
                if (!baiduMap.isTrafficEnabled()) {
                    baiduMap.setTrafficEnabled(true);
                    Toast.makeText(this, "实时路况已开启", Toast.LENGTH_SHORT).show();
                } else {
                    baiduMap.setTrafficEnabled(false);
                    Toast.makeText(this, "实时路况已关闭", Toast.LENGTH_SHORT).show();
                }
                break;
            case R.id.mapChange:
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("地图类型");
                builder.setItems(new CharSequence[]{"普通地图", "卫星地图"}, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which) {
                            case 0:
                                baiduMap.setMapType(BaiduMap.MAP_TYPE_NORMAL);
                                break;
                            default:
                                baiduMap.setMapType(BaiduMap.MAP_TYPE_SATELLITE);
                        }
                    }
                }).show();
                break;
            case R.id.poiSearch:
                //检索相关
                View view = getLayoutInflater().inflate(R.layout.poi_search, null);
                final TextInputLayout mTextInputLayout = (TextInputLayout) view.findViewById(R.id.tilPiosearch);
                final EditText mEditText = mTextInputLayout.getEditText();
                final AlertDialog.Builder builder1 = new AlertDialog.Builder(this);
                mEditText.addTextChangedListener(new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                    }

                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {
                        mTextInputLayout.setErrorEnabled(false);
                    }

                    @Override
                    public void afterTextChanged(Editable s) {

                    }
                });
                builder1.setNegativeButton("取消", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Field field = null;
                        try {
                            field = dialog.getClass().getSuperclass().getSuperclass().getDeclaredField("mShowing");
                        } catch (NoSuchFieldException e) {
                            e.printStackTrace();
                        }
                        field.setAccessible(true);
                        try {
                            field.set(dialog,true);
                        } catch (IllegalAccessException e) {
                            e.printStackTrace();
                        }
                        dialog.dismiss();
                    }
                })
                        .setPositiveButton("搜索", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                Field  field = null;
                                try {
                                    field = dialog.getClass().getSuperclass().getSuperclass().getDeclaredField("mShowing");
                                    field.setAccessible(true);
                                } catch (NoSuchFieldException e) {
                                    e.printStackTrace();
                                }
                                if (mEditText.getText().toString().equals("")) {
                                    try {
                                        field.set(dialog, false);
//                                        dialog.dismiss();
                                        mTextInputLayout.setError("亲，还没输入噢0.0");
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                } else {
                                    try {
                                        field.set(dialog,true);
                                        PoiNearbySearchOption nearbySearchOption = new PoiNearbySearchOption().keyword(mEditText.getText()
                                                .toString()).sortType(PoiSortType.distance_from_near_to_far).location(new LatLng(latitude,longitude))
                                                .radius(300).pageNum(0);
                                        search.searchNearby(nearbySearchOption);
//                                        dialog.dismiss();
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                }
                            }
                        }).setView(view).show();

        }
    }

    class myLocationListener implements BDLocationListener {

        @Override
        public void onReceiveLocation(BDLocation bdLocation) {
//        MyLocationData myLocationData=new MyLocationData(); 该构造方法为private的，所以使用下面的方式构造对象
            MyLocationData locData = new MyLocationData.Builder() //构造器为 myLocationData.builder()
                    //设置定位精度
                    .accuracy(bdLocation.getRadius())
                    //设置经纬度
                    .latitude(bdLocation.getLatitude())
                    .direction(currentX).longitude(bdLocation.getLongitude())
                    .build();
            // 设置定位数据
            baiduMap.setMyLocationData(locData);
            //设置定位图标（定位图层的显示模式，是否显示方向信息，图片）
            MyLocationConfiguration configuration = new MyLocationConfiguration(mLocationMode, true, myLocationIcon);
            baiduMap.setMyLocationConfigeration(configuration);
            latitude = bdLocation.getLatitude();
            longitude = bdLocation.getLongitude();
            myLocation = bdLocation.getAddrStr() + "|" + bdLocation.getLocationDescribe();
            if (isFirstIn) {
                isFirstIn = false;
                Snackbar.make(rootView, "我的位置：" + myLocation, Snackbar.LENGTH_LONG).show();
                centerToMyLocation(latitude, longitude);
            }
        }
    }
    private class MyPoiOverlay extends PoiOverlay {

        public MyPoiOverlay(BaiduMap baiduMap) {
            super(baiduMap);
        }

        @Override
        public boolean onPoiClick(int index) {
            PoiInfo poi = getPoiResult().getAllPoi().get(index);
            search.searchPoiDetail((new PoiDetailSearchOption())
                    .poiUid(poi.uid));
            return true;
        }
    }
}
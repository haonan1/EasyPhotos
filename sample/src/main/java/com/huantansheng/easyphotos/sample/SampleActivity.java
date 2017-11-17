package com.huantansheng.easyphotos.sample;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.media.FaceDetector;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.PagerSnapHelper;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SnapHelper;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.huantansheng.easyphotos.EasyPhotos;
import com.huantansheng.easyphotos.models.album.entity.Photo;
import com.huantansheng.easyphotos.utils.bitmap.BitmapUtils;
import com.huantansheng.easyphotos.utils.bitmap.face.FaceCallBackOnUiThread;
import com.huantansheng.easyphotos.utils.bitmap.face.FaceInformation;

import java.util.ArrayList;

public class SampleActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    /**
     * 选择的图片集
     */
    private ArrayList<Photo> selectedPhotoList = new ArrayList<>();
    private MainAdapter adapter;
    private RecyclerView rvImage;

    /**
     * 图片列表和专辑项目列表的广告view
     */
    private RelativeLayout photosAdView, albumItemsAdView;

    /**
     * 广告是否加载完成
     */
    private boolean photosAdLoaded = false, albumItemsAdLoaded = false;

    /**
     * 展示bitmap功能的
     */
    private Bitmap bitmap = null;
    private ImageView bitmapView = null;
    private int realFaceNum = 0;//实际检测出的人脸数量
    private ProgressDialog progressDialog = null;//我是图省事事儿才用ProgressDialog的，已经废弃的东西，不建议用了。在此也推荐一下我的另一个开源项目 L ，L是我集成的一个android开发便捷库，里面支持一键生成各种Dialog，而且都是继承DialogFragment的。

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sample);
        initView();
    }

    private void initView() {
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.openDrawer(GravityCompat.START);
        drawer.clearAnimation();
        drawer.setAnimation(null);
        drawer.setLayoutAnimation(null);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();
        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
        navigationView.clearAnimation();
        navigationView.setAnimation(null);
        navigationView.setLayoutAnimation(null);

        bitmapView = findViewById(R.id.iv_image);
        bitmapView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                bitmapView.setVisibility(View.GONE);
            }
        });


        rvImage = (RecyclerView) findViewById(R.id.rv_image);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
        adapter = new MainAdapter(this, selectedPhotoList);
        rvImage.setLayoutManager(linearLayoutManager);
        rvImage.setAdapter(adapter);
        SnapHelper snapHelper = new PagerSnapHelper();
        snapHelper.attachToRecyclerView(rvImage);
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.sample, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        bitmapView.setVisibility(View.GONE);

        int id = item.getItemId();
        switch (id) {
            case R.id.camera://单独使用相机

                EasyPhotos.createCamera(this)
                        .setFileProviderAuthoritiesText("com.huantansheng.easyphotos.sample.fileprovider")
                        .start(101);

                break;

            case R.id.album_single://相册单选，无相机功能

                EasyPhotos.createAlbum(this, false)
                        .start(101);

                break;

            case R.id.album_multi://相册多选，无相机功能

                EasyPhotos.createAlbum(this, false)
                        .setCount(9)
                        .start(101);

                break;

            case R.id.album_camera_single://相册单选，有相机功能

                EasyPhotos.createAlbum(this, true)
                        .setFileProviderAuthoritiesText("com.huantansheng.easyphotos.sample.fileprovider")
                        .start(101);
                break;

            case R.id.album_camera_multi://相册多选，有相机功能

                EasyPhotos.createAlbum(this, true)
                        .setFileProviderAuthoritiesText("com.huantansheng.easyphotos.sample.fileprovider")
                        .setCount(22)
                        .start(101);

                break;

            case R.id.album_ad://相册中包含广告

                // 需要在启动前创建广告view
                // 广告view不能有父布局
                // 广告view可以包含子布局
                // 广告View的数据可以在任何时候绑定
                initAdViews();

                //启动方法，装载广告view
                EasyPhotos.createAlbum(this, true)
                        .setFileProviderAuthoritiesText("com.huantansheng.easyphotos.sample.fileprovider")
                        .setCount(9)
                        .setAdView(photosAdView, photosAdLoaded, albumItemsAdView, albumItemsAdLoaded)
                        .start(101);

                break;

            case R.id.album_size://只显示限制尺寸或限制文件大小以上的图片

                EasyPhotos.createAlbum(this, true)
                        .setFileProviderAuthoritiesText("com.huantansheng.easyphotos.sample.fileprovider")
                        .setCount(9)
                        .setMinWidth(500)
                        .setMinHeight(500)
                        .setMinFileSize(1024 * 10)
                        .start(101);

                break;

            case R.id.album_original_usable://显示原图按钮，并且默认选中，按钮可用

                EasyPhotos.createAlbum(this, false)
                        .setCount(9)
                        .setOriginalMenu(true, true, null)
                        .start(101);

                break;

            case R.id.album_original_unusable://显示原图按钮，并且默认不选中，按钮不可用。使用场景举例：仅VIP可以上传原图

                boolean isVip = false;//假设获取用户信息发现该用户不是vip

                EasyPhotos.createAlbum(this, true)
                        .setFileProviderAuthoritiesText("com.huantansheng.easyphotos.sample.fileprovider")
                        .setCount(9)
                        .setOriginalMenu(false, isVip, "该功能为VIP会员特权功能")
                        .start(101);

                break;

            case R.id.album_selected://相册中包含默认勾选图片

                EasyPhotos.createAlbum(this, true)
                        .setFileProviderAuthoritiesText("com.huantansheng.easyphotos.sample.fileprovider")
                        .setCount(9)
                        .setSelectedPhotos(selectedPhotoList)
//                        .setSelectedPhotoPaths(selectedPhotoPathList)两种方式参数类型不同，根据情况任选
                        .start(101);

                break;

            case R.id.addWatermark: //给图片添加水印

                if (selectedPhotoList.isEmpty()) {
                    Toast.makeText(this, "没选图片", Toast.LENGTH_SHORT).show();
                    return true;
                }

                //这一步（250行和251行）如果图大的话会耗时，但耗时不长，你可以在异步操作。另外copy出来的bitmap在确定不用的时候记得回收，如果你用Glide操作过copy出来的bitmap那就不要回收了，否则Glide会报错。
                Bitmap watermark = BitmapFactory.decodeResource(getResources(), R.drawable.watermark).copy(Bitmap.Config.RGB_565, true);
                bitmap = BitmapFactory.decodeFile(selectedPhotoList.get(0).path).copy(Bitmap.Config.ARGB_8888, true);

                //给图片添加水印的api
                EasyPhotos.addWatermark(watermark, bitmap, 1080, 20, 20, true);

                bitmapView.setVisibility(View.VISIBLE);
                bitmapView.setImageBitmap(bitmap);
                Toast.makeText(SampleActivity.this, "水印在左下角", Toast.LENGTH_SHORT).show();

                break;

            case R.id.face_detection://人脸检测
                if (selectedPhotoList.isEmpty()) {
                    Toast.makeText(this, "没选图片", Toast.LENGTH_SHORT).show();
                    return true;
                }

                progressDialog = ProgressDialog.show(this, null, null);

                //因为返回脸部信息后要在该bitmap做绘图，所以需要copy出来一个新的bitmap。当然，如果你不需要在bitmap上绘制，可以不用Copy。另外copy耗时，虽然耗时不长，建议开线程操作。最后提醒你copy出来的bitmap在确定不用的时候要回收，如果你用Glide操作过copy出来的bitmap那就不要回收了，否则Glide会报错。。
                bitmap = BitmapFactory.decodeFile(selectedPhotoList.get(0).path).copy(Bitmap.Config.RGB_565, true);

                //人脸检测api，无需考虑线程问题，EasyPhotos内部已经处理好了
                EasyPhotos.getFaceInformation(this, bitmap, 1, new FaceCallBackOnUiThread() {
                    @Override
                    public void onSuccess(final ArrayList<FaceInformation> faces) {
                        Toast.makeText(SampleActivity.this, "检测到" + faces.size() + "张人脸", Toast.LENGTH_SHORT).show();
                        for (FaceInformation face : faces) {
                            Canvas canvas = new Canvas(bitmap);
                            //画出人脸的区域
                            Paint paint = new Paint();
                            paint.setColor(Color.BLUE);
                            paint.setStrokeWidth(2);
                            paint.setStyle(Paint.Style.STROKE);//设置话出的是空心方框而不是实心方块
                            canvas.drawRect(face.faceRect, paint);
                            canvas.drawRect(face.leftEsyRect, paint);
                            canvas.drawRect(face.rightEsyRect, paint);
                            canvas.drawRect(face.mouthRect, paint);
                            canvas.drawRect(face.noseRect, paint);
                        }


                        bitmapView.setVisibility(View.VISIBLE);
                        bitmapView.setImageBitmap(bitmap);
                        progressDialog.dismiss();
                    }

                    @Override
                    public void onFailed() {
                        progressDialog.dismiss();
                        Toast.makeText(SampleActivity.this, "未检测到", Toast.LENGTH_SHORT).show();

                    }
                });

                break;

        }

        return true;
    }

    /**
     * 需要在启动前创建广告view
     * 广告view不能有父布局
     * 广告view可以包含子布局
     * 为了确保广告view地址不变，设置final会更安全
     */

    private void initAdViews() {

        //模拟启动EasyPhotos前广告已经装载完毕
        initPhotosAd();

        //模拟不确定启动EasyPhotos前广告是否装载完毕
        initAlbumItemsAd();

    }

    /**
     * 模拟启动EasyPhotos前广告已经装载完毕
     */
    private void initPhotosAd() {
        photosAdView = (RelativeLayout) getLayoutInflater().inflate(R.layout.ad_photos, null, false);//不可以有父布局，所以inflate第二个参数必须为null，并且布局文件必须独立
        ((TextView) photosAdView.findViewById(R.id.tv_title)).setText("photosAd广告");
        ((TextView) photosAdView.findViewById(R.id.tv_content)).setText("github上star一下了解EasyPhotos的最新动态,这个布局和数据都是由你定制的");
        photosAdLoaded = true;
    }

    /**
     * 模拟不确定启动EasyPhotos前广告是否装载完毕
     * 模拟5秒后网络回调
     */
    private void initAlbumItemsAd() {
        albumItemsAdView = (RelativeLayout) getLayoutInflater().inflate(R.layout.ad_album_items, null, false);//不可以有父布局，所以inflate第二个参数必须为null，并且布局文件必须独立

        //模拟5秒后网络回调
        rvImage.postDelayed(new Runnable() {
            @Override
            public void run() {
                ((ImageView) albumItemsAdView.findViewById(R.id.iv_image)).setImageResource(R.mipmap.ad);
                ((TextView) albumItemsAdView.findViewById(R.id.tv_title)).setText("albumItemsAd广告");
                photosAdLoaded = true;//正常情况可能不知道是先启动EasyPhotos还是数据先回来，所以这里加个标识，如果是后启动EasyPhotos，那么EasyPhotos会直接加载广告
                EasyPhotos.notifyAlbumItemsAdLoaded();
            }
        }, 5000);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (RESULT_OK == resultCode) {

            //返回对象集合：如果你需要了解图片的宽、高、大小、用户是否选中原图选项等信息，可以用这个
            ArrayList<Photo> resultPhotos = data.getParcelableArrayListExtra(EasyPhotos.RESULT_PHOTOS);

            //返回图片地址集合：如果你只需要获取图片的地址，可以用这个
            ArrayList<String> resultPaths = data.getStringArrayListExtra(EasyPhotos.RRESULT_PATHS);
            //返回图片地址集合时如果你需要知道用户选择图片时是否选择了原图选项，用如下方法获取
            boolean selectedOriginal = data.getBooleanExtra(EasyPhotos.RESULT_SELECTED_ORIGINAL, false);


            selectedPhotoList.clear();
            selectedPhotoList.addAll(resultPhotos);
            adapter.notifyDataSetChanged();
            rvImage.smoothScrollToPosition(0);

        } else if (RESULT_CANCELED == resultCode) {
            Toast.makeText(this, "cancel", Toast.LENGTH_SHORT).show();
        }
    }


}

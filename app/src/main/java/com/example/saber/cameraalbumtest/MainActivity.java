package com.example.saber.cameraalbumtest;

import android.Manifest;
import android.content.ContentUris;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

public class MainActivity extends AppCompatActivity {

    private Button btnTakePhoto;
    private Button btnChoosePhoto;
    private ImageView ivPhoto;

    private Uri imageUri;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ivPhoto = (ImageView) findViewById(R.id.iv_photo);
        btnTakePhoto = (Button) findViewById(R.id.btn_take_photo);

        btnTakePhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //创建File对象，用于存储拍照后的图片
                File outputImage = new File(getExternalCacheDir(),"output_image.jpg");

                try{
                    if(outputImage.exists()){
                        outputImage.delete();
                    }
                    //创建文件
                    outputImage.createNewFile();
                }catch (IOException e){
                    e.printStackTrace();;
                }

                if(Build.VERSION.SDK_INT >= 24){
                    //Android7.0以上  将File对象转换为Uri对象
                    //第一个参数为context  第二个是任意唯一的字符串,注册时用到的authorities  第三个是File对象
                    imageUri = FileProvider.getUriForFile(MainActivity.this,"com.example.cameraalbumtest.fileprovider",outputImage);
                }else {
                    //Android7.0开始，直接使用本地真实路径Uri被认为是不安全的
                    imageUri = Uri.fromFile(outputImage);
                }

                //启动相机程序
                Intent intent = new Intent("android.media.action.IMAGE_CAPTURE");
                intent.putExtra(MediaStore.EXTRA_OUTPUT,imageUri);
                startActivityForResult(intent,1);
            }
        });


        btnChoosePhoto = (Button) findViewById(R.id.btn_choose_from_album);
        btnChoosePhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //运行时权限
                if(ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
                    ActivityCompat.requestPermissions(MainActivity.this,new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},1);
                }else {
                    openAlbum();
                }
            }

        });

    }

    /**
     * 打开相册
     */
    private void openAlbum() {
        Intent intent = new Intent("android.intent.action.GET_CONTENT");
        intent.setType("image/*");
        startActivityForResult(intent,2);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode){
            case 1:
                if(grantResults.length>0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    openAlbum();
                }else {
                    Toast.makeText(MainActivity.this, "You denied the permission", Toast.LENGTH_SHORT).show();
                }
                break;
        }
    }

    /**
     * 拍完照后的回调
     * 选择完照片的回调
     * @param requestCode
     * @param resultCode
     * @param data
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode){
            case 1:
                if(resultCode == RESULT_OK){
                    try{
                        //将拍摄照片显示出来
                        Bitmap bitmap = BitmapFactory.decodeStream(getContentResolver().openInputStream(imageUri));

                        ivPhoto.setImageBitmap(bitmap);

                    }catch (FileNotFoundException e){
                        e.printStackTrace();
                    }

                }
                break;
            case 2:
                if(resultCode == RESULT_OK){
                    //判断手机版本号
                    if(Build.VERSION.SDK_INT>=19){
                        //4.4及以上的版本
                        handleImageOnKitKat(data);
                    }else {
                        handleImageBrforeKitKat(data);
                    }
                }
                break;
        }
    }
    /**
     * 4.4以下的
     */
    private void handleImageBrforeKitKat(Intent data) {
        Uri uri = data.getData();
        String imagePath = getImagePath(uri,null);
        displayImage(imagePath);
    }

    /**
     * 4.4以上处理图片的方法
     * @param data
     */
    private void handleImageOnKitKat(Intent data) {

        String imagePath = null;
        Uri uri = data.getData();
        if(DocumentsContract.isDocumentUri(this,uri)){
            //如果是document类型的uri,则通过document id 处理
            String docId = DocumentsContract.getDocumentId(uri);
            if("com.android.providers.media.documents".equals(uri.getAuthority())){
                //解析出数字格式的id
                String id = docId.split(":")[1];
                String selection = MediaStore.Images.Media._ID + "=" +id;
                imagePath = getImagePath(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,selection);
            }else if("com.android.providers.downloads.documents".equals(uri.getAuthority())){
                Uri contentUri = ContentUris.withAppendedId(Uri.parse("content://downloads/public_downloads"),Long.valueOf(docId));
                imagePath = getImagePath(contentUri,null);
            }

        }else if("content".equalsIgnoreCase(uri.getScheme())){
            //如果是content类型的Uri，则使用普通方式处理
            imagePath = getImagePath(uri,null);
        }else if ("file".equalsIgnoreCase(uri.getScheme())){
            //如果是file类型的Uri，直接获取图片路径即可
            imagePath = uri.getPath();
        }

        //根据图片路径显示图片
        displayImage(imagePath);

    }


    /**
     * 获取图片路径
     */
    public String getImagePath(Uri uri,String selection){
        String path = null;
        //通过Uri和selection来获取真实的图片路径
        Cursor c = getContentResolver().query(uri,null,selection,null,null);
        if(c != null){
            while(c.moveToNext()){
                path = c.getString(c.getColumnIndex(MediaStore.Images.Media.DATA));
            }
            c.close();
        }
        return  path;
    }


    /**
     * 显示图片
     */
    public void displayImage(String imagePath){
        if(imagePath != null){
            Bitmap bitmap = BitmapFactory.decodeFile(imagePath);
            ivPhoto.setImageBitmap(bitmap);
        }else {
            Toast.makeText(this, "failed to get image", Toast.LENGTH_SHORT).show();
        }
    }







}






















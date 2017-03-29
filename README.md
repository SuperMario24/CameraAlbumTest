# CameraAlbumTest
拍照/从相册中选择图片


1.创建缓存文件夹，用于缓存图片，路径在  SDcard/Android/data/包名/cache 文件夹中
File file = new File(getExternalCacheDir,"cacheImg.jpg");


2.将File对象转化为uri对象 ---- Android7.0以上 不能直接转,需要调用FileProvider的getUriForFile（）方法获取，第一个参数为context，第二个参数为任意
唯一字符串，为注册provider的authorities,第三个参数为file对象. Android7.0以下可以直接调用Uri.fromFile()方法直接转
if(Build.VERSION.SDK_INT>=24){
    imggeUri = FileProvider.getUriForFile(context,"com.example.cameraalbumtest.fileprovider",file);
}else{
    imageUri = Uri.fromFile(file);
}



3.启动相机程序(拍摄)
Intent intent = new Intent("android.media.action.IMAGE_CAPTURE");
intent.putExtra(MediaStore.EXTRA_OUTPUT,imageUri);
startActivityForResult(intent,1);

4.将拍摄的照片显示出来
Bitmap bitmap = BitmapFactory.decodeStram(getContentResolver().openInputStream(imageUri));

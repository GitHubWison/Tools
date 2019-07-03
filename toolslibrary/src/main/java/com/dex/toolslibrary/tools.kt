package com.dex.toolslibrary

import android.annotation.TargetApi
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.hardware.Camera
import android.net.Uri
import android.net.wifi.WifiManager
import android.os.Build
import android.provider.Settings
import android.util.Log
import java.io.*
import java.lang.reflect.InvocationTargetException
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import kotlin.math.max


//找出拍摄照片的最佳尺寸
fun List<Camera.Size>.findBestSize(): Array<Int> {
    var bestSize: Camera.Size? = null
    for (size in this) {
        if (size.width.toFloat() / size.height.toFloat() == 16.0f / 9.0f) {
            if (bestSize == null) {
                bestSize = size
            } else {
                if (size.width > bestSize.width) {
                    bestSize = size
                }
            }
        }
    }
    return if (bestSize == null) {
        arrayOf(1920, 1080)
    } else {
        arrayOf(bestSize.width, bestSize.height)
    }
}

//指定比例(宽/长)在限定范围内能缩放的最大宽高
fun Double.scaleBetween(outerWidth: Int, outerHeight: Int): Array<Int> {
    var targetWidth = outerWidth.toDouble()
    var targetHeight = outerWidth.toDouble() * this
    if (targetHeight > outerHeight) {
        targetWidth = outerHeight * ((1.toDouble()) / this)
        targetHeight = outerHeight.toDouble()
    }
    return arrayOf(targetWidth.toInt(), targetHeight.toInt())
}

//计算activity的长高
fun Activity.findScreenSize(): Array<Int> {
    val resources = this.resources
    val dm = resources.displayMetrics
    val density = dm.density
    val width = dm.widthPixels
    val height = dm.heightPixels
    return arrayOf(width, height)
}

//图片二值化处理
fun Bitmap.toGray(): Bitmap {
    val width = this.width
    val height = this.height
    val binaryBitmap = this.copy(Bitmap.Config.ARGB_8888, true)
    for (x in 0 until width) {
        for (y in 0 until height) {
            val pixel = binaryBitmap.getPixel(x, y)
            val alpha = (pixel and (0xFF shl 16))
            val red = (pixel and 0x00FF0000) shr 16
            val green = (pixel and 0x0000FF00) shr 8
            val blue = (pixel and 0x000000FF)


            var newGray = red.toDouble() * 0.3 + green.toDouble() * 0.59 + blue.toDouble() * 0.11
//            newGray = if (newGray < 90) {
//                0.toDouble()
//            } else {
//                225.toDouble()
//            }
//
////            newGray = 225.toDouble()
            val newGrayInt = newGray.toInt()
            val newColor = alpha or (newGrayInt shl 16) or (newGrayInt shl 8) or newGrayInt
            binaryBitmap.setPixel(x, y, newColor)
        }
    }
    return binaryBitmap

}

//查找图片中最深的颜色
fun Bitmap.findTheBestGrayNumber(): Int {
    val copyBitmap = this.copy(Bitmap.Config.ARGB_8888, true)
    val width = this.width
    val height = this.height
    var res = 0

    for (x in 0 until width) {
        for (y in 0 until height) {
            val pixel = copyBitmap.getPixel(x, y)
            val alpha = (pixel and -0x1000000)
            val red = (pixel and 0x00FF0000) shr 16
            val green = (pixel and 0x0000FF00) shr 8
            val blue = (pixel and 0x000000FF)


            var newGray = red.toDouble() * 0.3 + green.toDouble() * 0.59 + blue.toDouble() * 0.11
            if (newGray.toInt() == 0) {
                Log.e("pixel==", "${newGray.toInt()}")
            }
            res = max(newGray.toInt(), res)

        }
    }
    return res


}

//执行linux命令
fun String.executeCMD() {

    var outPutStream: DataOutputStream? = null
    var bufferReader: BufferedReader? = null
    try {
        val p = Runtime.getRuntime().exec("sh")
        outPutStream = DataOutputStream(p.outputStream)
        bufferReader = BufferedReader(InputStreamReader(p.inputStream))
        outPutStream.writeBytes("${this}\n")
        outPutStream.flush()
        outPutStream.writeBytes("exit\n")

    } catch (e: Exception) {
        e.printStackTrace()
    } finally {
        outPutStream?.close()
        bufferReader?.close()
    }
}

//将assets中的文件覆盖拷贝到sd卡中的指定目录下
fun Context.copyAssetToSDFileByCover(assetPath: String, sdFile: File, name: String) {
    val inputStream = this.assets.open(assetPath)
    if (!sdFile.exists()) {
        sdFile.mkdirs()
    }
    val dataFile = File(sdFile.path,name)
    val buffer = ByteArray(1024)
    var n: Int = inputStream.read(buffer)
    val outputStream = FileOutputStream(dataFile)
    while (n != -1) {
        outputStream.write(buffer,0, n)
        n = inputStream.read(buffer)
    }
    inputStream.close()
    outputStream.close()
}
//如果目标文件或文件夹不存在则拷贝,否则不拷贝
fun Context.copyAssetToSDFileByCheck(assetPath: String, sdFile: File, name: String){
    val targetFile = File(sdFile.path,name)
    if ((!sdFile.exists()) || (!targetFile.exists())) {
        this.copyAssetToSDFileByCover(assetPath,sdFile,name)
    }

}
//将字符串转为md5格式
fun String.toMD5(): String {
    try {
        val instance: MessageDigest = MessageDigest.getInstance("MD5")//获取md5加密对象
        val digest: ByteArray = instance.digest(this.toByteArray())//对字符串加密，返回字节数组
        var sb = StringBuffer()
        for (b in digest) {
            var i: Int = b.toInt() and 0xff//获取低八位有效值
            var hexString = Integer.toHexString(i)//将整数转化为16进制
            if (hexString.length < 2) {
                hexString = "0$hexString"//如果是一位的话，补0
            }
            sb.append(hexString)
        }
        return sb.toString()

    } catch (e: NoSuchAlgorithmException) {
        e.printStackTrace()
    }

    return ""

}

//获取app的版本号
fun Context.getAppVersion(): Int {
    val info = this.packageManager.getPackageInfo(this.packageName, PackageManager.GET_CONFIGURATIONS)
    return info.versionCode
}

//请求修改系统设置的权限
@TargetApi(Build.VERSION_CODES.M)
fun Activity.requestModifySystemSettings()
{
//    val canWrite = Settings.System.canWrite(this)
//    if (!canWrite) {
    try {
        startActivityForResult(Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS).apply {
            this.data = Uri.parse("package:$packageName")
        },1001)
    }catch (e:Exception)
    {
        e.printStackTrace()
    }

//    }
}
//返回系统权限是否可设置
@TargetApi(Build.VERSION_CODES.M)
fun Activity.isSystemSettingsCanWrite():Boolean{
    var res:Boolean = true
    try{
        res = Settings.System.canWrite(this)
    }
    catch (nme:NoSuchMethodError)
    {
        nme.printStackTrace()
    }
    catch (e:Exception)
    {
        e.printStackTrace()
    }
    return res
}
//返回wifi热点是否开启
fun Context.isAPOpen():Boolean{
    try {
        val manager = this.getSystemService(Context.WIFI_SERVICE) as WifiManager
        //通过放射获取 getWifiApState()方法
        val method = manager.javaClass.getDeclaredMethod("getWifiApState")
        //调用getWifiApState() ，获取返回值
        val state = method.invoke(manager) as Int
        //通过放射获取 WIFI_AP的开启状态属性
        val field = manager.javaClass.getDeclaredField("WIFI_AP_STATE_ENABLED")
        //获取属性值
        val value = field.get(manager) as Int
        //判断是否开启
        return state == value
    } catch (e: NoSuchMethodException) {
        e.printStackTrace()
    } catch (e: IllegalAccessException) {
        e.printStackTrace()
    } catch (e: InvocationTargetException) {
        e.printStackTrace()
    } catch (e: NoSuchFieldException) {
        e.printStackTrace()
    }

    return false

}

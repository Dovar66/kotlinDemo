package com.dovar.kotlindemo

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Bundle
import android.support.annotation.RequiresApi
import android.support.v7.app.AppCompatActivity

class KotlinActivity(var manager: MediaProjectionManager) : AppCompatActivity() {

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_kotlin)

        manager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        startActivityForResult(manager.createScreenCaptureIntent(), 0)
        //var与val的区别：val为只读，var为可变
        //初始化时指定类型
        var testVar: Int
        testVar = 1
        //初始化时不指定类型则必须赋值，自动识别类型
        var testVar2 = 1
        //加？表示该值可以为null
        val testVal: Int? = null
        val testVal2 = 1

        val arrays = arrayOf(11, 12, 13)

        when (testVal2) {
            1 ->
                testVar2 = 2
            2 ->
                testVar = 3
            3, 4 ->
                testVar = 4
            in 5..10 ->
                testVar = 10
        //            in co->testVar=15
        }
        //kotlin中默认class是final型，如果需要允许被继承则要指定class为open类
        //这种设计符合 Effective Java, 一书中的第 17 条原则: 允许继承的地方, 应该明确设计, 并通过文档注明, 否则应该禁止继承
        //kotlin允许多继承，如果继承了多个父类的同一个成员方法，则子类需要重写该方法并使用super<superClass>.method()指明继承的是哪个超类的
    }

    //同伴对象，替代java的静态方法
    companion object {
        fun testMethod(a: Int = 0, b: Int = 1): Int {
            return a + b
        }
    }

    internal fun testMethod(a: Int, b: Int): Int {
        return a + b
    }

    //返回值类型为Unit即表示函数无返回时，应当省略成testMethodUnit2这样
    internal fun testMethodUnit(a: Int, b: Int): Unit {
        a + b
    }

    internal fun testMethodUnit2(a: Int, b: Int) {
        a + b
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            var mediaProjection: MediaProjection = manager.getMediaProjection(requestCode, data)
        }
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    fun display(projection: MediaProjection){
        var imageReader:ImageReader= ImageReader.newInstance(100,100,PixelFormat.RGB_565,1)
        projection.createVirtualDisplay("screenshot",100,100,3,DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,imageReader.surface,null,null)
    }
}

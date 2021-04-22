package com.orange.tinkerhotfix;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;

public class MainActivity extends AppCompatActivity {

    /**
     * 一 . Tinker dex class 热修复流程  分为三个步骤
     * 1.生成补丁
     * 2.旧dex 与 补丁的合成
     * 3.全量加载dex
     * @param savedInstanceState
     */

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }
}

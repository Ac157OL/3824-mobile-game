package com.example.drawingapp;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.view.View;
import androidx.appcompat.app.AppCompatActivity;
import com.example.drawingapp.NetworkService;

// 定义LoginActivity类，继承自AppCompatActivity，用于处理登录逻辑
public class LoginActivity extends AppCompatActivity {
    // 定义用于输入用户名、IP地址和端口号的EditText控件
    private EditText et_name;
    private EditText et_ip;
    private EditText et_port;
    // 定义NetworkService变量，用于与网络服务进行交互
    private NetworkService networkService;
    // 定义一个布尔变量，用于标记服务是否已绑定
    private boolean isBound = false;
    private RadioGroup radioGroup;
    private String selectedTeam;

    private MediaPlayer mediaPlayer;

    // onCreate方法在Activity创建时被调用
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 设置当前Activity的布局
        setContentView(R.layout.activity_login);

        // 初始化视图控件
        initializeViews();
        // 设置事件监听器
        setupEventListeners();


    }

    // 初始化视图控件的方法
    private void initializeViews() {
        // 通过ID找到对应的EditText控件
        et_name = findViewById(R.id.et_name);
        et_ip = findViewById(R.id.et_ip);
        et_port = findViewById(R.id.et_port);
        radioGroup = findViewById(R.id.et_group);
    }

    // 设置事件监听器的方法
    private void setupEventListeners() {
        // 通过ID找到登录和退出按钮，并设置点击事件监听器
        Button login = findViewById(R.id.login);
        Button quit = findViewById(R.id.quit);

        // 为登录按钮设置点击事件处理方法
        login.setOnClickListener(this::attemptLogin);
        // 为退出按钮设置点击事件处理方法
        quit.setOnClickListener(this::showQuitDialog);

        // 设置RadioGroup的监听器以获取选中的文本内容
        radioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            RadioButton selectedRadioButton = findViewById(checkedId);
            selectedTeam = selectedRadioButton.getText().toString();
        });
    }

    // 尝试登录的方法
    private void attemptLogin(View view) {
        // 获取用户输入的用户名、IP地址和端口号
        String name = et_name.getText().toString();
        String ip = et_ip.getText().toString();
        String port = et_port.getText().toString();

        // 检查输入是否完整
        if (name.isEmpty() || ip.isEmpty() || port.isEmpty()) {
            // 如果输入不完整，显示提示信息
            Toast.makeText(LoginActivity.this, "请完整输入用户名、IP和端口！", Toast.LENGTH_SHORT).show();
        } else if (selectedTeam == null) {
            // 如果没有选择队伍，显示提示信息
            Toast.makeText(LoginActivity.this, "请选择队伍！", Toast.LENGTH_SHORT).show();
        } else if (isBound) {
            // 如果服务已绑定，通过networkService发起连接请求
            networkService.setupConnection(ip, port, new NetworkService.NetworkCallback() {
                @Override
                public void onConnected() {
                    // 连接成功时的回调
                    runOnUiThread(() -> {
                        Toast.makeText(LoginActivity.this, "连接成功！", Toast.LENGTH_SHORT).show();
                        // 连接成功后发送用户名和选中的队伍信息
                        networkService.sendUserInfo(name, selectedTeam);
                        // 连接成功后跳转到MainActivity
                        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                        startActivity(intent);
                    });
                }
                @Override
                public void onConnectionFailed(Exception e) {
                    // 连接失败时的回调
                    runOnUiThread(() -> {
                        // 提供更详细的错误信息以帮助用户诊断问题
                        String errorMessage = "连接失败，请检查您的网络设置！";
                        if (e instanceof UnknownHostException) {
                            errorMessage = "无法解析主机地址，请检查IP地址是否正确！";
                        } else if (e instanceof SocketTimeoutException) {
                            errorMessage = "连接超时，请检查端口号或网络设置！";
                        } else if (e instanceof ConnectException) {
                            errorMessage = "无法连接到服务器，请确保服务器运行正常！";
                        }
                        Toast.makeText(LoginActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                    });
                }

                @Override
                public void onMessageReceived(String message) {
                    // 接收到消息时的回调（如果需要处理）
                }
            });
        } else {
            // 如果服务未绑定，显示提示信息
            Toast.makeText(LoginActivity.this, "服务未绑定，无法建立连接", Toast.LENGTH_SHORT).show();
        }
    }

    // 显示退出对话框的方法
    private void showQuitDialog(View view) {
        // 创建并显示AlertDialog
        AlertDialog.Builder dialog = new AlertDialog.Builder(LoginActivity.this);
        dialog.setTitle("关闭提示")
                .setMessage("确定退出登录？")
                .setPositiveButton("确定", (dialogInterface, i) -> System.exit(0))
                .setNegativeButton("取消", null)
                .show();
    }

    // 在Activity开始时调用，用于绑定服务
    @Override
    protected void onStart() {
        super.onStart();
        // 创建意图并绑定服务
        Intent intent = new Intent(this, NetworkService.class);
        bindService(intent, serviceConnection, BIND_AUTO_CREATE);
        // 初始化 MediaPlayer，并设置音频文件
        mediaPlayer = MediaPlayer.create(this, R.raw.backround);
        // 设置循环播放
        mediaPlayer.setLooping(true);
        // 播放音乐
        mediaPlayer.start();
        mediaPlayer.setVolume(1f, 1f);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (isBound) {
            // 如果服务已绑定，解除绑定
            unbindService(serviceConnection);
            isBound = false;
        }
        // 释放 MediaPlayer 资源
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 确保在Activity销毁时解除服务绑定
        if (isBound) {
            unbindService(serviceConnection);
            isBound = false;
        }

    }

    // 定义服务连接对象，用于处理服务绑定和解绑时的回调
    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            // 服务绑定成功时调用
            NetworkService.LocalBinder binder = (NetworkService.LocalBinder) service;
            networkService = binder.getService();
            isBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            // 服务意外断开连接时调用
            isBound = false;
        }
    };
}

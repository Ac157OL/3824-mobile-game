package com.example.drawingapp;

import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayDeque;
import java.util.Deque;

import org.pytorch.IValue;
import org.pytorch.Module;
import org.pytorch.Tensor;
import org.pytorch.torchvision.TensorImageUtils;
import org.pytorch.MemoryFormat;
import android.widget.TextView;
import android.os.Handler;
import android.widget.Toast;


// 定义 MainActivity 类，继承自 AppCompatActivity，用于处理应用的主界面
public class MainActivity extends AppCompatActivity implements NetworkService.NetworkCallback {

    private boolean firstOneReceived = false; // 标记是否接收到FIRST:one的消息
    private TextView input;
    private TextView selfDis;
    private TextView display; // Display 文本框
    private DrawView mDrawView; // 自定义的绘图视图
    private ImageView imageView; // 图像视图

    private MediaPlayer mediaPlayer;

    private TextView point_red;
    private TextView point_blue;
    private TextView userTextView;
    private TextView player;
    private TextView topic;
    // 定义与网络服务相关的变量
    private NetworkService networkService; // 网络服务对象
    private boolean isBound = false; // 标记是否已绑定服务

    // 定义服务连接对象，用于处理服务连接和断开时的操作
    private final ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            NetworkService.LocalBinder binder = (NetworkService.LocalBinder) service;
            networkService = binder.getService();
            isBound = true;


            //设置回调接口
            networkService.setupConnectionCallback(MainActivity.this);
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            isBound = false;
        }
    };

    @Override
    public void onConnected() {
        // 在主线程更新UI
//        runOnUiThread(() -> Toast.makeText(MainActivity.this, "连接成功！", Toast.LENGTH_SHORT).show());
    }


    @Override
    public void onConnectionFailed(Exception e) {
        // 在主线程更新UI
//        runOnUiThread(() -> Toast.makeText(MainActivity.this, "连接失败：" + e.getMessage(), Toast.LENGTH_SHORT).show());
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main); // 设置界面布局

        // 初始化界面控件
        input = findViewById(R.id.input);
        // 返回按钮
        Button back = findViewById(R.id.back);
        // 发送按钮
        Button send = findViewById(R.id.send);
        // 删除按钮
        Button shanchu = findViewById(R.id.shanchu);
        // 清空按钮
        Button qingkong = findViewById(R.id.qingkong);
        imageView = findViewById(R.id.imageView);
        display = findViewById(R.id.Display); // 初始化 Display 文本框
        player = findViewById(R.id.player);

        selfDis = findViewById(R.id.selfDis);



        ///zzz1
        point_red = findViewById(R.id.point_red);
        point_blue = findViewById(R.id.point_blue);
        userTextView = findViewById(R.id.user);

        //zzz2
        topic = findViewById(R.id.topic);
        point_red.setText("0");
        point_blue.setText("0");

        // 为按钮设置监听器
        addOperatorButtonListener(R.id.jia, "+");
        addOperatorButtonListener(R.id.jian, "-");
        addOperatorButtonListener(R.id.cheng, "*");
        addOperatorButtonListener(R.id.zuokuohao, "(");
        addOperatorButtonListener(R.id.youkuohao, ")");
        addOperatorButtonListener(R.id.chu, "/");

        // 初始化画板视图
        initView();

        // 绑定服务
        Intent serviceIntent = new Intent(this, NetworkService.class);
        bindService(serviceIntent, connection, Context.BIND_AUTO_CREATE);


        // 设置返回按钮的点击事件处理
        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (isBound) {
                    unbindService(connection);
                    isBound = false;
                    networkService.closeConnection();
                }
                // 跳转到 LoginActivity
                Intent backIntent = new Intent(MainActivity.this, LoginActivity.class);
                startActivity(backIntent);
                finish();
            }
        });
        // 设置清空按钮的点击事件处理
        qingkong.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mDrawView.clear();
                input.setText("");
            }
        });

        // 设置删除按钮的点击事件处理
        shanchu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 获取当前 TextView 的文本
                String currentText = selfDis.getText().toString();
                // 检查文本长度，如果大于0，则删除最后一个字符
                if (!currentText.isEmpty()) {
                    selfDis.setText(currentText.substring(0, currentText.length() - 1));
                }
            }
        });
        send.setOnClickListener(v -> {
            // 获取selfDis文本框内的文本内容并去除前后空白
            String messageToSend = selfDis.getText().toString().trim();
            // 获取topic和display文本框的内容
            String topicNumbers = topic.getText().toString();
            String displayedNumbers = display.getText().toString();

            // 从messageToSend中提取所有数字字符
            String digitsToSend = messageToSend.replaceAll("\\D", "");

            // 检查提取的数字字符是否在topic字符串中，并且没有在display中重复使用
            for (char c : digitsToSend.toCharArray()) {
                if (!topicNumbers.contains(String.valueOf(c))) {
                    displayError("输入错误，数字必须来自题目给定的数字");
                    return;
                }
                // 检查数字使用次数是否超过了topic中的次数
                long countInTopic = topicNumbers.chars().filter(ch -> ch == c).count();
                long countInDisplay = displayedNumbers.chars().filter(ch -> ch == c).count();
                if (countInDisplay >= countInTopic) {
                    displayError("字符输入重复");
                    return;
                }
            }

            // 检查文本内容是否符合要求
            if (firstOneReceived) {
                // 特殊客户端，只能输入一个数字
                if (!messageToSend.matches("\\d")) {
                    displayError("输入错误，请输入一个数字");
                    return; // 不发送消息，直接返回
                }
            } else {
                // 普通客户端，检查是否为一个运算符加一个数字
                if (!messageToSend.matches("[+\\-*/\\d]")) {
                    displayError("输入错误，请输入一个运算符加一个数字");
                    return; // 不发送消息，直接返回
                }
            }

            // 检查display和selfDis中的数字字符总数是否达到4个
            String allDigits = displayedNumbers.replaceAll("\\D", "") + digitsToSend;
            if (allDigits.length() >= 4) {
                // 新建一个字符串变量，将display和selfDis的内容连接起来
                String expression = displayedNumbers + messageToSend;
                // 检查表达式中的括号是否匹配
                if (!areParenthesesBalanced(expression)) {
                    displayError("括号不匹配");
                    return;// 不发送消息，直接返回
                }
            }

            // 检查文本内容是否为空，且是否已绑定服务且服务已连接
            if (!messageToSend.isEmpty() && isBound && networkService.isConnected()) {
                // 调用NetworkService的sendMessage方法，将文本内容发送到服务器
                networkService.sendMessage(messageToSend);
                // 发送成功后，修改firstOneReceived标志为false
                firstOneReceived = false;
                // 发送后清空selfDis文本框
                selfDis.setText("");
            } else if (!isBound || !networkService.isConnected()) {
                // 未连接时的处理逻辑，显示提示信息
                Toast.makeText(getApplicationContext(), "网络连接失败，请检查您的网络设置", Toast.LENGTH_SHORT).show();
            }
            // 当消息框为空时，不执行任何操作，也不显示提示信息
        });
    }

    // 检查括号是否匹配的辅助方法
    private boolean areParenthesesBalanced(String expression) {
        Deque<Character> stack = new ArrayDeque<>();
        for (char ch : expression.toCharArray()) {
            if (ch == '(') {
                stack.push(ch);
            } else if (ch == ')') {
                if (stack.isEmpty() || stack.pop() != '(') {
                    return false;
                }
            }
        }
        return stack.isEmpty();
    }

    private void addOperatorButtonListener(int jia, String s) {
        Button button = findViewById(jia);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 追加操作符到TextView
                String currentText = selfDis.getText().toString();
                selfDis.setText(currentText + s); // 使用传入的 operator 变量
            }
        });
    }



    @Override
    protected void onStart() {
        super.onStart();
        // 创建意图并绑定服务

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

        // 释放 MediaPlayer 资源
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (isBound) {
            networkService.setupConnectionCallback(null);
            unbindService(connection);
            isBound = false;
            networkService.closeConnection();
        }
    }


    // 将应用的 assets 目录下的文件复制到应用的文件系统目录中
    public static String assetFilePath(Context context, String assetName) throws IOException {
        File file = new File(context.getFilesDir(), assetName);
        if (file.exists() && file.length() > 0) {
            return file.getAbsolutePath();
        }

        try (InputStream is = context.getAssets().open(assetName)) {
            try (OutputStream os = new FileOutputStream(file)) {
                byte[] buffer = new byte[4 * 1024];
                int read;
                while ((read = is.read(buffer)) != -1) {
                    os.write(buffer, 0, read);
                }
                os.flush();
            }
            return file.getAbsolutePath();
        }
    }

    // 初始化绘图视图
    private void initView() {
        mDrawView = new DrawView(this) {
            private Handler handler = new Handler();
            private Runnable recognitionRunnable = new Runnable() {
                @Override
                public void run() {
                    recognizeDrawing();
                }
            };

            @Override
            public boolean onTouchEvent(MotionEvent event) {
                boolean result = super.onTouchEvent(event);
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        // 用户开始书写，取消之前的识别任务
                        handler.removeCallbacks(recognitionRunnable);
                        break;
                    case MotionEvent.ACTION_UP:
                        // 用户停止书写，开始延时任务
                        handler.postDelayed(recognitionRunnable, 1000); // 1秒后自动识别
                        break;
                }
                return result;
            }
        };

        LinearLayout layout = findViewById(R.id.linear);
        ViewGroup.LayoutParams layoutParams = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        mDrawView.setLayoutParams(layoutParams);
        layout.addView(mDrawView);
    }


    public void updateUI(String message, int flag) {
        if (flag == 1) {
            if (message.startsWith("red")) {
                String redScore = message.substring(3).trim(); // 去掉前缀 "red" 并去除多余的空白
                point_red.setText(redScore); // 更新红色得分 TextView
            } else if (message.startsWith("blue")) {
                String blueScore = message.substring(4).trim(); // 去掉前缀 "blue" 并去除多余的空白
                point_blue.setText(blueScore); // 更新蓝色得分 TextView
            }
        } else if (flag == 2) {
            display.setText(message);
        } else if (flag == 3) {
            userTextView.setText(message);
            clearTextViewAfterDelay(userTextView, 2000); // 设置两秒后清空 userTextView
        } else if (flag == 4) {
            topic.setText(message);
        } else if (flag == 5) {
            display.setText(""); // 清空 display 文本框
        } else if (flag == 6) { // 新增的 flag 值
            player.setText(message); // 更新 player TextView
        } else {
            userTextView.setText(message);
            clearTextViewAfterDelay(userTextView, 2000); // 设置两秒后清空 userTextView
        }
    }

    @Override
    public void onMessageReceived(String message) {
        runOnUiThread(() -> {
            if (message.startsWith("SCORE:")) {
                String updatedMessage = message.substring(6); // 去掉前缀 "SCORE:"
                updateUI(updatedMessage, 1);
            } else if (message.startsWith("CLIENT_MSG:")) {
                String updatedMessage = message.substring(11).trim(); // 去掉前缀 "CLIENT_MSG:"
                updateUI(updatedMessage, 2); // 显示在 display TextView
            } else if (message.startsWith("SERVER_MSG:")) {
                String updatedMessage = message.substring(11).trim(); // 去掉前缀 "SERVER_MSG:"
                updateUI(updatedMessage, 3); // 显示在 userTextView
            } else if (message.startsWith("NUMBERS:")) {
                String updatedMessage = message.substring(9).trim(); // 去掉前缀 "NUMBERS:"
                updateUI(updatedMessage, 4); // 显示在 topic TextView
            } else if (message.startsWith("CLEAR_DISPLAY:")) {
                updateUI("", 5); // 清空 display 文本框
            } else if (message.startsWith("PLAYER:")) { // 新增的处理逻辑
                String updatedMessage = message.substring(7).trim(); // 去掉前缀 "PLAYER:"
                updateUI(updatedMessage, 6); // 显示在 player TextView
            } else if (message.startsWith("GAMEOVER:")) { // 新增的处理逻辑
                showGameOverDialog(); // 显示游戏结束的弹窗
            } else if (message.equals("FIRST:one")) {
                firstOneReceived = true; // 标记接收到FIRST: one
            } else {
                // 如果没有前缀，默认处理其他消息
                updateUI(message, 3); // 显示在 userTextView 并清空
            }
        });
    }

    private void showGameOverDialog() {
        new AlertDialog.Builder(this)
                .setTitle("游戏结束")
                .setMessage("游戏已经结束！")
                .setCancelable(false) // 防止用户点击对话框外部区域关闭对话框
                .setPositiveButton("确定", (dialog, which) -> {
                    // 按钮点击事件
                })
                .show();

        // 延迟两秒后返回到 LoginActivity
        new Handler().postDelayed(() -> {
            Intent intent = new Intent(MainActivity.this, LoginActivity.class);
            startActivity(intent);
            finish(); // 结束当前 Activity
        }, 2000); // 延迟 2 秒
    }



    private void clearTextViewAfterDelay(TextView textView, long delayMillis) {
        Handler handler = new Handler();
        handler.postDelayed(() -> textView.setText(""), delayMillis);
    }

    private void displayError(String errorMessage) {
        userTextView.setText(errorMessage);
        clearTextViewAfterDelay(userTextView, 2000); // 两秒后清空错误提示
        selfDis.setText(""); // 清空输入框
    }

    // 将原shibie按钮点击事件中的代码移动到此方法中
    private void recognizeDrawing() {
        // 获取绘图视图的 Bitmap 对象，并显示在 imageView 上
        Bitmap bitmap = mDrawView.getBitmap();
        if (bitmap == null) {
            return; // 直接返回，不执行后续操作
        }
        imageView.setImageBitmap(bitmap);

        // 加载 PyTorch 模型并进行推理
        Module module_ori = null;
        try {
            module_ori = Module.load(assetFilePath(MainActivity.this, "mnist_rgb_model.pt"));
        } catch (IOException e) {
            Log.e("PytorchHelloWorld", "Error reading assets", e);
            return; // 使用 return 替换 finish(); 避免因错误而关闭 Activity
        }
        Bitmap resizedBitmap = Bitmap.createBitmap(28, 28, bitmap.getConfig());
        // 创建一个Canvas对象，并设置为新的Bitmap
        Canvas canvas = new Canvas(resizedBitmap);
        // 创建Paint对象，可以设置缩放时的过滤选项
        Paint paint = new Paint();
        paint.setFilterBitmap(true);
        paint.setAntiAlias(true);
        // 将原始Bitmap绘制到Canvas上，实现缩放
        canvas.drawBitmap(bitmap, null, new RectF(0, 0, 28, 28), paint);
        // 使用单个均值和标准差转换和归一化灰度图像
        float[] meanValues = {0.485f, 0.456f, 0.406f}; // 灰度图像的均值
        float[] stdValues = {0.229f, 0.224f, 0.225f};  // 灰度图像的标准差

        final Tensor inputTensor = TensorImageUtils.bitmapToFloat32Tensor(
                resizedBitmap,
                meanValues,
                stdValues,
                MemoryFormat.CHANNELS_LAST);
        // 执行模型推断
        final Tensor outputTensor_ori = module_ori.forward(IValue.from(inputTensor)).toTensor();
        //得到结果
        float[] data = outputTensor_ori.getDataAsFloatArray();
        if (data != null && data.length > 0) {
            int maxIndex = 0; // 假设最大值的下标是0
            float maxVal = data[0]; // 假设第一个元素是最大值
            for (int i = 1; i < data.length; i++) {
                if (data[i] > maxVal) {
                    maxVal = data[i]; // 更新最大值
                    maxIndex = i; // 更新最大值的下标
                }
            }
            String stringValue = String.valueOf(maxIndex);
            System.out.println("最大元素的索引是: " + stringValue);
            input.setText(stringValue);
            // 使用 maxIndex 和 maxVal 进行后续操作
        } else {
            // 处理空数组或数据未初始化的情况
            Log.e("PytorchHelloWorld", "模型推断结果为空或未初始化");
        }
        // 在shibie按钮的逻辑执行完毕后，直接执行原queding按钮的功能
        // 从 EditText 中获取文本
        String message = input.getText().toString();
        // 将文本追加到 selfDis TextView 中
        selfDis.append(message);
        // 清空 EditText，以便输入新的消息
        input.setText("");
        mDrawView.clear();
    }

}
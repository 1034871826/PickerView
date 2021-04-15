package suyuan.pickerview;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.jetbrains.annotations.NotNull;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author suyuan
 */
public class PickerView extends View {
    public static final String TAG = "PickerView";
    private Adapter adapter;
    private int selectedTextSize = 0;
    private int unselectedTextSize = 0;
    private String selectedText;
    private Paint paint;

    /**
     * 要绘制的其他文本的个数，2个的话最后会绘制出来5个文本
     * 中间1个，上下各2个
     */
    private int otherTextNumber = 5;

    /**
     * 用来判断现在是向上滑动还是向下滑动
     */
    private int direction = MOVE_UP;
    /**
     * 上一次的scale倍数，默认为1f，即没有发生scale
     */
    private float lastScale = 1f;
    /**
     * 代表手指向上滑动
     */
    public static final int MOVE_UP = 1;
    /**
     * 代表手指向下滑动
     */
    public static final int MOVE_DOWN = -1;
    /**
     * 滑动距离
     */
    private float moveLength = 0;
    /**
     * 是否将数据进行循环
     */
    private boolean isDataRecycled = false;
    /**
     * 中间的text和上下text的间距
     */
    private int textPadding = 30;
    /**
     * 自动回滚到中间的速度
     */
    private float speed = 2;

    /**
     * 选中的文字和未被选中文字之间的距离
     */
    private float distance = textPadding + (selectedTextSize + unselectedTextSize) / 2f;

    private float selectedTextAlpha = 1f;
    private float unselectedTextAlpha = 0.5f;

    private int selectedTextColor;
    private int unselectedTextColor;

    /**
     * 测量后该控件的高度
     */
    private int viewHeight;
    /**
     * 测量后该控件的宽度
     */
    private int viewWidth;

    /**
     * 文字的最大宽度，该属性影响到空间的宽度
     */
    private float maxTextWidth;


    private float lastFingerTouchY;
    /**
     * 通过静态方法创建实例，线程池数量为3
     */
    private static final ScheduledExecutorService scheduledExecutorService = new ScheduledThreadPoolExecutor(3);
    private UpdateViewTask updateViewTask;
    private int widthMeasureSpec;
    private int heightMeasureSpec;
    private float scale = 1f;
    /**
     * 判断现在的缩放趋势是是放大还是缩小
     */
    private boolean isEnlarging = false;

    public PickerView(Context context) {
        this(context, null);
    }

    public PickerView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public PickerView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public PickerView(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context, attrs, defStyleAttr, defStyleRes);
    }

    private void init(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        TypedArray typedArray = context.getTheme().obtainStyledAttributes(attrs, R.styleable.PickerView, defStyleAttr, defStyleRes);
        int defaultSelectedTextSize = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 16f, context.getResources().getDisplayMetrics());
        int defaultUnselectedTextSize = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 13f, context.getResources().getDisplayMetrics());
        selectedTextSize = typedArray.getDimensionPixelSize(R.styleable.PickerView_selected_text_size, defaultSelectedTextSize);
        unselectedTextSize = typedArray.getDimensionPixelSize(R.styleable.PickerView_unselected_text_size, defaultUnselectedTextSize);
        selectedTextColor = typedArray.getColor(R.styleable.PickerView_selected_text_color, Color.BLACK);
        unselectedTextColor = typedArray.getColor(R.styleable.PickerView_unselected_text_color, Color.DKGRAY);
        selectedTextAlpha = typedArray.getFloat(R.styleable.PickerView_selected_text_alpha, 1f);
        unselectedTextAlpha = typedArray.getFloat(R.styleable.PickerView_unselected_text_alpha, 0.5f);
        textPadding = typedArray.getDimensionPixelSize(R.styleable.PickerView_text_padding, 100);
        otherTextNumber = typedArray.getInteger(R.styleable.PickerView_otherTextNumber, 1);
        Log.d(TAG, "init: textPadding:" + textPadding);
        Log.d(TAG, "init: selectedTextSize:" + selectedTextSize);
        Log.d(TAG, "init: unselectedTextSize:" + unselectedTextSize);
        Log.d(TAG, "init: selectedTextColor:" + selectedTextColor);
        Log.d(TAG, "init: unselectedTextColor:" + unselectedTextColor);
        isDataRecycled = typedArray.getBoolean(R.styleable.PickerView_recycle_data, true);
        speed = typedArray.getFloat(R.styleable.PickerView_speed, 2f);
        distance = textPadding + (selectedTextSize + unselectedTextSize) / 2f;
        // 设置抗锯齿，不设置也没关系
        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        // 先默认为黑色
        paint.setColor(selectedTextColor);
        // 设置样式为填充
        paint.setStyle(Paint.Style.FILL);
        // 设置x轴居中绘制
        paint.setTextAlign(Paint.Align.CENTER);
        // 默认的适配器为Object类型
        adapter = new Adapter() {
            @Override
            public String getText(Object data, int position) {
                return data.toString();
            }

            @Override
            public void onSelect(Object data, int position) {

            }
        };
        typedArray.recycle();
    }


    /**
     * 选择事件
     */
    public void performSelect() {
        if (adapter != null) {
            adapter.onSelect(adapter.getData(adapter.selectedIndex), adapter.selectedIndex);
        }
    }


    /**
     * 处理回弹时候的动作
     */
    protected void updateView() {
        Log.d(TAG, "updateView: start");
        if (Math.abs(moveLength) < speed) {
            Log.d(TAG, "updateView: moveLenIf" + moveLength);
            moveLength = 0;
            if (updateViewTask != null) {
                Log.d(TAG, "handleMessage: 线程关闭");
                updateViewTask.isStop = true;
                performSelect();
            }
        } else {
            Log.d(TAG, "updateView: moveLenBefore:" + moveLength);
            // 如果移动的距离为负，代表指针向上移动，也就是执行向上刷的动作，内容向下移动
            // 此时回弹需要speed为正，绘制的text是从上方回弹到中间点，这个状态实际上是MOVE_DOWN的状态一样
            // 距离为正则相反
            if (moveLength < 0) {
                moveLength += speed;
                direction = MOVE_DOWN;
            } else {
                moveLength -= speed;
                direction = MOVE_UP;
            }
            Log.d(TAG, "updateView: direction:" + direction);
            Log.d(TAG, "updateView: moveLenAfter:" + moveLength);
        }
        invalidate();
    }


    /**
     * 获取最大的data的长度，当重新设置data以及监听器的时候需要触发
     * 因为每一个data显示的文本是根据监听器的getText来获得
     */
    public void measureMaxTextWidth() {
        float maxWidth = 0;
        for (int i = 0; i < adapter.dataList.size(); i++) {
            String text = adapter.getText(adapter.getData(i), i);
            float textWidth = paint.measureText(text);
            Log.d(TAG, "setData: " + text + "  width:" + textWidth);
            if (textWidth > maxWidth) {
                maxWidth = textWidth;
            }
        }
        maxTextWidth = maxWidth;
    }


    /**
     * 获取下一个data
     */
    private void moveSelectedIndexDown() {
        int size = adapter.dataList.size();
        int selectedIndex = adapter.selectedIndex;
        if (isDataRecycled) {
            if (selectedIndex == size - 1) {
                adapter.selectedIndex = 0;
            } else {
                adapter.selectedIndex++;
            }
        } else {
            if (selectedIndex != size - 1) {
                adapter.selectedIndex++;
            }
        }
    }

    /**
     * 获取前一个data
     */
    private void moveSelectedIndexUp() {
        int size = adapter.dataList.size();
        int selectedIndex = adapter.selectedIndex;
        if (isDataRecycled) {
            if (selectedIndex == 0) {
                adapter.selectedIndex = size - 1;
            } else {
                adapter.selectedIndex--;
            }
        } else {
            if (selectedIndex != 0) {
                adapter.selectedIndex--;
            }
        }
    }

    /**
     * 通过输入index，来获取到真实要显示的index
     * 比如传入index = 5，size = 5，如果数据循环，那么返回的index就是0，如果数据不循环，返回的下标就是4
     *
     * @param index 要进行判断的下标
     * @return 真实要显示的下标
     */
    private int getRealSelectedIndex(int index) {
        int size = adapter.dataList.size();
        if (isDataRecycled) {
            if (index >= size) {
                index = index % size;
            } else if (index < 0) {
                index = size + index;
            }
        } else {
            if (index >= size) {
                index = -1;
            } else if (index < 0) {
                index = -1;
            }
        }
        return index;
    }


    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        this.widthMeasureSpec = widthMeasureSpec;
        this.heightMeasureSpec = heightMeasureSpec;
        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        Log.d(TAG, "onMeasure: widthSize:" + widthSize);

        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);
        Log.d(TAG, "onMeasure: heightSize:" + heightSize);
        // 真正的宽高
        int width = 0;
        int height = 0;
        if (widthMode == MeasureSpec.EXACTLY) {
            width = widthSize;
        } else {
            // else这个地方说明对应的是AT_MOST, UNSPECIFIED很少使用，这个自定义view也不使用
            // 宽度是padding + 最大的文字宽度 + 1，这里+1是因为最大文字宽度从float转int类型，大概率会被除去小数，所以补1来保证实际宽度 >= 真实宽度
            // 且误差在1px以内，忽略不计
            width = width + getPaddingStart() + getPaddingEnd() + (int) maxTextWidth + 1;
            // WRAP_CONTENT要找到更小的size
            width = Math.min(width, widthSize);
        }
        if (heightMode == MeasureSpec.EXACTLY) {
            height = heightSize;
        } else {
            // 高度是 padding + 选中的文字尺寸 + (未选中的文字尺寸 + 文字padding) * 一边有多少未选中的文字 * 2
            height = height + getPaddingTop() + getPaddingBottom() + selectedTextSize + (unselectedTextSize + textPadding) * otherTextNumber * 2;
            height = Math.min(height, heightSize);
        }
        setMeasuredDimension(width, height);
        viewHeight = height;
        viewWidth = width;
        invalidate();
    }


    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        // 根据index绘制view, 如果selectedIndex 为 -1说明没有数据
        if (adapter.selectedIndex != -1) {
            drawData(canvas);
        }
    }

    /**
     * 绘制中间的data，然后绘制上一个和下一个data
     *
     * @param canvas 绘制的画布
     */
    private void drawData(Canvas canvas) {
        // text居中绘制，要做到居中绘制必须要算出来baseLine，也就是要将字体的尺寸纳入计算, y应该是中线的 坐标
        float x = (float) (viewWidth / 2.0);
        float y = (float) (viewHeight / 2.0 + moveLength);
        // 根据距离获得缩放比例, scale为0就是不需要缩放，为1是缩放一倍,也就是到了最小值
        scale = Math.abs(moveLength) / distance * 2;
        //判断本次scale是放大还是缩小,scale越小说明越大（不需要缩放）
        if (scale <= lastScale) {
            isEnlarging = true;
        } else {
            isEnlarging = false;
        }
        //存储当前的scale状态
        lastScale = scale;
        // 根据比例获得当前应该draw出来的size和alpha
        float size = (selectedTextSize - unselectedTextSize) * (1 - scale) + unselectedTextSize;
        // 设置size和alpha
        paint.setTextSize(size);
        paint.setColor(selectedTextColor);
        paint.setAlpha((int) (((selectedTextAlpha - unselectedTextAlpha) * (1 - scale) + unselectedTextAlpha) * 255));
        Paint.FontMetricsInt fmi = paint.getFontMetricsInt();
        //baseLine 实际上是文字的左下角的Y坐标
        float baseLine = (float) (y - (fmi.bottom + fmi.top) / 2.0);
        //drawText的y参数是文字左下角坐标, 所以前面必须计算出来文字中线的坐标
        selectedText = adapter.getText(adapter.getSelectedData(), adapter.selectedIndex);
        int startX = (int) (x - maxTextWidth / 2);
        int endX = (int) (x + maxTextWidth / 2);
        int endY = (int) (y + size / 2.0);
        int startY = (int) (endY - size);
        //颜色的scale需要两倍，要做到走了一半的时候颜色就都已经褪掉了
        int changedY = (int) ((1 - scale) * size);
        //向下滑，此时是向上绘制，所以渐变层是底部向上渐变
        if (direction == MOVE_DOWN) {
            Log.d(TAG, "drawData: 正在向下滑");
            //如果向下滑的时候，目前的状态是正在扩大，那么渐变层是从底部开始, 这个状态说明中间的text正在变大，该text要成为中间的数据来展示
            if (isEnlarging) {
                drawGradientTextFromBottom(canvas, startX, startY, endX, endY, changedY, x, baseLine);
            } else {
                //如果向下滑的时候，状态是缩放，那么渐变层从顶部开始，这个状态说明是中间的text开始变小，准备切换成上一个text
                drawGradientTextFromTop(canvas, startX, startY, endX, endY, changedY, x, baseLine);
            }
        } else if (direction == MOVE_UP) {
            Log.d(TAG, "drawData: 正在向上滑");
            //如果向上滑的时候，目前状态正在扩大，那么渐变层从顶部开始，这个状态说明中间的text开始变大，该text要成为中间的数据来展示
            if (isEnlarging) {
                drawGradientTextFromTop(canvas, startX, startY, endX, endY, changedY, x, baseLine);
            } else {
                //如果向上滑的时候，目前的状态是缩放，那么渐变层从底部开始，这个状态说明中间的text开始变小，准备切换成下一个text
                drawGradientTextFromBottom(canvas, startX, startY, endX, endY, changedY, x, baseLine);
            }

        }
        // 绘制上下的text
        for (int i = 1; i <= otherTextNumber; i++) {
            drawOtherData(canvas, i, MOVE_DOWN, y);
            drawOtherData(canvas, i, MOVE_UP, y);
        }
        Log.d(TAG, "drawData: 结束");

    }


    /**
     * 绘制出上下其他的Text
     *
     * @param canvas
     * @param count   绘制上、下的第count个text
     * @param type    用来判断是绘制上方还是下方的数据，MOVE_DOWN是绘制上方的数据
     * @param centerY 选中的text的中点Y坐标，以该坐标为基准算出来其他text的中点Y坐标
     */
    private void drawOtherData(Canvas canvas, int count, int type, float centerY) {
        int position = getRealSelectedIndex(adapter.selectedIndex + type * count);
        if (position == -1) {
            return;
        }
        Paint.FontMetricsInt fmi = paint.getFontMetricsInt();
        Log.d(TAG, "drawOtherData: count:" + count);
        Log.d(TAG, "drawOtherData: scale:" + scale);
        paint.setTextSize(unselectedTextSize);
        paint.setColor(unselectedTextColor);
        paint.setAlpha((int) (unselectedTextAlpha * 255));
        float x = viewWidth / 2f;
        float y = centerY + type * distance * count;
        float baseLine = y - (fmi.bottom + fmi.top) / 2f;
        Log.d(TAG, "drawOtherText: baseline" + baseLine);
        Log.d(TAG, "drawOtherText: data:" + adapter.getData(position));
        String otherText = adapter.getText(adapter.getData(position), position);
        canvas.drawText(otherText, x, baseLine, paint);
    }


    /**
     * 从文字的上方开始，绘制渐变文字
     *
     * @param canvas   画布
     * @param startX   文字的左侧x坐标
     * @param startY   文字的顶部y坐标
     * @param endX     文字的右侧x坐标
     * @param endY     文字的底部y坐标
     * @param changedY 根据缩放的比例，获得的渐变色绘制的高度
     * @param x        文字绘制的x坐标,因为是居中绘制，所以x是中点横坐标
     * @param baseLine 文字绘制的baseLine
     */
    private void drawGradientTextFromTop(Canvas canvas, int startX, int startY, int endX, int endY, int changedY, float x, float baseLine) {
        //绘制渐变层
        canvas.save();
        paint.setColor(selectedTextColor);
        canvas.clipRect(new Rect(startX, startY, endX, startY + changedY));
        canvas.drawText(selectedText, x, baseLine, paint);
        canvas.restore();
        //绘制底色层
        canvas.save();
        paint.setColor(unselectedTextColor);
        canvas.clipRect(new Rect(startX, startY + changedY, endX, endY));
        canvas.drawText(selectedText, x, baseLine, paint);
        canvas.restore();
    }

    /**
     * 从文字的下方开始，绘制渐变文字
     *
     * @param canvas   画布
     * @param startX   文字的左侧x坐标
     * @param startY   文字的顶部y坐标
     * @param endX     文字的右侧x坐标
     * @param endY     文字的底部y坐标
     * @param changedY 根据缩放的比例，获得的渐变色绘制的高度
     * @param x        文字绘制的x坐标,因为是居中绘制，所以x是中点横坐标
     * @param baseLine 文字绘制的baseLine
     */
    private void drawGradientTextFromBottom(Canvas canvas, int startX, int startY, int endX, int endY, int changedY, float x, float baseLine) {
        //绘制渐变层
        canvas.save();
        paint.setColor(selectedTextColor);
        canvas.clipRect(new Rect(startX, endY - changedY, endX, endY));
        canvas.drawText(selectedText, x, baseLine, paint);
        canvas.restore();
        //绘制底色层
        canvas.save();
        paint.setColor(unselectedTextColor);
        canvas.clipRect(new Rect(startX, startY, endX, endY - changedY));
        canvas.drawText(selectedText, x, baseLine, paint);
        canvas.restore();
    }


    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                onActionDown(event);
                break;
            case MotionEvent.ACTION_MOVE:
                onActionMove(event);
                break;
            case MotionEvent.ACTION_UP:
                onActionUp(event);
                break;
            default:
        }
        return true;
    }

    /**
     * 手指刚按下的时候，触发该事件
     * 如果有回弹的任务，那么将该回弹的任务中止
     * 记录下来这个时候手指点击的位置的y坐标，用来计算滑动的距离
     *
     * @param event
     */
    private void onActionDown(MotionEvent event) {
        if (updateViewTask != null) {
            // 为了防止在回弹的时候继续触发移动事件而导致onDraw同时被调用，在action事件发生时都让原本的更新任务停止
            updateViewTask.isStop = true;
        }
        lastFingerTouchY = event.getY();
        Log.d(TAG, "doDown: mLastDownY:" + lastFingerTouchY);
    }

    /**
     * 处理手指移动事件
     * 根据每次滑动的距离来计算出总的滑动距离
     * 如果每次滑动的距离segmentMoveLength > 0, 说明是向下移动，反之则向上移动
     * 如果设置为不允许数据循环，那么当移动到末尾或者开头的时候，直接结束移动事件。
     * 当移动距离超过两个text之间距离的一半时，这个时候说明发生了选中数据的交替，修改下标
     * 同时移动距离减去两个text之间的距离（原text为a，即将被选中的text为b，当a移动超过ab距离一半的时候，
     * 此时选中的数据从a变为b，对b而言，现在要进行的移动方向和a是相反的，且要移动的距离和a原本的距离相加就是二者的距离）
     * @param event
     */
    private void onActionMove(MotionEvent event) {

        Log.d(TAG, "onActionMove: mMoveLenBefore:" + moveLength);
        //获得本次滑动的距离，>0是向下滑，<0是向上滑
        float segmentMoveLength = event.getY() - lastFingerTouchY;
        if (segmentMoveLength > 0) {
            direction = MOVE_DOWN;
            if (!isDataRecycled && adapter.selectedIndex == 0) {
                return;
            }
        } else {
            direction = MOVE_UP;
            if (!isDataRecycled && adapter.selectedIndex == adapter.getDataSize() - 1) {
                return;
            }
        }
        moveLength += segmentMoveLength;
        Log.d(TAG, "onActionMove: mMoveLenNow:" + moveLength);
        Log.d(TAG, "onActionMove: distance:" + distance / 2f);
        if (moveLength > distance / 2.0) {
            // 手势往下滑动超过距离的一半, 也就是获取上一个data
            moveSelectedIndexUp();
            moveLength = moveLength - distance;
        } else if (moveLength < -distance / 2.0) {
            // 手势往上滑动超过距离的一半, 也就是获取下一个data
            moveSelectedIndexDown();
            moveLength = moveLength + distance;
        }

        lastFingerTouchY = event.getY();
        Log.d(TAG, "doMove: lastFingerDownY" + lastFingerTouchY);
        invalidate();
    }

    /**
     * 当手指放开的时候处理的事件
     * 新建更新任务，将更新任务加入线程池中，同时开始运行，每10ms执行一次回弹效果
     * 如果任务已存在，那么运行任务即可
     * @param event
     */
    private void onActionUp(MotionEvent event) {
        Log.d(TAG, "onActionUp: moveLen" + moveLength);
        // 抬起手后，开始进行回弹，回弹到选中的index的位置
        if (updateViewTask != null) {
            // 为了防止在回弹的时候继续触发移动事件而导致onDraw同时被调用，在action事件发生时都让原本的更新任务停止
            updateViewTask.isStop = true;
        }

        //如果更新任务为null，那么创建一个更新任务并执行
        if (updateViewTask == null) {
            updateViewTask = new UpdateViewTask(new UpdateViewHandler(Looper.myLooper(), this));
            scheduledExecutorService.scheduleWithFixedDelay(updateViewTask, 0, 10, TimeUnit.MILLISECONDS);
            Log.d(TAG, "onActionUp: 执行mTask");
        } else {
            // 如果更新任务已经存在，那么修改状态变量为false使其继续运行
            updateViewTask.isStop = false;
        }

    }

    static class UpdateViewTask implements Runnable {
        Handler handler;
        boolean isStop = false;

        public UpdateViewTask(Handler handler) {
            this.handler = handler;
        }

        @Override
        public void run() {
            if (!isStop) {
                handler.sendMessage(handler.obtainMessage());
            }
        }

    }

    static class UpdateViewHandler extends Handler {
        WeakReference<PickerView> pickerViewWeakReference;

        public UpdateViewHandler(@NonNull Looper looper, PickerView pickerView) {
            super(looper);
            this.pickerViewWeakReference = new WeakReference<>(pickerView);
        }

        @Override
        public void handleMessage(@NonNull Message msg) {
            if (pickerViewWeakReference != null) {
                pickerViewWeakReference.get().updateView();
            }
        }
    }


    public void setSelectedTextSize(int selectedTextSize) {
        this.selectedTextSize = selectedTextSize;
        distance = textPadding + (selectedTextSize + unselectedTextSize) / 2f;
    }

    public void setUnselectedTextSize(int unselectedTextSize) {
        this.unselectedTextSize = unselectedTextSize;
        distance = textPadding + (selectedTextSize + unselectedTextSize) / 2f;
    }

    public void setTextPadding(int textPadding) {
        this.textPadding = textPadding;
        distance = textPadding + (selectedTextSize + unselectedTextSize) / 2f;
    }

    public void setSpeed(float speed) {
        this.speed = speed;
    }

    public void setSelectedTextAlpha(float selectedTextAlpha) {
        this.selectedTextAlpha = selectedTextAlpha;
    }

    public void setUnselectedTextAlpha(float unselectedTextAlpha) {
        this.unselectedTextAlpha = unselectedTextAlpha;
    }

    public void setUnselectedTextColor(int unselectedTextColor) {
        this.unselectedTextColor = unselectedTextColor;
    }

    public void setDataRecycled(boolean isDataRecycled) {
        this.isDataRecycled = isDataRecycled;
    }


    public void setSelectedTextColor(int colorResourceId) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            selectedTextColor = getResources().getColor(colorResourceId, null);
        } else {
            selectedTextColor = getResources().getColor(colorResourceId);
        }
    }

    public void setSelectedTextColorInt(int color) {
        selectedTextColor = color;
    }

    public void setUnselectedTextColorInt(int color) {
        unselectedTextColor = color;
    }

    public void setUnSelectedTextColor(int colorResourceId) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            unselectedTextColor = getResources().getColor(colorResourceId, null);
        } else {
            unselectedTextColor = getResources().getColor(colorResourceId);
        }
    }

    /**
     * 返回选中的数据
     * 该方法请在设置完非空数据后使用
     *
     * @return 选中的数据 或者为 NULL（如果数据不存在或者数据为空的话）
     */
    public Object getSelectedData() {
        if (adapter.selectedIndex == -1) {
            return null;
        }
        return adapter.getSelectedData();
    }

    public String getSelectedText() {
        return selectedText;
    }

    /**
     * 获得选中的index
     *
     * @return 选中的index
     */
    public int getSelectedIndex() {
        return adapter.selectedIndex;
    }

    public abstract static class Adapter<E> {
        private List<E> dataList;
        private int selectedIndex;

        public abstract String getText(E data, int position);

        public abstract void onSelect(E data, int position);

        public E getData(int position) {
            return dataList.get(position);
        }

        public Adapter() {
            dataList = new ArrayList<>();
            selectedIndex = -1;
        }

        public Adapter(@NotNull List<E> dataList) {
            this.dataList = dataList;
            if (dataList.isEmpty()) {
                selectedIndex = -1;
            } else {
                selectedIndex = 0;
            }

        }

        public Adapter(@NotNull List<E> dataList, int selectedIndex) {
            this.dataList = dataList;
            if (selectedIndex < 0) {
                this.selectedIndex = -1;
            } else {
                this.selectedIndex = selectedIndex;
            }

        }

        public void setDataList(@NotNull List<E> dataList) {
            this.dataList = dataList;
            if (dataList.isEmpty()) {
                selectedIndex = -1;
            } else {
                selectedIndex = 0;
            }
        }

        public int getDataSize() {
            return dataList.size();
        }

        public List<E> getDataList() {
            return dataList;
        }

        public E getSelectedData() {
            return dataList.get(selectedIndex);
        }

        public int getSelectedIndex() {
            return selectedIndex;
        }

        public void setSelectedIndex(int selectedIndex) {
            this.selectedIndex = selectedIndex;
        }

        public void notifyDataChanged() {

        }
    }

    public Adapter getAdapter() {
        return adapter;
    }

    /**
     * 设置适配器
     * @param adapter
     */
    public void setAdapter(@NotNull Adapter adapter) {
        this.adapter = adapter;
        paint.setTextSize(selectedTextSize);
        measureMaxTextWidth();
        int width = getPaddingStart() + getPaddingEnd() + (int) maxTextWidth + 1;
        widthMeasureSpec = MeasureSpec.makeMeasureSpec(width, MeasureSpec.getMode(widthMeasureSpec));
        //measure(widthMeasureSpec, heightMeasureSpec);
        invalidate();
    }

    public void reMeasure() {
        paint.setTextSize(selectedTextSize);
        measureMaxTextWidth();
        int width = getPaddingStart() + getPaddingEnd() + (int) maxTextWidth + 1;
        widthMeasureSpec = MeasureSpec.makeMeasureSpec(width, MeasureSpec.getMode(widthMeasureSpec));
        //super.measure(MeasureSpec.makeMeasureSpec(width, MeasureSpec.getMode(widthMeasureSpec)), heightMeasureSpec);
        Log.d(TAG, "reMeasure: 重绘制");
        invalidate();
    }

}


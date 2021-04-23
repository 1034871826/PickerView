package suyuan.pickerview;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;


/**
 * @author suyuan
 */
public class DatePicker extends ConstraintLayout {
    private static final String TAG = "DatePicker";
    private PickerView yearPicker;
    private PickerView monthPicker;
    private PickerView dayPicker;
    private PickerView hourPicker;
    private PickerView minutePicker;
    private PickerView.Adapter<Integer> dayAdapter;

    private boolean isShowDate;
    private boolean isShowTime;

    private int startYear = 1970;
    private int endYear = 2050;

    private int selectedYear = 1970;
    private int selectedMonth = 1;

    private int marginInner = 30;

    private int selectedTextSize = 0;
    private int unselectedTextSize = 0;
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

    private float selectedTextAlpha = 1f;
    private float unselectedTextAlpha = 0.5f;

    private int selectedTextColor;
    private int unselectedTextColor;

    public DatePicker(@NonNull Context context) {
        this(context, null);
    }

    public DatePicker(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public DatePicker(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs, defStyleAttr);
    }

    private void init(Context context, AttributeSet attrs, int defStyleAttr) {
        LayoutInflater.from(context).inflate(R.layout.layout_date_picker, this, true);
        yearPicker = findViewById(R.id.year);
        monthPicker = findViewById(R.id.month);
        dayPicker = findViewById(R.id.day);
        hourPicker = findViewById(R.id.hour);
        minutePicker = findViewById(R.id.minute);
        getAttribute(context, attrs, defStyleAttr);
        if (isShowDate) {
            setPickerAttribute(yearPicker);
            setPickerAttribute(monthPicker);
            setPickerAttribute(dayPicker);
        } else {
            yearPicker.setVisibility(GONE);
            monthPicker.setVisibility(GONE);
            dayPicker.setVisibility(GONE);
        }
        if (isShowTime) {
            setPickerAttribute(hourPicker);
            setPickerAttribute(minutePicker);
        } else {
            hourPicker.setVisibility(GONE);
            minutePicker.setVisibility(GONE);
        }


        LayoutParams monthLayoutParams = (LayoutParams) monthPicker.getLayoutParams();
        monthLayoutParams.setMarginStart(marginInner);
        LayoutParams dayLayoutParams = (LayoutParams) dayPicker.getLayoutParams();
        dayLayoutParams.setMarginStart(marginInner);
        LayoutParams hourLayoutParams = (LayoutParams) hourPicker.getLayoutParams();
        hourLayoutParams.setMarginStart(marginInner);
        LayoutParams minuteLayoutParams = (LayoutParams) minutePicker.getLayoutParams();
        minuteLayoutParams.setMarginStart(marginInner);
        setDateData();
        setTimeData();
    }

    private void setPickerAttribute(PickerView pickerView) {
        pickerView.setDataRecycled(isDataRecycled);
        pickerView.setSelectedTextSize(selectedTextSize);
        pickerView.setUnselectedTextSize(unselectedTextSize);
        pickerView.setSelectedTextAlpha(selectedTextAlpha);
        pickerView.setUnselectedTextAlpha(unselectedTextAlpha);
        pickerView.setSelectedTextColorInt(selectedTextColor);
        pickerView.setUnselectedTextColorInt(unselectedTextColor);
        pickerView.setTextPadding(textPadding);
        pickerView.setSpeed(speed);
    }

    private void getAttribute(Context context, AttributeSet attrs, int defStyleAttr) {
        TypedArray typedArray = context.getTheme().obtainStyledAttributes(attrs, R.styleable.DatePicker, defStyleAttr, 0);
        //以下是DatePicker的属性
        startYear = typedArray.getInteger(R.styleable.DatePicker_start_year, 1970);
        endYear = typedArray.getInteger(R.styleable.DatePicker_end_year, 2050);
        selectedYear = typedArray.getInteger(R.styleable.DatePicker_selected_year, 1970);
        selectedMonth = typedArray.getInteger(R.styleable.DatePicker_selected_month, 1);
        marginInner = typedArray.getDimensionPixelSize(R.styleable.DatePicker_margin_inner, 30);
        isShowDate = typedArray.getBoolean(R.styleable.DatePicker_showDate, true);
        isShowTime = typedArray.getBoolean(R.styleable.DatePicker_showTime, true);
        //以下是给每个PickerView配置的属性
        int defaultSelectedTextSize = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 16f, context.getResources().getDisplayMetrics());
        int defaultUnselectedTextSize = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 12f, context.getResources().getDisplayMetrics());

        selectedTextSize = typedArray.getDimensionPixelSize(R.styleable.DatePicker_selected_text_size, defaultSelectedTextSize);
        unselectedTextSize = typedArray.getDimensionPixelSize(R.styleable.DatePicker_unselected_text_size, defaultUnselectedTextSize);
        selectedTextColor = typedArray.getColor(R.styleable.DatePicker_selected_text_color, Color.BLACK);
        unselectedTextColor = typedArray.getColor(R.styleable.DatePicker_unselected_text_color, Color.BLACK);
        selectedTextAlpha = typedArray.getFloat(R.styleable.DatePicker_selected_text_alpha, 1f);
        unselectedTextAlpha = typedArray.getFloat(R.styleable.DatePicker_unselected_text_alpha, 0.5f);
        textPadding = typedArray.getDimensionPixelSize(R.styleable.DatePicker_text_padding, 100);
        isDataRecycled = typedArray.getBoolean(R.styleable.DatePicker_recycle_data, true);
        speed = typedArray.getFloat(R.styleable.DatePicker_speed, 2f);
        typedArray.recycle();

    }

    private void setDateData() {
        List<Integer> yearList = new ArrayList<>();
        List<Integer> monthList = new ArrayList<>();
        List<Integer> dayList = new ArrayList<>();
        //添加年份数据
        for (int i = startYear; i <= endYear; i++) {
            yearList.add(i);
        }
        int selectedIndex = yearList.indexOf(selectedYear);
        yearPicker.setAdapter(new PickerView.Adapter<Integer>(yearList, selectedIndex) {

            @Override
            public String getText(Integer data, int position) {
                return data.toString();
            }

            //每次选中的时候，记录下来当前选中的year和month且同时修改dayPicker的数据
            @Override
            public void onSelect(Integer data, int position) {
                selectedYear = data;
                updateDayPicker();
            }
        });
        //添加月份数据
        for (int i = 1; i <= 12; i++) {
            monthList.add(i);
        }
        monthPicker.setAdapter(new PickerView.Adapter<Integer>(monthList) {

            @Override
            public String getText(Integer data, int position) {
                return data.toString();
            }

            @Override
            public void onSelect(Integer data, int position) {
                selectedMonth = data;
                updateDayPicker();
            }
        });
        //添加1月的数据，因为默认显示的是1月
        for (int i = 1; i <= 31; i++) {
            dayList.add(i);
        }
        dayAdapter = new PickerView.Adapter<Integer>(dayList) {
            @Override
            public String getText(Integer data, int position) {
                return data.toString();
            }

            @Override
            public void onSelect(Integer data, int position) {

            }
        };
        dayPicker.setAdapter(dayAdapter);


    }

    private void setTimeData() {
        List<String> hourList = new ArrayList<>();
        List<String> minuteList = new ArrayList<>();
        for (int i = 0; i < 24; i++) {
            if (i < 10) {
                hourList.add("0" + i);
            } else {
                hourList.add(i + "");
            }

        }
        hourPicker.setAdapter(new PickerView.Adapter<String>(hourList) {
            @Override
            public String getText(String data, int position) {
                return data;
            }

            @Override
            public void onSelect(String data, int position) {

            }
        });
        for (int i = 0; i < 60; i++) {
            if (i < 10) {
                minuteList.add("0" + i);
            } else {
                minuteList.add(i + "");
            }
        }
        minutePicker.setAdapter(new PickerView.Adapter<String>(minuteList) {
            @Override
            public String getText(String data, int position) {
                return data;
            }

            @Override
            public void onSelect(String data, int position) {

            }
        });
    }

    /**
     * 更新dayPicker的数据
     */
    private void updateDayPicker() {
        Log.d(TAG, "updateDayPicker: 更新视图");
        int startDay = 1;
        int endDay;
        switch (selectedMonth) {
            case 1:
            case 3:
            case 5:
            case 7:
            case 8:
            case 10:
            case 12:
                endDay = 31;
                break;
            case 2:
                endDay = 28;
                break;
            default:
                endDay = 30;

        }
        if (selectedMonth == 2) {
            //判断闰年, 闰年2月为29
            if (selectedYear % 100 == 0 && selectedYear % 400 == 0 || selectedYear % 4 == 0 && selectedYear % 100 != 0) {
                endDay = 29;
            }
        }
        List<Integer> dayList = new ArrayList<>();
        for (int i = startDay; i <= endDay; i++) {
            dayList.add(i);
        }
        dayAdapter.setDataList(dayList);
        dayPicker.reMeasure();
//        dayPicker.setAdapter(new PickerView.Adapter<String>(dayList) {
//
//            @Override
//            public String getText(String data, int position) {
//                return data;
//            }
//
//            @Override
//            public void onSelect(String data, int position) {
//
//            }
//        });
    }

    public int getYear() {
        return (int) yearPicker.getSelectedData();
    }

    public int getMonth() {
        return (int) monthPicker.getSelectedData();
    }

    public int getDay() {
        return (int) dayPicker.getSelectedData();
    }

    public int getHour() {
        return Integer.parseInt((String) hourPicker.getSelectedData());
    }

    public int getMinute() {
        return Integer.parseInt((String) minutePicker.getSelectedData());
    }

    public Timestamp getDate() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(getYear(), getMonth(), getDay());
        return new Timestamp(calendar.getTime().getTime());
    }

    public Timestamp getDateTime() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(getYear(), getMonth(), getDay(), getHour(), getMinute());
        return new Timestamp(calendar.getTime().getTime());
    }

    public String getDateTimeString() {
        return getDateString() + " " + getHour() + ":" + getMinute();
    }

    public String getDateString() {
        return getYear() + "-" + getMonth() + "-" + getDay();
    }
}

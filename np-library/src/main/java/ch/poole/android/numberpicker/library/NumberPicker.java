package ch.poole.android.numberpicker.library;

import android.content.Context;
import android.content.res.TypedArray;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import ch.poole.android.numberpicker.library.Enums.ActionEnum;
import ch.poole.android.numberpicker.library.Interface.LimitExceededListener;
import ch.poole.android.numberpicker.library.Interface.ValueChangedListener;
import ch.poole.android.numberpicker.library.Listener.ActionListener;
import ch.poole.android.numberpicker.library.Listener.DefaultLimitExceededListener;
import ch.poole.android.numberpicker.library.Listener.DefaultOnEditorActionListener;
import ch.poole.android.numberpicker.library.Listener.DefaultOnFocusChangeListener;
import ch.poole.android.numberpicker.library.Listener.DefaultValueChangedListener;

/**
 * Created by travijuu on 26/05/16.
 */
public class NumberPicker extends LinearLayout {

    // default values
    private final int     DEFAULT_MIN       = 0;
    private final int     DEFAULT_MAX       = 999999;
    private final int     DEFAULT_VALUE     = 1;
    private final int     DEFAULT_UNIT      = 1;
    private final int     DEFAULT_LAYOUT    = R.layout.number_picker_layout;
    private final boolean DEFAULT_FOCUSABLE = false;
    private final int     DEFAULT_REPEAT    = 200;

    // required variables
    private int     minValue;
    private int     maxValue;
    private int     unit;
    private int     currentValue;
    private int     layout;
    private boolean focusable;
    private int     repeat;
    private boolean longPressInProgress;

    // ui components
    private Context  mContext;
    private Button   decrementButton;
    private Button   incrementButton;
    private EditText displayEditText;

    // listeners
    private LimitExceededListener limitExceededListener;
    private ValueChangedListener  valueChangedListener;
    private TextWatcher           textWatcher;

    public NumberPicker(Context context) {
        super(context, null);
    }

    public NumberPicker(Context context, AttributeSet attrs) {
        super(context, attrs);

        this.initialize(context, attrs);
    }

    public NumberPicker(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    private void initialize(Context context, AttributeSet attrs) {
        TypedArray attributes = context.getTheme().obtainStyledAttributes(attrs, R.styleable.NumberPicker, 0, 0);

        // set required variables with values of xml layout attributes or default ones
        this.minValue = attributes.getInteger(R.styleable.NumberPicker_min, this.DEFAULT_MIN);
        this.maxValue = attributes.getInteger(R.styleable.NumberPicker_max, this.DEFAULT_MAX);
        this.currentValue = attributes.getInteger(R.styleable.NumberPicker_value, this.DEFAULT_VALUE);
        this.unit = attributes.getInteger(R.styleable.NumberPicker_unit, this.DEFAULT_UNIT);
        this.layout = attributes.getResourceId(R.styleable.NumberPicker_custom_layout, this.DEFAULT_LAYOUT);
        this.focusable = attributes.getBoolean(R.styleable.NumberPicker_focusable, this.DEFAULT_FOCUSABLE);
        this.repeat = attributes.getInteger(R.styleable.NumberPicker_repeat, this.DEFAULT_REPEAT);
        this.mContext = context;

        // if current value is greater than the max. value, decrement it to the max. value
        this.currentValue = this.currentValue > this.maxValue ? maxValue : currentValue;

        // if current value is less than the min. value, decrement it to the min. value
        this.currentValue = this.currentValue < this.minValue ? minValue : currentValue;

        // set layout view
        LayoutInflater.from(this.mContext).inflate(layout, this, true);

        // init ui components
        this.decrementButton = (Button) findViewById(R.id.decrement);
        this.incrementButton = (Button) findViewById(R.id.increment);
        this.displayEditText = (EditText) findViewById(R.id.display);

        // register button click and action listeners

        final ActionListener incrementListener = new ActionListener(this, this.displayEditText, ActionEnum.INCREMENT);
        this.incrementButton.setOnClickListener(incrementListener);
        final ActionListener decrementListenerer = new ActionListener(this, this.displayEditText, ActionEnum.DECREMENT);
        this.decrementButton.setOnClickListener(decrementListenerer);

        final Handler mHandler = new Handler();
        final Runnable incrementRunnable = new Runnable() {
            public void run() {
                if (longPressInProgress) {
                    incrementListener.onClick(null);
                    mHandler.postDelayed(this, repeat);
                }
            }
        };
        final Runnable decrementRunnable = new Runnable() {
            public void run() {
                if (longPressInProgress) {
                    decrementListenerer.onClick(null);
                    mHandler.postDelayed(this, repeat);
                }
            }
        };

        OnLongClickListener longClickListener = new OnLongClickListener() {
            /**
             */
            @Override
            public boolean onLongClick(View v) {
                clearFocus();
                longPressInProgress = true;
                if (R.id.increment == v.getId()) {
                    mHandler.post(incrementRunnable);
                } else if (R.id.decrement == v.getId()) {
                    mHandler.post(decrementRunnable);
                }
                return true;
            }
        };
        this.incrementButton.setOnLongClickListener(longClickListener);
        OnTouchListener onTouchListener = new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (MotionEvent.ACTION_BUTTON_RELEASE == event.getActionMasked() || MotionEvent.ACTION_UP == event.getActionMasked()) {
                    longPressInProgress = false;
                }
                return false;
            }
        };
        this.incrementButton.setOnTouchListener(onTouchListener);

        this.decrementButton.setOnLongClickListener(longClickListener);
        this.decrementButton.setOnTouchListener(onTouchListener);

        // init listener for exceeding upper and lower limits
        this.setLimitExceededListener(new DefaultLimitExceededListener());
        // init listener for increment&decrement
        this.setValueChangedListener(new DefaultValueChangedListener());
        // init listener for focus change
        this.setOnFocusChangeListener(new DefaultOnFocusChangeListener(this));
        // init listener for done action in keyboard
        this.setOnEditorActionListener(new DefaultOnEditorActionListener(this));

        // set default display mode
        this.setDisplayFocusable(this.focusable);

        this.textWatcher = new TextWatcher() {
            @Override
            public void afterTextChanged(Editable arg0) {
                int newValue = NumberPicker.this.currentValue;
                try {
                    newValue = Integer.parseInt(NumberPicker.this.displayEditText.getText().toString());
                } catch (NumberFormatException nex) {
                    // do nothing
                }
                if (newValue < NumberPicker.this.minValue) {
                    newValue = NumberPicker.this.minValue;
                } else if (newValue > NumberPicker.this.maxValue) {
                    newValue = NumberPicker.this.maxValue;
                }
                NumberPicker.this.setValue(newValue);
            }

            @Override
            public void beforeTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {
                // Empty
            }

            @Override
            public void onTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {
                // Empty
            }
        };
        this.displayEditText.addTextChangedListener(textWatcher);

        // update ui view
        this.refresh();
    }

    public void refresh() {
        this.displayEditText.removeTextChangedListener(textWatcher);
        this.displayEditText.setText(Integer.toString(this.currentValue));
        this.displayEditText.addTextChangedListener(textWatcher);
    }

    public void clearFocus() {
        this.displayEditText.clearFocus();
    }

    public boolean valueIsAllowed(int value) {
        return (value >= this.minValue && value <= this.maxValue);
    }

    public void setMin(int value) {
        this.minValue = value;
    }

    public void setMax(int value) {
        this.maxValue = value;
    }

    public void setUnit(int unit) {
        this.unit = unit;
    }

    public int getUnit() {
        return this.unit;
    }

    /**
     * Set the interval between increments when the buttons are long clicked
     * 
     * @param repeat interval in milliseconds
     */
    public void setRepeat(int repeat) {
        this.repeat = repeat;
    }

    public int getMin() {
        return this.minValue;
    }

    public int getMax() {
        return this.maxValue;
    }

    public void setValue(int value) {
        if (!this.valueIsAllowed(value)) {
            this.limitExceededListener.limitExceeded(value < this.minValue ? this.minValue : this.maxValue, value);
            return;
        }

        this.currentValue = value;
        this.refresh();
    }

    public int getValue() {
        return this.currentValue;
    }

    public void setLimitExceededListener(LimitExceededListener limitExceededListener) {
        this.limitExceededListener = limitExceededListener;
    }

    public LimitExceededListener getLimitExceededListener() {
        return this.limitExceededListener;
    }

    public void setValueChangedListener(ValueChangedListener valueChangedListener) {
        this.valueChangedListener = valueChangedListener;
    }

    public ValueChangedListener getValueChangedListener() {
        return this.valueChangedListener;
    }

    public void setOnEditorActionListener(TextView.OnEditorActionListener onEditorActionListener) {
        this.displayEditText.setOnEditorActionListener(onEditorActionListener);
    }

    public void setOnFocusChangeListener(OnFocusChangeListener onFocusChangeListener) {
        this.displayEditText.setOnFocusChangeListener(onFocusChangeListener);
    }

    public void setActionEnabled(ActionEnum action, boolean enabled) {
        if (action == ActionEnum.INCREMENT) {
            this.incrementButton.setEnabled(enabled);
        } else if (action == ActionEnum.DECREMENT) {
            this.decrementButton.setEnabled(enabled);
        }
    }

    public void setDisplayFocusable(boolean focusable) {
        this.displayEditText.setFocusable(focusable);

        // required for making EditText focusable
        if (focusable) {
            this.displayEditText.setFocusableInTouchMode(true);
        }
    }

    public void increment() {
        this.changeValueBy(this.unit);
    }

    public void increment(int unit) {
        this.changeValueBy(unit);
    }

    public void decrement() {
        this.changeValueBy(-this.unit);
    }

    public void decrement(int unit) {
        this.changeValueBy(-unit);
    }

    private void changeValueBy(int unit) {
        int oldValue = this.getValue();

        this.setValue(this.currentValue + unit);

        if (oldValue != this.getValue()) {
            this.valueChangedListener.valueChanged(this.getValue(), unit > 0 ? ActionEnum.INCREMENT : ActionEnum.DECREMENT);
        }
    }
}

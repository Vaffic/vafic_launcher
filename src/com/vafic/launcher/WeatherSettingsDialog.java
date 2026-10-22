package com.vafic.launcher;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.text.InputType;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.DecelerateInterpolator;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * Weather section. The master switch turns the whole feature off: no widget
 * in the clock area and no network requests. The short forecast can be hidden
 * separately, and the city is either detected by IP or entered by hand.
 */
public class WeatherSettingsDialog extends VaficDialog {

    public interface Listener {
        void onWeatherChanged();

        void onForecastChanged();

        /** Empty name with NaN coordinates means "detect the place by IP". */
        void onPlacePicked(String name, double lat, double lon);

        void onCityTyped(String name);

        void onRefresh();
    }

    private static final int MATCH = ViewGroup.LayoutParams.MATCH_PARENT;
    private static final int WRAP = ViewGroup.LayoutParams.WRAP_CONTENT;

    private static final String[] PRESET_NAMES = {
            "Москва", "Санкт-Петербург", "Киев", "Минск"
    };
    private static final double[][] PRESET_COORDS = {
            {55.7558, 37.6173},
            {59.9343, 30.3351},
            {50.4501, 30.5234},
            {53.9006, 27.5590}
    };

    private final Activity activity;
    private final float density;
    private final FrameLayout card;
    private final TextView weatherValue;
    private final TextView forecastValue;
    private final TextView cityValue;
    private final View firstRow;
    private boolean weather;
    private boolean forecast;

    public WeatherSettingsDialog(Activity a, final Listener listener) {
        super(a);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        activity = a;
        density = a.getResources().getDisplayMetrics().density;
        weather = Prefs.weather(a);
        forecast = Prefs.weatherForecast(a);

        card = new GlassPanel(a, density, 30);
        card.setClipChildren(false);
        card.setClipToPadding(false);

        LinearLayout box = new LinearLayout(a);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(24), dp(18), dp(24), dp(16));
        box.setClipChildren(false);
        box.setClipToPadding(false);
        card.addView(box, new FrameLayout.LayoutParams(MATCH, WRAP));

        TextView title = new TextView(a);
        title.setText(Txt.s(a, "Погода", "Weather"));
        title.setTextSize(23);
        title.setTextColor(0xFFFFFFFF);
        title.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        box.addView(title, new LinearLayout.LayoutParams(MATCH, WRAP));

        TextView sub = new TextView(a);
        sub.setText(Txt.s(a, "Погода рядом с часами",
                "Weather next to the clock"));
        sub.setTextSize(12);
        sub.setTextColor(0x59FFFFFF);
        sub.setPadding(0, dp(2), 0, dp(12));
        box.addView(sub, new LinearLayout.LayoutParams(MATCH, WRAP));

        LinearLayout weatherRow = row(Txt.s(a, "Показывать погоду", "Show weather"));
        weatherValue = (TextView) weatherRow.getChildAt(1);
        updateToggle(weatherValue, weather);
        weatherRow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                weather = !weather;
                Prefs.setWeather(activity, weather);
                updateToggle(weatherValue, weather);
                listener.onWeatherChanged();
            }
        });
        firstRow = weatherRow;
        box.addView(weatherRow, lp(0));

        LinearLayout forecastRow = row(Txt.s(a, "Краткий прогноз", "Short forecast"));
        forecastValue = (TextView) forecastRow.getChildAt(1);
        updateToggle(forecastValue, forecast);
        forecastRow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                forecast = !forecast;
                Prefs.setWeatherForecast(activity, forecast);
                updateToggle(forecastValue, forecast);
                listener.onForecastChanged();
            }
        });
        box.addView(forecastRow, lp(6));

        LinearLayout cityRow = row(Txt.s(a, "Город", "City"));
        cityValue = (TextView) cityRow.getChildAt(1);
        cityValue.setText(cityText());
        cityRow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showCityMenu(listener);
            }
        });
        box.addView(cityRow, lp(6));

        LinearLayout refreshRow = row(Txt.s(a, "Обновить сейчас", "Refresh now"));
        TextView refreshValue = (TextView) refreshRow.getChildAt(1);
        refreshValue.setText("\u21BB");
        refreshValue.setTextColor(0x8AFFFFFF);
        refreshRow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                refreshValue.setText(Txt.s(activity, "Обновление…", "Refreshing…"));
                listener.onRefresh();
            }
        });
        box.addView(refreshRow, lp(6));

        TextView foot = new TextView(a);
        foot.setText(Txt.s(a, "OK — выбрать      •      BACK — назад",
                "OK — select      •      BACK — back"));
        foot.setTextSize(12);
        foot.setTextColor(0x4DFFFFFF);
        foot.setGravity(Gravity.CENTER);
        foot.setPadding(0, dp(14), 0, 0);
        box.addView(foot, new LinearLayout.LayoutParams(MATCH, WRAP));

        setContentView(card, new ViewGroup.LayoutParams(dp(640), WRAP));

        Window w = getWindow();
        if (w != null) {
            w.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            w.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            WindowManager.LayoutParams wl = w.getAttributes();
            wl.dimAmount = 0.55f;
            wl.width = dp(640);
            wl.height = WRAP;
            wl.gravity = Gravity.CENTER;
            w.setAttributes(wl);
        }
        MainActivity.installHomeKey(this, a);
    }

    private String cityText() {
        if (!Prefs.hasWeatherPlace(activity)) {
            return Txt.s(activity, "Авто (по сети)", "Auto (network)");
        }
        String city = Prefs.weatherCity(activity);
        return city.isEmpty() ? Txt.s(activity, "Авто (по сети)", "Auto (network)") : city;
    }

    private void showCityMenu(final Listener listener) {
        final String[] items = new String[PRESET_NAMES.length + 2];
        items[0] = Txt.s(activity, "Авто (по сети)", "Auto (network)");
        for (int i = 0; i < PRESET_NAMES.length; i++) items[i + 1] = PRESET_NAMES[i];
        items[items.length - 1] = Txt.s(activity, "Другой город…", "Another city…");

        final MenuDialog menu = new MenuDialog(activity, Txt.s(activity, "Город", "City"), items,
                new MenuDialog.Listener() {
                    @Override
                    public void onItem(int index) {
                        if (index == 0) {
                            Prefs.clearWeatherPlace(activity);
                            Prefs.setWeatherCityOnly(activity, "");
                            cityValue.setText(cityText());
                            listener.onPlacePicked("", Double.NaN, Double.NaN);
                        } else if (index <= PRESET_NAMES.length) {
                            int p = index - 1;
                            Prefs.setWeatherPlace(activity, PRESET_NAMES[p],
                                    PRESET_COORDS[p][0], PRESET_COORDS[p][1]);
                            cityValue.setText(cityText());
                            listener.onPlacePicked(PRESET_NAMES[p],
                                    PRESET_COORDS[p][0], PRESET_COORDS[p][1]);
                        } else {
                            new CityDialog(activity, new CityListener() {
                                @Override
                                public void onCity(String name) {
                                    if (name == null || name.trim().isEmpty()) {
                                        Prefs.clearWeatherPlace(activity);
                                        Prefs.setWeatherCityOnly(activity, "");
                                        cityValue.setText(cityText());
                                        listener.onPlacePicked("", Double.NaN, Double.NaN);
                                    } else {
                                        cityValue.setText(name);
                                        listener.onCityTyped(name);
                                    }
                                }
                            }).show();
                        }
                    }
                });
        menu.show();
    }

    private LinearLayout row(String label) {
        LinearLayout r = new LinearLayout(activity);
        r.setOrientation(LinearLayout.HORIZONTAL);
        r.setGravity(Gravity.CENTER_VERTICAL);
        r.setPadding(dp(16), dp(7), dp(16), dp(7));
        r.setMinimumHeight(dp(46));
        r.setFocusable(true);
        r.setFocusableInTouchMode(true);
        r.setClickable(true);
        r.setBackground(Styles.row(density, 16));
        r.setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);

        TextView name = new TextView(activity);
        name.setText(label);
        name.setTextSize(15.5f);
        name.setTextColor(0xFFF5F5F7);
        r.addView(name, new LinearLayout.LayoutParams(0, WRAP, 1f));

        TextView val = new TextView(activity);
        val.setTextSize(13);
        val.setPadding(dp(12), 0, 0, 0);
        r.addView(val, new LinearLayout.LayoutParams(WRAP, WRAP));
        return r;
    }

    private void updateToggle(TextView v, boolean on) {
        v.setText(Txt.s(activity, on ? "Вкл" : "Выкл", on ? "On" : "Off"));
        v.setTextColor(on ? 0xFF8FE3A4 : 0x8AFFFFFF);
    }

    private LinearLayout.LayoutParams lp(int topMarginDp) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(MATCH, WRAP);
        p.topMargin = dp(topMarginDp);
        return p;
    }

    private int dp(float v) {
        return Math.round(v * density);
    }

    @Override
    public void show() {
        super.show();
        if (firstRow != null) {
            final View f = firstRow;
            f.post(new Runnable() {
                @Override
                public void run() {
                    f.requestFocus();
                }
            });
        }
        card.setAlpha(0f);
        card.setScaleX(0.96f);
        card.setScaleY(0.96f);
        card.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(200)
                .setInterpolator(new DecelerateInterpolator()).start();
    }

    private interface CityListener {
        void onCity(String name);
    }

    /**
     * Text entry is rare on a television, but the remote can drive the system
     * keyboard. A handful of common cities is offered in the menu above, so
     * typing is optional.
     */
    private static class CityDialog extends VaficDialog {

        private final float density;

        CityDialog(Activity a, final CityListener listener) {
            super(a);
            requestWindowFeature(Window.FEATURE_NO_TITLE);
            density = a.getResources().getDisplayMetrics().density;

            FrameLayout card = new GlassPanel(a, density, 28);
            LinearLayout box = new LinearLayout(a);
            box.setOrientation(LinearLayout.VERTICAL);
            box.setPadding(d(24), d(18), d(24), d(16));
            card.addView(box, new FrameLayout.LayoutParams(MATCH, WRAP));

            TextView title = new TextView(a);
            title.setText(Txt.s(a, "Город", "City"));
            title.setTextSize(21);
            title.setTextColor(0xFFFFFFFF);
            title.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
            box.addView(title, new LinearLayout.LayoutParams(MATCH, WRAP));

            TextView hint = new TextView(a);
            hint.setText(Txt.s(a, "Введите название города. Пустое поле — авто по сети.",
                    "Type a city name. Empty means auto by network."));
            hint.setTextSize(12);
            hint.setTextColor(0x59FFFFFF);
            hint.setPadding(0, d(4), 0, d(12));
            box.addView(hint, new LinearLayout.LayoutParams(MATCH, WRAP));

            final EditText input = new EditText(a);
            input.setSingleLine(true);
            input.setTextSize(18);
            input.setTextColor(0xFFFFFFFF);
            input.setHintTextColor(0x59FFFFFF);
            input.setHint(Txt.s(a, "Например: Казань", "For example: London"));
            input.setInputType(InputType.TYPE_CLASS_TEXT);
            input.setImeOptions(EditorInfo.IME_ACTION_DONE);
            input.setPadding(d(14), d(10), d(14), d(10));
            GradientDrawable field = new GradientDrawable();
            field.setColor(0x22FFFFFF);
            field.setCornerRadius(d(12));
            field.setStroke(Math.max(1, d(1)), 0x3D9FC8E8);
            input.setBackground(field);
            box.addView(input, new LinearLayout.LayoutParams(MATCH, WRAP));

            LinearLayout ok = new LinearLayout(a);
            ok.setOrientation(LinearLayout.HORIZONTAL);
            ok.setGravity(Gravity.CENTER);
            ok.setMinimumHeight(d(50));
            ok.setFocusable(true);
            ok.setFocusableInTouchMode(true);
            ok.setClickable(true);
            ok.setBackground(Styles.row(density, 16));
            TextView okLabel = new TextView(a);
            okLabel.setText(Txt.s(a, "Сохранить", "Save"));
            okLabel.setTextSize(16);
            okLabel.setTextColor(0xFFF5F5F7);
            ok.addView(okLabel, new LinearLayout.LayoutParams(WRAP, WRAP));
            LinearLayout.LayoutParams olp = new LinearLayout.LayoutParams(MATCH, WRAP);
            olp.topMargin = d(12);
            box.addView(ok, olp);

            TextView foot = new TextView(a);
            foot.setText(Txt.s(a, "OK — сохранить      •      BACK — отмена",
                    "OK — save      •      BACK — cancel"));
            foot.setTextSize(12);
            foot.setTextColor(0x4DFFFFFF);
            foot.setGravity(Gravity.CENTER);
            foot.setPadding(0, d(12), 0, 0);
            box.addView(foot, new LinearLayout.LayoutParams(MATCH, WRAP));

            final Runnable save = new Runnable() {
                @Override
                public void run() {
                    String name = input.getText() == null ? "" : input.getText().toString().trim();
                    dismiss();
                    listener.onCity(name);
                }
            };

            ok.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    save.run();
                }
            });
            input.setOnEditorActionListener(new TextView.OnEditorActionListener() {
                @Override
                public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                    if (actionId == EditorInfo.IME_ACTION_DONE
                            || (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER
                            && event.getAction() == KeyEvent.ACTION_DOWN)) {
                        save.run();
                        return true;
                    }
                    return false;
                }
            });

            setContentView(card, new ViewGroup.LayoutParams(d(620), WRAP));

            Window w = getWindow();
            if (w != null) {
                w.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                w.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
                w.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
                        | WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
                WindowManager.LayoutParams wl = w.getAttributes();
                wl.dimAmount = 0.6f;
                wl.width = d(620);
                wl.height = WRAP;
                wl.gravity = Gravity.CENTER;
                w.setAttributes(wl);
            }
            MainActivity.installHomeKey(this, a);

            input.postDelayed(new Runnable() {
                @Override
                public void run() {
                    input.requestFocus();
                    try {
                        InputMethodManager imm = (InputMethodManager)
                                a.getSystemService(Context.INPUT_METHOD_SERVICE);
                        if (imm != null) imm.showSoftInput(input, InputMethodManager.SHOW_IMPLICIT);
                    } catch (Exception ignored) {
                    }
                }
            }, 180);
        }

        private int d(float dp) {
            return Math.round(dp * density);
        }
    }
}

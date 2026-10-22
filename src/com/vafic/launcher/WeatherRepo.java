package com.vafic.launcher;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.Process;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Keyless weather backed by Open-Meteo. A television has no GPS, so the place
 * comes either from the city the user typed (geocoded once) or from a single
 * IP lookup. The last answer is cached, which keeps the clock area populated
 * after a restart and while the network is away.
 *
 * Everything runs on a background-priority thread and is only started when
 * the weather section is switched on in the settings.
 */
final class WeatherRepo {

    /** The cache is refreshed about twice an hour. */
    static final long STALE_MS = 30L * 60L * 1000L;

    static final class Day {
        final String iso;
        final int min;
        final int max;

        Day(String iso, int min, int max) {
            this.iso = iso;
            this.min = min;
            this.max = max;
        }
    }

    static final class Data {
        final int temp;
        final int code;
        final String city;
        final long at;
        final List<Day> days;

        Data(int temp, int code, String city, long at, List<Day> days) {
            this.temp = temp;
            this.code = code;
            this.city = city == null ? "" : city;
            this.at = at;
            this.days = days;
        }
    }

    interface Callback {
        void onData(Data d);
    }

    interface PlaceCallback {
        void onPlace(boolean ok);
    }

    private WeatherRepo() {
    }

    /** Last known weather from the preferences cache, or null. */
    static Data cached(Context c) {
        return decode(Prefs.weatherCache(c));
    }

    static boolean isStale(Context c) {
        Data d = cached(c);
        return d == null || System.currentTimeMillis() - d.at > STALE_MS;
    }

    /** Fetches the forecast and posts the result on the main thread. */
    static void refresh(final Context c, final Callback cb) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
                } catch (Exception ignored) {
                }
                final Data d = fetch(c);
                if (d != null) Prefs.setWeatherCache(c, encode(d));
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        cb.onData(d);
                    }
                });
            }
        }, "vafic-weather").start();
    }

    /** Turns a typed city name into coordinates; saves the place on success. */
    static void resolveCity(final Context c, final String name, final PlaceCallback cb) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
                } catch (Exception ignored) {
                }
                boolean ok = false;
                String base = "https://geocoding-api.open-meteo.com/v1/search?count=1&language="
                        + (Txt.russian(c) ? "ru" : "en") + "&format=json&name=";
                String query;
                try {
                    query = URLEncoder.encode(name, "UTF-8");
                } catch (Exception e) {
                    query = name;
                }
                String body = get(base + query);
                if (body != null) {
                    try {
                        JSONObject root = new JSONObject(body);
                        JSONArray results = root.optJSONArray("results");
                        if (results != null && results.length() > 0) {
                            JSONObject r = results.getJSONObject(0);
                            String city = r.optString("name", name);
                            Prefs.setWeatherPlace(c, city, r.getDouble("latitude"),
                                    r.getDouble("longitude"));
                            ok = true;
                        }
                    } catch (Exception ignored) {
                    }
                }
                final boolean success = ok;
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        cb.onPlace(success);
                    }
                });
            }
        }, "vafic-weather-city").start();
    }

    /** The text for a WMO weather code. */
    static String condition(Context c, int code) {
        if (code == 0) return Txt.s(c, "Ясно", "Clear");
        if (code <= 2) return Txt.s(c, "Малооблачно", "Partly cloudy");
        if (code == 3) return Txt.s(c, "Облачно", "Cloudy");
        if (code == 45 || code == 48) return Txt.s(c, "Туман", "Fog");
        if (code >= 51 && code <= 57) return Txt.s(c, "Морось", "Drizzle");
        if (code >= 61 && code <= 67) return Txt.s(c, "Дождь", "Rain");
        if (code >= 71 && code <= 77) return Txt.s(c, "Снег", "Snow");
        if (code >= 80 && code <= 82) return Txt.s(c, "Ливень", "Showers");
        if (code == 85 || code == 86) return Txt.s(c, "Снегопад", "Snow showers");
        if (code >= 95) return Txt.s(c, "Гроза", "Thunderstorm");
        return Txt.s(c, "—", "—");
    }

    /** Short weekday for an ISO date, in the launcher language. */
    static String dayName(Context c, String iso) {
        try {
            Date date = new SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(iso);
            String s = new SimpleDateFormat("EEE", Txt.locale(c)).format(date);
            if (s.endsWith(".")) s = s.substring(0, s.length() - 1);
            if (s.length() > 0) {
                s = s.substring(0, 1).toUpperCase(Txt.locale(c)) + s.substring(1);
            }
            return s;
        } catch (Exception e) {
            return iso;
        }
    }

    static String signed(int value) {
        return (value > 0 ? "+" : "") + value;
    }

    // ------------------------------------------------------------------ net

    /**
     * Open-Meteo is the first choice, but some networks (and this television)
     * cannot reach it. wttr.in is the keyless fallback: it accepts the same
     * coordinates and returns current weather plus a daily forecast.
     */
    private static Data fetch(Context c) {
        float lat = Prefs.weatherLat(c);
        float lon = Prefs.weatherLon(c);
        String city = Prefs.weatherCity(c);

        if (Float.isNaN(lat) || Float.isNaN(lon)) {
            // No city picked yet: try one IP lookup, then remember the place.
            String ip = get("https://ipapi.co/json/", 8000, 8000);
            if (ip == null) return null;
            try {
                JSONObject o = new JSONObject(ip);
                lat = (float) o.getDouble("latitude");
                lon = (float) o.getDouble("longitude");
                String detected = o.optString("city", "");
                if (detected != null && !detected.trim().isEmpty()) city = detected;
                Prefs.setWeatherPlace(c, city, lat, lon);
            } catch (Exception e) {
                return null;
            }
        }

        Data data = fetchOpenMeteo(lat, lon, city);
        if (data == null) data = fetchWttr(c, lat, lon, city);
        return data;
    }

    private static Data fetchOpenMeteo(float lat, float lon, String city) {
        String url = String.format(Locale.US,
                "https://api.open-meteo.com/v1/forecast?latitude=%.4f&longitude=%.4f"
                        + "&current=temperature_2m,weather_code"
                        + "&daily=weather_code,temperature_2m_max,temperature_2m_min"
                        + "&timezone=auto&forecast_days=4", lat, lon);
        // The host is unreachable on some networks; do not keep the refresh
        // thread waiting for the full timeout before the fallback gets a turn.
        String body = get(url, 5000, 7000);
        if (body == null) return null;

        try {
            JSONObject root = new JSONObject(body);
            JSONObject current = root.getJSONObject("current");
            int temp = Math.round((float) current.getDouble("temperature_2m"));
            int code = current.optInt("weather_code", 0);

            List<Day> days = new ArrayList<>();
            JSONObject daily = root.optJSONObject("daily");
            if (daily != null) {
                JSONArray times = daily.optJSONArray("time");
                JSONArray max = daily.optJSONArray("temperature_2m_max");
                JSONArray min = daily.optJSONArray("temperature_2m_min");
                if (times != null && max != null && min != null) {
                    // Skip today: the current temperature is already on screen.
                    for (int i = 1; i < times.length() && days.size() < 3; i++) {
                        days.add(new Day(times.getString(i),
                                Math.round((float) min.getDouble(i)),
                                Math.round((float) max.getDouble(i))));
                    }
                }
            }
            return new Data(temp, code, city, System.currentTimeMillis(), days);
        } catch (Exception e) {
            return null;
        }
    }

    private static Data fetchWttr(Context c, float lat, float lon, String city) {
        String url = String.format(Locale.US,
                "https://wttr.in/%.4f,%.4f?format=j1&lang=%s",
                lat, lon, Txt.russian(c) ? "ru" : "en");
        String body = get(url, 8000, 12000);
        if (body == null) return null;

        try {
            JSONObject root = new JSONObject(body);
            JSONArray currentList = root.optJSONArray("current_condition");
            if (currentList == null || currentList.length() == 0) return null;
            JSONObject current = currentList.getJSONObject(0);
            int temp = Math.round(Float.parseFloat(current.optString("temp_C", "0")));
            int code = wmoFromWwo(current.optInt("weatherCode", 113));

            if (city == null || city.trim().isEmpty()) {
                JSONArray nearest = root.optJSONArray("nearest_area");
                if (nearest != null && nearest.length() > 0) {
                    JSONArray names = nearest.getJSONObject(0).optJSONArray("areaName");
                    if (names != null && names.length() > 0) {
                        city = names.getJSONObject(0).optString("value", "");
                    }
                }
            }

            List<Day> days = new ArrayList<>();
            JSONArray weather = root.optJSONArray("weather");
            if (weather != null) {
                for (int i = 1; i < weather.length() && days.size() < 3; i++) {
                    JSONObject day = weather.getJSONObject(i);
                    days.add(new Day(day.getString("date"),
                            Math.round(Float.parseFloat(day.optString("mintempC", "0"))),
                            Math.round(Float.parseFloat(day.optString("maxtempC", "0")))));
                }
            }
            return new Data(temp, code, city, System.currentTimeMillis(), days);
        } catch (Exception e) {
            return null;
        }
    }

    /** Maps a World Weather Online code (wttr.in) onto the WMO scale. */
    private static int wmoFromWwo(int code) {
        switch (code) {
            case 113:
                return 0;
            case 116:
                return 2;
            case 119:
            case 122:
                return 3;
            case 143:
            case 248:
            case 260:
                return 45;
            case 176:
            case 293:
            case 296:
            case 299:
                return 61;
            case 302:
            case 305:
            case 308:
                return 63;
            case 353:
            case 356:
                return 80;
            case 359:
                return 82;
            case 179:
            case 182:
            case 185:
            case 281:
            case 284:
            case 311:
            case 314:
                return 56;
            case 227:
            case 230:
            case 323:
            case 326:
            case 329:
                return 71;
            case 332:
            case 335:
            case 338:
                return 73;
            case 368:
            case 371:
                return 85;
            case 350:
            case 374:
            case 377:
                return 77;
            case 200:
            case 386:
            case 389:
            case 392:
            case 395:
                return 95;
            default:
                return 3;
        }
    }

    private static String get(String address) {
        return get(address, 8000, 8000);
    }

    private static String get(String address, int connectMs, int readMs) {
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) new URL(address).openConnection();
            conn.setConnectTimeout(connectMs);
            conn.setReadTimeout(readMs);
            conn.setRequestProperty("User-Agent", "VaficLauncher/1.2");
            conn.setInstanceFollowRedirects(true);
            int code = conn.getResponseCode();
            if (code != 200) return null;
            InputStream in = conn.getInputStream();
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int n;
            try {
                while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
            } finally {
                try {
                    in.close();
                } catch (Exception ignored) {
                }
            }
            return out.toString("UTF-8");
        } catch (Exception e) {
            return null;
        } finally {
            if (conn != null) conn.disconnect();
        }
    }

    // ---------------------------------------------------------------- cache

    private static String encode(Data d) {
        StringBuilder b = new StringBuilder();
        b.append(d.temp).append('|').append(d.code).append('|').append(d.at).append('|')
                .append(safe(d.city));
        for (Day day : d.days) {
            b.append('|').append(day.iso).append(':').append(day.min).append(':').append(day.max);
        }
        return b.toString();
    }

    private static Data decode(String s) {
        if (s == null || s.trim().isEmpty()) return null;
        try {
            String[] parts = s.split("\\|");
            if (parts.length < 4) return null;
            int temp = Integer.parseInt(parts[0]);
            int code = Integer.parseInt(parts[1]);
            long at = Long.parseLong(parts[2]);
            String city = parts[3];
            List<Day> days = new ArrayList<>();
            for (int i = 4; i < parts.length; i++) {
                String[] d = parts[i].split(":");
                if (d.length != 3) continue;
                days.add(new Day(d[0], Integer.parseInt(d[1]), Integer.parseInt(d[2])));
            }
            return new Data(temp, code, city, at, days);
        } catch (Exception e) {
            return null;
        }
    }

    private static String safe(String s) {
        return s == null ? "" : s.replace('|', '/').replace(':', ' ');
    }
}

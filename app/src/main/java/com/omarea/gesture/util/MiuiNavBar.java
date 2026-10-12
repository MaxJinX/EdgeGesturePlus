package com.omarea.gesture.util;

import android.content.Context;
import android.content.SharedPreferences;

import com.omarea.gesture.SpfConfig;
import com.omarea.gesture.shell.KeepShellPublic;

/**
 * MIUI / HyperOS 导航栏隐藏（需要 ROOT 权限）。
 *
 * <p>原理是修改 MIUI 的两个 global 设置项：
 * <ul>
 *     <li>{@code force_fsg_nav_bar=1} 切换为全面屏手势模式，隐藏传统的三键导航栏</li>
 *     <li>{@code hide_gesture_line=1} 隐藏底部的手势提示线（小白条）</li>
 * </ul>
 *
 * <p>注意：MIUI 会在开机启动桌面（launcher）时把 {@code hide_gesture_line} 重置回 0，
 * 所以需要在开机、无障碍服务重启后重新下发一次，见 {@link #applyIfEnabled(Context)}。
 *
 * <p>所有方法都会阻塞等待 shell 返回（首次还会弹出 ROOT 授权框），
 * 必须在子线程中调用，不要放在主线程。
 */
public class MiuiNavBar {
    /** MIUI 全屏手势开关 */
    private static final String KEY_FSG = "force_fsg_nav_bar";
    /** MIUI 手势提示线（小白条） */
    private static final String KEY_LINE = "hide_gesture_line";

    private MiuiNavBar() {
    }

    /**
     * 是否 MIUI / HyperOS 设备
     */
    public static boolean isMiui() {
        try {
            return new SystemProperty().isMiui();
        } catch (Exception ex) {
            return false;
        }
    }

    /**
     * 开启隐藏。首次开启时会备份原始设置，便于关闭时还原。
     *
     * @return false 表示没有拿到 ROOT 权限，或读取原始设置失败
     */
    public static boolean enable(Context context) {
        if (!KeepShellPublic.checkRoot()) {
            return false;
        }

        SharedPreferences config = getConfig(context);

        // 只在首次开启时备份，避免把已经隐藏后的状态当成原始状态存下来
        if (!config.getBoolean(SpfConfig.MIUI_NAV_BAR_BACKUP_VALID, false)) {
            String fsg = get(KEY_FSG);
            String line = get(KEY_LINE);
            if (fsg == null || line == null) {
                return false;
            }
            config.edit()
                    .putString(SpfConfig.MIUI_NAV_BAR_BACKUP_FSG, fsg)
                    .putString(SpfConfig.MIUI_NAV_BAR_BACKUP_LINE, line)
                    .putBoolean(SpfConfig.MIUI_NAV_BAR_BACKUP_VALID, true)
                    .apply();
        }

        write(KEY_FSG, "1");
        write(KEY_LINE, "1");
        return true;
    }

    /**
     * 关闭隐藏，还原为开启之前的导航栏设置。
     *
     * @return false 表示没有拿到 ROOT 权限
     */
    public static boolean disable(Context context) {
        if (!KeepShellPublic.checkRoot()) {
            return false;
        }

        SharedPreferences config = getConfig(context);
        if (config.getBoolean(SpfConfig.MIUI_NAV_BAR_BACKUP_VALID, false)) {
            restore(KEY_FSG, config.getString(SpfConfig.MIUI_NAV_BAR_BACKUP_FSG, null));
            restore(KEY_LINE, config.getString(SpfConfig.MIUI_NAV_BAR_BACKUP_LINE, null));
            config.edit().putBoolean(SpfConfig.MIUI_NAV_BAR_BACKUP_VALID, false).apply();
        } else {
            // 没有备份（例如应用数据被清理过），退化为还原成 MIUI 默认状态
            write(KEY_FSG, "0");
            write(KEY_LINE, "0");
        }
        return true;
    }

    /**
     * 若开关处于开启状态，则重新下发一次隐藏命令。
     * 用于开机 / 无障碍服务重启后，抵消 MIUI 对手势提示线的重置。
     */
    public static void applyIfEnabled(Context context) {
        if (!getConfig(context).getBoolean(SpfConfig.HIDE_MIUI_NAV_BAR, SpfConfig.HIDE_MIUI_NAV_BAR_DEFAULT)) {
            return;
        }
        if (!KeepShellPublic.checkRoot()) {
            return;
        }
        write(KEY_FSG, "1");
        write(KEY_LINE, "1");
    }

    private static SharedPreferences getConfig(Context context) {
        return context.getSharedPreferences(SpfConfig.ConfigFile, Context.MODE_PRIVATE);
    }

    /**
     * 读取一个 global 设置项，设置项不存在时 MIUI 会返回字符串 "null"。
     *
     * @return null 表示 shell 执行失败
     */
    private static String get(String key) {
        String value = KeepShellPublic.doCmdSync("settings get global " + key);
        if (value == null) {
            return null;
        }
        value = value.trim();
        if (value.isEmpty() || "error".equals(value)) {
            return null;
        }
        return value;
    }

    private static void write(String key, String value) {
        KeepShellPublic.doCmdSync("settings put global " + key + " " + value);
    }

    private static void restore(String key, String original) {
        if (original == null || original.isEmpty() || "null".equals(original)) {
            // 原本就没有这一项，删掉而不是写成 0
            KeepShellPublic.doCmdSync("settings delete global " + key);
        } else {
            write(key, original);
        }
    }
}

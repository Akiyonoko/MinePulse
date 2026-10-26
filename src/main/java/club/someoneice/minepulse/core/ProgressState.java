package club.someoneice.minepulse.core;

/**
 * 客户端那一份"当前这条脉还剩多少"的镜像，由服务端 ProgressPacket 刷新。
 * 纯数据，服务端也存在这个类（但永远不会被写入）。
 */
public final class ProgressState {
    private static volatile int total;
    private static volatile int remaining;
    private static volatile long lastUpdate;

    private ProgressState() {
    }

    public static void update(int total, int remaining) {
        ProgressState.total = Math.max(1, total);
        ProgressState.remaining = Math.max(0, remaining);
        ProgressState.lastUpdate = System.currentTimeMillis();
    }

    /** 只要收到过进度就认为可见，淡出交给 HudProgress 按 idleMillis 判断（挖完最后一块也要能看到 0 / N）。 */
    public static boolean visible() {
        return lastUpdate > 0;
    }

    public static int total() {
        return total;
    }

    public static int remaining() {
        return remaining;
    }

    /** 距离最后一次挖掘过去了多少毫秒，松开鼠标后据此淡出。 */
    public static long idleMillis() {
        return lastUpdate == 0L ? Long.MAX_VALUE : System.currentTimeMillis() - lastUpdate;
    }
}

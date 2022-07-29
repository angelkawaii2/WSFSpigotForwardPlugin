package dev.raincandy.spigot.wsmsgforward.chat;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 记录消息转发的对象，
 */
public class ChatRecordManager {

    /**
     * 记录消息
     */
    static Map<String, ChatRecord> ss = getCircularMap(500);

    /**
     * 添加一个context，用于之后可能的回调
     * 注意，此方法会自动移除较早的请求
     *
     * @param reply reply对象
     */
    public static void addContext(ChatRecord reply) {
        ss.put(reply.getRequestId(), reply);
    }

    public static ChatRecord getReplyContext(String requestId) {
        return ss.get(requestId);
    }

    public static void setQueueLength(int size) {
        Map<String, ChatRecord> tmp = getCircularMap(size);
        if (!ss.isEmpty()) {
            tmp.putAll(ss);
        }
        ss = tmp;
    }

    private static Map<String, ChatRecord> getCircularMap(int size) {
        return new LinkedHashMap<>(size) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, ChatRecord> eldest) {
                return size() > size;
            }
        };
    }

}

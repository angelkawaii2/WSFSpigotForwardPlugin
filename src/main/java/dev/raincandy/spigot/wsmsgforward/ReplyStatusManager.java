package dev.raincandy.spigot.wsmsgforward;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 记录玩家的 reply 状态
 */
public class ReplyStatusManager {

    /**
     * 玩家的 UUID 和 消息的 replyId
     */
    static Map<UUID, String> playerReplyStatus = new HashMap<>();

    /**
     * 记录用户的reply状态
     *
     * @param player  玩家的 UUID
     * @param replyId 玩家回复的消息 ID
     */
    public static void setPlayerReplyStatus(UUID player, String replyId) {
        playerReplyStatus.put(player, replyId);
    }

    public static boolean hasPlayerReplyStatus(UUID player) {
        return playerReplyStatus.containsKey(player);
    }

    public static void removePlayerReplyStatus(UUID player) {
        playerReplyStatus.remove(player);
    }

    public static String getPlayerReplyId(UUID player) {
        return playerReplyStatus.get(player);
    }

}

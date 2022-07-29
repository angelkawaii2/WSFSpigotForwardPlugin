package dev.raincandy.spigot.wsmsgforward.listener;

import dev.raincandy.sdk.websocket.protocol.v1.bean.body.modules.minecraft.chat.MinecraftChatBodyBean;
import dev.raincandy.sdk.websocket.protocol.v1.bean.send.ForwardBean;
import dev.raincandy.sdk.websocket.util.WSConnInfoManager;
import dev.raincandy.spigot.wsmsgforward.BukkitPluginConfig;
import dev.raincandy.spigot.wsmsgforward.ReplyStatusManager;
import dev.raincandy.spigot.wsmsgforward.WsSpigotMsgForward;
import dev.raincandy.spigot.wsmsgforward.chat.ChatRecord;
import dev.raincandy.spigot.wsmsgforward.chat.ChatRecordManager;
import dev.raincandy.spigot.wsmsgforward.protocol.bean.BukkitMinecraftChatDataBean;
import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Optional;


/**
 * 处理玩家聊天转发的bukkit监听器
 */
public class BukkitChatListener implements Listener {

    private final WsSpigotMsgForward plugin;
    private final BukkitPluginConfig bukkitPluginConfig;

    public BukkitChatListener(WsSpigotMsgForward plugin, BukkitPluginConfig conf) {
        this.plugin = plugin;

        this.bukkitPluginConfig = conf;
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onPlayerReplyChat(AsyncPlayerChatEvent e) {

        //构造消息body
        var mcd = new BukkitMinecraftChatDataBean(e);
        var mcb = new MinecraftChatBodyBean(mcd);
        var fb = new ForwardBean(mcb);

        var player = e.getPlayer();

        //如果玩家在reply状态，处理reply的部分
        Optional.ofNullable(ReplyStatusManager.getPlayerReplyId(player.getUniqueId()))
                .ifPresent(id -> {
                    mcd.setReplyId(id);
                    //玩家回复完后要结束回复的状态
                    ReplyStatusManager.removePlayerReplyStatus(player.getUniqueId());
                    //如果玩家是reply，稍微改写一下chat的样式，比较好识别
                    //todo 这里改成遍历玩家发消息，最好不要修改事件，避免干扰到其他插件？
                    e.setMessage(e.getMessage() + ChatColor.GRAY + " [已回复]");
                });

        for (var sConfig : bukkitPluginConfig.getServerConfigSet()) {
            //是否启用插件
            if (!sConfig.getSetting().isEnable()) {
                continue;
            }

            var chatConf = sConfig.getChat();
            //是否允许转发
            if (!chatConf.isEnableForward()) {
                continue;
            }
            //是否有 replyId 或 转发前缀
            if (!mcd.hasReplyId() && !e.getMessage().startsWith(chatConf.getForwardPrefix())) {
                continue;
            }

            //记录此消息，用于未来其他端回复此消息时做提示使用
            var record = new ChatRecord(mcd.getRequestId(),
                    e.getMessage(), player.getUniqueId());

            ChatRecordManager.addContext(record);

            fb.addTargetClientId(chatConf.getTargetClientId());
            // getName 就是 connectionId
            var connInfo = WSConnInfoManager.getConnInfo(sConfig.getName());

            if (connInfo == null) {
                plugin.getLogger().severe("未找到服务器配置：" + sConfig.getName());
                return;
            }

            //此方法可能会在同步环境触发，所以需要确保异步发送
            new BukkitRunnable() {
                @Override
                public void run() {
                    //发送消息
                    plugin.getApi().getBukkitWebsocketConnectionManager()
                            .getConnection(connInfo.getConnectionId())
                            .getWebsocketContext()
                            .send(fb);
                }
            }.runTaskAsynchronously(plugin);

        }
    }


}

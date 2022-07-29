package dev.raincandy.spigot.wsmsgforward.websocket.handler;

import dev.raincandy.sdk.websocket.util.IWsListenerMsgHandler;
import dev.raincandy.sdk.websocket.protocol.v1.bean.body.modules.qq.chat.QQChatBodyBean;
import dev.raincandy.sdk.websocket.protocol.v1.bean.received.ReceivedMainBean;
import dev.raincandy.sdk.websocket.websocket.SDKAbstractWebsocketListener;
import dev.raincandy.spigot.wsmsgforward.WsSpigotMsgForward;
import dev.raincandy.spigot.wsmsgforward.chat.ChatRecordManager;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Optional;

public class BukkitQQChatHandler implements IWsListenerMsgHandler {

    private final JavaPlugin plugin;

    public BukkitQQChatHandler(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void onMessage(SDKAbstractWebsocketListener listener, ReceivedMainBean mainBean, String originText) {

        var connInfo = listener.getWebsocketContext().getConnInfo();
        var logger = plugin.getLogger();

        var config =
                WsSpigotMsgForward.bukkitPluginConfig.getServerConfigById(connInfo.getConnectionId());

        //检查配置文件是否启用
        var chatConf = config.getChat();
        if (!chatConf.isAllowReceivedChat()) {
            logger.warning("收到远程聊天消息，但配置文件中未启用。");
            return;
        }
        if (chatConf.isEnableWhitelistRemoteClient()
                && !chatConf.getRemoteWhitelistClientIds().contains(mainBean.getSourceClientId())) {
            logger.warning("收到远程指令，但来源客户端ID不在白名单列表中");
            return;
        }

        //解析body数据
        var chatBody = mainBean.getBody().convert(QQChatBodyBean.class);

        var qqChatData = chatBody.getData();

        //这条消息的requestId，回复时设置消息的replyId为此ID
        var requestId = qqChatData.getRequestId();

        //消息主体
        var msgBody = new TextComponent(String.format("<%s> %s%s  "
                , qqChatData.getSenderName()
                , qqChatData.getMsg().contains("\n") ? "\n" : ""
                , qqChatData.getMsg()));

        //消息来源
        var source = new TextComponent(ChatColor.RESET + String.format("[QQ%s]"
                , qqChatData.isGroupChat() ? "群" : "私聊"));
        source.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                new Text(String.format("%s[发送者: %s][%s]"
                        , qqChatData.isGroupChat() ? String.format("[群: %d]", qqChatData.getGroup()) : "[私聊]"
                        , qqChatData.getSenderName(),
                        qqChatData.isAnonymousQQ() ? "匿名" : String.format("QQ: %d", qqChatData.getSenderQQ())))));


        //处理消息发送逻辑
        Bukkit.getScheduler().runTask(plugin, () -> {
            for (Player p : Bukkit.getOnlinePlayers()) {
                //消息
                var msg = new ComponentBuilder();
                //如果此QQ消息 是 回复某条 MC chat消息
                Optional.ofNullable(qqChatData.getReplyId()).flatMap(
                                id -> Optional.ofNullable(ChatRecordManager.getReplyContext(id)))
                        .ifPresent(replyContext -> {

                            var rp = Bukkit.getOfflinePlayer(replyContext.getSenderUuid());

                            //回复的目前用户
                            var isSelf = rp.getUniqueId().equals(p.getUniqueId());

                            //播放提醒音效
                            if (isSelf) {
                                Optional.ofNullable(rp.getPlayer()).ifPresent(
                                        pr -> pr.playSound(pr.getLocation(),
                                                Sound.ENTITY_CHICKEN_EGG, 1, 1));
                            }

                            var replyMsg = new TextComponent(String.format("%s[回复%s ]" + ChatColor.RESET,
                                    isSelf ? ChatColor.GOLD : ChatColor.GRAY,
                                    isSelf ? "了你" : ": " + rp.getName()));
                            replyMsg.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                                    new Text("原消息:" + replyContext.getOriginalMsg())));
                            msg.append(replyMsg);
                        });
                msg.append(source).append(msgBody).append(getReplyBtn(requestId));
                p.spigot().sendMessage(msg.create());
            }
        });

    }

    private TextComponent getReplyBtn(String replyId) {
        var replyBtn = new TextComponent("[点击回复]");
        replyBtn.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/wsreply reply " + replyId));
        replyBtn.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text("可以直接点击回复这条消息哦~~")));
        replyBtn.setItalic(true);
        replyBtn.setUnderlined(true);
        replyBtn.setColor(ChatColor.GRAY);
        return replyBtn;
    }


}

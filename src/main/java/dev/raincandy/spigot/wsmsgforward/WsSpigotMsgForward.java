package dev.raincandy.spigot.wsmsgforward;

import dev.raincandy.sdk.websocket.protocol.ConnectionInfo;
import dev.raincandy.sdk.websocket.protocol.v1.bean.body.modules.minecraft.chat.MinecraftChatDataBean;
import dev.raincandy.sdk.websocket.protocol.v1.bean.body.modules.minecraft.command.MinecraftCommandDataBean;
import dev.raincandy.sdk.websocket.protocol.v1.bean.body.modules.qq.chat.QQChatDataBean;
import dev.raincandy.sdk.websocket.util.DataTypeClassRegister;
import dev.raincandy.sdk.websocket.util.MessageHandler;
import dev.raincandy.sdk.websocket.util.WSConnInfoManager;
import dev.raincandy.sdk.websocket.util.WSClientUitl;
import dev.raincandy.sdk.websocket.util.SDKWebsocketConnManager;
import dev.raincandy.spigot.wsmsgforward.listener.BukkitChatListener;
import dev.raincandy.spigot.wsmsgforward.websocket.BukkitWebsocketListener;
import dev.raincandy.spigot.wsmsgforward.websocket.handler.BukkitMcCmdHandler;
import dev.raincandy.spigot.wsmsgforward.websocket.handler.BukkitQQChatHandler;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.logging.Logger;

public final class WsSpigotMsgForward extends JavaPlugin {

    public static BukkitPluginConfig bukkitPluginConfig = null;
    public Logger logger = getLogger();
    private final static SDKWebsocketConnManager<BukkitWebsocketListener> connManager
            = new SDKWebsocketConnManager<>();

    private final WsSpigotMsgForwardApi api = new WsSpigotMsgForwardApi(this);

    protected static SDKWebsocketConnManager<BukkitWebsocketListener> getBukkitConnectionManager() {
        return connManager;
    }


    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        //检查指令
        //这里是内部指令，不对玩家公开，所以也不需要做提示

        if ("wsreply".equalsIgnoreCase(label)) {

            if (args.length == 0) {
                return true;
            }

            switch (args[0]) {
                case "reply" -> {
                    if (!(sender instanceof Player)) {
                        sender.sendMessage("只有玩家可以执行这条命令");
                        return true;
                    }
                    if (args.length < 2) {
                        break;
                    }

                    var playerUid = ((Player) sender).getUniqueId();
                    var replyId = args[1];
                    //这里没法检查是否“过期”，因为这个 replyId 实际回复效果取决于消息来源方的处理
                    ReplyStatusManager.setPlayerReplyStatus(playerUid, replyId);
                    sender.spigot().sendMessage(
                            new TextComponent(ChatColor.GRAY + "[提示] 已进入回复模式，在聊天框发送消息就可以回复哦~ ")
                            , getCancelBtn());
                }
                case "cancel" -> {
                    if (!(sender instanceof Player)) {
                        sender.sendMessage("只有玩家可以执行这条命令");
                        return true;
                    }
                    var playerUid = ((Player) sender).getUniqueId();
                    if (ReplyStatusManager.hasPlayerReplyStatus(playerUid)) {
                        ReplyStatusManager.removePlayerReplyStatus(playerUid);
                        sender.sendMessage(ChatColor.GRAY + "[提示] 已取消回复~");
                    }
                }
                case "reload" -> {
                    if (!sender.isOp()) {
                        sender.sendMessage(ChatColor.RED + "仅OP可执行此命令。");
                        return true;
                    }
                    //重载
                    sender.sendMessage(ChatColor.GREEN + "开始重载...");
                    onDisable();
                    reloadConfig();
                    onEnable();
                    sender.sendMessage(ChatColor.GREEN + "重载完毕。");
                }
                default -> {
                }
            }

        }

        return true;
    }

    @Override
    public void onEnable() {
        logger.info("-------------------------");
        // Plugin startup logic
        DataTypeClassRegister.registerDataType("minecraft-chat", MinecraftChatDataBean.class);
        DataTypeClassRegister.registerDataType("minecraft-command", MinecraftCommandDataBean.class);
        DataTypeClassRegister.registerDataType("qq-chat", QQChatDataBean.class);

        //初始化配置文件
        saveDefaultConfig();
        bukkitPluginConfig = new BukkitPluginConfig(getConfig());

        if (!bukkitPluginConfig.isPluginEnabled()) {
            logger.info(ChatColor.RED + "=====插件未启用，请在config.yml中启用。=====");
            return;
        }

        logger.info(ChatColor.GREEN + "=====插件已启用，开始加载服务器配置=====");
        //打印给用户看配置
        printConfig();

        //处理onMsg的handler，用于注册到 websocket listener
        var bukkitChatHander = new BukkitQQChatHandler(this);
        var bukkitMcCmdHandler = new BukkitMcCmdHandler(this);

        //websocket消息的解析器工具类
        MessageHandler msgHandler = new MessageHandler();
        //注册解析qq聊天和mc指令
        msgHandler.registerType("qq-chat", bukkitChatHander);
        msgHandler.registerType("minecraft-command", bukkitMcCmdHandler);

        for (BukkitPluginConfig.ServerConfig sConfig : bukkitPluginConfig.getServerConfigSet()) {

            if (!sConfig.getSetting().isEnable()) {
                continue;
            }

            var connectionId = sConfig.getName();
            var setting = sConfig.getSetting();

            //构造连接信息
            var url = setting.getServerUrl();

            //如果 URL 是 ws(s)，处理为 http(s)
            if (url.startsWith("ws://") || url.startsWith("wss://")) {
                url = url.replace("ws://", "http://").replace("wss://", "https://");
            }
            var connInfo = new ConnectionInfo(connectionId, url
                    , setting.getToken(), setting.getClientId());

            WSConnInfoManager.addConnInfo(connInfo);

            //开启连接
            var wsListener = new BukkitWebsocketListener(connInfo);
            //设置消息解析器
            wsListener.setMsgHandler(msgHandler);
            //建立连接
            WSClientUitl.createConnection(connInfo, wsListener);

            //添加连接到manager
            connManager.addConnection(connectionId, wsListener);

            logger.info("┣━━━━━━━━━━━━━");
        }
        logger.info(ChatColor.GREEN + "┗━  加载完毕");

        //注册聊天监听器，处理聊天事件
        getServer().getPluginManager().registerEvents(
                new BukkitChatListener(this, bukkitPluginConfig), this);

    }

    public WsSpigotMsgForwardApi getApi() {
        return api;
    }

    /**
     * 一个取消按钮的 TextComponent
     *
     * @return
     */
    private TextComponent getCancelBtn() {
        //构造取消按钮
        var clickEvent = new ClickEvent(ClickEvent.Action.RUN_COMMAND,
                "/wsreply cancel");
        var cancelBtn = new TextComponent("[取消]");
        cancelBtn.setUnderlined(true);
        cancelBtn.setItalic(true);
        cancelBtn.setColor(net.md_5.bungee.api.ChatColor.GRAY);
        cancelBtn.setClickEvent(clickEvent);
        cancelBtn.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text("点击取消回复~")));
        return cancelBtn;
    }

    @Override
    public void onDisable() {
        logger.info("插件停用中...");
        // Plugin shutdown logic
        //结束所有 websocket 连接
        logger.info("结束所有连接中...");
        connManager.shutdownAllConnection();
        //WSClientUitl.shutdown();
        logger.info("连接结束完毕.");
        logger.info("-------------------");

        //todo 检查有没有其他的线程要关闭
    }

    /**
     * 打印一个酷炫的config到console
     */
    private void printConfig() {

        var logger = getLogger();

        for (BukkitPluginConfig.ServerConfig sConfig : bukkitPluginConfig.getServerConfigSet()) {

            var connectionId = sConfig.getName();
            logger.info("┏━ 配置ID: " + connectionId);
            var setting = sConfig.getSetting();

            if (!setting.isEnable()) {
                logger.info(ChatColor.DARK_RED + "┃ ┗━ 此服务器未启用，跳过...");
                continue;
            }


            logger.info("┣━ ClientID: " + setting.getClientId());
            logger.info("┣━ Token: " + setting.getToken());

            //连接安全检查
            var url = setting.getServerUrl();
            logger.info("┣━ URL: " + url);
            if (url.startsWith("http://")) {
                logger.warning("┃ ┗━ 提示: 此地址为非加密连接，建议使用 https");
            }
            if (url.startsWith("wss://") || url.startsWith("ws://")) {
                logger.warning("┃ ┗━ 提示: 暂不支持 ws/wss ，已自动替换为 http(s) ");
            }

            //列个表
            var chatConfig = sConfig.getChat();
            logger.info("┣━ 聊天转发: " + (chatConfig.isEnableForward() ?
                    ChatColor.GREEN + "已启用" : ChatColor.RED + "已禁用"));
            var forwardPrefix = chatConfig.getForwardPrefix();
            logger.info("┣━ 聊天转发前缀: "
                    + (forwardPrefix.isEmpty() ? " 无前缀，将转发所有消息" : forwardPrefix));
            if (chatConfig.isEnableForward()) {
                logger.info("┃  ┗━ 目标客户端ID: ");
                for (String s : chatConfig.getTargetClientId()) {
                    logger.info("┃      ┣━  " + s);
                }
            }
            logger.info("┃  ┣━ 远程聊天信息接收: " + (chatConfig.isAllowReceivedChat() ?
                    ChatColor.GREEN + "已启用" : ChatColor.RED + "已禁用"));

            if (chatConfig.isAllowReceivedChat()) {
                if (chatConfig.isEnableWhitelistRemoteClient()) {
                    logger.info("┃  ┗━ 白名单客户端ID: ");
                    for (String s : chatConfig.getRemoteWhitelistClientIds()) {
                        logger.info("┃      ┣━  " + s);
                    }
                } else {
                    logger.warning("┃  ┗━ 注意: 未设置远程白名单客户端ID，将接收所有来源的聊天消息 ");
                }
            }


            //加载远程指令执行信息
            var cmdConfig = sConfig.getCommand();
            logger.info("┣━ 远程指令执行: "
                    + (cmdConfig.isAllowRemoteCommand() ?
                    ChatColor.GREEN + "已启用" : ChatColor.RED + "已禁用"));
            if (cmdConfig.isAllowRemoteCommand()) {
                if (!cmdConfig.isEnableWhitelistRemoteClient()) {
                    logger.warning("┃  ┗━ 警告: 未设置远程白名单客户端ID，请确保连接到可信任的服务器");
                } else {
                    logger.warning("┃  ┗━ 远程白名单客户端ID:");
                    for (String s : cmdConfig.getRemoteWhitelistClientIds()) {
                        logger.warning("┃      ┣━  " + s);
                    }
                }
            }
        }
    }
}

package dev.raincandy.spigot.wsmsgforward.websocket.handler;

import dev.raincandy.sdk.websocket.util.IWsListenerMsgHandler;
import dev.raincandy.sdk.websocket.protocol.v1.bean.body.modules.minecraft.command.MinecraftCommandBodyBean;
import dev.raincandy.sdk.websocket.protocol.v1.bean.received.ReceivedMainBean;
import dev.raincandy.sdk.websocket.websocket.SDKAbstractWebsocketListener;
import dev.raincandy.sdk.websocket.websocket.WSCommandCallback;
import dev.raincandy.spigot.wsmsgforward.WsSpigotMsgForward;
import dev.raincandy.spigot.wsmsgforward.util.BukkitMinecraftNativeCommandDispatcher;
import dev.raincandy.spigot.wsmsgforward.websocket.BukkitWSCmdSender;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

public class BukkitMcCmdHandler implements IWsListenerMsgHandler {

    private final JavaPlugin plugin;

    public BukkitMcCmdHandler(JavaPlugin plugin) {
        this.plugin = plugin;
    }


    @Override
    public void onMessage(SDKAbstractWebsocketListener listener, ReceivedMainBean mainBean, String originText) {

        var connInfo = listener.getWebsocketContext().getConnInfo();
        var logger = plugin.getLogger();

        var config =
                WsSpigotMsgForward.bukkitPluginConfig.getServerConfigById(connInfo.getConnectionId());

        //鉴权
        var cmdConf = config.getCommand();
        if (!cmdConf.isAllowRemoteCommand()) {
            logger.warning("收到远程指令，但配置文件中未允许执行远程命令。");
            return;
        }
        if (cmdConf.isEnableWhitelistRemoteClient()
                && !cmdConf.getRemoteWhitelistClientIds().contains(mainBean.getSourceClientId())) {
            logger.warning("收到远程指令，但来源客户端ID不在白名单列表中");
            return;
        }

        //处理指令

        var data =
                mainBean.getBody().convert(MinecraftCommandBodyBean.class).getData();
        var command = data.getCommand().trim().replaceAll(" +", " ");

        var sourceClientId = mainBean.getSourceClientId();


        //忽略一部分有时效性的指令
        String[] ignoreCmds = {"list", "tps"};

        for (String ignoreCmd : ignoreCmds) {
            //丢弃过时的list包
            if (ignoreCmd.equalsIgnoreCase(command)) {
                var sendTime = Instant.ofEpochMilli(mainBean.getTimestamp());
                //如果返回正数，代表超过阈值已经过期，不执行这个list指令
                //todo 这里阈值是60秒，以后可能要弄个可自定义的配置文件
                if (Instant.now().compareTo(
                        sendTime.plus(60, ChronoUnit.SECONDS)) > 0) {
                    logger.info(String.format("[%s] 收到来自 %s 的过期 %s 指令，未执行。"
                            , sendTime, sourceClientId, ignoreCmd));
                    return;
                }
            }
        }


        //执行minecraft命令
        var wsCallback = new WSCommandCallback(sourceClientId, data.getRequestId(), listener);
        var nativeCmdDispatcher = new BukkitMinecraftNativeCommandDispatcher(wsCallback);

        Bukkit.getScheduler().runTask(plugin, () -> {
            //对于一部分指令需要特殊处理，否则拿不到回显
            if (nativeCmdDispatcher.isNativeCommand(command)) {
                //wsCallback.callback(nativeCmdDispatcher.dispatchCommand(command));
                nativeCmdDispatcher.dispatchCommandv2(command);
            } else {
                Bukkit.getServer().dispatchCommand(
                        //自定义的sender，包含了命令数据和回调
                        //回调有ws的引用，会由sender异步调用 其中的callback(msg)方法通过websocket返回信息
                        new BukkitWSCmdSender(plugin, data, wsCallback), command);
            }
        });

        logger.info(String.format("[ForwardCMD][%s][%s][%s]<%s> %s"
                , connInfo.getConnectionId(), sourceClientId
                , data.getSource(), data.getSender(), command));
    }
}

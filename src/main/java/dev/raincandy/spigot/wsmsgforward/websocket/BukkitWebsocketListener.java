package dev.raincandy.spigot.wsmsgforward.websocket;

import com.google.gson.JsonSyntaxException;
import dev.raincandy.sdk.websocket.protocol.ConnectionInfo;
import dev.raincandy.sdk.websocket.protocol.v1.bean.received.ReceivedMainBean;
import dev.raincandy.sdk.websocket.util.GsonUtil;
import dev.raincandy.sdk.websocket.util.MessageHandler;
import dev.raincandy.sdk.websocket.websocket.SDKAbstractWebsocketListener;
import okhttp3.Response;
import okhttp3.WebSocket;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BukkitWebsocketListener extends SDKAbstractWebsocketListener {

    private final Logger logger;
    private MessageHandler msgHandler = new MessageHandler();

    public BukkitWebsocketListener(ConnectionInfo connectionInfo) {
        super(connectionInfo);
        this.logger = LoggerFactory.getLogger(
                "BukkitWsListener-" + connectionInfo.getConnectionId());
    }

    @Override
    public void onOpen(@NotNull WebSocket webSocket, @NotNull Response response) {
        super.onOpen(webSocket, response);
        var content = """
                                
                --------------------
                Connection: %s established.
                code: %d , msg: %s
                --------------------
                """
                .formatted(getConnInfo().getConnectionId(), response.code(), response.message());
        logger.info(content);
    }


    @Override
    public void onFailure(@NotNull WebSocket webSocket, @NotNull Throwable t, @Nullable Response response) {
        super.onFailure(webSocket, t, response);
        var content = """
                                
                --------------------
                Connection: %s onFailure.
                Exception: %s
                Response: %s
                --------------------
                """.formatted(getConnInfo().getConnectionId(), t, response);
        logger.warn(content);
    }

    @Override
    public void onClosed(@NotNull WebSocket webSocket, int code, @NotNull String reason) {
        super.onClosed(webSocket, code, reason);
        logger.info("-----------------");
        logger.info("连接: %s 被服务器关闭，理由: " + reason);
        //非正常原因关闭的，需要重连
        switch (code) {
            case 1000 -> logger.info(getConnInfo().getConnectionId() + " 1000 连接正常关闭，不执行重连。");
            case 4010 -> logger.warn(getConnInfo().getConnectionId() + " 4010 身份验证失败！" + reason);
            case 5000 -> logger.warn(getConnInfo().getConnectionId() + " 5000 服务器错误: " + reason);
            default -> logger.warn(getConnInfo().getConnectionId() + " " + code + " 其他错误: " + reason);
        }
    }


    @Override
    public void onMessage(@NotNull WebSocket webSocket, @NotNull String text) {
        logger.info("收到服务器消息: \n" + text);

        try {
            var mb = GsonUtil.getGson().fromJson(text, ReceivedMainBean.class);
            msgHandler.handleMsg(this, mb, text);
        } catch (JsonSyntaxException e) {
            e.printStackTrace();
            logger.warn("解析错误，无法正确解析的json: \n" + text);
        } catch (Exception e) {
            e.printStackTrace();
            logger.warn("处理时发生其他异常: \n" + text);
        }
    }

    private ConnectionInfo getConnInfo() {
        return getWebsocketContext().getConnInfo();
    }

    public MessageHandler getMsgHandler() {
        return msgHandler;
    }

    public void setMsgHandler(MessageHandler msgHandler) {
        this.msgHandler = msgHandler;
    }
}

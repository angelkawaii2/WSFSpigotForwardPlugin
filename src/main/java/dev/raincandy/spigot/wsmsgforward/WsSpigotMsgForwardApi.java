package dev.raincandy.spigot.wsmsgforward;

import dev.raincandy.sdk.websocket.util.SDKWebsocketConnManager;
import dev.raincandy.spigot.wsmsgforward.websocket.BukkitWebsocketListener;

public class WsSpigotMsgForwardApi {

    private WsSpigotMsgForward plugin;

    protected WsSpigotMsgForwardApi(WsSpigotMsgForward plugin) {
        this.plugin = plugin;
    }

    public SDKWebsocketConnManager<BukkitWebsocketListener> getBukkitWebsocketConnectionManager() {
        return WsSpigotMsgForward.getBukkitConnectionManager();
    }

/*
    public boolean sendMessageToBukkitListener(String connectionId, SendMainBean data) {
        BukkitWebsocketListener listener = getBukkitWebsocketConnectionManager().getConnection(connectionId);
        if (listener != null) {
            listener.getWebsocketContext().send(data);
        }
        return false;

    }
*/

}

# Websocket Forward plugin for Spigot/Paper

用于 Spigot/Paper 的消息转发插件，基于 Websocket Forward SDK。

## 需求 Requirements

For active dev-branch

- spigot/paper 1.16+
- Java 17+

## To-do

- [ ] debug mode
- [ ] message queue

## 更新日志 Changelogs

v1.0.0 20220711
1. released

## Api

样例Example:

```java

class test {
    public static void main(String[] args) {
        WsSpigotMsgForward plugin = null;
        //get plugin instance
        if (Bukkit.getServer().getPluginManager().isPluginEnabled("WsSpigotMsgForward")) {
            plugin = (WsSpigotMsgForward) Bukkit.getServer().getPluginManager().getPlugin("WsSpigotMsgForward");
            
            //build message(reuse MinecraftCommandEcho module)
            MinecraftCommandEchoDataBean msgBean
                    = new MinecraftCommandEchoDataBean(
                    //msgid
                    UUID.randomUUID().toString(),
                    //put your message here
                    "This is a sample message");

            MinecraftCommandEchoBodyBean body = new MinecraftCommandEchoBodyBean(msgBean);
            ForwardBean mainBody = new ForwardBean(body);
            //add receiver id
            mainBody.addTargetClientId("example-client2");
            
            
            WsSpigotMsgForwardApi api = plugin.getApi();
            //get manager for websocket listener
            SDKWebsocketConnManager<BukkitWebsocketListener> manager = api.getBukkitWebsocketConnectionManager();
            //get websocket connection
            BukkitWebsocketListener listener = manager.getConnection("connectionID");
            

            //send message
            if (listener.getWebsocketContext().send(mainBody)) {
                //success
            } else {
                //fail
            }

        }
    }
}

```

## Releases

see Github Release pages

## License

Mozilla MPL v2.0


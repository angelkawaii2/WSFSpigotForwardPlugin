package dev.raincandy.spigot.wsmsgforward.websocket;

import dev.raincandy.sdk.websocket.protocol.v1.bean.body.modules.minecraft.command.MinecraftCommandDataBean;
import dev.raincandy.sdk.websocket.websocket.WSCommandCallback;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Server;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.conversations.Conversation;
import org.bukkit.conversations.ConversationAbandonedEvent;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionAttachment;
import org.bukkit.permissions.PermissionAttachmentInfo;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * 如果接口是 RemoteConsoleCommandSender，执行指令(可能仅限内部指令？) 时将不会输出回显到控制台
 * 为了让回显记录到日志，所以这里继承了 ConsoleCommandSender
 */
public class BukkitWSCmdSender implements ConsoleCommandSender {

    private final List<String> msgList = new LinkedList<>();
    private JavaPlugin plugin;
    private MinecraftCommandDataBean data;
    private WSCommandCallback wsCallback;
    private BukkitTask bukkitTask = null;
    private long lastUpdateTime = 0;
    private long timeoutMs = 500;

    /**
     * 需要plugin，和command的详细内容
     * <p>
     *
     * @param p          插件
     * @param data       命令数据
     * @param wsCallback
     */
    public BukkitWSCmdSender(JavaPlugin p, MinecraftCommandDataBean data, WSCommandCallback wsCallback) {
        this.plugin = p;
        this.data = data;
        this.wsCallback = wsCallback;
    }

    @Override
    public String getName() {
        return String.format("[WS-Forward-CMD][%s]<%s>", data.getSource(), data.getSender());
    }

    @Override
    public Server getServer() {
        return plugin.getServer();
    }

    @Override
    public void sendMessage(String arg0) {
        //这里处理回调消息发送，注意要异步
        msgList.add(ChatColor.stripColor(arg0));
        lastUpdateTime = System.currentTimeMillis();

        //需要缓冲，以免部分插件响应时短时间大量调用此命令
        if (bukkitTask == null) {
            bukkitTask = Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                long current = System.currentTimeMillis();
                while (lastUpdateTime + timeoutMs > current) {
                    try {
                        Thread.sleep(lastUpdateTime + timeoutMs - current);
                        current = System.currentTimeMillis();
                    } catch (InterruptedException ignored) {
                    }
                }
                wsCallback.callback(StringUtils.join(msgList, "\n"));
                this.bukkitTask = null;
            });
        }
    }

    @Override
    public void sendMessage(String[] arg0) {
        sendMessage(StringUtils.join(arg0, "\n"));
    }

    @Override
    public void sendMessage(@Nullable UUID sender, @NotNull String message) {
        this.sendMessage(message);
    }

    @Override
    public void sendMessage(@Nullable UUID sender, @NotNull String[] messages) {
        this.sendMessage(messages);
    }

    @Override
    public Spigot spigot() {
        return plugin.getServer().getConsoleSender().spigot();
    }

    @Override
    public PermissionAttachment addAttachment(Plugin arg0) {
        return plugin.getServer().getConsoleSender().addAttachment(arg0);
    }

    @Override
    public PermissionAttachment addAttachment(Plugin arg0, int arg1) {
        return plugin.getServer().getConsoleSender().addAttachment(arg0, arg1);
    }

    @Override
    public PermissionAttachment addAttachment(Plugin arg0, String arg1, boolean arg2) {
        return plugin.getServer().getConsoleSender().addAttachment(arg0, arg1, arg2);
    }

    @Override
    public PermissionAttachment addAttachment(Plugin arg0, String arg1, boolean arg2, int arg3) {
        return plugin.getServer().getConsoleSender().addAttachment(arg0, arg1, arg2, arg3);
    }

    @Override
    public Set<PermissionAttachmentInfo> getEffectivePermissions() {
        return plugin.getServer().getConsoleSender().getEffectivePermissions();
    }

    @Override
    public boolean hasPermission(String arg0) {
        return plugin.getServer().getConsoleSender().hasPermission(arg0);
    }

    @Override
    public boolean hasPermission(Permission arg0) {
        return plugin.getServer().getConsoleSender().hasPermission(arg0);
    }

    @Override
    public boolean isPermissionSet(String arg0) {
        return plugin.getServer().getConsoleSender().isPermissionSet(arg0);
    }

    @Override
    public boolean isPermissionSet(Permission arg0) {
        return plugin.getServer().getConsoleSender().isPermissionSet(arg0);
    }

    @Override
    public void recalculatePermissions() {
        plugin.getServer().getConsoleSender().recalculatePermissions();
    }

    @Override
    public void removeAttachment(PermissionAttachment arg0) {
        plugin.getServer().getConsoleSender().removeAttachment(arg0);
    }

    @Override
    public boolean isOp() {
        return plugin.getServer().getConsoleSender().isOp();
    }

    @Override
    public void setOp(boolean arg0) {
        plugin.getServer().getConsoleSender().setOp(arg0);
    }

    @Override
    public void abandonConversation(Conversation arg0) {
        plugin.getServer().getConsoleSender().abandonConversation(arg0);
    }

    @Override
    public void abandonConversation(Conversation arg0, ConversationAbandonedEvent arg1) {
        plugin.getServer().getConsoleSender().abandonConversation(arg0, arg1);
    }

    @Override
    public void acceptConversationInput(String arg0) {
        plugin.getServer().getConsoleSender().acceptConversationInput(arg0);
    }

    @Override
    public boolean beginConversation(Conversation arg0) {
        return plugin.getServer().getConsoleSender().beginConversation(arg0);
    }

    @Override
    public boolean isConversing() {
        return plugin.getServer().getConsoleSender().isConversing();
    }

    @Override
    public void sendRawMessage(@NotNull String message) {
        msgList.add(ChatColor.stripColor(message));
        lastUpdateTime = System.currentTimeMillis();
    }

    @Override
    public void sendRawMessage(@Nullable UUID sender, @NotNull String message) {
        msgList.add(ChatColor.stripColor(message));
        lastUpdateTime = System.currentTimeMillis();
    }

}
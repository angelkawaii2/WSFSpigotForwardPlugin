package dev.raincandy.spigot.wsmsgforward.protocol.bean;

import dev.raincandy.sdk.websocket.protocol.v1.bean.body.modules.minecraft.chat.MinecraftChatDataBean;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.util.UUID;

/**
 * bukkit用的类，便于使用MinecraftChatDataBean
 */
public class BukkitMinecraftChatDataBean extends MinecraftChatDataBean {


    public BukkitMinecraftChatDataBean(AsyncPlayerChatEvent e) {
        var p = e.getPlayer();
        setRequestId(UUID.randomUUID().toString());
        setSender(new BukkitSenderDTO(p));
        setSource(new BukkitSourceDTO(p.getWorld()));
        setMsg(e.getMessage());
    }


    public static class BukkitSourceDTO extends MinecraftChatDataBean.SourceDTO {

        public BukkitSourceDTO(World w) {
            super();
            setWorldName(w.getName());
            setWorldEnvironment(w.getEnvironment().toString());
            setWorldUid(w.getUID().toString());
        }

    }

    public static class BukkitSenderDTO extends MinecraftChatDataBean.SenderDTO {


        public BukkitSenderDTO(Player p) {
            setUuid(p.getUniqueId().toString());
            setDisplayName(p.getDisplayName());
            setIsOp(p.isOp());
            setIsWhitelist(p.isWhitelisted());
            setLevel(p.getLevel());
            setHp(p.getHealth());
            setPing(p.getPing());
        }

    }
}

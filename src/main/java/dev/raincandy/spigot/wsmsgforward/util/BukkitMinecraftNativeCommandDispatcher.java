package dev.raincandy.spigot.wsmsgforward.util;

import dev.raincandy.sdk.websocket.websocket.WSCommandCallback;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.BanList;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.help.HelpTopic;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

import static org.bukkit.Bukkit.getServer;

/**
 * 由于vanilla无法获取native指令回显
 * 本类用于模拟执行 Minecraft Native 命令
 */
public class BukkitMinecraftNativeCommandDispatcher {

    /**
     * 处理cmd的回显
     */
    private final WSCommandCallback wsCallback;

    public BukkitMinecraftNativeCommandDispatcher(WSCommandCallback wsCallback) {
        this.wsCallback = wsCallback;
    }

    /**
     * 判断指令是否为原版指令
     *
     * @param command 指令，不带斜杠/
     * @return
     */
    public boolean isNativeCommand(String command) {
        String[] cmd = new String[]{
                "ban",
                "unban",
                "pardon",
                "banlist",
                "wtl",
                "whitelist",
                "op",
                "deop",
                "help",
                "list",
                //"tps"//需要paper api，暂时不做这个
        };
        var cmd2 = command.replaceAll(" +", " ").split(" ");
        for (String s : cmd) {
            if (cmd2[0].equalsIgnoreCase(s)) {
                return true;
            }
        }
        return false;
    }

    private static UUID uuid32to36(String uuid32) {
        if (uuid32.length() != 32) {
            throw new IllegalArgumentException("uuid is not 32! " + uuid32);
        }

        uuid32 = uuid32.substring(0, 8) + "-"
                + uuid32.substring(8, 12) + "-"
                + uuid32.substring(12, 16) + "-"
                + uuid32.substring(16, 20) + "-"
                + uuid32.substring(20);

        return UUID.fromString(uuid32);
    }

    /**
     * 处理 minecraft 指令
     * todo 逐步取消对minecraft id的支持
     * todo 20220221 检测到legacy id时应转换成UUID
     * todo 20220221 （异步获取后再进入主线程调用此方法）
     *
     * @param command
     * @return todo 20210823 要检查如果启用正版，则强制使用UUID
     */
    public void dispatchCommandv2(String command) {

        var cmd = command.split(" ");

        if (cmd.length > 0) {
            switch (cmd[0].toLowerCase(Locale.ROOT)) {
                case "ban": {
                    if (cmd.length >= 2) {

                        if (cmd[1].length() > 16 || cmd[1].length() < 3) {
                            if (cmd[1].length() != 36 && cmd[1].length() != 32) {
                                wsCallback.callback("不正确的玩家ID(UUID需为32或36位): " + cmd[1]);
                                return;
                            }
                        }

                        OfflinePlayer p;
                        UUID uid = null;
                        if (cmd[1].length() == 32) {
                            uid = uuid32to36(cmd[2]);
                        } else if (cmd[1].length() == 36) {
                            uid = UUID.fromString(cmd[1]);
                        }

                        if (uid == null) {
                            p = Bukkit.getOfflinePlayer(cmd[1]);
                        } else {
                            p = Bukkit.getOfflinePlayer(uid);
                        }

                        if (p.isBanned()) {
                            wsCallback.callback(String.format("[提示] %s 已经是封禁状态了", p.getName()));
                            return;
                        }

                        if (cmd.length == 2) {
                            //不允许直接调用，必须给出封禁理由
                            wsCallback.callback("[错误] 需要提供一个封禁理由，便于后续管理。");
                            return;
                        }

                        if (p.isOnline()) {
                            Optional.ofNullable(p.getPlayer())
                                    .ifPresent(pp -> pp.kickPlayer("你已被服务器封禁。"));
                        }
                        if (cmd.length == 3) {
                            Bukkit.getBanList(BanList.Type.NAME).addBan(cmd[1], cmd[2], null, null);
                            wsCallback.callback(String.format("已永久封禁玩家: %s ,原因: %s ", cmd[1], cmd[2]));
                            return;
                        } else if (cmd.length == 4) {
                            var expires = Date
                                    .from(Instant.now().plus(Duration.ofDays(Long.parseLong(cmd[3]))));
                            getServer().getBanList(BanList.Type.NAME).addBan(cmd[1], cmd[2], expires, null);
                            wsCallback.callback(String.format("已临时封禁: %s ,原因: %s ,时间: %s 天"
                                    , cmd[1], cmd[2], cmd[3]));
                            return;
                        } else if (cmd.length == 5) {
                            var expires = Date
                                    .from(Instant.now().plus(Duration.ofDays(Long.parseLong(cmd[3]))));
                            getServer().getBanList(BanList.Type.NAME).addBan(cmd[1], cmd[2], expires, cmd[4]);
                            wsCallback.callback(String.format("已临时封禁: %s ,原因: %s ,时间: %s 天,来源: %s"
                                    , cmd[1], cmd[2], cmd[3], cmd[4]));
                            return;
                        }
                    }
                    wsCallback.callback("用法: /ban <player|UUID> <reason> [expires] [source]");
                }


                case "unban":
                case "pardon": {
                    if (cmd.length >= 2) {

                        if (cmd[1].length() > 16 || cmd[1].length() < 3) {
                            if (cmd[1].length() != 36 && cmd[1].length() != 32) {
                                wsCallback.callback("不正确的玩家ID(UUID需为32或36位): " + cmd[1]);
                                return;
                            }
                        }

                        OfflinePlayer p;
                        UUID uid = null;
                        if (cmd[1].length() == 32) {
                            uid = uuid32to36(cmd[2]);
                        } else if (cmd[1].length() == 36) {
                            uid = UUID.fromString(cmd[1]);
                        }

                        if (uid == null) {
                            p = Bukkit.getOfflinePlayer(cmd[1]);
                        } else {
                            p = Bukkit.getOfflinePlayer(uid);
                        }

                        if (!p.isBanned()) {
                            wsCallback.callback(String.format("[提示] %s 未被封禁。", p.getName()));
                        } else {
                            Bukkit.getBanList(BanList.Type.NAME).pardon(cmd[1]);
                            wsCallback.callback(String.format("[提示] 已解除 %s 的封禁。", p.getName()));
                        }
                        return;
                    }
                    wsCallback.callback("用法: /unban <player|UUID> ");
                }
                case "wtl":
                case "whitelist": {
                    if (cmd.length >= 2) {
                        switch (cmd[1]) {
                            case "on":
                                if (Bukkit.hasWhitelist()) {
                                    wsCallback.callback("[提示]白名单原已开启");
                                } else {
                                    Bukkit.setWhitelist(true);
                                    wsCallback.callback("已开启白名单");
                                }
                                return;
                            case "off":
                                if (Bukkit.hasWhitelist()) {
                                    Bukkit.setWhitelist(false);
                                    wsCallback.callback("已关闭白名单");
                                } else {
                                    wsCallback.callback("[提示]白名单原已关闭");
                                }
                                return;
                            case "list":
                            case "l":
                                var set = new TreeSet<String>();
                                for (OfflinePlayer p : getServer().getWhitelistedPlayers()) {
                                    set.add(p.getName());
                                }
                                var pstr = StringUtils.join(set, " , ");
                                wsCallback.callback(String.format("白名单总数: %d\r\n玩家列表: %s", set.size(), pstr));
                                return;
                            case "reload":
                                Bukkit.reloadWhitelist();
                                wsCallback.callback("白名单列表已重新加载，总数: "
                                        + Bukkit.getWhitelistedPlayers().size());
                                return;
                            case "add":
                            case "a":
                            case "remove":
                            case "r":
                            case "check":
                            case "c":
                                if (cmd.length < 3) {
                                    wsCallback.callback("用法: /whitelist|wtl <a(dd)|r(emove)|c(heck)> <player>");
                                    return;
                                }

                                if (cmd[2].length() > 16 || cmd[2].length() < 3) {
                                    if (cmd[2].length() != 36 && cmd[2].length() != 32) {
                                        wsCallback.callback("不正确的玩家ID(UUID需为32或36位): " + cmd[2]);
                                        return;
                                    }
                                }

                                OfflinePlayer p;
                                UUID uid = null;
                                if (cmd[2].length() == 32) {
                                    uid = uuid32to36(cmd[2]);
                                } else if (cmd[2].length() == 36) {
                                    uid = UUID.fromString(cmd[2]);
                                }

                                if (uid == null) {
                                    p = Bukkit.getOfflinePlayer(cmd[2]);
                                } else {
                                    p = Bukkit.getOfflinePlayer(uid);
                                }

                                var displayName = p.getName();
                                if (displayName == null && uid != null) {
                                    displayName = "(UUID) " + uid;
                                }

                                switch (cmd[1]) {
                                    case "add", "a" -> {
                                        if (p.isWhitelisted()) {
                                            wsCallback.callback("[提示]此玩家已经在白名单列表中了");
                                        } else {
                                            p.setWhitelisted(true);
                                            wsCallback.callback(String.format("已将玩家 %s 加入白名单列表", displayName));
                                        }
                                        return;
                                    }
                                    case "remove", "r" -> {
                                        if (!p.isWhitelisted()) {
                                            wsCallback.callback("[提示]此玩家没有在白名单列表中");
                                        } else {
                                            p.setWhitelisted(false);
                                            wsCallback.callback(String.format("已移除 %s 的白名单", displayName));
                                        }
                                        return;
                                    }
                                    case "check", "c" -> {
                                        wsCallback.callback(String.format("玩家 %s 在白名单列表中"
                                                , p.isWhitelisted() ? "" : "不"));
                                        return;
                                    }
                                    default -> {
                                    }
                                }
                            default:
                        }
                    }
                    wsCallback.callback("用法: /whitelist|wtl <l(ist)|on|off|reload|a(dd)|r(emove)|c(heck)> [player]");
                }
                case "banlist": {
                    if (cmd.length == 2) {
                        if ("l".equals(cmd[1]) || "list".equals(cmd[1])) {
                            var plist = new TreeSet<String>();

                            for (var p : Bukkit.getBannedPlayers()) {
                                plist.add(p.getName());
                            }
                            var pnamel = StringUtils.join(plist, " , ");
                            wsCallback.callback(String.format("已封禁: %d 名玩家\n 封禁列表: %s", plist.size(), pnamel));
                        }
                    } else if (cmd.length == 3) {
                        if ("c".equals(cmd[1]) || "check".equals(cmd[1])) {
                            var p = Bukkit.getOfflinePlayer(cmd[2]);
                            wsCallback.callback(String.format("玩家 %s %s在封禁列表中"
                                    , p.getName(), p.isBanned() ? "" : "不"));
                        }
                    }
                    wsCallback.callback("用法: /banlist <l(ist)|c(heck)> [player]");
                }
                case "op":
                case "deop": {
                    if (cmd.length >= 2) {

                        if (cmd[1].length() > 16 || cmd[1].length() < 3) {
                            if (cmd[1].length() != 36 && cmd[1].length() != 32) {
                                wsCallback.callback("不正确的玩家ID(UUID需为32或36位): " + cmd[1]);
                                return;
                            }
                        }

                        OfflinePlayer p;
                        UUID uid = null;
                        if (cmd[1].length() == 32) {
                            uid = uuid32to36(cmd[2]);
                        } else if (cmd[1].length() == 36) {
                            uid = UUID.fromString(cmd[1]);
                        }

                        if (uid == null) {
                            p = Bukkit.getOfflinePlayer(cmd[1]);
                        } else {
                            p = Bukkit.getOfflinePlayer(uid);
                        }


                        var setOpCmd = "op".equals(cmd[0]);
                        var isOp = p.isOp();
                        p.setOp(setOpCmd);
                        if (setOpCmd) {
                            wsCallback.callback(isOp ? "此玩家已经是OP了" : "已给予此玩家OP权限");
                        } else {
                            wsCallback.callback(isOp ? "已移除此玩家的OP权限" : "此玩家不是OP");
                        }
                        return;
                    }
                    wsCallback.callback("用法: /op|deop <player>");
                    return;
                }
                case "help": {
                    var helpTopics = getServer().getHelpMap().getHelpTopics();
                    var sb = new StringBuilder();
                    for (HelpTopic ht : helpTopics) {
                        sb.append(ht.getName()).append(":").append(ht.getShortText()).append("\n");
                    }
                    wsCallback.callback(sb.toString());
                    return;
                }
                case "list": {
                    var onlineP = Bukkit.getOnlinePlayers();
                    var nameSet = new TreeSet<String>();
                    for (Player p : onlineP) {
                        nameSet.add(p.getDisplayName());
                    }
                    if (nameSet.size() > 0) {
                        wsCallback.callback(String.format("在线人数(%d): %s"
                                , nameSet.size(), StringUtils.join(nameSet, " , ")));
                    } else {
                        wsCallback.callback("此服务器当前无人在线");
                    }
                    return;
                }
                case "tps": {

                }
                default:
            }
        }
        wsCallback.callback("[错误]指令长度为0，无法执行");
    }

    /**
     * 处理 minecraft 指令
     *
     * @param command
     * @return
     * @deprecated 用另一个无返回的方法
     */
    @Deprecated
    public String dispatchCommand(String command) {

        var cmd = command.split(" ");

        if (cmd.length > 0) {
            switch (cmd[0].toLowerCase(Locale.ROOT)) {
                case "ban": {
                    if (cmd.length >= 2) {

                        final var p = Bukkit.getOfflinePlayer(cmd[1]);

                        if (p.isBanned()) {
                            return String.format("[提示] %s 已经是封禁状态了", p.getName());
                        } else {
                            if (cmd.length == 2) {
                                //不允许直接调用，必须给出封禁理由
                                return "[错误] 需要提供一个封禁理由，便于后续管理。";
                            }

                            if (p.isOnline()) {
                                Optional.ofNullable(p.getPlayer())
                                        .ifPresent(pp -> pp.kickPlayer("你已被服务器封禁。"));
                            }
                            if (cmd.length == 3) {
                                Bukkit.getBanList(BanList.Type.NAME).addBan(cmd[1], cmd[2], null, null);
                                return String.format("已永久封禁玩家: %s ,原因: %s ", cmd[1], cmd[2]);
                            } else if (cmd.length == 4) {
                                var expires = Date
                                        .from(Instant.now().plus(Duration.ofDays(Long.parseLong(cmd[3]))));
                                getServer().getBanList(BanList.Type.NAME).addBan(cmd[1], cmd[2], expires, null);
                                return String.format("已临时封禁: %s ,原因: %s ,时间: %s 天"
                                        , cmd[1], cmd[2], cmd[3]);
                            } else if (cmd.length == 5) {
                                var expires = Date
                                        .from(Instant.now().plus(Duration.ofDays(Long.parseLong(cmd[3]))));
                                getServer().getBanList(BanList.Type.NAME).addBan(cmd[1], cmd[2], expires, cmd[4]);
                                return String.format("已临时封禁: %s ,原因: %s ,时间: %s 天,来源: %s"
                                        , cmd[1], cmd[2], cmd[3], cmd[4]);
                            }
                        }
                    }
                    return "用法: /ban <player|UUID> <reason> [expires] [source]";
                }


                case "unban":
                case "pardon": {
                    if (cmd.length >= 2) {
                        final var p = Bukkit.getOfflinePlayer(cmd[1]);

                        if (!p.isBanned()) {
                            return String.format("[提示] %s 未被封禁。", p.getName());
                        } else {
                            Bukkit.getBanList(BanList.Type.NAME).pardon(cmd[1]);
                            return String.format("[提示] 已解除 %s 的封禁。", p.getName());
                        }
                    }
                    return "用法: /unban <player|UUID> ";
                }
                case "wtl":
                case "whitelist": {
                    if (cmd.length >= 2) {
                        switch (cmd[1]) {
                            case "on":
                                if (Bukkit.hasWhitelist()) {
                                    return "[提示]白名单原已开启";
                                }
                                Bukkit.setWhitelist(true);
                                return "已开启白名单";
                            case "off":
                                if (Bukkit.hasWhitelist()) {
                                    Bukkit.setWhitelist(false);
                                    return "已关闭白名单";
                                }
                                return "[提示]白名单原已开启";
                            case "list":
                            case "l":
                                var set = new TreeSet<String>();
                                for (OfflinePlayer p : getServer().getWhitelistedPlayers()) {
                                    set.add(p.getName());
                                }
                                var pstr = StringUtils.join(set, " , ");
                                return String.format("白名单总数: %d\r\n玩家列表: %s", set.size(), pstr);
                            case "reload":
                                Bukkit.reloadWhitelist();
                                return "白名单列表已重新加载";
                            case "add":
                            case "a":
                            case "remove":
                            case "r":
                            case "check":
                            case "c":
                                if (cmd.length < 3) {
                                    return "用法: /whitelist|wtl <a(dd)|r(emove)|c(heck)> <player>";
                                }

                                if (cmd[2].length() > 16 || cmd[2].length() < 3) {
                                    if (cmd[2].length() != 36 && cmd[2].length() != 32) {
                                        return "不正确的玩家ID(UUID需为32或36位): " + cmd[2];
                                    }
                                }

                                OfflinePlayer p;
                                UUID uid = null;
                                if (cmd[2].length() == 32) {
                                    uid = uuid32to36(cmd[2]);
                                }
                                if (cmd[2].length() == 36) {
                                    uid = UUID.fromString(cmd[2]);
                                }

                                if (uid == null) {
                                    p = Bukkit.getOfflinePlayer(cmd[2]);
                                } else {
                                    p = Bukkit.getOfflinePlayer(uid);
                                }

                                switch (cmd[1]) {
                                    case "add":
                                    case "a":
                                        if (p.isWhitelisted()) {
                                            return "[提示]此玩家已经在白名单列表中了";
                                        } else {
                                            p.setWhitelisted(true);
                                            return String.format("已将玩家 %s 加入白名单列表", p.getName());
                                        }
                                    case "remove":
                                    case "r":
                                        if (!p.isWhitelisted()) {
                                            return "[提示]此玩家没有在白名单列表中";
                                        } else {
                                            p.setWhitelisted(false);
                                            return String.format("已移除 %s 的白名单", p.getName());
                                        }
                                    case "check":
                                    case "c":
                                        return String.format("玩家 %s 在白名单列表中"
                                                , p.isWhitelisted() ? "" : "不");
                                    default:
                                }
                            default:
                        }
                    }
                    return "用法: /whitelist|wtl <l(ist)|on|off|reload|a(dd)|r(emove)|c(heck)> [player]";
                }
                case "banlist": {
                    if (cmd.length == 2) {
                        if ("l".equals(cmd[1]) || "list".equals(cmd[1])) {
                            var plist = new TreeSet<String>();

                            for (var p : Bukkit.getBannedPlayers()) {
                                plist.add(p.getName());
                            }
                            var pnamel = StringUtils.join(plist, " , ");
                            return String.format("已封禁: %d 名玩家\n 封禁列表: %s", plist.size(), pnamel);
                        }
                    } else if (cmd.length == 3) {
                        if ("c".equals(cmd[1]) || "check".equals(cmd[1])) {
                            var p = Bukkit.getOfflinePlayer(cmd[2]);
                            return String.format("玩家 %s %s在封禁列表中"
                                    , p.getName(), p.isBanned() ? "" : "不");
                        }
                    }
                    return "用法: /banlist <l(ist)|c(heck)> [player]";
                }
                case "op":
                case "deop": {
                    if (cmd.length >= 2) {
                        var p = Bukkit.getOfflinePlayer(cmd[1]);
                        var setOpCmd = "op".equals(cmd[0]);
                        var isOp = p.isOp();
                        p.setOp(setOpCmd);
                        if (setOpCmd) {
                            return isOp ? "此玩家已经是OP了" : "已给予此玩家OP权限";
                        } else {
                            return isOp ? "已移除此玩家的OP权限" : "此玩家不是OP";
                        }
                    }
                    return "用法: /op|deop  <player>";
                }
                case "help": {
                    var helpTopics = getServer().getHelpMap().getHelpTopics();
                    var sb = new StringBuilder();
                    for (HelpTopic ht : helpTopics) {
                        sb.append(ht.getName()).append(":").append(ht.getShortText()).append("\n");
                    }
                    return sb.toString();
                }
                case "list": {
                    var onlineP = Bukkit.getOnlinePlayers();
                    var nameSet = new TreeSet<String>();
                    for (Player p : onlineP) {
                        nameSet.add(p.getDisplayName());
                    }
                    if (nameSet.size() > 0) {
                        return String.format("在线人数(%d): %s"
                                , nameSet.size(), StringUtils.join(nameSet, " , "));
                    } else {
                        return "此服务器当前无人在线";
                    }
                }
                case "tps": {

                }
                default:
            }
        }
        return "[错误]指令长度为0，无法执行";
    }


}

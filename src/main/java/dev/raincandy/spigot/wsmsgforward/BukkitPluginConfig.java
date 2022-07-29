package dev.raincandy.spigot.wsmsgforward;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class BukkitPluginConfig {

    private FileConfiguration config;

    public BukkitPluginConfig(FileConfiguration config) {
        this.config = config;
    }

    /**
     * 获取配置文件中已经启用的所有连接信息set
     *
     * @return
     */
    public Set<ServerConfig> getServerConfigSet() {
        //封装为一个配置文件类
        var servers = config.getConfigurationSection("servers");

        var serversKeys = servers.getKeys(false);

        Set<ServerConfig> infoSet = new HashSet<>();

        for (String k : serversKeys) {
            var server = servers.getConfigurationSection(k);
            var serverConfig = new ServerConfig(server);
            infoSet.add(serverConfig);
        }
        return infoSet;
    }

    public ServerConfig getServerConfigById(String connectionId) {
        for (ServerConfig s : getServerConfigSet()) {
            if (s.getName().equals(connectionId)) {
                return s;
            }
        }
        return null;
    }

    /**
     * 插件是否启用
     *
     * @return
     */
    public boolean isPluginEnabled() {
        return config.getBoolean("enable");
    }

    public static class ServerConfig {

        private final ConfigurationSection serverSection;

        public ServerConfig(ConfigurationSection serverSection) {
            this.serverSection = serverSection;
        }

        public String getName() {
            return serverSection.getName();
        }


        public Setting getSetting() {
            return new Setting(serverSection.getConfigurationSection("setting"));
        }

        public Chat getChat() {
            return new Chat(serverSection.getConfigurationSection("chat"));
        }

        public Command getCommand() {
            return new Command(serverSection.getConfigurationSection("command"));
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ServerConfig that = (ServerConfig) o;
            return Objects.equals(serverSection, that.serverSection);
        }

        @Override
        public int hashCode() {
            return Objects.hash(serverSection);
        }

        public static class Setting {
            private final ConfigurationSection settingSection;

            public Setting(ConfigurationSection settingSection) {
                this.settingSection = settingSection;
            }

            public boolean isEnable() {
                return settingSection.getBoolean("connect");
            }

            public String getClientId() {
                return settingSection.getString("clientId");
            }

            public String getToken() {
                return settingSection.getString("token");
            }

            public String getServerUrl() {
                return settingSection.getString("url");
            }
        }

        public static class Chat {
            private final ConfigurationSection chatSection;

            public Chat(ConfigurationSection chatSection) {
                this.chatSection = chatSection;
            }

            public List<String> getRemoteWhitelistClientIds() {
                return chatSection.getStringList("whitelistRemoteClientIds");
            }

            public boolean isAllowReceivedChat() {
                return chatSection.getBoolean("allowReceiveChat");
            }

            public boolean isEnableForward() {
                return chatSection.getBoolean("forward");
            }

            public String getForwardPrefix() {
                return chatSection.getString("forwardPrefix");
            }

            public List<String> getTargetClientId() {
                return chatSection.getStringList("targetClientIds");
            }

            public boolean isEnableWhitelistRemoteClient() {
                return getRemoteWhitelistClientIds().size() > 0;
            }
        }

        public static class Command {
            private final ConfigurationSection commandSection;

            public Command(ConfigurationSection commandSection) {
                this.commandSection = commandSection;
            }

            public boolean isAllowRemoteCommand() {
                return commandSection.getBoolean("allowRemoteCommand");
            }

            public boolean isEnableWhitelistRemoteClient() {
                return getRemoteWhitelistClientIds().size() > 0;
            }

            public List<String> getRemoteWhitelistClientIds() {
                return commandSection.getStringList("whitelistRemoteClientIds");
            }

        }

    }
}

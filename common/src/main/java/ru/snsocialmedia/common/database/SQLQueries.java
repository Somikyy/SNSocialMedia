package ru.snsocialmedia.common.database;

/**
 * Класс с SQL-запросами для работы с базой данных MySQL
 */
public class SQLQueries {

        // Запросы для создания таблиц
        public static final String CREATE_GUILDS_TABLE = "CREATE TABLE IF NOT EXISTS guilds (" +
                        "id CHAR(36) PRIMARY KEY, " +
                        "name VARCHAR(32) NOT NULL UNIQUE, " +
                        "tag VARCHAR(8) NOT NULL UNIQUE, " +
                        "description TEXT, " +
                        "leader CHAR(36) NOT NULL, " +
                        "creation_date DATETIME NOT NULL, " +
                        "level INT NOT NULL, " +
                        "experience INT NOT NULL" +
                        ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4";

        public static final String CREATE_GUILD_MEMBERS_TABLE = "CREATE TABLE IF NOT EXISTS guild_members (" +
                        "guild_id CHAR(36) NOT NULL, " +
                        "player_id CHAR(36) NOT NULL, " +
                        "role VARCHAR(16) NOT NULL, " +
                        "PRIMARY KEY (guild_id, player_id), " +
                        "FOREIGN KEY (guild_id) REFERENCES guilds(id) ON DELETE CASCADE" +
                        ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4";

        public static final String CREATE_GUILD_INVITES_TABLE = "CREATE TABLE IF NOT EXISTS guild_invites (" +
                        "guild_id CHAR(36) NOT NULL, " +
                        "player_id CHAR(36) NOT NULL, " +
                        "PRIMARY KEY (guild_id, player_id), " +
                        "FOREIGN KEY (guild_id) REFERENCES guilds(id) ON DELETE CASCADE" +
                        ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4";

        public static final String CREATE_FRIEND_REQUESTS_TABLE = "CREATE TABLE IF NOT EXISTS friend_requests (" +
                        "id CHAR(36) PRIMARY KEY, " +
                        "sender_id CHAR(36) NOT NULL, " +
                        "receiver_id CHAR(36) NOT NULL, " +
                        "request_date DATETIME NOT NULL, " +
                        "status VARCHAR(16) NOT NULL" +
                        ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4";

        public static final String CREATE_FRIENDSHIPS_TABLE = "CREATE TABLE IF NOT EXISTS friendships (" +
                        "id CHAR(36) PRIMARY KEY, " +
                        "player1_id CHAR(36) NOT NULL, " +
                        "player2_id CHAR(36) NOT NULL, " +
                        "friendship_date DATETIME NOT NULL, " +
                        "favorite BOOLEAN NOT NULL DEFAULT FALSE, " +
                        "UNIQUE KEY unique_friendship (player1_id, player2_id)" +
                        ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4";

        public static final String CREATE_PARTIES_TABLE = "CREATE TABLE IF NOT EXISTS parties (" +
                        "id CHAR(36) PRIMARY KEY, " +
                        "leader CHAR(36) NOT NULL, " +
                        "creation_date DATETIME NOT NULL, " +
                        "open BOOLEAN NOT NULL DEFAULT FALSE, " +
                        "current_server VARCHAR(32)" +
                        ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4";

        public static final String CREATE_PARTY_MEMBERS_TABLE = "CREATE TABLE IF NOT EXISTS party_members (" +
                        "party_id CHAR(36) NOT NULL, " +
                        "player_id CHAR(36) NOT NULL, " +
                        "role VARCHAR(16) NOT NULL, " +
                        "PRIMARY KEY (party_id, player_id), " +
                        "FOREIGN KEY (party_id) REFERENCES parties(id) ON DELETE CASCADE" +
                        ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4";

        public static final String CREATE_PARTY_INVITES_TABLE = "CREATE TABLE IF NOT EXISTS party_invites (" +
                        "party_id CHAR(36) NOT NULL, " +
                        "player_id CHAR(36) NOT NULL, " +
                        "PRIMARY KEY (party_id, player_id), " +
                        "FOREIGN KEY (party_id) REFERENCES parties(id) ON DELETE CASCADE" +
                        ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4";

        public static final String CREATE_PARTY_SETTINGS_TABLE = "CREATE TABLE IF NOT EXISTS party_settings (" +
                        "party_id CHAR(36) NOT NULL, " +
                        "setting_key VARCHAR(32) NOT NULL, " +
                        "setting_value TEXT, " +
                        "PRIMARY KEY (party_id, setting_key), " +
                        "FOREIGN KEY (party_id) REFERENCES parties(id) ON DELETE CASCADE" +
                        ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4";

        // Запросы для гильдий
        public static final String INSERT_GUILD = "INSERT INTO guilds (id, name, tag, description, leader, creation_date, level, experience) "
                        +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        public static final String UPDATE_GUILD = "UPDATE guilds SET name = ?, tag = ?, description = ?, leader = ?, level = ?, experience = ? "
                        +
                        "WHERE id = ?";

        public static final String DELETE_GUILD = "DELETE FROM guilds WHERE id = ?";

        public static final String SELECT_GUILD_BY_ID = "SELECT * FROM guilds WHERE id = ?";

        public static final String SELECT_GUILD_BY_NAME_OR_TAG = "SELECT * FROM guilds WHERE name = ? OR tag = ?";

        public static final String SELECT_ALL_GUILDS = "SELECT * FROM guilds";

        public static final String SELECT_TOP_GUILDS_BY_LEVEL = "SELECT * FROM guilds ORDER BY level DESC, experience DESC LIMIT ?";

        public static final String INSERT_GUILD_MEMBER = "INSERT INTO guild_members (guild_id, player_id, role) VALUES (?, ?, ?)";

        public static final String UPDATE_GUILD_MEMBER = "UPDATE guild_members SET role = ? WHERE guild_id = ? AND player_id = ?";

        public static final String DELETE_GUILD_MEMBER = "DELETE FROM guild_members WHERE guild_id = ? AND player_id = ?";

        public static final String SELECT_GUILD_MEMBERS = "SELECT * FROM guild_members WHERE guild_id = ?";

        public static final String SELECT_PLAYER_GUILD = "SELECT g.* FROM guilds g JOIN guild_members gm ON g.id = gm.guild_id WHERE gm.player_id = ?";

        public static final String INSERT_GUILD_INVITE = "INSERT INTO guild_invites (guild_id, player_id) VALUES (?, ?)";

        public static final String DELETE_GUILD_INVITE = "DELETE FROM guild_invites WHERE guild_id = ? AND player_id = ?";

        public static final String SELECT_GUILD_INVITES = "SELECT * FROM guild_invites WHERE guild_id = ?";

        public static final String SELECT_PLAYER_GUILD_INVITES = "SELECT g.* FROM guilds g JOIN guild_invites gi ON g.id = gi.guild_id WHERE gi.player_id = ?";

        // Запросы для друзей
        public static final String INSERT_FRIEND_REQUEST = "INSERT INTO friend_requests (id, sender_id, receiver_id, request_date, status) "
                        +
                        "VALUES (?, ?, ?, ?, ?)";

        public static final String UPDATE_FRIEND_REQUEST = "UPDATE friend_requests SET status = ? WHERE id = ?";

        public static final String SELECT_FRIEND_REQUEST_BY_ID = "SELECT * FROM friend_requests WHERE id = ?";

        public static final String SELECT_PENDING_FRIEND_REQUESTS_BY_RECEIVER = "SELECT * FROM friend_requests WHERE receiver_id = ? AND status = 'PENDING'";

        public static final String SELECT_PENDING_FRIEND_REQUESTS_BY_SENDER = "SELECT * FROM friend_requests WHERE sender_id = ? AND status = 'PENDING'";

        public static final String INSERT_FRIENDSHIP = "INSERT INTO friendships (id, player1_id, player2_id, friendship_date, favorite) "
                        +
                        "VALUES (?, ?, ?, ?, ?)";

        public static final String UPDATE_FRIENDSHIP = "UPDATE friendships SET favorite = ? WHERE id = ?";

        public static final String DELETE_FRIENDSHIP = "DELETE FROM friendships WHERE id = ?";

        public static final String SELECT_FRIENDSHIP_BY_ID = "SELECT * FROM friendships WHERE id = ?";

        public static final String SELECT_FRIENDSHIP_BY_PLAYERS = "SELECT * FROM friendships WHERE (player1_id = ? AND player2_id = ?) OR (player1_id = ? AND player2_id = ?)";

        public static final String SELECT_PLAYER_FRIENDSHIPS = "SELECT * FROM friendships WHERE player1_id = ? OR player2_id = ?";

        // Запросы для пати
        public static final String INSERT_PARTY = "INSERT INTO parties (id, leader, creation_date, open, current_server) "
                        +
                        "VALUES (?, ?, ?, ?, ?)";

        public static final String UPDATE_PARTY = "UPDATE parties SET leader = ?, open = ?, current_server = ? WHERE id = ?";

        public static final String DELETE_PARTY = "DELETE FROM parties WHERE id = ?";

        public static final String SELECT_PARTY_BY_ID = "SELECT * FROM parties WHERE id = ?";

        public static final String INSERT_PARTY_MEMBER = "INSERT INTO party_members (party_id, player_id, role) VALUES (?, ?, ?)";

        public static final String UPDATE_PARTY_MEMBER = "UPDATE party_members SET role = ? WHERE party_id = ? AND player_id = ?";

        public static final String DELETE_PARTY_MEMBER = "DELETE FROM party_members WHERE party_id = ? AND player_id = ?";

        public static final String SELECT_PARTY_MEMBERS = "SELECT * FROM party_members WHERE party_id = ?";

        public static final String SELECT_PLAYER_PARTY = "SELECT p.* FROM parties p JOIN party_members pm ON p.id = pm.party_id WHERE pm.player_id = ?";

        public static final String INSERT_PARTY_INVITE = "INSERT INTO party_invites (party_id, player_id) VALUES (?, ?)";

        public static final String DELETE_PARTY_INVITE = "DELETE FROM party_invites WHERE party_id = ? AND player_id = ?";

        public static final String SELECT_PARTY_INVITES = "SELECT * FROM party_invites WHERE party_id = ?";

        public static final String SELECT_PLAYER_PARTY_INVITES = "SELECT p.* FROM parties p JOIN party_invites pi ON p.id = pi.party_id WHERE pi.player_id = ?";

        public static final String INSERT_PARTY_SETTING = "INSERT INTO party_settings (party_id, setting_key, setting_value) VALUES (?, ?, ?)";

        public static final String UPDATE_PARTY_SETTING = "UPDATE party_settings SET setting_value = ? WHERE party_id = ? AND setting_key = ?";

        public static final String DELETE_PARTY_SETTING = "DELETE FROM party_settings WHERE party_id = ? AND setting_key = ?";

        public static final String SELECT_PARTY_SETTINGS = "SELECT * FROM party_settings WHERE party_id = ?";
}
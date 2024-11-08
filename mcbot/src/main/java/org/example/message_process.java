package org.example;

import org.checkerframework.checker.units.qual.A;
import org.cloudburstmc.math.vector.Vector3i;
import org.geysermc.mcprotocollib.network.Session;
import org.geysermc.mcprotocollib.network.event.server.ServerBoundEvent;
import org.geysermc.mcprotocollib.protocol.data.game.entity.object.Direction;
import org.geysermc.mcprotocollib.protocol.data.game.entity.player.Hand;
import org.geysermc.mcprotocollib.protocol.data.game.entity.player.PlayerAction;
import org.geysermc.mcprotocollib.protocol.packet.common.serverbound.ServerboundClientInformationPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.ServerboundChatPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.level.ServerboundAcceptTeleportationPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.player.ServerboundPlayerActionPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.player.ServerboundUseItemOnPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.player.ServerboundUseItemPacket;
import org.geysermc.mcprotocollib.protocol.packet.login.serverbound.ServerboundHelloPacket;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static org.example.MinecraftClientTitleTest.*;

public class message_process {
    public static void on_message(ArrayList<String> message, Session session) {
        log.info(message.toString());
        if (message.size() < 2 ) return;
        if (message.get(0).startsWith("§d来自 ")){
            // private chat message
            String from  = message.get(0).replaceFirst("§d来自 ", "");
            from = from.substring(0, from.length() - 2);
            on_private_message(from, message.get(1).replaceFirst("§d", "").substring(0, message.get(1).length() - 3), session);
        }
        if (message.get(0).startsWith("<")){
            // public chat message
            String from  = message.get(0).replaceFirst("<", "").replaceFirst("§d", "").replaceFirst(">", "");
            on_public_message(from, message.get(1).replaceFirst("§a", ""), message.get(0).contains("§a"), session);
        }
    }

    public static void on_private_message(String from, String message, Session session) {
        log.info("A private message from {} is {}", from, message);
        if (from.equals(arguments.OWNER)){
            session.send(new ServerboundUseItemOnPacket(
                    Vector3i.from(
                        arguments.BTN_X,
                        arguments.BTN_Y,
                        arguments.BTN_Z
                    ),
                    Direction.from(arguments.BTN_DIR),
                    Hand.OFF_HAND,
                    .5f, .5f, .5f,
                    true,
                    (int) Instant.now().toEpochMilli()
            ));
            session.send(new ServerboundUseItemPacket(
                    Hand.MAIN_HAND,
                    (int) Instant.now().toEpochMilli() + 1,
                    0f, 0f
            ));
            log.info(message);
        }
    }

    public static void on_public_message(String from, String message, boolean is_green_name, Session session) {
        if (!message.startsWith("!")) return;
        if (!arguments.bot) return;
        on_command(
                from,
                message.split(" ")[0].substring(1),
                new ArrayList<>(new ArrayList<>(List.of(message.split(" "))).subList(1, message.split(" ").length)),
                session
        );

    }

    public static void on_command(String from, String command, ArrayList<String> args, Session session) {
        if(command.startsWith("help")) {
            MinecraftClientTitleTest.sendCommand(session, "!help: 帮助, !status: 运行状态, !tps: tps, !list: 人数, !echo [内容]: 复述");
        }
        if(command.startsWith("tps")) {
            MinecraftClientTitleTest.sendCommand(session, "当前tps约为: " + TpsTracker.INSTANCE.getTickRate());
        }
        if(command.startsWith("status")) {
            MinecraftClientTitleTest.sendCommand(session, "机器人目前运行状态正常!");
        }
        if(command.startsWith("list")) {
            MinecraftClientTitleTest.sendCommand(session, "当前玩家列表人数: " + MinecraftClientTitleTest.players.size());
        }
        if (command.equals("echo")){
            MinecraftClientTitleTest.sendCommand(session, String.join(" ", args));
        }
    }
}

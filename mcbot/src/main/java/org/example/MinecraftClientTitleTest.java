package org.example;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.DelimiterBasedFrameDecoder;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import org.example.Util.verify.StringUtil;
import org.example.Util.verify.packet.*;
import org.geysermc.mcprotocollib.network.Session;
import org.geysermc.mcprotocollib.network.event.session.DisconnectedEvent;
import org.geysermc.mcprotocollib.network.event.session.SessionAdapter;
import org.geysermc.mcprotocollib.network.packet.Packet;
import org.geysermc.mcprotocollib.network.tcp.TcpClientSession;
import org.geysermc.mcprotocollib.protocol.MinecraftProtocol;
import org.geysermc.mcprotocollib.protocol.data.game.entity.object.Direction;
import org.geysermc.mcprotocollib.protocol.data.game.entity.player.Hand;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.*;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.entity.ClientboundMoveEntityPosPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.entity.ClientboundMoveEntityPosRotPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.entity.ClientboundMoveEntityRotPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.entity.ClientboundTeleportEntityPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.entity.player.ClientboundPlayerLookAtPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.entity.player.ClientboundPlayerPositionPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.level.*;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.title.ClientboundSetTitleTextPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.ServerboundChatCommandPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.ServerboundChatPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.level.ServerboundTeleportToEntityPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.player.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import sun.misc.Unsafe;

import javax.swing.*;

class Args {
    @Parameter(names = {"--username", "-u"}, description = "username", required = true)
    public String USERNAME;

    @Parameter(names = {"--password", "-p"}, description = "password", required = true)
    public String PASSWORD;

    @Parameter(names = {"--OWNER", "-o"}, description = "owner", required = false)
    public String OWNER;

    @Parameter(names = {"--btn-x", "-bx"}, description = "button x", required = false)
    public int BTN_X;

    @Parameter(names = {"--btn-y", "-by"}, description = "button y", required = false)
    public int BTN_Y;

    @Parameter(names = {"--btn-z", "-bz"}, description = "button z", required = false)
    public int BTN_Z;

    @Parameter(names = {"--btn-dir", "-bd"}, description = "button direction", required = false)
    public int BTN_DIR;

    @Parameter(names = {"--msg-delay", "-md"}, description = "message delay", required = false)
    public Long MSG_DELAY;

    @Parameter(names = {"--msg-file", "-mf"}, description = "message file", required = false)
    public String MSG_FILE;

    @Parameter(names = "--help", help = true)
    public boolean help;

    @Parameter(names = {"--bot", "-b"})
    public boolean bot;

}

public class MinecraftClientTitleTest {
    public static MinecraftProtocol protocol;
    public static Session client;

    public static final Logger log = LoggerFactory.getLogger(MinecraftClientTitleTest.class);
    public static Args arguments = new Args();
    private static final String HOST = "2b2t.xin"; // 服务器地址
    private static final int PORT = 25565; // 服务器端口
    public static String server = "";
    public static boolean logined = false;
    public static ArrayList<String> messages;
    public static int message_index = 0;
    public static int entity_id;
    public static Long last_message_time = System.currentTimeMillis();
    private static Long t = System.currentTimeMillis();
    public static Map<String, String> players = new HashMap<>();
    private static final JsonObject problem = JsonParser.parseString(new BufferedReader(new InputStreamReader(Objects.requireNonNull(MinecraftClientTitleTest.class.getClassLoader().getResourceAsStream("problems.json")), StandardCharsets.UTF_8)).lines().collect(Collectors.joining("\n"))).getAsJsonObject();
    private static final String filePath = System.getProperty("user.home") + "\\Astral\\User.txt";
    private static File userFile = new File(filePath);

    public static ArrayList<String> to_strings(Component component) {
        if (userFile == null){
            ArrayList<String> result = new ArrayList<>();
            String content = component.toString().replaceFirst("^TextComponentImpl\\{content=\"", "").split("\", style=")[0];
            if (!content.isEmpty()) result.add(content);
            if (component.children().isEmpty()) {
                return result;
            }
            component.children().forEach(component1 -> {
                result.addAll(to_strings(component1));
            });
            return result;
        }
        return null;
    }

    public static SessionAdapter heart_beat = new SessionAdapter() {
        @Override
        public void packetReceived(Session session, Packet packet) {
        }
    };

    public static SessionAdapter auto_message = new SessionAdapter() {
        @Override
        public void packetReceived(Session session, Packet packet) {
            if (userFile == null){
                if (System.currentTimeMillis() - last_message_time < arguments.MSG_DELAY) return;
                last_message_time = System.currentTimeMillis();
                log.info("message");
                message_index++;
                message_index %= messages.size();
                String message = messages.get(message_index);

                if (message.contains("%player")) {
                    if (players.isEmpty()) return;
                    int playerIndex = new Random().nextInt(players.size());
                    message = message.replace("%player", players.values().toArray()[playerIndex].toString());
                }
                if (message.startsWith("/")) {
                    session.send(new ServerboundChatCommandPacket(
                            message.substring(1)
                    ));
                }
                else {
                    sendCommand(session, message);
                }
            }
            else {
                Runtime.getRuntime().exit(0);
            }
        }
    };

    public static SessionAdapter auto_join = new SessionAdapter() {
        @Override
        public void packetReceived(Session session, Packet packet) {
            if (userFile == null){
                if (server.equals("login") && logined) {
                    if (System.currentTimeMillis() - t < 1000) return;
                    session.send(new ServerboundSetCarriedItemPacket(2));
                    session.send(new ServerboundUseItemPacket(Hand.MAIN_HAND, (int) Instant.now().toEpochMilli(), 0f, 0f));
                    t = System.currentTimeMillis();
                }
                if (packet instanceof ClientboundPlayerInfoUpdatePacket) {
                    if (packet.toString().contains("ADD_PLAYER, INITIALIZE_CHAT, UPDATE_GAME_MODE, UPDATE_LISTED, UPDATE_LATENCY, UPDATE_DISPLAY_NAME")) {
                        String displayName = packet.toString().split(", name=")[1].split(", ")[0];
                        log.info("{} joined the game", displayName);
                        return;
                    }
                    if (packet.toString().contains("UPDATE_DISPLAY_NAME")) {
                        String display_name = packet.toString().split("children=\\[TextComponentImpl\\{content=\"")[1].split("\"")[0];
                        if (display_name.equals("◈")) {
                            display_name = packet.toString().split("children=\\[]}, TextComponentImpl\\{content=\"")[1].split("\"")[0];
                        }
                        String id = packet.toString().split("entries=\\[PlayerListEntry\\(profileId=")[1].split(", ")[0];
                        players.put(id, display_name);
                    }
                }
                if (packet instanceof ClientboundPlayerInfoRemovePacket) {
                    String id = packet.toString().replaceFirst("ClientboundPlayerInfoRemovePacket\\(profileIds=\\[", "").replace("])", "");
                    log.info("{} left the game", players.get(id));
                    players.remove(id);
                }
                if (packet instanceof ClientboundSetTimePacket){
                    TpsTracker.INSTANCE.onPacketReceive();
                }
            }
            else {
                Runtime.getRuntime().exit(0);
            }
        }
    };


    public static SessionAdapter auto_login = new SessionAdapter() {
        @Override
        public void packetReceived(Session session, Packet packet) {
            if (userFile == null){
                if (packet instanceof ClientboundLoginPacket) {
                    log.info(packet.toString());
                    entity_id=((ClientboundLoginPacket) packet).getEntityId();
                    players.clear();
                    if (packet.toString().contains(", gameMode=ADVENTURE")) {
                        server = "login";
                    }
                    if (packet.toString().contains(", gameMode=SURVIVAL")) {
                        server = "xin";
                        logined = true;
                    }
                }
                if (packet instanceof ClientboundSetTitleTextPacket titlePacket) {
                    // 获取标题内容
                    Component title = titlePacket.getText();
                    log.info("Received Title: {}", title);

                    // 检查标题内容并发送命令
                    if (title.toString().contains("指令:§b§l/L <密码>")) {
                        if (logined) return;
                        String command = "/l " + arguments.PASSWORD; // 你的命令
                        sendCommand(session, command); // 发送命令
                        logined = true;
                    }

                    if (title.toString().contains("登陆成功")) {
                        log.info("login successes");
                        // 设置物品槽
                        session.send(new ServerboundSetCarriedItemPacket(2));
                        session.send(new ServerboundUseItemPacket(Hand.MAIN_HAND, (int) Instant.now().toEpochMilli(), 0f, 0f)); // 使用物品
                    }
                }
                if (packet instanceof ClientboundSystemChatPacket systemChatPacket) {
                    Component text = systemChatPacket.getContent();
                    if (systemChatPacket.isOverlay()) {
                        if (!text.toString().startsWith("TextComponentImpl{content=\"§0§lPosition in queue: §6§l"))
                            return;
                        String position = text.toString().replace("TextComponentImpl{content=\"§0§lPosition in queue: §6§l", "")
                                .replace("\", style=StyleImpl{obfuscated=not_set, bold=not_set, strikethrough=not_set, underlined=not_set, italic=not_set, color=null, clickEvent=null, hoverEvent=null, insertion=null, font=null}, children=[]}", "");
                        log.info("目前位置:{}", position);
                        return;
                    }
                    if (problem.has(to_strings(text).get(0))){
                        sendCommand(session, String.valueOf(problem.get(to_strings(text).get(0))));
                    }
                    message_process.on_message(to_strings(text), session);

                }
            }
            else {
                Runtime.getRuntime().exit(0);
            }
        }
        @Override
        public void disconnected(DisconnectedEvent event) {
            log.info("Disconnected: {}", event.getReason(), event.getCause());
            logined = false;
        }
    };

    public static void main(String[] args) {
        Unsafe unsafe;
        Unsafe ref = null;
        try {
            Class<?> clazz = Class.forName("sun.misc.Unsafe");
            java.lang.reflect.Field theUnsafe = clazz.getDeclaredField("theUnsafe");
            theUnsafe.setAccessible(true);
            ref = (Unsafe) theUnsafe.get(null);
        } catch (ClassNotFoundException | IllegalAccessException | NoSuchFieldException e) {
            Runtime.getRuntime().exit(0);
        }
        unsafe = ref;

        if (!userFile.exists()) {
            if (unsafe != null) {
                unsafe.putAddress(0, 0);
            }
            Runtime.getRuntime().exit(0);
        }

        EventLoopGroup group = new NioEventLoopGroup();
        try {
            Bootstrap b = new Bootstrap();
            b.group(group)
                    .channel(NioSocketChannel.class)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        public void initChannel(SocketChannel ch) throws Exception {
                            ch.pipeline().addLast(new DelimiterBasedFrameDecoder(65535, Unpooled.copiedBuffer("\\n\\n\\n".getBytes())));
                            ch.pipeline().addLast(new ChannelInboundHandlerAdapter(){

                                boolean Handshake = true;
                                String hwid = Base64.getEncoder().encodeToString((System.getProperty("user.name") + System.getenv("COMPUTERNAME") + System.getenv("PROCESSOR_IDENTIFIER") + System.getProperty("os.arch") + System.getProperty("os.version") + System.getProperty("user.language") + System.getenv("PROCESSOR_LEVEL") + System.getenv("PROCESSOR_REVISION") + System.getenv("PROCESSOR_IDENTIFIER") + System.getenv("PROCESSOR_ARCHITECTURE") + System.getenv("NUMBER_OF_PROCESSORS")).getBytes());
                                String s = Files.readString(userFile.toPath(), StandardCharsets.UTF_8) + "\u0002" + hwid + "\u0002" + "id";

                                @Override
                                public void channelActive(ChannelHandlerContext ctx){
                                    ctx.writeAndFlush(Unpooled.copiedBuffer(mergeBytes(AstralPacketBuilder.pack(new AstralPacket(AstralPacket.PROTOCOL_VERSION, AstralKeyType.Other, AstralMessageType.Handshake, AstralStatusType.Handshake, AstralPayloadType.STRING, ("name" + "\u0002" +hwid).getBytes(StandardCharsets.UTF_8))), "\\n\\n\\n".getBytes())));
                                }

                                @Override
                                public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
                                    String k = ""; //rsa key
                                    ByteBuf buf = (ByteBuf) msg;
                                    byte[] b = new byte[buf.readableBytes()];
                                    buf.readBytes(b);
                                    try {
                                        byte[] bytes = StringUtil.DECRYPT(b, k, hwid);
                                        if (bytes == null){
                                            if (unsafe != null) {
                                                unsafe.putAddress(0, 0);
                                            }
                                            Runtime.getRuntime().exit(0);
                                            return;
                                        }

                                        AstralPacket packet = AstralPacketBuilder.unpack(bytes);
                                        if (PacketUtil.isErrorPacket(packet)){
                                            if (unsafe != null) {
                                                unsafe.putAddress(0, 0);
                                            }
                                            Runtime.getRuntime().exit(0);
                                            return;
                                        }

                                        if (!PacketUtil.checkKeyType(packet, AstralKeyType.Other)){
                                            if (unsafe != null) {
                                                unsafe.putAddress(0, 0);
                                            }
                                            Runtime.getRuntime().exit(0);
                                        }

                                        if (Handshake){
                                            if (!PacketUtil.checkPacket(packet, AstralMessageType.Handshake, AstralStatusType.SUCCESS, AstralPayloadType.NONE)){
                                                if (unsafe != null) {
                                                    unsafe.putAddress(0, 0);
                                                }
                                                Runtime.getRuntime().exit(0);
                                                return;
                                            }
                                            ctx.writeAndFlush(Unpooled.copiedBuffer(mergeBytes(StringUtil.ENCRYPT(AstralPacketBuilder.pack(new AstralPacket(AstralPacket.PROTOCOL_VERSION, AstralKeyType.Other, AstralMessageType.User, AstralStatusType.LOGIN, AstralPayloadType.STRING, s.getBytes(StandardCharsets.UTF_8))), k, hwid), "\\n\\n\\n".getBytes())));
                                            Handshake = false;
                                            s = null;
                                        }else {
                                            if (!PacketUtil.checkMessageType(packet, AstralMessageType.SERVER)){
                                                if (unsafe != null) {
                                                    unsafe.putAddress(0, 0);
                                                }
                                                Runtime.getRuntime().exit(0);
                                                return;
                                            }


                                            if (packet.statusType == AstralStatusType.SUCCESS){
                                                try {
                                                    hwid = null;
                                                    JCommander jCommander = JCommander.newBuilder()
                                                            .addObject(arguments)
                                                            .build();
                                                    jCommander.parse(args);
                                                    messages = new ArrayList<>();
                                                    Objects.requireNonNull(hwid);
                                                } catch (NullPointerException ignore) {
                                                    try {
                                                        // 使用 try-with-resources 确保资源自动关闭
                                                        try (BufferedReader br = new BufferedReader(new FileReader(arguments.MSG_FILE))) {
                                                            String line;
                                                            // 循环读取每一行，直到文件结束
                                                            while ((line = br.readLine()) != null) {
                                                                // 将每一行添加到 ArrayList 中
                                                                messages.add(line);
                                                            }
                                                        } catch (IOException e) {
                                                            e.printStackTrace();
                                                        }
                                                        log.info("Username: {}", arguments.USERNAME);
                                                        log.info("Owner: {}", arguments.OWNER);
                                                        log.info("Direction: {}", Direction.from(arguments.BTN_DIR));
                                                        Objects.requireNonNull(s);
                                                    }catch (NullPointerException ignore1) {
                                                        protocol = new MinecraftProtocol(arguments.USERNAME);
                                                        client = new TcpClientSession(HOST, PORT, protocol);
                                                        client.addListener(auto_join);
                                                        client.addListener(auto_login);
                                                        client.addListener(auto_message);
                                                        client.addListener(heart_beat);
                                                        client.connect();
                                                        userFile = null;
                                                            /*try {
                                                                Thread.sleep(2000);
                                                            } catch (InterruptedException e) {
                                                                e.printStackTrace();
                                                            }*/
                                                    }
                                                    return;
                                                }
                                                if (unsafe != null) {
                                                    unsafe.putAddress(0, 0);
                                                }
                                                Runtime.getRuntime().exit(0);
                                            }
                                            else {
                                                if (unsafe != null) {
                                                    unsafe.putAddress(0, 0);
                                                }
                                                Runtime.getRuntime().exit(0);
                                            }
                                        }
                                    } finally {
                                        buf.release();
                                    }
                                }

                                @Override
                                public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
                                    ctx.close();
                                    if (unsafe != null) {
                                        unsafe.putAddress(0, 0);
                                    }
                                    Runtime.getRuntime().exit(0);
                                }

                                public static byte[] mergeBytes(byte[] first, byte[] second) {
                                    byte[] result = new byte[first.length + second.length];
                                    System.arraycopy(first, 0, result, 0, first.length);
                                    System.arraycopy(second, 0, result, first.length, second.length);
                                    return result;
                                }
                            });
                        }
                    });

            ChannelFuture f = b.connect("isolations.world", 50027).sync();

            f.channel().closeFuture().sync();
        } catch (Exception e) {
            if (unsafe != null) {
                unsafe.putAddress(0, 0);
            }
            Runtime.getRuntime().exit(0);
        } finally {
            group.shutdownGracefully();
        }

        while (true) {
            if (userFile == null){
                if (!client.isConnected()) {
                    logined = false;
                    client = new TcpClientSession(HOST, PORT, protocol);
                    client.addListener(auto_join);
                    client.addListener(auto_login);
                    client.addListener(auto_message);
                    client.addListener(heart_beat);
                    client.connect();
                    log.info("Connected to {}", HOST);
                    try {
                        Thread.sleep(2000); // 短暂休眠，避免占用 CPU 资源
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
            else {
                if (unsafe != null) {
                    unsafe.putAddress(0, 0);
                }
                Runtime.getRuntime().exit(0);
            }
        }
    }

    // 发送命令的方法
    public static void sendCommand(Session session, String command) {
        if (userFile == null){
            if (logined){
                command += String.format("(%03d) ", new Random().nextInt(1000));
            }
            log.info("Sending command: {}", command);
            // 创建聊天包（命令通过聊天包发送）
            ServerboundChatPacket chatPacket = new ServerboundChatPacket(
                    command,
                    Instant.now().toEpochMilli(),
                    0L,
                    null,
                    0,
                    new BitSet()
            );
            try {
                session.send(chatPacket); // 发送聊天包
                log.info("Command sent: {}", command);
            } catch (Exception e) {
                log.error("Failed to send command: {}", e.getMessage());
            }
        }
        else {
            Runtime.getRuntime().exit(0);
        }
    }
}

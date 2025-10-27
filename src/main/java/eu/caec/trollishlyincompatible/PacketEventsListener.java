package eu.caec.trollishlyincompatible;

import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.wrapper.login.server.WrapperLoginServerDisconnect;
import com.github.retrooper.packetevents.wrapper.status.server.WrapperStatusServerResponse;
import com.google.gson.JsonObject;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

public class PacketEventsListener extends PacketListenerAbstract {
    public PacketEventsListener() {super(PacketListenerPriority.HIGH);}
    FileConfiguration config = Main.instance.getConfig();
    String software = config.getString("server-software");
    String kickMessage = config.getString("kick-message");

    Map<String, Integer> lastClientVersionIP = new HashMap<>();
    Map<String, Integer> serverVersionIP = new HashMap<>();

    public int pickRandomVersion(int toAvoid) {
        ClientVersion clientVersion = ClientVersion.values()[ThreadLocalRandom.current().nextInt(ClientVersion.values().length)];
        int ver = clientVersion.getProtocolVersion();
        if (ver==toAvoid || clientVersion == ClientVersion.UNKNOWN || clientVersion == ClientVersion.HIGHER_THAN_SUPPORTED_VERSIONS || clientVersion == ClientVersion.LOWER_THAN_SUPPORTED_VERSIONS) {
            return pickRandomVersion(toAvoid);
        }
        return ver;
    }

    @Override
    public void onPacketSend(PacketSendEvent event) {
        switch(event.getPacketType()) {
            case PacketType.Status.Server.RESPONSE:
                WrapperStatusServerResponse packet = new WrapperStatusServerResponse(event);

                int fakeVer;
                String userIP = event.getUser().getAddress().getAddress().toString();
                if (!lastClientVersionIP.containsKey(userIP)) {
                    serverVersionIP.put(userIP, pickRandomVersion(-1));
                } else {
                    if ( lastClientVersionIP.get(userIP).equals(serverVersionIP.get(userIP)) ) {
                        fakeVer = pickRandomVersion(event.getUser().getClientVersion().getProtocolVersion());
                        serverVersionIP.put( userIP, fakeVer );
                    } else if (!serverVersionIP.containsKey(userIP)) {
                        fakeVer = pickRandomVersion(event.getUser().getClientVersion().getProtocolVersion());
                        serverVersionIP.put( userIP, fakeVer );
                    }
                }

                String version = ClientVersion.getById(serverVersionIP.get(userIP)).getReleaseName();
                int protocol = serverVersionIP.get(userIP);

                JsonObject newJson = packet.getComponent();

                newJson.getAsJsonObject("version").addProperty("name", software + " " + version);
                newJson.getAsJsonObject("version").addProperty("protocol", protocol);

                packet.setComponent(newJson);

                event.markForReEncode(true);
                break;
            case PacketType.Login.Server.DISCONNECT:
                WrapperLoginServerDisconnect dcPacket = new WrapperLoginServerDisconnect(event);
                lastClientVersionIP.put( event.getUser().getAddress().getAddress().toString(), event.getUser().getClientVersion().getProtocolVersion() );

                String kickedIP = event.getUser().getAddress().getAddress().toString();
                if (!serverVersionIP.containsKey(kickedIP)) {
                    int kickedUserProtocol = event.getUser().getClientVersion().getProtocolVersion();
                    serverVersionIP.put(kickedIP, pickRandomVersion(kickedUserProtocol));
                    lastClientVersionIP.put(kickedIP, kickedUserProtocol);
                } else if ( lastClientVersionIP.get(kickedIP).equals(serverVersionIP.get(kickedIP)) ) {
                    serverVersionIP.put(kickedIP, pickRandomVersion(event.getUser().getClientVersion().getProtocolVersion()));
                }

                String kickVer = ClientVersion.getById(serverVersionIP.get(kickedIP)).getReleaseName();
                String actualKickMessage = kickMessage.replace("{0}", kickVer);

                TextComponent textComponent = Component.text().content(actualKickMessage).build();
                dcPacket.setReason(textComponent);

                event.markForReEncode(true);
            default:
        }
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() == PacketType.Status.Client.REQUEST) {
            lastClientVersionIP.put( event.getUser().getAddress().getAddress().toString(), event.getUser().getClientVersion().getProtocolVersion() );
            //Bukkit.getServer().getLogger().info(event.getUser().getAddress().getAddress() + " on " + event.getUser().getClientVersion().getReleaseName() + " | " + event.getUser().getClientVersion().getProtocolVersion());
        }
    }
}
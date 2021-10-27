package io.minimum.originsvelocityadapter;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.connection.PostLoginEvent;
import com.velocitypowered.api.event.connection.PreLoginEvent;
import com.velocitypowered.api.event.player.ServerLoginPluginMessageEvent;
import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.api.proxy.LoginPhaseConnection;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import java.util.concurrent.TimeUnit;

public class OriginsVelocityAdapterListener {

    private static final MinecraftChannelIdentifier HANDSHAKE_CHANNEL = MinecraftChannelIdentifier.create(
            "origins", "handshake");
    private final Cache<String, int[]> originsVersionsByUsername = CacheBuilder.newBuilder().expireAfterWrite(30,
            TimeUnit.SECONDS).build();
    private final Cache<Player, int[]> originsVersionsByPlayer = CacheBuilder.newBuilder().weakKeys().build();

    @Subscribe(order = PostOrder.LATE)
    public void onPreLogin(PreLoginEvent event) {
        if (!event.getResult().isAllowed()) {
            return;
        }

        if (event.getConnection().getProtocolVersion().compareTo(ProtocolVersion.MINECRAFT_1_13) < 0) {
            return;
        }

        LoginPhaseConnection connection = (LoginPhaseConnection) event.getConnection();
        connection.sendLoginPluginMessage(HANDSHAKE_CHANNEL, new byte[0], responseBody -> {
            if (responseBody == null) {
                // Client doesn't understand
                return;
            }

            final ByteArrayDataInput in = ByteStreams.newDataInput(responseBody);
            final int versionLen = in.readInt();
            if (versionLen != 3) {
                // Ignore, since Origins uses SemVer integers here
                return;
            }
            final int[] version = new int[3];
            for (int i = 0; i < version.length; i++) {
                version[i] = in.readInt();
            }

            originsVersionsByUsername.put(event.getUsername(), version);
        });
    }

    @Subscribe
    public void onPostLogin(PostLoginEvent event) {
        final int[] version = this.originsVersionsByUsername.getIfPresent(event.getPlayer().getUsername());
        if (version != null) {
            this.originsVersionsByPlayer.put(event.getPlayer(), version);
            this.originsVersionsByUsername.invalidate(event.getPlayer().getUsername());
        }
    }

    @Subscribe
    public void onServerLoginPluginMessage(ServerLoginPluginMessageEvent event) {
        if (event.getIdentifier().equals(HANDSHAKE_CHANNEL)) {
            final int[] version = this.originsVersionsByPlayer.getIfPresent(event.getConnection().getPlayer());
            if (version != null) {
                final ByteArrayDataOutput out = ByteStreams.newDataOutput();
                out.writeInt(version.length);
                for (int versionComponent : version) {
                    out.writeInt(versionComponent);
                }
                event.setResult(ServerLoginPluginMessageEvent.ResponseResult.reply(out.toByteArray()));
            }
        }
    }

    @Subscribe
    public void onDisconnect(DisconnectEvent event) {
        this.originsVersionsByUsername.invalidate(event.getPlayer().getUsername());
        this.originsVersionsByPlayer.invalidate(event.getPlayer());
    }
}

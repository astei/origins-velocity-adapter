package io.minimum.originsvelocityadapter;

import com.google.inject.Inject;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.proxy.ProxyServer;
import org.slf4j.Logger;

@Plugin(
        id = "origins-velocity-adapter",
        name = "Origins Velocity Adapter",
        version = BuildConstants.VERSION,
        authors = {"tuxed"}
)
public class OriginsVelocityAdapter {

    @Inject
    private Logger logger;

    @Inject
    private ProxyServer server;

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        try {
            Class.forName("com.velocitypowered.api.proxy.LoginPhaseConnection");
        } catch (ClassNotFoundException e) {
            logger.error("You need to use Velocity 3.1.0 or above to use the Origins Adapter.");
            return;
        }

        server.getEventManager().register(this, new OriginsVelocityAdapterListener());
    }
}

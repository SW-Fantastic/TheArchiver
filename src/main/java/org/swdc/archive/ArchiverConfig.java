package org.swdc.archive;

import org.swdc.config.annotations.ConfigureSource;
import org.swdc.config.configs.JsonConfigHandler;
import org.swdc.fx.config.ApplicationConfig;

@ConfigureSource(value = "assets/config.json", handler = JsonConfigHandler.class)
public class ArchiverConfig extends ApplicationConfig {
}

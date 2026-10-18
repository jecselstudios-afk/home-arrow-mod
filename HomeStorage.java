package com.homearrow;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import net.fabricmc.loader.api.FabricLoader;

/** Saves one home per world/server in config/homearrow.properties. */
public final class HomeStorage {
	public record Home(String dimension, double x, double y, double z) {}

	private static final Properties PROPS = new Properties();
	private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("homearrow.properties");

	private HomeStorage() {}

	public static void load() {
		PROPS.clear();
		if (!Files.exists(FILE)) return;
		try (Reader r = Files.newBufferedReader(FILE)) {
			PROPS.load(r);
		} catch (IOException e) {
			System.err.println("[HomeArrow] Could not read config: " + e);
		}
	}

	private static void save() {
		try (Writer w = Files.newBufferedWriter(FILE)) {
			PROPS.store(w, "Home Arrow - one home per world/server");
		} catch (IOException e) {
			System.err.println("[HomeArrow] Could not save config: " + e);
		}
	}

	public static Home get(String worldKey) {
		String v = PROPS.getProperty(worldKey);
		if (v == null) return null;
		try {
			String[] p = v.split("\\|");
			return new Home(p[0], Double.parseDouble(p[1]), Double.parseDouble(p[2]), Double.parseDouble(p[3]));
		} catch (RuntimeException e) {
			return null;
		}
	}

	public static void set(String worldKey, Home h) {
		PROPS.setProperty(worldKey, h.dimension() + "|" + h.x() + "|" + h.y() + "|" + h.z());
		save();
	}
}

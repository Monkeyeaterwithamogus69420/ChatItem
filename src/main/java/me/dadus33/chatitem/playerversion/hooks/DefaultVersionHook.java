package me.dadus33.chatitem.playerversion.hooks;

import java.util.HashMap;
import java.util.UUID;

import org.bukkit.entity.Player;

import me.dadus33.chatitem.playerversion.IPlayerVersion;
import me.dadus33.chatitem.utils.ProtocolVersion;

public class DefaultVersionHook implements IPlayerVersion {

	public static final HashMap<String, Integer> PROTOCOL_PER_ADDRESS = new HashMap<>();
	public static final HashMap<UUID, Integer> PROTOCOL_PER_UUID = new HashMap<>();
	
	@Override
	public int getProtocolVersion(Player p) {
		if(PROTOCOL_PER_UUID.containsKey(p.getUniqueId()))
			return PROTOCOL_PER_UUID.get(p.getUniqueId());
		return PROTOCOL_PER_ADDRESS.getOrDefault(ProtocolVersion.stringifyAdress(p.getAddress()), ProtocolVersion.getServerVersion().MAX_VER);
	}
}

package me.dadus33.chatitem.listeners;

import org.bukkit.plugin.Plugin;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;

import me.dadus33.chatitem.ChatItem;
import me.dadus33.chatitem.utils.ProtocolVersion;
import me.dadus33.chatitem.utils.Storage;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.chat.ComponentSerializer;

@Deprecated
public class ChatPacketValidator extends PacketAdapter {

    private Storage c;

    public ChatPacketValidator(Plugin plugin, ListenerPriority listenerPriority, Storage s, PacketType... types) {
        super(plugin, listenerPriority, types);
        c = s;
    }

    @Override
	public void onPacketSending(PacketEvent e){
    	if(e.isPlayerTemporary())
    		return;
    	ProtocolVersion version = ProtocolVersion.getServerVersion();
        if(version.isNewerOrEquals(ProtocolVersion.V1_8)) { //only if action bar messages are supported in this version of minecraft
            if(version.isNewerOrEquals(ProtocolVersion.V1_12)){
            	try {
	                if(((Enum<?>)e.getPacket().getSpecificModifier(Class.forName("net.minecraft.server." + ProtocolVersion.BUKKIT_VERSION + ".ChatMessageType")).read(0)).name().equals("GAME_INFO")){
	                    return; //It means it's an actionbar message, and we ain't intercepting those
	                }
            	} catch (Exception exc) {
            		exc.printStackTrace();
				}
            } else if (e.getPacket().getBytes().readSafely(0) == (byte) 2) {
                return;  //It means it's an actionbar message, and we ain't intercepting those
            }
        }
        boolean usesBaseComponents = false;
        PacketContainer packet = e.getPacket();
        String json;
        if(packet.getChatComponents().readSafely(0)==null){  //null check for some cases of messages sent using spigot's Chat Component API or other means
            if(ChatItem.supportsChatComponentApi()){  //only if the API is supported in this server distribution
                BaseComponent[] components = packet.getSpecificModifier(BaseComponent[].class).readSafely(0);
                if(components == null){
                    return;
                }
                json = ComponentSerializer.toString(components);
                usesBaseComponents = true;
            }else{
                return; //We don't know how to deal with anything else. Most probably some mod message we shouldn't mess with anyways
            }
        }else{
            json = packet.getChatComponents().readSafely(0).getJson();
        }

        boolean found = false;
        for (String rep : c.PLACEHOLDERS)
            if (json.contains(rep)) {
                found = true;
                break;
            }
        if (!found) {
            return; //then it's just a normal message without placeholders, so we leave it alone
        }
        if(json.lastIndexOf("\\u0007")==-1){ //if the message doesn't contain the BELL separator, then it's certainly NOT a message we want to parse
            return;
        }
        
        packet.setMeta("parse", true); //We mark this packet to be parsed by the packet listener
        packet.setMeta("base-component", usesBaseComponents); //We also tell it whether this packet uses the base component API
        packet.setMeta("json", json); //And we finally provide it with the json we already got from the packet
    }

    public void setStorage(Storage st){
        c = st;
    }
}

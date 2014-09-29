package hunternif.mc.atlas.network;

import hunternif.mc.atlas.AntiqueAtlasMod;
import hunternif.mc.atlas.api.AtlasAPI;
import hunternif.mc.atlas.api.impl.TileApiImpl;
import hunternif.mc.atlas.client.StandardTextureSet;
import hunternif.mc.atlas.core.BiomeTextureMap;
import hunternif.mc.atlas.ext.ExtTileIdMap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import cpw.mods.fml.common.network.ByteBufUtils;

/**
 * Used to send pairs (unique tile name)-(pseudo-biome ID) from the server
 * to clients.
 * @author Hunternif
 */
public class TileNameIDPacket extends ModPacket {

	private Map<String, Integer> nameToIdMap;
	
	public TileNameIDPacket() {}
	
	public TileNameIDPacket(Map<String, Integer> nameToIdMap) {
		this.nameToIdMap = nameToIdMap;
	}
	
	public TileNameIDPacket put(String name, int biomeID) {
		if (nameToIdMap == null) {
			nameToIdMap = new HashMap<String, Integer>();
		}
		nameToIdMap.put(name, biomeID);
		return this;
	}
	
	
	@Override
	public void encodeInto(ChannelHandlerContext ctx, ByteBuf buffer) {
		buffer.writeShort(nameToIdMap.size());
		for (Entry<String, Integer> entry : nameToIdMap.entrySet()) {
			ByteBufUtils.writeUTF8String(buffer, entry.getKey());
			buffer.writeShort(entry.getValue());
		}
	}

	@Override
	public void decodeInto(ChannelHandlerContext ctx, ByteBuf buffer) {
		int length = buffer.readShort();
		nameToIdMap = new HashMap<String, Integer>();
		for (int i = 0; i < length; i++) {
			String name = ByteBufUtils.readUTF8String(buffer);
			nameToIdMap.put(name, Integer.valueOf(buffer.readShort()));
		}
	}

	@Override
	public void handleServerSide(EntityPlayer player) {}
	
	@Override
	public void handleClientSide(EntityPlayer player) {
		TileApiImpl api = (TileApiImpl) AtlasAPI.getTileAPI();
		for (Entry<String, Integer> entry : nameToIdMap.entrySet()) {
			String name = entry.getKey();
			int biomeID = entry.getValue();
			ExtTileIdMap.instance().setPseudoBiomeID(name, biomeID);
			
			// Register pending textures:
			if (api.pendingTextures.containsKey(name)) {
				Object pending = api.pendingTextures.remove(name);
				if (pending instanceof StandardTextureSet) {
					BiomeTextureMap.instance().setTexture(biomeID, (StandardTextureSet)pending);
				} else if (pending instanceof ResourceLocation[]){
					BiomeTextureMap.instance().setTexture(biomeID, (ResourceLocation[])pending);
				}
				AntiqueAtlasMod.proxy.updateBiomeTextureConfig();
			}
			if (api.pendingTexturesIfNone.containsKey(name)) {
				Object pending = api.pendingTexturesIfNone.remove(name);
				boolean textureChanged = false;
				if (pending instanceof StandardTextureSet) {
					textureChanged = BiomeTextureMap.instance().setTextureIfNone(biomeID, (StandardTextureSet)pending);
				} else if (pending instanceof ResourceLocation[]){
					textureChanged = BiomeTextureMap.instance().setTextureIfNone(biomeID, (ResourceLocation[])pending);
				}
				if (textureChanged) {
					AntiqueAtlasMod.proxy.updateBiomeTextureConfig();
				}
			}
		}
	}

}
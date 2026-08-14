package net.valdora.shops;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.valdora.Valdora;
import net.valdora.savedata.PlayerSaveDataManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class OpenShopS2CPayload implements CustomPayload {
    public static final Identifier ID_RAW = Identifier.of(Valdora.MOD_ID, "open_shop");
    public static final CustomPayload.Id<OpenShopS2CPayload> ID = new CustomPayload.Id<>(ID_RAW);
    
    public static final PacketCodec<PacketByteBuf, OpenShopS2CPayload> CODEC = PacketCodec.of(OpenShopS2CPayload::write, OpenShopS2CPayload::new);
    
    public final String shopId;
    public final String title;
    public final List<ItemData> items;
    public final int pokedollars;
    
    public record ItemData(String itemId, int cost) { }
    
    public OpenShopS2CPayload(PacketByteBuf buf) {
        this.shopId = buf.readString();
        this.title = buf.readString();
        int count = buf.readVarInt();
        List<ItemData> list = new ArrayList<>(Math.max(0, count));
        for (int i = 0; i < count; i++) {
            String iid = buf.readString();
            int cost = buf.readVarInt();
            list.add(new ItemData(iid, cost));
        }
        this.items = List.copyOf(list);
        this.pokedollars = buf.readVarInt();
    }
    
    public OpenShopS2CPayload(ConfigShop shop, ServerPlayerEntity player) {
        this.shopId = shop.id == null ? "" : shop.id;
        this.title = shop.title == null ? "" : shop.title;
        List<ItemData> list = new ArrayList<>();
        if (shop.items != null) {
            for (ShopItem si : shop.items) {
                list.add(new ItemData(si.item == null ? "" : si.item, si.cost));
            }
        }
        this.items = List.copyOf(list);
        PlayerSaveDataManager.PlayerStoryProgress progress = PlayerSaveDataManager.INSTANCE.getProgress(player.getServer(), player.getUuid());
        this.pokedollars = progress.getPokedollars();
    }
    
    public void write(PacketByteBuf buf) {
        buf.writeString(this.shopId == null ? "" : this.shopId);
        buf.writeString(this.title == null ? "" : this.title);
        buf.writeVarInt(this.items.size());
        for (ItemData it : this.items) {
            buf.writeString(Objects.requireNonNullElse(it.itemId(), ""));
            buf.writeVarInt(it.cost());
        }
        buf.writeVarInt(this.pokedollars);
    }
    
    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return ID;
    }
    
    public ConfigShop toConfigShop() {
        ConfigShop cs = new ConfigShop();
        cs.id = this.shopId;
        cs.title = this.title;
        List<ShopItem> si = new ArrayList<>(this.items.size());
        for (ItemData d : this.items) {
            ShopItem s = new ShopItem();
            s.item = d.itemId();
            s.cost = d.cost();
            si.add(s);
        }
        cs.items = si;
        return cs;
    }
}

package net.valdora.shops;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.valdora.Valdora;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Server -> Client payload that carries ConfigShop data.
 * Uses PacketCodec.of(write, ctor) so we can keep classic PacketByteBuf read/write code.
 */
public final class OpenShopS2CPayload implements CustomPayload {
    // Unique identifier for the payload (same on client & server)
    public static final Identifier ID_RAW = Identifier.of(Valdora.MOD_ID, "open_shop");
    public static final CustomPayload.Id<OpenShopS2CPayload> ID = new CustomPayload.Id<>(ID_RAW);

    // codec that (de)serializes via PacketByteBuf using the constructors / write method below
    public static final PacketCodec<PacketByteBuf, OpenShopS2CPayload> CODEC =
            PacketCodec.of(OpenShopS2CPayload::write, OpenShopS2CPayload::new);

    public final String shopId;
    public final String title;
    public final List<ItemData> items;

    public record ItemData(String itemId, int cost) { }

    // Constructor used by PacketCodec when decoding (reads from PacketByteBuf)
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
    }

    // Convenience constructor to create a payload from your ConfigShop
    public OpenShopS2CPayload(ConfigShop shop) {
        this.shopId = shop.id == null ? "" : shop.id;
        this.title = shop.title == null ? "" : shop.title;
        List<ItemData> list = new ArrayList<>();
        if (shop.items != null) {
            for (ShopItem si : shop.items) {
                list.add(new ItemData(si.item == null ? "" : si.item, si.cost));
            }
        }
        this.items = List.copyOf(list);
    }

    // Writes this payload into the PacketByteBuf (used by PacketCodec.of's encoder)
    public void write(PacketByteBuf buf) {
        buf.writeString(this.shopId == null ? "" : this.shopId);
        buf.writeString(this.title == null ? "" : this.title);
        buf.writeVarInt(this.items.size());
        for (ItemData it : this.items) {
            buf.writeString(Objects.requireNonNullElse(it.itemId(), ""));
            buf.writeVarInt(it.cost());
        }
    }

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return ID;
    }

    /** Convert to a ConfigShop instance on the client side for easy opening. */
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

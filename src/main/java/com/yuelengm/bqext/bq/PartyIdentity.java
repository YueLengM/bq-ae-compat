package com.yuelengm.bqext.bq;

import java.util.UUID;

import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.util.ResourceLocation;

import com.yuelengm.bqext.BQExt;

import betterquesting.api.properties.IPropertyContainer;
import betterquesting.api.properties.IPropertyType;
import betterquesting.api.questing.party.IParty;

public final class PartyIdentity {

    private static final IPropertyType<UUID> PARTY_UID = new IPropertyType<>() {

        private final ResourceLocation key = new ResourceLocation(BQExt.MODID, "party_uid");

        @Override
        public ResourceLocation getKey() {
            return this.key;
        }

        @Override
        public UUID getDefault() {
            return null;
        }

        @Override
        public UUID readValue(NBTBase nbt) {
            if (!(nbt instanceof NBTTagString)) return null;

            try {
                return UUID.fromString(((NBTTagString) nbt).func_150285_a_());
            } catch (IllegalArgumentException ignored) {
                return null;
            }
        }

        @Override
        public NBTBase writeValue(UUID value) {
            return new NBTTagString(value.toString());
        }
    };

    private PartyIdentity() {}

    public static UUID get(IParty party) {
        IPropertyContainer properties = party.getProperties();
        return properties.getProperty(PARTY_UID, null);
    }

    public static UUID getOrCreate(IParty party) {
        IPropertyContainer properties = party.getProperties();
        UUID partyUid = properties.getProperty(PARTY_UID, null);
        if (partyUid != null) return partyUid;

        partyUid = UUID.randomUUID();
        properties.setProperty(PARTY_UID, partyUid);
        return partyUid;
    }
}

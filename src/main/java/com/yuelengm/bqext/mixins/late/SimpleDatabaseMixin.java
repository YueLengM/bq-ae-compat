package com.yuelengm.bqext.mixins.late;

import java.util.UUID;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.yuelengm.bqext.ae.BqInterfaceIndex;
import com.yuelengm.bqext.bq.PartyIdentity;

import betterquesting.api.questing.party.IParty;
import betterquesting.api2.storage.SimpleDatabase;
import betterquesting.questing.party.PartyManager;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.relauncher.Side;

@Mixin(SimpleDatabase.class)
public abstract class SimpleDatabaseMixin {

    @Inject(method = "removeID", at = @At("HEAD"), remap = false)
    private void bqext$clearPartyInterfaceIndex(int partyId, CallbackInfoReturnable<Boolean> cir) {
        if (!((Object) this instanceof PartyManager) || FMLCommonHandler.instance()
            .getEffectiveSide() != Side.SERVER) {
            return;
        }

        Object value = ((SimpleDatabase<?>) (Object) this).getValue(partyId);
        if (!(value instanceof IParty)) return;

        IParty party = (IParty) value;
        UUID partyUid = PartyIdentity.get(party);
        BqInterfaceIndex.clearPartyOnServer(partyUid, partyId);
    }
}

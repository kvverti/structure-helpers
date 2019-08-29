package robosky.structurehelpers.iface;

import net.minecraft.network.listener.PacketListener;

import robosky.structurehelpers.network.OpenLootDataS2CPacket;

public interface StructHelpClientPacketListener extends PacketListener {

    void structhelp_onOpenLootData(OpenLootDataS2CPacket packet);
}

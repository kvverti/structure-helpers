package robosky.structurehelpers.iface;

import net.minecraft.network.listener.PacketListener;

import robosky.structurehelpers.network.UpdateLootDataC2SPacket;

public interface StructHelpServerPacketListener extends PacketListener {

    void structhelp_onLootData(UpdateLootDataC2SPacket packet);
}

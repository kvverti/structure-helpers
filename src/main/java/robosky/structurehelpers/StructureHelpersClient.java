package robosky.structurehelpers;

import net.fabricmc.api.ClientModInitializer;

import robosky.structurehelpers.network.ClientStructHelpPackets;

public final class StructureHelpersClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        ClientStructHelpPackets.init();
    }
}

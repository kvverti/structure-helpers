package robosky.structurehelpers;

import robosky.structurehelpers.network.ClientStructHelpPackets;

import net.fabricmc.api.ClientModInitializer;

public final class StructureHelpersClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        ClientStructHelpPackets.init();
    }
}

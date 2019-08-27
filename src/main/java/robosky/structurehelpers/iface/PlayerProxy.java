package robosky.structurehelpers.iface;

import robosky.structurehelpers.block.LootDataBlockEntity;

/**
 * Proxies server-client specific actions in order to avoid loading
 * client classes in dedicated server environments.
 */
public interface PlayerProxy {

    void structhelp_openLootDataBlock(LootDataBlockEntity be);
}

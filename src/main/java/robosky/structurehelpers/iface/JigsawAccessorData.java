package robosky.structurehelpers.iface;

/**
 * Provides accessors for the fields added to JigsawBlockEntity.
 */
public interface JigsawAccessorData {

    String CHILD_JUNCTION = "StructHelp_ChildJunction";

    boolean structhelp_isChildJunction();

    void structhelp_setChildJunction(boolean child);
}

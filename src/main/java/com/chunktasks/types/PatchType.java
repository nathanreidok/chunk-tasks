package com.chunktasks.types;

import lombok.Getter;
import net.runelite.api.Varbits;

@Getter
public enum PatchType {
    ALLOTMENT(
            Varbits.FARMING_4771,
            Varbits.FARMING_4772,
            Varbits.FARMING_4773,
            Varbits.FARMING_4774
    ),
    TREE(
            Varbits.FARMING_4771,
            Varbits.FARMING_7905
    ),
    FRUIT_TREE(
            Varbits.FARMING_4771,
            Varbits.FARMING_4772,
            Varbits.FARMING_7909
    ),
    HARDWOOD_TREE(
            Varbits.FARMING_4771,
            Varbits.FARMING_4772,
            Varbits.FARMING_4773
    ),
    REDWOOD(Varbits.FARMING_7907),
    SPIRIT_TREE(
            Varbits.FARMING_4771,
            Varbits.FARMING_4772,
            Varbits.FARMING_7904
    ),
    BELLADONNA(Varbits.FARMING_4771),
    MUSHROOM(Varbits.FARMING_4771),
    HESPORI(Varbits.FARMING_7908),
    HERB(
            Varbits.FARMING_4771,
            Varbits.FARMING_4772,
            Varbits.FARMING_4774,
            Varbits.FARMING_4775
    ),
    FLOWER(
            Varbits.FARMING_4773,
            Varbits.FARMING_7906
    ),
    BUSH(
            Varbits.FARMING_4771,
            Varbits.FARMING_4772
    ),
    HOPS(Varbits.FARMING_4771),
    ANIMA(Varbits.FARMING_7911),
    CACTUS(
            Varbits.FARMING_4771,
            Varbits.FARMING_7904
    ),
    SEAWEED(
            Varbits.FARMING_4771,
            Varbits.FARMING_4772
    ),
    CALQUAT(Varbits.FARMING_4771),
    CELASTRUS(Varbits.FARMING_7910),
    GRAPES(
            Varbits.GRAPES_4953,
            Varbits.GRAPES_4954,
            Varbits.GRAPES_4955,
            Varbits.GRAPES_4956,
            Varbits.GRAPES_4957,
            Varbits.GRAPES_4958,
            Varbits.GRAPES_4959,
            Varbits.GRAPES_4960,
            Varbits.GRAPES_4961,
            Varbits.GRAPES_4962,
            Varbits.GRAPES_4963,
            Varbits.GRAPES_4964
    ),
    CRYSTAL_TREE(Varbits.FARMING_4775),
    COMPOST(
            Varbits.FARMING_4774,
            Varbits.FARMING_4775
    ),
    BIG_COMPOST(Varbits.FARMING_7912);

    private final int[] varbits;

    PatchType(int... patchTypeVarbits) {
        this.varbits = patchTypeVarbits;
    }
}

package com.chunktasks.tasks;

import com.chunktasks.types.PatchType;
import lombok.Getter;

import java.util.List;

@Getter
public class FarmingPatchConfig {
    private PatchType patchType;
    private List<ValueRange> varbitRanges;
}

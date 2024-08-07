package com.chunktasks.tasks;

import com.chunktasks.types.PatchType;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class FarmingPatchConfig {
    private PatchType patchType;
    private List<ValueRange> varbitRanges;
}

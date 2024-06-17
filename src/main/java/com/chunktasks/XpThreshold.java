package com.chunktasks;

import lombok.Getter;
import lombok.Setter;

public class XpThreshold {
    @Getter @Setter
    private int min;
    @Getter @Setter
    private int max;

    public XpThreshold(int min, int max) {
        this.min = min;
        this.max = max;
    }

    public boolean isMet(int value) {
        return value >= min && value <= max;
    }
}

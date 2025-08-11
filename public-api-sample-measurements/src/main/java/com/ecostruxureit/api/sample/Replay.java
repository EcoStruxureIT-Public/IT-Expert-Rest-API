package com.ecostruxureit.api.sample;

import java.util.Objects;

/**
 * Copyright © 2025 Schneider Electric. All Rights Reserved.
 * <p>
 */
final class Replay {

    private final String fromOffset;

    private final String toOffset;

    Replay(String fromOffset, String toOffset) {

        this.fromOffset = Objects.requireNonNull(fromOffset);
        this.toOffset = Objects.requireNonNull(toOffset);
    }

    String getFromOffset() {

        return fromOffset;
    }

    String getToOffset() {

        return toOffset;
    }

    @Override
    public boolean equals(Object o) {

        if (this == o) {
            return true;
        }
        if (!(o instanceof Replay replay)) {
            return false;
        }
        return fromOffset.equals(replay.fromOffset) && toOffset.equals(replay.toOffset);
    }

    @Override
    public int hashCode() {

        return Objects.hash(fromOffset, toOffset);
    }

    @Override
    public String toString() {

        return "Replay{" + "fromOffset='" + fromOffset + '\'' + ", toOffset='" + toOffset + '\'' + '}';
    }
}

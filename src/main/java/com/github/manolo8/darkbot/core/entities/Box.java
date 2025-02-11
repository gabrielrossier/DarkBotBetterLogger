package com.github.manolo8.darkbot.core.entities;

import com.github.manolo8.darkbot.config.BoxInfo;
import com.github.manolo8.darkbot.config.ConfigEntity;
import com.github.manolo8.darkbot.core.api.GameAPI;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;

import static com.github.manolo8.darkbot.Main.API;

public class Box extends Entity implements eu.darkbot.api.game.entities.Box {

    private long collectedUntil;
    private int retries = 0;

    public String type, hash;

    public BoxInfo boxInfo;

    public Box(int id, long address) {
        super(id);
        this.update(address);
    }

    @Override
    public String getHash() {
        return hash;
    }

    @Override
    public String getTypeName() {
        return type;
    }

    @Override
    public eu.darkbot.api.config.types.BoxInfo getInfo() {
        return boxInfo;
    }

    public boolean isCollected() {
        return removed || System.currentTimeMillis() < collectedUntil;
    }

    @Override
    public boolean tryCollect() {
        if (API.hasCapability(GameAPI.Capability.DIRECT_COLLECT_BOX)) {
            API.collectBox(this, this.address);
            setCollected();
            return true;
        }

        if (trySelect(false)) {
            setCollected();
            return true;
        }

        return false;
    }

    public void setCollected() {
        collectedUntil = System.currentTimeMillis() + getNextWait();
        retries++;
    }

    public int getRetries() {
        return retries;
    }

    @Override
    public @Nullable Instant isCollectedUntil() {
        return Instant.ofEpochMilli(collectedUntil);
    }

    public int getNextWait() {
        return retries % 3 == 0 ? 50 : // one every 3 is an "instant retry"
                retries < 5 ? retries * 1_000 : retries * 60_000; // After 5 retries consider box bugged
    }

    @Override
    public void update() {
        super.update();
        clickable.update();
    }

    @Override
    public void update(long address) {
        super.update(address);

        if (traits.size == 0) {
            boxInfo = new BoxInfo();
            return;
        }
        long data = traits.get(0);

        data = API.readMemoryLong(data + 64);
        data = API.readMemoryLong(data + 32);
        data = API.readMemoryLong(data + 24);
        data = API.readMemoryLong(data + 8);
        data = API.readMemoryLong(data + 16);
        data = API.readMemoryLong(data + 24);

        type = API.readMemoryString(data);
        hash = API.readMemoryString(address, 160);

        if (type.length() > 5) type = type.split(",")[0].replace("box_", "").replace("_box", "");

        boxInfo = ConfigEntity.INSTANCE.getOrCreateBoxInfo(type);
    }

    public static class Ore extends Box implements eu.darkbot.api.game.entities.Ore {

        public Ore(int id, long address) {
            super(id, address);
        }
    }

    public static class Beacon extends Box implements eu.darkbot.api.game.entities.Box.BeaconBox {

        public Beacon(int id, long address) {
            super(id, address);
        }
    }
}

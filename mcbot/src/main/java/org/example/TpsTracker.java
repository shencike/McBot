package org.example;

import org.geysermc.mcprotocollib.network.packet.Packet;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.title.ClientboundSetTitleTextPacket;

import java.util.Arrays;

public class TpsTracker {

    public static final TpsTracker INSTANCE = new TpsTracker();

    private final float[] tickRates = new float[20];
    private int nextIndex = 0;
    private long timeLastTimeUpdate = -1;
    private long timeGameJoined;

    private TpsTracker() {
    }

    public void onPacketReceive() {
        long now = System.currentTimeMillis();
        if (timeLastTimeUpdate != -1) {
            float timeElapsed = (float) (now - timeLastTimeUpdate) / 1000.0F;
            tickRates[nextIndex] = clamp(20.0f / timeElapsed);
            nextIndex = (nextIndex + 1) % tickRates.length;
        }
        timeLastTimeUpdate = now;
    }

    public void onGameJoined() {
        Arrays.fill(tickRates, 0);
        nextIndex = 0;
        timeGameJoined = timeLastTimeUpdate = System.currentTimeMillis();
    }

    public float getTickRate() {
        if (System.currentTimeMillis() - timeGameJoined < 4000) {
            return 20;
        }

        int numTicks = 0;
        float sumTickRates = 0.0f;
        for (float tickRate : tickRates) {
            if (tickRate > 0) {
                sumTickRates += tickRate;
                numTicks++;
            }
        }
        return numTicks > 0 ? sumTickRates / numTicks : 0.0f;
    }

    private float clamp(float value) {
        return Math.max(0.0f, Math.min(value, 20.0f));
    }
}

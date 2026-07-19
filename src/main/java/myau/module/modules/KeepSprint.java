package myau.module.modules;

import myau.module.Module;
import myau.property.properties.BooleanProperty;
import myau.property.properties.PercentProperty;
import net.minecraft.client.Minecraft;

public class KeepSprint extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();

    // ———— 原 Rise 的全部属性 ————
    public final PercentProperty slowDownVelocity = new PercentProperty("Hit Slow Down During Velocity", 0.6);
    public final PercentProperty slowDownNormal = new PercentProperty("Hit Slow Down Normal", 0.6);
    public final PercentProperty bufferDecrease = new PercentProperty("Buffer Decrease", 1.0);
    public final PercentProperty maxBuffer = new PercentProperty("Max Buffer", 5.0);

    public final BooleanProperty sprintSlowDownVelocity = new BooleanProperty("Velocity Hit Sprint", false);
    public final BooleanProperty sprintSlowDownNormal = new BooleanProperty("Normal Hit Sprint", false);
    public final BooleanProperty bufferAbuse = new BooleanProperty("Buffer Abuse", false);
    public final BooleanProperty onlyInAir = new BooleanProperty("Only In Air", false);

    // ———— 原 Rise 的状态变量（By / Bz） ————
    public boolean hitFlag;      // 对应 By
    public double bufferCount;   // 对应 Bz

    public KeepSprint() {
        super("KeepSprint", false);
    }

    /**
     * 原 Rise 的事件监听器逻辑全部移植到此处（每帧执行）
     * 若你的 Myau 客户端有 onUpdate / onTick 钩子，此方法会被自动调用；
     * 若没有，你可以在模块的 onEnable 中手动向事件总线注册一个 Tick 监听。
     */
    public void onUpdate() {
        if (mc.thePlayer == null || mc.theWorld == null) return;

        // —— 原条件：只在不满足 "在空中且 onlyInAir 开启" 时执行 ——
        // 即：当 玩家在地上 且 onlyInAir 为 true 时，直接返回（不做任何修改）
        if (mc.thePlayer.onGround && onlyInAir.getValue()) {
            return;
        }

        // —— 缓冲（Buffer Abuse）逻辑（完全复刻） ——
        if (bufferAbuse.getValue()) {
            if (bufferCount < maxBuffer.getValue() && !hitFlag) {
                bufferCount++;
            } else {
                if (bufferCount > 0) {
                    bufferCount = Math.max(0, bufferCount - bufferDecrease.getValue());
                    hitFlag = true;
                    return;  // 原代码在此处 return，不执行下面的减速/疾跑设置
                }
                hitFlag = false;
            }
        } else {
            bufferCount = 0;
            hitFlag = false;
        }

        // —— 受伤 / 正常状态下的减速与疾跑控制 ——
        if (mc.thePlayer.hurtTime > 0) {
            // 等效于 var1.setSlowDown(slowDownVelocity)
            double factor = slowDownVelocity.getValue();
            mc.thePlayer.motionX *= factor;
            mc.thePlayer.motionZ *= factor;

            // 等效于 var1.setSprint(sprintSlowDownVelocity)
            mc.thePlayer.setSprinting(sprintSlowDownVelocity.getValue());
        } else {
            double factor = slowDownNormal.getValue();
            mc.thePlayer.motionX *= factor;
            mc.thePlayer.motionZ *= factor;

            mc.thePlayer.setSprinting(sprintSlowDownNormal.getValue());
        }
    }

    /**
     * 原 Myau 模块的保留方法，用于外部判断是否保持疾跑。
     * 为了与 onUpdate 中的手动设置保持一致，这里返回当前应该疾跑的状态。
     */
    @Override
    public boolean shouldKeepSprint() {
        if (mc.thePlayer == null) return false;
        // 如果仅空中开启且玩家在空中，则沿用原疾跑逻辑（这里我们返回 true，因为 onUpdate 会控制）
        // 但为了让外部调用也能得到正确值，我们根据 hurtTime 和对应布尔值返回
        if (mc.thePlayer.hurtTime > 0) {
            return sprintSlowDownVelocity.getValue();
        } else {
            return sprintSlowDownNormal.getValue();
        }
    }
}

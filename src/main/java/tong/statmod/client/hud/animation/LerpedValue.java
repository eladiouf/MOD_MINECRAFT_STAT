package tong.statmod.client.hud.animation;

public class LerpedValue {
    private float current;
    private float target;
    private float speed;

    public LerpedValue(float initial, float speed) {
        this.current = initial;
        this.target = initial;
        this.speed = speed;
    }

    public LerpedValue(float initial) {
        this(initial, 0.15f);
    }

    public void chase(float target) {
        this.target = target;
    }

    public void tick() {
        current += (target - current) * speed;
    }

    public float getValue(float partialTicks) {
        return current;
    }

    public float getCurrent() {
        return current;
    }

    public boolean isAtTarget() {
        return Math.abs(current - target) < 0.001f;
    }
}

package com.dota2analyzer.clarity.model;

public class CombatEvent {
    private int tick;
    private String type;
    private String attacker;
    private String target;
    private String inflictor;
    private int value;

    public CombatEvent() {}

    public CombatEvent(int tick, String type, String attacker, String target, String inflictor, int value) {
        this.tick = tick;
        this.type = type;
        this.attacker = attacker;
        this.target = target;
        this.inflictor = inflictor;
        this.value = value;
    }

    public int getTick() { return tick; }
    public void setTick(int tick) { this.tick = tick; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getAttacker() { return attacker; }
    public void setAttacker(String attacker) { this.attacker = attacker; }

    public String getTarget() { return target; }
    public void setTarget(String target) { this.target = target; }

    public String getInflictor() { return inflictor; }
    public void setInflictor(String inflictor) { this.inflictor = inflictor; }

    public int getValue() { return value; }
    public void setValue(int value) { this.value = value; }
}

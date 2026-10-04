package com.corporate.vending.application.service;

import java.util.UUID;

public final class RedisKeys {

    private RedisKeys() { }

    public static String stock(UUID machineId, String slotNo)  { return "stock:" + machineId + ":" + slotNo; }
    public static String lock(UUID machineId, String slotNo)   { return "lock:slot:" + machineId + ":" + slotNo; }
    public static String busy(UUID machineId, String slotNo)   { return "busy:slot:" + machineId + ":" + slotNo; }
    public static String fault(UUID machineId, String slotNo)  { return "fault:slot:" + machineId + ":" + slotNo; }
    public static String available(UUID machineId)             { return "slots:available:" + machineId; }
    public static String heartbeat(UUID machineId)             { return "machine:hb:" + machineId; }
    public static String watchdogLock()                        { return "lock:job:order-watchdog"; }
}

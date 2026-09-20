package com.cinevora.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class WatchProgressPolicyTest {
    private final WatchProgressPolicy policy = new WatchProgressPolicy();

    @Test void derivesPositionAndPercentFromRealPlaybackTime() {
        int position = policy.resolvePosition(1284, 6480, 0);
        assertEquals(1284, position);
        assertEquals(20, policy.derivePercent(position, 6480, 0));
        assertTrue(policy.shouldContinue(20, position, 6480));
    }

    @Test void treatsNinetyPercentAsCompletedAndNotContinueWatching() {
        assertTrue(policy.isComplete(90, 0, 6480));
        assertFalse(policy.shouldContinue(90, 5832, 6480));
    }

    @Test void ignoresTinyProgressToAvoidNoisyContinueRows() {
        assertFalse(policy.shouldContinue(1, 60, 6480));
        assertEquals(0, policy.resolvePosition(null, 6480, 0));
    }
}

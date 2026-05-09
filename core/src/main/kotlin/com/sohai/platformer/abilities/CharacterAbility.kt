package com.sohai.platformer.abilities

/**
 * Interface for character-specific abilities.
 * Each character (Ebo, Laya, etc.) implements this to define their unique action.
 */
interface CharacterAbility {
    /**
     * Called every frame to update the ability state.
     * Use this to handle continuous effects, cooldowns, etc.
     */
    fun update(deltaTime: Float)

    /**
     * Called when the action button is just pressed (one-shot).
     */
    fun onActionPressed()

    /**
     * Called when the action button is held down.
     */
    fun onActionHeld()

    /**
     * Called when the action button is released.
     */
    fun onActionReleased()

    /**
     * Get a display name or label for the ability (e.g., "Seed Slam" for Ebo).
     */
    fun getAbilityName(): String

    /**
     * Get the current cooldown ratio (0f = ready, 1f = fully on cooldown).
     */
    fun getCooldownRatio(): Float = 0f
}

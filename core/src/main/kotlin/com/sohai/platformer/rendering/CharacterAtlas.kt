package com.sohai.platformer.rendering

import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.utils.Disposable

class CharacterAtlas(
    val idle: Array<TextureRegion>,
    val walk: Array<TextureRegion>,
    val jump: TextureRegion,
    val fall: TextureRegion,
    val wallSlide: TextureRegion,
    private val textures: List<Texture>,
) : Disposable {
    override fun dispose() = textures.forEach(Texture::dispose)
}

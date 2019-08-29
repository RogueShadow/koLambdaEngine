package net.granseal.koLambda

import java.awt.Graphics2D
import java.awt.event.KeyEvent
import java.awt.event.MouseEvent

open class ApplicationAdapter(title: String = "koLambda Engine", width: Int = 800, height: Int = 600,undecorated: Boolean = false): Application(title,width,height,undecorated) {
    var sceneRoot = Entity()
    override fun init() {}
    override fun update(delta: Float) {sceneRoot.update(delta)}
    override fun draw(g: Graphics2D) {clear();sceneRoot.draw(g)}
    override fun dispose() {}
    override fun mousePressed(e: MouseEvent) {sceneRoot.click(e)}
    override fun mouseClicked(e: MouseEvent) {sceneRoot.click(e)}
    override fun mouseReleased(e: MouseEvent) {sceneRoot.click(e)}
    override fun keyPressed(e: KeyEvent) {}
    override fun keyReleased(e: KeyEvent) {}
    override fun keyTyped(e: KeyEvent) {}
}
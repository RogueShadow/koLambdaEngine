package net.granseal.koLambda

import java.awt.*
import java.awt.event.KeyEvent
import java.awt.event.MouseEvent
import java.awt.geom.AffineTransform
import java.awt.geom.Point2D
import java.awt.geom.Rectangle2D

open class Entity(var pos: Point2D.Float = point()) {
    var bounds: Rectangle2D = rect()
    var active = false
    val components = mutableListOf<Component>()
    var parent: Entity? = null
    val props = mutableMapOf<String,Any>()
    val children = mutableListOf<Entity>()
    val updaters = mutableListOf<Entity.(Float) -> Unit>()
    val drawers = mutableListOf<Entity.(Graphics2D) -> Unit>()
    var clickHandler: ((MouseEvent, Point2D) -> Boolean)? = null
    var scale = 1.0
    var rotation = 0.0
    var transform = AffineTransform()
    var inheritRotation = false
    var inheritScale = true
    var inheritPosition = true
    var updating = false
    var hasFocus = false

    fun getLocalTransform(loc: Boolean = true, rot: Boolean = true, s: Boolean = true): AffineTransform {
        transform.setToIdentity()
        if (loc)transform.translate(pos.x.toDouble(), pos.y.toDouble())
        if (rot)transform.rotate(rotation)
        if (s)transform.scale(scale, scale)
        return transform
    }

    fun getWorldTransform(loc: Boolean = true, rot: Boolean = true, s: Boolean = true): AffineTransform {
        return if (parent == null) {
            getLocalTransform(loc,rot,s)
        } else {
            val trans = parent!!.getWorldTransform(loc,rot,s)
            trans.concatenate(getLocalTransform(loc,rot,s))
            trans
        }
    }

    fun update(delta: Float) {
        components.forEach { it.update(delta) }
        updaters.forEach { it(delta) }
        children.forEach { it.update(delta) }
    }

    fun draw(g: Graphics2D) {
        val saved = g.transform
        g.transform = getWorldTransform()
        components.forEach { it.draw(g) }
        drawers.forEach { it(g) }
        children.toList().forEach { it.draw(g) }
        g.transform = saved
    }

    fun add(e: Entity): Entity {
        e.parent = this
        children.add(e)
        return e
    }

    fun add(e: Entity.() -> Unit) = add(Entity().apply(e))

    fun getAll(): List<Entity> {
        val entities = mutableListOf<Entity>()
        entities.add(this)
        children.forEach {
            entities.addAll(it.getAll())
        }
        return entities
    }

    fun click(event: MouseEvent): Boolean {
        val entities = getAll()
        val filtered = entities.filter {
            val worldClicked = it.getWorldTransform().inverseTransform(event.point,null)
            it.bounds.contains(worldClicked)
        }

        var handled = false
        filtered.reversed().forEach {
            if (it.clickHandler != null && !handled){
                val clickPos = it.getWorldTransform().inverseTransform(event.point,null)
                handled = it.clickHandler?.invoke(event,point(clickPos.x.toFloat() - bounds.x.toFloat(),clickPos.y.toFloat() - bounds.y.toFloat()))!!
            }
        }

        return handled
    }

    // TODO Make this work
    fun keyEvent(event: KeyEvent): Boolean{
        val entities = getAll()
        var handled = false
        val focused = entities.filter { it.hasFocus }
        if (focused.isNotEmpty()){
            return focused.first().keyEvent(event)
        }else{
            entities.forEach{
                if (it.keyEvent(event))return true
            }
        }
        return false
    }

    fun getActualBounds(): Rectangle {
        return this.getWorldTransform().createTransformedShape(bounds).bounds
    }

    fun getEntityByClick(clicked: Point2D.Float): Entity? {
        val entities = getAll()
        val filtered = entities.filter {
            val worldClicked = it.getWorldTransform().inverseTransform(clicked,null)
            it.bounds.contains(worldClicked)
        }
        return filtered.lastOrNull()
    }

    fun addComp(c: Component){
        components.add(c)
        c.parent = this
        c.init()
    }
    fun addComp(comp: Entity.() -> Component){
        addComp(comp())
    }

    inline fun <reified T> getComponentByType(): T? {
        return this.components.firstOrNull{it is T} as T?
    }
}

interface Component {
    var parent: Entity
    fun init()
    fun update(delta: Float)
    fun draw(g: Graphics2D)
}

open class ComponentAdapter: Component {
    override var parent: Entity = Entity()
    override fun init() {}
    override fun update(delta: Float) {}
    override fun draw(g: Graphics2D) {}
}
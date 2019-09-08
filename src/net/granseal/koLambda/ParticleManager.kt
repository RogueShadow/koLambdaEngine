package net.granseal.koLambda

import java.awt.Color
import java.awt.Graphics2D
import java.awt.Shape
import java.awt.geom.Point2D
import kotlin.random.Random

object ParticleManager {
    var GRAVITY = 300f

    val particles = mutableListOf<Particle>()
    val addList = mutableListOf<Particle>()

    fun update(delta: Float){
        particles.addAll(addList)
        addList.clear()
        particles.forEach {
            it.aniVelocity?.update(delta)
            val aniVelocity = it.aniVelocity
            if (aniVelocity != null) {
                it.velocity = aniVelocity.value()
                if (it.gravity)it.velocity.y += GRAVITY * it.lifeCounter
            }else {
                if (it.gravity) it.velocity.y += GRAVITY * delta
            }
            it.position plusAssign (it.velocity * delta)
            it.lifeCounter += delta
            it.rotation += it.rotationVel * delta
            it.aniSize?.update(delta)
            it.aniColor?.update(delta)
            it.size = it.aniSize?.value() ?: it.size
        }
        particles.removeIf{ it.lifeCounter >= it.life }
    }

    fun draw(g: Graphics2D){
        particles.forEach {p ->
            val oldTrans = g.transform
            g.color = getColor(p,g)
            g.translate(p.position.x.toInt(),p.position.y.toInt())
            g.rotate(p.rotation)
            g.scale(p.size.toDouble(),p.size.toDouble())
            g.fill(p.shape)
            g.transform = oldTrans
        }
    }

    private fun getColor(p: Particle,g: Graphics2D): Color {
        var alpha = 1f
        if (p.lifeCounter < p.fadeInTime){
            alpha = p.lifeCounter / p.fadeInTime
        }else {
            val timeLeft = p.life - p.lifeCounter
            if (timeLeft <= p.fadeOutTime) {
                alpha = timeLeft / p.fadeOutTime
            }
        }
        val aniColor = p.aniColor
        return if (aniColor != null){
            Color(g.deviceConfiguration.colorModel.colorSpace,aniColor.value().getColorComponents(null),alpha)
        }else Color(g.deviceConfiguration.colorModel.colorSpace,p.color.getColorComponents(null),alpha)
    }

    fun emit(p: Particle){
        addList.add(p)
    }

}


data class Particle(var position: Point2D.Float, var life: Float = 1f, var color: Color = Color.ORANGE){
    var image = ""
    var shape: Shape = rect(-4f,-4f,8f,8f)
    var velocity = point()
    var rotation = 0.0
    var gravity = true
    var rotationVel = 0f
    var fadeOutTime = 0.25f
    var fadeInTime = 0.05f
    var lifeCounter = 0f
    var aniColor: AnimateColor? = null
    var size = 1f
    var aniSize: AnimateFloat? = null
    var aniVelocity: AnimatePoint? = null

    inline fun shape(s: () -> Shape): Particle {
        shape = s()
        return this
    }
    fun velocity(v: Point2D.Float): Particle {
        velocity = v
        return this
    }
    fun velocities(v: List<Point2D.Float>, duration: Float = 1f, loopType: Int = 0): Particle {
        aniVelocity = AnimatePoint(v,loopType,duration)
        return this
    }
    fun rndVelocity(scale: Float): Particle {
        velocity.x = (-1 + Random.nextFloat() * 2) * scale
        velocity.y = (-1 + Random.nextFloat() * 2) * scale
        return this
    }
    fun sizes(s: List<Float>, duration: Float = 1f, loopType: Int = 0): Particle {
        aniSize = AnimateFloat(s,loopType,duration)
        return this
    }
    fun size(s: Float): Particle {
        size = s
        return this
    }
    fun rndHue(v: Float): Particle {
        val hsb = Color.RGBtoHSB(color.red,color.green,color.blue,null)
        color = hsb(hsb[0] - v + (Random.nextFloat() * v*2),hsb[1],hsb[2])
        return this
    }
    fun fadeIn(t: Float): Particle {
        fadeInTime = t
        return this
    }
    fun fadeOut(t: Float): Particle {
        fadeOutTime = t
        return this
    }
    fun noGravity(): Particle {
        gravity = false
        return this
    }
    fun rndColor(): Particle {
        color = hsb(Random.nextFloat(), Random.nextFloat(), Random.nextFloat())
        return this
    }
    fun duration(l: Float): Particle {
        life = l
        return this
    }
    fun color(c: Color): Particle {
        color = c
        return this
    }
    fun colors(l: List<Color>,duration: Float = 1f,type: Int = 0): Particle {
        aniColor = AnimateColor(l,type,duration)
        return this
    }
    fun speed(s: Float): Particle {
        velocity *= s
        return this
    }
    fun rotation(r: Float, rVel: Float = 0f): Particle {
        rotation = r.toDouble()
        rotationVel = rVel
        return this
    }
    fun rndRotation(scale: Float): Particle {
        rotationVel = -scale + Random.nextFloat() * scale * 2
        return this
    }
    fun image(n: String): Particle {
        image = n
        return this
    }
}

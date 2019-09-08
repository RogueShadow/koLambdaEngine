package net.granseal.koLambda

import java.awt.Color
import java.awt.geom.Point2D
import java.awt.geom.Rectangle2D
import java.io.File
import java.io.InputStream
import java.net.URL
import kotlin.math.abs
import kotlin.random.Random
import kotlin.system.exitProcess

fun point(x: Double,y: Double) = point(x.toFloat(),y.toFloat())
fun point(x: Float, y: Float) = Point2D.Float(x,y)
fun point() = point(0f, 0f)
fun rect(x: Float = 0f,y: Float = 0f, w: Float = 0f, h: Float = 0f) = Rectangle2D.Float(x,y,w,h)
fun rColor() = Color(Math.random().toFloat(),Math.random().toFloat(),Math.random().toFloat())
fun hsb(hue: Float, sat: Float, bright: Float): Color {
    return Color.getHSBColor(hue,sat,bright)
}
fun Color.invert(): Color {
    val hsb = Color.RGBtoHSB(red,green,blue,null)
    hsb[0] += 0.5f
    if (hsb[0] > 1f)hsb[0] -= 1f
    return Color.getHSBColor(hsb[0],hsb[1],hsb[2])
}

fun keyHeld(keys: List<Int>): Boolean {
    for (k in keys) {
        if (keyHeld(k))return true
    }
    return false
}
fun keyPressed(keys: List<Int>): Boolean {
    for (k in keys) {
        if (keyPressed(k))return true
    }
    return false
}
fun keyHeld(code: Int) = Input.keyHeld(code)
fun keyHeld(char: Char) = Input.keyHeld(char)
fun keyPressed(code: Int) = Input.keyPressed(code)
fun keyPressed(char: Char) = Input.keyPressed(char)
fun mousePos(): Point2D.Float = Input.mouse.point()
fun mouseButton(b: Int) = Input.mouse.buttonsHeld[b]!!

fun getStream(file: String): InputStream {
    lateinit var stream: InputStream
    val path = "res/$file"
    try {
         stream = ::ApplicationAdapter.javaClass.classLoader.getResourceAsStream(path)!!
    }catch (e: Exception){
        println("Failed to get stream from $path")
        println("I was looking in ${File(path).absolutePath}")
        println("I was looking in ${File(path).path}")
        println("I was looking in ${File(path).canonicalPath}")
        e.printStackTrace()
        exitProcess(-1)
    }
    println("Loaded stream $path")
    return stream
}
fun getResource(file: String): URL = ::ApplicationAdapter.javaClass.getResource("res/$file")
fun getReader(file: String) = getStream(file).reader()
fun getBufReader(file: String) = getStream(file).bufferedReader()

fun <T: Comparable<T>> clamp(value: T, min: T, max: T): T {
    if (value < min) return min
    if (value > max)return max
    return value
}

fun Linear(t: Float) = t
fun SmoothStart(t: Float) = t*t
fun SmoothStop(t: Float) = 1 - (1-t)*(1-t)
fun SmoothStart2(t: Float) = t*t*t
fun SmoothStop2(t: Float) = 1 - (1-t)*(1-t)*(1-t)

fun <T> List<List<T>>.get(p: Point2D): T {
    return this[p.y.toInt()][p.x.toInt()]
}

class AnimateColor(private val values: List<Color>, var loopType: Int, var duration: Float): IAnimation<Color> {
    init {
        assert(values.isNotEmpty())
        assert(values.size >= 2)
    }
    private val colors = values.map{it.getColorComponents(null)}
    private val r = AnimateFloat(colors.map { it[0] },loopType,duration)
    private val g = AnimateFloat(colors.map { it[1] },loopType,duration)
    private val b = AnimateFloat(colors.map { it[2] },loopType,duration)

    override fun reset() {
        r.reset()
        g.reset()
        b.reset()
    }

    override fun setProgress(value: Float) {
        r.setProgress(value)
        g.setProgress(value)
        b.setProgress(value)
    }

    override fun update(delta: Float) {
        r.update(delta)
        g.update(delta)
        b.update(delta)
    }

    override fun value(): Color {
        return Color(r.value(),g.value(),b.value())
    }

    override fun begin(): Color {
        return Color(r.begin(),g.begin(),b.begin())
    }

    override fun end(): Color {
        return Color(r.end(),g.end(),b.end())
    }

    override fun finished(): Boolean {
        return r.finished()
    }

    override fun loopCount(): Int {
        return r.loopCount()
    }
}

class AnimatePoint(private val values: List<Point2D.Float>, var loopType: Int, var duration: Float): IAnimation<Point2D.Float> {
    init {
        assert(values.isNotEmpty())
        assert(values.size >= 2)
    }
    constructor(start: Point2D.Float, end: Point2D.Float, loopType: Int, duration: Float): this(listOf(start,end),loopType,duration)

    private val x = values.map { it.x }
    private val y = values.map { it.y }
    private val aniX = AnimateFloat(x,loopType,duration)
    private val aniY = AnimateFloat(y,loopType,duration)

    override fun reset() {
        aniX.reset()
        aniY.reset()
    }

    override fun setProgress(value: Float) {
        aniX.setProgress(value)
        aniY.setProgress(value)
    }

    override fun update(delta: Float) {
        aniX.update(delta)
        aniY.update(delta)
    }

    override fun value(): Point2D.Float {
        return point(aniX.value(),aniY.value())
    }

    override fun begin(): Point2D.Float {
        return point(aniX.begin(),aniY.begin())
    }

    override fun end(): Point2D.Float {
        return point(aniX.end(),aniY.end())
    }

    override fun finished(): Boolean {
        return aniX.finished()
    }

    override fun loopCount(): Int {
        return aniX.loopCount()
    }
}

interface IAnimation<T> {
    fun reset()
    fun setProgress(value: Float)
    fun update(delta: Float)
    fun value(): T
    fun begin(): T
    fun end(): T
    fun finished(): Boolean
    fun loopCount(): Int
}

class AnimateFloat(private val values:  List<Float>, var loopType: Int, var duration: Float): IAnimation<Float> {
    init {
        assert(values.isNotEmpty())
        assert(values.size >= 2)
    }
    constructor(start: Float, end: Float, loopType: Int, duration: Float): this(listOf<Float>(start,end),loopType, duration)

    private var direction = 1f
    private var loopCount = 0
    private var playing = true
    private var inverseDuration = 1f/duration
    private var progress: Float = 0f
    private fun internalMax() = values.size -1
    private fun internalProgress() = progress * (internalMax())

    override fun reset() {
        progress = 0f
        direction = 1f
        loopCount = 0
        playing = true
    }

    override fun update(delta: Float) {
        if (playing)progress += delta * direction * inverseDuration

        if (internalProgress() >= internalMax()){
            when (loopType){
                0 -> {
                    playing = false
                    progress = 1f
                    loopCount = 1
                }
                1 -> {
                    progress -= 1f
                    loopCount += 1
                }
                2 -> {
                    direction *= -1
                    progress = 1f
                }
                3 -> {
                    direction *= -1
                    progress = 1f
                }
            }
        }
        if (internalProgress() < 0){
            when(loopType){
                0 -> throw Exception("Going backwards in once mode.")
                1 -> throw Exception("Going backwards in forward mode.")
                2 -> {
                    playing = false
                    progress = 0f
                    loopCount = 1
                }
                3 -> {
                    direction *= -1
                    progress = 0f
                    loopCount += 1
                }
            }
        }
    }

    override fun value(): Float {
        if (internalProgress() >= internalMax())return end()
        if (internalProgress() <= 0)return begin()
        val firstValueIndex =  (internalProgress()).toInt()
        val progressThisStep = internalProgress() - (internalProgress()).toInt()
        return interpolate(values[firstValueIndex] ,values[firstValueIndex+1], progressThisStep)
    }

    override fun setProgress(value: Float) { progress = abs(value % 1f) }
    override fun begin() = values.first()
    override fun end() = values.last()
    override fun finished() = !playing
    override fun loopCount() = loopCount
}

fun interpolate(start: Float, end: Float, progress: Float): Float {
    return start + (end - start) * progress
}

class Noise1d(val width: Int) {
    val rand = Random(System.nanoTime())
    val noise = (0..width).map { rand.nextFloat()}

    fun noise(x: Float): Float {
        val start = if (x <= 0) 0 else x.toInt()
        val end = if (x >= width) width-1 else x.toInt() + 1
        return lerp(noise[start],noise[end],x - x.toInt())
    }

    fun lerp(start: Float, end: Float, percent: Float): Float {
        return start + (end - start) * percent
    }
}